package com.example.mcp.analysis;

import com.example.mcp.project.LocalProjectSource;
import com.example.mcp.project.ProjectSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpoonAnalysisServiceTest {

    private SpoonAnalysisService spoonAnalysisService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        spoonAnalysisService = new SpoonAnalysisService();
    }

    @Test
    void analyze_SimpleClassReference() throws IOException {
        // Create a dummy project with one class referencing another
        Path projectRoot = Files.createDirectory(tempDir.resolve("simpleProject"));
        Path comExampleDir = Files.createDirectories(projectRoot.resolve("com").resolve("example"));

        String classAContent = "package com.example; public class ClassA { public void methodA() { ClassB b = new ClassB(); b.methodB(); } }";
        String classBContent = "package com.example; public class ClassB { public void methodB() { } }";
        // String classCContent = "package com.example; public class ClassC { public void methodC() { ClassA a = new ClassA(); a.methodA(); } }";


        Files.writeString(comExampleDir.resolve("ClassA.java"), classAContent);
        Files.writeString(comExampleDir.resolve("ClassB.java"), classBContent);
        // Files.writeString(comExampleDir.resolve("ClassC.java"), classCContent);


        ProjectSource projectSource = new LocalProjectSource("simpleProject", projectRoot.toString());
        assertTrue(projectSource.resolve()); // Ensure path is valid

        // Analyze ClassB to find references to it
        AnalysisResult resultToB = spoonAnalysisService.analyze(projectSource, "com.example.ClassB");
        assertNotNull(resultToB);
        assertEquals("com.example.ClassB", resultToB.getAnalysisTarget());

        // Expected: ClassA#methodA() references ClassB
        boolean foundRefToB = resultToB.getReferences().stream().anyMatch(ref ->
            "com.example.ClassA#methodA()".equals(ref.getFullyQualifiedName()) &&
            ref.getCodeContext().contains("ClassB b = new ClassB();") &&
            "self".equals(ref.getSource())
        );
        // System.out.println("References to ClassB:");
        // resultToB.getReferences().forEach(r -> System.out.println("  FQN: " + r.getFullyQualifiedName() + ", Source: " + r.getSource() + ", Context: " + r.getCodeContext().substring(0, Math.min(r.getCodeContext().length(), 50)) + "..."));
        assertTrue(foundRefToB, "Should find ClassA#methodA() referencing ClassB. References found: " + resultToB.getReferences().size());


        // Analyze ClassA#methodA() to find references from it
        AnalysisResult resultFromA = spoonAnalysisService.analyze(projectSource, "com.example.ClassA#methodA()");
        assertNotNull(resultFromA);
        assertEquals("com.example.ClassA#methodA()", resultFromA.getAnalysisTarget());

        // Expected: ClassA#methodA() references ClassB and ClassB#methodB()
        boolean foundRefFromAToBClass = resultFromA.getReferences().stream().anyMatch(ref ->
            "com.example.ClassB".equals(ref.getFullyQualifiedName()) && // Type reference
            // The context for "from" references is the target element itself.
            ref.getCodeContext().contains("ClassB b = new ClassB();") &&
            "self".equals(ref.getSource())
        );
        boolean foundRefFromAToBMethod = resultFromA.getReferences().stream().anyMatch(ref ->
            "com.example.ClassB#methodB()".equals(ref.getFullyQualifiedName()) && // Executable reference
            // The context for "from" references is the target element itself.
            ref.getCodeContext().contains("b.methodB();") &&
            "self".equals(ref.getSource())
        );
        // System.out.println("References from ClassA#methodA():");
        // resultFromA.getReferences().forEach(r -> System.out.println("  FQN: " + r.getFullyQualifiedName() + ", Source: " + r.getSource() + ", Context: " + r.getCodeContext().substring(0, Math.min(r.getCodeContext().length(), 50)) + "..."));

        assertTrue(foundRefFromAToBClass, "Should find ClassA#methodA() referencing ClassB type. References found: " + resultFromA.getReferences().size());
        assertTrue(foundRefFromAToBMethod, "Should find ClassA#methodA() referencing ClassB#methodB(). References found: " + resultFromA.getReferences().size());
    }

    @Test
    void analyze_TargetNotFound() throws IOException {
        Path projectRoot = Files.createDirectory(tempDir.resolve("notFoundProject"));
        ProjectSource projectSource = new LocalProjectSource("notFoundProject", projectRoot.toString());
        assertTrue(projectSource.resolve());

        AnalysisResult result = spoonAnalysisService.analyze(projectSource, "com.example.NonExistentClass");
        assertNotNull(result);
        assertTrue(result.getReferences().isEmpty());
    }
}
