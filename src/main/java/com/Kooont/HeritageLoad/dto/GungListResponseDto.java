package com.Kooont.HeritageLoad.dto;

import lombok.Data;
import org.apache.catalina.connector.Response;

import java.util.List;

@Data
public class GungListResponseDto {
    private Response response;

    @Data
    public static class Response {
        private Header header;
        private Body body;  // body 필드를 정의하여, getBody()를 사용할 수 있게 합니다.

        @Data
        public static class Header {
            private String resultCode;
            private String resultMsg;
        }

        @Data
        public static class Body {
            private Items items;
            private int numOfRows;
            private int pageNo;
            private int totalCount;

            @Data
            public static class Items {
                private java.util.List<GungListItemDto> item;
            }
        }
    }
}
