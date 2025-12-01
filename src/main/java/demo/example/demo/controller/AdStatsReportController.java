package demo.example.demo.controller;

import demo.example.demo.service.AdReportPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ads-reports")
public class AdStatsReportController {

    private final AdReportPdfService adReportPdfService;

    public AdStatsReportController(AdReportPdfService adReportPdfService) {
        this.adReportPdfService = adReportPdfService;
    }

    /**
     * Export PDF for an ad.
     * This always generates the LIFETIME report for that ad.
     *
     * Called from the stats page's "Download Report (.pdf)" button:
     *   GET /ads/{adId}/stats/export?day=...
     */
    @GetMapping("/{adId}/stats/export")
    public ResponseEntity<byte[]> exportAdStatsPdf(@PathVariable Integer adId) {

        byte[] pdfBytes = adReportPdfService.generateLifetimeReport(adId);

        String filename = String.format("ad-%d-lifetime-report.pdf", adId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
