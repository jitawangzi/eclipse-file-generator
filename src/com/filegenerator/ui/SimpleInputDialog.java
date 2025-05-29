package com.filegenerator.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SimpleInputDialog extends Dialog {
	private Text inputText;
	private String userInput = "";
	private String modulePath = "";
	private IProject project;

	public SimpleInputDialog(Shell parentShell, IProject project) {
		super(parentShell);
		this.project = project;
		// 设置默认模块路径为项目根路径
		this.modulePath = project.getLocation().toOSString();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("文件生成器");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));

		Label label = new Label(container, SWT.NONE);
		label.setText("请输入要解析的文本:");

		inputText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 300;
		gridData.widthHint = 500;
		inputText.setLayoutData(gridData);

		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "生成", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "取消", false);
	}

	@Override
	protected void okPressed() {
		userInput = inputText.getText();
		super.okPressed();
	}

	public String getInputText() {
		return userInput;
	}

	public String getSelectedModulePath() {
		return modulePath;
	}
}