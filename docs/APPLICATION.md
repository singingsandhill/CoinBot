# Application Layer Documentation

This document describes the application layer (`src/main/java/scoula/coin/application/`) which handles data transfer objects (DTOs) and entities for the CoinBot application.

## Overview

The application layer implements the data layer using clear separation between:
- **DTOs** (`/dto/`): API contracts and data transfer objects
- **Entities** (`/entity/`): JPA entities for database persistence
- **Service** (`/service/`): Currently empty (business logic in domain layer)

## Architecture Patterns

### Data Flow
```
External APIs → DTOs → Domain Services → Entities → Database
              ↑                         ↓
          API Responses ← DTOs ← Domain Services ← Entities
```

### Database Partitioning
- **Coin catalog**: Trading-related entities (`order_history`, `trading_signal_history`)
- **Run catalog**: Running application entities (`Regular`, `record`)

## DTO Layer (`/dto/`)

### Trading & Market DTOs

#### `Account.java`
**Purpose**: Cryptocurrency account balance information  
**Key Fields**:
- `currency`: Currency code (e.g., "KRW", "BTC")
- `balance`: Available balance
- `locked`: Locked (reserved) balance
- `avg_buy_price`: Average purchase price

**Patterns**:
- `@JsonProperty` for snake_case API field mapping
- `@Builder` for fluent construction
- Immutable data structure

#### `Market.java`
**Purpose**: Market configuration and constraints  
**Key Fields**:
- `id`: Market identifier (e.g., "KRW-BTC")
- `name`: Display name
- `order_types`: Supported order types array
- `order_sides`: Supported order sides
- `state`: Market operational state

#### `OrderBookDTO.java`
**Purpose**: Complex order book and account information  
**Architecture**: Nested static classes for structured data
```java
public class OrderBookDTO {
    // Nested classes for organization
    public static class Bid { ... }
    public static class Ask { ... }
    public static class Account { ... }
}
```

#### `CandleDTO.java`
**Purpose**: Market candlestick data for technical analysis  
**Key Fields**:
- OHLC pricing data (open, high, low, close)
- Volume and value information
- Market and timestamp data

**Usage**: Primary data structure for trading algorithm inputs

#### `OrderConstraint.java`
**Purpose**: Trading constraints and limits  
**Key Fields**:
- `currency`: Base currency
- `price_unit`: Minimum price increment
- `min_total`: Minimum order value

### Response DTOs

#### `ApiResponse<T>`
**Purpose**: Generic API response wrapper  
**Type-safe design**: Parameterized generic for any data type
```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    
    // Static factory methods
    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
}
```

#### `ErrorResponse.java`
**Purpose**: Standardized error information  
**Integration**: Works with custom `ErrorCode` enum from global layer
```java
@Builder
public class ErrorResponse {
    private HttpStatus status;
    private String code;
    private String message;
    private LocalDateTime timestamp;
}
```

#### `SuccessResponse.java`
**Purpose**: Simple success acknowledgment wrapper

### History & Tracking DTOs

#### `OrderHistoryDTO.java`
**Purpose**: Order transaction history with entity conversion  
**Key Features**:
- Bidirectional entity conversion
- Trading result calculations
- Comprehensive order lifecycle data

**Conversion Pattern**:
```java
// Entity to DTO
public static OrderHistoryDTO fromEntity(OrderHistory entity) { ... }

// DTO to Entity  
public OrderHistory toEntity() { ... }
```

#### `TradingSignalHistoryDTO.java`
**Purpose**: Trading signal tracking and analysis  
**Key Fields**:
- RSI values and technical indicators
- Order correlation data
- Execution timestamps

### Running Application DTOs

#### `RunningRecord.java`
**⚠️ Architectural Issue**: Contains JPA annotations in DTO package
**Purpose**: Running activity data structure  
**Fields**:
- Runner name, location, datetime
- Distance and pace information
- Should be refactored to separate DTO and entity

#### `RunnerDistanceDTO.java`
**Purpose**: Distance aggregation for analytics  
**Implementation**: Record-based DTO for immutability
```java
public record RunnerDistanceDTO(String runnerName, Double totalDistance) {}
```

## Entity Layer (`/entity/`)

### Trading Entities (catalog: "coin")

#### `OrderHistory.java`
**Purpose**: Order transaction persistence  
**Key Features**:
- UUID primary key for distributed systems
- Manual timestamp management with `@PrePersist`
- Comprehensive order lifecycle tracking

**Table Structure**:
```sql
CREATE TABLE order_history (
    uuid VARCHAR(255) PRIMARY KEY,
    market VARCHAR(50),
    side VARCHAR(10),
    volume DECIMAL(20,8),
    price DECIMAL(20,8),
    created_at TIMESTAMP,
    -- additional fields...
);
```

