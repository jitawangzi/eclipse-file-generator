package com.filegenerator.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
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

public class InputDialog extends Dialog {
	private Text textInput;
	private Combo moduleCombo;
	private String inputText = "";
	private String selectedModule = "";
	private IProject project;
	private List<String> moduleNames = new ArrayList<>();
	private List<String> modulePaths = new ArrayList<>();

	public InputDialog(Shell parentShell, IProject project) {
		super(parentShell);
		this.project = project;
		detectModules();
	}

	private void detectModules() {
		// 添加主项目
		moduleNames.add(project.getName() + " (主项目)");
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
		newShell.setText("生成文件");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		container.setLayout(layout);

		// 模块选择
		Label moduleLabel = new Label(container, SWT.NONE);
		moduleLabel.setText("目标模块:");
		moduleLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		moduleCombo = new Combo(container, SWT.READ_ONLY);
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
		Label textLabel = new Label(container, SWT.NONE);
		textLabel.setText("包含文件定义的文本:");
		textLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		// 添加从剪贴板粘贴按钮
		Button pasteButton = new Button(container, SWT.PUSH);
		pasteButton.setText("从剪贴板粘贴");
		pasteButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		pasteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pasteFromClipboard();
			}
		});

		Button clearButton = new Button(container, SWT.PUSH);
		clearButton.setText("清空");
		clearButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		clearButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				textInput.setText("");
			}
		});

		textInput = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData textGd = new GridData(SWT.FILL, SWT.FILL, true, true);
		textGd.horizontalSpan = 3;
		textGd.heightHint = 300;
		textGd.widthHint = 500;
		textInput.setLayoutData(textGd);

		// 尝试从剪贴板自动粘贴
		pasteFromClipboard();

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

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "下一步", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(550, 400);
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
}