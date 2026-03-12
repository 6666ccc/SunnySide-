# SunnySide - 智能养老助手

>  本项目基于 **Spring AI Alibaba** 构建，集成 **LLM (大语言模型)**、**RAG (检索增强生成)** 以及 **Function Calling (函数调用)** 

---

##  核心功能 (Features)

###  已实现功能
- ** 智能对话与记忆 (Memory)**
  - 集成 Spring AI Alibaba，支持多轮对话。
  - 具备上下文记忆能力，能够记住用户的身份与过往对话内容。

- ** 流式传输 (Streaming)**
  - 支持 Server-Sent Events (SSE)，提供类似 ChatGPT 的打字机实时响应体验，降低用户等待焦虑。

- ** RAG 知识检索 (Retrieval-Augmented Generation)**
  - 基于 **Qdrant** 向量数据库构建本地知识库。
  - 能够根据用户问题自动检索相关的养老规定、护理知识，增强回答的准确性与专业度。

- ** 函数调用 (Function Calling)**
  - 智能集成业务系统，AI 可自主调用工具完成特定任务：
    - **信息查询**：查询时间、老人位置、今日食谱、通知公告。
    - **业务办理**：活动报名/取消、医护值班查询、访客预约管理。

###  待开发功能 (Roadmap)
- [ ] **多模态交互 (Multimodal)**：
  -  **图片理解与生成**：识别上传的图片内容（如药品说明书、体检单）。
  -  **语音识别与输出 (ASR/TTS)**：支持语音对话，方便视力不佳的老人使用。
  -  **视频推理**：基于视频流的行为分析（如跌倒检测）。

---

##  项目结构 (Project Structure)

```plaintext
src/main/java/cn/lc/sunnyside
├── AIConfig --- AI 模型配置与 ChatMemory 记忆管理
├── AITool --- Function Calling 工具集 (如 DailyTool)
├── Controller --- 控制器层，处理 HTTP/SSE 请求
├── POJO --- 数据模型层 (DO: 数据库实体 / DTO: 数据传输对象)
├── RAG --- RAG 模块，负责知识库的加载、分割与向量化
├── Service --- 服务层，实现核心业务逻辑
├── WebConfig --- Web 配置层 (跨域设置)
├── mapper --- 数据访问层 (MyBatis Mapper 接口)
└── SunnySideApplication.java --- 应用启动入口
```

##  快速开始

1. **环境准备**：确保安装 JDK 17+, MySQL 8.0+, Qdrant。
2. **数据库初始化**：
   - 执行 `db/sql.sql` 初始化表结构。
3. **配置**：
   - 修改 `application.yml`
   - 配置 `DASHSCOPE_API_KEY` 和数据库连接信息。