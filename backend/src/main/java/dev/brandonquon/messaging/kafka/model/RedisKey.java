package dev.brandonquon.messaging.kafka.model;

/**
 * Redis key prefixes for hash keys. Use {@link #key(String)} to build the full key.
 */
public enum RedisKey {
  EMAIL("email:"),
  DRAFT("draft:");

  private final String prefix;

  RedisKey(String prefix) {
  this.prefix = prefix;
  }

  /** Builds the full Redis key for the given id, e.g. EMAIL.key("abc") → "email:abc". */
  public String key(String id) {
  return id == null || id.isBlank() ? prefix : prefix + id;
  }
}
