package dev.brandonquon.messaging.kafka.model;

public enum EmailStatus {
  CATEGORIZATION_FAILED("categorization_failed"),
  SCAM_DETECTED("scam_detected"),
  LIKELY_SPAM("likely_spam"),
  SUBSCRIPTION_MAIL("subscription_mail"),
  SENT_TO_DRAFT_GENERATION("sent_to_draft_generation"),
  SENT_TO_EMAIL_CATEGORIZATION("sent_to_email_categorization"),
  DRAFT_CREATED("draft_created"),
  IGNORED("ignored"),
  DRAFT_GENERATION_FAILED("draft_generation_failed");

  private final String value;

  EmailStatus(String value) {
  this.value = value;
  }

  public String getValue() {
  return value;
  }

  /** Parse from string (e.g. Redis or API); returns null if unknown. */
  public static EmailStatus fromValue(String s) {
  if (s == null || s.isBlank()) return null;
  for (EmailStatus status : values()) {
  if (status.value.equalsIgnoreCase(s)) return status;
  }
  return null;
  }
}
