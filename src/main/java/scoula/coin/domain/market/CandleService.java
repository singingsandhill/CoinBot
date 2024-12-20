package scoula.coin.domain.market;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.apache.bcel.classfile.annotation.NameValuePair;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import scoula.coin.application.dto.CandleDTO;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class CandleService {

    private String baseUrl = "https://api.bithumb.com/v1/candles/minutes/1";
    @Value("${mycoin.appKey}")
    private String appKey;
    @Value("${mycoin.secretKey}")
    private String secretKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<CandleDTO> getCandle(String market, int count){
        try {
            // Generate access token using JWT
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            String jwtToken = JWT.create()
                    .withClaim("access_key", appKey)
                    .withClaim("nonce", UUID.randomUUID().toString())
                    .withClaim("timestamp", System.currentTimeMillis())
                    .sign(algorithm);

            // Construct Authorization token in Bearer format
            String authenticationToken = "Bearer " + jwtToken;

            // Set HTTP headers, including Authorization
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authenticationToken);
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json; charset=UTF-8");

            // Create HttpEntity object with headers
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Construct URL with parameters
            String url = String.format("%s?market=%s&count=%d", baseUrl, market, count);

            // Make the API call and get the response as String
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Parse the response using ObjectMapper
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            List<CandleDTO> candleDTOs = new ArrayList<>();

            for (JsonNode candleData : jsonNode) {
                CandleDTO candleDTO = new CandleDTO();
                candleDTO.setMarket(candleData.get("market").asText());
                candleDTO.setCandleDateTimeUtc(candleData.get("candle_date_time_utc").asText());
                candleDTO.setCandleDateTimeKst(candleData.get("candle_date_time_kst").asText());
                candleDTO.setOpeningPrice(candleData.get("opening_price").asDouble());
                candleDTO.setHighPrice(candleData.get("high_price").asDouble());
                candleDTO.setLowPrice(candleData.get("low_price").asDouble());
                candleDTO.setTradePrice(candleData.get("trade_price").asDouble());
                candleDTO.setTimeStamp(candleData.get("timestamp").asLong());
                candleDTO.setCandleAccTradePrice(candleData.get("candle_acc_trade_price").asDouble());
                candleDTO.setCandleAccTradeVolume(candleData.get("candle_acc_trade_volume").asDouble());
                candleDTO.setUnit(candleData.get("unit").asInt());

                candleDTOs.add(candleDTO);
            }

            // Return the parsed response as a Map
            return candleDTOs;

        } catch (Exception e) {
            log.error("Error occurred while fetching coin data: ", e);
            throw new RuntimeException("Failed to fetch coin data", e);
        }

    }
}
