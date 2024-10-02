package com.Kooont.HeritageLoad.controller;

import com.Kooont.HeritageLoad.dto.HeritageDetailDto;
import com.Kooont.HeritageLoad.dto.HeritageItemDto;
import com.Kooont.HeritageLoad.dto.HeritageResponseDto;
import com.Kooont.HeritageLoad.dto.RelatedAttractionDto;
import com.Kooont.HeritageLoad.service.HeritageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HeritageController {

    private final HeritageService heritageService;

    public HeritageController(HeritageService heritageService) {
        this.heritageService = heritageService;
    }

    // 전체보기 API
    @GetMapping(value = "/home-all-heritage-list/{pageIndex}/{pageUnit}/{ccbaKdcd}", produces = "application/json")
    public HeritageResponseDto getHomeAllHeritageList(@PathVariable String pageIndex,
                                                      @PathVariable String pageUnit,
                                                      @PathVariable String ccbaKdcd) {
        return heritageService.fetchHomeAllHeritageItems(pageIndex, pageUnit, ccbaKdcd);
    }

    // 국보 랜덤 API
    @GetMapping(value = "/home-random-heritage-list", produces = "application/json")
    public List<HeritageItemDto> getHomeRandomHeritageList() {
        return heritageService.fetchHomeRandomHeritageItems();
    }

    @GetMapping(value = "/home-random-treasure-list", produces = "application/json")
    public List<HeritageItemDto> getHomeRandomTreasureList() {
        return heritageService.fetchHomeRandomTreasureItems();
    }

    @GetMapping(value = "/home-random-historic-list", produces = "application/json")
    public List<HeritageItemDto> getHomeRandomHistoricList() {
        return heritageService.fetchHomeRandomHistoricItems();
    }

    // 특정 국보의 상세 정보 제공
    @GetMapping(value ="/heritage-detail/{ccbaAsno}/{ccbaKdcd}/{ccbaCtcd}", produces = "application/json")
    public HeritageDetailDto getHeritageDetail(@PathVariable String ccbaAsno,
                                               @PathVariable String ccbaKdcd,
                                               @PathVariable String ccbaCtcd) {
        return heritageService.fetchHeritageDetailByAsno(ccbaAsno, ccbaKdcd, ccbaCtcd);
    }
    // 연관 관광지
    @GetMapping(value = "/heritage-detail-related-attractions/{mapX}/{mapY}", produces = "application/json")
    public Map<String, List<RelatedAttractionDto>> getHeritageDetailRelatedAttractions(@PathVariable Double mapX,
                                                                                       @PathVariable Double mapY)
            throws UnsupportedEncodingException, URISyntaxException, JsonProcessingException {
        // heritageService 메서드 호출
        Map<String, List<RelatedAttractionDto>> relatedAttractions = heritageService.fetchHeritageDetailRelatedAttractions(mapX, mapY);

        // 반환
        return relatedAttractions; // Map 형태로 반환
    }

    @GetMapping(value = "/heritage-detail-related-attractions-area/{maxCount}/{areaCode}/{sigunguCode}", produces = "application/json")
    public Map<String, List<RelatedAttractionDto>> getHeritageDetailRelatedAttractions(@PathVariable Integer maxCount,
                                                                                       @PathVariable String areaCode,
                                                                                       @PathVariable String sigunguCode)
            throws UnsupportedEncodingException, URISyntaxException, JsonProcessingException {
        // heritageService 메서드 호출
        Map<String, List<RelatedAttractionDto>> relatedAttractions = heritageService.fetchHeritageDetailRelatedAttractionsArea(maxCount, areaCode, sigunguCode);

        // 반환
        return relatedAttractions; // Map 형태로 반환
    }

    // 검색
    @GetMapping(value ="/heritage-search/{pageIndex}/{pageUnit}/{ccbaMnm1}", produces = "application/json")
    public HeritageResponseDto getHeritageSearch(@PathVariable String pageIndex,
                                                   @PathVariable String pageUnit,
                                                    @PathVariable String ccbaMnm1) {
        return heritageService.fetchHeritageSearch(pageIndex, pageUnit, ccbaMnm1);
    }
}
