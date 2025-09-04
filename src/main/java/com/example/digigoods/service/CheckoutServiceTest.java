package com.example.digigoods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.digigoods.dto.CheckoutRequest;
import com.example.digigoods.dto.OrderResponse;
import com.example.digigoods.exception.ExcessiveDiscountException;
import com.example.digigoods.exception.UnauthorizedAccessException;
import com.example.digigoods.model.Discount;
import com.example.digigoods.model.DiscountType;
import com.example.digigoods.model.Order;
import com.example.digigoods.model.Product;
import com.example.digigoods.model.User;
import com.example.digigoods.repository.OrderRepository;
import com.example.digigoods.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

  @Mock
  private ProductService productService;

  @Mock
  private DiscountService discountService;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CheckoutService checkoutService;

  private CheckoutRequest checkoutRequest;
  private User testUser;
  private Product product1;
  private Product product2;
  private Discount generalDiscount;
  private Discount productSpecificDiscount;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");

    product1 = new Product();
    product1.setId(1L);
    product1.setName("Product 1");
    product1.setPrice(new BigDecimal("100.00"));
    product1.setStock(10);

    product2 = new Product();
    product2.setId(2L);
    product2.setName("Product 2");
    product2.setPrice(new BigDecimal("50.00"));
    product2.setStock(5);

    generalDiscount = new Discount(1L, "GENERAL10", new BigDecimal("10.00"),
        DiscountType.GENERAL, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
        5, new HashSet<>());

    Set<Product> applicableProducts = new HashSet<>();
    applicableProducts.add(product1);
    productSpecificDiscount = new Discount(2L, "PRODUCT20", new BigDecimal("20.00"),
        DiscountType.PRODUCT_SPECIFIC, LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30), 3, applicableProducts);

    checkoutRequest = new CheckoutRequest();
    checkoutRequest.setUserId(1L);
    checkoutRequest.setProductIds(List.of(1L, 2L));
    checkoutRequest.setDiscountCodes(List.of("GENERAL10"));
  }

  @Test
  @DisplayName("Given valid checkout request, when processing checkout, "
      + "then return successful order response")
  void givenValidCheckoutRequest_whenProcessingCheckout_thenReturnSuccessfulOrderResponse() {
    // Arrange
    when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1, product2));
    when(discountService.validateAndGetDiscounts(anyList())).thenReturn(List.of(generalDiscount));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(Order.class))).thenReturn(new Order());

    // Act
    OrderResponse response = checkoutService.processCheckout(checkoutRequest, 1L);

    // Assert
    assertEquals("Order created successfully!", response.getMessage());
    assertEquals(new BigDecimal("135.00"), response.getFinalPrice()); // 150 - 10% = 135
    verify(productService).validateAndUpdateStock(checkoutRequest.getProductIds());
    verify(discountService).updateDiscountUsage(List.of(generalDiscount));
    verify(orderRepository).save(any(Order.class));
  }

  @Test
  @DisplayName("Given unauthorized user, when processing checkout, "
      + "then throw UnauthorizedAccessException")
  void givenUnauthorizedUser_whenProcessingCheckout_thenThrowUnauthorizedAccessException() {
    // Act & Assert
    UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
        () -> checkoutService.processCheckout(checkoutRequest, 2L));

    assertEquals("User cannot place order for another user", exception.getMessage());
  }

  @Test
  @DisplayName("Given checkout with no discounts, when processing checkout, "
      + "then calculate original price")
  void givenCheckoutWithNoDiscounts_whenProcessingCheckout_thenCalculateOriginalPrice() {
    // Arrange
    checkoutRequest.setDiscountCodes(null);
    when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1, product2));
    when(discountService.validateAndGetDiscounts(null)).thenReturn(List.of());
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(Order.class))).thenReturn(new Order());

    // Act
    OrderResponse response = checkoutService.processCheckout(checkoutRequest, 1L);

    // Assert
    assertEquals(new BigDecimal("150.00"), response.getFinalPrice()); // 100 + 50 = 150
  }

  @Test
  @DisplayName("Given checkout with product-specific discount, when processing checkout, "
      + "then apply discount only to applicable products")
  void givenCheckoutWithProductSpecificDiscount_whenProcessingCheckout_thenApplyDiscount() {
    // Arrange
    checkoutRequest.setDiscountCodes(List.of("PRODUCT20"));
    when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1, product2));
    when(discountService.validateAndGetDiscounts(anyList()))
        .thenReturn(List.of(productSpecificDiscount));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(Order.class))).thenReturn(new Order());

    // Act
    OrderResponse response = checkoutService.processCheckout(checkoutRequest, 1L);

    // Assert
    // Product1: 100 - 20% = 80, Product2: 50 (no discount) = 130 total
    assertEquals(new BigDecimal("130.00"), response.getFinalPrice());
  }

  @Test
  @DisplayName("Given checkout with multiple general discounts, when processing checkout, "
      + "then apply all discounts sequentially")
  void givenCheckoutWithMultipleGeneralDiscounts_whenProcessingCheckout_thenApplyAllDiscounts() {
    // Arrange
    Discount secondGeneralDiscount = new Discount(3L, "GENERAL15", new BigDecimal("15.00"),
        DiscountType.GENERAL, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
        2, new HashSet<>());

    checkoutRequest.setDiscountCodes(List.of("GENERAL10", "GENERAL15"));
    when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1, product2));
    when(discountService.validateAndGetDiscounts(anyList()))
        .thenReturn(List.of(generalDiscount, secondGeneralDiscount));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(Order.class))).thenReturn(new Order());

    // Act
    OrderResponse response = checkoutService.processCheckout(checkoutRequest, 1L);

    // Assert
    // 150 - 10% = 135, then 135 - 15% = 114.75
    assertEquals(new BigDecimal("114.75"), response.getFinalPrice());
  }

  @Test
  @DisplayName("Given checkout with excessive discount, when processing checkout, "
      + "then throw ExcessiveDiscountException")
  void givenCheckoutWithExcessiveDiscount_whenProcessingCheckout_thenThrowException() {
    // Arrange
    Discount excessiveDiscount = new Discount(4L, "EXCESSIVE80", new BigDecimal("80.00"),
        DiscountType.GENERAL, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
        1, new HashSet<>());

    checkoutRequest.setDiscountCodes(List.of("EXCESSIVE80"));
    when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1, product2));
    when(discountService.validateAndGetDiscounts(anyList())).thenReturn(List.of(excessiveDiscount));

    // Act & Assert
    ExcessiveDiscountException exception = assertThrows(ExcessiveDiscountException.class,
        () -> checkoutService.processCheckout(checkoutRequest, 1L));

    assertEquals("Total discount exceeds the maximum allowed 75% of the original subtotal",
        exception.getMessage());
  }

  @Test
  @DisplayName("Given checkout with mixed discount types, when processing checkout, "
      + "then apply product-specific first then general")
  void givenCheckoutWithMixedDiscountTypes_whenProcessingCheckout_thenApplyProductSpecificFirst() {
    // Arrange
    checkoutRequest.setDiscountCodes(List.of("PRODUCT20", "GENERAL10"));
    when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1, product2));
    when(discountService.validateAndGetDiscounts(anyList()))
        .thenReturn(List.of(productSpecificDiscount, generalDiscount));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(Order.class))).thenReturn(new Order());

    // Act
    OrderResponse response = checkoutService.processCheckout(checkoutRequest, 1L);

    // Assert
    // Product1: 100 - 20% = 80, Product2: 50, Subtotal: 130
    // Then apply general 10%: 130 - 10% = 117
    assertEquals(new BigDecimal("117.00"), response.getFinalPrice());
  }

  @Test
  @DisplayName("Given user not found, when processing checkout, then throw RuntimeException")
  void givenUserNotFound_whenProcessingCheckout_thenThrowRuntimeException() {
    // Arrange
    when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1, product2));
    when(discountService.validateAndGetDiscounts(anyList())).thenReturn(List.of(generalDiscount));
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> checkoutService.processCheckout(checkoutRequest, 1L));

    assertEquals("User not found", exception.getMessage());
  }

  @Test
  @DisplayName("Given checkout with duplicate products, when processing checkout, "
      + "then calculate total correctly")
  void givenCheckoutWithDuplicateProducts_whenProcessingCheckout_thenCalculateTotalCorrectly() {
    // Arrange
    checkoutRequest.setProductIds(List.of(1L, 1L, 2L)); // Two of product1, one of product2
    checkoutRequest.setDiscountCodes(null);
    when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1, product2));
    when(discountService.validateAndGetDiscounts(null)).thenReturn(List.of());
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(Order.class))).thenReturn(new Order());

    // Act
    OrderResponse response = checkoutService.processCheckout(checkoutRequest, 1L);

    // Assert
    assertEquals(new BigDecimal("250.00"), response.getFinalPrice()); // 100 + 100 + 50 = 250
  }
}
