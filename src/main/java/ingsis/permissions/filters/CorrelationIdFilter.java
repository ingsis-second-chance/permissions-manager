package ingsis.permissions.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;

public class CorrelationIdFilter implements Filter {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(CorrelationIdFilter.class);
  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);

    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = UUID.randomUUID().toString();
    }

    MDC.put("correlationId", correlationId);

    logger.info("Received request and assigned correlation id");

    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove("correlationId");
    }
  }
}
