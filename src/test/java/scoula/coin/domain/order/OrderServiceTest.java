package scoula.coin.domain.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import scoula.coin.application.dto.OrderBookDTO;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "accessKey", "test-access-key");
        ReflectionTestUtils.setField(orderService, "secretKey", "test-secret-key");
    }

    @Test
    @DisplayName("주문 가능 정보 조회 성공 테스트")
    void getOrderChance_Success() {
        // given
        String market = "KRW-BTC";
        String mockResponse = """
                {
                    "success": true,
                    "data": {
                        "bid_fee": "0.0005",
                        "ask_fee": "0.0005",
                        "maker_bid_fee": "0.0003",
                        "maker_ask_fee": "0.0003",
                        "market": {
                            "id": "KRW-BTC",
                            "name": "BTC/KRW",
                            "order_types": ["limit"],
                            "order_sides": ["ask", "bid"],
                            "bid": {
                                "currency": "KRW",
                                "price_unit": "1000",
                                "min_total": "5000"
                            },
                            "ask": {
                                "currency": "BTC",
                                "price_unit": "0.00000001",
                                "min_total": "5000"
                            },
                            "max_total": "1000000000",
                            "state": "active"
                        },
                        "bid_account": {
                            "currency": "KRW",
                            "balance": "1000000",
                            "locked": "0",
                            "avg_buy_price": "0",
                            "avg_buy_price_modified": false,
                            "unit_currency": "KRW"
                        },
                        "ask_account": {
                            "currency": "BTC",
                            "balance": "1",
                            "locked": "0",
                            "avg_buy_price": "30000000",
                            "avg_buy_price_modified": false,
                            "unit_currency": "KRW"
                        }
                    }
                }
                """;

        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponseEntity);

        // when
        OrderBookDTO result = orderService.getOrderChance(market);

        // then
        assertNotNull(result);
        assertEquals(new BigDecimal("0.0005"), result.getBidFee());
        assertEquals(new BigDecimal("0.0005"), result.getAskFee());
        assertEquals(new BigDecimal("0.0003"), result.getMakerBidFee());
        assertEquals(new BigDecimal("0.0003"), result.getMakerAskFee());

        // Market 정보 검증
        assertNotNull(result.getMarket());
        assertEquals("KRW-BTC", result.getMarket().getId());
        assertEquals("BTC/KRW", result.getMarket().getName());
        assertTrue(result.getMarket().getOrderTypes().contains("limit"));

        // Bid Account 정보 검증
        assertNotNull(result.getBidAccount());
        assertEquals("KRW", result.getBidAccount().getCurrency());
        assertEquals(new BigDecimal("1000000"), result.getBidAccount().getBalance());

        // Ask Account 정보 검증
        assertNotNull(result.getAskAccount());
        assertEquals("BTC", result.getAskAccount().getCurrency());
        assertEquals(new BigDecimal("1"), result.getAskAccount().getBalance());
    }

    @Test
    @DisplayName("주문 가능 정보 조회 실패 테스트 - 데이터 노드 없음")
    void getOrderChance_Failure_MissingDataNode() {
        // given
        String market = "KRW-BTC";
        String mockResponse = """
                {
                    "success": false,
                    "error": "Invalid API key"
                }
                """;

        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponseEntity);

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.getOrderChance(market));
        assertEquals("Invalid response format: missing data node", exception.getMessage());
    }

    @Test
    void doOrder() {
    }
}