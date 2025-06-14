package org.wesuper.liteai.bridge.javaseeker.tool;

import org.wesuper.liteai.bridge.javaseeker.config.ProjectEntry;
import org.wesuper.liteai.bridge.javaseeker.model.AnalysisReference;
import org.wesuper.liteai.bridge.javaseeker.service.ProjectConfigurationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.ai.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.code.CtStatement; // Added import
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.SpoonProgress;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A Spring AI {@link Tool} for analyzing Java code using Spoon.
 * <p>
 * This tool takes a project name and a code snippet (class FQN or method signature)
 * as input. It then uses Spoon to parse the specified project (which must be in a "READY" state)
 * and find all references to and from the given code snippet.
 * </p>
 * The results are streamed as {@link AnalysisReference} objects via a {@link Flux},
 * suitable for consumption by Server-Sent Event (SSE) clients.
 * </p>
 * It relies on {@link ProjectConfigurationManager} to get project details like its
 * local cache path and readiness status.
 */
@Component
@Description("Java Code Analysis Tool: Analyzes a Java project to find references to and from a given code snippet (class or method).")
public class JavaCodeAnalysisTool {

    private static final Logger logger = LoggerFactory.getLogger(JavaCodeAnalysisTool.class);
    private final ProjectConfigurationManager projectConfigurationManager;

    /**
     * Constructs a new JavaCodeAnalysisTool.
     * @param projectConfigurationManager The manager for accessing project configurations.
     */
    @Autowired
    public JavaCodeAnalysisTool(ProjectConfigurationManager projectConfigurationManager) {
        this.projectConfigurationManager = projectConfigurationManager;
    }

    /**
     * Inner record defining the structure for requests to the {@code analyzeJavaCodeReferences} tool.
     * This is used by Spring AI for schema generation and request parsing when the tool is invoked.
     */
    public record JavaCodeReferenceRequest(
        @Description("The unique name of the project to analyze, as defined in javaseeker-projects.yml.") String projectName,
        @Description("The fully qualified name of the class (e.g., com.example.MyClass) or method signature (e.g., com.example.MyClass#myMethod(String,int)) to analyze.") String codeSnippet
    ) {}


