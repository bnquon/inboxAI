package dev.brandonquon.messaging.kafka.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.brandonquon.messaging.kafka.config.KafkaConfig;
import dev.brandonquon.messaging.kafka.dto.SeedEmailRequest;
import dev.brandonquon.messaging.kafka.model.RedisKey;
import dev.brandonquon.messaging.kafka.service.GmailService;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

  private final GmailService gmailService;
  private final KafkaTemplate<String, String> stringKafkaTemplate;
  private final StringRedisTemplate redisTemplate;

  public GmailController(GmailService gmailService, KafkaTemplate<String, String> stringKafkaTemplate,
      StringRedisTemplate redisTemplate) {
    this.gmailService = gmailService;
    this.stringKafkaTemplate = stringKafkaTemplate;
    this.redisTemplate = redisTemplate;
  }

  private static String extractEmailAddress(String headerValue) {
    if (headerValue == null || headerValue.isBlank()) return "";
    String s = headerValue.trim();
    int open = s.indexOf('<');
    int close = s.indexOf('>', open);
    if (open >= 0 && close > open) {
      return s.substring(open + 1, close).trim();
    }
    return s;
  }

  /**
   * Run the inbox poll once. Check the app console for message IDs.
   * GET http://localhost:8080/api/gmail/poll
   */
  @GetMapping("/poll")
  public ResponseEntity<Map<String, String>> poll() {
    gmailService.fetchNewEmails();
    return ResponseEntity.ok(Map.of(
        "message", "Poll run. Check the application console for message IDs."));
  }

  /**
   * Send a test email id to the incoming-emails topic (to test IncomingEmailListener).
   * GET http://localhost:8080/api/gmail/test?id=test-email-1
   */
  @GetMapping("/test")
  public ResponseEntity<Map<String, String>> test(@RequestParam(defaultValue = "test-email-1") String id) {
    stringKafkaTemplate.send(KafkaConfig.INCOMING_EMAILS_TOPIC, id, id);
    return ResponseEntity.ok(Map.of(
        "message", "Sent test email id to Kafka topic " + KafkaConfig.INCOMING_EMAILS_TOPIC,
        "id", id));
  }

  /**
   * Seed demo emails into Redis and send their ids to the pipeline (categorization → draft generation).
   * POST http://localhost:8080/api/gmail/seed with JSON array of { "id", "from", "subject", "body", "snippet" (optional) }.
   */
  @PostMapping("/seed")
  public ResponseEntity<Map<String, Object>> seed(@RequestBody List<SeedEmailRequest> emails) {
    if (emails == null || emails.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("message", "Request body must be a non-empty array of demo emails."));
    }
    var hash = redisTemplate.opsForHash();
    int seeded = 0;
    for (SeedEmailRequest e : emails) {
      if (e.getId() == null || e.getId().isBlank()) continue;
      String id = e.getId().trim();
      String from = e.getFrom() != null ? e.getFrom() : "";
      String subject = e.getSubject() != null ? e.getSubject() : "";
      String body = e.getBody() != null ? e.getBody() : "";
      String snippet = e.getSnippet() != null && !e.getSnippet().isBlank()
          ? e.getSnippet()
          : (body.length() > 150 ? body.substring(0, 150) + "..." : body);
      String replyToEmail = extractEmailAddress(from);

      String key = RedisKey.EMAIL.key(id);
      hash.put(key, "id", id);
      hash.put(key, "threadId", "");
      hash.put(key, "from", from);
      hash.put(key, "replyToEmail", replyToEmail);
      hash.put(key, "subject", subject);
      hash.put(key, "date", "");
      hash.put(key, "snippet", snippet);
      hash.put(key, "body", body);

      stringKafkaTemplate.send(KafkaConfig.INCOMING_EMAILS_TOPIC, id, id);
      seeded++;
    }
    return ResponseEntity.ok(Map.of(
        "message", "Seeded " + seeded + " demo email(s). Pipeline (categorization → draft generation) triggered.",
        "seeded", seeded));
  }
}
