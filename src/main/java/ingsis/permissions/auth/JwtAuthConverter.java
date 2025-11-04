package ingsis.permissions.auth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;

public class JwtAuthConverter extends JwtAuthenticationConverter {

  public JwtAuthConverter() {
    JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
    scopeConverter.setAuthorityPrefix("SCOPE_");
    scopeConverter.setAuthoritiesClaimName("scope"); // fallback

    setJwtGrantedAuthoritiesConverter(
        (Converter<Jwt, Collection<GrantedAuthority>>)
            jwt -> {
              Object perms = jwt.getClaims().get("permissions");
              if (perms instanceof Collection<?> c && !c.isEmpty()) {
                return c.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(p -> (GrantedAuthority) () -> "SCOPE_" + p)
                    .toList();
              }
              return scopeConverter.convert(jwt);
            });
  }
}
