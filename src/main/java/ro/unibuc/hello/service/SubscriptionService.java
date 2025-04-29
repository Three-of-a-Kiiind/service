package ro.unibuc.hello.service;

import java.util.*;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ro.unibuc.hello.data.SubscriptionEntity;
import ro.unibuc.hello.data.SubscriptionRepository;
import ro.unibuc.hello.dto.Subscription;

import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.exception.TierAlreadyExistsException;

import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SubscriptionService {
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    public List<Subscription> getAllSubscriptions() {
        List<SubscriptionEntity> entities = subscriptionRepository.findAll();

        meterRegistry.counter("subscriptions.list.all").increment();

        return convert(entities);
    }

    public List<Subscription> getSubscriptionsByTier(int tier) {
        List<SubscriptionEntity> entities = subscriptionRepository.findByTier(tier);

        meterRegistry.counter("subscriptions.list.by_tier").increment();

        return convert(entities);
    }

    public List<Subscription> getSubscriptionsUpToTier(int tier) {
        List<SubscriptionEntity> entities = subscriptionRepository.findByTierLessThanEqual(tier);

        meterRegistry.counter("subscriptions.list.up_to_tier").increment();

        return convert(entities);
    }

    public List<Subscription> getSubscriptionById(String id) {
        Optional<SubscriptionEntity> entity = subscriptionRepository.findById(id);
        
        List<Subscription> response = new ArrayList<>();
        
        if (!entity.isPresent()) {
            response.add(new Subscription());
        } else {
            SubscriptionEntity subscription = entity.get();
            response.add(new Subscription(subscription.getTier(), subscription.getPrice()));
        }
        
        return response;
    }

    public List<Subscription> getSubscriptionsByMaxPrice(int price) {
        List<SubscriptionEntity> entities = subscriptionRepository.findByPriceLessThanEqual(price);

        meterRegistry.counter("subscriptions.list.by_price").increment();

        return convert(entities);
    }

    public List<Subscription> getSubscriptionsByTierAndMaxPrice(int tier, int price) {
        List<SubscriptionEntity> entities = subscriptionRepository.findByTierAndPriceLessThanEqual(tier, price);

        meterRegistry.counter("subscriptions.list.by_tier_by_price").increment();
        
        return convert(entities);
    }

    public Subscription saveSubscription(Subscription subscription) throws TierAlreadyExistsException {
        // Check if tier already exists
        if (subscriptionRepository.existsByTier(subscription.getTier())) {
            throw new TierAlreadyExistsException("Tier " + subscription.getTier() + " already exists. Cannot add duplicate tiers.");
        }
        
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setTier(subscription.getTier());
        entity.setPrice(subscription.getPrice());
        subscriptionRepository.save(entity);

        meterRegistry.counter("subscriptions.created").increment();

        return new Subscription(entity.getId(), entity.getTier(), entity.getPrice());
    }

    public boolean deleteSubscription(String id) {
        Optional<SubscriptionEntity> entity = subscriptionRepository.findById(id);
        
        if (entity.isPresent()) {
            subscriptionRepository.delete(entity.get());

            meterRegistry.counter("subscriptions.deleted").increment();

            return true;
        }
        
        return false;
    }

    public void deleteAllSubscriptions() {
        subscriptionRepository.deleteAll();
    }

    private List<Subscription> convert(List<SubscriptionEntity> entities) {
        return entities.stream()
            .map(entity -> new Subscription(entity.getTier(), entity.getPrice()))
            .collect(Collectors.toList());
    }
}