package com.filegenerator.ui;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.filegenerator.core.FileModel;

public class PreviewDialog extends Dialog {
	private List<FileModel> fileModels;
	private String basePath;
	private ListViewer fileListViewer;
	private Text contentText;

	public PreviewDialog(Shell parentShell, List<FileModel> fileModels, String basePath) {
		super(parentShell);
		this.fileModels = fileModels;
		this.basePath = basePath;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("预览文件");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		container.setLayout(layout);

		// 基础路径信息
		Label basePathLabel = new Label(container, SWT.NONE);
		basePathLabel.setText("基础路径:");
		GridData lblGd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		basePathLabel.setLayoutData(lblGd);

		Label basePathValue = new Label(container, SWT.NONE);
		basePathValue.setText(basePath);
		basePathValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// 文件列表
		Label filesLabel = new Label(container, SWT.NONE);
		filesLabel.setText("将要生成的文件:");
		GridData filesLblGd = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		filesLblGd.horizontalSpan = 2;
		filesLabel.setLayoutData(filesLblGd);

		fileListViewer = new ListViewer(container, SWT.BORDER | SWT.V_SCROLL);
		GridData listGd = new GridData(SWT.FILL, SWT.FILL, true, true);
		listGd.horizontalSpan = 2;
		listGd.heightHint = 100;
		fileListViewer.getList().setLayoutData(listGd);

		fileListViewer.setContentProvider(ArrayContentProvider.getInstance());
		fileListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((FileModel) element).getFilePath();
			}
		});
		fileListViewer.setInput(fileModels);

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

		// 文件内容预览
		Label contentLabel = new Label(container, SWT.NONE);
		contentLabel.setText("文件内容:");
		GridData contentLblGd = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		contentLblGd.horizontalSpan = 2;
		contentLabel.setLayoutData(contentLblGd);

		contentText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
		GridData contentGd = new GridData(SWT.FILL, SWT.FILL, true, true);
		contentGd.horizontalSpan = 2;
		contentGd.heightHint = 200;
		contentText.setLayoutData(contentGd);

		// 默认选择第一个文件
		if (!fileModels.isEmpty()) {
			fileListViewer.getList().select(0);
			contentText.setText(fileModels.get(0).getContent());
		}

		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "生成文件", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 500);
	}
}