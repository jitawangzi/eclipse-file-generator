package com.filegenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.filegenerator.core.FileGenerator;
import com.filegenerator.core.FileModel;
import com.filegenerator.core.TextParser;

/**
 * 命令行应用程序入口点
 * 用法：java -jar filegenerator.jar <输入文件> <基础路径>
 */
public class CommandLineApp {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法：java -jar filegenerator.jar <输入文件> [<基础路径>]");
            System.exit(1);
            return;
        }
        
        String inputFile = args[0];
        String basePath = args.length > 1 ? args[1] : new File("").getAbsolutePath();
        
        try {
            // 读取输入文件
            String inputText = new String(Files.readAllBytes(Paths.get(inputFile)));
            
            // 解析文本
            TextParser parser = new TextParser(basePath);
            List<FileModel> fileModels = parser.parseText(inputText);
            
            if (fileModels.isEmpty()) {
                System.out.println("在输入文本中未检测到文件定义");
                System.exit(1);
                return;
            }
            
            // 生成文件
            FileGenerator generator = new FileGenerator(basePath);
            generator.generateFiles(fileModels);
            
            System.out.println("成功生成 " + fileModels.size() + " 个文件:");
            for (FileModel model : fileModels) {
                System.out.println("- " + model.getFilePath());
            }
        } catch (IOException e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
