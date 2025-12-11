// ============================================================================

package com.filegenerator.core.patch;

import java.util.ArrayList;
import java.util.List;

public class PatchFile {

    private String oldPath;
    private String newPath;
    private PatchFileType type;
    private final List<PatchHunk> hunks = new ArrayList<>();

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public PatchFileType getType() {
        return type;
    }

    public void setType(PatchFileType type) {
        this.type = type;
    }

    public List<PatchHunk> getHunks() {
        return hunks;
    }

    public void addHunk(PatchHunk hunk) {
        hunks.add(hunk);
    }

    /**
     * 获取应用到本地文件系统时使用的“目标路径”：
     * - ADD: 使用 newPath
     * - DELETE: 使用 oldPath
     * - MODIFY: 通常 newPath 与 oldPath 等价（除前缀）
     */
    public String getEffectivePath() {
        if (type == PatchFileType.ADD) {
            return newPath;
        } else if (type == PatchFileType.DELETE) {
            return oldPath;
        } else {
            return (newPath != null && !"/dev/null".equals(newPath)) ? newPath : oldPath;
        }
    }
}


// ============================================================================
