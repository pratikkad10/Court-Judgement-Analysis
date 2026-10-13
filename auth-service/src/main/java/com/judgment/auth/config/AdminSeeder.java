package com.judgment.auth.config;

import com.judgment.auth.entity.*;
import com.judgment.auth.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the configured administrator once so bootstrap is explicit and repeatable. */
@Component
public class AdminSeeder {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final String email, password;

  public AdminSeeder(
      UserRepository users,
      PasswordEncoder encoder,
      @Value("${app.admin-email}") String email,
      @Value("${app.admin-password}") String password) {
    this.users = users;
    this.encoder = encoder;
    this.email = email;
    this.password = password;
  }

  @PostConstruct
  void seed() {
    if (email != null
        && !email.isBlank()
        && password != null
        && password.length() >= 12
        && users.findByEmailIgnoreCase(email).isEmpty())
      users.save(new User(email.trim().toLowerCase(), encoder.encode(password), Role.ADMIN));
  }
}
