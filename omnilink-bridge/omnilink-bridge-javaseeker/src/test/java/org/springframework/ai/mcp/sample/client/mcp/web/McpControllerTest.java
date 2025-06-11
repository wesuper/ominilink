package org.springframework.ai.mcp.sample.client.mcp.web;

import org.wesuper.liteai.bridge.javaseeker.analysis.AnalysisResult;
import org.wesuper.liteai.bridge.javaseeker.analysis.CodeAnalysisService;
import org.wesuper.liteai.bridge.javaseeker.project.ProjectLoaderService;
import org.wesuper.liteai.bridge.javaseeker.project.ProjectSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(McpController.class)
class McpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectLoaderService projectLoaderService;

    @MockBean
    private CodeAnalysisService codeAnalysisService;

    @Autowired
    private ObjectMapper objectMapper; // For converting request object to JSON

    @Test
    void analyzeCode_Success() throws Exception {
        AnalysisRequest request = new AnalysisRequest();
        request.setProjectName("testProject");
        request.setSourceIdentifier("/path/to/project");
        request.setCodeSnippet("com.example.MyClass");

        ProjectSource mockProjectSource = mock(ProjectSource.class);
        when(mockProjectSource.getProjectName()).thenReturn("testProject");
        when(mockProjectSource.getProjectPath()).thenReturn(Paths.get("/path/to/project"));

        when(projectLoaderService.getProject("testProject")).thenReturn(Optional.empty());
        when(projectLoaderService.loadProject("testProject", "/path/to/project", null))
            .thenReturn(Optional.of(mockProjectSource));

        AnalysisResult mockResult = new AnalysisResult("com.example.MyClass", Collections.emptyList());
        when(codeAnalysisService.analyze(any(ProjectSource.class), anyString())).thenReturn(mockResult);

        mockMvc.perform(post("/api/mcp/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisTarget").value("com.example.MyClass"));
    }

    @Test
    void analyzeCode_ProjectLoadFail() throws Exception {
        AnalysisRequest request = new AnalysisRequest();
        request.setProjectName("failProject");
        request.setSourceIdentifier("/path/to/fail");
        request.setCodeSnippet("com.example.FailClass");

        when(projectLoaderService.getProject(anyString())).thenReturn(Optional.empty());
        when(projectLoaderService.loadProject(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/mcp/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("Failed to load project: failProject from /path/to/fail"));
    }

    @Test
    void analyzeCode_AnalysisError() throws Exception {
        AnalysisRequest request = new AnalysisRequest();
        request.setProjectName("analysisErrorProject");
        request.setSourceIdentifier("/path/to/project");
        request.setCodeSnippet("com.example.ErrorClass");

        ProjectSource mockProjectSource = mock(ProjectSource.class);
        when(projectLoaderService.getProject(anyString())).thenReturn(Optional.empty());
        when(projectLoaderService.loadProject(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(mockProjectSource));

        when(codeAnalysisService.analyze(any(ProjectSource.class), anyString()))
            .thenThrow(new RuntimeException("Spoon crashed"));

        mockMvc.perform(post("/api/mcp/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("An error occurred during code analysis: Spoon crashed"));
    }

    @Test
    void analyzeCode_BadRequest_MissingFields() throws Exception {
        AnalysisRequest request = new AnalysisRequest(); // Missing all fields
        mockMvc.perform(post("/api/mcp/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Missing required fields: projectName, sourceIdentifier, codeSnippet"));
    }
}
