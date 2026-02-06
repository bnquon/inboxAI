package dev.brandonquon.messaging.kafka.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import dev.brandonquon.messaging.kafka.config.KafkaConfig;
import dev.brandonquon.messaging.kafka.model.EmailCategory;
import dev.brandonquon.messaging.kafka.model.EmailStatus;
import dev.brandonquon.messaging.kafka.model.RedisKey;
import dev.brandonquon.messaging.kafka.service.GeminiService;
import dev.brandonquon.messaging.kafka.service.PreferencesService;

@Component
public class EmailCategorizationListener {

  private final RedisTemplate<String, String> redisTemplate;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final GeminiService geminiService;
  private final PreferencesService preferencesService;
  private static final Logger log = LoggerFactory.getLogger(EmailCategorizationListener.class);

  public EmailCategorizationListener(RedisTemplate<String, String> redisTemplate, KafkaTemplate<String, String> kafkaTemplate, GeminiService geminiService, PreferencesService preferencesService) {
    this.redisTemplate = redisTemplate;
    this.kafkaTemplate = kafkaTemplate;
    this.geminiService = geminiService;
    this.preferencesService = preferencesService;
  }

  @KafkaListener(topics = KafkaConfig.EMAIL_CATEGORIZATION_TOPIC, containerFactory = "stringListenerContainerFactory")
  public void handle(ConsumerRecord<String, String> record) {
    if (record.value() == null || record.value().isBlank()) {
      log.warn("EmailCategorizationListener: Received empty emailId from Kafka");
      return;
    }
    String emailId = record.value();

    if (Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(RedisKey.EMAIL.key(emailId), "id"))) {
      String emailSubject = (String) redisTemplate.opsForHash().get(RedisKey.EMAIL.key(emailId), "subject");
      String emailBody = (String) redisTemplate.opsForHash().get(RedisKey.EMAIL.key(emailId), "body");
      String emailFrom = (String) redisTemplate.opsForHash().get(RedisKey.EMAIL.key(emailId), "from");

      java.util.List<String> ignorePhrases = preferencesService.getIgnorePhrases();
      EmailCategory category = geminiService.generateEmailCategory(emailSubject, emailBody, emailFrom, ignorePhrases);

      redisTemplate.opsForHash().put(RedisKey.EMAIL.key(emailId), "category", category.getValue());

      if (category == EmailCategory.FAILED) {
        log.warn("EmailCategorizationListener: Failed to generate email category for emailId={}", emailId);
        redisTemplate.opsForHash().put(RedisKey.EMAIL.key(emailId), "status", EmailStatus.CATEGORIZATION_FAILED.getValue());
        return;
      }

      if (category == EmailCategory.NEWSLETTER || category == EmailCategory.PROMOTIONAL) {
        log.info("EmailCategorizationListener: Skipping emailId={} because it is a newsletter or promotional email", emailId);
        redisTemplate.opsForHash().put(RedisKey.EMAIL.key(emailId), "status", EmailStatus.SUBSCRIPTION_MAIL.getValue());
        return;
      }

      if (category == EmailCategory.SCAM) {
        log.info("EmailCategorizationListener: Skipping emailId={} because it is scam", emailId);
        redisTemplate.opsForHash().put(RedisKey.EMAIL.key(emailId), "status", EmailStatus.SCAM_DETECTED.getValue());
        return;
      }

      if (category == EmailCategory.SPAM) {
        log.info("EmailCategorizationListener: Skipping emailId={} because it is likely spam", emailId);
        redisTemplate.opsForHash().put(RedisKey.EMAIL.key(emailId), "status", EmailStatus.LIKELY_SPAM.getValue());
        return;
      }

      if (category == EmailCategory.IGNORED) {
        log.info("EmailCategorizationListener: Skipping emailId={} because it matches user ignore preferences", emailId);
        redisTemplate.opsForHash().put(RedisKey.EMAIL.key(emailId), "status", EmailStatus.IGNORED.getValue());
        return;
      }

      log.info("EmailCategorizationListener: Sending emailId={} to draft generation", emailId);
      redisTemplate.opsForHash().put(RedisKey.EMAIL.key(emailId), "status", EmailStatus.SENT_TO_DRAFT_GENERATION.getValue());
      kafkaTemplate.send(KafkaConfig.DRAFT_GENERATION_TOPIC, emailId);
    } else {
      log.warn("EmailCategorizationListener: EmailId={} not found in Redis", emailId);
    }
  }
}
