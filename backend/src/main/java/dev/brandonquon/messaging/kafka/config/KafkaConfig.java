package dev.brandonquon.messaging.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

  public static final String INCOMING_EMAILS_TOPIC = "incoming-emails";
  public static final String EMAIL_CATEGORIZATION_TOPIC = "email-categorization";
  public static final String DRAFT_GENERATION_TOPIC = "draft-generation";

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Bean
  public NewTopic incomingEmailsTopic() {
    return new NewTopic(INCOMING_EMAILS_TOPIC, 1, (short) 1);
  }

  @Bean
  public NewTopic emailCategorizationTopic() {
    return new NewTopic(EMAIL_CATEGORIZATION_TOPIC, 1, (short) 1);
  }

  @Bean
  public NewTopic draftGenerationTopic() {
    return new NewTopic(DRAFT_GENERATION_TOPIC, 1, (short) 1);
  }

  /** String-key/String-value producer for Gmail → Kafka (incoming-emails topic). */
  @Bean
  public ProducerFactory<String, String> stringProducerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, String> stringKafkaTemplate() {
    return new KafkaTemplate<>(stringProducerFactory());
  }

  @Bean
  public ConsumerFactory<String, String> stringConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "incoming-emails-group");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> stringListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(stringConsumerFactory());
    return factory;
  }
}
