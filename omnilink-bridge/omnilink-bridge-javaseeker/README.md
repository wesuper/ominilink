# Omnilink Bridge - JavaSeeker MCP Server (Headless SSE)

The `omnilink-bridge-javaseeker` module provides a headless Model Code Provider (MCP) server designed for advanced code analysis of Java projects. It leverages Abstract Syntax Tree (AST) parsing via the Spoon library to identify and extract code contexts for references related to a specified code snippet (class or method). This service operates without a traditional web server and streams analysis results exclusively via WebFlux-powered Server-Sent Events (SSE). Project configurations are managed dynamically through an external YAML file.

## Architecture Overview

The JavaSeeker MCP server is built using Java 21, Spring Boot 3.x (in headless mode), Spring AI, and Spoon.

**Key Components:**

1.  **`McpJavaSeekerApplication` (`com.omnilink.bridge.javaseeker.McpJavaSeekerApplication`)**:
    *   The main Spring Boot application class, configured to run with `WebApplicationType.NONE` (headless).
    *   Enables component scanning and scheduling.

2.  **`ProjectConfigurationManager` (`com.omnilink.bridge.javaseeker.service.ProjectConfigurationManager`)**:
    *   Manages the list of analyzable Java projects via an external YAML file (default: `javaseeker-projects.yml`).
    *   Loads and parses the YAML file at startup.
    *   Monitors the YAML file for changes at runtime and dynamically updates project configurations without requiring an application restart.
    *   Resolves `localCachePath` for projects and ensures these directories exist.

3.  **`ProjectLifecycleService` (`com.omnilink.bridge.javaseeker.service.ProjectLifecycleService`)**:
    *   Manages the lifecycle of projects defined in `javaseeker-projects.yml`.
    *   **Syncing**: For `git` projects, clones new repositories or pulls updates for existing ones into their `localCachePath` using JGit. Updates project status (e.g., `SYNCING`, `FAILED_SYNC`).
    *   **Compilation**: After syncing (for Git) or for local projects, triggers a build command (`mvn compile` or `gradle build -x test`) in the project's directory to resolve dependencies and prepare for analysis. Updates project status (e.g., `COMPILING`, `READY`, `FAILED_BUILD`).
    *   Operates periodically via scheduled tasks.

4.  **`JavaCodeAnalysisTool` (`com.omnilink.bridge.javaseeker.tool.JavaCodeAnalysisTool`)**:
    *   The core Spring AI `@Tool` responsible for code analysis.
    *   Accepts `projectName` and `codeSnippet` as input via a `JavaCodeReferenceRequest` record.
    *   Verifies the project is in `READY` status using `ProjectConfigurationManager`.
    *   Uses Spoon to parse the project's source code from its `localCachePath`.
    *   Finds references *to* and *from* the target code snippet.
    *   Returns a `Flux<AnalysisReference>`, streaming each found reference as an individual item.

5.  **SSE Endpoint (via `spring-ai-mcp-webflux`)**:
    *   The `spring-ai-mcp-webflux` dependency automatically exposes `@Tool`-annotated methods that return a `Flux` as Server-Sent Event (SSE) streams.
    *   The endpoint for `JavaCodeAnalysisTool`'s `analyzeCodeReferences` method will typically be `/mcp/flux/analyzeJavaCodeReferences`.

**Workflow:**

1.  The `McpJavaSeekerApplication` starts.
2.  `ProjectConfigurationManager` loads `javaseeker-projects.yml` and starts monitoring it.
3.  `ProjectLifecycleService` periodically processes projects:
    *   Clones/pulls Git projects.
    *   Compiles projects to make them `READY`.
4.  A client connects to the SSE endpoint (e.g., `/mcp/flux/analyzeJavaCodeReferences?projectName=my-project&codeSnippet=com.example.MyClass`). The request parameters are passed as query parameters for GET requests, which is the typical method for `spring-ai-mcp-webflux` Flux tools.
5.  `JavaCodeAnalysisTool` receives the request (Spring AI maps query parameters to the `JavaCodeReferenceRequest` record).
6.  It validates the project's status.
7.  If `READY`, it performs Spoon AST analysis on the project's code in `localCachePath`.
8.  Each identified `AnalysisReference` is streamed back to the client as an SSE event.

## Configuration & Setup

### Prerequisites

*   Java Development Kit (JDK) 21 or newer.
*   Git client (for cloning repositories by the service).
*   Maven and/or Gradle installed and available on the system path if projects managed by this service require them for compilation and don't have wrappers (`mvnw`/`gradlew`).