    /**
     * Analyzes a specified Java project for references to and from a given code snippet.
     * <p>
     * The code snippet can be a fully qualified class name (e.g., {@code com.example.MyClass})
     * or a method signature (e.g., {@code com.example.MyClass#myMethod(String,int)}).
     * </p>
     * <p>
     * The method streams {@link AnalysisReference} objects, each representing a found reference.
     * An error flux is returned if the project is not found, not in a "READY" state, or if
     * the target code snippet cannot be located within the project.
     * </p>
     * @param request The {@link JavaCodeReferenceRequest} containing the project name and code snippet.
     * @return A {@link Flux} of {@link AnalysisReference} objects, streaming each found reference.
     *         Returns {@code Flux.error} for various error conditions.
     */
    @Tool(name = "analyzeJavaCodeReferences", description = "Analyzes a specified Java project for references to and from a given code snippet (fully qualified class name or method signature) and streams the results.")
    public Flux<AnalysisReference> analyzeCodeReferences(JavaCodeReferenceRequest request) {
        String projectName = request.projectName();
        String codeSnippet = request.codeSnippet();
        logger.info("Received analysis request for project '{}', snippet '{}'", projectName, codeSnippet);
        Optional<ProjectEntry> projectOpt = projectConfigurationManager.getProject(projectName);

        if (projectOpt.isEmpty()) {
            logger.warn("Project not found: {}", projectName);
            return Flux.error(new IllegalArgumentException("Project not found: " + projectName + ". Ensure it is defined in javaseeker-projects.yml."));
        }

        ProjectEntry project = projectOpt.get();
        if (!"READY".equalsIgnoreCase(project.getStatus())) {
            logger.warn("Project '{}' is not READY. Current status: {}", projectName, project.getStatus());
            return Flux.error(new IllegalStateException("Project '" + projectName + "' is not in READY state. Current status: " + project.getStatus() + ". Please wait for sync/compilation or check logs."));
        }

        return Flux.create(fluxSink -> {
            try {
                Launcher launcher = new Launcher();
                launcher.addInputResource(project.getLocalCachePath());
                configureSpoonLauncher(launcher, project);

                logger.info("Building Spoon model for project '{}' at path '{}'", project.getName(), project.getLocalCachePath());
                CtModel model = launcher.buildModel();
                logger.info("Spoon model built for project '{}'. Total types found: {}", project.getName(), model.getAllTypes().size());

                Set<AnalysisReference> uniqueReferences = new HashSet<>();
                String targetFQN = codeSnippet;
                String methodNameWithParams = null;
                if (codeSnippet.contains("#")) {
                    targetFQN = codeSnippet.substring(0, codeSnippet.indexOf("#"));
                    methodNameWithParams = codeSnippet.substring(codeSnippet.indexOf("#") + 1);
                }

                CtElement targetElement = findTargetElement(model, targetFQN, methodNameWithParams);
                if (targetElement == null && (targetFQN != null && !targetFQN.contains("."))) {
                     targetElement = findTargetElementBySimpleName(model, targetFQN, methodNameWithParams);
                }

                if (targetElement == null) {
                    logger.warn("Target element not found in project '{}': {}", projectName, codeSnippet);
                    fluxSink.error(new IllegalArgumentException("Target element not found in project '" + projectName + "': " + codeSnippet + ". Ensure it's a valid FQN or simple name present in the project."));
                    return;
                }
                logger.info("Target element found in project '{}': {}", projectName, targetElement.getShortRepresentation());

                final CtElement finalTargetElement = targetElement;
                if (targetElement instanceof CtType) {
                    CtType<?> targetType = (CtType<?>) targetElement;
                    model.getElements(new TypeFilter<>(CtTypeReference.class)).forEach(typeRef -> {
                        if ((typeRef.getDeclaration() != null && typeRef.getDeclaration().equals(targetType)) ||
                            targetType.getQualifiedName().equals(typeRef.getQualifiedName())) {
                            addReference(typeRef, uniqueReferences, finalTargetElement, "TO", model);
                        }
                    });
                } else if (targetElement instanceof CtExecutable) {
                    CtExecutable<?> targetExecutable = (CtExecutable<?>) targetElement;
                    model.getElements(new TypeFilter<>(CtExecutableReference.class)).forEach(execRef -> {
                        if ((execRef.getDeclaration() != null && execRef.getDeclaration().equals(targetExecutable)) ||
                            (targetExecutable.getSignature().equals(execRef.getSignature()) &&
                               execRef.getDeclaringType() != null &&
                               targetExecutable.getDeclaringType().getQualifiedName().equals(execRef.getDeclaringType().getQualifiedName()))) {
                            addReference(execRef, uniqueReferences, finalTargetElement, "TO", model);
                        }
                    });
                }

                targetElement.getElements(new TypeFilter<>(CtTypeReference.class)).forEach(typeRef -> {
                    if (typeRef.getDeclaration() != null && typeRef.getQualifiedName()!= null && !typeRef.getQualifiedName().startsWith("java.")) {
                       addReference(typeRef, uniqueReferences, finalTargetElement, "FROM", model);
                    } else if (typeRef.getDeclaration() == null && typeRef.getQualifiedName()!=null && !typeRef.getQualifiedName().startsWith("java.")){
                        addReference(typeRef, uniqueReferences, finalTargetElement, "FROM", model);
                    }
                });
                targetElement.getElements(new TypeFilter<>(CtExecutableReference.class)).forEach(execRef -> {
                     if ((execRef.getDeclaration() != null || execRef.getExecutableDeclaration() != null) && // Check both for robustness
                        execRef.getDeclaringType() != null &&
                        execRef.getDeclaringType().getQualifiedName() != null && // Ensure declaring type FQN exists
                        !execRef.getDeclaringType().getQualifiedName().startsWith("java.")) {
                        addReference(execRef, uniqueReferences, finalTargetElement, "FROM", model);
                    }
                });

                uniqueReferences.forEach(fluxSink::next);
                fluxSink.complete();
                logger.info("Finished analysis for project '{}', snippet '{}'. Streamed {} references.", projectName, codeSnippet, uniqueReferences.size());

            } catch (Exception e) {
                logger.error("Error during Spoon analysis for project '{}', snippet '{}': {}", projectName, codeSnippet, e.getMessage(), e);
                fluxSink.error(new RuntimeException("Analysis failed for " + codeSnippet + ": " + e.getMessage(), e));
            }
        });
    }

