package com.filegenerator.ui;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.filegenerator.core.BackupManager;
import com.filegenerator.core.BackupManager.BackupEntry;

public class RestoreFileHandler extends AbstractHandler {
    final ILog log = Platform.getLog(Platform.getBundle("com.filegenerator"));

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Display.getDefault().asyncExec(() -> {
            try {
                ISelection selection = HandlerUtil.getCurrentSelection(event);
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                    Object firstElement = structuredSelection.getFirstElement();
                    
                    String filePath = getFilePath(firstElement);
                    
                    if (filePath != null) {
                        showRestoreDialog(event, filePath);
                    } else {
                        showMessage(event, "请选择一个文件来恢复备份");
                    }
                }
            } catch (Exception e) {
                log.log(new Status(Status.ERROR, "com.filegenerator", "恢复文件时出错", e));
                showErrorMessage(event, "恢复文件时出错: " + e.getMessage());
            }
        });
        
        return null;
    }
    
    private String getFilePath(Object element) {
        if (element instanceof IFile) {
            return ((IFile) element).getLocation().toOSString();
        } else if (element instanceof ICompilationUnit) {
            ICompilationUnit unit = (ICompilationUnit) element;
            IResource resource = unit.getResource();
            if (resource instanceof IFile) {
                return resource.getLocation().toOSString();
            }
        } else if (element instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) element;
            IFile file = adaptable.getAdapter(IFile.class);
            if (file != null) {
                return file.getLocation().toOSString();
            }
        }
        
        return null;
    }
    
    private void showRestoreDialog(ExecutionEvent event, String filePath) {
        try {
            Shell shell = HandlerUtil.getActiveShell(event);
            RestoreDialog dialog = new RestoreDialog(shell, filePath);
            
            if (!dialog.hasBackups()) {
                showMessage(event, "该文件没有可用的备份");
                return;
            }
            
            if (dialog.open() == RestoreDialog.OK) {
                BackupEntry selectedBackup = dialog.getSelectedBackup();
                if (selectedBackup != null) {
                    BackupManager backupManager = BackupManager.getInstance();
                    boolean success = backupManager.restoreFromBackup(
                            selectedBackup.getBackupFilePath(), 
                            selectedBackup.getOriginalFilePath());
                    
                    if (success) {
                        // 刷新文件
                        refreshFile(filePath);
                        showMessage(event, "文件已成功恢复到备份版本");
                    } else {
                        showErrorMessage(event, "恢复文件失败");
                    }
                }
            }
        } catch (Exception e) {
            log.log(new Status(Status.ERROR, "com.filegenerator", "显示恢复对话框时出错", e));
            showErrorMessage(event, "显示恢复对话框时出错: " + e.getMessage());
        }
    }
    
    private void refreshFile(String filePath) {
        try {
            // 在Eclipse工作空间中刷新文件
            IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(new File(filePath).toURI());
            for (IFile file : files) {
                file.refreshLocal(IResource.DEPTH_ZERO, null);
            }
        } catch (Exception e) {
            log.log(new Status(Status.WARNING, "com.filegenerator", "刷新文件时出错", e));
        }
    }
    
    private void showMessage(ExecutionEvent event, String message) {
        Shell shell = HandlerUtil.getActiveShell(event);
        MessageDialog.openInformation(shell, "文件恢复", message);
    }
    
    private void showErrorMessage(ExecutionEvent event, String message) {
        Shell shell = HandlerUtil.getActiveShell(event);
        MessageDialog.openError(shell, "错误", message);
    }
}