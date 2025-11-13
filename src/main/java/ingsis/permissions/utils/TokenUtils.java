package ingsis.permissions.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Map;

public class TokenUtils {
  public static Map<String, String> decodeToken(String token) {
    DecodedJWT decodedJWT = JWT.decode(token);
    return Map.of(
        "userId", decodedJWT.getSubject(), "username", decodedJWT.getClaim("username").asString());
  }
}
