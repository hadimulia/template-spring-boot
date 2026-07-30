package com.template.dto.approval;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long id;
    private String action;
    private String reviewNotes;
}
