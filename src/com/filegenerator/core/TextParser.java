// File: src/com/filegenerator/core/TextParser.java
package com.filegenerator.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public class TextParser {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("public\\s+(?:class|interface|enum)\\s+(\\w+)");
    
    private ProjectTypeDetector projectTypeDetector;
	private final ILog log;
    
    /**
     * 默认构造函数，使用当前工作目录作为项目根目录
     */
    public TextParser() {
        this(new File("").getAbsolutePath());
    }
    
    /**
     * 使用指定的项目根目录创建TextParser
     * 
     * @param projectRoot 项目根目录路径
     */
    public TextParser(String projectRoot) {
        this.projectTypeDetector = new ProjectTypeDetector(projectRoot);
		this.log = Platform.getLog(Platform.getBundle("com.filegenerator"));
    }

    public List<FileModel> parseText(String text) {
        List<FileModel> fileModels = new ArrayList<>();
        
		// 预处理：移除可能的Markdown格式标记、规范化换行符
		text = cleanInputText(text);

		// 记录原始输入的前100个字符，帮助调试
		String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;
		log.log(new Status(Status.INFO, "com.filegenerator", "输入文本预览: " + preview.replace("\n", "\\n")));

		// 使用简单的行分割方法查找文件标记
		String[] lines = text.split("\n");
		log.log(new Status(Status.INFO, "com.filegenerator", "输入文本共 " + lines.length + " 行"));

		List<Integer> fileMarkerLines = new ArrayList<>();
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.startsWith("// File:")) {
				fileMarkerLines.add(i);
				log.log(new Status(Status.INFO, "com.filegenerator", "找到文件标记(第" + (i + 1) + "行): " + line));
			}
		}

		log.log(new Status(Status.INFO, "com.filegenerator", "找到 " + fileMarkerLines.size() + " 个文件标记行"));
        
		// 处理每个文件段落
		for (int i = 0; i < fileMarkerLines.size(); i++) {
			int markerLineIndex = fileMarkerLines.get(i);
			String markerLine = lines[markerLineIndex].trim();

			// 提取文件路径
			String filePath = markerLine.substring("// File:".length()).trim();
			// 如果路径包含注释部分，只取前面的部分
			if (filePath.contains("(")) {
				filePath = filePath.substring(0, filePath.indexOf("(")).trim();
			}

			// 计算内容的开始和结束行
			int contentStartLine = markerLineIndex + 1;
			int contentEndLine = (i < fileMarkerLines.size() - 1) ? fileMarkerLines.get(i + 1) - 1 : lines.length - 1;

			// 构建内容
			StringBuilder content = new StringBuilder();
			for (int j = contentStartLine; j <= contentEndLine; j++) {
				content.append(lines[j]).append("\n");
			}

			// 调整文件路径
			String adjustedPath = adjustFilePath(filePath);

			log.log(new Status(Status.INFO, "com.filegenerator",
					"添加文件: " + filePath + " -> " + adjustedPath + " (内容行数: " + (contentEndLine - contentStartLine + 1) + ")"));
            
			fileModels.add(new FileModel(adjustedPath, content.toString()));
        }
        
		// 如果没有找到任何文件，尝试从整个文本推断
        if (fileModels.isEmpty() && text.trim().length() > 0) {
            String inferredPath = inferPathFromJavaCode(text);
            if (inferredPath != null) {
				log.log(new Status(Status.INFO, "com.filegenerator", "从整个内容推断文件路径: " + inferredPath));
                fileModels.add(new FileModel(inferredPath, text));
            }
        }
        
        return fileModels;
    }
    
	/**
	 * 清理输入文本
	 */
	private String cleanInputText(String text) {
		// 规范化换行符
		text = text.replace("\r\n", "\n").replace("\r", "\n");

		// 尝试识别并移除Markdown代码块标记
		if (text.startsWith("```")) {
			log.log(new Status(Status.INFO, "com.filegenerator", "检测到Markdown代码块标记"));

			// 查找结束标记
			int endMarkIndex = text.lastIndexOf("```");
			if (endMarkIndex > 3) { // 确保不是同一个标记
				// 找到第一行结束
				int firstLineEnd = text.indexOf('\n', 3);
				if (firstLineEnd != -1 && firstLineEnd < endMarkIndex) {
					// 移除开始和结束标记
					text = text.substring(firstLineEnd + 1, endMarkIndex);
					log.log(new Status(Status.INFO, "com.filegenerator", "移除了Markdown代码块标记，处理后长度: " + text.length()));
				}
			}
		}

		return text;
	}

    private String adjustFilePath(String filePath) {
        boolean isJavaFile = ProjectTypeDetector.isJavaFile(filePath);
        return projectTypeDetector.adjustFilePath(filePath, isJavaFile);
    }
    
    private String inferPathFromJavaCode(String code) {
        // 查找包名
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(code);
        if (packageMatcher.find()) {
            String packageName = packageMatcher.group(1);
            
            // 查找类名
            Matcher classMatcher = CLASS_PATTERN.matcher(code);
            
            if (classMatcher.find()) {
                String className = classMatcher.group(1);
                // 转换包名为路径
                String packagePath = packageName.replace('.', '/');
                String relativePath = packagePath + "/" + className + ".java";
                
                // 根据项目类型调整路径
                return projectTypeDetector.adjustFilePath(relativePath, true);
            }
        }
        
        return null;
    }
}