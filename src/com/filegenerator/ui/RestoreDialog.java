package com.filegenerator.ui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import com.filegenerator.core.BackupManager;
import com.filegenerator.core.BackupManager.BackupEntry;

public class RestoreDialog extends Dialog {
    private String filePath;
    private TableViewer tableViewer;
    private BackupEntry selectedBackup;
    private List<BackupEntry> backups;
    
    public RestoreDialog(Shell parentShell, String filePath) {
        super(parentShell);
        this.filePath = filePath;
        
        // 获取文件的备份记录
        BackupManager backupManager = BackupManager.getInstance();
        this.backups = backupManager.getBackupsForFile(filePath);
    }
    
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("恢复文件");
        shell.setSize(600, 400);
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));
        
        Label titleLabel = new Label(container, SWT.NONE);
        titleLabel.setText("选择要恢复的备份版本：");
        
        Label fileLabel = new Label(container, SWT.NONE);
        fileLabel.setText("文件: " + new File(filePath).getName());
        
        // 创建备份列表
        tableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        table.setLayoutData(gridData);
        
        // 创建列
        TableViewerColumn timeColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        timeColumn.getColumn().setText("备份时间");
        timeColumn.getColumn().setWidth(200);
        timeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                BackupEntry entry = (BackupEntry) element;
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(entry.getTimestamp());
            }
        });
        
        TableViewerColumn pathColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        pathColumn.getColumn().setText("备份文件路径");
        pathColumn.getColumn().setWidth(350);
        pathColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                BackupEntry entry = (BackupEntry) element;
                return entry.getBackupFilePath();
            }
        });
        
        // 设置内容提供者
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        
        // 设置输入
        tableViewer.setInput(backups);
        
        // 添加选择监听器
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                selectedBackup = (BackupEntry) selection.getFirstElement();
                getButton(IDialogConstants.OK_ID).setEnabled(selectedBackup != null);
            }
        });
        
        return container;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        getButton(IDialogConstants.OK_ID).setText("恢复");
    }
    
    public BackupEntry getSelectedBackup() {
        return selectedBackup;
    }
    
    @Override
    protected void okPressed() {
        super.okPressed();
    }
    
    /**
     * 检查是否有可用的备份
     */
    public boolean hasBackups() {
        return backups != null && !backups.isEmpty();
    }
}