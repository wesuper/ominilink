package com.example.mcp.analysis;

import com.example.mcp.project.ProjectSource;
import java.util.List;
import java.util.Map;

public interface CodeAnalysisService {
    AnalysisResult analyze(ProjectSource projectSource, String codeSnippet);
}
