package dev.brandonquon.messaging.kafka.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.brandonquon.messaging.kafka.config.KafkaConfig;
import dev.brandonquon.messaging.kafka.model.DraftStatus;
import dev.brandonquon.messaging.kafka.model.RedisKey;

@Service
public class GmailService {

  private static final Logger log = LoggerFactory.getLogger(GmailService.class);
  private static final String REDIS_OAUTH_KEY = "oauth:default";
  private final StringRedisTemplate redisTemplate;
  private final KafkaTemplate<String, String> kafkaTemplate;

  public GmailService(StringRedisTemplate redisTemplate, KafkaTemplate<String, String> kafkaTemplate) {
    this.redisTemplate = redisTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }

  @SuppressWarnings("deprecation")
  private static Credential getCredentials(String accessToken) {
    return new GoogleCredential().setAccessToken(accessToken);
  }

  private static String getHeader(Message message, String name) {
    if (message.getPayload() == null || message.getPayload().getHeaders() == null) return "";
    for (MessagePartHeader h : message.getPayload().getHeaders()) {
      if (name.equalsIgnoreCase(h.getName())) return h.getValue() != null ? h.getValue() : "";
    }
    return "";
  }

  private static String getBody(Message full) {
    if (full.getPayload() == null) return "";
    if (full.getPayload().getBody() != null && full.getPayload().getBody().getData() != null) {
      return decodeBase64Url(full.getPayload().getBody().getData());
    }
    List<MessagePart> parts = full.getPayload().getParts();
    if (parts == null || parts.isEmpty()) return "";
    for (String mime : new String[] { "text/plain", "text/html" }) {
      for (MessagePart part : parts) {
        if (mime.equalsIgnoreCase(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
          return decodeBase64Url(part.getBody().getData());
        }
      }
    }
    return "";
  }

  private static String decodeBase64Url(String data) {
    if (data == null || data.isEmpty()) return "";
    try {
      byte[] bytes = Base64.getUrlDecoder().decode(data);
      return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : "";
    } catch (IllegalArgumentException e) {
      return data;
    }
  }

  /**
   * Extracts the email address from a header value like "Name &lt;user@example.com&gt;" or "user@example.com".
   * Used for reply-to when sending.
   */
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

  @Scheduled(fixedRate = 3600000)
  public void fetchNewEmails() {
    String accessToken = (String) redisTemplate.opsForHash().get(REDIS_OAUTH_KEY, "access_token");
    if (accessToken == null || accessToken.isBlank()) {
      log.debug("GmailService: No access token in Redis (hash {}, field access_token)", REDIS_OAUTH_KEY);
      return;
    }

    Gmail service = new Gmail.Builder(
        new NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        getCredentials(accessToken))
        .setApplicationName("Emailbot")
        .build();

    ListMessagesResponse response;
    try {
      response = service.users().messages().list("me").setMaxResults(3L).execute();
    } catch (IOException e) {
      log.warn("GmailService: Error listing messages: {}", e.getMessage());
      return;
    }
    if (response == null || response.getMessages() == null) return;

    var hash = redisTemplate.opsForHash();
    for (Message message : response.getMessages()) {
      String id = message.getId();
      if (Boolean.TRUE.equals(hash.hasKey(RedisKey.EMAIL.key(id), "id"))) continue;

      Message full;
      try {
        full = service.users().messages().get("me", id).execute();
      } catch (IOException e) {
        log.warn("GmailService: Error fetching message {}: {}", id, e.getMessage());
        continue;
      }
      String threadId = full.getThreadId();
      String from = getHeader(full, "From");
      String replyToHeader = getHeader(full, "Reply-To");
      String subject = getHeader(full, "Subject");
      String dateStr = full.getInternalDate() != null ? String.valueOf(full.getInternalDate()) : "";
      String snippet = full.getSnippet() != null ? full.getSnippet() : "";
      String body = getBody(full);

      // Who to send the reply to: Reply-To if present, else extract address from From
      String replyToEmail = replyToHeader != null && !replyToHeader.isBlank()
          ? extractEmailAddress(replyToHeader)
          : extractEmailAddress(from);

      hash.put(RedisKey.EMAIL.key(id), "id", id);
      hash.put(RedisKey.EMAIL.key(id), "threadId", threadId != null ? threadId : "");
      hash.put(RedisKey.EMAIL.key(id), "from", from);
      hash.put(RedisKey.EMAIL.key(id), "replyToEmail", replyToEmail != null ? replyToEmail : "");
      hash.put(RedisKey.EMAIL.key(id), "subject", subject);
      hash.put(RedisKey.EMAIL.key(id), "date", dateStr);
      hash.put(RedisKey.EMAIL.key(id), "snippet", snippet);
      hash.put(RedisKey.EMAIL.key(id), "body", body);

      try {
        kafkaTemplate.send(KafkaConfig.INCOMING_EMAILS_TOPIC, id, id).get();
        log.info("GmailService: Sent email id={} to Kafka topic {}", id, KafkaConfig.INCOMING_EMAILS_TOPIC);
      } catch (Exception e) {
        log.error("GmailService: Failed to send email id={} to Kafka topic {}", id, KafkaConfig.INCOMING_EMAILS_TOPIC, e);
      }
    }
  }
  public void sendEmail(String emailId) {
    String accessToken = (String) redisTemplate.opsForHash().get(REDIS_OAUTH_KEY, "access_token");
    if (accessToken == null || accessToken.isBlank()) {
      log.debug("GmailService: No access token in Redis (hash {}, field access_token)", REDIS_OAUTH_KEY);
      return;
    }

    try {
      Gmail service = new Gmail.Builder(
          new NetHttpTransport(),
          GsonFactory.getDefaultInstance(),
          getCredentials(accessToken))
          .setApplicationName("Emailbot")
          .build();

      String draftSubject = (String) redisTemplate.opsForHash().get(RedisKey.DRAFT.key(emailId), "draftSubject");
      String draftBody = (String) redisTemplate.opsForHash().get(RedisKey.DRAFT.key(emailId), "draftText");
      String replyToEmail = (String) redisTemplate.opsForHash().get(RedisKey.EMAIL.key(emailId), "replyToEmail");

      Properties properties = new Properties();
      Session session = Session.getDefaultInstance(properties, null);
      MimeMessage email = new MimeMessage(session);

      email.setFrom(new InternetAddress("bnquon@gmail.com"));
      email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(replyToEmail));
      email.setSubject(draftSubject != null ? draftSubject : "");
      email.setText(draftBody != null ? draftBody : "");

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      email.writeTo(buffer);
      byte[] rawMessageBytes = buffer.toByteArray();
      String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(rawMessageBytes);
      Message gmailMessage = new Message();
      gmailMessage.setRaw(encodedEmail);

      service.users().messages().send("me", gmailMessage).execute();
      log.info("GmailService: Sent email for draft emailId={}", emailId);
      redisTemplate.opsForHash().put(RedisKey.DRAFT.key(emailId), "status", DraftStatus.ACCEPTED.getValue());
    } catch (Exception e) {
      log.error("GmailService: Failed to send email for draft emailId={}: {}", emailId, e.getMessage(), e);
      throw new RuntimeException("Failed to send email", e);
    }
  }
}
