package dev.brandonquon.messaging.kafka.listener;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dev.brandonquon.messaging.kafka.config.KafkaConfig;
import dev.brandonquon.messaging.kafka.model.EmailDraft;
import dev.brandonquon.messaging.kafka.model.EmailStatus;
import dev.brandonquon.messaging.kafka.model.RedisKey;
import dev.brandonquon.messaging.kafka.service.GeminiService;
import dev.brandonquon.messaging.kafka.service.PreferencesService;

@Component
public class DraftGenerationListener {
  private final RedisTemplate<String, String> redisTemplate;
  private final GeminiService geminiService;
  private final PreferencesService preferencesService;
  private static final Logger log = LoggerFactory.getLogger(DraftGenerationListener.class);

  public DraftGenerationListener(RedisTemplate<String, String> redisTemplate, GeminiService geminiService, PreferencesService preferencesService) {
    this.redisTemplate = redisTemplate;
    this.geminiService = geminiService;
    this.preferencesService = preferencesService;
  }

  @KafkaListener(topics = KafkaConfig.DRAFT_GENERATION_TOPIC, containerFactory = "stringListenerContainerFactory")
  public void handle(ConsumerRecord<String, String> record) {
    if (record.value() == null || record.value().isBlank()) {
      log.warn("DraftGenerationListener: Received empty emailId from Kafka");
      return;
    }
    String emailId = record.value();

    if (Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(RedisKey.DRAFT.key(emailId), "id"))) {
      log.info("DraftGenerationListener: Draft for emailId={} already generated", emailId);
      return;
    }

    log.info("DraftGenerationListener: Generating draft for emailId={}", emailId);

    String emailSubject = (String) redisTemplate.opsForHash().get(RedisKey.EMAIL.key(emailId), "subject");
    String emailBody = (String) redisTemplate.opsForHash().get(RedisKey.EMAIL.key(emailId), "body");
    String emailFrom = (String) redisTemplate.opsForHash().get(RedisKey.EMAIL.key(emailId), "from");
    String category = (String) redisTemplate.opsForHash().get(RedisKey.EMAIL.key(emailId), "category");
    String signoff = preferencesService.getSignoff();
    EmailDraft draft = geminiService.generateResponse(emailId, emailSubject, emailBody, emailFrom, category, signoff);

    if (draft == null) {
      log.warn("DraftGenerationListener: Failed to generate draft for emailId={}", emailId);
      redisTemplate.opsForHash().put(RedisKey.EMAIL.key(emailId), "status", EmailStatus.DRAFT_GENERATION_FAILED.getValue());
      return;
    }

    redisTemplate.opsForHash().put(RedisKey.DRAFT.key(emailId), "draftText", draft.getDraftText());
    redisTemplate.opsForHash().put(RedisKey.DRAFT.key(emailId), "replyToEmailId", emailId);
    redisTemplate.opsForHash().put(RedisKey.DRAFT.key(emailId), "draftSubject", draft.getDraftSubject());
    redisTemplate.opsForHash().put(RedisKey.DRAFT.key(emailId), "category", category);
    redisTemplate.opsForHash().put(RedisKey.DRAFT.key(emailId), "status", draft.getStatus().getValue());
    redisTemplate.opsForHash().put(RedisKey.DRAFT.key(emailId), "generatedAt", draft.getGeneratedAt().toString());

    log.info("DraftGenerationListener: Generated draft for emailId={}", emailId);
  }
}
