package com.Kooont.HeritageLoad.service;

import com.Kooont.HeritageLoad.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class HeritageService {
    private final RestTemplate restTemplate;

    @Value("${data-key}")
    private String data_key;

    @Value("${kakao-api-key}")
    private String kakaoApiKey;

    private String BASE_URL = "https://apis.data.go.kr/B551011/KorService1/locationBasedList1";

    private static final Logger logger = LoggerFactory.getLogger(GungListService.class);

    public HeritageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ccbaCtcd에 맞는 국보 리스트를 가져오는 메서드
    public List<HeritageItemDto> fetchHeritageItemsByCtcd(String ccbaCtcd) {
        logger.info("fetchHeritageItemsByCtcd for ccbaCtcd: {}", ccbaCtcd);

        // API 요청을 통해 지역에 해당하는 국보 데이터를 가져옴
        String url = "http://www.khs.go.kr/cha/SearchKindOpenapiList.do?pageUnit=100&ccbaCncl=N&ccbaKdcd=11&ccbaCtcd=" + ccbaCtcd;
        String xmlData = restTemplate.getForObject(url, String.class);
        List<HeritageItemDto> heritageItems = parseHeritageXml(xmlData);

        // 필터링: 이미지 URL이 필요한 국보만 추출
        List<HeritageItemDto> itemsToFetchDetails = heritageItems.stream()
                .filter(item -> item.getCcbaAsno() != null && !item.getCcbaAsno().isEmpty())
                .collect(Collectors.toList());

        // 병렬 처리용 Executor 생성 (최대 10개의 스레드를 사용해 병렬 처리)
        Executor executor = Executors.newFixedThreadPool(10);

        // 각 항목에 대해 비동기로 상세 정보 가져오기 및 이미지 URL 설정
        List<CompletableFuture<Void>> futures = itemsToFetchDetails.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    try {
                        HeritageDetailDto detail = fetchHeritageSimpleDetailByAsno(item.getCcbaAsno(), item.getCcbaKdcd(), item.getCcbaCtcd());
                        if (detail != null) {
                            item.setImageUrl(detail.getImageUrl());
                        }
                    } catch (Exception e) {
                        logger.error("Error fetching details for ccbaAsno: {}", item.getCcbaAsno(), e);
                    }
                }, executor))
                .collect(Collectors.toList());

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Executor 종료
        ((ExecutorService) executor).shutdown();

        return heritageItems;
    }
    //랜덤으로 10개의 국보 리스트 가져오기
    public List<HeritageItemDto> fetchHomeRandomHeritageItems() {
        logger.info("fetchHomeRandomHeritageItems");

        // 총 359개의 데이터가 있을 때 페이지당 10개라면 총 36 페이지
        int totalPages = 36;
        Random random = new Random();
        int randomPage = random.nextInt(totalPages) + 1;  // 1부터 totalPages 사이의 랜덤 페이지 선택

        // 랜덤 페이지에서 데이터 가져오기
        String url = "http://www.khs.go.kr/cha/SearchKindOpenapiList.do?pageUnit=10&pageIndex=" + randomPage + "&ccbaCncl=N&ccbaKdcd=11";
        String xmlData = restTemplate.getForObject(url, String.class);
        List<HeritageItemDto> heritageItems = parseHeritageXml(xmlData);

        // 리스트에서 랜덤으로 10개의 항목 선택
        Collections.shuffle(heritageItems);
        List<HeritageItemDto> randomItems = heritageItems.subList(0, Math.min(10, heritageItems.size()));

        // 병렬 처리용 Executor 생성 (최대 10개의 스레드를 사용해 병렬 처리)
        Executor executor = Executors.newFixedThreadPool(10);

        // 각 항목에 대해 비동기로 상세 정보 가져오기 및 이미지 URL 설정
        List<CompletableFuture<Void>> futures = randomItems.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    HeritageDetailDto detail = fetchHeritageSimpleDetailByAsno(item.getCcbaAsno(), item.getCcbaKdcd(), item.getCcbaCtcd());
                    if (detail != null) {
                        item.setImageUrl(detail.getImageUrl());
                    }
                }, executor))
                .collect(Collectors.toList());

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        ((ExecutorService) executor).shutdown();

        return randomItems;
    }

    //랜덤으로 10개의 보물 리스트 가져오기
    public List<HeritageItemDto> fetchHomeRandomTreasureItems() {
        logger.info("fetchHomeRandomTreasureItems");

        // 총 359개의 데이터가 있을 때 페이지당 10개라면 총 36 페이지
        int totalPages = 242;
        Random random = new Random();
        int randomPage = random.nextInt(totalPages) + 1;  // 1부터 totalPages 사이의 랜덤 페이지 선택

        // 랜덤 페이지에서 데이터 가져오기
        String url = "http://www.khs.go.kr/cha/SearchKindOpenapiList.do?pageUnit=10&pageIndex=" + randomPage + "&ccbaCncl=N&ccbaKdcd=12";
        String xmlData = restTemplate.getForObject(url, String.class);
        List<HeritageItemDto> heritageItems = parseHeritageXml(xmlData);

        // 리스트에서 랜덤으로 10개의 항목 선택
        Collections.shuffle(heritageItems);
        List<HeritageItemDto> randomItems = heritageItems.subList(0, Math.min(10, heritageItems.size()));

        // 병렬 처리용 Executor 생성 (최대 10개의 스레드를 사용해 병렬 처리)
        Executor executor = Executors.newFixedThreadPool(10);

        // 각 항목에 대해 비동기로 상세 정보 가져오기 및 이미지 URL 설정
        List<CompletableFuture<Void>> futures = randomItems.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    HeritageDetailDto detail = fetchHeritageSimpleDetailByAsno(item.getCcbaAsno(), item.getCcbaKdcd(), item.getCcbaCtcd());
                    if (detail != null) {
                        item.setImageUrl(detail.getImageUrl());
                    }
                }, executor))
                .collect(Collectors.toList());

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        ((ExecutorService) executor).shutdown();

        return randomItems;
    }

    //랜덤으로 10개의 사적 리스트 가져오기
    public List<HeritageItemDto> fetchHomeRandomHistoricItems() {
        logger.info("fetchHomeRandomTreasureItems");

        // 총 359개의 데이터가 있을 때 페이지당 10개라면 총 36 페이지
        int totalPages = 57;
        Random random = new Random();
        int randomPage = random.nextInt(totalPages) + 1;  // 1부터 totalPages 사이의 랜덤 페이지 선택

        // 랜덤 페이지에서 데이터 가져오기
        String url = "http://www.khs.go.kr/cha/SearchKindOpenapiList.do?pageUnit=10&pageIndex=" + randomPage + "&ccbaCncl=N&ccbaKdcd=13";
        String xmlData = restTemplate.getForObject(url, String.class);
        List<HeritageItemDto> heritageItems = parseHeritageXml(xmlData);

        // 리스트에서 랜덤으로 10개의 항목 선택
        Collections.shuffle(heritageItems);
        List<HeritageItemDto> randomItems = heritageItems.subList(0, Math.min(10, heritageItems.size()));

        // 병렬 처리용 Executor 생성 (최대 10개의 스레드를 사용해 병렬 처리)
        Executor executor = Executors.newFixedThreadPool(10);

        // 각 항목에 대해 비동기로 상세 정보 가져오기 및 이미지 URL 설정
        List<CompletableFuture<Void>> futures = randomItems.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    HeritageDetailDto detail = fetchHeritageSimpleDetailByAsno(item.getCcbaAsno(), item.getCcbaKdcd(), item.getCcbaCtcd());
                    if (detail != null) {
                        item.setImageUrl(detail.getImageUrl());
                    }
                }, executor))
                .collect(Collectors.toList());

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        ((ExecutorService) executor).shutdown();

        return randomItems;
    }


    // 검색하기
    public HeritageResponseDto fetchHeritageSearch(String pageIndex, String pageUnit, String ccbaMnm1) {
        // 1. 국보 리스트
        String url = "https://www.khs.go.kr/cha/SearchKindOpenapiList.do?pageIndex=" + pageIndex + "&pageUnit=" + pageUnit + "&ccbaCncl=N&ccbaMnm1=" + ccbaMnm1;
        String xmlData = restTemplate.getForObject(url, String.class);

        int totalCnt = parseTotalCntFromXml(xmlData);

        // 2. XML 데이터를 파싱하여 HeritageItemDto 리스트로 변환
        List<HeritageItemDto> heritageItems = parseHeritageXml(xmlData);

        // 3. 필터링: ccbaAsno가 존재하는 항목만 추출
        List<HeritageItemDto> itemsToFetchDetails = heritageItems.stream()
                .filter(item -> item.getCcbaAsno() != null && !item.getCcbaAsno().isEmpty())
                .collect(Collectors.toList());

        // 4. 병렬 처리용 Executor 생성 (최대 10개의 스레드를 사용해 병렬 처리)
        Executor executor = Executors.newFixedThreadPool(10);

        // 5. 각 항목에 대해 비동기로 상세 정보 가져오기 및 이미지 URL 설정
        List<CompletableFuture<Void>> futures = itemsToFetchDetails.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    try {
                        // 6. 상세 정보 가져오기
                        HeritageDetailDto detail = fetchHeritageSimpleDetailByAsno(item.getCcbaAsno(), item.getCcbaKdcd(), item.getCcbaCtcd());
                        if (detail != null) {
                            // 7. 이미지 URL 설정
                            item.setImageUrl(detail.getImageUrl());
                        }
                    } catch (Exception e) {
                        // 에러 발생 시 로그 출력
                        logger.error("Error fetching details for ccbaAsno: {}", item.getCcbaAsno(), e);
                    }
                }, executor))
                .collect(Collectors.toList());

        // 8. 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 9. Executor 종료
        ((ExecutorService) executor).shutdown();

        // 10. 필터링된 항목 반환 (이미지 URL이 있는 항목만 반환 가능하도록 할 수도 있음)
        return new HeritageResponseDto(totalCnt, heritageItems);
    }


    // 국보의 상세 정보를 ccbaAsno로 요청하여 가져오기
    public HeritageDetailDto fetchHeritageDetailByAsno(String ccbaAsno, String ccbaKdcd, String ccbaCtcd) {
        logger.info("fetchHeritageDetailByAsno: ccbaAsno={}", ccbaAsno);

        try {
            // XML 데이터를 가져오기 위한 URL 구성
            // 상세 정보 데이터 가져오기
            String detailUrl = "http://www.khs.go.kr/cha/SearchKindOpenapiDt.do?ccbaKdcd="
                    + ccbaKdcd + "&ccbaAsno=" + ccbaAsno + "&ccbaCtcd=" + ccbaCtcd;

            // XML 데이터를 String으로 가져옴
            String xmlData = restTemplate.getForObject(detailUrl, String.class);
            HeritageDetailDto detail = parseHeritageDetailXml(xmlData);

            if (detail.getLongitude() == 0) {
                logger.info("Longitude is 0 for ccbaAsno: {}. Applying alternative logic.", ccbaAsno);

                // Kakao API 호출을 위한 URL 구성
                String query = URLEncoder.encode(extractBeforeComma(detail.getCcbaLcad()), StandardCharsets.UTF_8); // 예시 주소
                String url = "https://dapi.kakao.com/v2/local/search/address.json?query=" + extractBeforeComma(detail.getCcbaLcad());
                logger.info("Longitude is 0 for query: {}. Applying alternative logic.", query);

                // HTTP Headers 설정
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey); // 'Authorization' 키는 대문자로 설정

                // 요청 엔티티 생성
                HttpEntity<String> entity = new HttpEntity<>(headers);

                try {
                    // GET 요청 보내기
                    ResponseEntity<String> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

                    // 응답 확인
                    if (response.getStatusCode() == HttpStatus.OK) {
                        logger.info("Kakao API response: {}", response.getBody());

                        // 응답 본문을 JsonNode로 변환 (Jackson 사용)
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(response.getBody());

                        // documents 배열에서 첫 번째 요소의 x, y 값 가져오기
                        JsonNode documents = root.path("documents");
                        if (documents.isArray() && documents.size() > 0) {
                            JsonNode firstDoc = documents.get(0); // 첫 번째 문서 선택
                            String longitude = firstDoc.path("x").asText();  // 경도 (x)
                            String latitude = firstDoc.path("y").asText();   // 위도 (y)

                            // 파싱한 경도와 위도를 detail 객체에 설정
                            detail.setLongitude(Double.parseDouble(longitude));
                            detail.setLatitude(Double.parseDouble(latitude));

                            logger.info("Updated HeritageDetailDto with longitude: {}, latitude: {}", longitude, latitude);
                        }

                    } else {
                        logger.error("Kakao API 호출 실패: {}", response.getStatusCode());
                    }

                } catch (Exception e) {
                    logger.error("Error during Kakao API request", e);
                    throw new RuntimeException("Kakao API 호출 중 오류 발생", e);
                }
            }

            // 영상 데이터 가져오기 (단일 비디오 URL)
            String videoUrl = "https://www.khs.go.kr/cha/SearchVideoOpenapi.do?ccbaKdcd="
                    + ccbaKdcd + "&ccbaAsno=" + ccbaAsno + "&ccbaCtcd=" + ccbaCtcd + "&ccbaGbn=kr";
            String videoData = restTemplate.getForObject(videoUrl, String.class);
            logger.info("Received Video Data: {}", videoData);  // 로그로 확인

            // 비디오 URL 파싱 후 단일 URL로 설정
            String parsedVideoUrl = parseSingleHeritageVideoXml(videoData);
            detail.setVideoUrl(parsedVideoUrl);

            // 이미지 데이터 가져오기
            String imageUrl = "https://www.khs.go.kr/cha/SearchImageOpenapi.do?ccbaKdcd="
                    + ccbaKdcd + "&ccbaAsno=" + ccbaAsno + "&ccbaCtcd=" + ccbaCtcd;
            String imageData = restTemplate.getForObject(imageUrl, String.class);
            List<ImageDto> imageList = parseHeritageImageXml(imageData); // 이미지 데이터 파싱
            detail.setImages(imageList);

            return detail;
        } catch (Exception e) {
            logger.error("Error fetching or parsing heritage detail", e);
            return null;
        }
    }
    public Map<String, List<RelatedAttractionDto>> fetchHeritageDetailRelatedAttractions(double mapX, double mapY)
            throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {

        // UTF-8로 인코딩된 API 키
        String encodedKey = URLEncoder.encode(data_key, "UTF-8");

        // API 요청 URL 생성
        String[] contentTypes = {"12", "32", "39"}; // 관광지, 숙박, 음식점 contentTypeId
        String[] typeNames = {"relatedAttractions", "accommodations", "restaurants"}; // 반환할 JSON 키 이름
        Map<String, List<RelatedAttractionDto>> resultMap = new HashMap<>();

        for (int i = 0; i < contentTypes.length; i++) {
            String url = String.format(
                    "https://apis.data.go.kr/B551011/KorService1/locationBasedList1?serviceKey=%s&numOfRows=20&pageNo=1&MobileOS=ETC&MobileApp=HeritageLoad&_type=json&listYN=Y&arrange=A&mapX=%s&mapY=%s&radius=5000&contentTypeId=%s",
                    encodedKey, mapX, mapY, contentTypes[i]
            );

            // URI 생성 및 API 요청
            URI uri = new URI(url);
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);

            // 응답 문자열을 로그로 출력
            String jsonString = responseEntity.getBody();
            System.out.println("API Response: " + jsonString); // 응답 내용 로그 출력

            // JSON 데이터를 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonString);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            // RelatedAttractionDto 리스트 생성
            List<RelatedAttractionDto> relatedItems = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String contentId = item.path("contentid").asText(null);
                    String title = item.path("title").asText(null);
                    String addr1 = item.path("addr1").asText(null);
                    String addr2 = item.path("addr2").asText(null);
                    String firstImage = item.path("firstimage").asText(null);
                    String mapXStr = item.path("mapx").asText(null);
                    String mapYStr = item.path("mapy").asText(null);
                    String contentTypeId = item.path("contenttypeid").asText(null);

                    // addr1에서 시/도와 구/군 정보를 추출
                    String areaName = getSimpleAreaName(extractAreaNameFromAddr(addr1));  // 시/도
                    logger.info("areaName: {}", areaName);
                    String districtName = extractDistrictFromAddr(addr1);  // 구/군
                    String addr3 = (areaName != null ? areaName : "") + " " + (districtName != null ? districtName : "");  // addr3에 시/도 + 구/군 설정

                    // firstImage가 비어 있지 않을 때만 DTO로 변환하여 리스트에 추가
                    if (firstImage != null && !firstImage.isEmpty()) {
                        RelatedAttractionDto attraction = new RelatedAttractionDto(contentId, title, addr1, addr2, addr3, firstImage, mapXStr, mapYStr, contentTypeId);
                        relatedItems.add(attraction);

                        // 최대 10개만 포함되도록 제한
                        if (relatedItems.size() >= 10) {
                            break;  // 10개가 되면 반복문 종료
                        }
                    }
                }
            }

            // 결과 맵에 추가
            resultMap.put(typeNames[i], relatedItems);
        }

        return resultMap;  // 관련 관광지, 숙박, 음식점 DTO 리스트를 포함하는 맵 반환
    }

    public Map<String, List<RelatedAttractionDto>> fetchHeritageDetailRelatedAttractionsArea(Integer maxCount, String areaCode, String sigunguCode)
            throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
        // UTF-8로 인코딩된 API 키
        String encodedKey = URLEncoder.encode(data_key, "UTF-8");
        String originAreaCode = getSimpleAreaName(areaCode);
        String transAreaCode = getOfficialAreaCode(originAreaCode);
        String sigunguCodeToNumber ="";
        String url = String.format(
                "https://apis.data.go.kr/B551011/KorService1/areaCode1?serviceKey=%s&numOfRows=%s&pageNo=1&MobileOS=ETC&MobileApp=HeritageLoad&areaCode=%s&_type=json",
                encodedKey, 100, transAreaCode);

        // API 요청
        URI sigunguUri = new URI(url);
        ResponseEntity<String> sigunguResponse = restTemplate.getForEntity(sigunguUri, String.class);

        // 응답 문자열 출력 (디버그용)
        String sigunguJson = sigunguResponse.getBody();
        System.out.println("Sigungu API Response: " + sigunguJson);

        // JSON 데이터를 파싱하여 sigunguCode 찾기
        ObjectMapper mapper = new ObjectMapper();
        JsonNode sigunguRootNode = mapper.readTree(sigunguJson);
        JsonNode sigunguItemsNode = sigunguRootNode.path("response").path("body").path("items").path("item");

        if (sigunguItemsNode.isArray()) {
            for (JsonNode item : sigunguItemsNode) {
                String name = item.path("name").asText(null); // 구/군 이름
                String code = item.path("code").asText(null); // 해당 구/군의 코드

                // 입력된 sigunguCode와 name을 비교
                if (sigunguCode.equals(name)) {
                    sigunguCodeToNumber = code;  // 일치하는 코드 찾기
                    break;
                }
            }
        }

        // sigunguCode를 찾았는지 여부를 확인
        if (sigunguCodeToNumber == null) {
            throw new IllegalArgumentException("입력된 구/군 이름에 해당하는 코드가 없습니다.");
        }


        // API 요청 URL 생성
        String[] contentTypes = {"12", "32", "39"}; // 관광지, 숙박, 음식점 contentTypeId
        String[] typeNames = {"relatedAttractions", "accommodations", "restaurants"}; // 반환할 JSON 키 이름
        Map<String, List<RelatedAttractionDto>> resultMap = new HashMap<>();

        for (int i = 0; i < contentTypes.length; i++) {
            String url1 = String.format(
                    "https://apis.data.go.kr/B551011/KorService1/areaBasedList1?serviceKey=%s&numOfRows=%s&pageNo=1&MobileOS=ETC&MobileApp=HeritageLoad&areaCode=%s&sigunguCode=%s&_type=json&listYN=Y&arrange=Q&contentTypeId=%s",
                    encodedKey, maxCount, transAreaCode, sigunguCodeToNumber, contentTypes[i]
            );

            // URI 생성 및 API 요청
            URI uri = new URI(url1);
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);

            // 응답 문자열을 로그로 출력
            String jsonString = responseEntity.getBody();
            System.out.println("API Response: " + jsonString); // 응답 내용 로그 출력

            // JSON 데이터를 파싱
            ObjectMapper mapper1 = new ObjectMapper();
            JsonNode rootNode = mapper1.readTree(jsonString);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            // RelatedAttractionDto 리스트 생성
            List<RelatedAttractionDto> relatedItems = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String contentId = item.path("contentid").asText(null);
                    String title = item.path("title").asText(null);
                    String addr1 = item.path("addr1").asText(null);
                    String addr2 = item.path("addr2").asText(null);
                    String firstImage = item.path("firstimage").asText(null);
                    String mapXStr = item.path("mapx").asText(null);
                    String mapYStr = item.path("mapy").asText(null);
                    String contentTypeId = item.path("contenttypeid").asText(null);

                    // addr1에서 시/도와 구/군 정보를 추출
                    String areaName = getSimpleAreaName(extractAreaNameFromAddr(addr1));  // 시/도
                    logger.info("areaName: {}", areaName);
                    String districtName = extractDistrictFromAddr(addr1);  // 구/군
                    String addr3 = (areaName != null ? areaName : "") + " " + (districtName != null ? districtName : "");  // addr3에 시/도 + 구/군 설정

                    // firstImage가 비어 있지 않을 때만 DTO로 변환하여 리스트에 추가
                    if (firstImage != null && !firstImage.isEmpty()) {
                        RelatedAttractionDto attraction = new RelatedAttractionDto(contentId, title, addr1, addr2, addr3, firstImage, mapXStr, mapYStr, contentTypeId);
                        relatedItems.add(attraction);

                        // 최대 10개만 포함되도록 제한
                        if (relatedItems.size() >= maxCount) {
                            break;  // 10개가 되면 반복문 종료
                        }
                    }
                }
            }

            // 결과 맵에 추가
            resultMap.put(typeNames[i], relatedItems);
        }

        return resultMap;  // 관련 관광지, 숙박, 음식점 DTO 리스트를 포함하는 맵 반환
    }

    public HeritageDetailDto fetchHeritageSimpleDetailByAsno(String ccbaAsno, String ccbaKdcd, String ccbaCtcd) {
        logger.info("fetchHeritageDetailByAsno: ccbaAsno={}", ccbaAsno);

        try {
            // XML 데이터를 가져오기 위한 URL 구성
            String detailUrl = "http://www.khs.go.kr/cha/SearchKindOpenapiDt.do?ccbaKdcd="
                    + ccbaKdcd + "&ccbaAsno=" + ccbaAsno + "&ccbaCtcd=" + ccbaCtcd;

            // XML 데이터를 String으로 가져옴
            String xmlData = restTemplate.getForObject(detailUrl, String.class);

            // XML 데이터를 파싱해서 HeritageDetailDto로 변환
            return parseHeritageDetailXml(xmlData);
        } catch (Exception e) {
            logger.error("Error fetching or parsing heritage detail", e);
            return null;
        }
    }

    public HeritageResponseDto fetchHomeAllHeritageItems(String pageIndex, String pageUnit, String ccbaKdcd) {

        String url = "http://www.khs.go.kr/cha/SearchKindOpenapiList.do?pageUnit=" + pageUnit + "&pageIndex=" + pageIndex + "&ccbaCncl=N&ccbaKdcd=" + ccbaKdcd;
        String xmlData = restTemplate.getForObject(url, String.class);

        // totalCnt 파싱
        int totalCnt = parseTotalCntFromXml(xmlData);

        List<HeritageItemDto> heritageItems = parseHeritageXml(xmlData);

        // 병렬 처리용 Executor 생성 (최대 10개의 스레드를 사용해 병렬 처리)
        Executor executor = Executors.newFixedThreadPool(10);

        // 각 항목에 대해 비동기로 상세 정보 가져오기 및 이미지 URL 설정
        List<CompletableFuture<Void>> futures = heritageItems.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    HeritageDetailDto detail = fetchHeritageDetailByAsno(item.getCcbaAsno(), item.getCcbaKdcd(), item.getCcbaCtcd());
                    if (detail != null) {
                        item.setImageUrl(detail.getImageUrl());
                    }
                }, executor))
                .collect(Collectors.toList());

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        ((ExecutorService) executor).shutdown();

        return new HeritageResponseDto(totalCnt, heritageItems);
    }

    // XML 데이터 파싱(리스트)
    public List<HeritageItemDto> parseHeritageXml(String xmlData) {
        List<HeritageItemDto> heritageList = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlData)));

            NodeList itemList = document.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                Element itemElement = (Element) itemList.item(i);

                HeritageItemDto dto = new HeritageItemDto();
