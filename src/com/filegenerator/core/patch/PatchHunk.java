// ============================================================================

package com.filegenerator.core.patch;

import java.util.ArrayList;
import java.util.List;

public class PatchHunk {

    private int oldStart;
    private int oldCount;
    private int newStart;
    private int newCount;
    private final List<PatchLine> lines = new ArrayList<>();

    public int getOldStart() {
        return oldStart;
    }

    public void setOldStart(int oldStart) {
        this.oldStart = oldStart;
    }

    public int getOldCount() {
        return oldCount;
    }

    public void setOldCount(int oldCount) {
        this.oldCount = oldCount;
    }

    public int getNewStart() {
        return newStart;
    }

    public void setNewStart(int newStart) {
        this.newStart = newStart;
    }

    public int getNewCount() {
        return newCount;
    }

    public void setNewCount(int newCount) {
        this.newCount = newCount;
    }

    public List<PatchLine> getLines() {
        return lines;
    }

    public void addLine(PatchLine line) {
        lines.add(line);
    }
}


// ============================================================================