    /**
     * Configures the Spoon Launcher with classpath information and other environment settings.
     * @param launcher The Spoon Launcher instance.
     * @param project The project entry containing cache path information.
     */
    private void configureSpoonLauncher(Launcher launcher, ProjectEntry project) {
        List<String> classpathEntries = new ArrayList<>();
        try {
            Path projectPath = Paths.get(project.getLocalCachePath());
            Path mainClassesMaven = projectPath.resolve("target/classes");
            if (Files.exists(mainClassesMaven)) classpathEntries.add(mainClassesMaven.toString());
            Path testClassesMaven = projectPath.resolve("target/test-classes");
            if (Files.exists(testClassesMaven)) classpathEntries.add(testClassesMaven.toString());
            Path mavenDepsDir = projectPath.resolve("target/dependency");
            if (Files.exists(mavenDepsDir) && Files.isDirectory(mavenDepsDir)) {
                Files.list(mavenDepsDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(jar -> classpathEntries.add(jar.toString()));
            }
            Path mainClassesGradle = projectPath.resolve("build/classes/java/main");
            if (Files.exists(mainClassesGradle)) classpathEntries.add(mainClassesGradle.toString());
            Path testClassesGradle = projectPath.resolve("build/classes/java/test");
            if (Files.exists(testClassesGradle)) classpathEntries.add(testClassesGradle.toString());
        } catch (IOException e) {
            logger.warn("Error trying to automatically list dependency JARs for project '{}': {}", project.getName(), e.getMessage());
        }

        if (!classpathEntries.isEmpty()) {
            launcher.getEnvironment().setSourceClasspath(classpathEntries.toArray(new String[0]));
            logger.info("Spoon classpath for '{}': {} entries. First 5: {}", project.getName(), classpathEntries.size(), classpathEntries.stream().limit(5).collect(Collectors.joining(", ")));
        } else {
            launcher.getEnvironment().setNoClasspath(true);
            logger.info("No specific classpath entries found for project '{}'. Spoon running with noClasspath=true.", project.getName());
        }

        launcher.getEnvironment().setShouldCompile(false);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.getEnvironment().setSpoonProgress(new SpoonProgress() {
            @Override public void step(Process process, String task, int taskId, int nbTask) { /* verbose */ }
            @Override public void start(Process process) { logger.debug("Spoon process started for project {}: {}",project.getName(), process); }
            @Override public void end(Process process) { logger.debug("Spoon process ended for project {}: {}", project.getName(), process); }
        });
    }

    /**
     * Finds a target element (class or method) in the Spoon model by its simple name.
     * @param model The Spoon AST model.
     * @param simpleName The simple name of the class.
     * @param methodNameWithParams The method signature (e.g., "myMethod(String,int)"), or null for class lookup.
     * @return The found {@link CtElement} or null.
     */
    private CtElement findTargetElementBySimpleName(CtModel model, String simpleName, String methodNameWithParams) {
        for (CtType<?> type : model.getAllTypes()) {
            if (type.getSimpleName().equals(simpleName)) {
                if (methodNameWithParams == null || methodNameWithParams.isEmpty()) return type;
                return findMethodInType(type, methodNameWithParams);
            }
        }
        logger.warn("Type with simple name '{}' not found in model.", simpleName);
        return null;
    }

    /**
     * Finds a target element (class or method) in the Spoon model by its fully qualified name.
     * @param model The Spoon AST model.
     * @param typeFQN The fully qualified name of the class.
     * @param methodNameWithParams The method signature (e.g., "myMethod(String,int)"), or null for class lookup.
     * @return The found {@link CtElement} or null.
     */
    private CtElement findTargetElement(CtModel model, String typeFQN, String methodNameWithParams) {
        CtType<?> type = model.getRootPackage().getTopLevelType(typeFQN);
         if (type == null) {
            type = model.getAllTypes().stream()
                        .filter(t -> t.getQualifiedName().equals(typeFQN))
                        .findFirst().orElse(null);
        }
        if (type == null) {
            logger.warn("Type with FQN '{}' not found in model.", typeFQN);
            return null;
        }
        if (methodNameWithParams == null || methodNameWithParams.isEmpty()) return type;
        return findMethodInType(type, methodNameWithParams);
    }

    /**
     * Finds a specific method within a given Spoon {@link CtType} based on its name and parameter types.
     * Parameter type matching is based on simple names.
     * @param type The Spoon {@link CtType} to search within.
     * @param methodNameWithParams The method signature string, e.g., "myMethod(String,int)".
     * @return The found {@link CtExecutable} or null.
     */
    private CtExecutable<?> findMethodInType(CtType<?> type, String methodNameWithParams) {
        if (!methodNameWithParams.contains("(") || !methodNameWithParams.endsWith(")")) {
            logger.warn("Invalid method signature format for {}: {}. Expected name(paramType1,paramType2).", type.getQualifiedName(), methodNameWithParams);
            return null;
        }
        String inputMethodName = methodNameWithParams.substring(0, methodNameWithParams.indexOf('('));
        String paramsFromInputStr = methodNameWithParams.substring(methodNameWithParams.indexOf('(') + 1, methodNameWithParams.length() - 1);
        String[] inputParamTypesArray = paramsFromInputStr.isEmpty() ? new String[0] : paramsFromInputStr.split(",");

        for (CtExecutable<?> executable : type.getExecutables()) {
            if (executable.getSimpleName().equals(inputMethodName)) {
                List<CtTypeReference<?>> spoonParamTypesRefs = executable.getParameters().stream()
                                                                .map(p -> p.getType())
                                                                .collect(Collectors.toList());
                if (inputParamTypesArray.length == spoonParamTypesRefs.size()) {
                    boolean match = true;
                    for (int i = 0; i < inputParamTypesArray.length; i++) {
                        String inputParamSimpleName = inputParamTypesArray[i].trim();
                        if (inputParamSimpleName.contains(".")) {
                            inputParamSimpleName = inputParamSimpleName.substring(inputParamSimpleName.lastIndexOf('.') + 1);
                        }
                        CtTypeReference<?> spoonParamTypeRef = spoonParamTypesRefs.get(i);
                        if (!spoonParamTypeRef.getSimpleName().equals(inputParamSimpleName)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) return executable;
                }
            }
        }
        logger.warn("Method not found by signature: {} in type {}", methodNameWithParams, type.getQualifiedName());
        return null;
    }

    /**
     * Adds a discovered code reference to the set of unique references.
     * Determines the source, FQN, and code context for the reference based on its type ("TO" or "FROM").
     * @param referenceUsageLocation The Spoon element representing the usage of a type or executable ({@link CtTypeReference} or {@link CtExecutableReference}).
     * @param uniqueReferences The set to add the new {@link AnalysisReference} to.
     * @param analyzedTargetElement The primary code element (class or method) that is the target of the analysis.
     * @param referenceDirection A string "TO" or "FROM", indicating if the reference points to the {@code analyzedTargetElement} or originates from it.
     * @param model The full Spoon AST model, used for resolving declarations if needed.
     */
    private void addReference(CtElement referenceUsageLocation,
                              Set<AnalysisReference> uniqueReferences,
                              CtElement analyzedTargetElement,
                              String referenceDirection,
                              CtModel model) {

        CtElement actualReferencedDeclaration;
        String fqnOfReferencedElement;
        String sourceKeyOfReferencedElement = "self";

        if (referenceUsageLocation instanceof CtTypeReference) {
            CtTypeReference<?> typeRef = (CtTypeReference<?>) referenceUsageLocation;
            actualReferencedDeclaration = typeRef.getDeclaration();
            fqnOfReferencedElement = typeRef.getQualifiedName();
            if (actualReferencedDeclaration == null && fqnOfReferencedElement != null) {
                 actualReferencedDeclaration = model.getAllTypes().stream().filter(t -> t.getQualifiedName().equals(fqnOfReferencedElement)).findFirst().orElse(null);
            }
        } else if (referenceUsageLocation instanceof CtExecutableReference) {
            CtExecutableReference<?> execRef = (CtExecutableReference<?>) referenceUsageLocation;
            actualReferencedDeclaration = execRef.getExecutableDeclaration();
            if (actualReferencedDeclaration == null) actualReferencedDeclaration = execRef.getDeclaration(); // Fallback

            CtTypeReference<?> declaringTypeRef = execRef.getDeclaringType();
            String typeFQNForExec = (declaringTypeRef != null && declaringTypeRef.getQualifiedName() != null) ? declaringTypeRef.getQualifiedName() : "unknown_declaring_type";
            fqnOfReferencedElement = typeFQNForExec + "#" + execRef.getSignature();
        } else {
            return;
        }

        if (actualReferencedDeclaration != null && actualReferencedDeclaration.getPosition().isValidPosition() && actualReferencedDeclaration.getPosition().getFile() != null ) {
            String filePath = actualReferencedDeclaration.getPosition().getFile().getAbsolutePath();
            if (filePath.contains(".jar" + File.separator) || filePath.endsWith(".jar")) {
                String jarName = Paths.get(filePath).getFileName().toString();
                sourceKeyOfReferencedElement = "dependency:" + jarName.replaceAll("-[0-9].*\\.jar$", ".jar");
            }
        } else if (fqnOfReferencedElement != null && (fqnOfReferencedElement.startsWith("java.") || fqnOfReferencedElement.startsWith("javax."))) {
             sourceKeyOfReferencedElement = "jdk";
        }

        CtElement contextOfBlockContainingReference = referenceUsageLocation.getParent(p -> p instanceof CtExecutable || p instanceof CtType);
        if (contextOfBlockContainingReference == null) return;

        String fqnOfContextBlock, codeOfContextBlock, sourceKeyOfContextBlock = "self";
        if (contextOfBlockContainingReference instanceof CtExecutable) {
            CtExecutable<?> exec = (CtExecutable<?>) contextOfBlockContainingReference;
            fqnOfContextBlock = exec.getDeclaringType().getQualifiedName() + "#" + exec.getSignature();
            codeOfContextBlock = exec.toString();
        } else if (contextOfBlockContainingReference instanceof CtType) {
            CtType<?> type = (CtType<?>) contextOfBlockContainingReference;
            fqnOfContextBlock = type.getQualifiedName();
            codeOfContextBlock = type.toString();
        } else {
            return;
        }

        if (contextOfBlockContainingReference.getPosition().isValidPosition() && contextOfBlockContainingReference.getPosition().getFile() != null) {
            String usageFilePath = contextOfBlockContainingReference.getPosition().getFile().getAbsolutePath();
             if (usageFilePath.contains(".jar" + File.separator) || usageFilePath.endsWith(".jar")) {
                sourceKeyOfContextBlock = "dependency:" + Paths.get(usageFilePath).getFileName().toString().replaceAll("-[0-9].*\\.jar$", ".jar");
             } else if (fqnOfContextBlock.startsWith("java.") || fqnOfContextBlock.startsWith("javax.")){
                 sourceKeyOfContextBlock = "jdk";
             }
        }

        AnalysisReference newRef = null;
        if ("TO".equals(referenceDirection)) {
            boolean targetMatch = false;
            if (actualReferencedDeclaration != null && actualReferencedDeclaration.equals(analyzedTargetElement)) {
                targetMatch = true;
            } else if (actualReferencedDeclaration == null && fqnOfReferencedElement != null) {
                 if (analyzedTargetElement instanceof CtType && fqnOfReferencedElement.equals(((CtType<?>)analyzedTargetElement).getQualifiedName())) targetMatch = true;
                 if (analyzedTargetElement instanceof CtExecutable) {
                     CtExecutable<?> targetExec = (CtExecutable<?>) analyzedTargetElement;
                     String targetSig = targetExec.getDeclaringType().getQualifiedName() + "#" + targetExec.getSignature();
                     if(fqnOfReferencedElement.equals(targetSig)) targetMatch = true;
                 }
            }
            if (!targetMatch) return;
            if (contextOfBlockContainingReference.equals(analyzedTargetElement)) return;
            newRef = new AnalysisReference(sourceKeyOfContextBlock, fqnOfContextBlock, codeOfContextBlock, "TO");
        } else if ("FROM".equals(referenceDirection)) {
            boolean isContained = false;
            for (CtElement p = referenceUsageLocation; p != null; p = p.getParent()) { if (p.equals(analyzedTargetElement)) { isContained = true; break; } }
            if(!isContained) return;

            // For "FROM" references, actualReferencedDeclaration is the element being called/used by the target.
            // If this is the target itself (e.g. recursive call), we still want to capture it.
            // if (actualReferencedDeclaration != null && actualReferencedDeclaration.equals(analyzedTargetElement)) return;

            String fromContextString = "";
            CtElement contextProvider = referenceUsageLocation.getParent(CtStatement.class);
            if (contextProvider != null) {
                fromContextString = contextProvider.toString();
            } else {
                // Fallback if not directly within a statement (e.g., field initializer referring to a static method)
                // Try to get the closest executable or type as context, but not the entire analyzed target if it's very large.
                contextProvider = referenceUsageLocation.getParent(p -> p instanceof CtExecutable || p instanceof CtType);
                if (contextProvider != null && contextProvider.equals(analyzedTargetElement)) {
                    // If the direct parent block is the analyzed element itself,
                    // we still want a more specific context if possible.
                    // For now, as a simpler fallback, use the reference itself and a bit of its parent structure.
                    // This part might need more refinement if specific examples show it's insufficient.
                    CtElement parentOfRef = referenceUsageLocation.getParent();
                    if (parentOfRef != null) {
                         fromContextString = parentOfRef.toString();
                         if (fromContextString.length() > 500) { // Limit context length for very large parents
                            fromContextString = referenceUsageLocation.toString() + " (in context of " + ( (parentOfRef instanceof CtType) ? ((CtType)parentOfRef).getQualifiedName() : ( (parentOfRef instanceof CtExecutable) ? ((CtExecutable)parentOfRef).getSignature() : parentOfRef.getShortRepresentation() ) ) + ")";
                         }
                    } else {
                        fromContextString = referenceUsageLocation.toString();
                    }

                } else if (contextProvider != null) {
                    fromContextString = contextProvider.toString();
                } else {
                    fromContextString = analyzedTargetElement.toString(); // Ultimate fallback
                }
            }
            // Ensure context string is not excessively large, as a general safeguard
            if (fromContextString.length() > 2000) {
                 fromContextString = fromContextString.substring(0, 1997) + "...";
            }

            newRef = new AnalysisReference(sourceKeyOfReferencedElement, fqnOfReferencedElement, fromContextString, "FROM");
        }

        if (newRef != null && uniqueReferences.add(newRef)) {
            logger.trace("Ref Added: {}", newRef);
        }
    }
}

/**
 * Configuration class for explicitly registering the JavaCodeAnalysisTool as a Spring AI Function.
 * While Spring AI can often auto-discover @Tool annotated methods in @Component classes,
 * this explicit registration using FunctionCallbackWrapper provides more control and clarity.
 * It defines the exact request structure (JavaCodeReferenceRequest) for the function.
 */
@Configuration
class JavaCodeAnalysisToolFunctionConfiguration {

    /**
     * Creates a FunctionCallbackWrapper bean for the JavaCodeAnalysisTool.
     * This bean makes the 'analyzeJavaCodeReferences' method available to Spring AI
     * as a callable function, with a defined name, description, and request schema.
     *
     * @param javaCodeAnalysisTool The instance of JavaCodeAnalysisTool to be wrapped.
     * @return A FunctionCallbackWrapper configured for the code analysis tool.
     */
    @Bean
    @Description("Function bean for Java Code Analysis. Analyzes a Java project to find references to and from a given code snippet. " +
                 "Input should be a JSON object with 'projectName' and 'codeSnippet' fields.")
    public FunctionCallbackWrapper<JavaCodeAnalysisTool.JavaCodeReferenceRequest, Flux<AnalysisReference>> analyzeJavaCodeReferencesFunction(JavaCodeAnalysisTool javaCodeAnalysisTool) {
        return FunctionCallbackWrapper.builder(javaCodeAnalysisTool)
            .withName("analyzeJavaCodeReferences")
            .withDescription("Analyzes a specified Java project for references to and from a given code snippet (class or method).")
            .withRequestType(JavaCodeAnalysisTool.JavaCodeReferenceRequest.class)
            .build();
    }
}
