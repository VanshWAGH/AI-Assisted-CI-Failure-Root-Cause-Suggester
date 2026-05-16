package com.rootcause.web.dto;

// import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request body for POST /api/v1/jobs/{jobId}/analyze
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzeRequest {

    /** Classification mode: RULE_BASED, ML_ONNX, or HYBRID */
    private String mode;
}
