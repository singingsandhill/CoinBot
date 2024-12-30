package scoula.coin.global.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import scoula.coin.global.error.CustomException;
import scoula.coin.global.error.ErrorCode;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderUtils {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${mycoin.appKey}")
    private String accessKey; // static 제거

    @Value("${mycoin.secretKey}")
    private String secretKey; // static 제거

    /**
     * JWT 토큰 생성
     * @param queryString
     * @return
     * @throws NoSuchAlgorithmException
     */
    public String generateJwtToken(String queryString) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(queryString.getBytes(StandardCharsets.UTF_8));
        String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));

        return JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("timestamp", System.currentTimeMillis())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(Algorithm.HMAC256(secretKey));
    }

    /**
     * API 응답 처리
     * @param response
     * @return
     * @throws JsonProcessingException
     */
    public JsonNode processApiResponse(ResponseEntity<String> response) throws JsonProcessingException {
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode dataNode = objectMapper.readTree(response.getBody());
            if (dataNode == null) {
                log.error("Invalid response format: missing data node");
                throw new CustomException(ErrorCode.ORDER_INVALID_RESPONSE);
            }
            return dataNode;
        } else {
            log.error("API request failed. Status: {}", response.getStatusCode());
            throw new CustomException(ErrorCode.ORDER_EXECUTION_FAILED);
        }
    }

    /**
     * Http 헤더 생성
     * @param jwtToken
     * @return
     */
    public HttpHeaders createHttpHeaders(String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwtToken);
        return headers;
    }


    /**
     * GET 요청
     * @param baseUrl
     * @param endpoint
     * @param queryParams
     * @return
     */
    public JsonNode executeGetRequest(String baseUrl, String endpoint, List<NameValuePair> queryParams) {
        try {
            String query = URLEncodedUtils.format(queryParams, StandardCharsets.UTF_8);
            String jwtToken = generateJwtToken(query);
            HttpHeaders headers = createHttpHeaders(jwtToken);

            String url = baseUrl + endpoint + "?" + query;
            log.debug("GET Request - URL: {}", url);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return processApiResponse(response);
        } catch (Exception e) {
            log.error("Failed to execute GET request: {}", e.getMessage());
            throw new CustomException(ErrorCode.ORDER_EXECUTION_FAILED);
        }
    }

    /**
     * post 요청
     * @param baseUrl
     * @param endpoint
     * @param requestBody
     * @return
     */
    public JsonNode executePostRequest(String baseUrl, String endpoint, Map<String, Object> requestBody) {
        try {
            List<BasicNameValuePair> queryParams = requestBody.entrySet().stream()
                    .map(entry -> new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())))
                    .toList();

            String query = URLEncodedUtils.format(queryParams, StandardCharsets.UTF_8);
            String jwtToken = generateJwtToken(query);

            final HttpPost httpRequest = new HttpPost(baseUrl + endpoint);
            httpRequest.addHeader("Authorization", "Bearer " + jwtToken);
            httpRequest.addHeader("Content-type", "application/json");
            httpRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8));

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(httpRequest)) {

                if (response.getStatusLine().getStatusCode() != HttpStatus.OK.value() &&
                        response.getStatusLine().getStatusCode() != HttpStatus.CREATED.value()) {
                    throw new CustomException(ErrorCode.ORDER_EXECUTION_FAILED);
                }

                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return objectMapper.readTree(responseBody);
            }
        } catch (Exception e) {
            log.error("Failed to execute POST request: {}", e.getMessage());
            throw new CustomException(ErrorCode.ORDER_EXECUTION_FAILED);
        }
    }
}
