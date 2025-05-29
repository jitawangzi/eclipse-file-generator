package com.filegenerator.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileGenerator {
	private String basePath;

	public FileGenerator(String basePath) {
		this.basePath = basePath;
	}

	public void generateFiles(List<FileModel> fileModels) throws IOException {
		for (FileModel model : fileModels) {
			generateFile(model);
		}
	}

	public void generateFile(FileModel fileModel) throws IOException {
		String fullPath = Paths.get(basePath, fileModel.getFilePath()).toString();
		File file = new File(fullPath);

		// 确保目录存在
		file.getParentFile().mkdirs();

		// 写入文件内容
		Files.write(file.toPath(), fileModel.getContent().getBytes());
	}
}