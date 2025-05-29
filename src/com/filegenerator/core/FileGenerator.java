package com.filegenerator.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public class FileGenerator {
    private String basePath;
    private ILog log;
    private BackupManager backupManager;
    
    public FileGenerator(String basePath) {
        this.basePath = basePath;
        this.log = Platform.getLog(Platform.getBundle("com.filegenerator"));
        this.backupManager = BackupManager.getInstance();
    }
    
    public void generateFiles(List<FileModel> fileModels) throws IOException {
        // 开始一个新的备份会话
        backupManager.startBackupSession();
        
        try {
            for (FileModel model : fileModels) {
                generateFile(model);
            }
        } finally {
            // 结束备份会话
            backupManager.endBackupSession();
        }
    }
    
    private void generateFile(FileModel model) throws IOException {
        String fullPath = basePath + File.separator + model.getFilePath();
        File file = new File(fullPath);
        
        // 创建目录结构
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // 如果文件已存在，先备份
        if (file.exists()) {
            backupManager.backupFile(fullPath);
        }
        
        // 写入新文件
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(model.getContent());
        }
        
        log.log(new Status(Status.INFO, "com.filegenerator", "已生成文件: " + fullPath));
    }
}

