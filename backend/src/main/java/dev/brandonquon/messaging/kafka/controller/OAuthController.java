package dev.brandonquon.messaging.kafka.controller;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@RestController
@RequestMapping("/oauth2")
public class OAuthController {

  private static final Logger log = LoggerFactory.getLogger(OAuthController.class);
  private static final String AUTH_BASE = "https://accounts.google.com/o/oauth2/v2/auth";
  private static final String REDIS_OAUTH_KEY = "oauth:default";
  private static final String[] GMAIL_SCOPES = {
      "https://www.googleapis.com/auth/gmail.readonly",
      "https://www.googleapis.com/auth/gmail.send",
      "https://www.googleapis.com/auth/gmail.modify"
  };

  private final StringRedisTemplate redisTemplate;

  @Value("${google.client.id:}")
  private String clientId;

  @Value("${google.redirect.uri:}")
  private String redirectUri;

  @Value("${google.client.secret:}")
  private String clientSecret;

  @Value("${app.frontend-url:http://localhost:5173}")
  private String frontendUrl;

  public OAuthController(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Returns 200 if the user has valid OAuth tokens in Redis, 401 otherwise.
   * Frontend uses this to decide whether to show the app or the login screen.
   */
  /**
   * Clears OAuth tokens from Redis. After calling this, /oauth2/status will return 401.
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    Boolean removed = redisTemplate.delete(REDIS_OAUTH_KEY);
    log.info("OAuthController: logout, Redis key {} removed={}", REDIS_OAUTH_KEY, removed);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/status")
  public ResponseEntity<Void> status() {
    String accessToken = (String) redisTemplate.opsForHash().get(REDIS_OAUTH_KEY, "access_token");
    if (accessToken == null || accessToken.isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok().build();
  }

  @GetMapping("/authorize")
  public ResponseEntity<Map<String, String>> authorize() {
    if (clientId == null || clientId.isBlank() || redirectUri == null || redirectUri.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "OAuth not configured (google.client.id, google.redirect.uri)"));
    }
    String scope = String.join(" ", GMAIL_SCOPES);
    String authUrl = UriComponentsBuilder.fromUriString(AUTH_BASE)
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirectUri)
        .queryParam("response_type", "code")
        .queryParam("scope", scope)
        .queryParam("access_type", "offline")
        .queryParam("prompt", "consent")
        .build()
        .toUriString();
    return ResponseEntity.ok(Map.of("authUrl", authUrl));
  }

  @GetMapping("/callback")
  public ResponseEntity<Void> callback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state) {
    if (code == null || code.isBlank()) {
      log.warn("OAuthController: OAuth callback missing or empty code");
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(frontendUrl + "?error=missing_code")).build();
    }
    if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()
        || redirectUri == null || redirectUri.isBlank()) {
      log.error("OAuthController: OAuth config incomplete (google.client.id, google.client.secret, google.redirect.uri)");
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(frontendUrl + "?error=config_incomplete")).build();
    }

    try {
      GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
          new NetHttpTransport(),
          GsonFactory.getDefaultInstance(),
          clientId,
          clientSecret,
          code,
          redirectUri
      ).execute();

      String accessToken = response.getAccessToken();
      String refreshToken = response.getRefreshToken();
      Long expiresIn = response.getExpiresInSeconds();
      long expiresAt = (expiresIn != null) ? System.currentTimeMillis() / 1000 + expiresIn : 0L;

      var hash = redisTemplate.opsForHash();
      hash.put(REDIS_OAUTH_KEY, "access_token", accessToken);
      hash.put(REDIS_OAUTH_KEY, "refresh_token", refreshToken != null ? refreshToken : "");
      hash.put(REDIS_OAUTH_KEY, "expires_at", String.valueOf(expiresAt));
      hash.put(REDIS_OAUTH_KEY, "scope", String.join(" ", GMAIL_SCOPES));
      hash.put(REDIS_OAUTH_KEY, "token_type", response.getTokenType() != null ? response.getTokenType() : "Bearer");

      log.info("OAuthController: OAuth tokens saved to Redis for key {}", REDIS_OAUTH_KEY);
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(frontendUrl)).build();
    } catch (IOException e) {
      log.error("OAuthController: OAuth token exchange failed: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(frontendUrl + "?error=exchange_failed")).build();
    } catch (Exception e) {
      log.error("OAuthController: OAuth callback error: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(frontendUrl + "?error=unexpected")).build();
    }
  }
}
