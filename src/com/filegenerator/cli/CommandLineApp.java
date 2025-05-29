package com.filegenerator.cli;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.filegenerator.core.FileGenerator;
import com.filegenerator.core.FileModel;
import com.filegenerator.core.TextParser;

public class CommandLineApp {

	public static void main(String[] args) {
		if (args.length == 0) {
			printUsage();
			System.exit(1);
			return;
		}

		String basePath = args[0];
		String inputText = null;

		// 如果提供了输入文件，从文件读取内容
		if (args.length > 1) {
			String inputFile = args[1];
			try {
				inputText = new String(Files.readAllBytes(Paths.get(inputFile)));
			} catch (IOException e) {
				System.err.println("读取输入文件错误: " + e.getMessage());
				System.exit(1);
				return;
			}
		} else {
			// 否则，尝试从剪贴板读取
			try {
				inputText = getClipboardContents();
				if (inputText == null || inputText.isEmpty()) {
					System.err.println("剪贴板中没有文本");
					System.exit(1);
					return;
				}
			} catch (Exception e) {
				System.err.println("从剪贴板读取错误: " + e.getMessage());
				System.exit(1);
				return;
			}
		}

		try {
			// 解析文本
			TextParser parser = new TextParser();
			List<FileModel> fileModels = parser.parseText(inputText);

			if (fileModels.isEmpty()) {
				System.out.println("在输入文本中未检测到文件定义");
				System.exit(1);
				return;
			}

			// 预览文件
			System.out.println("以下文件将在 " + basePath + " 生成:");
			for (FileModel model : fileModels) {
				System.out.println("- " + model.getFilePath());
			}

			// 生成文件
			FileGenerator generator = new FileGenerator(basePath);
			generator.generateFiles(fileModels);

			System.out.println("成功生成 " + fileModels.size() + " 个文件");

		} catch (IOException e) {
			System.err.println("错误: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static String getClipboardContents() throws Exception {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		return (String) clipboard.getData(DataFlavor.stringFlavor);
	}

	private static void printUsage() {
		System.out.println("使用方法: java -jar filegenerator.jar <基础路径> [输入文件]");
		System.out.println("  基础路径 - 文件将生成的目录");
		System.out.println("  输入文件 - (可选) 包含文件定义的文本文件");
		System.out.println("            如果未提供，将从剪贴板读取文本");
	}
}