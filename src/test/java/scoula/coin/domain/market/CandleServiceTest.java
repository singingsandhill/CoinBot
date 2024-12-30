package scoula.coin.domain.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import scoula.coin.application.dto.CandleDTO;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CandleServiceTest {

    @InjectMocks
    private CandleService candleService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        candleService = new CandleService(restTemplate, objectMapper);
        ReflectionTestUtils.setField(candleService, "secretKey", "testSecretKey");
        ReflectionTestUtils.setField(candleService, "appKey", "testAppKey");
    }

    @Test
    void testGetCandle_Success() throws Exception {
        // Given
        String market = "BTC_KRW";
        int count = 2;
        String apiResponse = "[\n" +
                "  {\"market\":\"BTC_KRW\", \"candle_date_time_utc\":\"2023-12-01T00:00:00\", \"candle_date_time_kst\":\"2023-12-01T09:00:00\", \"opening_price\":60000, \"high_price\":61000, \"low_price\":59000, \"trade_price\":60500, \"timestamp\":1700000000000, \"candle_acc_trade_price\":1000000, \"candle_acc_trade_volume\":20, \"unit\":1},\n" +
                "  {\"market\":\"BTC_KRW\", \"candle_date_time_utc\":\"2023-12-01T01:00:00\", \"candle_date_time_kst\":\"2023-12-01T10:00:00\", \"opening_price\":60500, \"high_price\":62000, \"low_price\":60000, \"trade_price\":61500, \"timestamp\":1700003600000, \"candle_acc_trade_price\":1200000, \"candle_acc_trade_volume\":25, \"unit\":1}\n" +
                "]";

        JsonNode jsonNode = new ObjectMapper().readTree(apiResponse);

        ResponseEntity<String> mockResponse = mock(ResponseEntity.class);
        when(mockResponse.getBody()).thenReturn(apiResponse);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);
        when(objectMapper.readTree(apiResponse)).thenReturn(jsonNode);

        // When
        List<CandleDTO> candleDTOList = candleService.getCandle(market, count);

        // Then
        assertNotNull(candleDTOList);
        assertEquals(2, candleDTOList.size());

        CandleDTO firstCandle = candleDTOList.get(0);
        assertEquals("BTC_KRW", firstCandle.getMarket());
        assertEquals("2023-12-01T00:00:00", firstCandle.getCandleDateTimeUtc());
        assertEquals("2023-12-01T09:00:00", firstCandle.getCandleDateTimeKst());
        assertEquals(60000, firstCandle.getOpeningPrice());
        assertEquals(61000, firstCandle.getHighPrice());
        assertEquals(59000, firstCandle.getLowPrice());
        assertEquals(60500, firstCandle.getTradePrice());
        assertEquals(1700000000000L, firstCandle.getTimeStamp());
        assertEquals(1000000, firstCandle.getCandleAccTradePrice());
        assertEquals(20, firstCandle.getCandleAccTradeVolume());
        assertEquals(1, firstCandle.getUnit());

        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        verify(objectMapper, times(1)).readTree(apiResponse);
    }

    @Test
    void testGetCandle_Failure() {
        // Given
        String market = "BTC_KRW";
        int count = 2;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("API call failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> candleService.getCandle(market, count));
        assertEquals("Failed to fetch coin data", exception.getMessage());

        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }
}
