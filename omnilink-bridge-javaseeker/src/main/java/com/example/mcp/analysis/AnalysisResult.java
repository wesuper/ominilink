package com.example.mcp.analysis;

import java.util.List;
import java.util.Objects;

public class AnalysisResult {
    private final String analysisTarget;
    private final List<Reference> references;

    public AnalysisResult(String analysisTarget, List<Reference> references) {
        this.analysisTarget = analysisTarget;
        this.references = references;
    }

    public String getAnalysisTarget() {
        return analysisTarget;
    }

    public List<Reference> getReferences() {
        return references;
    }

    // Inner class for Reference
    public static class Reference {
        private final String source; // "self" or "dependency:group:artifact:version"
        private final String fullyQualifiedName;
        private final String codeContext;

        public Reference(String source, String fullyQualifiedName, String codeContext) {
            this.source = source;
            this.fullyQualifiedName = fullyQualifiedName;
            this.codeContext = codeContext;
        }

        public String getSource() {
            return source;
        }

        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        public String getCodeContext() {
            return codeContext;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Reference reference = (Reference) o;
            return Objects.equals(source, reference.source) &&
                   Objects.equals(fullyQualifiedName, reference.fullyQualifiedName) &&
                   Objects.equals(codeContext, reference.codeContext);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, fullyQualifiedName, codeContext);
        }
    }
}
