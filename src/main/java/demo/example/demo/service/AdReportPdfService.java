package demo.example.demo.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import demo.example.demo.dto.AdGlobalStats;
import demo.example.demo.dto.AdPerScreenStats;
import demo.example.demo.entity.Schedule;
import demo.example.demo.repositories.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdReportPdfService {

    private final AdStatsService adStatsService;
    private final ScheduleRepository scheduleRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat INT_FMT =
            NumberFormat.getIntegerInstance(new Locale("sq", "AL"));

    // Logo path inside src/main/resources
    private static final String LOGO_PATH = "/reports/company-logo.png";

    public AdReportPdfService(AdStatsService adStatsService,
                              ScheduleRepository scheduleRepository) {
        this.adStatsService = adStatsService;
        this.scheduleRepository = scheduleRepository;
    }

    // ========================================================================
    //                       LIFETIME PDF REPORT (USED BY BUTTON)
    // ========================================================================
    public byte[] generateLifetimeReport(Integer adId) {
        AdGlobalStats stats = adStatsService.computeTotalStats(adId);

        // Lifetime start/end from schedules
        List<Schedule> schedules = scheduleRepository.findByMediaAsset_Id(adId);
        LocalDate activeFrom = schedules.stream()
                .map(Schedule::getFromdate)
                .min(Comparator.naturalOrder())
                .orElse(null);

        LocalDate activeTo = schedules.stream()
                .map(Schedule::getTodate)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return buildPdf(
                adId,
                stats.getAdName(),
                activeFrom,
                activeTo,
                stats.getTotalPlays(),
                stats.getTotalSeconds(),
                stats.getPerScreen(),
                null,
                null
        );
    }

    // ========================================================================
    //                       OPTIONAL RANGE REPORT
    // ========================================================================
    public byte[] generateRangeReport(Integer adId, LocalDate reportFrom, LocalDate reportTo) {
        List<AdPerScreenStats> perScreenRange =
                adStatsService.computeStatsForRange(adId, reportFrom, reportTo);

        long totalPlaysRange = perScreenRange.stream()
                .mapToLong(AdPerScreenStats::getPlays)
                .sum();

        long totalSecondsRange = perScreenRange.stream()
                .mapToLong(AdPerScreenStats::getTotalSeconds)
                .sum();

        AdGlobalStats lifetimeStats = adStatsService.computeTotalStats(adId);
        String adName = lifetimeStats.getAdName();

        List<Schedule> schedules = scheduleRepository.findByMediaAsset_Id(adId);
        LocalDate activeFrom = schedules.stream()
                .map(Schedule::getFromdate)
                .min(Comparator.naturalOrder())
                .orElse(null);

        LocalDate activeTo = schedules.stream()
                .map(Schedule::getTodate)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return buildPdf(
                adId,
                adName,
                activeFrom,
                activeTo,
                totalPlaysRange,
                totalSecondsRange,
                perScreenRange,
                reportFrom,
                reportTo
        );
    }

    // ========================================================================
    //                  CORE PDF BUILDER (CUSTOM LAYOUT)
    // ========================================================================
    private byte[] buildPdf(Integer adId,
                            String adName,
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

            // ------------ FONTS ------------
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font tableHeaderFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(255, 255, 255));
            Font tableBodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            // ------------ HEADER (LOGO + TITLE) ------------
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100f);
            headerTable.setWidths(new float[]{1.5f, 3.5f});

            // Logo cell
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            Image logo = tryLoadLogo();
            if (logo != null) {
                logo.scaleToFit(80, 40);
                logoCell.addElement(logo);
            }
            headerTable.addCell(logoCell);

            // Title cell
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);

            Paragraph title = new Paragraph("Ad Performance Report", titleFont);
            title.setSpacingAfter(4f);
            titleCell.addElement(title);

            String campaignName = adName != null ? adName : "(Unknown ad)";
            Paragraph sub = new Paragraph(campaignName, subtitleFont);
            titleCell.addElement(sub);

            headerTable.addCell(titleCell);
            headerTable.setSpacingAfter(20f);
            document.add(headerTable);

            // ------------ SUMMARY BLOCK ------------
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100f);
            summaryTable.setSpacingAfter(18f);
            summaryTable.setWidths(new float[]{1.2f, 3.8f});

            addLabelValueRow(summaryTable, "Ad ID", String.valueOf(adId), labelFont, valueFont);
            addLabelValueRow(summaryTable, "Ad name", campaignName, labelFont, valueFont);

            if (activeFrom != null && activeTo != null) {
                addLabelValueRow(summaryTable,
                        "Lifetime",
                        DATE_FMT.format(activeFrom) + "  -  " + DATE_FMT.format(activeTo),
                        labelFont, valueFont);
            } else {
                addLabelValueRow(summaryTable,
                        "Lifetime",
                        "-",
                        labelFont, valueFont);
            }

            if (reportFrom != null && reportTo != null) {
                addLabelValueRow(summaryTable,
                        "Report range",
                        DATE_FMT.format(reportFrom) + "  -  " + DATE_FMT.format(reportTo),
                        labelFont, valueFont);
            }

            addLabelValueRow(summaryTable,
                    "Total plays",
                    INT_FMT.format(totalPlays),
                    labelFont, valueFont);

            addLabelValueRow(summaryTable,
                    "Total seconds",
                    INT_FMT.format(totalSeconds),
                    labelFont, valueFont);

            document.add(summaryTable);

            // ------------ PER-SCREEN TABLE ------------
            if (perScreen != null && !perScreen.isEmpty()) {

                PdfPTable table = new PdfPTable(5); // #, Screen, Plays, Seconds, Seconds/Play
                table.setWidthPercentage(100f);
                table.setSpacingBefore(5f);
                table.setWidths(new float[]{0.6f, 3f, 1.4f, 1.6f, 1.6f});

                // Header row
                addHeaderCell(table, "#", tableHeaderFont);
                addHeaderCell(table, "Screen", tableHeaderFont);
                addHeaderCell(table, "Plays", tableHeaderFont);
                addHeaderCell(table, "Seconds", tableHeaderFont);
                addHeaderCell(table, "Sec / play", tableHeaderFont);

                int index = 1;
                for (AdPerScreenStats row : perScreen) {
                    String rawName = row.getScreenName() != null ? row.getScreenName() : "(screen)";
                    String cleanedName = rawName.replace("++", "").trim();

                    long plays = row.getPlays();
                    long seconds = row.getTotalSeconds();

                    long secPerPlay = 0;
                    if (plays > 0 && seconds > 0) {
                        secPerPlay = Math.round((double) seconds / (double) plays);
                    }

                    addBodyCell(table, String.valueOf(index), tableBodyFont);
                    addBodyCell(table, cleanedName, tableBodyFont);
                    addBodyCell(table, INT_FMT.format(plays), tableBodyFont);
                    addBodyCell(table, INT_FMT.format(seconds), tableBodyFont);
                    addBodyCell(table, secPerPlay > 0 ? INT_FMT.format(secPerPlay) : "-", tableBodyFont);

                    index++;
                }

                document.add(table);
            } else {
                Paragraph p = new Paragraph("This ad has no recorded plays on any screen.", valueFont);
                document.add(p);
            }

            document.close();
            writer.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    // ========================================================================
    //                         HELPERS
    // ========================================================================
    private Image tryLoadLogo() {
        try (InputStream in = getClass().getResourceAsStream(LOGO_PATH)) {
            if (in == null) {
                return null;
            }
            byte[] data = in.readAllBytes();
            return Image.getInstance(data);
        } catch (IOException | BadElementException e) {
            return null;
        }
    }

    private void addLabelValueRow(PdfPTable table,
                                  String label,
                                  String value,
                                  Font labelFont,
                                  Font valueFont) {

        PdfPCell labelCell = new PdfPCell(new Phrase(label + ":", labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(2f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(2f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(23, 42, 15)); // Dark green-ish header like your web UI
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setPadding(5f);
        table.addCell(cell);
    }
}
