// ============================================================================

package com.filegenerator.core.patch;

import java.util.ArrayList;
import java.util.List;

public class PatchParser {

    public PatchParseResult parse(String rawText) {
        PatchParseResult result = new PatchParseResult();
        if (rawText == null || rawText.trim().isEmpty()) {
            result.addError("Patch text is empty.");
            return result;
        }

        String text = stripMarkdownCodeFence(rawText);
        String[] lines = text.split("\r?\n");

        PatchFile currentFile = null;
        PatchHunk currentHunk = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.startsWith("diff --git ")) {
                // 开始一个新的文件块，旧块如果存在则加入结果
                if (currentFile != null) {
                    finalizeFile(currentFile, result);
                    currentFile = null;
                    currentHunk = null;
                }
                continue;
            }

            if (line.startsWith("--- ")) {
                // 文件旧路径
                if (currentFile != null) {
                    finalizeFile(currentFile, result);
                    currentHunk = null;
                }
                currentFile = new PatchFile();
                currentHunk = null;

                String oldPath = line.substring(4).trim();
                if (oldPath.startsWith("a/")) {
                    oldPath = oldPath.substring(2);
                }
                currentFile.setOldPath(oldPath);
            } else if (line.startsWith("+++ ")) {
                // 文件新路径
                if (currentFile == null) {
                    // 非法格式
                    result.addError("Found '+++' without preceding '---' at line: " + (i + 1));
                    continue;
                }
                String newPath = line.substring(4).trim();
                if (newPath.startsWith("b/")) {
                    newPath = newPath.substring(2);
                }
                currentFile.setNewPath(newPath);

                determineFileType(currentFile);
            } else if (line.startsWith("@@ ")) {
                // 新的 hunk
                if (currentFile == null) {
                    result.addError("Found hunk header '@@' before file header at line: " + (i + 1));
                    continue;
                }
                currentHunk = parseHunkHeader(line, result, i + 1);
                if (currentHunk != null) {
                    currentFile.addHunk(currentHunk);
                }
            } else if (currentHunk != null && (line.startsWith(" ") || line.startsWith("+") || line.startsWith("-"))) {
                // hunk 中的行
                char prefix = line.charAt(0);
                String content = line.length() > 1 ? line.substring(1) : "";
                PatchLine.Type type;
                if (prefix == ' ') {
                    type = PatchLine.Type.CONTEXT;
                } else if (prefix == '+') {
                    type = PatchLine.Type.ADD;
                } else {
                    type = PatchLine.Type.REMOVE;
                }
                currentHunk.addLine(new PatchLine(type, content));
            } else {
                // 普通文本 / 空行 / 其他
                // 对解析 hunk 不致命，一般可忽略。
            }
        }

        if (currentFile != null) {
            finalizeFile(currentFile, result);
        }

        return result;
    }

    private String stripMarkdownCodeFence(String txt) {
        String trimmed = txt.trim();
        if (trimmed.startsWith("```")) {
            // 去除首尾 ``` 块即可，不做复杂语言识别
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private void determineFileType(PatchFile file) {
        String oldPath = file.getOldPath();
        String newPath = file.getNewPath();
        boolean oldNull = "/dev/null".equals(oldPath);
        boolean newNull = "/dev/null".equals(newPath);

        PatchFileType type;
        if (oldNull && !newNull) {
            type = PatchFileType.ADD;
        } else if (!oldNull && newNull) {
            type = PatchFileType.DELETE;
        } else {
            type = PatchFileType.MODIFY;
        }
        file.setType(type);
    }

    private PatchHunk parseHunkHeader(String line, PatchParseResult result, int lineNumber) {
        // 格式: @@ -oldStart,oldCount +newStart,newCount @@ optional
        // 我们简单解析：找到第二个 '@@'，中间部分拆分
        int firstAt = line.indexOf("@@");
        int secondAt = line.indexOf("@@", firstAt + 2);
        if (firstAt < 0 || secondAt < 0) {
            result.addError("Invalid hunk header format at line: " + lineNumber + " -> " + line);
            return null;
        }
        String header = line.substring(firstAt + 2, secondAt).trim();
        String[] parts = header.split(" ");
        if (parts.length < 2) {
            result.addError("Invalid hunk header range at line: " + lineNumber + " -> " + line);
            return null;
        }

        String oldRange = parts[0]; // -oldStart,oldCount
        String newRange = parts[1]; // +newStart,newCount

        PatchHunk hunk = new PatchHunk();
        parseRange(oldRange, true, hunk, result, lineNumber);
        parseRange(newRange, false, hunk, result, lineNumber);

        return hunk;
    }

    private void parseRange(String range, boolean isOld, PatchHunk hunk, PatchParseResult result, int lineNumber) {
        // 形如 -5,10 或 +3,7 或 -5 或 +3
        range = range.trim();
        if (range.isEmpty()) {
            return;
        }
        char sign = range.charAt(0);
        if (sign != '-' && sign != '+') {
            result.addError("Invalid range sign in hunk header at line: " + lineNumber + " -> " + range);
            return;
        }
        String rest = range.substring(1);
        int start;
        int count = 1;
        if (rest.contains(",")) {
            String[] arr = rest.split(",");
            try {
                start = Integer.parseInt(arr[0]);
                count = Integer.parseInt(arr[1]);
            } catch (NumberFormatException e) {
                result.addError("Invalid range number in hunk header at line: " + lineNumber + " -> " + range);
                return;
            }
        } else {
            try {
                start = Integer.parseInt(rest);
                count = 1;
            } catch (NumberFormatException e) {
                result.addError("Invalid range number in hunk header at line: " + lineNumber + " -> " + range);
                return;
            }
        }
        if (isOld) {
            hunk.setOldStart(start);
            hunk.setOldCount(count);
        } else {
            hunk.setNewStart(start);
            hunk.setNewCount(count);
        }
    }

    private void finalizeFile(PatchFile file, PatchParseResult result) {
        // 如果没有 hunks，也许是空文件或不支持的格式，可发 warning
        if (file.getHunks().isEmpty()
                && file.getType() == PatchFileType.MODIFY) {
            result.addWarning("No hunks found for file: " + file.getEffectivePath());
        }
        result.addFile(file);
    }
}


// ============================================================================
