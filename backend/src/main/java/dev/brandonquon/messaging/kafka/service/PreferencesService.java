package dev.brandonquon.messaging.kafka.service;

import java.util.Collections;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PreferencesService {

  private static final String IGNORES_KEY = "preferences:ignores";
  private static final String SIGNOFF_KEY = "preferences:signoff";
  private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {};
  private static final TypeReference<String> STRING = new TypeReference<>() {};

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public PreferencesService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public List<String> getIgnorePhrases() {
    String json = redisTemplate.opsForValue().get(IGNORES_KEY);
    if (json == null || json.isBlank()) return Collections.emptyList();
    try {
      return objectMapper.readValue(json, LIST_OF_STRING);
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  public void setIgnorePhrases(List<String> phrases) {
    if (phrases == null) {
      redisTemplate.delete(IGNORES_KEY);
      return;
    }
    try {
      String json = objectMapper.writeValueAsString(phrases);
      redisTemplate.opsForValue().set(IGNORES_KEY, json);
    } catch (Exception e) {
      throw new RuntimeException("Failed to save ignore phrases", e);
    }
  }

  public String getSignoff() {
    String json = redisTemplate.opsForValue().get(SIGNOFF_KEY);
    if (json == null || json.isBlank()) return "";
    try {
      return objectMapper.readValue(json, STRING);
    } catch (Exception e) {
      return "";
    }
  }

  public void setSignoff(String signoff) {
    if (signoff == null || signoff.isBlank()) {
      redisTemplate.delete(SIGNOFF_KEY);
      return;
    }
    try {
      String json = objectMapper.writeValueAsString(signoff);
      redisTemplate.opsForValue().set(SIGNOFF_KEY, json);
    } catch (Exception e) {
      throw new RuntimeException("Failed to save signoff", e);
    }
  }
}
