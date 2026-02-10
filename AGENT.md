# Agent Instructions (Detailed)

## 1. Design Principles
- Immutability first: prefer records and avoid shared mutable state.
- Interface-driven boundaries: interfaces only at module boundaries; internal components use direct class references.
- Modular independence: each `module-info.java` package is a vertical slice; no cross-module dependencies.
- Code expression: names must carry intent; avoid comments by improving names.
- Single responsibility: one reason to change for each class/module/method.
- Platform independence: all filesystem paths must be OS-agnostic and configurable.
- Dependency stability: only stable releases; verify coordinates in the primary registry.
- Avoid try/catch blocks that suppress exceptions. Every exception MUST be either rethrown (potentially wrapped) or logged with the appropriate level (ERROR/WARN) depending on the context. Exceptions must NEVER be silently omitted.

## 2. Architecture Rules
**Module Boundary Rules:**
- Each package (e.g., `ask`, `search`, `playwright`, `config`) is an independent module.
- Modules communicate only through interfaces defined in their own or shared packages.
- No direct class imports between business modules (ask ↔ search forbidden).
- Shared infrastructure (config, api) can be imported by business modules.
- Module APIs must use proper Java naming conventions (PascalCase for classes/interfaces).

**Core Patterns:**
- Records by default for data structures.
- Builders only for complex object construction.
- Composition over inheritance.
- Pure methods with no side effects.
- Clear separation of concerns.
- One business concept per class.
- Fail-fast validation.
- **Short-Circuiting (Guard Clauses)**: Prefer early exits/guard clauses at the beginning of methods to reduce nesting and improve readability.
    - Example:
      ```java
      public void process(Data data) {
          if (data == null || data.isInvalid()) {
              return;
          }
          // main logic follows
      }
      ```
- **CDC State Synchronization**: When processing changelogs for key-value (Upsert) sinks:
    - `DELETE` events must trigger a tombstone to clear the state.
    - `UPDATE_BEFORE` events should be **ignored** to avoid redundant tombstone-upsert pairs, as the subsequent `UPDATE_AFTER` handles the update.

**Import Rules:**
- Use explicit/qualified imports (e.g., `import java.util.List;`) whenever possible.
- Avoid wildcard imports (`import java.util.*;`).
- Avoid fully qualified names in code unless necessary for disambiguation.

## 3. Java-Specific Rules
**Optional Usage (MANDATORY):**
- Optional types are ONLY permitted as method return types.
- Optional is FORBIDDEN as method parameters, local variables, and fields.
- Optional chaining in return statements is encouraged.

**Text Blocks for Multi-line Strings (MANDATORY):**
- Use text blocks for any multi-line string literal.
- Traditional `\n` concatenation or `StringBuilder` for static multi-line strings is FORBIDDEN.

- **Visibility Modifiers**:
    - **Default to Package-Private**: All classes, interfaces, records, and beans should be `package-private` unless they are explicitly required to be public (e.g., Spring Boot application entry points, cross-module API interfaces, domain models).
    - **Strict Least Privilege**: Always start with `private` or `package-private`. Only promote to `public` if a compilation error or runtime requirement dictates it.
    - **Review Rule**: Revisit all access modifiers during implementation to confirm they follow the least visibility principle.

**Lambda Usage (MANDATORY):**
- Avoid calling methods that take two or more lambda arguments in a row (e.g., `Optional.ifPresentOrElse(..)`). Use traditional `isPresent()` / `if` or other constructs to maintain high readability.

**Required Pattern:**
```java
String message = """
    Error occurred
    Please try again
    Contact support""";
```

**Forbidden Pattern:**
```java
String message = "Error occurred\nPlease try again\nContact support";
```

## 4. Object Construction
- Use Lombok `@RequiredArgsConstructor` for constructor generation.
- Classes or records with 4 or more attributes MUST also use Lombok `@Builder`.
    - When using `@Builder` on a class, `@RequiredArgsConstructor` is still needed for dependency injection or if the class is instantiated via constructor elsewhere.
    - If `@Builder` covers all construction needs, `@RequiredArgsConstructor` *may* be omitted, but `@AllArgsConstructor` (package-private) is typically required by `@Builder`.
- Prevent telescoping constructors while maintaining immutability.
- **Custom Exceptions:**
    - Use static inner classes for custom exceptions to keep them close to their usage context.
    - Exceptions should be expressive and specific to the failure mode.
    - All exceptions must be handled: either thrown to the caller or logged with a proper level (e.g., SLF4J log.error). Silently ignoring exceptions is strictly FORBIDDEN.

- **Variable Names**:
    - Must be descriptive and use `camelCase`.
    - Single-letter variable names are acceptable ONLY in lambda expressions or loop counters (e.g., `i`).
    - **Type Alignment**: Variable names must align with their type. Do not use alias names that differ significantly from the type (e.g., do NOT name a parameter of type `SnapshotHandler` as `orchestrator`; instead use `snapshotHandler` or `handler`).

## 5. Testing Standards
- Unit tests for all business logic.
- Integration tests for module boundaries.
- ArchUnit tests for architectural rules.
- No untested public interfaces.

