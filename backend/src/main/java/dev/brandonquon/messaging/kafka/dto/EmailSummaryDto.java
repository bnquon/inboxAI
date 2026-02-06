package dev.brandonquon.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailSummaryDto(
    String emailId,
    String from,
    String subject,
    String date,
    String snippet
) {}
