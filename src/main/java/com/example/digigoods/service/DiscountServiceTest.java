package com.example.digigoods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.digigoods.exception.InvalidDiscountException;
import com.example.digigoods.model.Discount;
import com.example.digigoods.model.DiscountType;
import com.example.digigoods.model.Product;
import com.example.digigoods.repository.DiscountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

  @Mock
  private DiscountRepository discountRepository;

  @InjectMocks
  private DiscountService discountService;

  private Discount validDiscount;
  private Discount expiredDiscount;
  private Discount notYetValidDiscount;
  private Discount noUsesLeftDiscount;

  @BeforeEach
  void setUp() {
    LocalDate today = LocalDate.now();

    validDiscount = new Discount(1L, "VALID20", new BigDecimal("20.00"),
        DiscountType.GENERAL, today.minusDays(1), today.plusDays(30), 5, new HashSet<>());

    expiredDiscount = new Discount(2L, "EXPIRED10", new BigDecimal("10.00"),
        DiscountType.GENERAL, today.minusDays(30), today.minusDays(1), 3, new HashSet<>());

    notYetValidDiscount = new Discount(3L, "FUTURE15", new BigDecimal("15.00"),
        DiscountType.GENERAL, today.plusDays(1), today.plusDays(30), 2, new HashSet<>());

    noUsesLeftDiscount = new Discount(4L, "NOUSES25", new BigDecimal("25.00"),
        DiscountType.GENERAL, today.minusDays(1), today.plusDays(30), 0, new HashSet<>());
  }

  @Test
  @DisplayName("Given repository with discounts, when getting all discounts, "
      + "then return all discounts")
  void givenRepositoryWithDiscounts_whenGettingAllDiscounts_thenReturnAllDiscounts() {
    // Arrange
    List<Discount> expectedDiscounts = List.of(validDiscount, expiredDiscount);
    when(discountRepository.findAll()).thenReturn(expectedDiscounts);

    // Act
    List<Discount> actualDiscounts = discountService.getAllDiscounts();

    // Assert
    assertEquals(expectedDiscounts, actualDiscounts);
    verify(discountRepository).findAll();
  }

  @Test
  @DisplayName("Given null discount codes, when validating and getting discounts, "
      + "then return empty list")
  void givenNullDiscountCodes_whenValidatingAndGettingDiscounts_thenReturnEmptyList() {
    // Act
    List<Discount> result = discountService.validateAndGetDiscounts(null);

    // Assert
    assertTrue(result.isEmpty());
    verify(discountRepository, never()).findAllByCodeIn(anyList());
  }

  @Test
  @DisplayName("Given empty discount codes, when validating and getting discounts, "
      + "then return empty list")
  void givenEmptyDiscountCodes_whenValidatingAndGettingDiscounts_thenReturnEmptyList() {
    // Act
    List<Discount> result = discountService.validateAndGetDiscounts(Collections.emptyList());

    // Assert
    assertTrue(result.isEmpty());
    verify(discountRepository, never()).findAllByCodeIn(anyList());
  }

  @Test
  @DisplayName("Given valid discount codes, when validating and getting discounts, "
      + "then return valid discounts")
  void givenValidDiscountCodes_whenValidatingAndGettingDiscounts_thenReturnValidDiscounts() {
    // Arrange
    List<String> discountCodes = List.of("VALID20");
    List<Discount> foundDiscounts = List.of(validDiscount);
    when(discountRepository.findAllByCodeIn(discountCodes)).thenReturn(foundDiscounts);

    // Act
    List<Discount> result = discountService.validateAndGetDiscounts(discountCodes);

    // Assert
    assertEquals(foundDiscounts, result);
    verify(discountRepository).findAllByCodeIn(discountCodes);
  }

  @Test
  @DisplayName("Given missing discount code, when validating and getting discounts, "
      + "then throw InvalidDiscountException")
  void givenMissingDiscountCode_whenValidatingDiscounts_thenThrowInvalidDiscountException() {
    // Arrange
    List<String> discountCodes = List.of("VALID20", "MISSING");
    List<Discount> foundDiscounts = List.of(validDiscount); // Only one found, one missing
    when(discountRepository.findAllByCodeIn(discountCodes)).thenReturn(foundDiscounts);

    // Act & Assert
    InvalidDiscountException exception = assertThrows(InvalidDiscountException.class,
        () -> discountService.validateAndGetDiscounts(discountCodes));

    assertTrue(exception.getMessage().contains("MISSING"));
    assertTrue(exception.getMessage().contains("discount code not found"));
  }

  @Test
  @DisplayName("Given expired discount, when validating and getting discounts, "
      + "then throw InvalidDiscountException")
  void givenExpiredDiscount_whenValidatingAndGettingDiscounts_thenThrowInvalidDiscountException() {
    // Arrange
    List<String> discountCodes = List.of("EXPIRED10");
    List<Discount> foundDiscounts = List.of(expiredDiscount);
    when(discountRepository.findAllByCodeIn(discountCodes)).thenReturn(foundDiscounts);

    // Act & Assert
    InvalidDiscountException exception = assertThrows(InvalidDiscountException.class,
        () -> discountService.validateAndGetDiscounts(discountCodes));

    assertTrue(exception.getMessage().contains("EXPIRED10"));
    assertTrue(exception.getMessage().contains("discount has expired"));
  }

  @Test
  @DisplayName("Given not yet valid discount, when validating and getting discounts, "
      + "then throw InvalidDiscountException")
  void givenNotYetValidDiscount_whenValidatingDiscounts_thenThrowInvalidDiscountException() {
    // Arrange
    List<String> discountCodes = List.of("FUTURE15");
    List<Discount> foundDiscounts = List.of(notYetValidDiscount);
    when(discountRepository.findAllByCodeIn(discountCodes)).thenReturn(foundDiscounts);

    // Act & Assert
    InvalidDiscountException exception = assertThrows(InvalidDiscountException.class,
        () -> discountService.validateAndGetDiscounts(discountCodes));

    assertTrue(exception.getMessage().contains("FUTURE15"));
    assertTrue(exception.getMessage().contains("discount is not yet valid"));
  }

  @Test
  @DisplayName("Given discount with no uses left, when validating and getting discounts, "
      + "then throw InvalidDiscountException")
  void givenDiscountWithNoUsesLeft_whenValidatingDiscounts_thenThrowInvalidDiscountException() {
    // Arrange
    List<String> discountCodes = List.of("NOUSES25");
    List<Discount> foundDiscounts = List.of(noUsesLeftDiscount);
    when(discountRepository.findAllByCodeIn(discountCodes)).thenReturn(foundDiscounts);

    // Act & Assert
    InvalidDiscountException exception = assertThrows(InvalidDiscountException.class,
        () -> discountService.validateAndGetDiscounts(discountCodes));

    assertTrue(exception.getMessage().contains("NOUSES25"));
    assertTrue(exception.getMessage().contains("discount has no remaining uses"));
  }

  @Test
  @DisplayName("Given multiple valid discounts, when validating and getting discounts, "
      + "then return all valid discounts")
  void givenMultipleValidDiscounts_whenValidatingAndGettingDiscounts_thenReturnAllValidDiscounts() {
    // Arrange
    Discount anotherValidDiscount = new Discount(5L, "ANOTHER30", new BigDecimal("30.00"),
        DiscountType.GENERAL, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), 3,
        new HashSet<>());

    List<String> discountCodes = List.of("VALID20", "ANOTHER30");
    List<Discount> foundDiscounts = List.of(validDiscount, anotherValidDiscount);
    when(discountRepository.findAllByCodeIn(discountCodes)).thenReturn(foundDiscounts);

    // Act
    List<Discount> result = discountService.validateAndGetDiscounts(discountCodes);

    // Assert
    assertEquals(foundDiscounts, result);
    verify(discountRepository).findAllByCodeIn(discountCodes);
  }

  @Test
  @DisplayName("Given discounts to update, when updating discount usage, "
      + "then decrease remaining uses and save")
  void givenDiscountsToUpdate_whenUpdatingDiscountUsage_thenDecreaseRemainingUsesAndSave() {
    // Arrange
    Discount discount1 = new Discount(1L, "TEST1", new BigDecimal("10.00"), DiscountType.GENERAL,
        LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), 5, new HashSet<>());
    Discount discount2 = new Discount(2L, "TEST2", new BigDecimal("20.00"), DiscountType.GENERAL,
        LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), 3, new HashSet<>());

    List<Discount> discounts = List.of(discount1, discount2);

    // Act
    discountService.updateDiscountUsage(discounts);

    // Assert
    assertEquals(4, discount1.getRemainingUses()); // 5 - 1 = 4
    assertEquals(2, discount2.getRemainingUses()); // 3 - 1 = 2
    verify(discountRepository, times(2)).save(any(Discount.class));
  }

  @Test
  @DisplayName("Given empty discount list, when updating discount usage, "
      + "then no operations performed")
  void givenEmptyDiscountList_whenUpdatingDiscountUsage_thenNoOperationsPerformed() {
    // Act
    discountService.updateDiscountUsage(Collections.emptyList());

    // Assert
    verify(discountRepository, never()).save(any(Discount.class));
  }
}
