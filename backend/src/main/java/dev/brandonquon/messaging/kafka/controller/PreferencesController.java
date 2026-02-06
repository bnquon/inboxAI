package dev.brandonquon.messaging.kafka.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.brandonquon.messaging.kafka.service.PreferencesService;

@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

  private final PreferencesService preferencesService;

  public PreferencesController(PreferencesService preferencesService) {
    this.preferencesService = preferencesService;
  }

  @GetMapping("/ignores")
  public ResponseEntity<List<String>> getIgnores() {
    return ResponseEntity.ok(preferencesService.getIgnorePhrases());
  }

  @PutMapping("/ignores")
  public ResponseEntity<Void> setIgnores(@RequestBody List<String> phrases) {
    preferencesService.setIgnorePhrases(phrases);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/signoff")
  public ResponseEntity<String> signoff() {
    return ResponseEntity.ok(preferencesService.getSignoff());
  }

  @PutMapping("/signoff")
  public ResponseEntity<Void> setSignoff(@RequestBody String signoff) {
    preferencesService.setSignoff(signoff);
    return ResponseEntity.noContent().build();
  }
}
