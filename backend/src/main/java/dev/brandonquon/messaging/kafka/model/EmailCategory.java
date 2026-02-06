package dev.brandonquon.messaging.kafka.model;

public enum EmailCategory {
  NEWSLETTER("newsletter"),
  PROMOTIONAL("promotional"),
  SOCIAL("social"),
  SPAM("spam"),
  SCAM("scam"),
  GENERAL("general"),
  OTHER("other"),
  IGNORED("ignored"),
  FAILED("failed");

  private final String value;

  EmailCategory(String value) {
  this.value = value;
  }

  public String getValue() {
  return value;
  }

  /** Parse from string (e.g. Redis or API); defaults to OTHER if unknown. */
  public static EmailCategory fromValue(String s) {
  if (s == null || s.isBlank()) return OTHER;
  for (EmailCategory c : values()) {
  if (c.value.equalsIgnoreCase(s)) return c;
  }
  return FAILED;
  }
}
