package com.Kooont.HeritageLoad.controller;

import com.Kooont.HeritageLoad.dto.GungListItemDto;
import com.Kooont.HeritageLoad.service.GungListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@RestController
public class GungListController {

    private GungListService gungListService;

    @Autowired
    public GungListController(GungListService gungListService) {
        this.gungListService = gungListService;
    }

    @GetMapping("api/GungList")
    public List<GungListItemDto> getGungListItems() throws URISyntaxException, UnsupportedEncodingException {
        List<String> defaultKeywords = Arrays.asList("경복궁", "창덕궁", "창경궁", "덕수궁", "경희궁");

        // TourService를 사용해 고정된 키워드로 데이터를 검색
        return gungListService.searchGungListItemsByKeywords(defaultKeywords);

    }


}
