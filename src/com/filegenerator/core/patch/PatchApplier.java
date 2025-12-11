// ============================================================================

package com.filegenerator.core.patch;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import com.filegenerator.core.BackupManager;

public class PatchApplier {

    public static class ApplyContentResult {
        private final boolean success;
        private final String newContent;
        private final String error;

        public ApplyContentResult(boolean success, String newContent, String error) {
            this.success = success;
            this.newContent = newContent;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getNewContent() {
            return newContent;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * 应用于已有文件内容（MODIFY/DELETE 的内容校验）。
     */
    public ApplyContentResult applyToExistingContent(String originalContent, PatchFile file) {
        if (file.getType() == PatchFileType.MODIFY) {
            return applyModify(originalContent, file);
        } else if (file.getType() == PatchFileType.DELETE) {
            // 对 DELETE，可以选择验证原内容是否与 hunk 中的 REMOVE/CONTEXT 一致
            // 这里简单做一个“模拟 patch 后生成空或少量内容”的校验；若不匹配则失败。
            ApplyContentResult modifyResult = applyModify(originalContent, file);
            if (!modifyResult.isSuccess()) {
                return modifyResult;
            }
            String newContent = modifyResult.getNewContent();
            // 删除后文件通常应为空；若不为空也允许，由调用方决定是否直接删除。
            return new ApplyContentResult(true, newContent, null);
        } else {
            return new ApplyContentResult(false, null, "applyToExistingContent only supports MODIFY/DELETE");
        }
    }

    /**
     * 纯内存的 MODIFY patch 应用。
     */
    private ApplyContentResult applyModify(String originalContent, PatchFile file) {
        List<String> origLines = splitLines(originalContent);
        List<String> newLines = new ArrayList<>(origLines);

        // 为了简单实现，我们按 hunks 从后往前应用（避免行号变化影响后续位置）
        List<PatchHunk> hunks = file.getHunks();
        for (int i = hunks.size() - 1; i >= 0; i--) {
            PatchHunk hunk = hunks.get(i);
            ApplyContentResult one = applySingleHunk(newLines, hunk);
            if (!one.isSuccess()) {
                return one;
            }
        }
        String newContent = joinLines(newLines);
        return new ApplyContentResult(true, newContent, null);
    }

    private ApplyContentResult applySingleHunk(List<String> lines, PatchHunk hunk) {
        int oldStartIdx = hunk.getOldStart() - 1; // 1-based -> 0-based
        if (oldStartIdx < 0 || oldStartIdx > lines.size()) {
            return new ApplyContentResult(false, null, "Hunk oldStart out of range.");
        }

        // 先验证
        int idx = oldStartIdx;
        for (PatchLine line : hunk.getLines()) {
            if (line.getType() == PatchLine.Type.CONTEXT || line.getType() == PatchLine.Type.REMOVE) {
                if (idx >= lines.size()) {
                    return new ApplyContentResult(false, null, "Hunk does not match original content (index overflow).");
                }
                String orig = lines.get(idx);
                if (!orig.equals(line.getText())) {
                    return new ApplyContentResult(false, null,
                            "Hunk context/remove line does not match original content.\nExpected: "
                                    + line.getText() + "\nActual: " + orig);
                }
                idx++;
            }
        }

        // 再真正应用：先删除，再插入
        // 删除段：从 oldStartIdx 开始，移除所有 REMOVE & CONTEXT 对应的行数
        idx = oldStartIdx;
        List<String> toInsert = new ArrayList<>();
        for (PatchLine line : hunk.getLines()) {
            if (line.getType() == PatchLine.Type.CONTEXT || line.getType() == PatchLine.Type.REMOVE) {
                // REMOVE 和 CONTEXT 在旧文件中都占一行
                if (idx < lines.size()) {
                    lines.remove(idx);
                }
            }
        }

        // 再次从 oldStartIdx 位置，根据 ADD & CONTEXT 构造新内容
        idx = oldStartIdx;
        for (PatchLine line : hunk.getLines()) {
            if (line.getType() == PatchLine.Type.CONTEXT || line.getType() == PatchLine.Type.ADD) {
                lines.add(idx, line.getText());
                idx++;
            }
        }

        return new ApplyContentResult(true, null, null);
    }

    private List<String> splitLines(String content) {
        List<String> list = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return list;
        }
        String[] arr = content.split("\r?\n", -1); // 保留空行和末尾空行
        // 这里我们保留最后一个空字符串行，便于与 diff 对齐
        for (String s : arr) {
            list.add(s);
        }
        return list;
    }

    private String joinLines(List<String> lines) {
        return String.join("\n", lines);
    }

    /**
     * 真正应用到 IFile，包含 ADD / MODIFY / DELETE。
     * 所有写入前会调用 BackupManager.backupFile。
     */
    public boolean applyToFile(IFile file, PatchFile patchFile, BackupManager backupManager, StringBuilder errorOut) {
        try {
            if (patchFile.getType() == PatchFileType.ADD) {
                if (file.exists()) {
                    errorOut.append("Target file already exists for ADD patch: " + file.getFullPath());
                    return false;
                }
                String newContent = buildContentForAdd(patchFile);
                // 备份：此处 file 不存在，跳过
                // 写入新文件
                ByteArrayInputStream in = new ByteArrayInputStream(newContent.getBytes(StandardCharsets.UTF_8));
                file.create(in, true, null);
                return true;
            } else if (patchFile.getType() == PatchFileType.DELETE) {
                if (!file.exists()) {
                    // 文件本来就不存在，可视为已删除
                    return true;
                }
                String originalContent = readFileContent(file);
                ApplyContentResult check = applyToExistingContent(originalContent, patchFile);
                if (!check.isSuccess()) {
                    errorOut.append("DELETE patch does not match original content: ").append(check.getError());
                    return false;
                }
                // 备份
                backupManager.backupFile(file.getLocation().toOSString());
                // 删除
                file.delete(true, null);
                return true;
            } else {
                // MODIFY
                if (!file.exists()) {
                    errorOut.append("Target file does not exist for MODIFY patch: " + file.getFullPath());
                    return false;
                }
                String originalContent = readFileContent(file);
                ApplyContentResult result = applyModify(originalContent, patchFile);
                if (!result.isSuccess()) {
                    errorOut.append(result.getError());
                    return false;
                }
                String newContent = result.getNewContent();
                // 备份
                backupManager.backupFile(file.getLocation().toOSString());
                // 写回
                ByteArrayInputStream in = new ByteArrayInputStream(newContent.getBytes(StandardCharsets.UTF_8));
                if (file.isReadOnly()) {
                    file.getResourceAttributes().setReadOnly(false);
                }
                file.setContents(in, true, false, null);
                return true;
            }
        } catch (Exception e) {
            errorOut.append("Exception while applying patch: ").append(e.getMessage());
            return false;
        }
    }

    private String readFileContent(IFile file) throws CoreException {
        try {
            byte[] bytes = file.getContents().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CoreException(org.eclipse.core.runtime.Status.error("Error reading file: " + e.getMessage(), e));
        }
    }

    /**
     * 对于 ADD 类型，根据 hunks 构造新文件内容。
     * 简化策略：所有 hunk 的 ADD 和 CONTEXT (以新文件为准) 合并。
     */
	public String buildContentForAdd(PatchFile file) {
        List<String> newLines = new ArrayList<>();
        for (PatchHunk hunk : file.getHunks()) {
            for (PatchLine line : hunk.getLines()) {
                if (line.getType() == PatchLine.Type.ADD || line.getType() == PatchLine.Type.CONTEXT) {
                    newLines.add(line.getText());
                }
            }
        }
        return String.join("\n", newLines);
    }
}


// ============================================================================
