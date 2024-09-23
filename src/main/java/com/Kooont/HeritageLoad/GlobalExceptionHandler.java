package com.Kooont.HeritageLoad;

import com.Kooont.HeritageLoad.service.GungListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GungListService.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        // 로그로 오류 상세 정보를 기록
        logger.error("Error: ", e);

        return new ResponseEntity<>("Server error occurred. Please contact support.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
