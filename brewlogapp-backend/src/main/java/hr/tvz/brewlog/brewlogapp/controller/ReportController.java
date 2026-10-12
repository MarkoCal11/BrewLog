package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.ReportResponse;
import hr.tvz.brewlog.brewlogapp.service.ReportService;
import hr.tvz.brewlog.brewlogapp.service.ReportStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportStreamService reportStreamService;

    public ReportController(ReportService reportService, ReportStreamService reportStreamService) {
        this.reportService = reportService;
        this.reportStreamService = reportStreamService;
    }

    @GetMapping
    public ResponseEntity<List<ReportResponse>> getAllReports(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.findAllReports(userDetails.getUsername()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReports(@AuthenticationPrincipal UserDetails userDetails) {
        return reportStreamService.subscribe(userDetails.getUsername());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReportById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reportService.findReportById(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