**Test Structure and Style:**
- Test methods follow Given-When-Then via naming and variable organization.
- Unit under test must be named `uut`.
- Use Mockito extension (`@ExtendWith(MockitoExtension.class)`).
- Mock dependencies with `@Mock`.
- Create beans using actual constructors, not `@InjectMocks`.
- No comments in test code.

## 6. Documentation Standards
- Code must be self-documenting through clear naming and structure.
- Comments are FORBIDDEN unless required to explain “why” in extremely complex logic.
- Javadoc is prohibited; names must express intent.

## 7. Nullability and Validation
- **Null Safety Enforcement**: NullAway (via Error Prone) is used to enforce null safety at compile time.
- **Non-Null by Default**: All classes within the `edu.jmaycon` package tree are **non-null by default**.
- **JSpecify Annotations**:
    - Use `org.jspecify.annotations.Nullable` to explicitly mark any field, parameter, or return type that can be null.
    - Do NOT use `@NonNull` or equivalent annotations; non-null is the implicit and enforced default.
- **Validation**:
    - Any attempt to pass a potentially null value where non-null is expected will trigger a compilation error.
    - Dereferencing a `@Nullable` variable without a prior null check will trigger a compilation error.
- **Lombok Alignment**: Lombok `@RequiredArgsConstructor` correctly handles dependency injection for non-null fields.
- **Records**: Use records for immutable data structures, applying `@Nullable` to components only when necessary.

## 8. Configuration Management
- Each module must have its own Config class (e.g., `SearchConfig`).
- All Spring components MUST be assembled via `@Bean` methods in module Config.
- `@Component` and `@Service` are FORBIDDEN.
- Bean declarations must be explicit and module-scoped.
- Configuration properties must be validated at startup.
- Filesystem-backed configuration must be configurable via env or config files using normalized paths.

**Spring Boot @Bean Wiring Pattern:**
```java
@Configuration
public class SearchConfig {

    @Bean
    public SearchService searchService(BrowserHost browserHost) {
        return new SearchService(browserHost);
    }

    @Bean
    public SearchTool searchTool(SearchService searchService) {
        return new SearchTool(searchService);
    }
}
```

## 9. Version Control Standards
**Commit Format:**
```
<type>(<optional scope>): <description>

<optional body>

<optional footer>
```

**Types:**
- feat, fix, refactor, perf, style, test, docs, build, ops, chore

**Rules:**
- Description is imperative, lowercase, no period.
- Scope is optional.
- Use `!` for breaking changes: `feat(api)!: remove endpoint`.
- Breaking changes must include:
  `BREAKING CHANGE: <description>`

**Versioning:**
- breaking -> major
- feat/fix -> minor
- else -> patch

**Examples:**
- `feat: add email notifications`
- `fix(api): correct checksum calculation`
- `refactor: simplify request pipeline`
- `build: update dependencies`

## 10. Code Organization
- **Member Ordering**:
    - Within a class or record, order methods with the same access modifier by relevance and meaning (most significant business logic first).
    - All static members (static fields, static methods, constants and static initialization blocks) MUST be placed at the beginning of the class.
    - Static inner classes MUST be placed at the bottom of the file.

## 11. Naming Conventions
- **Class Suffixes**: Use descriptive suffixes to indicate the role of a class or interface:
    - `Handler`: For interfaces or classes that handle events or specific actions (e.g., `SnapshotHandler`).
    - `Listener`: For components that react to external triggers or messages (e.g., `SqsSnapshotListener`).
    - `Mapper`: For components that transform data between different representations (e.g., `FlightTicketRowMapper`).
    - `Reader`: For components that fetch data from external sources (e.g., `IcebergSnapshotReader`).
    - `Module`: For Spring `@Configuration` classes representing a module (e.g., `SourceModule`).
    - `Properties`: For `@ConfigurationProperties` classes (e.g., `SourceModule.Properties`).

## 12. Module Config Pattern
- **Configuration Classes**:
    - Each module should have its own `@Configuration` class (e.g., `SourceModule`).
    - Prefer `package-private` visibility for the configuration class and its `@Bean` methods, unless public access is required by other modules.
- **Properties**:
    - Use `record` classes for `@ConfigurationProperties`.
    - Do NOT use Lombok (`@Getter`, `@Setter`, etc.) on property records.
    - Property records should be inner types of the module configuration class and `package-private`.
    - Example:
      ```java
      @Configuration
      @EnableConfigurationProperties(MyModule.Properties.class)
      class MyModule {
          @ConfigurationProperties(prefix = "app.my-module")
          record Properties(String host, int port) {}
      }
      ```
- **Rationale**:
    - **Isolation**: Allows each module to be tested in isolation without loading the entire application context.
    - **Reduced Coupling**: Decouples packages by preventing cross-module bean leaking.
    - **Encapsulation**: Enforces package-level visibility (package-private) to ensure internal components remain internal.

---
**Version**: 1.15.0 | **Ratified**: 2025-10-25 | **Last Amended**: 2026-02-10
