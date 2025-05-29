package com.filegenerator.ui;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import com.filegenerator.core.FileGenerator;
import com.filegenerator.core.FileModel;
import com.filegenerator.core.TextParser;

public class GenerateFilesHandler extends AbstractHandler {
	final ILog log = Platform.getLog(Platform.getBundle("com.filegenerator"));

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// 使用 UI 线程安全的方式执行
		Display.getDefault().asyncExec(() -> {
			try {
				ISelection selection = HandlerUtil.getCurrentSelection(event);
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection structuredSelection = (IStructuredSelection) selection;

					Object firstElement = structuredSelection.getFirstElement();
					IProject project = null;

					if (firstElement instanceof IProject) {
						project = (IProject) firstElement;
					} else if (firstElement instanceof IJavaProject) {
						project = ((IJavaProject) firstElement).getProject();
					} else if (firstElement instanceof IAdaptable) {
						project = ((IAdaptable) firstElement).getAdapter(IProject.class);
					}
					if (project != null) {
						showInputDialog(project, getParentShell(event));
					} else {
						log.log(new Status(Status.ERROR, "com.filegenerator", "不是项目类型 " + firstElement.getClass().getSimpleName()));
					}
				} else {
					log.log(new Status(Status.ERROR, "com.filegenerator", "what selection？ " + selection.getClass().getSimpleName()));

				}
			} catch (Exception e) {
				e.printStackTrace();
				log.log(new Status(Status.ERROR, "com.filegenerator", "what the fuck " + e.getMessage()));
			}
		});
		return null;
	}

	private void showInputDialog(IProject project, Shell parentShell) {
		log.log(new Status(Status.INFO, "com.filegenerator", "showInputDialog : " + Thread.currentThread().getName()));

		// 如果 parentShell 为 null，使用备用方案
		if (parentShell == null) {
			parentShell = getParentShell();
		}
		InputDialog dialog = new InputDialog(parentShell, project);
		if (dialog.open() == InputDialog.OK) {
			String inputText = dialog.getInputText();
			String modulePath = dialog.getSelectedModulePath();
			generateFiles(project, inputText, modulePath);
		}
	}

	private Shell getParentShell(ExecutionEvent event) {
		Shell shell = HandlerUtil.getActiveShell(event);
		if (shell != null)
			return shell;
		if (Display.getDefault().getActiveShell() != null)
			return Display.getDefault().getActiveShell();
		if (PlatformUI.isWorkbenchRunning()) {
			if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null)
				return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		}
		return new Shell(Display.getDefault());
	}

	private Shell getParentShell() {
		// 方案1：尝试通过 Display 获取
		Shell shell = Display.getDefault().getActiveShell();
		if (shell != null)
			return shell;

		// 方案2：如果 Workbench 已启动，获取其 Shell
		if (PlatformUI.isWorkbenchRunning()) {
			return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		}

		// 方案3：最后尝试新建一个 Shell
		return new Shell(Display.getDefault());
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
			PreviewDialog previewDialog = new PreviewDialog(Display.getDefault().getActiveShell(), fileModels, basePath);
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
		MessageDialog.openInformation(Display.getDefault().getActiveShell(), "文件生成器", message);
	}
}