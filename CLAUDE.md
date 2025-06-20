# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application that integrates two distinct services for cost optimization:

1. **CoinBot** - Cryptocurrency automated trading bot for Bitcoin (KRW-BTC market)
2. **Running Crew Archive** - Mileage tracking and regular run attendance records

**Service URLs:**
- Trading Bot: http://run.blu2print.site:8081/market/chart
- Running Archive: http://run.blu2print.site:8081/running/home

## Development Commands

### Build and Run
```bash
./gradlew clean build
./gradlew bootRun
```

### Testing
```bash
./gradlew test                    # Run all tests
./gradlew test --tests "*.TradingServiceTest"  # Run specific test class
```

### Build for Production
```bash
./gradlew clean bootJar
```

## Architecture Overview

### Domain-Driven Design Structure
The application follows DDD principles with clear separation across four main layers:

- **`domain/`** - Business logic organized by domains (trading, market, order, running, etc.)
- **`application/`** - DTOs and entities (data contracts and persistence)
- **`presentation/`** - Controllers and handlers (web/API layer)
- **`global/`** - Cross-cutting concerns (config, error handling, utilities)

### Technology Stack
- **Framework**: Spring Boot 3.2.1 with Java 17
- **Database**: MySQL (AWS RDS) with JPA/Hibernate
- **APIs**: Bithumb (crypto), Korea Investment (stocks)
- **Frontend**: Thymeleaf + JavaScript (Chart.js, React, Tesseract.js)
- **Security**: Spring Security (currently disabled)
- **Documentation**: Swagger/OpenAPI 3.0

## Detailed Architecture Documentation

For comprehensive understanding of each layer, refer to the detailed documentation:

### 📁 [Domain Layer Documentation](docs/DOMAIN.md)
**Business Logic & Core Services**
- Trading domain with automated signal analysis
- Market data integration with external APIs
- Order management and history tracking
- Running activity processing with OCR
- Technical indicators and strategy implementation
- Repository patterns and external API integration

### 📁 [Application Layer Documentation](docs/APPLICATION.md)
**Data Transfer Objects & Entities**
- DTO patterns for API communication
- JPA entities with audit trails
- Entity-DTO conversion patterns
- Database schema design
- Validation and serialization patterns

### 📁 [Presentation Layer Documentation](docs/PRESENTATION.md)
**Web Controllers & API Endpoints**
- REST API and MVC controller patterns
- URL routing and endpoint design
- Thymeleaf template integration
- Error handling and response formatting
- File upload and image processing
- Swagger API documentation

### 📁 [Global Layer Documentation](docs/GLOBAL.md)
**Infrastructure & Cross-cutting Concerns**
- Configuration management (security, scheduling, JPA)
- Centralized error handling and custom exceptions
- Utility classes for API integration
- JWT authentication for external APIs
- Logging and monitoring patterns

## Quick Start Guide

### Key Service Classes
- **`TradingService`** - Main orchestrator for automated trading (domain/trading/)
- **`CandleService`** - Market data from Bithumb API (domain/market/)
- **`OrderService`** - Order lifecycle management (domain/order/)
- **`RunService`** - Running record processing (domain/run/)
- **`TradingScheduler`** - Runs analysis every 60 seconds (global/config/)

### Main Controllers
- **`ChartController`** - Market visualization dashboard
- **`MarketController`** - Market data REST APIs
- **`OrderController`** - Order management APIs
- **`RunningController`** - Running app with OCR processing

### Database Configuration
- **Connection**: MySQL on AWS RDS
- **Catalogs**: `coin` (trading data), `run` (running data)
- **Auditing**: Automatic timestamps via JPA auditing

## Error Handling

Centralized exception system:
- **`ErrorCode`** enum with HTTP status mapping
- **`CustomException`** for application-specific errors
- **`GlobalExceptionHandler`** for consistent API responses
- **Response Format**: `ApiResponse<T>` wrapper for all APIs

## Testing Strategy

- **Unit Tests**: JUnit 5 + Mockito for service layer testing
- **Integration Tests**: Full request/response cycle testing
- **Test Data**: Builder pattern for complex objects
- **Mocking**: External API dependencies mocked in tests

## Security Notes

⚠️ **Important**: Security is currently disabled in production
- Spring Security configuration commented out
- API keys stored in application.properties (should use environment variables)
- No authentication on public endpoints
- External API integration uses proper JWT authentication

## Performance Considerations

- **Scheduled Tasks**: Trading analysis every 60 seconds
- **Database**: Optimized queries with native SQL for complex calculations
- **API Integration**: Proper error handling and timeout management
- **File Processing**: Image uploads up to 10MB with OCR processing

## Development Notes

- **Configuration**: External APIs configured via application.properties
- **Templates**: Thymeleaf templates in `src/main/resources/templates/`
- **Static Assets**: Located in `src/main/resources/static/`
- **Logging**: SLF4J with comprehensive error and business logic logging
- **Deployment**: Automated CI/CD via GitHub Actions to AWS EC2