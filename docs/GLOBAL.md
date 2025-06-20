# Global Layer Documentation

This document describes the global layer (`src/main/java/scoula/coin/global/`) which handles cross-cutting concerns, configuration, and infrastructure for the CoinBot application.

## Overview

The global layer provides infrastructure services including:
- **Configuration Management**: Spring Boot configurations and external integrations
- **Error Handling**: Centralized exception handling and error codes
- **Utilities**: Common functionality for API integration and data processing
- **Cross-cutting Concerns**: Logging, security, scheduling, and auditing

## Layer Architecture

### Cross-Cutting Services
```
Application Layers
├── Domain Services
├── Presentation Controllers  
├── Application DTOs/Entities
└── Global Infrastructure ←── (Supports all layers)
    ├── Configuration
    ├── Error Handling
    ├── Utilities
    └── Security
```

## Configuration Layer (`/config/`)

### 1. AppConfig
**Purpose**: Basic application bean configuration  
**Key Beans**:
```java
@Configuration
public class AppConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();  // HTTP client for external APIs
    }
}
```

**Usage**: Provides common beans for dependency injection across the application

### 2. JpaAuditingConfiguration
**Purpose**: Automatic entity auditing configuration  
**Features**:
```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
    // Enables automatic timestamp tracking for entities
}
```

**Integration**: Works with `@CreatedDate` and `@LastModifiedDate` annotations in entities

### 3. SecurityConfig
**Purpose**: Security configuration (currently disabled)  
**Current State**: All configurations commented out
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // All security configurations are commented out
    // CSRF protection disabled
    // Authentication disabled
}
```

**⚠️ Security Risk**: Application currently runs without security protection

### 4. SwaggerConfig
**Purpose**: API documentation configuration  
**OpenAPI Integration**:
```java
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .select()
            .apis(RequestHandlerSelectors.basePackage("scoula.coin.presentation"))
            .paths(PathSelectors.any())
            .build();
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("Trading Bot API 문서")
            .version("1.0.0")
            .contact(new Contact("Jisu Gim", "", "jisu0259@naver.com"))
            .build();
    }
}
```

**Access**: Available at `/swagger-ui.html`

### 5. TradingScheduler
**Purpose**: Automated trading signal analysis  
**Schedule Configuration**:
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class TradingScheduler {
    
    private final TradingService tradingService;
    
    @Scheduled(fixedRate = 60_000)  // Every 60 seconds
    public void scheduledAnalyzeTradingSignals() {
        try {
            tradingService.analyzeTradingSignals("KRW-BTC", 100);
            log.info("Trading analysis executed successfully");
        } catch (Exception e) {
            log.error("Error in scheduled analysis: ", e);
        }
    }
}
```

**Features**:
- Runs every minute automatically
- Comprehensive error handling
- Logging for monitoring and debugging

## Error Handling Layer (`/error/`)

### 1. ErrorCode Enum
**Purpose**: Centralized error code definitions  
**Structure**: HTTP Status + Error Code + Message pattern
```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // Real Estate Domain
    REAL_ESTATE_NOT_FOUND(HttpStatus.NOT_FOUND, "ESTATE_001", "부동산 정보를 찾을 수 없습니다."),
    
    // Order Domain Errors
    ORDER_INVALID_PARAMETERS(HttpStatus.BAD_REQUEST, "ORDER_001", "주문 파라미터가 유효하지 않습니다."),
    ORDER_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_002", "주문 실행에 실패했습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_003", "주문 정보를 찾을 수 없습니다."),
    ORDER_INVALID_MARKET(HttpStatus.BAD_REQUEST, "ORDER_004", "유효하지 않은 마켓입니다."),
    ORDER_INVALID_SIDE(HttpStatus.BAD_REQUEST, "ORDER_005", "유효하지 않은 거래 방향입니다."),
    ORDER_INVALID_VOLUME(HttpStatus.BAD_REQUEST, "ORDER_006", "유효하지 않은 거래량입니다."),
    ORDER_INVALID_PRICE(HttpStatus.BAD_REQUEST, "ORDER_007", "유효하지 않은 가격입니다."),
    ORDER_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_008", "응답 오류"),
    ORDER_DELETION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_008", "주문 취소에 실패했습니다."),
    ORDER_UUID_INVALID(HttpStatus.BAD_REQUEST, "ORDER_009", "유효하지 않은 주문 UUID입니다."),
    
    // System Errors
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_001", "시스템 오류가 발생했습니다.");
    
    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

### 2. CustomException
**Purpose**: Application-specific exception wrapper  
**Integration with ErrorCode**:
```java
@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

**Usage Pattern**:
```java
// In domain services
if (invalidParameter) {
    throw new CustomException(ErrorCode.ORDER_INVALID_PARAMETERS);
}
```

