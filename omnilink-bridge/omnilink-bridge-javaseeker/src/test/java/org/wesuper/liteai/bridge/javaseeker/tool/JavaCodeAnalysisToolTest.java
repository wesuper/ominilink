package org.wesuper.liteai.bridge.javaseeker.tool;

import org.wesuper.liteai.bridge.javaseeker.config.ProjectEntry;
import org.wesuper.liteai.bridge.javaseeker.model.AnalysisReference;
import org.wesuper.liteai.bridge.javaseeker.service.ProjectConfigurationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JavaCodeAnalysisToolTest {

    @Mock
    private ProjectConfigurationManager projectConfigurationManager;

    @InjectMocks
    private JavaCodeAnalysisTool javaCodeAnalysisTool;

    @TempDir
    Path tempDir;
    private Path projectRoot;
    private ProjectEntry projectEntry;

    @BeforeEach
    void setUp() throws IOException {
        projectRoot = Files.createDirectory(tempDir.resolve("testProject"));
        projectEntry = new ProjectEntry();
        projectEntry.setName("testProject");
        projectEntry.setLocalCachePath(projectRoot.toString());
        projectEntry.setStatus("READY");

        when(projectConfigurationManager.getProject("testProject")).thenReturn(Optional.of(projectEntry));
    }

    private void createJavaFile(String packageName, String className, String content) throws IOException {
        Path packageDir = projectRoot;
        if (packageName != null && !packageName.isEmpty()) {
            String packagePath = packageName.replace('.', '/');
            packageDir = projectRoot.resolve(packagePath);
            Files.createDirectories(packageDir);
        }
        Files.writeString(packageDir.resolve(className + ".java"), content);
    }

    @Test
    void analyzeCodeReferences_ClassToClassReference_FindsToAndFrom() throws IOException {
        createJavaFile("com.example.tool", "TargetClass",
                "package com.example.tool; public class TargetClass { public void doSomething() {} }");
        createJavaFile("com.example.tool", "SourceClass",
                "package com.example.tool; public class SourceClass { public void callTarget() { TargetClass t = new TargetClass(); t.doSomething(); } }");

        Files.createFile(projectRoot.resolve("pom.xml")); // For Spoon classpath heuristic


        JavaCodeAnalysisTool.JavaCodeReferenceRequest requestTo = new JavaCodeAnalysisTool.JavaCodeReferenceRequest("testProject", "com.example.tool.TargetClass");
        Flux<AnalysisReference> resultsToTarget = javaCodeAnalysisTool.analyzeCodeReferences(requestTo);

        StepVerifier.create(resultsToTarget)
            .expectNextMatches(ref ->
                ref.getFullyQualifiedName().equals("com.example.tool.SourceClass#callTarget()") &&
                ref.getReferenceType().equals("TO") &&
                ref.getCodeContext().contains("TargetClass t = new TargetClass();")
            )
            .verifyComplete();

        JavaCodeAnalysisTool.JavaCodeReferenceRequest requestFrom = new JavaCodeAnalysisTool.JavaCodeReferenceRequest("testProject", "com.example.tool.SourceClass#callTarget()");
        Flux<AnalysisReference> resultsFromSourceMethod = javaCodeAnalysisTool.analyzeCodeReferences(requestFrom);

        StepVerifier.create(resultsFromSourceMethod)
            .expectNextMatches(ref ->
                ref.getFullyQualifiedName().equals("com.example.tool.TargetClass") &&
                ref.getReferenceType().equals("FROM") &&
                ref.getCodeContext().contains("TargetClass t = new TargetClass();")
            )
             .expectNextMatches(ref ->
                ref.getFullyQualifiedName().equals("com.example.tool.TargetClass#doSomething()") &&
                ref.getReferenceType().equals("FROM") &&
                ref.getCodeContext().contains("t.doSomething();")
            )
            .verifyComplete();
    }

    @Test
    void analyzeCodeReferences_ProjectNotReady() {
        projectEntry.setStatus("COMPILING");
        JavaCodeAnalysisTool.JavaCodeReferenceRequest request = new JavaCodeAnalysisTool.JavaCodeReferenceRequest("testProject", "com.example.NonExistent");
        Flux<AnalysisReference> results = javaCodeAnalysisTool.analyzeCodeReferences(request);
        StepVerifier.create(results)
            .expectErrorMessage("Project 'testProject' is not in READY state. Current status: COMPILING. Please wait for sync/compilation or check logs.")
            .verify();
    }

    @Test
    void analyzeCodeReferences_ProjectNotFound() {
        when(projectConfigurationManager.getProject("unknownProject")).thenReturn(Optional.empty());
        JavaCodeAnalysisTool.JavaCodeReferenceRequest request = new JavaCodeAnalysisTool.JavaCodeReferenceRequest("unknownProject", "com.example.NonExistent");
        Flux<AnalysisReference> results = javaCodeAnalysisTool.analyzeCodeReferences(request);
        StepVerifier.create(results)
            .expectErrorMessage("Project not found: unknownProject. Ensure it is defined in javaseeker-projects.yml.")
            .verify();
    }

    @Test
    void analyzeCodeReferences_TargetNotFoundInProject() throws IOException {
         createJavaFile("com.example.tool", "SomeClass", "package com.example.tool; public class SomeClass {}");
         Files.createFile(projectRoot.resolve("pom.xml"));

        JavaCodeAnalysisTool.JavaCodeReferenceRequest request = new JavaCodeAnalysisTool.JavaCodeReferenceRequest("testProject", "com.example.tool.NonExistentClass");
        Flux<AnalysisReference> results = javaCodeAnalysisTool.analyzeCodeReferences(request);
        StepVerifier.create(results)
            .expectErrorMessage("Target element not found in project 'testProject': com.example.tool.NonExistentClass. Ensure it's a valid FQN or simple name present in the project.")
            .verify();
    }

}
