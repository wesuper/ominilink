```markdown
# Omnilink Bridge - JavaSeeker MCP Server

The `omnilink-bridge-javaseeker` module provides a Model Code Provider (MCP) server designed for advanced code analysis of Java projects. It leverages Abstract Syntax Tree (AST) parsing to identify and extract code contexts for references related to a specified code snippet (class or method). This service is intended to supply detailed code information to Large Language Models (LLMs) for further processing and understanding.

## Architecture Overview

The JavaSeeker MCP server is built using Java 21, Spring Boot 3.x, and Spoon for AST analysis.

**Key Components:**

1.  **`McpController` (`org.wesuper.liteai.bridge.javaseeker.web.McpController`)**:
    *   Exposes a REST API endpoint (`/api/mcp/analyze`) to receive analysis requests.
    *   Handles incoming requests, validates parameters, and orchestrates the analysis workflow.

2.  **`ProjectLoaderService` (`org.wesuper.liteai.bridge.javaseeker.project.ProjectLoaderService`)**:
    *   Manages the loading and caching of Java projects.
    *   Supports loading projects from:
        *   **Local Directories**: Specified by a file system path.
        *   **Git Repositories**: Specified by a Git URL and an optional branch name. (Uses JGit for cloning).
    *   `ProjectSource` (interface), `LocalProjectSource`, `GitProjectSource` (implementations) define the abstraction for project sources.

3.  **`CodeAnalysisService` (`org.wesuper.liteai.bridge.javaseeker.analysis.CodeAnalysisService`)**:
    *   Interface for code analysis operations.
    *   `SpoonAnalysisService` is the primary implementation, utilizing the [Spoon](https://spoon.gforge.inria.fr/) library.

4.  **`SpoonAnalysisService` (`org.wesuper.liteai.bridge.javaseeker.analysis.SpoonAnalysisService`)**:
    *   Parses the specified Java project to build an AST.
    *   Locates the AST node corresponding to the input `codeSnippet`.
    *   Traverses the AST to find:
        *   References *to* the target code snippet from other parts of the project or dependencies.
        *   References *from* the target code snippet to other parts of the project or dependencies (e.g., method calls, type usages).
    *   Extracts the full source code of the containing method or class for each identified reference.
    *   Attempts to configure Spoon's classpath based on common project layouts (e.g., `target/classes`, `build/classes`, `target/dependency`) to improve type resolution.

**Workflow:**

1.  An HTTP POST request is sent to `/api/mcp/analyze` with project details and the code snippet to analyze.
2.  `McpController` receives the request.
3.  `ProjectLoaderService` is invoked to load the Java project. If the project is from Git, it's cloned into a temporary directory. Local projects are accessed directly.
4.  `SpoonAnalysisService` is called with the loaded project and the code snippet.
5.  Spoon parses the project's source code (and attempts to use available classpath information for better type resolution).
6.  The service identifies the target element (class or method).
7.  It then searches for all references to and from this element across the codebase (including known parts of dependencies if Spoon can resolve them).
8.  For each reference, the source code of the method or class containing the reference (or being referenced) is extracted.
9.  The results are compiled into an `AnalysisResult` object and returned as a JSON response.

## Configuration & Setup

### Prerequisites

*   Java Development Kit (JDK) 21 or newer.
*   Git client (for cloning repositories).
*   The application is a Spring Boot application and can be run as a standard JAR.

### Project Configuration

The service itself requires no specific external configuration files beyond what's needed to run a Spring Boot application. Project details are provided via the API request.

**Dependency Management:**

*   The project uses Gradle with Kotlin DSL.
*   Dependencies, including Spring Boot, Spring AI, and Spoon, are managed via `build.gradle.kts` and `gradle/libs.versions.toml`.

### Running the Service

1.  Build the module:
    ```bash
    ./gradlew :omnilink-bridge-javaseeker:bootJar
    ```
2.  Run the application:
    ```bash
    java -jar omnilink-bridge-javaseeker/build/libs/omnilink-bridge-javaseeker-0.0.1-SNAPSHOT.jar
    ```
    The server will typically start on port 8080.

## API Usage

### Endpoint: `POST /api/mcp/analyze`

Analyzes a Java project for references related to a given code snippet.

**Request Body (JSON):**

```json
{
  "projectName": "my-java-app",
  "sourceIdentifier": "/path/to/local/my-java-app", // or "https://github.com/user/my-java-app.git"
  "branchName": "main", // Optional, for Git sources
  "codeSnippet": "com.example.MyService#processData(java.lang.String,int)" // or "com.example.MyOtherClass"
}
```

**Parameters:**

*   `projectName` (String, required): A unique identifier for the target project. Used for caching and management.
*   `sourceIdentifier` (String, required): The location of the project.
    *   For local projects: Absolute path to the project's root directory.
    *   For Git projects: The HTTPS or SSH URL of the Git repository.
*   `branchName` (String, optional): The specific branch to check out if the `sourceIdentifier` is a Git repository. Defaults to the repository's default branch (e.g., `main` or `master`) if not specified by JGit.
*   `codeSnippet` (String, required): The fully qualified name of a class or a specific method signature to be analyzed.
    *   Class example: `com.example.MyClass`
    *   Method example: `com.example.MyClass#myMethod(String, int)` or `com.example.MyClass#myMethod()` (ensure parameter types are simple names or fully qualified as they appear in the source, e.g., `String` not `java.lang.String` if not imported that way in the method signature context of Spoon).

