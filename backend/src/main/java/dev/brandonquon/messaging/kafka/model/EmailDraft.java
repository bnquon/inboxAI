package dev.brandonquon.messaging.kafka.model;

import java.time.LocalDateTime;

public class EmailDraft {
  private String responseToEmailId;
  private String draftText;
  private String draftSubject;
  private DraftStatus status;
  private LocalDateTime generatedAt;

  public EmailDraft(String responseToEmailId, String draftText, String draftSubject, DraftStatus status, LocalDateTime generatedAt) {
  this.responseToEmailId = responseToEmailId;
  this.draftText = draftText;
  this.draftSubject = draftSubject;
  this.status = status;
  this.generatedAt = generatedAt;
  }

  public String getResponseToEmailId() { return responseToEmailId; }
  public String getDraftText() { return draftText; }
  public String getDraftSubject() { return draftSubject; }
  public DraftStatus getStatus() { return status; }
  public LocalDateTime getGeneratedAt() { return generatedAt; }
}
