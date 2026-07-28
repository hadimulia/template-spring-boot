# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application template. The specific purpose and domain will be determined once the project is initialized.

## Tech Stack

- **Framework**: Spring Boot
- **Build Tool**: To be determined (Maven or Gradle)
- **Java Version**: To be determined
- **Database**: To be determined

## Development Commands

### Maven Projects
```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Run a single test method
./mvnw test -Dtest=ClassName#methodName

# Package without running tests
./mvnw clean package -DskipTests
```

### Gradle Projects
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests ClassName

# Run a single test method
./gradlew test --tests ClassName.methodName

# Package without running tests
./gradlew build -x test
```

## Architecture

### Standard Spring Boot Structure
- `src/main/java` - Application source code
- `src/main/resources` - Configuration files, static resources
- `src/test/java` - Test source code
- `src/test/resources` - Test configuration files

### Typical Package Organization
- `controller` - REST API endpoints and web controllers
- `service` - Business logic layer
- `mapper` - MyBatis mapper interfaces for data access
- `model` or `entity` - Domain models and database entities
- `dto` - Data Transfer Objects for API requests/responses
- `config` - Spring configuration classes
- `exception` - Custom exceptions and exception handlers

## Spring Boot Conventions

### Configuration
- Use `application.properties` or `application.yml` for configuration
- Profile-specific configs: `application-{profile}.properties`
- Externalize sensitive configuration using environment variables

### Dependency Injection
- Prefer constructor injection over field injection
- Use `@RequiredArgsConstructor` from Lombok when applicable
- Avoid circular dependencies

### REST API Design
- Controllers should delegate to service layer
- Use appropriate HTTP methods (GET, POST, PUT, PATCH, DELETE)
- Return appropriate HTTP status codes
- Use `@RestController` for REST APIs, `@Controller` for MVC

### Database Access
- Use MyBatis for database operations
- Mapper interfaces define database operations using annotations or XML mapping files
- Consider using tk.mybatis (MyBatis Mapper) library for common CRUD operations
- Place mapper XML files in `src/main/resources/mapper/` directory
- Use `@Mapper` annotation on mapper interfaces
- Use transactions (`@Transactional`) appropriately at service layer

#### MyBatis Mapper Patterns
- **Annotation-based**: Use `@Select`, `@Insert`, `@Update`, `@Delete` for simple queries, and for complex query and many parameters using `@SelectProvider`
- **XML-based**: Use mapper XML files for complex queries, dynamic SQL, and result mapping
- **tk.mybatis**: Extend `tk.mybatis.mapper.common.Mapper<T>` for automatic CRUD methods (insert, delete, update, select by primary key, select all)

### Exception Handling
- Use `@ControllerAdvice` for global exception handling
- Create custom exceptions that extend appropriate base classes
- Return consistent error response structure

## Testing

### Test Structure
- Unit tests: Mock dependencies using Mockito
- Integration tests: Use `@SpringBootTest` with appropriate test configuration
- Mapper tests: Use `@MybatisTest` for MyBatis mapper testing with in-memory database
- Controller tests: Use `@WebMvcTest` for web slice testing

### Test Naming
- Test class: `{ClassName}Test` or `{ClassName}Tests`
- Test method: `should{ExpectedBehavior}When{StateUnderTest}`

## Common Spring Boot Annotations

- `@SpringBootApplication` - Main application class
- `@RestController` / `@Controller` - Web layer
- `@Service` - Service layer
- `@Mapper` - MyBatis mapper interface (data access layer)
- `@Configuration` - Configuration classes
- `@Bean` - Bean definition
- `@Autowired` - Dependency injection (prefer constructor injection)
- `@Value` - Property injection
- `@ConfigurationProperties` - Type-safe configuration properties
- `@MapperScan` - Scan for MyBatis mapper interfaces in specified package


## AI Agent Instructions

When working with this Spring Boot codebase:

1. **Understand the layer structure** - Identify if this follows controller → service → mapper pattern before making changes
2. **Check existing patterns** - Look at existing code to understand naming conventions, package structure, and architectural decisions
3. **Maintain consistency** - Follow the existing code style and architectural patterns
4. **Use appropriate annotations** - Ensure proper Spring and MyBatis annotations are used for each component
5. **Test coverage** - Write or update tests when modifying business logic
6. **Configuration management** - Keep sensitive data in environment variables, not in code
7. **Transaction boundaries** - Place `@Transactional` at service layer, not mapper or controller
8. **Error handling** - Use existing exception handling patterns or create consistent new ones
9. **MyBatis mapper organization** - Keep mapper interfaces and XML files organized and named consistently (e.g., `UserMapper.java` and `UserMapper.xml`)


## Code Style

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and concise
- Prefer immutability where possible
- Use Lombok annotations to reduce boilerplate when available

## General Principles

- Follow the **DRY (Don't Repeat Yourself)** principle.
- Follow the **SOLID** principles.
- Write clean and readable code.
- Prefer composition over inheritance unless inheritance is the better choice.
- Keep business logic independent from infrastructure.
- Avoid hardcoded values whenever possible.
- Design components with future reuse in mind.

---

## Before Writing Code

Always analyze the requested feature and identify:

1. Which logic is business-specific.
2. Which logic may be reused by other features.
3. Existing classes or methods that already provide similar functionality.
4. Opportunities to extract reusable components.

Never duplicate an existing implementation if it can be reused or extended.

---

## Process Design

When implementing a business process:

- Break large processes into smaller reusable methods.
- Separate orchestration logic from business logic.
- Extract common workflows into reusable services or helper classes.
- Keep methods focused on a single responsibility.
- Avoid methods that perform multiple unrelated tasks.

---

## Reusable Components

Whenever applicable, create reusable components such as:

- Utility classes
- Helper classes
- Shared services
- Generic services
- Base classes
- Strategy Pattern
- Template Method Pattern
- Factory Pattern
- Builder Pattern
- Adapter Pattern
- Functional interfaces
- Generic methods
- Extension methods (if supported)

Choose the design that provides the highest reusability with the lowest complexity.

---

## Method Design

Each method should:

- Have a single responsibility.
- Be small and easy to understand.
- Have a descriptive name.
- Minimize side effects.
- Be independently testable.
- Return meaningful values.
- Avoid unnecessary dependencies.

---

## Class Design

Each class should:

- Have one clear responsibility.
- Hide implementation details.
- Expose only necessary methods.
- Be loosely coupled.
- Be highly cohesive.
- Be easy to extend without modification.

---

## Code Reuse Rules

Before creating:

- a new Service
- a new Repository
- a new Utility
- a new Mapper
- a new Validator
- a new DTO converter
- a new HTTP client
- a new SQL query
- a new business process

Always check whether an existing implementation already exists.

If one exists:

- Reuse it.
- Extend it.
- Refactor it.

Do **not** duplicate it.

---

## Refactoring Rules

If duplicated logic is detected:

- Extract it into reusable methods.
- Extract common interfaces.
- Introduce shared services.
- Replace duplicated code with reusable abstractions.
- Preserve backward compatibility.

---

## Error Handling

Centralize:

- Exception handling
- Logging
- Retry mechanism
- Validation
- Response formatting

Avoid implementing these repeatedly in every service.

---

## Configuration

Do not hardcode:

- URLs
- Credentials
- Timeout values
- SQL limits
- File paths
- Environment-specific settings

Use configuration files or environment variables.

---

## Documentation

Every reusable component should include:

- Purpose
- Usage
- Parameters
- Return values
- Exceptions
- Example usage (if necessary)

---

## Performance

Prefer reusable implementations that are also:

- Efficient
- Thread-safe (when applicable)
- Stateless (when possible)
- Cache-friendly

Avoid premature optimization.

---

## Testing

Reusable components should be easy to test.

Prefer:

- Unit tests
- Integration tests
- Mockable dependencies

Avoid tightly coupled implementations.

---

## Final Review Checklist

Before completing the implementation, verify:

- [ ] No duplicated business logic.
- [ ] No duplicated SQL.
- [ ] No duplicated validation.
- [ ] No duplicated mapping.
- [ ] No duplicated logging.
- [ ] No duplicated exception handling.
- [ ] No duplicated retry mechanism.
- [ ] Existing reusable components have been reused.
- [ ] New reusable components have been extracted where appropriate.
- [ ] Code follows SOLID principles.
- [ ] Code follows DRY principles.
- [ ] Methods have a single responsibility.
- [ ] Classes have a single responsibility.
- [ ] Code is easy to read and maintain.
- [ ] Code is production-ready.

---

## Expected Output

Every implementation should be:

- Reusable
- Modular
- Extensible
- Maintainable
- Testable
- Well documented
- Production-ready
- Easy to understand by other developers
