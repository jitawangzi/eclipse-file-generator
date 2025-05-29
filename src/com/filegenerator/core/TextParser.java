// File: src/main/java/com/filegenerator/core/TextParser.java
package com.filegenerator.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParser {
	// 修改正则表达式，只捕获实际文件路径部分，忽略括号中的注释
	private static final Pattern FILE_PATTERN = Pattern.compile("//\\s*File:\\s*([^(\\s]+)(?:\\s+\\([^)]*\\))?");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("public\\s+(?:class|interface|enum)\\s+(\\w+)");

    public List<FileModel> parseText(String text) {
        List<FileModel> fileModels = new ArrayList<>();
        String[] lines = text.split("\n");
        
        StringBuilder currentContent = new StringBuilder();
        String currentFilePath = null;
        boolean collectingContent = false;
        
        for (String line : lines) {
            Matcher fileMatcher = FILE_PATTERN.matcher(line);
            
            if (fileMatcher.find()) {
                // 如果已经在收集内容，保存前一个文件
                if (collectingContent && currentFilePath != null) {
                    fileModels.add(new FileModel(currentFilePath, currentContent.toString()));
                }
                
				// 只使用捕获组1作为文件路径，忽略可能的注释部分
                currentFilePath = fileMatcher.group(1).trim();
                currentContent = new StringBuilder();
                collectingContent = true;
            } else if (collectingContent) {
                currentContent.append(line).append("\n");
            }
        }
        
        // 保存最后一个文件
        if (collectingContent && currentFilePath != null) {
            fileModels.add(new FileModel(currentFilePath, currentContent.toString()));
        }
        
        // 如果没有找到任何文件标记，但内容看起来是Java代码，尝试基于包名推断
        if (fileModels.isEmpty() && text.trim().length() > 0) {
            String inferredPath = inferPathFromJavaCode(text);
            if (inferredPath != null) {
                fileModels.add(new FileModel(inferredPath, text));
            }
        }
        
        return fileModels;
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
                return "src/main/java/" + packagePath + "/" + className + ".java";
            }
        }
        
        return null;
    }
}