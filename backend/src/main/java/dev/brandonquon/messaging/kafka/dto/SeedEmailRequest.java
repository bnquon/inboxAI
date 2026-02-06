package dev.brandonquon.messaging.kafka.dto;

/**
 * Request body for seeding a demo email into Redis and triggering the pipeline.
 */
public class SeedEmailRequest {

  private String id;
  private String from;
  private String subject;
  private String body;
  private String snippet;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getSnippet() {
    return snippet;
  }

  public void setSnippet(String snippet) {
    this.snippet = snippet;
  }
}
