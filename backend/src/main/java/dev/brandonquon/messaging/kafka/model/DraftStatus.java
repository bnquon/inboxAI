package dev.brandonquon.messaging.kafka.model;

public enum DraftStatus {
  REJECTED("rejected"),
  PENDING("pending"),
  SKIPPED("skipped"),
  ACCEPTED("accepted");

  private final String value;

  DraftStatus(String value) {
  this.value = value;
  }

  public String getValue() {
  return value;
  }

  public static DraftStatus fromValue(String s) {
  if (s == null || s.isBlank()) return PENDING;
  for (DraftStatus status : values()) {
  if (status.value.equalsIgnoreCase(s)) return status;
  }
  return PENDING;
  }
}
