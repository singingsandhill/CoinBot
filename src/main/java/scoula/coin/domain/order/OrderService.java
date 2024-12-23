package scoula.coin.domain.order;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import scoula.coin.application.dto.OrderBookDTO;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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

    /**
     * 주문 가능 조건을 알려주는 메서드
     * @param market 시장 코드, String
     * @return
     */
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
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode dataNode = objectMapper.readTree(response.getBody());

                if (dataNode == null) {
                    throw new RuntimeException("Invalid response format: missing data node");
                }

                // OrderBookDTO로 매핑
                OrderBookDTO orderBook = OrderBookDTO.builder()
                        .bidFee(getBigDecimal(dataNode, "bid_fee"))
                        .askFee(getBigDecimal(dataNode, "ask_fee"))
                        .makerBidFee(getBigDecimal(dataNode, "maker_bid_fee"))
                        .makerAskFee(getBigDecimal(dataNode, "maker_ask_fee"))
                        .market(mapMarket(dataNode.get("market")))
                        .bidAccount(mapAccount(dataNode.get("bid_account")))
                        .askAccount(mapAccount(dataNode.get("ask_account")))
                        .build();

                log.debug("Successfully mapped order book data for market: {}", market);
                return orderBook;
            } else {
                log.error("Failed to get order book. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to get order book");
            }
        } catch (Exception e) {
            log.error("Error getting order book: "+e+" , "+e.getMessage());
            throw new RuntimeException("Failed to get order book", e);
        }
    }

    private OrderBookDTO.Market mapMarket(JsonNode marketNode) {
        if (marketNode == null) return null;

        OrderBookDTO.Market build = OrderBookDTO.Market.builder()
                .id(getText(marketNode, "id"))
                .name(getText(marketNode, "name"))
                .orderTypes(getStringList(marketNode, "order_types"))
                .askTypes(getStringList(marketNode, "ask_types"))
                .bidTypes(getStringList(marketNode, "bid_types"))
                .orderSides(getStringList(marketNode, "order_sides"))
                .bid(mapOrderConstraint(marketNode.get("bid")))
                .ask(mapOrderConstraint(marketNode.get("ask")))
                .maxTotal(getBigDecimal(marketNode, "max_total"))
                .state(getText(marketNode, "state"))
                .build();
        return build;
    }

    private OrderBookDTO.OrderConstraint mapOrderConstraint(JsonNode constraintNode) {
        if (constraintNode == null) return null;

        return OrderBookDTO.OrderConstraint.builder()
                .currency(getText(constraintNode, "currency"))
                .priceUnit(getBigDecimal(constraintNode, "price_unit"))
                .minTotal(getBigDecimal(constraintNode, "min_total"))
                .build();
    }

    private OrderBookDTO.Account mapAccount(JsonNode accountNode) {
        if (accountNode == null) return null;

        return OrderBookDTO.Account.builder()
                .currency(getText(accountNode, "currency"))
                .balance(getBigDecimal(accountNode, "balance"))
                .locked(getBigDecimal(accountNode, "locked"))
                .avgBuyPrice(getBigDecimal(accountNode, "avg_buy_price"))
                .avgBuyPriceModified(getBoolean(accountNode, "avg_buy_price_modified"))
                .unitCurrency(getText(accountNode, "unit_currency"))
                .build();
    }

    // Utility methods for safe JsonNode value extraction
    private BigDecimal getBigDecimal(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        return valueNode != null && !valueNode.isNull() ?
                new BigDecimal(valueNode.asText()) : null;
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        return valueNode != null && !valueNode.isNull() ?
                valueNode.asText() : null;
    }

    private Boolean getBoolean(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        return valueNode != null && !valueNode.isNull() ?
                valueNode.asBoolean() : null;
    }

    private List<String> getStringList(JsonNode node, String fieldName) {
        JsonNode arrayNode = node.get(fieldName);
        if (arrayNode == null || !arrayNode.isArray()) return new ArrayList<>();

        List<String> result = new ArrayList<>();
        arrayNode.elements().forEachRemaining(element ->
                result.add(element.asText())
        );
        return result;
    }

    /**
     * 주문 생성 메서드
     * @return
     * @throws NoSuchAlgorithmException
     */
    public Object doOrder() throws NoSuchAlgorithmException {
        // Set API parameters
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("market", "KRW-BTC");
        requestBody.put("side", "bid");
        requestBody.put("volume", 0.001);
        requestBody.put("price", 84000000);
        requestBody.put("ord_type", "limit");

        // Generate access token
        List<BasicNameValuePair> queryParams = requestBody.entrySet().stream()
                .map(entry -> new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())))
                .toList();
        String query = URLEncodedUtils.format(queryParams, StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(query.getBytes(StandardCharsets.UTF_8));
        String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("timestamp", System.currentTimeMillis())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(algorithm);
        String authenticationToken = "Bearer " + jwtToken;

        // Call API
        final HttpPost httpRequest = new HttpPost(baseUrl + "/v1/orders");
        httpRequest.addHeader("Authorization", authenticationToken);
        httpRequest.addHeader("Content-type", "application/json");
        try {
            httpRequest.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(requestBody), StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(httpRequest)) {
            // handle to response
            int httpStatus = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            System.out.println(httpStatus);
            System.out.println(responseBody);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return requestBody;
    }
}