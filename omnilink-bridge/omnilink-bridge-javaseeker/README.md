# Omnilink Bridge - JavaSeeker Module

The JavaSeeker module is a headless, reactive Model Code Provider (MCP) server designed for deep Java code analysis. It integrates with Large Language Models (LLMs) by exposing its analysis capabilities as a Spring AI Tool, communicating via Server-Sent Events (SSE).

## 1. Architecture Overview

JavaSeeker operates as a fully headless Spring Boot application. Key architectural features include:

*   **Reactive MCP Server**: Built using `spring-ai-mcp-webflux`, enabling a reactive, streaming API suitable for LLM interactions. It does not use traditional REST controllers or servlets.
*   **Spring AI Tool**: The core code analysis functionality is exposed as a function callable by LLMs, using the `@Tool` annotation from the Spring AI framework.
*   **Dynamic Project Configuration**: Project sources for analysis are managed via an external YAML file (`javaseeker-projects.yml`). The service monitors this file for changes and dynamically reloads configurations without requiring a restart.
*   **Automated Project Lifecycle**: The module automatically manages the lifecycle of configured Java projects, including cloning/pulling from Git, compiling dependencies, and tracking their status.

## 2. Configuration (`javaseeker-projects.yml`)

The primary configuration for JavaSeeker is the `javaseeker-projects.yml` file.

*   **Location**:
    *   On first run, if `javaseeker-projects.yml` is present in the application's classpath (e.g., under `src/main/resources`), it will be copied to the application's working directory.
    *   If not found in the classpath, an empty default file will be created in the working directory.
    *   The application then monitors the `javaseeker-projects.yml` file in the working directory for any changes.
*   **Purpose**: This file defines all Java projects that the JavaSeeker service can analyze. Modifications (additions, removals, changes to existing projects) are detected and applied dynamically.

### Schema

The YAML file structure consists of a root `projects` list, where each item defines a project:

```yaml
projects:
  - name: "unique-project-name"        # (Required) A unique identifier for the project.
    sourceType: "git"                  # (Required) Type of the project source. Options: "git", "local".
    location: "https://github.com/your-org/project.git" # (Required)
                                       # For "git": The repository URL.
                                       # For "local": Absolute or relative file system path to the project.
    branch: "main"                     # (Optional) For "git" sources. Specifies the branch to use.
                                       # Defaults to the repository's default branch if omitted.
    localCachePath: ".cache/project-name" # (Optional) Path to cache/store the project locally.
                                       # If relative, resolved against the app's working directory.
                                       # Defaults to "./.cache/<project-name>" if omitted.
                                       # For "local" sourceType, if 'location' is absolute and this is omitted,
                                       # 'location' is used directly.
    status: "NOT_SYNCED"               # (Internal) Managed by the service. Tracks the project's current state.
                                       # User should generally not set this manually.
```

### Field Explanations:

*   `name`: A unique string identifying the project. Used when requesting analysis.
*   `sourceType`:
    *   `git`: The project source is a Git repository.
    *   `local`: The project source is a directory on the local file system.
*   `location`:
    *   If `sourceType` is `git`, this is the HTTPS or SSH URL of the Git repository.
    *   If `sourceType` is `local`, this is the file system path to the project's root directory.
*   `branch`: (Optional, for `git` type only) The specific Git branch to clone or pull from.
*   `localCachePath`: The directory where the project's source code will be stored and processed. For `git` projects, this is where the repository is cloned. For `local` projects, this usually points to the same `location` or can be omitted if `location` is absolute. The service ensures this path is an absolute path.
*   `status`: (Read-only for users) Indicates the current state of the project within the JavaSeeker service. See "Project Lifecycle" below.

## 3. Project Lifecycle and Statuses

The JavaSeeker service manages each configured project through several lifecycle states. The `status` field in `javaseeker-projects.yml` (though primarily managed internally) reflects this state. Only projects in the `READY` state are available for analysis.

