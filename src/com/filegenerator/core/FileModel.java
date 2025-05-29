package com.filegenerator.core;

public class FileModel {
	private String filePath;
	private String content;

	public FileModel(String filePath, String content) {
		this.filePath = filePath;
		this.content = content;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getContent() {
		return content;
	}
}