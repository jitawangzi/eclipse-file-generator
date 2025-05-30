package com.filegenerator.core;

import java.io.File;

/**
 * 项目类型检测器
 * 用于检测当前项目的类型并提供相应的路径调整规则
 */
public class ProjectTypeDetector {
    
    public enum ProjectType {
        MAVEN,      // Maven项目
        GRADLE,     // Gradle项目
        ECLIPSE_PLUGIN, // Eclipse插件项目
        STANDARD_JAVA,  // 标准Java项目
        UNKNOWN     // 未知项目类型
    }
    
    private String projectRoot;
    private ProjectType cachedType = null;
    
    public ProjectTypeDetector(String projectRoot) {
        this.projectRoot = projectRoot;
    }
    
    /**
     * 检测项目类型
     */
    public ProjectType detectProjectType() {
        if (cachedType != null) {
            return cachedType;
        }
        
		// 检查Eclipse插件项目 - 提高优先级，先检查
		if (new File(projectRoot, "META-INF/MANIFEST.MF").exists() && new File(projectRoot, "plugin.xml").exists()) {
			cachedType = ProjectType.ECLIPSE_PLUGIN;
			return ProjectType.ECLIPSE_PLUGIN;
		}

        // 检查Maven项目
        if (new File(projectRoot, "pom.xml").exists()) {
            cachedType = ProjectType.MAVEN;
            return ProjectType.MAVEN;
        }
        
        // 检查Gradle项目
        if (new File(projectRoot, "build.gradle").exists() || 
            new File(projectRoot, "build.gradle.kts").exists()) {
            cachedType = ProjectType.GRADLE;
            return ProjectType.GRADLE;
        }
        
        // 检查是否有src目录，判断为标准Java项目
        if (new File(projectRoot, "src").exists() || 
            new File(projectRoot, "source").exists()) {
            cachedType = ProjectType.STANDARD_JAVA;
            return ProjectType.STANDARD_JAVA;
        }
        
        cachedType = ProjectType.UNKNOWN;
        return ProjectType.UNKNOWN;
    }
    
    /**
     * 根据项目类型和文件类型调整文件路径
     * 
     * @param filePath 原始文件路径
     * @param isJavaFile 是否为Java文件
     * @return 调整后的文件路径
     */
    public String adjustFilePath(String filePath, boolean isJavaFile) {
        // 如果路径已经包含src、source等目录前缀，则不做调整
        if (filePath.startsWith("src/") || 
            filePath.startsWith("source/") || 
            filePath.startsWith("META-INF/")) {
            return filePath;
        }
        
        ProjectType type = detectProjectType();
        
        switch (type) {
		case ECLIPSE_PLUGIN:
			// Eclipse插件项目只添加src前缀
			return "src/" + filePath;

            case MAVEN:
                if (isJavaFile) {
                    return "src/main/java/" + filePath;
                } else if (filePath.endsWith(".xml") || 
                          filePath.endsWith(".properties") || 
                          filePath.endsWith(".yml")) {
                    return "src/main/resources/" + filePath;
                }
                break;
                
            case GRADLE:
                if (isJavaFile) {
                    return "src/main/java/" + filePath;
                } else if (filePath.endsWith(".xml") || 
                          filePath.endsWith(".properties") || 
                          filePath.endsWith(".yml")) {
                    return "src/main/resources/" + filePath;
                }
                break;
                
            case STANDARD_JAVA:
                if (isJavaFile) {
                    return "src/" + filePath;
                }
                break;
                
            case UNKNOWN:
            default:
				// 对于未知项目类型，仅添加src前缀
				return "src/" + filePath;
        }
        
        return filePath;
    }
    
    /**
     * 判断文件是否为Java文件
     */
    public static boolean isJavaFile(String filePath) {
        return filePath.endsWith(".java");
    }
}
