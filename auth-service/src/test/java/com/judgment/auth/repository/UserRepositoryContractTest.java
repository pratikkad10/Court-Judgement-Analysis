package com.judgment.auth.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.judgment.auth.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.datasource.url=jdbc:h2:mem:authdb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS"
          + " auth",
      "spring.jpa.properties.hibernate.default_schema=auth"
    })
class UserRepositoryContractTest {
  @Autowired UserRepository users;

  @Test
  void findsEmailIgnoringCase() {
    users.save(new User("person@example.com", "hash", Role.USER));
    assertTrue(users.findByEmailIgnoreCase("PERSON@example.com").isPresent());
  }
}
