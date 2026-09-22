package com.uade.lime.property.dto;

import com.uade.lime.property.model.Inquiry;
import org.springframework.data.domain.Page;

public record InquiryInboxResponse(Page<Inquiry> inquiries, long unreadCount) {}
