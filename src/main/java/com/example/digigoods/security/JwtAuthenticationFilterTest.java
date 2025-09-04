package com.example.digigoods.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.digigoods.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock
  private JwtService jwtService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @InjectMocks
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    userDetails = new User("testuser", "password", new ArrayList<>());
  }

  @Test
  @DisplayName("Given request without Authorization header, when filtering, "
      + "then continue filter chain without authentication")
  void givenRequestWithoutAuthorizationHeader_whenFiltering_thenContinueFilterChain()
      throws ServletException, IOException {
    // Arrange
    when(request.getHeader("Authorization")).thenReturn(null);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService, never()).extractUsername(anyString());
    verify(userDetailsService, never()).loadUserByUsername(anyString());
  }

  @Test
  @DisplayName("Given request with non-Bearer Authorization header, when filtering, "
      + "then continue filter chain without authentication")
  void givenRequestWithNonBearerAuthorizationHeader_whenFiltering_thenContinueFilterChain()
      throws ServletException, IOException {
    // Arrange
    when(request.getHeader("Authorization")).thenReturn("Basic dGVzdDp0ZXN0");

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService, never()).extractUsername(anyString());
    verify(userDetailsService, never()).loadUserByUsername(anyString());
  }

  @Test
  @DisplayName("Given valid JWT token, when filtering, then set authentication in security context")
  void givenValidJwtToken_whenFiltering_thenSetAuthenticationInSecurityContext()
      throws ServletException, IOException {
    // Arrange
    String token = "valid.jwt.token";
    String bearerToken = "Bearer " + token;
    String username = "testuser";

    when(request.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUsername(token)).thenReturn(username);
    when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
    when(jwtService.validateToken(token, username)).thenReturn(true);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername(token);
    verify(userDetailsService).loadUserByUsername(username);
    verify(jwtService).validateToken(token, username);
  }

  @Test
  @DisplayName("Given invalid JWT token, when filtering, then continue without authentication")
  void givenInvalidJwtToken_whenFiltering_thenContinueWithoutAuthentication()
      throws ServletException, IOException {
    // Arrange
    String token = "invalid.jwt.token";
    String bearerToken = "Bearer " + token;
    String username = "testuser";

    when(request.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUsername(token)).thenReturn(username);
    when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
    when(jwtService.validateToken(token, username)).thenReturn(false);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername(token);
    verify(userDetailsService).loadUserByUsername(username);
    verify(jwtService).validateToken(token, username);
  }

  @Test
  @DisplayName("Given expired JWT token, when filtering, then handle ExpiredJwtException")
  void givenExpiredJwtToken_whenFiltering_thenHandleExpiredJwtException()
      throws ServletException, IOException {
    // Arrange
    String token = "expired.jwt.token";
    String bearerToken = "Bearer " + token;

    when(request.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUsername(token))
        .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername(token);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    verify(jwtService, never()).validateToken(anyString(), anyString());
  }

  @Test
  @DisplayName("Given malformed JWT token, when filtering, then handle MalformedJwtException")
  void givenMalformedJwtToken_whenFiltering_thenHandleMalformedJwtException()
      throws ServletException, IOException {
    // Arrange
    String token = "malformed.jwt.token";
    String bearerToken = "Bearer " + token;

    when(request.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUsername(token)).thenThrow(new MalformedJwtException("Malformed token"));

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername(token);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    verify(jwtService, never()).validateToken(anyString(), anyString());
  }

  @Test
  @DisplayName("Given JWT token with invalid signature, when filtering, "
      + "then handle SignatureException")
  void givenJwtTokenWithInvalidSignature_whenFiltering_thenHandleException()
      throws ServletException, IOException {
    // Arrange
    String token = "invalid.signature.token";
    String bearerToken = "Bearer " + token;

    when(request.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUsername(token)).thenThrow(new SignatureException("Invalid signature"));

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername(token);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    verify(jwtService, never()).validateToken(anyString(), anyString());
  }

  @Test
  @DisplayName("Given JWT token with illegal argument, when filtering, "
      + "then handle IllegalArgumentException")
  void givenJwtTokenWithIllegalArgument_whenFiltering_thenHandleException()
      throws ServletException, IOException {
    // Arrange
    String token = "illegal.argument.token";
    String bearerToken = "Bearer " + token;

    when(request.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUsername(token))
        .thenThrow(new IllegalArgumentException("Illegal argument"));

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername(token);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    verify(jwtService, never()).validateToken(anyString(), anyString());
  }

  @Test
  @DisplayName("Given valid token but user already authenticated, when filtering, "
      + "then skip authentication")
  void givenValidTokenButUserAlreadyAuthenticated_whenFiltering_thenSkipAuthentication()
      throws ServletException, IOException {
    // Arrange
    String token = "valid.jwt.token";
    String bearerToken = "Bearer " + token;
    String username = "testuser";

    // Set existing authentication
    SecurityContextHolder.getContext().setAuthentication(
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()));

    when(request.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUsername(token)).thenReturn(username);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername(token);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    verify(jwtService, never()).validateToken(anyString(), anyString());
  }

  @Test
  @DisplayName("Given empty Bearer token, when filtering, then continue without authentication")
  void givenEmptyBearerToken_whenFiltering_thenContinueWithoutAuthentication()
      throws ServletException, IOException {
    // Arrange
    String bearerToken = "Bearer ";

    when(request.getHeader("Authorization")).thenReturn(bearerToken);
    when(jwtService.extractUsername("")).thenThrow(new IllegalArgumentException("Empty token"));

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername("");
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    verify(jwtService, never()).validateToken(anyString(), anyString());
  }
}
