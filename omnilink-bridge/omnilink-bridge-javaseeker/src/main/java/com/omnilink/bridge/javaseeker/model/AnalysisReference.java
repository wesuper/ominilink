package com.omnilink.bridge.javaseeker.model;

import java.util.Objects;

/**
 * Represents a single code reference found during analysis.
 * <p>
 * This class encapsulates information about a reference, including its source (e.g., self, dependency, JDK),
 * its fully qualified name (FQN), the source code context where the reference occurs or is defined,
 * and the type of reference (e.g., "TO" the analysis target, or "FROM" the analysis target).
 * </p>
 * This object is typically streamed as a Server-Sent Event (SSE) payload.
 */
public class AnalysisReference {
    private final String source;
    private final String fullyQualifiedName;
    private final String codeContext;
    private final String referenceType;

    /**
     * Constructs a new AnalysisReference.
     * @param source The origin of the code containing this reference (e.g., "self", "dependency:group:artifact:version", "jdk").
     * @param fullyQualifiedName The fully qualified name of the class or method associated with this reference.
     * @param codeContext The source code snippet providing context for this reference.
     * @param referenceType The type of reference, typically "TO" (pointing to the analysis target)
     *                      or "FROM" (origination from the analysis target).
     */
    public AnalysisReference(String source, String fullyQualifiedName, String codeContext, String referenceType) {
        this.source = source;
        this.fullyQualifiedName = fullyQualifiedName;
        this.codeContext = codeContext;
        this.referenceType = referenceType;
    }

    /**
     * Gets the source origin of this reference.
     * @return A string indicating the source (e.g., "self", "dependency:...", "jdk").
     */
    public String getSource() { return source; }

    /**
     * Gets the fully qualified name of the element associated with this reference.
     * If {@code referenceType} is "TO", this is the FQN of the element *containing* the reference to the target.
     * If {@code referenceType} is "FROM", this is the FQN of the element *being referenced by* the target.
     * @return The fully qualified name.
     */
    public String getFullyQualifiedName() { return fullyQualifiedName; }

    /**
     * Gets the source code context for this reference.
     * If {@code referenceType} is "TO", this is the code of the element *containing* the reference.
     * If {@code referenceType} is "FROM", this is the code of the *analysis target element* where the reference is made.
     * @return The source code snippet.
     */
    public String getCodeContext() { return codeContext; }

    /**
     * Gets the type of reference.
     * @return "TO" if this reference points to the analysis target,
     *         "FROM" if this reference originates from the analysis target.
     */
    public String getReferenceType() { return referenceType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisReference that = (AnalysisReference) o;
        return Objects.equals(source, that.source) &&
               Objects.equals(fullyQualifiedName, that.fullyQualifiedName) &&
               Objects.equals(codeContext, that.codeContext) &&
               Objects.equals(referenceType, that.referenceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, fullyQualifiedName, codeContext, referenceType);
    }

    @Override
    public String toString() {
        return "AnalysisReference{" +
               "source='" + source + '\'' +
               ", fullyQualifiedName='" + fullyQualifiedName + '\'' +
               ", codeContextLength=" + (codeContext != null ? codeContext.length() : 0) +
               ", referenceType='" + referenceType + '\'' +
               '}';
    }
}
