package com.uade.lime.property.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record InquiryInboxResponse(
        List<InquiryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long unreadCount) {

    public static InquiryInboxResponse from(Page<InquiryResponse> result, long unreadCount) {
        return new InquiryInboxResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                unreadCount);
    }
}
