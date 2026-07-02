package com.Kooont.HeritageLoad.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImageProxyService {
    private static final long CACHE_TTL_MILLIS = 12L * 60 * 60 * 1000;
    private static final int CACHE_MAX_ENTRIES = 700;
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final int MIN_WIDTH = 80;
    private static final int MAX_WIDTH = 1200;
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "www.khs.go.kr",
            "khs.go.kr",
            "tong.visitkorea.or.kr");

    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${app.public-base-url:https://api.heritageload.store}")
    private String publicBaseUrl;

    public ImageProxyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ProxiedImage getImage(String rawUrl, Integer requestedWidth) {
        String normalizedUrl = normalizeAndValidate(rawUrl);
        int width = normalizeWidth(requestedWidth);
        String cacheKey = normalizedUrl + "|w=" + width;
        long now = Instant.now().toEpochMilli();
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAtMillis > now) {
            return cached.image;
        }

        if (cached != null) {
            cache.remove(cacheKey, cached);
        }

        ProxiedImage image = loadImage(normalizedUrl, width);
        pruneCache();
        cache.put(cacheKey, new CacheEntry(image, now + CACHE_TTL_MILLIS));
        return image;
    }

    public void warmImages(List<String> imageUrls, Integer requestedWidth, int limit) {
        if (imageUrls == null || imageUrls.isEmpty() || limit <= 0) {
            return;
        }

        imageUrls.stream()
                .filter(Objects::nonNull)
                .filter(url -> !url.isBlank())
                .limit(limit)
                .forEach(url -> {
                    try {
                        getImage(url, requestedWidth);
                    } catch (IllegalArgumentException ignored) {
                        // Cache warming is best effort and must not affect API startup.
                    }
                });
    }

    public String toProxyUrl(String rawUrl, Integer requestedWidth) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }

        try {
            String normalizedUrl = normalizeAndValidate(sourceUrlForProxy(rawUrl));
            int width = normalizeWidth(requestedWidth);
            String encodedUrl = URLEncoder.encode(normalizedUrl, StandardCharsets.UTF_8);
            return normalizedPublicBaseUrl() + "/api/image-proxy?url=" + encodedUrl + "&w=" + width;
        } catch (IllegalArgumentException e) {
            return rawUrl.trim();
        }
    }

    private ProxiedImage loadImage(String imageUrl, int width) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15");
            headers.set(HttpHeaders.ACCEPT, "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    new URI(imageUrl),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class);

            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                throw new IllegalArgumentException("Image response is empty");
            }
            if (body.length > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("Image response is too large");
            }

            MediaType contentType = response.getHeaders().getContentType();
            return resizeIfPossible(body, width, contentType);
        } catch (RestClientException | URISyntaxException e) {
            throw new IllegalArgumentException("Failed to fetch image", e);
        }
    }

    private ProxiedImage resizeIfPossible(byte[] sourceBytes, int targetWidth, MediaType contentType) {
        if (targetWidth <= 0) {
            return new ProxiedImage(sourceBytes, resolveContentType(contentType));
        }

        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(sourceBytes));
            if (source == null || source.getWidth() <= targetWidth) {
                return new ProxiedImage(sourceBytes, resolveContentType(contentType));
            }

            int targetHeight = Math.max(1, Math.round(source.getHeight() * (targetWidth / (float) source.getWidth())));
            BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = resized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", output);
            return new ProxiedImage(output.toByteArray(), MediaType.IMAGE_JPEG);
        } catch (Exception e) {
            return new ProxiedImage(sourceBytes, resolveContentType(contentType));
        }
    }

    private String normalizeAndValidate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank() || rawUrl.length() > 2000) {
            throw new IllegalArgumentException("Image URL is required");
        }

        try {
            URI uri = new URI(rawUrl.trim());
            String host = uri.getHost();
            if (host == null || !ALLOWED_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Image host is not allowed");
            }

            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Image scheme is not allowed");
            }

            return new URI("https", uri.getUserInfo(), host, uri.getPort(), uri.getPath(), uri.getQuery(), null)
                    .toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Image URL is invalid", e);
        }
    }

    private String sourceUrlForProxy(String rawUrl) {
        String proxiedSourceUrl = extractProxiedSourceUrl(rawUrl);
        return proxiedSourceUrl == null ? rawUrl : proxiedSourceUrl;
    }

    private String extractProxiedSourceUrl(String rawUrl) {
        try {
            URI uri = new URI(rawUrl.trim());
            if (!"/api/image-proxy".equals(uri.getPath()) || uri.getRawQuery() == null) {
                return null;
            }

            for (String param : uri.getRawQuery().split("&")) {
                int separator = param.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = URLDecoder.decode(param.substring(0, separator), StandardCharsets.UTF_8);
                if ("url".equals(key)) {
                    return URLDecoder.decode(param.substring(separator + 1), StandardCharsets.UTF_8);
                }
            }
        } catch (URISyntaxException ignored) {
            return null;
        }
        return null;
    }

    private String normalizedPublicBaseUrl() {
        String baseUrl = publicBaseUrl == null || publicBaseUrl.isBlank()
                ? "https://api.heritageload.store"
                : publicBaseUrl.trim();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private int normalizeWidth(Integer requestedWidth) {
        if (requestedWidth == null || requestedWidth <= 0) {
            return 640;
        }
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, requestedWidth));
    }

    private MediaType resolveContentType(MediaType contentType) {
        if (contentType != null && contentType.getType().equals("image")) {
            return contentType;
        }
        return MediaType.IMAGE_JPEG;
    }

    private void pruneCache() {
        if (cache.size() < CACHE_MAX_ENTRIES) {
            return;
        }

        long now = Instant.now().toEpochMilli();
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis <= now);

        int removeCount = cache.size() - CACHE_MAX_ENTRIES + 1;
        if (removeCount <= 0) {
            return;
        }

        Iterator<Map.Entry<String, CacheEntry>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext() && removeCount-- > 0) {
            iterator.next();
            iterator.remove();
        }
    }

    public record ProxiedImage(byte[] bytes, MediaType contentType) {
    }

    private static class CacheEntry {
        private final ProxiedImage image;
        private final long expiresAtMillis;

        private CacheEntry(ProxiedImage image, long expiresAtMillis) {
            this.image = image;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
