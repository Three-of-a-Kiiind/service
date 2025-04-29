package ro.unibuc.hello.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ro.unibuc.hello.data.SubscriptionEntity;
import ro.unibuc.hello.data.SubscriptionRepository;
import ro.unibuc.hello.dto.Subscription;
import ro.unibuc.hello.exception.TierAlreadyExistsException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SubscriptionServiceTest {
    
    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllSubscriptions() {
        // Arrange
        List<SubscriptionEntity> entities = Arrays.asList(
            new SubscriptionEntity(1, 10),
            new SubscriptionEntity(2, 20)
        );
        when(subscriptionRepository.findAll()).thenReturn(entities);

        Counter counterMock = mock(Counter.class);
        when(meterRegistry.counter("subscriptions.list.all")).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        // Act
        List<Subscription> result = subscriptionService.getAllSubscriptions();

        // Assert
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getTier());
        assertEquals(10, result.get(0).getPrice());
        assertEquals(2, result.get(1).getTier());
        assertEquals(20, result.get(1).getPrice());
        verify(counterMock, times(1)).increment();
    }

    @Test
    void testGetSubscriptionByIdFound() {
        // Arrange
        String id = "123";
        SubscriptionEntity entity = new SubscriptionEntity(1, 10);
        entity.setId(id);
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(entity));

        // Act
        List<Subscription> result = subscriptionService.getSubscriptionById(id);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getTier());
        assertEquals(10, result.get(0).getPrice());
    }

    @Test
    void testGetSubscriptionByIdNotFound() {
        // Arrange
        String id = "123";
        when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

        // Act
        List<Subscription> result = subscriptionService.getSubscriptionById(id);

        // Assert
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getTier());
        assertEquals(0, result.get(0).getPrice());
    }

    @Test
    void testGetSubscriptionsByTier() {
        // Arrange
        int tier = 1;
        List<SubscriptionEntity> entities = Collections.singletonList(new SubscriptionEntity(tier, 10));
        when(subscriptionRepository.findByTier(tier)).thenReturn(entities);

        Counter counterMock = mock(Counter.class);
        when(meterRegistry.counter("subscriptions.list.by_tier")).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        // Act
        List<Subscription> result = subscriptionService.getSubscriptionsByTier(tier);

        // Assert
        assertEquals(1, result.size());
        assertEquals(tier, result.get(0).getTier());
        assertEquals(10, result.get(0).getPrice());
        verify(counterMock, times(1)).increment();
    }

    @Test
    void testGetSubscriptionsUpToTier() {
        // Arrange
        int tier = 2;
        List<SubscriptionEntity> entities = Arrays.asList(
            new SubscriptionEntity(1, 10),
            new SubscriptionEntity(2, 20)
        );
        when(subscriptionRepository.findByTierLessThanEqual(tier)).thenReturn(entities);

        Counter counterMock = mock(Counter.class);
        when(meterRegistry.counter("subscriptions.list.up_to_tier")).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        // Act
        List<Subscription> result = subscriptionService.getSubscriptionsUpToTier(tier);

        // Assert
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getTier());
        assertEquals(10, result.get(0).getPrice());
        assertEquals(2, result.get(1).getTier());
        assertEquals(20, result.get(1).getPrice());
        verify(counterMock, times(1)).increment();
    }

    @Test
    void testSaveSubscriptionSuccess() throws TierAlreadyExistsException {
        // Arrange
        Subscription subscription = new Subscription(1, 10);
        when(subscriptionRepository.existsByTier(1)).thenReturn(false);

        Counter counterMock = mock(Counter.class);
        when(meterRegistry.counter("subscriptions.created")).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        when(subscriptionRepository.save(any(SubscriptionEntity.class)))
            .thenAnswer(invocation -> {
                SubscriptionEntity entity = invocation.getArgument(0);
                entity.setId("123");
                return entity;
            });

        // Act
        Subscription result = subscriptionService.saveSubscription(subscription);

        // Assert
        assertEquals(1, result.getTier());
        assertEquals(10, result.getPrice());
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
        verify(counterMock, times(1)).increment();
    }

    @Test
    void testSaveSubscriptionWithDuplicateTier() {
        // Arrange
        Subscription subscription = new Subscription(1, 10);
        when(subscriptionRepository.existsByTier(1)).thenReturn(true);

        // Act & Assert
        assertThrows(TierAlreadyExistsException.class, () -> {
            subscriptionService.saveSubscription(subscription);
        });
    }

    @Test
    void testDeleteSubscriptionSuccess() {
        // Arrange
        String id = "123";
        SubscriptionEntity entity = new SubscriptionEntity(1, 10);
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(entity));

        Counter counterMock = mock(Counter.class);
        when(meterRegistry.counter("subscriptions.deleted")).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        // Act
        boolean result = subscriptionService.deleteSubscription(id);

        // Assert
        assertTrue(result);
        verify(subscriptionRepository).delete(entity);
        verify(counterMock, times(1)).increment();
    }

    @Test
    void testDeleteSubscriptionNotFound() {
        // Arrange
        String id = "123";
        when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

        // Act
        boolean result = subscriptionService.deleteSubscription(id);

        // Assert
        assertFalse(result);
        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void testGetSubscriptionsByMaxPrice() {
        // Arrange
        int price = 20;
        List<SubscriptionEntity> entities = Arrays.asList(
            new SubscriptionEntity(1, 10),
            new SubscriptionEntity(2, 20)
        );
        when(subscriptionRepository.findByPriceLessThanEqual(price)).thenReturn(entities);

        Counter counterMock = mock(Counter.class);
        when(meterRegistry.counter("subscriptions.list.by_price")).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        // Act
        List<Subscription> result = subscriptionService.getSubscriptionsByMaxPrice(price);

        // Assert
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getTier());
        assertEquals(10, result.get(0).getPrice());
        assertEquals(2, result.get(1).getTier());
        assertEquals(20, result.get(1).getPrice());
        verify(counterMock, times(1)).increment();
    }

    @Test
    void testGetSubscriptionsByTierAndMaxPrice() {
        // Arrange
        int tier = 1;
        int price = 10;
        List<SubscriptionEntity> entities = Collections.singletonList(new SubscriptionEntity(tier, price));
        when(subscriptionRepository.findByTierAndPriceLessThanEqual(tier, price)).thenReturn(entities);

        Counter counterMock = mock(Counter.class);
        when(meterRegistry.counter("subscriptions.list.by_tier_by_price")).thenReturn(counterMock);
        doNothing().when(counterMock).increment();

        // Act
        List<Subscription> result = subscriptionService.getSubscriptionsByTierAndMaxPrice(tier, price);

        // Assert
        assertEquals(1, result.size());
        assertEquals(tier, result.get(0).getTier());
        assertEquals(price, result.get(0).getPrice());
        verify(counterMock, times(1)).increment();
    }

}
