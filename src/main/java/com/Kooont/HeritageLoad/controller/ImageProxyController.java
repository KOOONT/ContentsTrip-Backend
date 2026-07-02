package com.Kooont.HeritageLoad.controller;

import com.Kooont.HeritageLoad.service.ImageProxyService;
import com.Kooont.HeritageLoad.service.ImageProxyService.ProxiedImage;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class ImageProxyController {
    private final ImageProxyService imageProxyService;

    public ImageProxyController(ImageProxyService imageProxyService) {
        this.imageProxyService = imageProxyService;
    }

    @GetMapping("/image-proxy")
    public ResponseEntity<byte[]> proxyImage(@RequestParam("url") String imageUrl,
                                             @RequestParam(value = "w", required = false) Integer width) {
        ProxiedImage image = imageProxyService.getImage(imageUrl, width);
        return ResponseEntity.ok()
                .contentType(image.contentType())
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.VARY, "Accept")
                .body(image.bytes());
    }
}
