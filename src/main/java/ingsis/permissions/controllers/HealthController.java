package ingsis.permissions.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping // (sin prefijo)
public class HealthController {
  @GetMapping("/ping")
  public String ping() {
    return "pong 🟢";
  }
}
