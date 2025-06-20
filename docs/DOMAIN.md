# Domain Layer Documentation

This document describes the business logic layer (`src/main/java/scoula/coin/domain/`) of the CoinBot application.

## Overview

The domain layer implements the core business logic using Domain-Driven Design principles. It consists of seven distinct domains handling cryptocurrency trading, market data, order management, running activity tracking, stock trading, trading strategies, and automated trading orchestration.

## Domain Architecture

### Dependency Flow
```
TradingService (orchestrator)
├── CandleService (market data)
├── OrderService (order management) 
├── TechnicalIndicator (strategy analysis)
└── TradingSignalHistoryRepository (signal tracking)

RunService (running activities)
├── RunningRecordsRepository
└── RegularRepository

ImageTextService (image processing)
└── Standalone utility service
```

## Domain Descriptions

### 1. Chart Domain (`/chart/`)
**Purpose**: Chart display and visualization logic  
**Status**: Currently empty, planned for future expansion  
**Key Files**: `ChartService.java` (placeholder)

### 2. Market Domain (`/market/`)
**Purpose**: Cryptocurrency market data management  
**External Integration**: Bithumb API  
**Key Responsibilities**:
- Fetches 1-minute candlestick data
- JWT authentication with external APIs
- Real-time market data processing

**Key Classes**:
- `CandleService`: Primary service class
  - `getCandle(String market, int count)`: Fetches candle data
  - JWT token generation and API authentication
  - Error handling with comprehensive logging

**API Integration Pattern**:
```java
// JWT-based authentication
String jwt = createJwt(accessKey, secretKey, queryString);
HttpHeaders headers = new HttpHeaders();
headers.set("Authorization", "Bearer " + jwt);
```

### 3. Order Domain (`/order/`)
**Purpose**: Cryptocurrency trading order lifecycle management  
**External Integration**: Bithumb API  
**Key Responsibilities**:
- Order creation, cancellation, and validation
- Order history tracking and analytics
- Fee calculation and constraints

**Key Classes**:
- `OrderService`: Core order management
  - `createOrder()`: Places buy/sell orders with validation
  - `cancelOrder()`: Cancels existing orders
  - `getOrderHistory()`: Retrieves trading history
  - `getOrderChance()`: Gets account balance and constraints

**Repository**:
- `OrderHistoryRepository`: Complex financial queries
  - Native SQL for trading result calculations
  - Aggregated profit/loss reporting

**Error Handling**: Uses custom `ErrorCode` enum for order-specific errors

### 4. Running Domain (`/run/`)
**Purpose**: Running activity tracking and image processing  
**Key Responsibilities**:
- OCR text extraction from running app screenshots
- Running record management and analytics
- Regular run attendance tracking
- Image overlay generation

**Key Classes**:

#### `RunService`: Core running logic
- `parseRunningRecord(String text)`: Extracts running data from OCR text
- `saveRunningRecord()`: Persists running records
- `getTotalDistanceThisWeek()`: Analytics and reporting
- **Regex Patterns**: Complex text parsing for Korean/English formats

#### `ImageTextService`: Image processing
- `addTextToImage()`: Overlays text on images with custom fonts
- **Font Management**: Configurable fonts, sizes, and colors
- **Quality Control**: Anti-aliasing and high-quality rendering

**Repositories**:
- `RunningRecordsRepository`: Individual running sessions
- `RegularRepository`: Group running events
- `TradingSignalHistoryRepository`: Signal correlation tracking

### 5. Stock Domain (`/stock/`)
**Purpose**: Stock market trading (future implementation)  
**Status**: Skeleton implementation  
**Key Files**: 
- `StockService`: Placeholder service
- `StockRepository`: Basic repository structure

### 6. Strategy Domain (`/strategy/`)
**Purpose**: Technical analysis and trading signal generation  
**Key Responsibilities**:
- Technical indicator calculations (MACD, RSI, Bollinger Bands)
- Multi-indicator signal analysis
- Mathematical precision for financial calculations

