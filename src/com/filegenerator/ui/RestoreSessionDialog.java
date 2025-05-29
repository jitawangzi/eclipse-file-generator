package com.filegenerator.ui;

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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

import com.filegenerator.core.BackupManager;
import com.filegenerator.core.BackupSession;

public class RestoreSessionDialog extends Dialog {
    private TableViewer sessionTableViewer;
    private TreeViewer fileTreeViewer;
    private BackupSession selectedSession;
    private List<BackupSession> sessions;
    
    public RestoreSessionDialog(Shell parentShell) {
        super(parentShell);
        this.sessions = BackupManager.getInstance().getBackupSessions();
    }
    
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("批量恢复文件");
        shell.setSize(800, 600);
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));
        
        Label titleLabel = new Label(container, SWT.NONE);
        titleLabel.setText("选择要恢复的备份会话：");
        
        SashForm sashForm = new SashForm(container, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        // 创建会话列表
        createSessionTable(sashForm);
        
        // 创建文件列表
        createFileTree(sashForm);
        
        sashForm.setWeights(new int[] {40, 60});
        
        return container;
    }
    
    private void createSessionTable(Composite parent) {
        sessionTableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
        Table table = sessionTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        
        // 创建列
        TableViewerColumn timeColumn = new TableViewerColumn(sessionTableViewer, SWT.NONE);
        timeColumn.getColumn().setText("备份时间");
        timeColumn.getColumn().setWidth(200);
        timeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                BackupSession session = (BackupSession) element;
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(session.getTimestamp());
            }
        });
        
        TableViewerColumn descColumn = new TableViewerColumn(sessionTableViewer, SWT.NONE);
        descColumn.getColumn().setText("描述");
        descColumn.getColumn().setWidth(300);
        descColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                BackupSession session = (BackupSession) element;
                return session.getDescription();
            }
        });
        
        TableViewerColumn countColumn = new TableViewerColumn(sessionTableViewer, SWT.NONE);
        countColumn.getColumn().setText("文件数量");
        countColumn.getColumn().setWidth(100);
        countColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                BackupSession session = (BackupSession) element;
                return String.valueOf(session.getFileCount());
            }
        });
        
        // 设置内容提供者
        sessionTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        
        // 设置输入
        sessionTableViewer.setInput(sessions);
        
        // 添加选择监听器
        sessionTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                selectedSession = (BackupSession) selection.getFirstElement();
                updateFileTree();
                getButton(IDialogConstants.OK_ID).setEnabled(selectedSession != null);
            }
        });
    }
    
    private void createFileTree(Composite parent) {
        fileTreeViewer = new TreeViewer(parent, SWT.BORDER | SWT.MULTI);
        Tree tree = fileTreeViewer.getTree();
        tree.setHeaderVisible(true);
        
        // 创建内容提供者
        fileTreeViewer.setContentProvider(new SessionFilesContentProvider());
        fileTreeViewer.setLabelProvider(new SessionFilesLabelProvider());
    }
    
    private void updateFileTree() {
        if (selectedSession != null) {
            fileTreeViewer.setInput(selectedSession);
        } else {
            fileTreeViewer.setInput(null);
        }
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        getButton(IDialogConstants.OK_ID).setText("恢复所有文件");
    }
    
    public BackupSession getSelectedSession() {
        return selectedSession;
    }
    
    /**
     * 检查是否有可用的备份会话
     */
    public boolean hasSessions() {
        return sessions != null && !sessions.isEmpty();
    }
}

