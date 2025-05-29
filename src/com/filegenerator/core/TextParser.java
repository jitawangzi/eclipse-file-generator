package com.filegenerator.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParser {
	private static final String FILE_PATTERN = "// File: (.*?)\\s*\\n([\\s\\S]*?)(?=// File:|$)";

	public List<FileModel> parseText(String inputText) {
		List<FileModel> fileModels = new ArrayList<>();

		Pattern pattern = Pattern.compile(FILE_PATTERN);
		Matcher matcher = pattern.matcher(inputText);

		while (matcher.find()) {
			String filePath = matcher.group(1).trim();
			String content = matcher.group(2).trim();
			fileModels.add(new FileModel(filePath, content));
		}

		return fileModels;
	}
}