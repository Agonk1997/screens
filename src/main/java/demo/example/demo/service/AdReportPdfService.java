package demo.example.demo.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import demo.example.demo.dto.AdGlobalStats;
import demo.example.demo.dto.AdPerScreenStats;
import demo.example.demo.entity.MediaAsset;
import demo.example.demo.repositories.MediaAssetRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class AdReportPdfService {

    private final AdStatsService adStatsService;
    private final MediaAssetRepository mediaAssetRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat INT_FMT =
            NumberFormat.getIntegerInstance(new Locale("sq", "AL"));

    private static final String LOGO_PATH = "/reports/company-logo.png";

    public AdReportPdfService(AdStatsService adStatsService,
                              MediaAssetRepository mediaAssetRepository) {
        this.adStatsService = adStatsService;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    // ---------------------------------------------------------
    // LIFETIME REPORT
    // ---------------------------------------------------------
    public byte[] generateLifetimeReport(Integer adId) {
        AdGlobalStats stats = adStatsService.computeTotalStats(adId);

        LocalDate activeFrom = stats.getLifetimeFrom();
        LocalDate activeTo   = stats.getLifetimeTo();

        return buildPdf(
                adId,
                stats.getAdName(),
                stats.getCompanyname(),    // NEW FIELD
                activeFrom,
                activeTo,
                stats.getTotalPlays(),
                stats.getTotalSeconds(),
                stats.getPerScreen(),
                null,
                null
        );
    }

    // ---------------------------------------------------------
    // RANGE REPORT
    // ---------------------------------------------------------
    public byte[] generateRangeReport(Integer adId, LocalDate from, LocalDate to) {

        List<AdPerScreenStats> perScreenRange =
                adStatsService.computeStatsForRange(adId, from, to);

        long totalPlaysRange = perScreenRange.stream()
                .mapToLong(AdPerScreenStats::getPlays)
                .sum();

        long totalSecondsRange = perScreenRange.stream()
                .mapToLong(AdPerScreenStats::getTotalSeconds)
                .sum();

        AdGlobalStats lifetimeStats = adStatsService.computeTotalStats(adId);

        return buildPdf(
                adId,
                lifetimeStats.getAdName(),
                lifetimeStats.getCompanyname(),   // NEW FIELD
                lifetimeStats.getLifetimeFrom(),
                lifetimeStats.getLifetimeTo(),
                totalPlaysRange,
                totalSecondsRange,
                perScreenRange,
                from,
                to
        );
    }

    // ---------------------------------------------------------
    // PDF BUILDER
    // ---------------------------------------------------------
    private byte[] buildPdf(Integer adId,
                            String adName,
                            String companyName,     // NEW FIELD
                            LocalDate activeFrom,
                            LocalDate activeTo,
                            long totalPlays,
                            long totalSeconds,
                            List<AdPerScreenStats> perScreen,
                            LocalDate reportFrom,
                            LocalDate reportTo) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            document.open();

            // FONTS
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(255,255,255));
            Font cellFont = new Font(Font.HELVETICA, 9);

            // --------------------------------------------------
            // HEADER (LOGO + TITLE)
            // --------------------------------------------------
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.5f, 3.5f});

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);

            Image logo = tryLoadLogo();
            if (logo != null) {
                logo.scaleToFit(80, 40);
                logoCell.addElement(logo);
            }
            header.addCell(logoCell);

            PdfPCell textCell = new PdfPCell();
            textCell.setBorder(Rectangle.NO_BORDER);

            textCell.addElement(new Paragraph("Ad Performance Report", titleFont));
            textCell.addElement(new Paragraph(adName, subtitleFont));
            if (companyName != null && !companyName.isBlank()) {
                textCell.addElement(new Paragraph("Company: " + companyName, subtitleFont));
            }

            header.addCell(textCell);
            header.setSpacingAfter(20);
            document.add(header);

            // --------------------------------------------------
            // SUMMARY TABLE
            // --------------------------------------------------
            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(100);
            summary.setWidths(new float[]{1.2f, 4f});

            addSummaryRow(summary, "Ad ID", adId.toString(), labelFont, valueFont);
            addSummaryRow(summary, "Ad name", adName, labelFont, valueFont);
            addSummaryRow(summary, "Company", (companyName != null ? companyName : "-"), labelFont, valueFont);

            if (activeFrom != null && activeTo != null) {
                addSummaryRow(summary, "Lifetime", DATE_FMT.format(activeFrom) + " - " + DATE_FMT.format(activeTo),
                        labelFont, valueFont);
            } else {
                addSummaryRow(summary, "Lifetime", "-", labelFont, valueFont);
            }

            if (reportFrom != null && reportTo != null) {
                addSummaryRow(summary, "Report range",
                        DATE_FMT.format(reportFrom) + " - " + DATE_FMT.format(reportTo),
                        labelFont, valueFont);
            }

            addSummaryRow(summary, "Total plays", INT_FMT.format(totalPlays), labelFont, valueFont);
            addSummaryRow(summary, "Total seconds", INT_FMT.format(totalSeconds), labelFont, valueFont);

            summary.setSpacingAfter(15);
            document.add(summary);

            // --------------------------------------------------
            // PER-SCREEN TABLE
            // --------------------------------------------------
            if (perScreen != null && !perScreen.isEmpty()) {

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{0.7f, 3f, 1.3f, 1.3f, 1.3f});

                addHeader(table, "#", headerFont);
                addHeader(table, "Screen", headerFont);
                addHeader(table, "Plays", headerFont);
                addHeader(table, "Seconds", headerFont);
                addHeader(table, "Sec/play", headerFont);

                int i = 1;
                for (AdPerScreenStats row : perScreen) {

                    String screenName = row.getScreenName() != null
                            ? row.getScreenName().replace("++", "").trim()
                            : "(unknown)";

                    long plays = row.getPlays();
                    long secs = row.getTotalSeconds();
                    String sp = (plays > 0) ? String.valueOf(secs / plays) : "-";

                    addCell(table, String.valueOf(i++), cellFont);
                    addCell(table, screenName, cellFont);
                    addCell(table, INT_FMT.format(plays), cellFont);
                    addCell(table, INT_FMT.format(secs), cellFont);
                    addCell(table, sp, cellFont);
                }

                document.add(table);
            } else {
                document.add(new Paragraph("No plays recorded.", valueFont));
            }

            document.close();
            writer.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------
    private Image tryLoadLogo() {
        try (InputStream in = getClass().getResourceAsStream(LOGO_PATH)) {
            if (in == null) return null;
            return Image.getInstance(in.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell l = new PdfPCell(new Phrase(label + ":", labelFont));
        l.setBorder(Rectangle.NO_BORDER);

        PdfPCell v = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        v.setBorder(Rectangle.NO_BORDER);

        table.addCell(l);
        table.addCell(v);
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(23, 42, 15));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }
}