//                dto.setSn(Integer.parseInt(getTagValue("sn", itemElement)));
                dto.setCcmaName(getTagValue("ccmaName", itemElement));
                dto.setCcbaMnm1(getTagValue("ccbaMnm1", itemElement));
                dto.setCcbaCtcdNm(getTagValue("ccbaCtcdNm", itemElement));
                dto.setCcsiName(getTagValue("ccsiName", itemElement));
//                dto.setCcbaAdmin(getTagValue("ccbaAdmin", itemElement));
                dto.setCcbaKdcd(getTagValue("ccbaKdcd", itemElement));
                dto.setCcbaCtcd(getTagValue("ccbaCtcd", itemElement));
                dto.setCcbaAsno(getTagValue("ccbaAsno", itemElement));
//                dto.setCcbaCncl(getTagValue("ccbaCncl", itemElement));
//                dto.setLongitude(Double.parseDouble(getTagValue("longitude", itemElement)));
//                dto.setLatitude(Double.parseDouble(getTagValue("latitude", itemElement)));
//                dto.setRegDt(getTagValue("regDt", itemElement));

                heritageList.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return heritageList;
    }

    private int parseTotalCntFromXml(String xmlData) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlData)));

            Element totalCntElement = (Element) document.getElementsByTagName("totalCnt").item(0);
            if (totalCntElement != null) {
                return Integer.parseInt(totalCntElement.getTextContent());
            }
        } catch (Exception e) {
            logger.error("Error parsing totalCnt from XML", e);
        }
        return 0;  // 에러 발생 시 기본 값 0 반환
    }

    // XML 데이터 파싱 (상세 정보)
    private HeritageDetailDto parseHeritageDetailXml(String xmlData) {
        HeritageDetailDto detail = new HeritageDetailDto();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlData)));

            Element itemElement = (Element) document.getElementsByTagName("item").item(0);

            if (itemElement != null) {
                detail.setCcbaLcad(getTagValue("ccbaLcad", itemElement));  // 로깅 추가
                logger.info("ccbaLcad Value: {}", detail.getCcbaLcad());  // 로깅 확인
                // 기본 정보
                detail.setCcbaKdcd(getTagValue("ccbaKdcd", document.getDocumentElement()));
                detail.setCcbaAsno(getTagValue("ccbaAsno", document.getDocumentElement()));
                detail.setCcbaCtcd(getTagValue("ccbaCtcd", document.getDocumentElement()));
                detail.setCcbaCpno(getTagValue("ccbaCpno", document.getDocumentElement()));
                detail.setLongitude(parseDoubleOrDefault(getTagValue("longitude", document.getDocumentElement()), 0.0));
                detail.setLatitude(parseDoubleOrDefault(getTagValue("latitude", document.getDocumentElement()), 0.0));

                // item 태그 내 정보
                detail.setCcmaName(getTagValue("ccmaName", itemElement));
                detail.setCcbaMnm1(getTagValue("ccbaMnm1", itemElement));
                detail.setCcbaMnm2(getTagValue("ccbaMnm2", itemElement));
                detail.setGcodeName(getTagValue("gcodeName", itemElement));
                detail.setBcodeName(getTagValue("bcodeName", itemElement));
                detail.setMcodeName(getTagValue("mcodeName", itemElement));
                detail.setScodeName(getTagValue("scodeName", itemElement));
                detail.setCcbaQuan(getTagValue("ccbaQuan", itemElement));
                detail.setCcbaAsdt(getTagValue("ccbaAsdt", itemElement));
                detail.setCcbaCtcdNm(getTagValue("ccbaCtcdNm", itemElement));
                detail.setCcsiName(getTagValue("ccsiName", itemElement));
                detail.setCcbaLcad(getTagValue("ccbaLcad", itemElement));
                detail.setCcceName(getTagValue("ccceName", itemElement));
                detail.setCcbaPoss(getTagValue("ccbaPoss", itemElement));
                detail.setCcbaAdmin(getTagValue("ccbaAdmin", itemElement));
                detail.setCcbaCncl(getTagValue("ccbaCncl", itemElement));
                detail.setCcbaCndt(getTagValue("ccbaCndt", itemElement));
                detail.setImageUrl(getTagValue("imageUrl", itemElement));
                detail.setContent(getTagValue("content", itemElement));
            } else {
                logger.error("Item element not found in the XML response.");
            }
        } catch (Exception e) {
            logger.error("Error parsing heritage detail XML", e);
        }

        return detail;
    }

    // XML 데이터 파싱 (영상)
    private List<String> parseHeritageVideoXml(String xmlData) {
        List<String> videoUrls = new ArrayList<>();
//        logger.info("Received video XML Data: {}", xmlData);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlData)));

            NodeList itemList = document.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                Element itemElement = (Element) itemList.item(i);
                logger.info("itemElement : {}", itemList);
                // 각 <item> 안의 <videoUrl> 태그 값을 가져옴
                String videoUrl = getTagValue("videoUrl", itemElement);


                // 비어 있거나 기본 경로로 끝나는 URL인 경우 빈 값으로 처리
                if (videoUrl != null && !videoUrl.isEmpty() && !videoUrl.endsWith("/")) {
                    videoUrls.add(videoUrl);
                } else {
                    videoUrls.add(""); // 빈 값을 리스트에 추가
                    logger.warn("Video URL is empty or default path: {}", videoUrl);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing video XML", e);
        }
        return videoUrls;
    }

    // 비디오 XML 데이터에서 단일 비디오 URL을 파싱하는 메서드
    private String parseSingleHeritageVideoXml(String videoData) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(videoData)));

            // <item> 태그에서 첫 번째 비디오 URL 추출
            NodeList itemList = document.getElementsByTagName("item");
            if (itemList.getLength() > 0) {
                Element itemElement = (Element) itemList.item(0);

                // 각 <item> 안의 <videoUrl> 태그 값을 가져옴
                String videoUrl = getTagValue("videoUrl", itemElement);

                // 비어 있거나 기본 경로로 끝나는 URL인 경우 빈 값으로 처리
                if (videoUrl != null && !videoUrl.isEmpty() && !videoUrl.endsWith("/")) {
                    return videoUrl;
                } else {
                    logger.warn("Video URL is empty or default path: {}", videoUrl);
                    return ""; // 빈 값을 반환
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing single video XML", e);
        }
        return ""; // 예외 발생 시 빈 값 반환
    }

    // XML 데이터 파싱 (이미지)
    private List<ImageDto> parseHeritageImageXml(String xmlData) {
        List<ImageDto> imageList = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlData)));

            // item 태그 내에서 모든 자식 요소를 가져옴
            NodeList itemList = document.getElementsByTagName("item");

            for (int i = 0; i < itemList.getLength(); i++) {
                Element itemElement = (Element) itemList.item(i);

                // item 내에서 모든 sn 태그를 찾아서 처리
                NodeList snList = itemElement.getElementsByTagName("sn");

                for (int j = 0; j < snList.getLength(); j++) {
                    // 해당 sn에 연결된 imageUrl과 ccimDesc를 가져옴
                    String imageUrl = getTagValue("imageUrl", itemElement, j);
                    String description = getTagValue("ccimDesc", itemElement, j);

                    // 이미지 URL이 유효한 경우 리스트에 추가
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        ImageDto imageDto = new ImageDto();
                        imageDto.setImageUrl(imageUrl);
                        imageDto.setDescription(description);

                        // 로깅: 각각의 이미지 정보 확인
                        logger.info("Image URL: {}", imageUrl);
                        logger.info("Description: {}", description);

                        imageList.add(imageDto);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing image XML", e);
        }
        return imageList;
    }

    // 특정 태그값을 인덱스별로 가져오는 함수
    private String getTagValue(String tag, Element element, int index) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > index) {
            return nodeList.item(index).getTextContent();
        }
        return "";
    }




    // 빈 문자열이나 null일 경우 기본 값을 사용해 double로 파싱하는 메서드
    private double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse double value: " + value, e);
            return defaultValue;
        }
    }


    // XML 태그 값 추출
    private String getTagValue(String tag, Element element) {
        try {
            NodeList nodeList = element.getElementsByTagName(tag);
            if (nodeList != null && nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                if (node != null) {
                    // getTextContent()를 사용하여 모든 텍스트와 CDATA 섹션을 가져옴
                    String value = node.getTextContent().replaceAll("\\s+", " ").trim();
//                    logger.info("Tag: {}, Value: {}", tag, value); // 로깅 추가
                    return value;
                }
            }
        } catch (Exception e) {
            logger.error("Error getting tag value for tag: " + tag, e);
        }
        return "";  // 빈 문자열 반환
    }

    private String extractAreaNameFromAddr(String addr) {
        if (addr == null || addr.isEmpty()) return null;
        // 예시: "서울특별시 중구 을지로 66" -> "서울특별시"
        String[] parts = addr.split(" ");

        return parts.length > 0 ? parts[0] : null;  // 첫 번째 요소를 지역명으로 반환
    }

    // 구/군 정보 추출
    private String extractDistrictFromAddr(String addr) {
        if (addr == null || addr.isEmpty()) return null;

        // 예시: "서울특별시 중구 을지로 66" -> "중구"
        String[] parts = addr.split(" ");
        return parts.length > 1 ? parts[1] : null;  // 두 번째 요소를 구/군으로 간주
    }

    // 지역명을 간단하게 변환하는 메서드
    private String getSimpleAreaName(String areaName) {
        if (areaName == null) return null;

        switch (areaName) {
            case "서울특별시", "서울", "서울시" : return "서울";
            case "부산광역시", "부산", "부산시": return "부산";
            case "대구광역시", "대구", "대구시": return "대구";
            case "인천광역시", "인천", "인천시": return "인천";
            case "광주광역시", "광주", "광주시": return "광주";
            case "울산광역시", "울산", "울산시": return "울산";
            case "세종특별자치시", "세종", "세종시": return "세종";
            case "경기도", "경기": return "경기";
            case "강원특별자치도", "강원", "강원도": return "강원";
            case "충청북도", "충북", "충북도": return "충북";
            case "충청남도", "충남", "충남도": return "충남";
            case "전북특별자치도", "전북", "전라북도", "전북도": return "전북";
            case "전라남도", "전남", "전남도": return "전남";
            case "경상북도", "경북", "경북도" : return "경북";
            case "경상남도", "경남", "경남도": return "경남";
            case "제주특별자치도", "제주시", "제주도": return "제주";
            default: return areaName;  // 기본적으로 그대로 반환 (일치하지 않는 경우)
        }
    }

    public static String extractBeforeComma(String address) {
        // 쉼표 기준으로 문자열을 자름
        int index = address.indexOf(',');
        if (index != -1) {
            return address.substring(0, index);  // 쉼표 앞의 부분을 반환
        } else {
            return address;  // 쉼표가 없으면 전체 주소 반환
        }
    }

    private String getOfficialAreaCode(String areaCode) {
        // 시도명 -> 시도 코드 변환 로직
        Map<String, String> areaMap = new HashMap<>();
        // 시도를 공식 명칭으로 매핑
        areaMap.put("서울", "1");
        areaMap.put("부산", "6");
        areaMap.put("대구", "4");
        areaMap.put("인천", "2");
        areaMap.put("광주", "5");
        areaMap.put("대전", "3");
        areaMap.put("울산", "7");
        areaMap.put("세종", "8");
        areaMap.put("경기", "31");
        areaMap.put("강원", "32");
        areaMap.put("충북", "33");
        areaMap.put("충남", "34");
        areaMap.put("전북", "37");
        areaMap.put("전남", "38");
        areaMap.put("경북", "35");
        areaMap.put("경남", "36");
        areaMap.put("제주", "39");

        return areaMap.getOrDefault(areaCode, areaCode); // 매핑되지 않으면 그대로 반환
    }

}
