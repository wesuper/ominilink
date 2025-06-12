package org.wesuper.liteai.bridge.javaseeker.analysis;

import org.wesuper.liteai.bridge.javaseeker.project.ProjectSource;
import org.springframework.stereotype.Service;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.SpoonProgress;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.io.IOException;


@Service
public class SpoonAnalysisService implements CodeAnalysisService {

    @Override
    public AnalysisResult analyze(ProjectSource projectSource, String codeSnippet) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(projectSource.getProjectPath().toString());

        // Attempt to configure classpath
        List<String> classpathEntries = new ArrayList<>();
        try {
            // 1. Add project's own compiled classes (if available, e.g. target/classes or build/classes)
            Path mainClasses = projectSource.getProjectPath().resolve("target/classes"); // Maven
            if (Files.exists(mainClasses)) classpathEntries.add(mainClasses.toString());
            mainClasses = projectSource.getProjectPath().resolve("build/classes/java/main"); // Gradle
            if (Files.exists(mainClasses)) classpathEntries.add(mainClasses.toString());

            Path testClasses = projectSource.getProjectPath().resolve("target/test-classes"); // Maven
             if (Files.exists(testClasses)) classpathEntries.add(testClasses.toString());
            testClasses = projectSource.getProjectPath().resolve("build/classes/java/test"); // Gradle
            if (Files.exists(testClasses)) classpathEntries.add(testClasses.toString());


            // 2. Basic local Maven repository discovery (.m2)
            // This is a simplified approach. A robust solution would parse pom.xml or use Gradle tooling API.
            Path localMavenRepo = Paths.get(System.getProperty("user.home"), ".m2", "repository");
            if (Files.exists(localMavenRepo)) {
                // This is a very broad inclusion and might be slow or include too much.
                // A better way is to parse the build file (pom.xml/build.gradle) to get specific dependencies.
                // For now, we're just telling Spoon where to potentially find things.
                // classpathEntries.add(localMavenRepo.toString()); // This might be too much
            }

            // For projects built with Maven, try to find dependencies via a 'dependency' directory if it exists
            // (e.g., if 'mvn dependency:copy-dependencies' was run)
            Path mavenDepsDir = projectSource.getProjectPath().resolve("target/dependency");
            if(Files.exists(mavenDepsDir) && Files.isDirectory(mavenDepsDir)){
                Files.list(mavenDepsDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(jar -> classpathEntries.add(jar.toString()));
            }

            // For projects built with Gradle, try to find dependencies via a 'libs' directory if it exists
            // (e.g., if a custom task copied dependencies there)
            // Gradle's dependency management is more complex to inspect without its Tooling API.
            // A common pattern is to have a flatDir repository or check specific configurations.
            // This is highly project-specific without deeper build file parsing.

        } catch (IOException e) {
            System.err.println("Error while trying to list dependency JARs: " + e.getMessage());
        }

        if (!classpathEntries.isEmpty()) {
            String[] cp = classpathEntries.toArray(new String[0]);
            // System.out.println("Spoon Classpath being set to: " + Arrays.toString(cp));
            launcher.getEnvironment().setSourceClasspath(cp);
        } else {
             System.out.println("No specific classpath entries added for Spoon. Using noClasspath=true.");
             launcher.getEnvironment().setNoClasspath(true); // Fallback if no classpath could be derived
        }


        launcher.getEnvironment().setShouldCompile(false);
        launcher.getEnvironment().setAutoImports(true);
        // Determine Java version (default to 11, but should be configurable or detected)
        // For now, let's assume JDK 8+ as per requirements. Spoon defaults usually work well.
        // launcher.getEnvironment().setComplianceLevel(ComplianceLevel.JAVA_11); // Example
        launcher.getEnvironment().setCommentEnabled(true); // Preserve comments for context


        launcher.getEnvironment().setSpoonProgress(new SpoonProgress() {
            @Override
            public void step(Process process, String task, int taskId, int nbTask) {
                // System.out.println("Spoon progress: " + process + " - " + task + " (" + taskId + "/" + nbTask + ")");
            }
             @Override
            public void start(Process process) {
                System.out.println("Spoon process started: " + process);
            }

            @Override
            public void end(Process process) {
                System.out.println("Spoon process ended: " + process);
            }
        });

        System.out.println("Building Spoon model for: " + projectSource.getProjectPath().toString());
        CtModel model = launcher.buildModel();
        System.out.println("Spoon model built successfully. Total elements: " + model.getAllElements().size());

        List<AnalysisResult.Reference> references = new ArrayList<>();
        Set<AnalysisResult.Reference> uniqueReferences = new HashSet<>();


        String targetFQN = codeSnippet;
        String methodNameWithParams = null;
        if (codeSnippet.contains("#")) {
            targetFQN = codeSnippet.substring(0, codeSnippet.indexOf("#"));
            methodNameWithParams = codeSnippet.substring(codeSnippet.indexOf("#") + 1);
        }

        final String finalTargetFQN = targetFQN;
        final String finalMethodNameWithParams = methodNameWithParams;

        CtElement targetElement = findTargetElement(model, finalTargetFQN, finalMethodNameWithParams);

        if (targetElement == null) {
            System.err.println("Target element not found: " + codeSnippet + " in project " + projectSource.getProjectName());
            // Try to find by simple name if FQN fails or if it's not a fully qualified name
            if (!finalTargetFQN.contains(".")) {
                 System.out.println("Attempting to find by simple name: " + finalTargetFQN);
                 targetElement = findTargetElementBySimpleName(model, finalTargetFQN, finalMethodNameWithParams);
            }
            if (targetElement == null) {
                System.err.println("Target element still not found after trying simple name: " + codeSnippet);
                return new AnalysisResult(codeSnippet, references);
            }
        }
        System.out.println("Target element found: " + targetElement.getShortRepresentation() + " (" + targetElement.getClass().getSimpleName() + ")");


        // Find references TO the target element
        if (targetElement instanceof CtType) {
            CtType<?> targetType = (CtType<?>) targetElement;
            model.getElements(new TypeFilter<>(CtTypeReference.class)).forEach(typeRef -> {
                if (typeRef.getDeclaration() != null && typeRef.getDeclaration().equals(targetType)) {
                    addReference(typeRef, uniqueReferences, projectSource, false, targetElement, model);
                } else if (targetType.getQualifiedName().equals(typeRef.getQualifiedName())) {
                     // Fallback for unresolved references
                     addReference(typeRef, uniqueReferences, projectSource, false, targetElement, model);
                }
            });
        } else if (targetElement instanceof CtExecutable) {
            CtExecutable<?> targetExecutable = (CtExecutable<?>) targetElement;
            model.getElements(new TypeFilter<>(CtExecutableReference.class)).forEach(execRef -> {
                if (execRef.getDeclaration() != null && execRef.getDeclaration().equals(targetExecutable)) {
                     addReference(execRef, uniqueReferences, projectSource, false, targetElement, model);
                } else if (targetExecutable.getSignature().equals(execRef.getSignature()) &&
                           execRef.getDeclaringType() != null &&
                           targetExecutable.getDeclaringType().getQualifiedName().equals(execRef.getDeclaringType().getQualifiedName())) {
                    // Fallback for unresolved references
                    addReference(execRef, uniqueReferences, projectSource, false, targetElement, model);
                }
            });
        }

        // Find references FROM the target element
        if (targetElement != null) {
            targetElement.getElements(new TypeFilter<>(CtTypeReference.class)).forEach(typeRef -> {
                if (typeRef.getDeclaration() != null && !typeRef.getQualifiedName().startsWith("java.")) {
                   addReference(typeRef, uniqueReferences, projectSource, true, targetElement, model);
                }
            });

            targetElement.getElements(new TypeFilter<>(CtExecutableReference.class)).forEach(execRef -> {
                 if (execRef.getDeclaration() != null &&
                    execRef.getDeclaringType() != null &&
                    !execRef.getDeclaringType().getQualifiedName().startsWith("java.")) {
                    addReference(execRef, uniqueReferences, projectSource, true, targetElement, model);
                }
            });
        }


        references.addAll(uniqueReferences);
        System.out.println("Found " + references.size() + " unique references for " + codeSnippet);
        return new AnalysisResult(codeSnippet, references);
    }