**Response Body (JSON on Success - Status 200 OK):**

```json
{
  "analysisTarget": "com.example.MyService#processData(java.lang.String,int)",
  "references": [
    {
      "source": "self", // "self", "dependency:guava-32.1.2-jre.jar", or "jdk"
      "fullyQualifiedName": "com.example.another.SomeOtherClass#someMethod()",
      "codeContext": "public void someMethod() {\n    // ...\n    new MyService().processData(\"test\", 123);\n    // ...\n}"
    },
    {
      "source": "dependency:another-lib-1.0.jar",
      "fullyQualifiedName": "com.thirdparty.HelperUtil#staticHelper(com.example.MyService)",
      "codeContext": "public static void staticHelper(MyService service) {\n    // ... code from the dependency ...\n}"
    },
    {
      "source": "self", // Reference *from* MyService#processData
      "fullyQualifiedName": "com.example.internal.DataProcessor#handle(java.lang.String)",
      "codeContext": "public class MyService {\n    //...\n    public void processData(String s, int i) {\n        new DataProcessor().handle(s);\n    }\n    //...\n}"
    }
    // ... more references
  ]
}
```

**Fields in `references` array:**

*   `source` (String): Indicates the origin of the referenced code.
    *   `"self"`: The reference is within the analyzed project itself.
    *   `"dependency:<jar_name>"`: The reference is from a dependency JAR (e.g., `dependency:guava-32.1.2-jre.jar`). The name is derived from the JAR file.
    *   `"jdk"`: The reference is to a core JDK class.
*   `fullyQualifiedName` (String): The fully qualified name of the class or method that either *contains* the reference (if looking for references *to* the target) or *is* the reference itself (if looking for references *from* the target).
*   `codeContext` (String): The complete source code of the method or class associated with this reference.
    *   If the reference is *to* the `analysisTarget`: `codeContext` is the source of the method/class *containing* the call/usage of the `analysisTarget`.
    *   If the reference is *from* the `analysisTarget`: `codeContext` is the source of the `analysisTarget` itself (showing where the call *is made from*), and `fullyQualifiedName` is the item being called/used.

**Error Responses:**

*   **400 Bad Request**: If required request parameters are missing or invalid.
    ```json
    "Missing required fields: projectName, sourceIdentifier, codeSnippet"
    ```
*   **500 Internal Server Error**: If there's an issue loading the project or an unexpected error during the analysis process.
    ```json
    "Failed to load project: my-java-app from /path/to/local/my-java-app"
    ```
    ```json
    "An error occurred during code analysis: <specific error message>"
    ```

### Example cURL Request

**Local Project:**
```bash
curl -X POST -H "Content-Type: application/json" -d '{
  "projectName": "my-local-project",
  "sourceIdentifier": "/Users/jules/dev/sample-java-project",
  "codeSnippet": "com.example.Main#main(java.lang.String[])"
}' http://localhost:8080/api/mcp/analyze
```

**Git Project:**
```bash
curl -X POST -H "Content-Type: application/json" -d '{
  "projectName": "spring-petclinic",
  "sourceIdentifier": "https://github.com/spring-projects/spring-petclinic.git",
  "branchName": "main",
  "codeSnippet": "org.springframework.samples.petclinic.owner.OwnerController#initCreationForm(java.util.Map)"
}' http://localhost:8080/api/mcp/analyze
```
```
