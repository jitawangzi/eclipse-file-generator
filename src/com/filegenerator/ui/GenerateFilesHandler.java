// File: src/com/filegenerator/ui/GenerateFilesHandler.java
package com.filegenerator.ui;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.filegenerator.core.FileGenerator;
import com.filegenerator.core.FileModel;
import com.filegenerator.core.TextParser;

public class GenerateFilesHandler extends AbstractHandler {
    private final ILog log = Platform.getLog(Platform.getBundle("com.filegenerator"));

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            // 获取当前项目
            IProject project = getSelectedProject(event);
            if (project == null) {
                showError(event, "请先选择一个项目");
                return null;
            }
            
            // 获取项目路径
            String projectPath = project.getLocation().toOSString();
            
			// 只从剪贴板获取文本
			String text = getClipboardText();

			// 如果剪贴板为空，提示错误
            if (text == null || text.trim().isEmpty()) {
				showError(event, "请先复制要处理的代码到剪贴板");
				return null;
            }
            
			log.log(new Status(Status.INFO, "com.filegenerator", "从剪贴板获取到文本，长度: " + text.length()));

            // 解析文本
            TextParser parser = new TextParser(projectPath);
            List<FileModel> fileModels = parser.parseText(text);
            
			log.log(new Status(Status.INFO, "com.filegenerator", "解析完成，检测到 " + fileModels.size() + " 个文件"));

            if (fileModels.isEmpty()) {
				showError(event, "未找到有效的文件定义，请确保剪贴板内容包含类似\"// File: 路径\"的标记");
                return null;
            }
            
            // 显示确认对话框
            if (!showConfirmDialog(event, fileModels)) {
                return null;
            }
            
            // 生成文件
            FileGenerator generator = new FileGenerator(projectPath);
            generator.generateFiles(fileModels);
            
            // 刷新项目
            project.refreshLocal(IProject.DEPTH_INFINITE, null);
            
            // 显示成功消息
            showInfo(event, "成功生成 " + fileModels.size() + " 个文件");
        } catch (Exception e) {
            log.log(new Status(Status.ERROR, "com.filegenerator", "生成文件时出错", e));
            showError(event, "生成文件时出错: " + e.getMessage());
        }
        
        return null;
    }
    
	/**
	 * 从系统剪贴板获取文本
	 */
	private String getClipboardText() {
		Clipboard clipboard = null;
		try {
			clipboard = new Clipboard(Display.getDefault());
			TextTransfer textTransfer = TextTransfer.getInstance();
			String text = (String) clipboard.getContents(textTransfer);

			if (text != null && !text.isEmpty()) {
				return text;
			}
		} catch (Exception e) {
			log.log(new Status(Status.WARNING, "com.filegenerator", "获取剪贴板内容时出错: " + e.getMessage(), e));
		} finally {
			if (clipboard != null) {
				clipboard.dispose();
			}
		}
		return null;
	}

    private IProject getSelectedProject(ExecutionEvent event) {
		try {
			// 从当前选择获取项目
			ISelection selection = HandlerUtil.getCurrentSelection(event);
			if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
				Object firstElement = ((IStructuredSelection) selection).getFirstElement();

				// 直接是项目
				if (firstElement instanceof IProject) {
					return (IProject) firstElement;
				}

				// 是资源，获取其所属项目
				if (firstElement instanceof IResource) {
					return ((IResource) firstElement).getProject();
				}

				// 是可适配的，尝试适配为资源
				if (firstElement instanceof IAdaptable) {
					IResource resource = ((IAdaptable) firstElement).getAdapter(IResource.class);
					if (resource != null) {
						return resource.getProject();
					}
				}
			}

			// 如果从选择中无法获取项目，则尝试从活动编辑器获取
			return getProjectFromActiveEditor();
		} catch (Exception e) {
			log.log(new Status(Status.WARNING, "com.filegenerator", "获取选定项目时出错", e));
			return null;
		}
    }
    
    private IProject getProjectFromActiveEditor() {
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPart part = window.getActivePage().getActivePart();
                if (part instanceof IEditorPart) {
                    IEditorPart editor = (IEditorPart) part;
                    if (editor.getEditorInput() instanceof IFileEditorInput) {
                        return ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
                    }
                }
            }
        } catch (Exception e) {
            log.log(new Status(Status.WARNING, "com.filegenerator", "获取当前项目时出错", e));
        }
        return null;
    }
    
    private boolean showConfirmDialog(ExecutionEvent event, List<FileModel> fileModels) {
        Shell shell = HandlerUtil.getActiveShell(event);
        StringBuilder message = new StringBuilder("将生成以下文件:\n");
        
        for (FileModel model : fileModels) {
            message.append("- ").append(model.getFilePath()).append("\n");
        }
        
        return MessageDialog.openQuestion(shell, "确认生成文件", message.toString());
    }
    
    private void showError(ExecutionEvent event, String message) {
        Shell shell = HandlerUtil.getActiveShell(event);
        MessageDialog.openError(shell, "错误", message);
    }
    
    private void showInfo(ExecutionEvent event, String message) {
        Shell shell = HandlerUtil.getActiveShell(event);
        MessageDialog.openInformation(shell, "信息", message);
    }
}