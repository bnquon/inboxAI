package dev.brandonquon.messaging.kafka.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import dev.brandonquon.messaging.kafka.config.KafkaConfig;
import dev.brandonquon.messaging.kafka.model.RedisKey;

@Component
public class IncomingEmailListener {

  private final RedisTemplate<String, String> redisTemplate;
  private static final Logger log = LoggerFactory.getLogger(IncomingEmailListener.class);
  private final KafkaTemplate<String, String> kafkaTemplate;

  public IncomingEmailListener(RedisTemplate<String, String> redisTemplate, KafkaTemplate<String, String> kafkaTemplate) {
    this.redisTemplate = redisTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }

  @KafkaListener(
      topics = KafkaConfig.INCOMING_EMAILS_TOPIC,
      containerFactory = "stringListenerContainerFactory")
  public void handle(ConsumerRecord<String, String> record) {
    if (record.value() == null || record.value().isBlank()) {
      log.warn("IncomingEmailListener: Received empty emailId from Kafka");
      return;
    }
    String emailId = record.value();

    if (Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(RedisKey.EMAIL.key(emailId), "id"))) {
      log.info("IncomingEmailListener: Sending emailId={} to email categorization", emailId);
      kafkaTemplate.send(KafkaConfig.EMAIL_CATEGORIZATION_TOPIC, emailId);
    } else {
      log.warn("IncomingEmailListener: EmailId={} not found in Redis", emailId);
    }
  }
}
