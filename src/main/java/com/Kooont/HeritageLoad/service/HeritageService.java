package com.Kooont.HeritageLoad.service;

import com.Kooont.HeritageLoad.dto.HeritageDetailDto;
import com.Kooont.HeritageLoad.dto.HeritageItemDto;
import com.Kooont.HeritageLoad.dto.HeritageResponseDto;
import com.Kooont.HeritageLoad.dto.ImageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class HeritageService {
    private final RestTemplate restTemplate;

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


    // 전체보기
//    public List<HeritageItemDto> fetchHeritageSearch(ccbaMnm1) {
//
//        String url = "http://www.khs.go.kr/cha/SearchKindOpenapiList.do?pageUnit=10&pageIndex=" + pageIndex + "&ccbaCncl=N&ccbaKdcd=" + ccbaKdcd;
//        String xmlData = restTemplate.getForObject(url, String.class);
//        List<HeritageItemDto> heritageItems = parseHeritageXml(xmlData);
//
//
//
//        // 병렬 처리용 Executor 생성 (최대 10개의 스레드를 사용해 병렬 처리)
//        Executor executor = Executors.newFixedThreadPool(10);
//
//        // 각 항목에 대해 비동기로 상세 정보 가져오기 및 이미지 URL 설정
//        List<CompletableFuture<Void>> futures = heritageItems.stream()
//                .map(item -> CompletableFuture.runAsync(() -> {
//                    HeritageDetailDto detail = fetchHeritageDetailByAsno(item.getCcbaAsno(), item.getCcbaKdcd(), item.getCcbaCtcd());
//                    if (detail != null) {
//                        item.setImageUrl(detail.getImageUrl());
//                    }
//                }, executor))
//                .collect(Collectors.toList());
//
//        // 모든 비동기 작업이 완료될 때까지 대기
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//        ((ExecutorService) executor).shutdown();
//
//        return heritageItems;
//    }

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
//            logger.info("Received XML Data: {}", xmlData);

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
}
