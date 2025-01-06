package scoula.coin.domain.trading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scoula.coin.application.dto.CandleDTO;
import scoula.coin.application.dto.OrderBookDTO;
import scoula.coin.application.entity.OrderHistory;
import scoula.coin.application.entity.TradingSignalHistory;
import scoula.coin.domain.market.CandleService;
import scoula.coin.domain.order.OrderService;
import scoula.coin.domain.run.Repository.TradingSignalHistoryRepository;
import scoula.coin.domain.strategy.TechnicalIndicator;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * signal 생성 위주의 테스트
 */
@ExtendWith(MockitoExtension.class) // JUnit 5에서 Mockito를 사용하기 위한 확장 설정
class TradingServiceTest {

    @Mock // @Mock 어노테이션을 사용하여 필요한 서비스들의 모의 객체를 생성
    private CandleService candleService;

    @Mock
    private TechnicalIndicator technicalIndicator;

    @Mock
    private OrderService orderService;

    @Mock
    private TradingSignalHistoryRepository signalHistoryRepository;

    @InjectMocks //  Mock 객체들을 자동으로 주입
    private TradingService tradingService;

    private ObjectMapper objectMapper;

    @BeforeEach
    /**
     * @BeforeEach 각 테스트 실행 전에 실행되는 설정 메소드,
     * JSON 처리를 위한 ObjectMapper를 초기화
     */
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    /**
     * 성공 케이스를 테스트
     */
    void analyzeTradingSignals_Success() {
        // Given
        String market = "KRW-BTC";
        int count = 10;
        List<CandleDTO> mockCandles = createMockCandles(count + 35);

        // Mock all dependencies first
        setupBasicMocks(market, mockCandles, Arrays.asList(0, 0, -1));

        // Mock TradingSignalHistory
        TradingSignalHistory mockSignalHistory = TradingSignalHistory.builder()
                .market(market)
                .signalType(-1)
                .price(BigDecimal.valueOf(50000.0))
                .build();
        when(signalHistoryRepository.save(any(TradingSignalHistory.class))).thenReturn(mockSignalHistory);

        // When
        Map<String, Object> result = tradingService.analyzeTradingSignals(market, count);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("prices"));
        assertTrue(result.containsKey("macd"));
        assertTrue(result.containsKey("rsi"));
        assertTrue(result.containsKey("bollingerBands"));
        assertTrue(result.containsKey("signals"));
        assertTrue(result.containsKey("orderExecuted"));
        assertTrue(result.containsKey("orderStatus"));
    }

    @Test
    /**
     * 데이터가 부족한 경우의 테스트
     */
    void analyzeTradingSignals_InsufficientData() {
        // Given
        String market = "KRW-BTC";
        int count = 10;
        List<CandleDTO> mockCandles = createMockCandles(15); // Less than required 20 data points

        // Mock dependency with complete stubbing
        when(candleService.getCandle(anyString(), anyInt())).thenReturn(mockCandles);

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () ->
                tradingService.analyzeTradingSignals(market, count)
        );
        assertTrue(exception.getMessage().contains("Failed to analyze trading signals"));
    }

    @Test
    void analyzeTradingSignals_WithBuySignal() {
        // Given
        String market = "KRW-BTC";
        int count = 10;
        List<CandleDTO> mockCandles = createMockCandles(count + 35);
        List<Integer> mockSignals = Arrays.asList(0, 0, 1); // Buy signal at the end

        // Mock TradingSignalHistory
        TradingSignalHistory mockSignalHistory = TradingSignalHistory.builder()
                .market(market)
                .signalType(1)
                .price(BigDecimal.valueOf(50000.0))
                .build();
        when(signalHistoryRepository.save(any(TradingSignalHistory.class))).thenReturn(mockSignalHistory);

        // Mock regular dependencies
        setupBasicMocks(market, mockCandles, mockSignals);

        // Mock OrderHistory response
        OrderHistory mockOrderHistory = new OrderHistory();
        mockOrderHistory.setUuid("mock-order-uuid");
        when(orderService.doOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyString()))
                .thenReturn(mockOrderHistory);

        // When
        Map<String, Object> result = tradingService.analyzeTradingSignals(market, count);

        // Then
        assertNotNull(result);
        verify(orderService).doOrder(eq(market), eq("bid"), anyDouble(), anyDouble(), eq("limit"));
    }

    // [Previous helper methods remain the same: createMockCandles, createMockOrderBookDTO, createMockOrdersJsonNode]

    private void setupBasicMocks(String market, List<CandleDTO> mockCandles, List<Integer> mockSignals) {
        when(candleService.getCandle(eq(market), anyInt())).thenReturn(mockCandles);
        when(technicalIndicator.calculateMACD(anyList(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList(0.5, 0.6, 0.7));
        when(technicalIndicator.calculateRSI(anyList(), anyInt()))
                .thenReturn(Arrays.asList(45.0, 50.0, 55.0));
        when(technicalIndicator.calculateBollingerBands(anyList(), anyInt(), anyDouble()))
                .thenReturn(Arrays.asList(Arrays.asList(95.0, 100.0, 105.0)));
        when(technicalIndicator.generateSignals(anyList(), anyList(), anyList(), anyList()))
                .thenReturn(mockSignals);
        when(orderService.getOrderChance(market)).thenReturn(createMockOrderBookDTO());
        when(orderService.getOrders(eq(market), isNull(), anyInt(), anyInt(), anyString()))
                .thenReturn(createMockOrdersJsonNode(false));
    }

    private List<CandleDTO> createMockCandles(int count) {
        List<CandleDTO> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CandleDTO candle = new CandleDTO();
            candle.setTradePrice(50000.0 + i * 100); // Incremental prices
            candles.add(candle);
        }
        return candles;
    }

    private OrderBookDTO createMockOrderBookDTO() {
        OrderBookDTO.Account bidAccount = OrderBookDTO.Account.builder()
                .currency("KRW")
                .balance(new BigDecimal("1000000")) // 100만원
                .locked(BigDecimal.ZERO)
                .avgBuyPrice(BigDecimal.ZERO)
                .avgBuyPriceModified(false)
                .unitCurrency("KRW")
                .build();

        OrderBookDTO.Account askAccount = OrderBookDTO.Account.builder()
                .currency("BTC")
                .balance(new BigDecimal("0.1")) // 0.1 BTC
                .locked(BigDecimal.ZERO)
                .avgBuyPrice(new BigDecimal("50000000")) // 5천만원
                .avgBuyPriceModified(false)
                .unitCurrency("KRW")
                .build();

        OrderBookDTO.Market market = OrderBookDTO.Market.builder()
                .id("KRW-BTC")
                .name("BTC/KRW")
                .orderTypes(Arrays.asList("limit", "price"))
                .askTypes(Arrays.asList("limit", "market"))
                .bidTypes(Arrays.asList("limit", "price"))
                .orderSides(Arrays.asList("ask", "bid"))
                .maxTotal(new BigDecimal("1000000000")) // 10억원
                .state("active")
                .build();

        return OrderBookDTO.builder()
                .bidFee(new BigDecimal("0.0005")) // 0.05% fee
                .askFee(new BigDecimal("0.0005")) // 0.05% fee
                .makerBidFee(new BigDecimal("0.0005"))
                .makerAskFee(new BigDecimal("0.0005"))
                .market(market)
                .bidAccount(bidAccount)
                .askAccount(askAccount)
                .build();
    }

    private JsonNode createMockOrdersJsonNode(boolean hasOrders) {
        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode dataArray = objectMapper.createArrayNode();

        if (hasOrders) {
            ObjectNode orderNode = objectMapper.createObjectNode();
            orderNode.put("uuid", "mock-uuid");
            orderNode.put("side", "bid");
            orderNode.put("state", "done");
            orderNode.put("price", "50000.0");
            dataArray.add(orderNode);
        }

        rootNode.set("data", dataArray);
        return rootNode;
    }

    private Object createMockOrderResult() {
        ObjectNode orderResult = objectMapper.createObjectNode();
        orderResult.put("uuid", "mock-order-uuid");
        return orderResult;
    }
}