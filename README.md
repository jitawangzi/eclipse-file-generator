# EclipseFileGenerator

A powerful Eclipse plugin that generates files from text templates using "// File:" markers. Perfectly integrates with LLMs (Large Language Models) to transform AI-generated code into actual project files.

一款功能强大的Eclipse插件，通过使用"// File:"标记从文本模板生成文件。完美集成大语言模型(LLM)，将AI生成的代码转换为实际项目文件。

[English](#english) | [中文](#chinese)

---

<a name="english"></a>
## English

### Overview

EclipseFileGenerator is an Eclipse plugin designed to simplify file generation from text templates. It allows developers to quickly create multiple files from a single text input, making it ideal for scaffolding, boilerplate code generation, and template-based development. The plugin shines especially when paired with LLMs like ChatGPT, Claude, or GitHub Copilot, providing a seamless bridge between AI-generated code and your Eclipse workspace.

### Key Features

- **LLM Integration**: Perfectly accepts output from AI models for rapid implementation
- **Easy File Generation**: Create multiple files from a single text template
- **Smart Path Detection**: Automatically adjusts file paths based on project structure (Maven, Gradle, Eclipse plugin, etc.)
- **Clipboard Integration**: Directly paste content from clipboard
- **File Preview**: Preview files before generation
- **Backup System**: Automatically backs up existing files before overwriting
- **Restore Capability**: Restore files from backup if needed

### Installation

1. **Manual Installation**:
   - Download the latest `.jar` file from the [Releases](https://github.com/jitawangzi/EclipseFileGenerator/releases) page
   - Place it in your Eclipse's `dropins` folder
   - Restart Eclipse


### Working with LLMs

EclipseFileGenerator is designed to work seamlessly with Large Language Models to accelerate your development process:

1. **Ask your LLM to generate code with file markers**:

Generate a Java Spring Boot controller and service for user management with endpoints for
creating, updating, retrieving, and deleting users. Include proper file paths with
'// File:' markers at the beginning of each file.

sql_more


2. **Copy the LLM response** (with file markers)

3. **Generate in Eclipse**: Right-click your project, select "Batch Generate Files", and the code will be properly distributed across multiple files

This workflow dramatically reduces the time needed to implement features and ensures structural consistency.

### Usage Guide

1. **Preparing Your Template**:
- Create a text template with file markers (or ask an LLM to generate one)
- Use `// File: path/to/file.ext` to mark the beginning of each file

Example:
```
// File: src/main/java/com/example/model/User.java
package com.example.model;

public class User {
private String username;
private String email;

// Getters and setters
   public String getUsername() {
       return username;
   }
   
   public void setUsername(String username) {
       this.username = username;
   }
   
   public String getEmail() {
       return email;
   }
   
   public void setEmail(String email) {
       this.email = email;
   }

   }

// File: src/main/java/com/example/service/UserService.java
package com.example.service;

import com.example.model.User;

public class UserService {
public User findById(Long id) {
// Implementation
return null;
}
}
```

2. **Generating Files**:
- Copy your template to clipboard
- Right-click on your project in Eclipse
- Select "Batch Generate Files"
- The plugin will automatically detect files and display them for preview
- Click "Generate Files" to create all files

3. **Restoring Files**:
- Right-click on a project
- Select "Batch Restore Files"
- Choose a backup version to restore

### LLM Prompt Examples

Here are some useful LLM tips when used with EclipseFileGenerator to format LLM output for easier copying and pasting:

**Please give the full contents of all files. For newly added files, give the full contents; for modified files, give the full contents of the modified files without omitting any of the original parts. Please use the following format for output: // File: File path File content... The file path can be a full path or a path relative to the project root. Please combine all file contents into one continuous code block, using the above file delimiter comments to distinguish between different files. This way I can copy everything at once without including Markdown formatting indicators or other non-code content.**



### Future Plans


### Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

<a name="chinese"></a>
## 中文

### 概述

EclipseFileGenerator是一款Eclipse插件，旨在简化从文本模板生成文件的过程。它允许开发者从单一文本输入快速创建多个文件，非常适合用于脚手架搭建、样板代码生成和基于模板的开发。该插件与ChatGPT、Claude或GitHub Copilot等大语言模型(LLM)配合使用时特别出色，为AI生成的代码与Eclipse工作空间之间提供了无缝桥接。

### 主要特性

- **LLM集成**：完美接收AI模型输出，实现快速实现
- **轻松生成文件**：从单一文本模板创建多个文件
- **智能路径检测**：根据项目结构（Maven、Gradle、Eclipse插件等）自动调整文件路径
- **剪贴板集成**：直接从剪贴板粘贴内容
- **文件预览**：生成前预览文件
- **备份系统**：在覆盖前自动备份现有文件
- **恢复功能**：需要时可从备份恢复文件

### 安装方法

1. **手动安装**：
- 从[Releases](https://github.com/jitawangzi/EclipseFileGenerator/releases)页面下载最新的`.jar`文件
- 将其放入Eclipse的`dropins`文件夹中
- 重启Eclipse


### 与大语言模型协作

EclipseFileGenerator设计为与大语言模型无缝协作，加速您的开发过程：

1. **要求LLM生成带有文件标记的代码**：


生成一个Java Spring Boot用户管理控制器和服务，包含创建、更新、检索和删除用户的端点。
在每个文件开头包含带有'// File:'标记的正确文件路径。

2. **复制LLM的响应**（包含文件标记）

3. **在Eclipse中生成**：右键点击项目，选择"Batch Generate Files"，代码将被正确分配到多个文件中

这种工作流程大大减少了实现功能所需的时间，并确保了结构一致性。

### 使用指南

1. **准备模板**：
- 创建带有文件标记的文本模板（或要求LLM生成一个）
- 使用`// File: path/to/file.ext`标记每个文件的开始

示例：

```
// File: src/main/java/com/example/model/User.java
package com.example.model;

public class User {
private String username;
private String email;

// Getters and setters
   public String getUsername() {
       return username;
   }
   
   public void setUsername(String username) {
       this.username = username;
   }
   
   public String getEmail() {
       return email;
   }
   
   public void setEmail(String email) {
       this.email = email;
   }

   }

// File: src/main/java/com/example/service/UserService.java
package com.example.service;

import com.example.model.User;

public class UserService {
public User findById(Long id) {
// 实现代码
return null;
}
}
```

2. **生成文件**：
- 将模板复制到剪贴板
- 在Eclipse中右键点击您的项目
- 选择"Batch Generate Files"
- 插件将自动检测文件并显示预览
- 点击"Generate Files"创建所有文件

3. **恢复文件**：
- 右键点击项目
- 选择"Batch Restore Files"
- 选择要恢复的备份版本

### LLM提示示例

以下是与EclipseFileGenerator配合使用时，有效的LLM提示，可以使LLM输出格式化的结果，更适合复制粘贴：


**请给出所有文件的完整内容。对于新增文件，提供全部内容；对于修改的文件，提供修改后的文件完整内容，不要省略任何原有部分。
请使用以下格式输出：
// File: 文件路径
文件内容…
文件路径可以是完整路径，也可以是相对于项目根目录的路径。
请将所有文件内容合并在一个连续的代码块中，使用上述文件分隔注释来区分不同文件。这样我可以一次性复制所有内容，而不会包含Markdown格式指示符或其他非代码内容。**


### 未来计划


### 贡献

欢迎贡献！请随时提交Pull Request。



---

## Tags / 标签

Eclipse, Plugin, Code Generation, File Generation, Template, Java, Development Tool, Productivity, AI Integration, LLM, ChatGPT, Code Automation, 代码生成, 文件生成, 模板, 开发工具, 生产力工具, 人工智能集成, 大语言模型, 代码自动化

---

## License / 许可证

This project is licensed under the MIT License - see the LICENSE file for details.

本项目采用MIT许可证 - 查看LICENSE文件了解详情。


