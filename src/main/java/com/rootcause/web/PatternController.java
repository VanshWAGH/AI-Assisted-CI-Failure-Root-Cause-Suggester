package com.rootcause.web;

import com.rootcause.domain.FailurePattern;
import com.rootcause.repository.FailurePatternRepository;
import com.rootcause.web.dto.FailurePatternDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for FailurePattern management.
 */
@RestController
@RequestMapping("/api/v1/patterns")
@RequiredArgsConstructor
@Tag(name = "Patterns", description = "Failure pattern catalogue management")
public class PatternController {

    private final FailurePatternRepository patternRepository;

    @GetMapping
    @Operation(summary = "List all patterns", description = "Returns all active and inactive failure patterns")
    public ResponseEntity<List<FailurePatternDTO>> listPatterns(
            @RequestParam(required = false) Boolean active) {

        List<FailurePattern> patterns = (active == null)
                ? patternRepository.findAll()
                : patternRepository.findAll().stream()
                        .filter(p -> p.getActive().equals(active))
                        .toList();

        return ResponseEntity.ok(patterns.stream()
                .map(this::toDTO)
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single pattern by ID")
    public ResponseEntity<FailurePatternDTO> getPattern(@PathVariable UUID id) {
        return patternRepository.findById(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    @Operation(summary = "Pattern statistics", description = "Returns count of patterns per failure type")
    public ResponseEntity<java.util.Map<String, Long>> getPatternStats() {
        List<FailurePattern> all = patternRepository.findAll();
        java.util.Map<String, Long> stats = all.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getFailureType().name().toLowerCase(),
                        java.util.stream.Collectors.counting()
                ));
        stats.put("total", (long) all.size());
        stats.put("active", all.stream().filter(FailurePattern::getActive).count());
        return ResponseEntity.ok(stats);
    }

    private FailurePatternDTO toDTO(FailurePattern p) {
        return FailurePatternDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .failureType(p.getFailureType().name().toLowerCase())
                .regexPattern(p.getRegexPattern())
                .explanationTemplate(p.getExplanationTemplate())
                .suggestedActionTemplate(p.getSuggestedActionTemplate())
                .priority(p.getPriority())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
