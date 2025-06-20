# Presentation Layer Documentation

This document describes the presentation layer (`src/main/java/scoula/coin/presentation/`) which handles web controllers and HTTP request/response processing for the CoinBot application.

## Overview

The presentation layer implements a hybrid architecture combining:
- **Traditional MVC**: Server-side rendering with Thymeleaf templates
- **REST API**: JSON-based APIs for external integration and AJAX calls
- **Mixed Controllers**: Controllers that serve both views and API endpoints

## Architecture Patterns

### Request Flow
```
HTTP Request → Controller → Domain Service → Application Layer → Database
HTTP Response ← Controller ← Domain Service ← Application Layer ← Database
```

### Layer Integration
- **Controllers** depend on **Domain Services** (not repositories directly)
- **Consistent Error Handling** through global exception handlers
- **Standardized Responses** using `ApiResponse<T>` wrapper

## Controller Layer (`/controller/`)

### 1. ChartController
**Type**: `@Controller` (MVC with views)  
**Purpose**: Market analysis chart visualization  
**Base Path**: `/market`

**Key Endpoint**:
```java
@GetMapping("/chart")
public String chart(Model model) {
    // Integrates multiple services for comprehensive data
    List<CandleDTO> candles = candleService.getCandle("KRW-BTC", 100);
    Map<String, Object> analysis = tradingService.analyzeTradingSignals("KRW-BTC", 100);
    
    // Complex data preparation for JavaScript charting
    model.addAttribute("candleData", candleDataJson);
    model.addAttribute("analysisData", analysisDataJson);
    
    return "market/chart";  // Thymeleaf template
}
```

**Template Integration**: Returns `market/chart.html` with rich JSON data for Chart.js visualization

### 2. MarketController
**Type**: `@RestController` (Pure API)  
**Purpose**: Market data REST API endpoints  
**Base Path**: `/market`

**API Endpoints**:
```java
@GetMapping("/candle")
public ResponseEntity<?> getCandle(
    @RequestParam(defaultValue = "KRW-BTC") String market,
    @RequestParam(defaultValue = "10") int count
) {
    List<CandleDTO> candles = candleService.getCandle(market, count);
    return ResponseEntity.ok(candles);
}

@GetMapping("/analysis")
public ResponseEntity<?> getMarketAnalysis(
    @RequestParam(defaultValue = "KRW-BTC") String market,
    @RequestParam(defaultValue = "100") int count
) {
    return ResponseEntity.ok(tradingService.analyzeTradingSignals(market, count));
}
```

**Response Pattern**: Direct JSON responses using Spring's `ResponseEntity`

### 3. OrderController
**Type**: `@RestController`  
**Purpose**: Comprehensive order management API  
**Base Path**: `/coin/orders`

**Complete API Surface**:
```java
// Order Management
POST   /coin/orders           - Create new order
DELETE /coin/orders/{uuid}    - Cancel order

// History & Analytics
GET    /coin/orders/history   - Order history with pagination
GET    /coin/orders/signals   - Trading signal history
GET    /coin/orders/result    - Trading performance results
GET    /coin/orders/startdate - Trading start date
GET    /coin/orders/numbid    - Buy order count
GET    /coin/orders/numask    - Sell order count
```

**Consistent Response Pattern**:
```java
return ResponseEntity.ok(ApiResponse.success(data));
```

**Advanced Features**:
- **Pagination Support**: `@RequestParam` for page, size, sorting
- **Filtering**: Date ranges, order types, status filtering
- **Analytics**: Comprehensive trading statistics and metrics

### 4. RunningController
**Type**: `@Controller` (Hybrid MVC + REST)  
**Purpose**: Running/fitness tracking application

**View Endpoints** (Thymeleaf templates):
```java
@GetMapping("/running/home")     - Dashboard with statistics
@GetMapping("/running/record")   - Record entry form
@GetMapping("/running/regular")  - Regular runs management
@GetMapping("/running/upload")   - Image upload interface
```

**API Endpoints** (JSON responses):
```java
@PostMapping("/running/save-ocr")     - OCR text processing
@PostMapping("/running/save")         - Save running records
@GetMapping("/running/runner-distances") - Distance analytics
@PostMapping("/running/modify")       - Image modification
@GetMapping("/running/available-fonts") - Font list for images
```

**Special Features**:
- **File Upload**: Multipart form handling for images
- **OCR Integration**: Text extraction from running app screenshots
- **Image Processing**: Dynamic text overlay generation
- **Real-time Statistics**: AJAX-powered dashboard updates

### 5. StockController
**Type**: `@RestController`  
**Purpose**: Stock market integration (placeholder)  
**Base Path**: `/api/stock`  
**Status**: Stub implementation for future development

## Handler Layer (`/handler/`)

### Exception Handling Architecture

**Active Handler**: `GlobalExceptionHandler` (in `/global/error/`)
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(ApiResponse.error(e.getMessage()));
    }
}
```

**Unused Handler**: `GlobalExcertionHandler` (in `/presentation/handler/`)
- All methods commented out
- Should be removed or consolidated

## URL Routing Architecture

### Routing Hierarchy
```
/market/
├── chart (view)           - Market analysis dashboard
├── candle (api)           - OHLC candlestick data
├── analysis (api)         - Technical analysis results
└── orderchance (api)      - Order possibility data

/coin/orders/
├── / (POST)               - Create order
├── /{uuid} (DELETE)       - Cancel order
├── history (GET)          - Order history
├── signals (GET)          - Signal history
├── result (GET)           - Trading results
└── [various counters]     - Statistics endpoints

