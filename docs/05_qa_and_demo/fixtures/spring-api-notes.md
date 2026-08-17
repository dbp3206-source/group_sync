# Spring Boot REST API Architecture

Spring Boot provides a comprehensive infrastructure for developing robust RESTful web services in Java.

## Layered Architecture

1. **Controller Layer (`@RestController`)**: Handles HTTP requests, deserializes JSON request bodies into Data Transfer Objects (DTOs), validates input parameters, and returns HTTP responses with appropriate status codes (e.g. 200 OK, 201 Created, 204 No Content).
2. **Service Layer (`@Service`)**: Coordinates business transactions, enforces business rules, coordinates domain workflows, and coordinates with persistence and external services. Annotated with `@Transactional`.
3. **Repository Layer (`@Repository`)**: Interacts with the relational database using Spring Data JPA or JDBC templates. Handles CRUD operations and custom query execution.
4. **Model/Entity Layer (`@Entity`)**: Represents the relational database tables with JPA annotations (`@Table`, `@Id`, `@Column`, `@ManyToOne`).

## Key Annotations

- `@GetMapping`, `@PostMapping`, `@PatchMapping`, `@DeleteMapping`: Route HTTP verbs to Java methods.
- `@RequestBody`: Binds the incoming JSON payload to a Java DTO.
- `@Valid`: Triggers Jakarta Bean Validation constraints (e.g., `@NotBlank`, `@Size`, `@NotNull`).
- `@AuthenticationPrincipal`: Injects the currently authenticated user session into controller methods.
