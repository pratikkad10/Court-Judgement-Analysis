package com.judgment.document.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Only accepts requests carrying the private mesh service key. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain chain(HttpSecurity h, InternalKeyFilter f) throws Exception {
    return h.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a -> a.requestMatchers("/actuator/health").permitAll().anyRequest().permitAll())
        .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}

@Component
class InternalKeyFilter extends OncePerRequestFilter {
  private final String key;

  InternalKeyFilter(@Value("${app.internal-key}") String key) {
    if (key == null || key.isBlank())
      throw new IllegalStateException("INTERNAL_SERVICE_KEY is required");
    this.key = key;
  }

  protected boolean shouldNotFilter(HttpServletRequest r) {
    return r.getRequestURI().equals("/actuator/health");
  }

  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c)
      throws IOException, ServletException {
    if (!key.equals(r.getHeader("X-Internal-Key"))) {
      s.setStatus(HttpStatus.UNAUTHORIZED.value());
      s.setContentType("application/json");
      s.getWriter()
          .write(
              "{\"success\":false,\"data\":null,\"error\":{\"code\":\"INTERNAL_KEY_REQUIRED\",\"message\":\"Service"
                  + " authentication is required.\"}}");
      return;
    }
    c.doFilter(r, s);
  }
}
