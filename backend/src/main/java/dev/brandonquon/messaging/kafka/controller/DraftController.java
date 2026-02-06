package dev.brandonquon.messaging.kafka.controller;

import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.brandonquon.messaging.kafka.dto.DraftDetailDto;
import dev.brandonquon.messaging.kafka.dto.DraftSummaryDto;
import dev.brandonquon.messaging.kafka.dto.UpdateDraftRequest;
import dev.brandonquon.messaging.kafka.model.DraftStatus;
import dev.brandonquon.messaging.kafka.model.RedisKey;
import dev.brandonquon.messaging.kafka.service.GmailService;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/drafts")
public class DraftController {

  private static final String DRAFT_KEY_PREFIX = "draft:";

  private final StringRedisTemplate redisTemplate;
  private final GmailService gmailService;

  public DraftController(StringRedisTemplate redisTemplate, GmailService gmailService) {
    this.redisTemplate = redisTemplate;
    this.gmailService = gmailService;
  }

  @GetMapping
  public ResponseEntity<List<DraftSummaryDto>> list() {
    // Get all draft keys matching pattern
    Set<String> draftKeys = redisTemplate.keys(DRAFT_KEY_PREFIX + "*");
    if (draftKeys == null || draftKeys.isEmpty()) {
      return ResponseEntity.ok(List.of());
    }

    var hash = redisTemplate.opsForHash();
    
    List<DraftSummaryDto> list = draftKeys.stream()
      .map(key -> {
        String emailId = key.substring(DRAFT_KEY_PREFIX.length());
        String emailKey = RedisKey.EMAIL.key(emailId);
        
        return new DraftSummaryDto(
          emailId,
          getOrEmpty(hash, emailKey, "subject"),
          getOrEmpty(hash, emailKey, "from"),
          getOrEmpty(hash, key, "draftSubject"),
          getOrEmpty(hash, emailKey, "snippet"),
          getOrEmpty(hash, key, "status"),
          getOrEmpty(hash, key, "generatedAt"),
          getOrEmpty(hash, key, "category")
        );
      })
      .sorted((a, b) -> b.generatedAt().compareTo(a.generatedAt()))
      .toList();
    
    return ResponseEntity.ok(list);
  }

  @PatchMapping("/{emailId}")
  public ResponseEntity<Void> updateDraft(
      @PathVariable String emailId,
      @RequestBody UpdateDraftRequest body) {
    if (body == null || (body.draftText() == null && body.draftSubject() == null)) {
      return ResponseEntity.badRequest().build();
    }
    String draftKey = RedisKey.DRAFT.key(emailId);
    var hash = redisTemplate.opsForHash();
    if (!Boolean.TRUE.equals(hash.hasKey(draftKey, "draftText"))) {
      return ResponseEntity.notFound().build();
    }
    if (body.draftText() != null) {
      hash.put(draftKey, "draftText", body.draftText());
    }
    if (body.draftSubject() != null) {
      hash.put(draftKey, "draftSubject", body.draftSubject());
    }
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{emailId}/reject")
  public ResponseEntity<Void> reject(@PathVariable String emailId) {
    String draftKey = RedisKey.DRAFT.key(emailId);
    var hash = redisTemplate.opsForHash();
    if (!Boolean.TRUE.equals(hash.hasKey(draftKey, "draftText"))) {
      return ResponseEntity.notFound().build();
    }
    hash.put(draftKey, "status", DraftStatus.REJECTED.getValue());
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{emailId}/skip")
  public ResponseEntity<Void> skip(@PathVariable String emailId) {
    String draftKey = RedisKey.DRAFT.key(emailId);
    var hash = redisTemplate.opsForHash();
    if (!Boolean.TRUE.equals(hash.hasKey(draftKey, "draftText"))) {
      return ResponseEntity.notFound().build();
    }
    hash.put(draftKey, "status", DraftStatus.SKIPPED.getValue());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{emailId}")
  public ResponseEntity<DraftDetailDto> get(@PathVariable String emailId) {
    String draftKey = RedisKey.DRAFT.key(emailId);
    String emailKey = RedisKey.EMAIL.key(emailId);
    var hash = redisTemplate.opsForHash();
    if (!Boolean.TRUE.equals(hash.hasKey(draftKey, "draftText"))) {
      return ResponseEntity.notFound().build();
    }
    String from = (String) hash.get(emailKey, "from");
    String subject = (String) hash.get(emailKey, "subject");
    String body = (String) hash.get(emailKey, "body");
    String date = (String) hash.get(emailKey, "date");
    String draftText = (String) hash.get(draftKey, "draftText");
    String draftSubject = (String) hash.get(draftKey, "draftSubject");
    String status = (String) hash.get(draftKey, "status");
    String generatedAt = (String) hash.get(draftKey, "generatedAt");
    String category = (String) hash.get(draftKey, "category");

    DraftDetailDto.EmailPart email = new DraftDetailDto.EmailPart(
        emailId,
        from != null ? from : "",
        subject != null ? subject : "",
        body != null ? body : "",
        date != null ? date : ""
    );
    DraftDetailDto.DraftPart draft = new DraftDetailDto.DraftPart(
        draftText != null ? draftText : "",
        draftSubject != null ? draftSubject : "",
        status != null ? status : "",
        generatedAt != null ? generatedAt : "",
        category != null ? category : ""
    );
    return ResponseEntity.ok(new DraftDetailDto(email, draft));
  }

  @PostMapping("/{emailId}/send")
  public ResponseEntity<String> send(@PathVariable String emailId) {
    String draftKey = RedisKey.DRAFT.key(emailId);
    if (!Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(draftKey, "draftText"))) {
      return ResponseEntity.notFound().build();
    }
    try {
      gmailService.sendEmail(emailId);
      return ResponseEntity.ok("Email sent successfully");
    } catch (RuntimeException e) {
      return ResponseEntity.status(500).body(e.getMessage() != null ? e.getMessage() : "Failed to send email");
    }
  }

  private String getOrEmpty(HashOperations<String, Object, Object> hash, String key, String field) {
    Object value = hash.get(key, field);
    return value != null ? value.toString() : "";
  }
}
