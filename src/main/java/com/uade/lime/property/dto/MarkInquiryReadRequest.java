package com.uade.lime.property.dto;

import jakarta.validation.constraints.NotNull;

public record MarkInquiryReadRequest(@NotNull Boolean read) {
}
