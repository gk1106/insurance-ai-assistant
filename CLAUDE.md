# Insurance AI Assistant

## Project Purpose
A learning project: an AI-powered Insurance Assistant that will eventually demonstrate Claude, Spring AI, tool calling, RAG, MCP, Skills, subagents, and agent orchestration — built on top of a conventional Spring Boot backend.

## Technology
- Java 17
- Spring Boot 3
- Maven
- PostgreSQL
- Flyway
- MapStruct
- Spring Security + JWT
- React
- Spring AI
- Claude

## Backend Conventions
- Feature/domain-based packages (e.g. `customer`, `policy`, `claim`, `renewal`), each layered internally
- Strict flow: Controller → Service → Repository
- DTOs for all API input/output — never expose JPA entities directly
- Constructor injection only (no field injection)
- Bean Validation (`@Valid`, `jakarta.validation`) on request DTOs
- Global exception handling via `@ControllerAdvice`
- Unit tests for business logic (service layer, especially business rules)

## Security
- Never hardcode secrets or API keys — use config/environment variables
- Never expose passwords or secrets in responses, logs, or DTOs
- Authorization must remain in backend code, never trust a client- or LLM-supplied role/permission
- AI must not directly access the database

## AI Rules
- Deterministic business rules must remain in Java, not delegated to the LLM
- AI should use approved tools for business operations, not ad hoc logic
- Use RAG for grounded, document-based answers
- Validate AI-generated structured output before acting on it
- Never allow the LLM to bypass authorization

## Development Workflow
- Analyze before modifying
- Make small, incremental changes
- Do not modify unrelated files
- Run tests after implementation
- Explain important architectural decisions
- Prefer simple solutions suitable for a learning project
