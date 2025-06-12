package org.wesuper.liteai.bridge.javaseeker.analysis;

import org.wesuper.liteai.bridge.javaseeker.project.ProjectSource;
import java.util.List;
import java.util.Map;

public interface CodeAnalysisService {
    AnalysisResult analyze(ProjectSource projectSource, String codeSnippet);
}
