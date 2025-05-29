package com.filegenerator.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.filegenerator.core.BackupManager.BackupEntry;
import com.filegenerator.core.BackupSession;

public class SessionFilesContentProvider implements ITreeContentProvider {
	static class TreeNode {
        String name;
        String path;
        Map<String, TreeNode> children = new HashMap<>();
        TreeNode parent;
        BackupEntry backupEntry;
        
        TreeNode(String name, String path) {
            this.name = name;
            this.path = path;
        }
        
        void addChild(TreeNode child) {
            children.put(child.name, child);
            child.parent = this;
        }
    }
    
    private TreeNode rootNode;
    
    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof BackupSession) {
            BackupSession session = (BackupSession) inputElement;
            buildTree(session);
            return rootNode.children.values().toArray();
        }
        return new Object[0];
    }
    
    private void buildTree(BackupSession session) {
        rootNode = new TreeNode("root", "");
        
        for (BackupEntry entry : session.getBackupEntries()) {
            String filePath = entry.getOriginalFilePath();
            File file = new File(filePath);
            
            // 获取相对路径部分
            String relativePath = file.getPath();
            
			// 修复：使用Pattern.quote来确保File.separator被正确转义
			String[] pathSegments = relativePath.split(Pattern.quote(File.separator));

            TreeNode currentNode = rootNode;
            StringBuilder currentPath = new StringBuilder();
            
            // 构建目录树
            for (int i = 0; i < pathSegments.length - 1; i++) {
                String segment = pathSegments[i];
                if (segment.isEmpty()) continue;
                
				if (currentPath.length() > 0) {
					currentPath.append(File.separator);
				}
				currentPath.append(segment);
                
                if (!currentNode.children.containsKey(segment)) {
                    TreeNode newNode = new TreeNode(segment, currentPath.toString());
                    currentNode.addChild(newNode);
                }
                
                currentNode = currentNode.children.get(segment);
            }
            
            // 添加文件节点
            String fileName = pathSegments[pathSegments.length - 1];
			if (currentPath.length() > 0) {
				currentPath.append(File.separator);
			}
            currentPath.append(fileName);
            
            TreeNode fileNode = new TreeNode(fileName, currentPath.toString());
            fileNode.backupEntry = entry;
            currentNode.addChild(fileNode);
        }
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof TreeNode) {
            return ((TreeNode)parentElement).children.values().toArray();
        }
        return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof TreeNode) {
            return ((TreeNode)element).parent;
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof TreeNode) {
            return !((TreeNode)element).children.isEmpty();
        }
        return false;
    }
    
    @Override
    public void dispose() {
    }

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}