package scoula.coin.domain.order;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import scoula.coin.application.dto.OrderBookDTO;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final String baseUrl = "https://api.bithumb.com";

    @Value("${mycoin.appKey}")
    private String accessKey;

    @Value("${mycoin.secretKey}")
    private String secretKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OrderBookDTO getOrderChance(String market) {
        try {
            // API 파라미터 설정
            List<NameValuePair> queryParams = new ArrayList<>();
            queryParams.add(new BasicNameValuePair("market", market));

            // 쿼리 문자열 생성
            String query = URLEncodedUtils.format(queryParams, StandardCharsets.UTF_8);

            // 쿼리 해시 생성
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(query.getBytes(StandardCharsets.UTF_8));
            String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));

            // JWT 토큰 생성
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            String jwtToken = JWT.create()
                    .withClaim("access_key", accessKey)
                    .withClaim("nonce", UUID.randomUUID().toString())
                    .withClaim("timestamp", System.currentTimeMillis())
                    .withClaim("query_hash", queryHash)
                    .withClaim("query_hash_alg", "SHA512")
                    .sign(algorithm);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + jwtToken);

            // HTTP 요청 엔티티 생성
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // API 호출 URL 구성
            String url = baseUrl + "/v1/orders/chance?" + query;

            log.debug("Request URL: {}", url);
            log.debug("Authorization Token: {}", jwtToken);
            log.debug("Query: {}", query);
            log.debug("Query Hash: {}", queryHash);

            // API 호출
            ResponseEntity<OrderBookDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    OrderBookDTO.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("Failed to get order book. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to get order book");
            }

        } catch (Exception e) {
            log.error("Error getting order book: ", e);
            throw new RuntimeException("Failed to get order book", e);
        }
    }
}