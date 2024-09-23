package com.Kooont.HeritageLoad.controller;

import com.Kooont.HeritageLoad.dto.TouristPopularDataDto;
import com.Kooont.HeritageLoad.service.TouristPopularDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TouristPopularDataController {

    @Autowired
    private TouristPopularDataService touristPopularDataService;

    @GetMapping(value = "/home-random-popular-list", produces = "application/json")
    public List<TouristPopularDataDto> getExternalTourists() {
        return touristPopularDataService.fetchTouristPopulars();
    }
}