### Project Configuration File (`javaseeker-projects.yml`)

The service requires a configuration file named `javaseeker-projects.yml` to define the Java projects to be analyzed.

*   **Location**:
    *   If a file specified by the `javaseeker.config.file` Spring property (default: `javaseeker-projects.yml`) exists as an absolute path, it will be used directly.
    *   Otherwise, the service attempts to copy `javaseeker-projects.yml` from the classpath to a directory named `.` (relative to where the application is run) and uses that external copy.
    *   If not found in classpath, an empty default `javaseeker-projects.yml` is created in the `.` directory.
*   **Dynamic Reloading**: The external `javaseeker-projects.yml` is monitored for changes, and configurations are reloaded automatically.

**Schema for `javaseeker-projects.yml`:**

```yaml
projects:
  - name: "project-alpha-service"        # Unique identifier for the project
    sourceType: "git"                     # Type of source: "git" or "local"
    location: "https://github.com/your-org/project-alpha.git" # Git URL or local file path
    branch: "main"                        # Optional: Git branch to checkout
    localCachePath: ".cache/project-alpha" # Path to store/access the project. Relative paths are resolved from app's CWD.
                                          # Defaults to ./.cache/<project-name> if not specified.
    status: "NOT_SYNCED"                  # Initial status (managed by the service, typically "NOT_SYNCED" for new git projects)
                                          # Lifecycle: NOT_SYNCED -> SYNCING -> COMPILING -> READY / FAILED (or FAILED_SYNC, FAILED_BUILD etc.)
  - name: "legacy-beta-library"
    sourceType: "local"
    location: "/mnt/legacy/beta-lib"      # Absolute path to the local project
    localCachePath: "/mnt/legacy/beta-lib"  # For local, can be same as location if no separate cache needed
    status: "READY"                       # Local projects might be initially READY or need compilation
```
**Note on `localCachePath`**: If a relative path is provided, it will be resolved relative to the current working directory of the application. It's recommended to use absolute paths or ensure the application is run from a consistent location. The application will attempt to create this directory if it doesn't exist.

### Running the Service

1.  Ensure `javaseeker-projects.yml` is configured and accessible as described above.
2.  Build the module:
    ```bash
    ./gradlew :omnilink-bridge:omnilink-bridge-javaseeker:bootJar
    ```
3.  Run the application:
    ```bash
    java -jar omnilink-bridge/omnilink-bridge-javaseeker/build/libs/omnilink-bridge-javaseeker-*.jar
    ```
    (The exact JAR name might vary based on version. Check the `build/libs` directory.)

## API Usage (Server-Sent Events - SSE)

The primary way to interact with the service is by connecting to its Server-Sent Events (SSE) endpoint. The `spring-ai-mcp-webflux` framework automatically exposes `@Tool` methods that return a `Flux` as SSE streams.

**Endpoint for `analyzeJavaCodeReferences` tool:**

`GET /mcp/flux/analyzeJavaCodeReferences`

**Query Parameters (for GET requests):**

*   `projectName` (String, required): The unique name of the project (must match a name in `javaseeker-projects.yml`).
*   `codeSnippet` (String, required): The fully qualified class name or method signature to analyze (e.g., `com.example.MyClass` or `com.example.MyClass#myMethod(String,int)`).

**Example Interaction (using `curl` for GET):**

```bash
curl -N "http://localhost:8080/mcp/flux/analyzeJavaCodeReferences?projectName=project-alpha-service&codeSnippet=com.example.TargetService%23process"
```
*(Note: Default Spring Boot port is 8080. Adjust if your application runs on a different port. `#` in `codeSnippet` should be URL-encoded to `%23` for GET requests.)*

**SSE Event Format:**

Each event in the stream will be a JSON object representing an `AnalysisReference`:

```
event: message
data: {"source":"self","fullyQualifiedName":"com.example.another.SomeOtherClass#someMethod()","codeContext":"public void someMethod() { /* ... code ... */ }","referenceType":"TO"}
```
*(The actual `data:` line will be a single line of JSON. The above is formatted for readability.)*

*   `source` (String): `"self"`, `"dependency:<jar_name>"`, or `"jdk"`.
*   `fullyQualifiedName` (String): FQN of the class/method associated with the reference.
*   `codeContext` (String): Full source code of the method or class.
*   `referenceType` (String): `"TO"` (points to the analysis target) or `"FROM"` (points from the analysis target).

The stream will send multiple such events, one for each reference found. The stream completes when the analysis is finished for the requested snippet. If an error occurs (e.g., project not ready, target not found), the SSE stream might terminate with an error event or close prematurely.