*   **`NOT_SYNCED`**: Initial state for a new project or a project that needs to be re-processed.
*   **`SYNCING`**: For `git` projects, the service is currently cloning the repository or pulling updates.
*   **`FAILED_SYNC`**: A Git operation (clone/pull) failed due to network issues, authentication problems, or other Git errors.
*   **`FAILED_MERGE_CONFLICT`**: A `git pull` resulted in merge conflicts that require manual intervention in the `localCachePath`.
*   **`COMPILING`**: The project source code (after syncing for Git projects, or directly for local projects) is being compiled (e.g., `mvn dependency:copy-dependencies compile` or `gradle build -x test`). This step resolves dependencies.
*   **`FAILED_BUILD`**: The compilation command (Maven/Gradle) failed. Check service logs for build errors.
*   **`FAILED_BUILD_TIMEOUT`**: The compilation command exceeded the allowed time limit.
*   **`FAILED_BUILD_EXCEPTION`**: An unexpected error occurred while trying to execute the build command.
*   **`READY_NO_BUILD_FILE`**: For both `git` and `local` projects, if no `pom.xml` or `build.gradle[.kts]` is found in the root of `localCachePath`. The project's source code is available, but dependencies are not compiled automatically. Analysis might be limited.
*   **`READY`**: The project is successfully synced (if applicable) and compiled. It is ready for code analysis.
*   **`FAILED_INVALID_PATH`**: For `local` projects, the specified `location` does not exist or is not a directory.
*   **`FAILED_UNEXPECTED_ERROR`**: An unexpected error occurred during the project's lifecycle processing.

The service periodically checks and updates project states.

## 4. API Usage (Tool Invocation)

JavaSeeker exposes its code analysis capabilities as a Spring AI Tool that can be invoked by an LLM.

*   **Tool Name**: `analyzeJavaCodeReferences`

*   **Input Request**:
    The LLM should provide a JSON object with the following fields:
    *   `projectName` (string): The unique name of the project to analyze (must match a `name` in `javaseeker-projects.yml` and the project must be in `READY` state).
    *   `codeSnippet` (string): The code element to analyze. This can be:
        *   A fully qualified class name (e.g., `"com.example.MyClass"`)
        *   A fully qualified method signature (e.g., `"com.example.MyClass#myMethod(java.lang.String,int)"`). Parameter types should be fully qualified or simple names if unambiguous (e.g., `String`, `int`).

    Example Request JSON:
    ```json
    {
      "projectName": "example-spring-petclinic",
      "codeSnippet": "org.springframework.samples.petclinic.vet.VetController#showVetList(int, java.util.Map)"
    }
    ```

*   **Output Stream (Server-Sent Events - SSE)**:
    The tool returns a reactive stream (`Flux`) of Server-Sent Events (SSE). Each event in the stream is a JSON object representing a single found code reference (`AnalysisReference`). The LLM should process this stream as events arrive.

*   **`AnalysisReference` JSON Object Structure**:
    Each JSON event will have the following structure:
    *   `sourceKey` (string): Indicates the origin of the referencing or referenced code.
        *   `"self"`: The code is part of the analyzed project itself.
        *   `"jdk"`: The code is part of the Java Development Kit (e.g., `java.lang.String`).
        *   `"dependency:some-library.jar"`: The code comes from a dependency JAR (e.g., `dependency:spring-webmvc.jar`). Version numbers are typically normalized from the JAR filename.
    *   `fqn` (string): The fully qualified name of the class or method signature of the reference.
    *   `codeContext` (string): A snippet of the source code of the block (method or class) that contains the reference. This provides context to the LLM.
    *   `type` (string): The direction of the reference relative to the `codeSnippet` provided in the request:
        *   `"TO"`: The `codeContext` (and its `fqn`) makes a call *to* or uses the `codeSnippet`.
        *   `"FROM"`: The `codeSnippet` (i.e., the analyzed element) makes a call *from* itself to the `fqn` described in this reference. The `codeContext` for a "FROM" reference is a specific snippet from the `codeSnippet` (e.g., the statement making the call) rather than the entire source of the `codeSnippet`.

    Example `AnalysisReference` JSON event:
    ```json
    {
      "sourceKey": "self",
      "fqn": "org.springframework.samples.petclinic.owner.OwnerController#processFindForm(org.springframework.samples.petclinic.owner.Owner, org.springframework.validation.BindingResult, java.util.Map)",
      "codeContext": "public String processFindForm(@Valid Owner owner, BindingResult result, Map<String, Object> model) { ... owner.getLastName() ... }",
      "type": "TO"
    }
    ```
    (Another event might be for a reference of type "FROM")

This comprehensive README should provide users and LLMs with the necessary information to configure and use the JavaSeeker module.
```
