package com.travel.module.itinerary.biz.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.module.itinerary.biz.domain.entity.DayPlan;
import com.travel.module.itinerary.biz.domain.entity.Itinerary;
import com.travel.module.itinerary.biz.domain.repository.ItineraryRepository;
import com.travel.module.itinerary.biz.infra.persistence.ItineraryMapper;
import com.travel.module.itinerary.biz.infra.persistence.ItineraryPO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@Primary
@RequiredArgsConstructor
public class ItineraryRepositoryImpl implements ItineraryRepository {

    private final ItineraryMapper itineraryMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void save(Itinerary itinerary) {
        ItineraryPO po = toPO(itinerary);
        if (po.getId() == null) {
            itineraryMapper.insert(po);
            itinerary.setId(po.getId());
        } else {
            itineraryMapper.updateById(po);
        }
    }

    @Override
    public Itinerary findByItineraryId(String itineraryId) {
        LambdaQueryWrapper<ItineraryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ItineraryPO::getItineraryId, itineraryId);
        ItineraryPO po = itineraryMapper.selectOne(wrapper);
        return po == null ? null : toItinerary(po);
    }

    @Override
    public List<Itinerary> findByUserId(Long userId) {
        LambdaQueryWrapper<ItineraryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ItineraryPO::getUserId, userId);
        wrapper.orderByDesc(ItineraryPO::getCreatedAt);
        List<ItineraryPO> pos = itineraryMapper.selectList(wrapper);
        return pos.stream().map(this::toItinerary).collect(Collectors.toList());
    }

    @Override
    public void deleteByItineraryId(String itineraryId) {
        LambdaQueryWrapper<ItineraryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ItineraryPO::getItineraryId, itineraryId);
        itineraryMapper.delete(wrapper);
    }

    private Itinerary toItinerary(ItineraryPO po) {
        Itinerary itinerary = new Itinerary();
        itinerary.setId(po.getId());
        itinerary.setItineraryId(po.getItineraryId());
        itinerary.setSessionId(po.getSessionId());
        itinerary.setUserId(po.getUserId());
        itinerary.setDestination(po.getDestination());
        itinerary.setStartDate(po.getStartDate());
        itinerary.setEndDate(po.getEndDate());
        itinerary.setTotalBudget(po.getTotalBudget());
        itinerary.setDays(po.getDays());
        itinerary.setCreatedAt(po.getCreatedAt());
        itinerary.setUpdatedAt(po.getUpdatedAt());

        if (po.getDayPlansJson() != null) {
            try {
                itinerary.setDayPlans(objectMapper.readValue(po.getDayPlansJson(),
                        new TypeReference<List<DayPlan>>() {}));
            } catch (JsonProcessingException e) {
                itinerary.setDayPlans(null);
            }
        }
        return itinerary;
    }

    private ItineraryPO toPO(Itinerary itinerary) {
        ItineraryPO po = new ItineraryPO();
        po.setId(itinerary.getId());
        po.setItineraryId(itinerary.getItineraryId());
        po.setSessionId(itinerary.getSessionId());
        po.setUserId(itinerary.getUserId());
        po.setDestination(itinerary.getDestination());
        po.setStartDate(itinerary.getStartDate());
        po.setEndDate(itinerary.getEndDate());
        po.setTotalBudget(itinerary.getTotalBudget());
        po.setDays(itinerary.getDays());
        po.setCreatedAt(itinerary.getCreatedAt());
        po.setUpdatedAt(itinerary.getUpdatedAt());

        if (itinerary.getDayPlans() != null) {
            try {
                po.setDayPlansJson(objectMapper.writeValueAsString(itinerary.getDayPlans()));
            } catch (JsonProcessingException e) {
                po.setDayPlansJson(null);
            }
        }
        return po;
    }
}
