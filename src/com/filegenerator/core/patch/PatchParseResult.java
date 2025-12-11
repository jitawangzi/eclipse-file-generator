// ============================================================================

package com.filegenerator.core.patch;

import java.util.ArrayList;
import java.util.List;

public class PatchParseResult {

    private final List<PatchFile> files = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public List<PatchFile> getFiles() {
        return files;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addFile(PatchFile file) {
        files.add(file);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public void addError(String error) {
        errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}


// ============================================================================
