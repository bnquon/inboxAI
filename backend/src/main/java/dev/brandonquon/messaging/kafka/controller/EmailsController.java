package dev.brandonquon.messaging.kafka.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.brandonquon.messaging.kafka.dto.EmailSummaryDto;
import dev.brandonquon.messaging.kafka.model.EmailStatus;

@RestController
@RequestMapping("/api/emails")
public class EmailsController {

  private static final String EMAIL_KEY_PREFIX = "email:";

  private final StringRedisTemplate redisTemplate;

  public EmailsController(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @GetMapping("/ignored")
  public ResponseEntity<List<EmailSummaryDto>> listIgnored() {
    Set<String> emailKeys = redisTemplate.keys(EMAIL_KEY_PREFIX + "*");
    if (emailKeys == null || emailKeys.isEmpty()) {
      return ResponseEntity.ok(List.of());
    }

    var hash = redisTemplate.opsForHash();
    List<EmailSummaryDto> list = new ArrayList<>();

    for (String key : emailKeys) {
      String status = (String) hash.get(key, "status");
      if (status == null || !EmailStatus.IGNORED.getValue().equalsIgnoreCase(status)) {
        continue;
      }
      String emailId = key.substring(EMAIL_KEY_PREFIX.length());
      String from = getOrEmpty(hash, key, "from");
      String subject = getOrEmpty(hash, key, "subject");
      String date = getOrEmpty(hash, key, "date");
      String snippet = getOrEmpty(hash, key, "snippet");
      list.add(new EmailSummaryDto(emailId, from, subject, date, snippet));
    }

    list.sort((a, b) -> (b.date()).compareTo(a.date()));
    return ResponseEntity.ok(list);
  }

  private static String getOrEmpty(HashOperations<String, Object, Object> hash, String key, String field) {
    Object value = hash.get(key, field);
    return value != null ? value.toString() : "";
  }
}
