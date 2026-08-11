package com.travel.module.itinerary.biz.domain.repository;

import com.travel.module.itinerary.biz.domain.entity.Itinerary;

public interface ItineraryRepository {

    void save(Itinerary itinerary);

    Itinerary findByItineraryId(String itineraryId);

    java.util.List<Itinerary> findByUserId(Long userId);

    void deleteByItineraryId(String itineraryId);
}
