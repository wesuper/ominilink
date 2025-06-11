package com.example.mcp.web;

import com.example.mcp.analysis.AnalysisResult;
import com.example.mcp.analysis.CodeAnalysisService;
import com.example.mcp.project.ProjectLoaderService;
import com.example.mcp.project.ProjectSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final ProjectLoaderService projectLoaderService;
    private final CodeAnalysisService codeAnalysisService;

    @Autowired
    public McpController(ProjectLoaderService projectLoaderService, CodeAnalysisService codeAnalysisService) {
        this.projectLoaderService = projectLoaderService;
        this.codeAnalysisService = codeAnalysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeCode(@RequestBody AnalysisRequest request) {
        if (request.getProjectName() == null || request.getProjectName().isEmpty() ||
            request.getSourceIdentifier() == null || request.getSourceIdentifier().isEmpty() ||
            request.getCodeSnippet() == null || request.getCodeSnippet().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required fields: projectName, sourceIdentifier, codeSnippet");
        }

        // Try to load or get the project
        Optional<ProjectSource> projectSourceOpt = projectLoaderService.getProject(request.getProjectName());

        if (!projectSourceOpt.isPresent()) {
             System.out.println("Project not found in cache, attempting to load: " + request.getProjectName());
            projectSourceOpt = projectLoaderService.loadProject(
                request.getProjectName(),
                request.getSourceIdentifier(),
                request.getBranchName() // Can be null
            );
        } else {
             System.out.println("Project found in cache: " + request.getProjectName());
             // Optional: Add logic here to check if the cached sourceIdentifier matches the request's one.
             // If not, it might indicate a request to re-load or use a different source for the same project name.
             // For simplicity, this example assumes if a project name exists, it's the correct one.
        }


        if (!projectSourceOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Failed to load project: " + request.getProjectName() + " from " + request.getSourceIdentifier());
        }

        ProjectSource projectSource = projectSourceOpt.get();

        try {
            System.out.println("Starting analysis for snippet: " + request.getCodeSnippet() + " in project: " + projectSource.getProjectName());
            AnalysisResult result = codeAnalysisService.analyze(projectSource, request.getCodeSnippet());
            System.out.println("Analysis complete. Found " + result.getReferences().size() + " references.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
            // Optional: Unload project if analysis fails critically
            // projectLoaderService.unloadProject(request.getProjectName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("An error occurred during code analysis: " + e.getMessage());
        }
    }
}
