## Design Principles

### I. Immutability First
All data structures are immutable by default. Records are the preferred choice for data containers. Methods must never share mutable state. This ensures thread safety and eliminates an entire class of bugs related to state management.

### II. Interface-Driven
Interfaces are used exclusively for externalizing functionality at module boundaries. Internal module components must use direct class references. Modules must expose and communicate through well-defined interfaces only at their boundaries. Implementation details remain hidden from other modules, promoting loose coupling and enabling independent evolution.

### III. Modular Independence  
Each package that contains a `module-info.java` represents a complete vertical slice module. Modules must never create direct dependencies on each other. ArchUnit tests enforce strict module boundaries to prevent architectural drift and maintain clean separation of concerns.

**Module Boundary Rules:**
- Each package (e.g., `ask`, `search`, `playwright`, `config`) is an independent module
- Modules communicate only through interfaces defined in their own or shared packages
- No direct class imports between business modules (ask ↔ search forbidden)
- Shared infrastructure (config, api) can be imported by business modules
- Module APIs must use proper Java naming conventions (PascalCase for classes/interfaces)

### IV. Code Expression
- Code must express intent, never implementation details. All names must follow Coleman–Liau readability guidelines with an index between 1 and 4. 
- If a comment feels necessary just to explain a name, improve the name instead.


**Java Optional Usage (MANDATORY):**
- Optional types are ONLY permitted as method return types to indicate nullable results
- Optional is FORBIDDEN as method parameters - use overloaded methods or nullable parameters with @Nullable annotation instead
- Optional is FORBIDDEN for local variables and field declarations - use direct null checks or @Nullable annotations
- Optional chaining in return statements is encouraged (e.g., `return Optional.of(result).filter(this::isValid)`)

Rationale: Optional as parameters creates confusing APIs and forces callers to wrap values unnecessarily. Optional fields waste memory and complicate serialization. Optional should express "this method might not return a value" rather than "this parameter might be null".

**Java Text Blocks for Multi-line Strings (MANDATORY):**
- Text blocks (triple quotes) MUST be used for any multi-line string literals instead of concatenation or escape sequences
- Traditional string concatenation with `\n` escapes is FORBIDDEN for multi-line content
- StringBuilder patterns with multiple `.append()` calls for static multi-line strings MUST use text blocks
- Text blocks eliminate platform-specific line ending dependencies and improve readability
- Dynamic content can still be inserted using `.append()` or string formatting within text block contexts

**FORBIDDEN Patterns:**
```java
// FORBIDDEN: Platform-dependent escape sequences  
String message = "Error occurred\nPlease try again\nContact support";
formatted.append("**Error**\n").append("Details: ").append(details).append("\n");
```

**REQUIRED Patterns:**
```java
// REQUIRED: Text blocks for cross-platform compatibility
String message = """
    Error occurred
    Please try again
    Contact support""";
    
formatted.append("""
    **Error**
    Details: """).append(details);
```

Rationale: Text blocks provide platform-independent line ending handling, eliminate escape sequence complexity, and improve multi-line string maintainability. Hardcoded `\n` sequences can cause inconsistent formatting across Windows (CRLF), macOS (LF), and Linux (LF) environments.

### VI. Single Responsibility
Every class, module, and method must have exactly one reason to change (Single Responsibility Principle). Each component must perform one cohesive task and encapsulate one business concept or technical concern. Changes to one aspect of the system should not require modifications to unrelated components.

### VII. File-System & Platform Independence
All file paths, keystore locations, and filesystem-backed resources MUST be handled in an OS-agnostic way. The application MUST expose a configurable base path for runtime artifacts (certs, keystores, temp files) and MUST use the language/runtime's path APIs to normalize and resolve paths (e.g., Java's `Path.resolve`, Node's `path.join`). Hard-coded path separators or platform-specific path assumptions are forbidden. This ensures the software runs correctly on Windows, macOS, and Linux without code changes.

Rationale: Development and CI environments vary in path conventions and separators. Enforcing runtime normalization and config-driven base paths prevents environment-specific bugs and makes local dev, CI, and containerized deployments consistent.

### VIII. Dependency Stability & Registry Verification
All third-party dependencies MUST be stable releases (no `-SNAPSHOT`, `-RC`, or experimental pre-releases) unless an explicit, time-boxed exception is approved with a documented migration plan. New libraries SHOULD use the latest stable release available from the primary registry for that ecosystem (e.g., Maven Central for Java, npmjs.org for JavaScript). When adding or upgrading dependencies, the author MUST:

