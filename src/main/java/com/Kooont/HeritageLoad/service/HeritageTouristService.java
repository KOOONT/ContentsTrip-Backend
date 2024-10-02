package com.Kooont.HeritageLoad.service;

import com.Kooont.HeritageLoad.dto.HeritageDetailDto;
import com.Kooont.HeritageLoad.dto.HeritageItemDto;
import com.Kooont.HeritageLoad.dto.TouristPopularDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
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
//        List<TouristPopularDataDto> popularTouristAreas = touristPopularDataService.fetchTouristPopulars();
//
//        // 2. 랜덤으로 하나의 지역 선택
//        Random random = new Random();
//        TouristPopularDataDto selectedArea = popularTouristAreas.get(random.nextInt(popularTouristAreas.size()));
//        String areaName = selectedArea.getAreaNm();
//        String ccbaCtcd = getCcbaCtcdByAreaName(areaName);
        // 국가유산 API 11 : 서울 21 : 부산 22 : 대구 23 : 인천 24 : 광주 25 : 대전 26 : 울산 45 : 세종 31 : 경기 32 : 강원 33 : 충북 34 : 충남 35 : 전북 36 : 전남 37 : 경북 38 : 경남 50 : 제주
        // 공공데이터 API
        List<String> ccbaCtcdList = Arrays.asList("11", "21", "22", "23", "24", "25", "26", "45", "31", "32", "33", "34", "35", "36", "37", "38", "50");

        // 랜덤으로 하나의 ccbaCtcd 값 선택
        Random random = new Random();
        String ccbaCtcd = ccbaCtcdList.get(random.nextInt(ccbaCtcdList.size()));
        // 특정 ccbaCtcd 값 목록 (23, 24, 26, 50 등)
        List<String> specialCtcds = Arrays.asList("23", "24", "25", "26", "45", "50");
        List<HeritageItemDto> heritageItems = new ArrayList<>();


        if (specialCtcds.contains(ccbaCtcd)) {
            // ccbaCtcd가 특정 값에 해당하는 경우 모두 가져옴
            for (String ctcd : specialCtcds) {
                heritageItems.addAll(heritageService.fetchHeritageItemsByCtcd(ctcd));
            }
        } else if (ccbaCtcd != null) {
//             그렇지 않은 경우 한 개의 항목만 요청
            heritageItems.addAll(heritageService.fetchHeritageItemsByCtcd(ccbaCtcd));
        }

        if (!heritageItems.isEmpty()) {
            // 4. 최대 10개의 국보 항목을 가져옴
            List<HeritageItemDto> selectedHeritageItems = heritageItems.stream()
                    .limit(10)
                    .collect(Collectors.toList());

            // 병렬 처리용 Executor 생성 (최대 20개의 스레드를 사용해 병렬 처리)
            Executor executor = Executors.newFixedThreadPool(20);

            List<CompletableFuture<Void>> futures = selectedHeritageItems.stream()
                    .map(item -> CompletableFuture.runAsync(() -> {
                        try {
                            HeritageDetailDto detail = heritageService.fetchHeritageSimpleDetailByAsno(
                                    item.getCcbaAsno(), item.getCcbaKdcd(), item.getCcbaCtcd()
                            );
                            if (detail != null) {
                                item.setImageUrl(detail.getImageUrl());
                                item.setCcmaName(detail.getCcmaName());
                                item.setCcbaMnm1(detail.getCcbaMnm1());
                                item.setCcbaCtcdNm(detail.getCcbaCtcdNm());
                                item.setCcsiName(detail.getCcsiName());
                            }
                        } catch (Exception e) {
                            logger.error("Error fetching heritage detail for item: {}", item.getCcbaAsno(), e);
                        }
                    }, executor))
                    .collect(Collectors.toList());

// 모든 비동기 작업이 완료될 때까지 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

// Executor 종료
            ((ExecutorService) executor).shutdown();

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
