package com.Kooont.HeritageLoad.service;

import com.Kooont.HeritageLoad.dto.TouristPopularDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TouristPopularDataService {

    @Value("${data-key}")
    private String data_key;

    private String BASE_URL = "https://apis.data.go.kr/B551011/DataLabService/metcoRegnVisitrDDList";

    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(GungListService.class);

    public TouristPopularDataService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<TouristPopularDataDto> fetchTouristPopulars() {

        List<TouristPopularDataDto> externalTouristAreas = new ArrayList<>();

        // 날짜 계산
        LocalDate now = LocalDate.now();  // 현재 날짜
        LocalDate firstDayOfLastMonth = now.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());  // 저번달 1일
        LocalDate lastDayOfLastMonth = now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());   // 저번달 마지막 날

        // 날짜를 "yyyyMMdd" 형식으로 포맷팅
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String startYmd = firstDayOfLastMonth.format(formatter);
//        String endYmd = lastDayOfLastMonth.format(formatter);

        try {
            // XML 파싱 준비
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            String encodedKey = URLEncoder.encode(data_key, "UTF-8");  // UTF-8로 인코딩
            String url = BASE_URL + "?serviceKey=" + encodedKey
                    + "&numOfRows=51"
                    + "&pageNo=" + "1"
                    + "&MobileOS=ETC"
                    + "&MobileApp=AppTest"
                    + "&startYmd=" + startYmd
                    + "&endYmd=" + startYmd;

//            logger.info("fetchTouristPopulars url : {} ", url);

            // API 요청을 전송하고 응답을 받음
            URI uri = new URI(url);
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
            String jsonString = new String(responseEntity.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);


//            logger.info("Raw API Response: {}", jsonString);
            Document document = builder.parse(new InputSource(new StringReader(jsonString)));
            // 모든 item 태그를 가져옴
            NodeList itemList = document.getElementsByTagName("item");



            // 각 item 태그를 순회하며 외지인(touDivCd=2) 정보 파싱
            for (int i = 0; i < itemList.getLength(); i++) {
                Element itemElement = (Element) itemList.item(i);

                // touDivCd 값을 확인
                String touDivCd = getTagValue("touDivCd", itemElement);
                logger.info("fetchTouristPopulars touDivCd : {} ", touDivCd);

                // 외지인 (touDivCd = 2)만 필터링
                if ("2".equals(touDivCd)) {
                    TouristPopularDataDto touristPopularDataDto = new TouristPopularDataDto();
                    touristPopularDataDto.setAreaNm(getTagValue("areaNm", itemElement));
                    touristPopularDataDto.setDaywkDivNm(getTagValue("daywkDivNm", itemElement));
                    touristPopularDataDto.setTouDivNm(getTagValue("touDivNm", itemElement));
                    touristPopularDataDto.setTouNum(Double.parseDouble(getTagValue("touNum", itemElement)));
                    touristPopularDataDto.setBaseYmd(getTagValue("baseYmd", itemElement));

                    externalTouristAreas.add(touristPopularDataDto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return externalTouristAreas;

    }

    // XML 태그 값 가져오는 헬퍼 메소드
    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodeList.item(0);
        return node != null ? node.getNodeValue() : "";
    }


}
