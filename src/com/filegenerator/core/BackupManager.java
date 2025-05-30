package com.filegenerator.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public class BackupManager {
    private static final String BACKUP_DIR = ".filebackups";
    private static final String SESSION_FILE = "backup_sessions.dat";
    private static BackupManager instance;
    private final ILog log;
    
    // 存储文件备份历史记录
    private Map<String, List<BackupEntry>> backupHistory = new HashMap<>();
    
    // 存储批量备份会话
    private List<BackupSession> backupSessions = new ArrayList<>();
    
    // 当前活动的会话
    private BackupSession currentSession;
    
    private BackupManager() {
        log = Platform.getLog(Platform.getBundle("com.filegenerator"));
        loadSessions();
    }
    
    public static synchronized BackupManager getInstance() {
        if (instance == null) {
            instance = new BackupManager();
        }
        return instance;
    }
    
    /**
     * 开始一个新的备份会话
     */
    public void startBackupSession() {
        currentSession = new BackupSession();
        log.log(new Status(Status.INFO, "com.filegenerator", "开始新的备份会话: " + currentSession.getSessionId()));
    }
    
    /**
     * 结束当前备份会话并保存
     */
    public void endBackupSession() {
        if (currentSession != null && currentSession.getFileCount() > 0) {
            backupSessions.add(currentSession);
            saveSessions();
            log.log(new Status(Status.INFO, "com.filegenerator", 
                    "结束备份会话: " + currentSession.getSessionId() + 
                    ", 包含 " + currentSession.getFileCount() + " 个文件"));
            currentSession = null;
        }
    }
    
    /**
     * 获取所有备份会话
     */
    public List<BackupSession> getBackupSessions() {
        return Collections.unmodifiableList(backupSessions);
    }
    
    /**
     * 在覆盖文件前创建备份
     * 
     * @param filePath 要备份的文件路径
     * @return 备份文件的路径，如果备份失败则返回null
     */
    public String backupFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return null; // 文件不存在，无需备份
        }
        
        try {
            // 确保备份目录存在
            File backupDir = new File(file.getParentFile(), BACKUP_DIR);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // 创建带时间戳的备份文件名
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = file.getName();
            String backupFileName = fileName + "." + timestamp + ".bak";
            
            File backupFile = new File(backupDir, backupFileName);
            
            // 复制文件
            Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // 记录备份历史
            String originalFilePath = file.getAbsolutePath();
            BackupEntry entry = new BackupEntry(originalFilePath, backupFile.getAbsolutePath(), new Date());
            
            if (!backupHistory.containsKey(originalFilePath)) {
                backupHistory.put(originalFilePath, new ArrayList<>());
            }
            backupHistory.get(originalFilePath).add(entry);
            
            // 添加到当前会话
            if (currentSession != null) {
                currentSession.addFile(originalFilePath);
                currentSession.addBackupEntry(entry);
            }
            
            log.log(new Status(Status.INFO, "com.filegenerator", "已备份文件: " + originalFilePath + " 到 " + backupFile.getAbsolutePath()));
            
            return backupFile.getAbsolutePath();
        } catch (IOException e) {
            log.log(new Status(Status.ERROR, "com.filegenerator", "备份文件失败: " + filePath, e));
            return null;
        }
    }
    
    /**
     * 获取指定文件的所有备份记录
     */
    public List<BackupEntry> getBackupsForFile(String filePath) {
        List<BackupEntry> backups = backupHistory.get(filePath);
        if (backups == null) {
            // 尝试扫描目录查找历史备份
            backups = scanForBackups(filePath);
            if (!backups.isEmpty()) {
                backupHistory.put(filePath, backups);
            }
        }
        return backups != null ? backups : Collections.emptyList();
    }
    
    /**
     * 从备份恢复文件
     */
    public boolean restoreFromBackup(String backupFilePath, String targetFilePath) {
        try {
            // 先备份当前文件
            backupFile(targetFilePath);
            
            // 从备份恢复
            File backupFile = new File(backupFilePath);
            File targetFile = new File(targetFilePath);
            
            if (!backupFile.exists()) {
                log.log(new Status(Status.ERROR, "com.filegenerator", "备份文件不存在: " + backupFilePath));
                return false;
            }
            
            Files.copy(backupFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.log(new Status(Status.INFO, "com.filegenerator", "已从备份恢复文件: " + targetFilePath));
            
            return true;
        } catch (IOException e) {
            log.log(new Status(Status.ERROR, "com.filegenerator", "从备份恢复文件失败", e));
            return false;
        }
    }
    
    /**
     * 扫描目录查找历史备份
     */
    private List<BackupEntry> scanForBackups(String filePath) {
        List<BackupEntry> backups = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
            return backups;
        }
        
        File backupDir = new File(file.getParentFile(), BACKUP_DIR);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return backups;
        }
        
        String fileName = file.getName();
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith(fileName) && name.endsWith(".bak"));
        
        if (backupFiles != null) {
            for (File backupFile : backupFiles) {
                String backupName = backupFile.getName();
                // 从文件名中提取时间戳
                try {
                    String timestampStr = backupName.substring(fileName.length() + 1, backupName.length() - 4);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    Date timestamp = sdf.parse(timestampStr);
                    
                    BackupEntry entry = new BackupEntry(filePath, backupFile.getAbsolutePath(), timestamp);
                    backups.add(entry);
                } catch (Exception e) {
                    log.log(new Status(Status.WARNING, "com.filegenerator", "无法解析备份文件名: " + backupName, e));
                }
            }
        }
        
        // 按时间排序，最新的在前
        Collections.sort(backups, (a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        
        return backups;
    }
    
    /**
     * 保存会话信息到文件
     */
    private void saveSessions() {
        try {
            File sessionFile = getSessionFile();
            if (sessionFile == null) {
                log.log(new Status(Status.INFO, "com.filegenerator", "无法获取会话文件位置，跳过备份保存"));
                return;
            }
            
            // 确保父目录存在
            File parentDir = sessionFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(sessionFile))) {
                oos.writeObject(backupSessions);
            }
            
            log.log(new Status(Status.INFO, "com.filegenerator", "已保存 " + backupSessions.size() + " 个备份会话"));
        } catch (IOException e) {
            log.log(new Status(Status.WARNING, "com.filegenerator", "保存备份会话失败，但不影响主要功能", e));
        }
    }
    
    /**
     * 从文件加载会话信息
     */
    @SuppressWarnings("unchecked")
    private void loadSessions() {
        try {
            File sessionFile = getSessionFile();
            if (sessionFile == null || !sessionFile.exists()) {
                backupSessions = new ArrayList<>();
                return;
            }
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(sessionFile))) {
                Object obj = ois.readObject();
                if (obj instanceof List<?>) {
                    backupSessions = (List<BackupSession>) obj;
                    log.log(new Status(Status.INFO, "com.filegenerator", "已加载 " + backupSessions.size() + " 个备份会话"));
                }
            }
        } catch (Exception e) {
            log.log(new Status(Status.WARNING, "com.filegenerator", "加载备份会话失败，将使用新的会话", e));
            // 如果加载失败，使用空列表
            backupSessions = new ArrayList<>();
        }
    }
    
    /**
     * 获取会话文件位置，兼容多种环境
     */
    private File getSessionFile() {
        try {
            // 首先尝试插件状态位置
            if (Platform.getBundle("com.filegenerator") != null) {
                File dataDir = Platform.getStateLocation(Platform.getBundle("com.filegenerator")).toFile();
                return new File(dataDir, SESSION_FILE);
            }
        } catch (Exception e) {
            // 如果插件状态位置不可用，忽略错误
        }
        
        try {
            // 尝试使用临时目录
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "filegenerator");
            return new File(tempDir, SESSION_FILE);
        } catch (Exception e) {
            // 如果临时目录也不可用，记录警告
            log.log(new Status(Status.WARNING, "com.filegenerator", "无法确定备份存储位置", e));
        }
        
        return null;
    }
    
    /**
     * 备份记录类
     */
    public static class BackupEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String originalFilePath;
        private String backupFilePath;
        private Date timestamp;
        
        public BackupEntry(String originalFilePath, String backupFilePath, Date timestamp) {
            this.originalFilePath = originalFilePath;
            this.backupFilePath = backupFilePath;
            this.timestamp = timestamp;
        }
        
        public String getOriginalFilePath() {
            return originalFilePath;
        }
        
        public String getBackupFilePath() {
            return backupFilePath;
        }
        
        public Date getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
        }
    }
}
