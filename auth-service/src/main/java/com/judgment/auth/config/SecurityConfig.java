package com.judgment.auth.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Restricts direct service calls to the gateway or an internal service key. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http, InternalKeyFilter filter) throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a -> a.requestMatchers("/actuator/health").permitAll().anyRequest().permitAll())
        .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
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

  @Override
  protected boolean shouldNotFilter(HttpServletRequest r) {
    return r.getRequestURI().equals("/actuator/health");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    if (!key.equals(req.getHeader("X-Internal-Key"))) {
      res.setStatus(HttpStatus.UNAUTHORIZED.value());
      res.setContentType("application/json");
      res.getWriter()
          .write(
              "{\"success\":false,\"data\":null,\"error\":{\"code\":\"INTERNAL_KEY_REQUIRED\",\"message\":\"Service"
                  + " authentication is required.\"}}");
      return;
    }
    chain.doFilter(req, res);
  }
}
