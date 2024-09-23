package com.Kooont.HeritageLoad.dto;

import lombok.Data;

import java.util.List;

// 국보 리스트 상세 정보
@Data
public class HeritageDetailDto {
    private String ccbaKdcd;
    private String ccbaAsno;
    private String ccbaCtcd;
    private String ccbaCpno;
    private double longitude;
    private double latitude;
    private String ccmaName;
    private String ccbaMnm1;
    private String ccbaMnm2;
    private String gcodeName;
    private String bcodeName;
    private String mcodeName;
    private String scodeName;
    private String ccbaQuan;
    private String ccbaAsdt;
    private String ccbaCtcdNm;
    private String ccsiName;
    private String ccbaLcad;
    private String ccceName;
    private String ccbaPoss;
    private String ccbaAdmin;
    private String ccbaCncl;
    private String ccbaCndt;
    private String imageUrl;
    private String content;

    private String videoUrl;
    private List<ImageDto> images;


}
