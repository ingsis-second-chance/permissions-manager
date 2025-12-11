package ingsis.permissions.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ingsis.permissions.DTO.UserDTO;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

  private final Logger logger = LoggerFactory.getLogger(UserService.class);

  private final JwtDecoder jwtDecoder;
  private final String audience;
  private String token;

  private final RestTemplate restTemplate;

  public UserService(
      JwtDecoder jwtDecoder,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String audience) {
    this.jwtDecoder = jwtDecoder;
    this.audience = audience;
    this.restTemplate = new RestTemplate();
  }

  public List<UserDTO> getAllUsers() {
    logger.info("Getting all users.");
    validateAndRefreshTokenIfNeeded();
    String url = audience + "/api/v2/users";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

    try {
      ResponseEntity<UserDTO[]> response =
          restTemplate.exchange(url, HttpMethod.GET, requestEntity, UserDTO[].class);
      logger.info("Request to get all users completed.");
      return Arrays.asList(response.getBody());
    } catch (Exception e) {
      logger.error("Error retrieving users: {}", e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving users");
    }
  }

  public String getUsernameFromUserId(String userId) {
    logger.info("Getting username for user ID: {}", userId);
    validateAndRefreshTokenIfNeeded();
    String url = audience + "/api/v2/users/" + userId;

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
      JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
      return jsonNode.get("username").asText();
    } catch (Exception e) {
      logger.error("Error fetching username for user ID: {}", userId, e);
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Username not found for user ID: " + userId, e);
    }
  }

  private void validateAndRefreshTokenIfNeeded() {
    if (token == null || !isTokenValid(token)) {
      logger.info("Token is invalid or missing. Requesting a new token.");
      fetchToken();
    } else {
      logger.info("Using existing valid token.");
    }
  }

  private void fetchToken() {
    String url = audience + "/oauth/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    String body =
        "grant_type=client_credentials"
            + "&client_id="
            + System.getenv("AUTH0_CLIENT_ID")
            + "&client_secret="
            + System.getenv("AUTH0_CLIENT_SECRET")
            + "&audience="
            + System.getenv("AUTH0_AUDIENCE_API");

    HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
      JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
      token = jsonNode.get("access_token").asText();
      logger.info("New token acquired successfully.");
    } catch (Exception e) {
      logger.error("Error fetching token: {}", e.getMessage(), e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching token", e);
    }
  }

  private boolean isTokenValid(String token) {
    try {
      Jwt decodedJwt = jwtDecoder.decode(token);
      boolean isValid = decodedJwt.getExpiresAt().isAfter(Instant.now());
      if (isValid) {
        logger.info("Token is valid.");
      } else {
        logger.warn("Token has expired.");
      }
      return isValid;
    } catch (Exception e) {
      logger.error("Error decoding token: {}", e.getMessage(), e);
      return false;
    }
  }
}
