package dev.brandonquon.messaging.kafka.service;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import dev.brandonquon.messaging.kafka.model.DraftStatus;
import dev.brandonquon.messaging.kafka.model.EmailCategory;
import dev.brandonquon.messaging.kafka.model.EmailDraft;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GeminiService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
  private static final String GEMINI_MODEL = "gemini-2.5-flash-lite";

  private final Client client;

  public GeminiService(@Value("${google.api.key:${GOOGLE_API_KEY:}}") String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
          "Gemini API key not set. Set google.api.key in application-local.yml or GOOGLE_API_KEY env var.");
    }
    this.client = Client.builder().apiKey(apiKey).build();
  }

  private static final String DRAFT_EMAIL_REPLY_JSON_INSTRUCTION =
      "URGENT INSTRUCTIONS: Respond with valid JSON only, and extremely important that it's without the ```json ``` tags. Do not include any other markdown text. Use exactly these keys:\n"
      + "  \"draftText\": string — the reply body (plain text, 2-3 short sentences)\n"
      + "  \"draftSubject\": string — subject line for the reply (e.g. Re: <original subject>), or Re: No Subject if the original subject is empty\n";

  private static final String DRAFT_EMAIL_CATEGORY_JSON_INSTRUCTION =
      "URGENT INSTRUCTIONS: Respond with valid JSON only, and extremely important that it's without the ```json ``` tags. Do not include any other markdown text. Use exactly these keys:\n"
      + "  \"category\": string — the category of the email from one of the following: 'newsletter', 'promotional', 'social', 'spam', 'scam', 'general', 'other', 'ignored'\n";

  private static String buildEmailCategoryPrompt(String emailSubject, String emailBody, String emailFrom, java.util.List<String> ignorePhrases) {
    String base = "Determine the category of the email based on the subject and body to the best of your ability.\n\n"
        + "Original email — From: " + emailFrom + "\nSubject: " + emailSubject + "\n\nBody:\n" + emailBody + "\n\n";
    if (ignorePhrases != null && !ignorePhrases.isEmpty()) {
      String list = String.join(", ", ignorePhrases);
      base += "The user wants to IGNORE these types of emails (do not generate drafts for them): " + list + ".\n"
          + "If this email matches any of these descriptions, respond with category 'ignored'.\n\n";
    }
    return base + DRAFT_EMAIL_CATEGORY_JSON_INSTRUCTION;
  }

  private static String buildEmailReplyPrompt(String emailSubject, String emailBody, String emailFrom, String category, String signoff) {
    return "Generate a small and brief professional reply to this email.\n\n"
        + "Original email — From: " + emailFrom + "\nSubject: " + emailSubject + "\n\nBody:\n" + emailBody + "\n\n"
        + DRAFT_EMAIL_REPLY_JSON_INSTRUCTION
        + "Category: " + category + "\n\n"
        + "Signoff: " + signoff + "\n\n";
  }


  /** Strip only the markdown fence lines (opening ```json/``` and closing ```); keep the JSON content in between. */
  private static String stripJsonCodeFence(String raw) {
    if (raw == null || raw.isBlank()) return raw;
    String s = raw.trim();
    if (!s.startsWith("```")) return s;
    // Drop opening fence line (e.g. ```json or ```)
    int firstNewline = s.indexOf('\n');
    if (firstNewline == -1) return s;
    s = s.substring(firstNewline + 1);
    // Drop closing fence only when on its own line (\n```) or at end (so we don't cut JSON that contains ```)
    int end = s.lastIndexOf("\n```");
    if (end != -1) s = s.substring(0, end).trim();
    else if (s.endsWith("```")) s = s.substring(0, s.length() - 3).trim();
    return s;
  }

  public EmailDraft generateResponse(String emailId, String emailSubject, String emailBody, String emailFrom, String category, String signoff) {
    try {
      GenerateContentResponse response =
          client.models.generateContent(GEMINI_MODEL, buildEmailReplyPrompt(emailSubject, emailBody, emailFrom, category, signoff), null);
      String raw = response.text();
      log.info("GeminiService: Generated response for emailId={}: {}", emailId, raw);

      if (raw == null || raw.isBlank()) return null;

      JsonNode node = OBJECT_MAPPER.readTree(stripJsonCodeFence(raw));
      String draftText = node.has("draftText") ? node.get("draftText").asText("") : "";
      String draftSubject = node.has("draftSubject") ? node.get("draftSubject").asText("") : "";

      return new EmailDraft(emailId, draftText, draftSubject, DraftStatus.PENDING, LocalDateTime.now());
    } catch (Exception e) {
      log.error("GeminiService: Error generating response for emailId={}: {}", emailId, e.getMessage());
      return null;
    }
  }

  public EmailCategory generateEmailCategory(String emailSubject, String emailBody, String emailFrom, java.util.List<String> ignorePhrases) {
    try {
      GenerateContentResponse response =
          client.models.generateContent(GEMINI_MODEL, buildEmailCategoryPrompt(emailSubject, emailBody, emailFrom, ignorePhrases), null);
      String raw = response.text();
      log.info("GeminiService: Generated email category for emailSubject={}: {}", emailSubject, raw);

      if (raw == null || raw.isBlank()) return EmailCategory.FAILED;

      JsonNode node = OBJECT_MAPPER.readTree(stripJsonCodeFence(raw));
      String category = node.has("category") ? node.get("category").asText("") : "";
      return EmailCategory.fromValue(category);
    } catch (Exception e) {
      log.error("GeminiService: Error generating email category for emailSubject={}: {}", emailSubject, e.getMessage());
      return EmailCategory.FAILED;
    }
  }
}