- Verify the artifact coordinates and latest stable version from the official registry (e.g., Maven Central search for Java artifacts).
- Prefer non-experimental, non-preview, GA releases. If an experimental feature is considered, document risk, fallback plan, and pin a Jira/issue tracking the follow-up to move to a GA release.
- Avoid using snapshot or nightly builds in mainline branches or CI by default.
- Document any transitive dependency upgrades that could affect compatibility and run a dependency vulnerability scan as part of CI.

Rationale: Experimental or snapshot dependencies introduce instability, unexpected breakage, and can complicate reproducible builds. Requiring registry verification and GA releases improves reproducibility, security, and operational stability.

### Core Patterns
- Records by default for data structures
- Builders only for complex object construction
- Composition over inheritance
- Interfaces only at module boundaries
- Direct class usage within modules
- Pure methods with no side effects
- Single responsibility for all components
- Clear separation of concerns
- One business concept per class
- Cohesive method grouping
 - Path normalization and base-path config for filesystem-backed resources
- Fail fast validation 

### Module Design
- Vertical slice architecture
- No cross-module dependencies
- Interfaces only at module boundaries
- Direct class references within modules
- Explicit module boundaries
- Independent testing per module

### Complex Object Construction
- Classes or records with more than 3 attributes MUST use Lombok @Builder annotation
- @Builder enables fluent, immutable object construction for complex data structures
- Prevents telescoping constructor anti-pattern while maintaining immutability
- Builder pattern is mandatory, not optional, for classes exceeding 3-attribute threshold

### Testing Requirements
- Unit tests for all business logic
- Integration tests for module boundaries
- ArchUnit tests for architectural rules
- No untested public interfaces
- Test coverage metrics maintained

### Test Structure and Style
- Test methods must follow Given-When-Then structure through clear method naming and variable organization
- Unit under test must be named 'uut'
- Use Mockito extension (@ExtendWith(MockitoExtension.class))
- Mock dependencies with @Mock annotation
- Create beans using actual constructors, not @InjectMocks
- Test methods must have descriptive names that express the complete scenario without comments
- No comments in test code - test method names and variable names must be completely self-explanatory

### Documentation Standards
- Code must be self-documenting through clear naming and structure
- Comments are FORBIDDEN unless absolutely required to explain "why" for extremely complex business logic
- Javadoc is completely prohibited - class and method names must express complete intent
- No explanatory comments, implementation notes, or "what" documentation in source code
- Self-explanatory code through proper naming eliminates need for comments

### Nullability and Validation
- JSpecify annotations required for nullability declarations
- All properties are non-null by default
- @Nullable annotation only for exceptional cases
- Lombok @RequiredArgsConstructor for dependency injection
- Records for immutable data structures

### Configuration Management
- Each module must have its own Config class (e.g., `SearchConfig` for search module, `McpConfig` for mcp module)
- All Spring components MUST be assembled via `@Bean` methods in their module's Config class
- `@Component`, `@Service` annotations are FORBIDDEN - use explicit `@Bean` declarations instead
- Bean declarations must be explicit, module-scoped, and use constructor injection
- Configuration properties must be validated at startup

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

- Filesystem-backed configuration (cert paths, keystores, data dirs) MUST be configurable via environment variables or config files using normalized, platform-independent paths. Document recommended default relative paths (e.g., `./certs`) and allow overrides with absolute paths.

## Version Control Standards

#### Commit Format
<type>(<optional scope>): <description>

<optional body>

<optional footer>

#### Types
- feat: user-facing feature
- fix: bug fix
- refactor: behavior-preserving restructure
- perf: performance improvement
- style: formatting only
- test: tests only
- docs: documentation only
- build: build, deps, versions
- ops: infra, CI/CD, deployment
- chore: maintenance

#### Rules
- description is mandatory, imperative, lowercase, no period
- scope is optional and project-defined
- use ! for breaking changes: feat(api)!: remove endpoint
- breaking changes must include:
BREAKING CHANGE: <description>

#### Versioning
- breaking -> major
- feat/fix -> minor
- else -> patch

#### Examples
feat: add email notifications
fix(api): correct checksum calculation
refactor: simplify request pipeline
build: update dependencies



**Version**: 1.11.0 | **Ratified**: 2025-10-25 | **Last Amended**: 2025-10-25