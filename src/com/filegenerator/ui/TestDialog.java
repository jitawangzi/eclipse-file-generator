package com.filegenerator.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class TestDialog extends Dialog {
	private String message;
	private String result;

	public TestDialog(Shell parent) {
		this(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	public TestDialog(Shell parent, int style) {
		super(parent, style);
		setText("测试对话框");
		setMessage("这是一个测试对话框");
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String open() {
		Shell parent = getParent();
		Shell shell = new Shell(parent, getStyle());
		shell.setText(getText());
		createContents(shell);
		shell.pack();
		shell.open();

		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	private void createContents(final Shell shell) {
		shell.setLayout(new GridLayout(2, true));

		// 显示消息
		Label label = new Label(shell, SWT.NONE);
		label.setText(message);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		// 创建"确定"按钮
		Button ok = new Button(shell, SWT.PUSH);
		ok.setText("确定");
		ok.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ok.addListener(SWT.Selection, event -> {
			result = "OK";
			shell.close();
		});

		// 创建"取消"按钮
		Button cancel = new Button(shell, SWT.PUSH);
		cancel.setText("取消");
		cancel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		cancel.addListener(SWT.Selection, event -> {
			result = "Cancel";
			shell.close();
		});

		shell.setDefaultButton(ok);
	}
}