package com.rootcause.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrendDayDTO {
    private String date;
    private long infra;
    private long test;
    private long build;
    private long security;
    private long unknown;
}
