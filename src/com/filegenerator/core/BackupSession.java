package com.filegenerator.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 表示一次备份会话，包含同一次操作中备份的多个文件
 */
public class BackupSession {
    private String sessionId;
    private Date timestamp;
    private List<String> files;
    private List<BackupManager.BackupEntry> backupEntries;
    private String description;
    
    public BackupSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.timestamp = new Date();
        this.files = new ArrayList<>();
        this.backupEntries = new ArrayList<>();
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void addFile(String filePath) {
        if (!files.contains(filePath)) {
            files.add(filePath);
        }
    }
    
    public List<String> getFiles() {
        return files;
    }
    
    public void addBackupEntry(BackupManager.BackupEntry entry) {
        backupEntries.add(entry);
    }
    
    public List<BackupManager.BackupEntry> getBackupEntries() {
        return backupEntries;
    }
    
    public String getDescription() {
        return description != null ? description : "修改了 " + files.size() + " 个文件";
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getFileCount() {
        return files.size();
    }
}

