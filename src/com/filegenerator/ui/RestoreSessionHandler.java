package com.filegenerator.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.filegenerator.core.BackupManager;
import com.filegenerator.core.BackupManager.BackupEntry;
import com.filegenerator.core.BackupSession;

public class RestoreSessionHandler extends AbstractHandler {
    final ILog log = Platform.getLog(Platform.getBundle("com.filegenerator"));

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Display.getDefault().asyncExec(() -> {
            try {
                Shell shell = HandlerUtil.getActiveShell(event);
                showRestoreSessionDialog(shell, event);
            } catch (Exception e) {
                log.log(new Status(Status.ERROR, "com.filegenerator", "恢复会话时出错", e));
                showErrorMessage(event, "恢复会话时出错: " + e.getMessage());
            }
        });
        
        return null;
    }
    
    private void showRestoreSessionDialog(Shell shell, ExecutionEvent event) {
        RestoreSessionDialog dialog = new RestoreSessionDialog(shell);
        
        if (!dialog.hasSessions()) {
            showMessage(event, "没有可用的备份会话");
            return;
        }
        
        if (dialog.open() == RestoreSessionDialog.OK) {
            BackupSession selectedSession = dialog.getSelectedSession();
            if (selectedSession != null) {
                boolean confirmed = MessageDialog.openConfirm(shell, "确认恢复", 
                        "确定要恢复这个会话中的 " + selectedSession.getFileCount() + " 个文件吗？\n" +
                        "备份时间: " + selectedSession.getTimestamp());
                
                if (confirmed) {
                    restoreSession(selectedSession, event);
                }
            }
        }
    }
    
    private void restoreSession(BackupSession session, ExecutionEvent event) {
        try {
            BackupManager backupManager = BackupManager.getInstance();
            int successCount = 0;
            int failCount = 0;
            
            for (BackupEntry entry : session.getBackupEntries()) {
                boolean success = backupManager.restoreFromBackup(
                        entry.getBackupFilePath(), 
                        entry.getOriginalFilePath());
                
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
            
            // 刷新项目
            refreshProjects(event);
            
            showMessage(event, "批量恢复完成\n成功: " + successCount + " 个文件\n失败: " + failCount + " 个文件");
        } catch (Exception e) {
            log.log(new Status(Status.ERROR, "com.filegenerator", "批量恢复文件失败", e));
            showErrorMessage(event, "批量恢复文件失败: " + e.getMessage());
        }
    }
    
    private void refreshProjects(ExecutionEvent event) {
		try {
			for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
				if (project.isOpen()) {
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				}
			}
		} catch (Exception e) {
			log.log(new Status(Status.WARNING, "com.filegenerator", "刷新项目时出错", e));
		}
    }
    
    private void showMessage(ExecutionEvent event, String message) {
        Shell shell = HandlerUtil.getActiveShell(event);
        MessageDialog.openInformation(shell, "批量文件恢复", message);
    }
    
    private void showErrorMessage(ExecutionEvent event, String message) {
        Shell shell = HandlerUtil.getActiveShell(event);
        MessageDialog.openError(shell, "错误", message);
    }
}