/running/
├── home (view)            - Main dashboard
├── record (view)          - Record entry
├── regular (view)         - Regular runs
├── save-ocr (api)         - OCR processing
├── save (api)             - Record persistence
├── runner-distances (api) - Analytics
└── modify (api)           - Image processing

/api/stock/
└── [placeholder endpoints]
```

## Request/Response Patterns

### Response Wrapper Strategy
**Standardized API Responses**:
```java
// Success Response
{
    "success": true,
    "data": { /* actual data */ },
    "message": null
}

// Error Response
{
    "success": false,
    "data": null,
    "message": "Error description"
}
```

### Request Handling Patterns

**Query Parameters with Defaults**:
```java
@RequestParam(defaultValue = "KRW-BTC") String market
@RequestParam(defaultValue = "100") int count
@RequestParam(defaultValue = "0") int page
```

**Request Body Processing**:
```java
@PostMapping("/save-ocr")
public ResponseEntity<ApiResponse<RunningRecord>> saveOcrData(
    @RequestBody Map<String, String> request
) {
    String extractedText = request.get("extractedText");
    RunningRecord record = runService.parseRunningRecord(extractedText);
    return ResponseEntity.ok(ApiResponse.success(record));
}
```

**File Upload Handling**:
```java
@PostMapping("/modify")
public ResponseEntity<byte[]> modifyImage(
    @RequestParam("image") MultipartFile image,
    @RequestParam String text,
    @RequestParam String font
) {
    byte[] modifiedImage = imageTextService.addTextToImage(
        image.getBytes(), text, font
    );
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(modifiedImage);
}
```

## Thymeleaf Template Integration

### Template Structure
```
/templates/
├── market/
│   └── chart.html          - Complex chart visualization
└── running/
    ├── home.html           - Dashboard with React components
    ├── record.html         - OCR form processing
    ├── regular.html        - Static content with AJAX
    └── makeimage.html      - Image modification interface
```

### Key Template Features

**Rich Data Binding**:
```html
<!-- Complex JSON data for JavaScript -->
<script th:inline="javascript">
    const candleData = /*[[${candleDataJson}]]*/ [];
    const analysisData = /*[[${analysisDataJson}]]*/ {};
</script>
```

**External Library Integration**:
- **Chart.js**: Market data visualization
- **Tesseract.js**: Client-side OCR processing
- **React.js**: Interactive components
- **Kakao SDK**: Mobile integration

**Real-time Updates**:
```javascript
// Auto-refresh market data
setInterval(() => {
    fetch('/market/analysis')
        .then(response => response.json())
        .then(data => updateChart(data));
}, 60000);
```

## Error Handling Strategy

### Centralized Exception Handling
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.error("Custom exception: {}", e.getMessage());
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
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("요청한 리소스를 찾을 수 없습니다."));
    }
}
```

### Error Response Consistency
All API endpoints follow the same error response format through the global handler, ensuring consistent client-side error handling.

## API Documentation

### Swagger Integration
**Configuration**: Located in `/global/config/SwaggerConfig.java`
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
}
```

**Endpoint Documentation**:
```java
@Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다")
@PostMapping
public ResponseEntity<ApiResponse<String>> createOrder(
    @RequestBody OrderRequestDTO request
) {
    // Implementation
}
```

**Access**: Available at `/swagger-ui.html`

## Common Patterns

### Dependency Injection
```java
@RequiredArgsConstructor  // Lombok constructor injection
@Slf4j                    // Logging
@RestController           // or @Controller
@RequestMapping("/base")  // Base path
public class SampleController {
    
    private final SampleService sampleService;  // Constructor injection
}
```

### Response Building
```java
// Success with data
return ResponseEntity.ok(ApiResponse.success(data));

// Error response
return ResponseEntity
    .status(HttpStatus.BAD_REQUEST)
    .body(ApiResponse.error("Invalid parameters"));

// View response
return "template/view";  // Returns template name
```

### Validation Pattern
```java
@PostMapping("/orders")
public ResponseEntity<ApiResponse<String>> createOrder(
    @Valid @RequestBody OrderRequestDTO request
) {
    // Validation handled by @Valid and global exception handler
    String result = orderService.createOrder(request);
    return ResponseEntity.ok(ApiResponse.success(result));
}
```

## Performance Considerations

### Caching Strategy
- **Static Resources**: CSS, JS, images cached by browser
- **API Responses**: No explicit caching (consider Redis for frequently accessed data)
- **Template Caching**: Thymeleaf templates cached in production

### Async Processing
- **File Upload**: Synchronous processing (consider async for large files)
- **OCR Processing**: Client-side with Tesseract.js
- **Chart Updates**: Real-time via JavaScript polling

## Security Considerations

### Authentication
- Currently disabled Spring Security
- API keys stored in configuration (should be externalized)
- No explicit authentication on endpoints

### Input Validation
- Basic parameter validation
- File upload size limits (10MB configured)
- SQL injection prevention through JPA

## Testing Recommendations

### Controller Testing
```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    
    @MockBean
    private OrderService orderService;
    
    @Test
    void createOrder_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/coin/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "market": "KRW-BTC",
                        "side": "bid",
                        "volume": "0.001"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

### Integration Testing
```java
@SpringBootTest
@AutoConfigureTestDatabase
class OrderControllerIntegrationTest {
    
    @Test
    void fullOrderFlow_ShouldComplete() {
        // Test complete order lifecycle
    }
}
```

This presentation layer provides a robust foundation for both traditional web applications and modern API-driven services, with consistent patterns and comprehensive error handling throughout.