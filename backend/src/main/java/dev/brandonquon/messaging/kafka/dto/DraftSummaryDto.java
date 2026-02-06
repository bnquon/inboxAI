package dev.brandonquon.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DraftSummaryDto(
    String emailId,
    String subject,
    String from,
    String draftSubject,
    String snippet,
    String status,
    String generatedAt,
    String category
) {}
