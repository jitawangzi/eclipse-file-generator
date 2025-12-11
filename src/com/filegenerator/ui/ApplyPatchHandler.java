package com.filegenerator.ui;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.filegenerator.Activator;
import com.filegenerator.core.BackupManager;
import com.filegenerator.core.patch.PatchApplier;
import com.filegenerator.core.patch.PatchFile;
import com.filegenerator.core.patch.PatchFileType;

public class ApplyPatchHandler extends AbstractHandler {
	private final ILog log = Platform.getLog(Platform.getBundle("com.filegenerator"));

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);
        IProject project = getSelectedProject(HandlerUtil.getCurrentSelection(event));

        if (project == null) {
            MessageDialog.openInformation(shell, "Apply LLM Patch", "Please select a project first.");
            return null;
        }

        PatchInputDialog dialog = new PatchInputDialog(shell, project);
        if (dialog.open() != PatchInputDialog.OK) {
            return null;
        }

        List<PatchFile> filesToApply = dialog.getSelectedPatchFiles();
        if (filesToApply.isEmpty()) {
            return null;
        }

        boolean hasDelete = filesToApply.stream().anyMatch(f -> f.getType() == PatchFileType.DELETE);
        if (hasDelete) {
            boolean confirm = MessageDialog.openQuestion(shell, "Confirm Deletion",
                    "This patch will delete one or more files.\n\nDo you want to continue?");
            if (!confirm) {
                return null;
            }
        }

        BackupManager backupManager = dialog.getBackupManager();
        if (dialog.isBackupEnabled()) {
            backupManager.startBackupSession();
        }

        PatchApplier applier = new PatchApplier();
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (PatchFile pf : filesToApply) {
            String relPath = normalizeEffectivePath(pf);
            if (relPath == null) {
                failCount++;
                errorMessages.append("Invalid path in patch: ").append(pf.getEffectivePath()).append("\n");
                continue;
            }
            IFile file = project.getFile(relPath);
            StringBuilder err = new StringBuilder();
            boolean ok = applier.applyToFile(file, pf, dialog.isBackupEnabled() ? backupManager : new BackupManager() {
                @Override
				public String backupFile(String path) {
                    // 如果不启用备份，这里就是空实现
					return "";
                }
            }, err);
            if (ok) {
                successCount++;
            } else {
                failCount++;
                errorMessages.append("File: ").append(relPath).append(" -> ").append(err.toString()).append("\n");
            }
        }

        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
			log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "Error refreshing project after applying patch.", e));
        }

        if (dialog.isBackupEnabled()) {
            backupManager.endBackupSession();
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Patch application finished.\n\n");
        msg.append("Succeeded: ").append(successCount).append("\n");
        msg.append("Failed: ").append(failCount).append("\n");
        if (failCount > 0) {
            msg.append("\nErrors:\n").append(errorMessages);
        }

        MessageDialog.openInformation(shell, "Apply LLM Patch", msg.toString());

        return null;
    }

    private IProject getSelectedProject(Object selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection) selection;
            Object element = ss.getFirstElement();
            if (element instanceof IProject) {
                return (IProject) element;
            }
            if (element instanceof IAdaptable) {
                IResource res = (IResource) ((IAdaptable) element).getAdapter(IResource.class);
                if (res != null) {
                    return res.getProject();
                }
            }
        }
        return null;
    }

    private String normalizeEffectivePath(PatchFile pf) {
        String p = pf.getEffectivePath();
        if (p == null) {
            return null;
        }
        if ("/dev/null".equals(p)) {
            return null;
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }
}
