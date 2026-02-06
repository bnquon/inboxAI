package dev.brandonquon.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateDraftRequest(String draftText, String draftSubject) {}
