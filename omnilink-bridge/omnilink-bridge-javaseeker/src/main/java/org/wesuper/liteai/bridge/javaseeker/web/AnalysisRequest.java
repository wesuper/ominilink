package org.wesuper.liteai.bridge.javaseeker.web;

// Can use records for concise DTOs if preferred, or standard classes with getters/setters.
// Using a class here for broader compatibility/understanding.
public class AnalysisRequest {
    private String projectName;
    private String sourceIdentifier; // Local path or Git URL
    private String branchName;       // Optional, for Git sources
    private String codeSnippet;      // e.g., com.example.MyClass or com.example.MyClass#myMethod(String,int)

    // Getters and Setters
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }
}
