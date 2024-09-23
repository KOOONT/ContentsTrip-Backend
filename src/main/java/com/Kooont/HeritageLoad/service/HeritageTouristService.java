package com.Kooont.HeritageLoad.service;

import com.Kooont.HeritageLoad.dto.HeritageDetailDto;
import com.Kooont.HeritageLoad.dto.HeritageItemDto;
import com.Kooont.HeritageLoad.dto.TouristPopularDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class HeritageTouristService {
    private final TouristPopularDataService touristPopularDataService;
    private final HeritageService heritageService;
    private static final Logger logger = LoggerFactory.getLogger(GungListService.class);

    public HeritageTouristService(TouristPopularDataService touristPopularDataService, HeritageService heritageService) {
        this.touristPopularDataService = touristPopularDataService;
        this.heritageService = heritageService;
    }

    // 인기 지역별로 국보 리스트 가져오기
    public List<HeritageItemDto> fetchHeritageItemsByRandomTouristArea() {
        // 1. 인기 관광지 데이터를 가져옴
        List<TouristPopularDataDto> popularTouristAreas = touristPopularDataService.fetchTouristPopulars();

        // 2. 랜덤으로 하나의 지역 선택
        Random random = new Random();
        TouristPopularDataDto selectedArea = popularTouristAreas.get(random.nextInt(popularTouristAreas.size()));
        String areaName = selectedArea.getAreaNm();
        String ccbaCtcd = getCcbaCtcdByAreaName(areaName);
        logger.info("HeritageTouristService ccbaCtcd {}", ccbaCtcd);

        // 특정 ccbaCtcd 값 목록
        List<String> specialCtcds = Arrays.asList("23", "24", "26", "50");
        List<HeritageItemDto> heritageItems = new ArrayList<>();

        if (specialCtcds.contains(ccbaCtcd)) {
            // ccbaCtcd가 특정 값에 해당하는 경우 모두 가져옴
            for (String ctcd : specialCtcds) {
                heritageItems.addAll(heritageService.fetchHeritageItemsByCtcd(ctcd));
            }
        } else if (ccbaCtcd != null) {
            // 그렇지 않은 경우 한 개의 항목만 요청
            heritageItems.addAll(heritageService.fetchHeritageItemsByCtcd(ccbaCtcd));
        }

        if (!heritageItems.isEmpty()) {
            // 4. 최대 10개의 국보 항목을 가져옴
            List<HeritageItemDto> selectedHeritageItems = heritageItems.stream()
                    .limit(10)
                    .collect(Collectors.toList());

            // 5. 각 항목에 대한 이미지 URL과 상세 정보 설정
            List<CompletableFuture<Void>> futures = selectedHeritageItems.stream()
                    .map(item -> CompletableFuture.runAsync(() -> {
                        try {
                            HeritageDetailDto detail = heritageService.fetchHeritageSimpleDetailByAsno(
                                    item.getCcbaAsno(), item.getCcbaKdcd(), item.getCcbaCtcd()
                            );
                            if (detail != null) {
                                logger.info("Fetched detail: {}", detail); // 디버깅을 위한 로그 추가

                                item.setImageUrl(detail.getImageUrl());
                                item.setCcmaName(detail.getCcmaName());
                                item.setCcbaMnm1(detail.getCcbaMnm1());
                                item.setCcbaCtcdNm(detail.getCcbaCtcdNm());
                                item.setCcsiName(detail.getCcsiName());
                            }
                        } catch (Exception e) {
                            logger.error("Error fetching heritage detail for item: {}", item.getCcbaAsno(), e);
                        }
                    }))
                    .collect(Collectors.toList());

            // 6. 모든 비동기 작업이 완료될 때까지 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            return selectedHeritageItems;
        }
        return Collections.emptyList();
    }



    // 지역명을 기반으로 ccbaCtcd 값을 매핑하는 메서드
    private String getCcbaCtcdByAreaName(String areaName) {
        switch (areaName) {
            case "서울특별시": return "11"; // ok
            case "부산광역시": return "21"; // ok
            case "대구광역시": return "22";
            case "인천광역시": return "23"; // 1
            case "광주광역시": return "24"; // 2
            case "울산광역시": return "26"; // 2
            case "세종특별자치시": return "45";
            case "경기도": return "31";
            case "강원특별자치도": return "32"; // 2 ok
            case "충청북도": return "33";
            case "충청남도": return "34";
            case "전북특별자치도": return "35";
            case "전라남도": return "36"; // ok
            case "경상북도": return "37"; // ok
            case "경상남도": return "38";
            case "제주특별자치도": return "50";

            // 다른 지역들에 대한 매핑 추가
            default: return null;
        }
    }
}
