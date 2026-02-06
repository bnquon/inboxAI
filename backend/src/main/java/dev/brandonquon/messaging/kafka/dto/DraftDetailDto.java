package dev.brandonquon.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DraftDetailDto(
    EmailPart email,
    DraftPart draft
) {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record EmailPart(String id, String from, String subject, String body, String date) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record DraftPart(String draftText, String draftSubject, String status, String generatedAt, String category) {}
}