### 3. GlobalExceptionHandler
**Purpose**: Centralized exception handling  
**Handler Configuration**:
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.error("Custom exception occurred: {}", e.getMessage());
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(ApiResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        // Special handling for favicon.ico requests
        if (e.getResourcePath().contains("favicon.ico")) {
            return ResponseEntity.notFound().build();
        }
        log.warn("Resource not found: {}", e.getResourcePath());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("요청한 리소스를 찾을 수 없습니다."));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("시스템 오류가 발생했습니다."));
    }
}
```

**Response Format Consistency**:
```json
{
    "success": false,
    "data": null,
    "message": "Error description"
}
```

## Utility Layer (`/util/`)

### OrderUtils
**Purpose**: Comprehensive cryptocurrency API integration utility  
**Key Responsibilities**:
- JWT token generation for API authentication
- HTTP request handling (GET, POST, DELETE)
- API response processing and error handling
- Cryptocurrency trading operations

**JWT Authentication Implementation**:
```java
public static String createJwt(String accessKey, String secretKey, String queryString) {
    Algorithm algorithm = Algorithm.HMAC256(secretKey);
    long currentTimeMillis = System.currentTimeMillis();
    
    return JWT.create()
        .withClaim("access_key", accessKey)
        .withClaim("nonce", UUID.randomUUID().toString())
        .withClaim("query_hash", encryptQueryString(queryString))
        .withClaim("query_hash_alg", "SHA512")
        .withIssuedAt(new Date(currentTimeMillis))
        .sign(algorithm);
}
```

**HTTP Operations**:
```java
// GET request with authentication
public static String sendGetRequest(String url, String accessKey, String secretKey) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + createJwt(accessKey, secretKey, ""));
    
    HttpEntity<?> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    
    return response.getBody();
}

// POST request for order creation
public static String sendPostRequest(String url, String accessKey, String secretKey, Map<String, String> params) {
    // Implementation with proper authentication and error handling
}
```

**API Integration Features**:
- **Bithumb API**: Market data, order management, account information
- **Error Handling**: Integrates with custom exception system
- **Security**: Proper query string hashing and JWT signing
- **Response Processing**: JSON parsing and validation

## Security Architecture

### Current Security State
**⚠️ Major Security Issues**:
1. **Spring Security Disabled**: All security configurations commented out
2. **No Authentication**: No user authentication mechanism
3. **Exposed Endpoints**: All API endpoints publicly accessible
4. **Hardcoded Credentials**: API keys stored in application.properties

### API Security (External)
**JWT-based Authentication** for cryptocurrency APIs:
```java
// Proper API authentication
String queryString = buildQueryString(params);
String queryHash = encryptQueryString(queryString);  // SHA-512 hashing
String jwt = createJwt(accessKey, secretKey, queryString);

HttpHeaders headers = new HttpHeaders();
headers.set("Authorization", "Bearer " + jwt);
headers.set("Content-Type", "application/json");
```

### Recommended Security Improvements
1. **Enable Spring Security**: Configure authentication and authorization
2. **Environment Variables**: Move API keys to environment variables
3. **HTTPS Only**: Force HTTPS in production
4. **Rate Limiting**: Implement API rate limiting
5. **Input Validation**: Add comprehensive request validation

## Configuration Management

### External Configuration
**Application Properties Integration**:
```java
// In utility classes
@Value("${mycoin.appKey}")
private String appKey;

@Value("${mycoin.secretKey}")
private String secretKey;

@Value("${mycoin.url}")
private String apiUrl;
```

**Database Configuration**:
- AWS RDS MySQL integration
- JPA/Hibernate configuration
- Connection pooling settings

### Environment-Specific Configuration
**Current**: Single configuration file  
**Recommended**: Profile-based configuration
```properties
# application-dev.properties
# application-prod.properties
# application-test.properties
```

## Monitoring and Logging

### Logging Strategy
**SLF4J with Lombok**:
```java
@Slf4j
public class ServiceClass {
    
    public void processData() {
        log.info("Processing started");
        try {
            // Business logic
            log.debug("Processing details: {}", data);
        } catch (Exception e) {
            log.error("Processing failed: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
```

**Logging Levels**:
- **INFO**: Business operations, scheduler execution
- **DEBUG**: Detailed processing information
- **ERROR**: Exception handling with stack traces
- **WARN**: Non-critical issues and warnings

### Scheduled Task Monitoring
```java
@Scheduled(fixedRate = 60_000)
public void scheduledAnalyzeTradingSignals() {
    try {
        tradingService.analyzeTradingSignals("KRW-BTC", 100);
        log.info("scheduledAnalyzeTradingSignals executed at every 1 minute.");
    } catch (Exception e) {
        log.error("Error in scheduled analyze: ", e);
        // Could integrate with monitoring system here
    }
}
```

## Integration Patterns

### External API Integration
**Consistent Pattern** across all API calls:
1. **Authentication**: JWT token generation
2. **Request Building**: Proper headers and parameters
3. **Response Processing**: JSON parsing and validation
4. **Error Handling**: Custom exception propagation
5. **Logging**: Request/response logging for debugging

### Database Integration
**JPA Auditing Configuration**:
```java
@EnableJpaAuditing
public class JpaAuditingConfiguration {
    // Automatic timestamp tracking
    // Integration with Spring Data JPA
    // Consistent audit trail across entities
}
```

## Testing Strategies

### Configuration Testing
```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public RestTemplate mockRestTemplate() {
        return Mockito.mock(RestTemplate.class);
    }
}
```

### Error Handling Testing
```java
@Test
void customException_ShouldReturnProperResponse() {
    CustomException exception = new CustomException(ErrorCode.ORDER_INVALID_PARAMETERS);
    
    ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleCustomException(exception);
    
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertFalse(response.getBody().isSuccess());
}
```

## Performance Considerations

### Scheduled Tasks
- **Fixed Rate**: 60-second intervals for trading analysis
- **Error Recovery**: Continues execution despite individual failures
- **Resource Management**: Proper connection handling for API calls

### HTTP Client Configuration
- **Connection Pooling**: Default RestTemplate configuration
- **Timeout Settings**: Should be configured for external API calls
- **Retry Logic**: Currently not implemented (recommended addition)

This global layer provides essential infrastructure services with strong patterns for configuration management and error handling, though security improvements are critically needed for production deployment.