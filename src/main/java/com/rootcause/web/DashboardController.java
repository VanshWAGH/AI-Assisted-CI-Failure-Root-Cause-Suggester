package com.rootcause.web;

import com.rootcause.service.DashboardService;
import com.rootcause.web.dto.DashboardStatsDTO;
import com.rootcause.web.dto.RecentAnalysisDTO;
import com.rootcause.web.dto.TrendDayDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistical and trend endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(
            summary = "Get dashboard summary statistics",
            description = "Returns count of failures today by type and average confidence score"
    )
    public ResponseEntity<DashboardStatsDTO> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/trend")
    @Operation(
            summary = "Get daily failure trend data",
            description = "Returns daily failure counts grouped by category for the past N days"
    )
    public ResponseEntity<List<TrendDayDTO>> getTrend(
            @Parameter(description = "Number of days in the past to look at")
            @RequestParam(defaultValue = "7") int days) {
        
        days = Math.min(Math.max(days, 1), 90); // constraint to 1-90 days
        return ResponseEntity.ok(dashboardService.getDashboardTrend(days));
    }

    @GetMapping("/recent")
    @Operation(
            summary = "Get recent analyses",
            description = "Returns a list of the most recent failure analyses"
    )
    public ResponseEntity<List<RecentAnalysisDTO>> getRecent(
            @Parameter(description = "Maximum number of recent analyses to return")
            @RequestParam(defaultValue = "10") int limit) {
        
        limit = Math.min(Math.max(limit, 1), 100); // constraint to 1-100 items
        return ResponseEntity.ok(dashboardService.getRecentAnalyses(limit));
    }
}
