package com.example.digigoods.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.digigoods.dto.CheckoutRequest;
import com.example.digigoods.dto.OrderResponse;
import com.example.digigoods.exception.MissingJwtTokenException;
import com.example.digigoods.service.CheckoutService;
import com.example.digigoods.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

  @Mock
  private CheckoutService checkoutService;

  @Mock
  private JwtService jwtService;

  @Mock
  private HttpServletRequest httpServletRequest;

  @InjectMocks
  private CheckoutController checkoutController;

  private CheckoutRequest checkoutRequest;
  private OrderResponse orderResponse;

  @BeforeEach
  void setUp() {
    checkoutRequest = new CheckoutRequest();
    checkoutRequest.setUserId(1L);
    checkoutRequest.setProductIds(List.of(1L, 2L));
    checkoutRequest.setDiscountCodes(List.of("DISCOUNT10"));

    orderResponse = new OrderResponse("Order created successfully!", new BigDecimal("90.00"));
  }

  @Test
  @DisplayName("Given valid checkout request with valid JWT token, when creating order, "
      + "then return successful response")
  void givenValidCheckoutRequestWithValidJwtToken_whenCreatingOrder_thenReturnSuccessfulResponse() {
    // Arrange
    String token = "valid.jwt.token";
    String bearerToken = "Bearer " + token;
    Long authenticatedUserId = 1L;

    when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUserId(token)).thenReturn(authenticatedUserId);
    when(checkoutService.processCheckout(checkoutRequest, authenticatedUserId))
        .thenReturn(orderResponse);

    // Act
    ResponseEntity<OrderResponse> response = checkoutController.createOrder(
        checkoutRequest, httpServletRequest);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(orderResponse, response.getBody());
    assertEquals("Order created successfully!", response.getBody().getMessage());
    assertEquals(new BigDecimal("90.00"), response.getBody().getFinalPrice());
  }

  @Test
  @DisplayName("Given checkout request with missing Authorization header, when creating order, "
      + "then throw MissingJwtTokenException")
  void givenCheckoutRequestWithMissingAuthorizationHeader_whenCreatingOrder_thenThrowException() {
    // Arrange
    when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

    // Act & Assert
    MissingJwtTokenException exception = assertThrows(MissingJwtTokenException.class,
        () -> checkoutController.createOrder(checkoutRequest, httpServletRequest));

    assertEquals("JWT token is missing or invalid", exception.getMessage());
  }

  @Test
  @DisplayName("Given checkout request with invalid Authorization header format, "
      + "when creating order, then throw MissingJwtTokenException")
  void givenCheckoutRequestWithInvalidAuthHeader_whenCreatingOrder_thenThrowException() {
    // Arrange
    when(httpServletRequest.getHeader("Authorization")).thenReturn("InvalidFormat token");

    // Act & Assert
    MissingJwtTokenException exception = assertThrows(MissingJwtTokenException.class,
        () -> checkoutController.createOrder(checkoutRequest, httpServletRequest));

    assertEquals("JWT token is missing or invalid", exception.getMessage());
  }

  @Test
  @DisplayName("Given checkout request with empty Authorization header, when creating order, "
      + "then throw MissingJwtTokenException")
  void givenCheckoutRequestWithEmptyAuthorizationHeader_whenCreatingOrder_thenThrowException() {
    // Arrange
    when(httpServletRequest.getHeader("Authorization")).thenReturn("");

    // Act & Assert
    MissingJwtTokenException exception = assertThrows(MissingJwtTokenException.class,
        () -> checkoutController.createOrder(checkoutRequest, httpServletRequest));

    assertEquals("JWT token is missing or invalid", exception.getMessage());
  }

  @Test
  @DisplayName("Given checkout request with Bearer token without space, when creating order, "
      + "then throw MissingJwtTokenException")
  void givenCheckoutRequestWithBearerTokenWithoutSpace_whenCreatingOrder_thenThrowException() {
    // Arrange
    when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearertoken");

    // Act & Assert
    MissingJwtTokenException exception = assertThrows(MissingJwtTokenException.class,
        () -> checkoutController.createOrder(checkoutRequest, httpServletRequest));

    assertEquals("JWT token is missing or invalid", exception.getMessage());
  }

  @Test
  @DisplayName("Given checkout request with only Bearer prefix, when creating order, "
      + "then extract empty token and process")
  void givenCheckoutRequestWithOnlyBearerPrefix_whenCreatingOrder_thenExtractEmptyToken() {
    // Arrange
    String bearerToken = "Bearer ";
    String emptyToken = "";
    Long authenticatedUserId = 1L;

    when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUserId(emptyToken)).thenReturn(authenticatedUserId);
    when(checkoutService.processCheckout(checkoutRequest, authenticatedUserId))
        .thenReturn(orderResponse);

    // Act
    ResponseEntity<OrderResponse> response = checkoutController.createOrder(
        checkoutRequest, httpServletRequest);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(orderResponse, response.getBody());
  }

  @Test
  @DisplayName("Given valid checkout request with valid Bearer token, when creating order, "
      + "then extract token correctly and process checkout")
  void givenValidCheckoutRequestWithValidBearerToken_whenCreatingOrder_thenExtractAndProcess() {
    // Arrange
    String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    String bearerToken = "Bearer " + token;
    Long authenticatedUserId = 123L;

    when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUserId(token)).thenReturn(authenticatedUserId);
    when(checkoutService.processCheckout(checkoutRequest, authenticatedUserId))
        .thenReturn(orderResponse);

    // Act
    ResponseEntity<OrderResponse> response = checkoutController.createOrder(
        checkoutRequest, httpServletRequest);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(orderResponse, response.getBody());
  }

  @Test
  @DisplayName("Given checkout request with different user ID, when creating order, "
      + "then process with authenticated user ID")
  void givenCheckoutRequestWithDifferentUserId_whenCreatingOrder_thenProcessWithAuthUser() {
    // Arrange
    String token = "valid.jwt.token";
    String bearerToken = "Bearer " + token;
    Long authenticatedUserId = 999L; // Different from checkoutRequest.userId (1L)

    when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUserId(token)).thenReturn(authenticatedUserId);
    when(checkoutService.processCheckout(checkoutRequest, authenticatedUserId))
        .thenReturn(orderResponse);

    // Act
    ResponseEntity<OrderResponse> response = checkoutController.createOrder(
        checkoutRequest, httpServletRequest);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(orderResponse, response.getBody());
  }

  @Test
  @DisplayName("Given checkout request with whitespace in Bearer token, when creating order, "
      + "then extract token correctly")
  void givenCheckoutRequestWithWhitespaceInBearerToken_whenCreatingOrder_thenExtractToken() {
    // Arrange
    String token = "token.with.parts";
    String bearerToken = "Bearer   " + token; // Multiple spaces
    Long authenticatedUserId = 1L;

    when(httpServletRequest.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUserId("  " + token)).thenReturn(authenticatedUserId);
    when(checkoutService.processCheckout(checkoutRequest, authenticatedUserId))
        .thenReturn(orderResponse);

    // Act
    ResponseEntity<OrderResponse> response = checkoutController.createOrder(
        checkoutRequest, httpServletRequest);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(orderResponse, response.getBody());
  }
}
