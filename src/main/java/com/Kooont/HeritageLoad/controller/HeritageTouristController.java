package com.Kooont.HeritageLoad.controller;

import com.Kooont.HeritageLoad.dto.HeritageItemDto;
import com.Kooont.HeritageLoad.service.HeritageTouristService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HeritageTouristController {
    private final HeritageTouristService heritageTouristService;

    public HeritageTouristController(HeritageTouristService heritageTouristService) {
        this.heritageTouristService = heritageTouristService;
    }

    // 인기 지역 순서에 맞는 국보 리스트 반환하는 API
    @GetMapping(value = "/heritage-tourist-popular-heritage", produces = "application/json")
    public ResponseEntity<List<HeritageItemDto>> getPopularHeritageItems() {
        List<HeritageItemDto> heritageItems = heritageTouristService.fetchHeritageItemsByRandomTouristArea();
        return ResponseEntity.ok(heritageItems);
    }
}
