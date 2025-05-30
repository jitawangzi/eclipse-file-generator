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
import org.eclipse.jface.window.Window;
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
            
            // 显示输入对话框（自动从剪贴板获取初始内容）
            Shell shell = HandlerUtil.getActiveShell(event);
            InputDialog inputDialog = new InputDialog(shell, project);
            if (inputDialog.open() != Window.OK) {
                // 用户取消了操作
                return null;
            }
            
            // 获取用户输入的文本和选择的模块路径
            String text = inputDialog.getInputText();
            String modulePath = inputDialog.getSelectedModulePath();
            
            // 如果用户没有输入任何内容，提示错误
            if (text == null || text.trim().isEmpty()) {
                showError(event, "请输入要处理的代码");
                return null;
            }
            
            log.log(new Status(Status.INFO, "com.filegenerator", 
                  "获取到用户输入的文本，长度: " + text.length()));
            
            // 解析文本
            TextParser parser = new TextParser(modulePath);
            List<FileModel> fileModels = parser.parseText(text);
            
            log.log(new Status(Status.INFO, "com.filegenerator", 
                  "解析完成，检测到 " + fileModels.size() + " 个文件"));
            
            if (fileModels.isEmpty()) {
                showError(event, "未找到有效的文件定义，请确保文本中包含类似\"// File: 路径\"的标记");
                return null;
            }
            
            // 显示预览对话框
            PreviewDialog previewDialog = new PreviewDialog(shell, fileModels, modulePath);
            if (previewDialog.open() != Window.OK) {
                // 用户在预览界面取消了操作
                return null;
            }
            
            // 生成文件
            FileGenerator generator = new FileGenerator(modulePath);
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
    
    private void showError(ExecutionEvent event, String message) {
        Shell shell = HandlerUtil.getActiveShell(event);
        MessageDialog.openError(shell, "错误", message);
    }
    
    private void showInfo(ExecutionEvent event, String message) {
        Shell shell = HandlerUtil.getActiveShell(event);
        MessageDialog.openInformation(shell, "信息", message);
    }
}
