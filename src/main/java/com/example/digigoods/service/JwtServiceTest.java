package com.example.digigoods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(jwtService, "secret",
        "myVerySecretKeyThatIsAtLeast256BitsLongForHS256Algorithm");
    ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
  }

  @Test
  @DisplayName("Given valid user data, when generating token, then return valid JWT token")
  void givenValidUserData_whenGeneratingToken_thenReturnValidJwtToken() {
    // Arrange
    Long userId = 1L;
    String username = "testuser";

    // Act
    String token = jwtService.generateToken(userId, username);

    // Assert
    assertNotNull(token);
    assertTrue(token.length() > 0);
  }

  @Test
  @DisplayName("Given valid token, when extracting username, then return correct username")
  void givenValidToken_whenExtractingUsername_thenReturnCorrectUsername() {
    // Arrange
    Long userId = 1L;
    String username = "testuser";
    String token = jwtService.generateToken(userId, username);

    // Act
    String extractedUsername = jwtService.extractUsername(token);

    // Assert
    assertEquals(username, extractedUsername);
  }

  @Test
  @DisplayName("Given valid token, when extracting user ID, then return correct user ID")
  void givenValidToken_whenExtractingUserId_thenReturnCorrectUserId() {
    // Arrange
    Long userId = 1L;
    String username = "testuser";
    String token = jwtService.generateToken(userId, username);

    // Act
    Long extractedUserId = jwtService.extractUserId(token);

    // Assert
    assertEquals(userId, extractedUserId);
  }

  @Test
  @DisplayName("Given valid token, when checking if expired, then return false")
  void givenValidToken_whenCheckingIfExpired_thenReturnFalse() {
    // Arrange
    Long userId = 1L;
    String username = "testuser";
    String token = jwtService.generateToken(userId, username);

    // Act
    boolean isExpired = jwtService.isTokenExpired(token);

    // Assert
    assertFalse(isExpired);
  }

  @Test
  @DisplayName("Given valid token and username, when validating token, then return true")
  void givenValidTokenAndUsername_whenValidatingToken_thenReturnTrue() {
    // Arrange
    Long userId = 1L;
    String username = "testuser";
    String token = jwtService.generateToken(userId, username);

    // Act
    boolean isValid = jwtService.validateToken(token, username);

    // Assert
    assertTrue(isValid);
  }

  @Test
  @DisplayName("Given valid token and wrong username, when validating token, then return false")
  void givenValidTokenAndWrongUsername_whenValidatingToken_thenReturnFalse() {
    // Arrange
    Long userId = 1L;
    String username = "testuser";
    String wrongUsername = "wronguser";
    String token = jwtService.generateToken(userId, username);

    // Act
    boolean isValid = jwtService.validateToken(token, wrongUsername);

    // Assert
    assertFalse(isValid);
  }
}
