package com.mhc.dashboard.dtos;

import lombok.Data;
import java.util.List;

@Data
public class ErrorClassificationPayload {
    private List<ErrorClassificationRequest> classifications;
}
