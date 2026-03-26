# 项目概述

SunnySide 是一个基于 Spring AI Alibaba 的后端学习项目，聚焦养老场景，覆盖老人端与家属端的智能问答与业务服务。  
项目目标是在真实业务语境下实践 AI 应用开发能力，包括多轮对话、RAG 检索、工具调用、工作流编排与登录态上下文注入等能力。

## 已实现功能

- 对话与记忆：基于 `ChatMemory` 支持多轮上下文记忆
- RAG 检索增强：启动时加载本地知识文件到 Qdrant，问答时进行向量检索
- 工具调用：支持菜单、活动、公告、值班、来访预约、健康概览等业务查询
- 流式输出：支持 SSE 实时返回模型响应
- 多模态输入：支持 `image/*`、`audio/*`、`video/*` 文件上传后参与对话
- 家属登录态：支持 JWT 登录，并自动注入家属身份上下文
- 工作流编排：基于 Spring AI Alibaba Workflow 支持单轮对话执行多任务

## 项目结构

```plaintext
src/main/java/cn/lc/sunnyside
├── AITool      # AI 可调用工具（老人端/家属端）
├── Auth        # JWT 解析与登录态上下文
├── Config      # AI、RAG、Web 配置
├── Controller  # HTTP/SSE 接口层
├── POJO        # DO/DTO 数据模型
├── Service     # 业务接口与实现
├── mapper      # MyBatis Mapper 接口
└── SunnySideApplication.java

src/main/resources
├── application.yml
├── mapper/     # MyBatis XML
├── prompts/    # 系统提示词
└── rag/        # RAG 知识源文件
```

## RAG 说明

- 知识源文件：`src/main/resources/rag/ragKonloage.txt`
- 增量写入策略：启动时计算知识文件哈希，若未变化则跳过重复写入
- 哈希记录位置：`target/rag/ragKonloage.sha256`

## 文件分析

- `src/main/java/cn/lc/sunnyside/SunnySideApplication.java`  
  项目启动入口，负责 Spring Boot 应用初始化
- `src/main/resources/application.yml`  
  核心配置文件，包含 AI、数据库、向量库与服务端口等配置
- `src/main/java/cn/lc/sunnyside/Controller/AIController.java`  
  AI 对话接口入口，承载普通问答、流式输出、多模态输入等能力
- `src/main/java/cn/lc/sunnyside/Controller/AuthController.java`  
  认证相关接口入口，处理家属端登录等鉴权请求
- `src/main/java/cn/lc/sunnyside/AITool/ElderTool.java`  
  老人端工具集，封装可被模型调用的养老业务能力
- `src/main/java/cn/lc/sunnyside/AITool/RelativesTool.java`  
  家属端工具集，提供家属侧查询与交互能力
- `src/main/java/cn/lc/sunnyside/AITool/ElderIdentityHelper.java`  
  老人身份辅助处理，支撑工具调用中的身份识别与上下文补充
- `src/main/java/cn/lc/sunnyside/Auth/FamilyJwtInterceptor.java`  
  JWT 拦截器，负责请求鉴权与家属身份注入
- `src/main/java/cn/lc/sunnyside/Auth/FamilyLoginContext.java`  
  登录态上下文容器，提供当前家属身份信息读取能力
- `src/main/java/cn/lc/sunnyside/Config/RAGConfig.java`  
  RAG 相关配置，负责向量检索与知识加载的关键装配
- `src/main/java/cn/lc/sunnyside/Workflow/SunnySideWorkflowService.java`  
  工作流主服务，组织多节点任务执行并统一输出结果
- `src/main/resources/rag/ragKonloage.txt`  
  RAG 知识源主文件，作为向量化检索输入内容
