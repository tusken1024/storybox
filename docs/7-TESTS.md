# 🧪 Tests

Storybox includes a comprehensive automated test suite covering state machine transitions, edge cases, audio control, and REST endpoints without requiring physical hardware or audio devices:

```bash
# Execute all automated unit and slice tests
./mvnw test
```

### Test Suite Architecture
- **State Machine Isolation**: `StoryEngineTest` thoroughly tests the carousel logic, carousel boundary wrap-around, button progression, reset behavior, and unauthored fallback resolution.
- **Hardware Decoupling**: All tests execute with `@ActiveProfiles("dev")` where `storybox.gpio.enabled=false`. Pi4J hardware initialization is completely bypassed during test runs.
- **Web Controller Slice Testing**: REST controllers (`AudioControllerTest`, `StoryControllerTest`, `StoryGeneratorControllerTest`) use Spring Boot's `@WebMvcTest` with `@MockitoBean` to test HTTP request mapping and error handling in isolation.
