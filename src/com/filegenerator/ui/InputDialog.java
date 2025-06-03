// File: src/com/filegenerator/ui/InputDialog.java
package com.filegenerator.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.filegenerator.core.FileModel;
import com.filegenerator.core.TextParser;

public class InputDialog extends Dialog {

    private Text textInput;
    private Combo moduleCombo;
    private ListViewer fileListViewer;
    private Text contentText;
    private Button previewButton;
    
    private String inputText = "";
    private String selectedModule = "";
    private IProject project;
    private List<String> moduleNames = new ArrayList<>();
    private List<String> modulePaths = new ArrayList<>();
    private List<FileModel> fileModels = new ArrayList<>();
    private final ILog log = Platform.getLog(Platform.getBundle("com.filegenerator"));
	private boolean buttonsCreated = false; // 标记按钮是否已创建

    public InputDialog(Shell parentShell, IProject project) {
        super(parentShell);
        this.project = project;
        detectModules();
    }

    private void detectModules() {
        // 添加主项目
		moduleNames.add(project.getName() + " (main project)");
        modulePaths.add(project.getLocation().toOSString());
        
        // 尝试检测Maven模块
        try {
            IJavaProject javaProject = JavaCore.create(project);
            if (javaProject != null && javaProject.exists()) {
                project.accept(resource -> {
                    if (resource.getType() == IProject.FOLDER) {
                        String folderName = resource.getName();
                        // 如果文件夹中包含pom.xml，可能是Maven模块
                        IFolder folder = (IFolder) resource;
                        IFile pomFile = folder.getFile("pom.xml");
                        if (pomFile.exists()) {
                            moduleNames.add(folderName);
                            modulePaths.add(resource.getLocation().toOSString());
                        }
                    }
                    return true;
                });
            }
        } catch (Exception e) {
            // 忽略错误，至少会有根项目可选
        }
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
		newShell.setText("Generate files");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        container.setLayout(layout);
        
        // 创建SashForm用于分割输入区域和预览区域
        SashForm sashForm = new SashForm(container, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        // 上半部分：输入区域
        Composite inputArea = new Composite(sashForm, SWT.NONE);
        GridLayout inputLayout = new GridLayout(3, false);
        inputArea.setLayout(inputLayout);
        
        // 模块选择
        Label moduleLabel = new Label(inputArea, SWT.NONE);
		moduleLabel.setText("target project");
        moduleLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        moduleCombo = new Combo(inputArea, SWT.READ_ONLY);
        GridData comboGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        comboGd.horizontalSpan = 2;
        moduleCombo.setLayoutData(comboGd);
        
        for (String name : moduleNames) {
            moduleCombo.add(name);
        }
        if (moduleCombo.getItemCount() > 0) {
            moduleCombo.select(0);
        }
        
        // 文本输入区域
        Label textLabel = new Label(inputArea, SWT.NONE);
		textLabel.setText("Contains the text of the file definition:");
        textLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        // 添加从剪贴板粘贴按钮
        Button pasteButton = new Button(inputArea, SWT.PUSH);
		pasteButton.setText("Paste from clipboard");
        pasteButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        
        Button clearButton = new Button(inputArea, SWT.PUSH);
		clearButton.setText("Clear");
        clearButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        
        textInput = new Text(inputArea, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData textGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        textGd.horizontalSpan = 3;
        textGd.heightHint = 200;
        textInput.setLayoutData(textGd);
        
        // 预览按钮
        previewButton = new Button(inputArea, SWT.PUSH);
		previewButton.setText("preview files");
        GridData previewBtnGd = new GridData(SWT.END, SWT.CENTER, false, false);
        previewBtnGd.horizontalSpan = 3;
        previewButton.setLayoutData(previewBtnGd);
        
        // 下半部分：预览区域
        Composite previewArea = new Composite(sashForm, SWT.NONE);
        GridLayout previewLayout = new GridLayout(1, false);
        previewArea.setLayout(previewLayout);
        
        // 文件列表标签
        Label filesLabel = new Label(previewArea, SWT.NONE);
		filesLabel.setText("Files to be generated");
        filesLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        // 文件列表
        fileListViewer = new ListViewer(previewArea, SWT.BORDER | SWT.V_SCROLL);
        GridData listGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        listGd.heightHint = 100;
        fileListViewer.getList().setLayoutData(listGd);
        
        fileListViewer.setContentProvider(ArrayContentProvider.getInstance());
        fileListViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return ((FileModel) element).getFilePath();
            }
        });
        