**Key Classes**:
- `TechnicalIndicator`: Core analysis engine
  - `calculateMACD()`: Moving Average Convergence Divergence
  - `calculateRSI()`: Relative Strength Index
  - `calculateBollingerBands()`: Volatility analysis
  - **Precision**: Uses `BigDecimal` for financial calculations

**Technical Analysis Pipeline**:
```java
// Multi-indicator analysis
Map<String, Object> macdResult = calculateMACD(prices, 12, 26, 9);
double rsi = calculateRSI(prices, 14);
Map<String, Double> bollinger = calculateBollingerBands(prices, 20, 2.0);
```

### 7. Trading Domain (`/trading/`)
**Purpose**: Automated trading orchestration and decision making  
**Key Responsibilities**:
- Combines technical analysis with order execution
- Risk management and position sizing
- Trade monitoring and timeout handling
- Signal history tracking

**Key Classes**:
- `TradingService`: Primary orchestrator (600+ lines)
  - `analyzeTradingSignals()`: Main analysis engine
  - `executeTradeBasedOnSignal()`: Order execution logic
  - `cancelTimedOutOrders()`: Risk management
  - **State Management**: Tracks latest analysis results

**Trading Algorithm Flow**:
1. Fetch extended candle data (count + 35 for indicators)
2. Calculate multiple technical indicators
3. Generate composite buy/sell signals
4. Execute orders with risk management
5. Track signals and correlate with results

**Risk Management Features**:
- Position sizing based on account balance
- Stop-loss and take-profit logic
- Order timeout handling
- Comprehensive logging and error handling

## Common Patterns

### Repository Pattern
- All repositories extend `JpaRepository<Entity, ID>`
- Custom queries use `@Query` annotations
- Native SQL for complex financial calculations
- Method naming follows Spring Data JPA conventions

### Service Layer Pattern
- Rich business logic in service classes
- Dependency injection with `@RequiredArgsConstructor`
- Comprehensive error handling with custom exceptions
- Extensive logging with SLF4J

### External API Integration
- JWT-based authentication for cryptocurrency APIs
- Centralized error handling for API failures
- Request/response DTOs for API communication
- Proper credential management through configuration

### Error Handling Strategy
```java
// Domain-specific exceptions
throw new CustomException(ErrorCode.ORDER_EXECUTION_FAILED);

// API error handling
catch (Exception e) {
    log.error("API call failed: {}", e.getMessage());
    throw new CustomException(ErrorCode.SYSTEM_ERROR);
}
```

## Testing Patterns

Domain services use comprehensive unit testing:
- Mock external dependencies (APIs, repositories)
- Test business logic in isolation
- Use builders and factories for test data
- Verify error handling and edge cases

Example test structure:
```java
@ExtendWith(MockitoExtension.class)
class TradingServiceTest {
    @Mock private CandleService candleService;
    @Mock private OrderService orderService;
    @InjectMocks private TradingService tradingService;
}
```

## Integration Points

### Between Domains
- Trading domain coordinates market, order, and strategy domains
- Running domain operates independently
- Shared utilities through global layer

### External Systems
- **Bithumb API**: Market data and order management
- **Korea Investment API**: Stock data (planned)
- **Database**: JPA-based persistence
- **File System**: Image processing and storage

## Performance Considerations

- **Scheduled Operations**: Trading analysis runs every 60 seconds
- **Database Queries**: Optimized with native SQL for complex calculations
- **API Rate Limiting**: Proper request handling to avoid limits
- **Memory Management**: Efficient handling of large candle datasets

## Future Enhancements

1. **Chart Domain**: Implement visualization logic
2. **Stock Domain**: Complete stock trading functionality
3. **Caching**: Add Redis for frequently accessed market data
4. **Event Sourcing**: Consider event-driven architecture for order processing
5. **Circuit Breaker**: Add resilience patterns for external API calls