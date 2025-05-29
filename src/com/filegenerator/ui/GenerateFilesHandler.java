package com.filegenerator.ui;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import com.filegenerator.core.FileGenerator;
import com.filegenerator.core.FileModel;
import com.filegenerator.core.TextParser;

public class GenerateFilesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object firstElement = structuredSelection.getFirstElement();

			if (firstElement instanceof IProject) {
				IProject project = (IProject) firstElement;
				showInputDialog(project);
			}
		}
		return null;
	}

	private void showInputDialog(IProject project) {
		InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), project);
		if (dialog.open() == InputDialog.OK) {
			String inputText = dialog.getInputText();
			String modulePath = dialog.getSelectedModulePath();
			generateFiles(project, inputText, modulePath);
		}
	}

	private void generateFiles(IProject project, String inputText, String basePath) {
		try {
			// 解析文本
			TextParser parser = new TextParser();
			List<FileModel> fileModels = parser.parseText(inputText);

			if (fileModels.isEmpty()) {
				showMessage("在输入文本中未检测到文件定义");
				return;
			}

			// 显示预览对话框
			PreviewDialog previewDialog = new PreviewDialog(Display.getCurrent().getActiveShell(), fileModels, basePath);

			if (previewDialog.open() == PreviewDialog.OK) {
				// 生成文件
				FileGenerator generator = new FileGenerator(basePath);
				generator.generateFiles(fileModels);

				// 刷新项目
				project.refreshLocal(IProject.DEPTH_INFINITE, null);

				showMessage("成功生成 " + fileModels.size() + " 个文件");
			}

		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, "com.filegenerator", "生成文件时出错", e);
			StatusManager.getManager().handle(status, StatusManager.SHOW);
		}
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "文件生成器", message);
	}
}