#### `TradingSignalHistory.java`
**Purpose**: Trading signal correlation tracking  
**Key Features**:
- Links technical analysis signals to actual orders
- RSI value tracking
- Manual auditing with `@PrePersist`

### Running Entities (catalog: "run")

#### `Regular.java`
**Purpose**: Regular running schedule management  
**Key Features**:
- Basic JPA entity with auto-generated ID
- Simple auditing with creation timestamp

#### `RunningRecords.java`
**Purpose**: Individual running activity records  
**Key Features**:
- Full Spring Data auditing with `@EntityListeners`
- Automatic timestamp management
- Complete running session data

**Auditing Configuration**:
```java
@EntityListeners(AuditingEntityListener.class)
public class RunningRecords {
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

## Common Patterns

### Builder Pattern
**Usage**: Consistent across all complex DTOs and entities
```java
@Builder
public class OrderHistoryDTO {
    // Enables fluent construction
    OrderHistoryDTO.builder()
        .market("KRW-BTC")
        .side("bid")
        .volume(new BigDecimal("0.001"))
        .build();
}
```

### JSON Serialization
**Pattern**: Consistent API field mapping
```java
@JsonProperty("avg_buy_price")
private String avgBuyPrice;  // Maps snake_case to camelCase

@JsonInclude(JsonInclude.Include.NON_NULL)
private String optionalField;  // Excludes null values
```

### Auditing Strategies

#### Manual Auditing (Legacy Pattern)
```java
@PrePersist
protected void onCreate() {
    this.createdAt = LocalDateTime.now();
}
```

#### Spring Data Auditing (Recommended Pattern)
```java
@EntityListeners(AuditingEntityListener.class)
@CreatedDate
@LastModifiedDate
```

### Entity-DTO Conversion

#### Bidirectional Pattern (Recommended)
```java
// In DTO class
public static OrderHistoryDTO fromEntity(OrderHistory entity) {
    return OrderHistoryDTO.builder()
        .uuid(entity.getUuid())
        .market(entity.getMarket())
        .build();
}

public OrderHistory toEntity() {
    return OrderHistory.builder()
        .uuid(this.uuid)
        .market(this.market)
        .build();
}
```

## Validation Patterns

### Current State
- Limited explicit validation annotations
- Relies on database constraints
- Manual validation in service layer

### Recommended Improvements
```java
// Add validation annotations
@NotNull
@Size(min = 1, max = 50)
private String market;

@Positive
@DecimalMin("0.00000001")
private BigDecimal volume;
```

## Error Handling Integration

### With Global Exception Handler
```java
// In controller
try {
    OrderHistoryDTO result = service.processOrder(request);
    return ApiResponse.success(result);
} catch (CustomException e) {
    return ApiResponse.error(e.getMessage());
}
```

### Standardized Error Responses
```java
// Automatic error response formatting
{
    "success": false,
    "data": null,
    "message": "주문 파라미터가 유효하지 않습니다."
}
```

## Testing Patterns

### DTO Testing
```java
@Test
void testEntityConversion() {
    OrderHistory entity = createTestEntity();
    OrderHistoryDTO dto = OrderHistoryDTO.fromEntity(entity);
    OrderHistory converted = dto.toEntity();
    
    assertEquals(entity.getUuid(), converted.getUuid());
}
```

### Entity Testing
```java
@DataJpaTest
class OrderHistoryRepositoryTest {
    @Test
    void testSaveAndFind() {
        OrderHistory order = OrderHistory.builder()
            .market("KRW-BTC")
            .build();
        
        repository.save(order);
        assertThat(order.getCreatedAt()).isNotNull();
    }
}
```

## Performance Considerations

### Database Optimization
- Proper indexing on frequently queried fields
- Efficient pagination for history queries
- Connection pooling configuration

### Serialization Optimization
- `@JsonInclude(NON_NULL)` to reduce payload size
- Lazy loading for entity relationships
- DTO projection for large datasets

## Migration Recommendations

### Architectural Improvements
1. **Fix DTO-Entity Separation**: Move `RunningRecord` JPA annotations to separate entity
2. **Standardize Auditing**: Use Spring Data auditing consistently
3. **Add Validation**: Implement comprehensive DTO validation
4. **Entity Relationships**: Add proper JPA relationships where appropriate

### Code Quality Improvements
1. **Bidirectional Conversion**: Implement for all DTO-Entity pairs
2. **Builder Consistency**: Ensure all complex objects use builders
3. **Test Coverage**: Add comprehensive unit tests for conversions
4. **Documentation**: Add JavaDoc for all public methods

This application layer provides a solid foundation for data handling with clear separation of concerns, though some architectural inconsistencies should be addressed for better maintainability.