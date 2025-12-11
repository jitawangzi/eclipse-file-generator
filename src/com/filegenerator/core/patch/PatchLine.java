// ============================================================================

package com.filegenerator.core.patch;

public class PatchLine {

    public enum Type {
        CONTEXT, // ' ' 前缀
        ADD,     // '+' 前缀
        REMOVE   // '-' 前缀
    }

    private final Type type;
    private final String text; // 不含前缀

    public PatchLine(Type type, String text) {
        this.type = type;
        this.text = text;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}


// ============================================================================
