package com.filegenerator.ui;

import org.eclipse.jface.viewers.LabelProvider;

public class SessionFilesLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
        if (element instanceof SessionFilesContentProvider.TreeNode) {
            return ((SessionFilesContentProvider.TreeNode) element).name;
        }
        return super.getText(element);
    }
}