        // 文件内容预览标签
        Label contentLabel = new Label(previewArea, SWT.NONE);
		contentLabel.setText("File Contents:");
        contentLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        // 文件内容预览
        contentText = new Text(previewArea, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData contentGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        contentGd.heightHint = 150;
        contentText.setLayoutData(contentGd);
        
        // 设置SashForm的权重
        sashForm.setWeights(new int[] {50, 50});
        
        // 添加事件监听器
        pasteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                pasteFromClipboard();
                updatePreview();
            }
        });
        
        clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                textInput.setText("");
                clearPreview();
            }
        });
        
        previewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePreview();
            }
        });
        
        moduleCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePreview();
            }
        });
        
        fileListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                if (!selection.isEmpty()) {
                    FileModel model = (FileModel) selection.getFirstElement();
                    contentText.setText(model.getContent());
                } else {
                    contentText.setText("");
                }
            }
        });
        
		// 稍后通过Display.asyncExec执行初始化，确保UI已完全创建
		Display.getCurrent().asyncExec(() -> {
			// 尝试从剪贴板自动粘贴并预览
			pasteFromClipboard();
			updatePreview();
		});
        
        return container;
    }

    private void pasteFromClipboard() {
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        try {
            TextTransfer textTransfer = TextTransfer.getInstance();
            String text = (String) clipboard.getContents(textTransfer);
            if (text != null && !text.isEmpty()) {
                textInput.setText(text);
            }
        } finally {
            clipboard.dispose();
        }
    }
    
    private void updatePreview() {
        String text = textInput.getText();
        if (text == null || text.trim().isEmpty()) {
            clearPreview();
            return;
        }
        
        try {
            int index = moduleCombo.getSelectionIndex();
            String modulePath = index >= 0 ? modulePaths.get(index) : project.getLocation().toOSString();
            
            TextParser parser = new TextParser(modulePath);
            fileModels = parser.parseText(text);
            
            fileListViewer.setInput(fileModels);
            
            if (!fileModels.isEmpty()) {
                fileListViewer.getList().select(0);
                contentText.setText(fileModels.get(0).getContent());

				// 安全地设置按钮状态
				updateButtonState(true);
            } else {
                contentText.setText("");

				// 安全地设置按钮状态
				updateButtonState(false);
            }
            
            log.log(new Status(Status.INFO, "com.filegenerator", 
                  "预览更新，检测到 " + fileModels.size() + " 个文件"));
        } catch (Exception e) {
            log.log(new Status(Status.ERROR, "com.filegenerator", "更新预览时出错", e));
            clearPreview();
        }
    }
    
    private void clearPreview() {
        fileModels.clear();
        fileListViewer.setInput(fileModels);
        contentText.setText("");

		// 安全地设置按钮状态
		updateButtonState(false);
	}

	// 安全地更新按钮状态的方法
	private void updateButtonState(boolean enabled) {
		if (buttonsCreated) {
			Button okButton = getButton(IDialogConstants.OK_ID);
			if (okButton != null && !okButton.isDisposed()) {
				okButton.setEnabled(enabled);
			}
		}
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Generate files", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		buttonsCreated = true; // 标记按钮已创建

		// 根据当前文件模型状态设置"生成文件"按钮状态
		updateButtonState(!fileModels.isEmpty());
    }

    @Override
    protected Point getInitialSize() {
        return new Point(700, 600);
    }

    @Override
    protected void okPressed() {
        inputText = textInput.getText();
        int index = moduleCombo.getSelectionIndex();
        if (index >= 0 && index < modulePaths.size()) {
            selectedModule = modulePaths.get(index);
        } else {
            selectedModule = project.getLocation().toOSString();
        }
        super.okPressed();
    }

    public String getInputText() {
        return inputText;
    }

    public String getSelectedModulePath() {
        return selectedModule;
    }
    
    public List<FileModel> getFileModels() {
        return fileModels;
    }
}