    private CtElement findTargetElementBySimpleName(CtModel model, String simpleName, String methodNameWithParams) {
        for (CtType<?> type : model.getAllTypes()) {
            if (type.getSimpleName().equals(simpleName)) {
                if (methodNameWithParams == null || methodNameWithParams.isEmpty()) {
                    return type;
                }
                return findMethodInType(type, methodNameWithParams);
            }
        }
        return null;
    }


    private CtElement findTargetElement(CtModel model, String typeFQN, String methodNameWithParams) {
        CtType<?> type = model.getRootPackage().getTopLevelType(typeFQN); // Use getTopLevelType for FQN
         if (type == null) {
            // Fallback for nested classes or other scenarios
            type = model.getAllTypes().stream()
                        .filter(t -> t.getQualifiedName().equals(typeFQN))
                        .findFirst().orElse(null);
        }

        if (type == null) return null;
        if (methodNameWithParams == null || methodNameWithParams.isEmpty()) return type;

        return findMethodInType(type, methodNameWithParams);
    }

    private CtExecutable<?> findMethodInType(CtType<?> type, String methodNameWithParams) {
        String inputMethodName = methodNameWithParams.substring(0, methodNameWithParams.indexOf('('));
        String paramsFromInputStr = methodNameWithParams.substring(methodNameWithParams.indexOf('(') + 1, methodNameWithParams.length() - 1);
        String[] inputParamTypes = paramsFromInputStr.isEmpty() ? new String[0] : paramsFromInputStr.split(",");

        for (CtExecutable<?> executable : type.getExecutables()) {
            if (executable.getSimpleName().equals(inputMethodName)) {
                List<CtTypeReference<?>> spoonParamTypesRefs = executable.getParameters().stream()
                                                                .map(p -> p.getType())
                                                                .collect(Collectors.toList());
                if (inputParamTypes.length == spoonParamTypesRefs.size()) {
                    boolean match = true;
                    for (int i = 0; i < inputParamTypes.length; i++) {
                        String inputParamType = inputParamTypes[i].trim();
                        CtTypeReference<?> spoonParamTypeRef = spoonParamTypesRefs.get(i);
                        // This comparison needs to be more robust. It should handle:
                        // 1. Simple names vs. FQNs (e.g., "String" vs "java.lang.String")
                        // 2. Primitive types (e.g., "int" vs "int")
                        // 3. Array types (e.g., "String[]" vs "java.lang.String[]")
                        if (!spoonParamTypeRef.getQualifiedName().endsWith(inputParamType) &&
                            !spoonParamTypeRef.getSimpleName().equals(inputParamType)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) return executable;
                }
            }
        }
        System.err.println("Method not found: " + methodNameWithParams + " in type " + type.getQualifiedName());
        return null;
    }


    private void addReference(CtElement referenceElement, // This is the CtTypeReference or CtExecutableReference itself
                              Set<AnalysisResult.Reference> uniqueReferences,
                              ProjectSource projectSource,
                              boolean isReferenceFromTarget, // true if we are looking for calls *made by* the targetElement
                              CtElement analyzedTargetElement, // The class or method being analyzed
                              CtModel model) {

        CtElement actualReferencedDeclaration = null;
        String referencedFqn = "";
        String referencedSourceKey = "self"; // Default to "self"

        if (referenceElement instanceof CtTypeReference) {
            actualReferencedDeclaration = ((CtTypeReference<?>) referenceElement).getDeclaration();
            if (actualReferencedDeclaration != null) {
                 referencedFqn = ((CtType<?>)actualReferencedDeclaration).getQualifiedName();
            } else {
                referencedFqn = ((CtTypeReference<?>)referenceElement).getQualifiedName(); // Fallback to unresolved FQN
            }
        } else if (referenceElement instanceof CtExecutableReference) {
            actualReferencedDeclaration = ((CtExecutableReference<?>) referenceElement).getExecutableDeclaration();
             if (actualReferencedDeclaration != null) {
                CtExecutable<?> exec = (CtExecutable<?>) actualReferencedDeclaration;
                referencedFqn = exec.getDeclaringType().getQualifiedName() + "#" + exec.getSignature();
            } else {
                // Fallback for unresolved executable references
                CtTypeReference<?> declaringTypeRef = ((CtExecutableReference<?>)referenceElement).getDeclaringType();
                String typeFqn = declaringTypeRef != null ? declaringTypeRef.getQualifiedName() : "unknown_declaring_type";
                referencedFqn = typeFqn + "#" + ((CtExecutableReference<?>)referenceElement).getSignature();
            }
        } else {
            return; // Should not happen
        }

        // Determine source of the referenced declaration (self or dependency)
        if (actualReferencedDeclaration != null && actualReferencedDeclaration.getPosition().isValidPosition()) {
            String filePath = actualReferencedDeclaration.getPosition().getFile().getAbsolutePath();
            if (filePath.contains(".jar" + File.separator) || filePath.endsWith(".jar")) { // Check if path indicates it's from a JAR
                 // Try to get a meaningful name for the JAR
                referencedSourceKey = Paths.get(filePath).getFileName().toString();
                if(referencedSourceKey.matches(".*-[0-9].*\\.jar")) { // Heuristic for typical maven jar names like artifact-version.jar
                    referencedSourceKey = "dependency:" + referencedSourceKey.substring(0, referencedSourceKey.lastIndexOf('-'));
                } else {
                    referencedSourceKey = "dependency:" + referencedSourceKey;
                }
            }
        } else if (referencedFqn.startsWith("java.") || referencedFqn.startsWith("javax.")) {
             referencedSourceKey = "jdk"; // Special case for JDK classes
        }


        CtElement contextOfReferenceUsage = referenceElement.getParent(p -> p instanceof CtExecutable || p instanceof CtType);
        if (contextOfReferenceUsage == null) {
             contextOfReferenceUsage = referenceElement.getParent(CtType.class);
        }
         if (contextOfReferenceUsage == null) {
            // System.out.println("Could not find usage context for reference: " + referenceElement.getShortRepresentation());
            return; // Cannot determine where this reference is used.
        }

        // Skip if the reference usage is within the analyzed target element itself when looking for "references TO target"
        if (!isReferenceFromTarget && contextOfReferenceUsage.equals(analyzedTargetElement)) {
            return;
        }
        // Skip if the referenced declaration is the analyzed target itself when looking for "references FROM target"
         if (isReferenceFromTarget && actualReferencedDeclaration != null && actualReferencedDeclaration.equals(analyzedTargetElement)) {
            return;
        }
        // Skip if the referenced declaration is the same as the context of usage (self-reference within a method/class body)
        // unless it's a specific type of reference we want to capture (e.g. recursive call, which would be an Executable ref)
        if (actualReferencedDeclaration != null && actualReferencedDeclaration.equals(contextOfReferenceUsage)) {
            // This is tricky. A class can reference itself (e.g. static field of its own type).
            // A method can call itself (recursion).
            // For "references TO", we want things outside the target.
            // For "references FROM", we want things called by the target.
            // If A calls B, and A is target, B is the reference.
            // If B calls A, and A is target, B is the reference.
            // If A calls A, and A is target, then A is the reference for "FROM" (recursion)
            // but not for "TO" (as it's internal).
            if (!isReferenceFromTarget && !(referenceElement instanceof CtExecutableReference)) { // Allow recursive calls to be listed as "from"
                 // return;
            }
        }


        String usageFqn, usageCodeContext;
        if (contextOfReferenceUsage instanceof CtExecutable) {
            CtExecutable<?> exec = (CtExecutable<?>) contextOfReferenceUsage;
            usageFqn = exec.getDeclaringType().getQualifiedName() + "#" + exec.getSignature();
            usageCodeContext = exec.toString();
        } else if (contextOfReferenceUsage instanceof CtType) {
            CtType<?> type = (CtType<?>) contextOfReferenceUsage;
            usageFqn = type.getQualifiedName();
            usageCodeContext = type.toString();
        } else {
            return;
        }

        String sourceOfUsage = "self"; // Default for the code that *contains* the reference
         if (contextOfReferenceUsage.getPosition().isValidPosition() && contextOfReferenceUsage.getPosition().getFile() != null) {
            String usageFilePath = contextOfReferenceUsage.getPosition().getFile().getAbsolutePath();
             if (usageFilePath.contains(".jar" + File.separator) || usageFilePath.endsWith(".jar")) {
                sourceOfUsage = "dependency:" + Paths.get(usageFilePath).getFileName().toString();
             } else if (usageFqn.startsWith("java.") || usageFqn.startsWith("javax.")){
                 sourceOfUsage = "jdk";
             }
        }


        AnalysisResult.Reference newRef;
        if (isReferenceFromTarget) {
            // The "reference" is what the target *points to*.
            // FQN is the FQN of the thing being pointed to.
            // Code context is the target element's code.
            // Source is the source of the thing being pointed to.
            String targetContextCode = "";
            if (analyzedTargetElement instanceof CtExecutable) targetContextCode = analyzedTargetElement.toString();
            else if (analyzedTargetElement instanceof CtType) targetContextCode = analyzedTargetElement.toString();
            newRef = new AnalysisResult.Reference(referencedSourceKey, referencedFqn, targetContextCode);
        } else {
            // The "reference" is what points *to* the target.
            // FQN is the FQN of the thing that contains the usage.
            // Code context is the code of the thing that contains the usage.
            // Source is the source of the thing that contains the usage.
            newRef = new AnalysisResult.Reference(sourceOfUsage, usageFqn, usageCodeContext);
        }

        if(uniqueReferences.add(newRef)) {
            // System.out.println("Added reference: " + newRef.getFullyQualifiedName() + " (Source: " + newRef.getSource() + ")");
        }
    }
}
