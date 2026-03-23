# SunnySide - 智能养老助手

基于 Spring Boot + Spring AI Alibaba 的养老场景智能助手，整合了多轮对话记忆、RAG 检索增强、工具调用、多模态与家属登录态能力。

## 核心能力

- 对话与记忆：基于 `ChatMemory` 支持多轮上下文
- RAG 检索：启动时加载本地知识文件到 Qdrant，回答时进行向量检索增强
- 工具调用：支持查询菜单、活动、公告、值班、来访预约、健康概览等业务工具
- 流式输出：支持 SSE 实时返回
- 多模态输入：支持 `image/*`、`audio/*`、`video/*` 文件上传对话
- 家属登录态：支持 JWT 登录并自动注入家属身份上下文

## 技术栈

- Java 17
- Spring Boot 3.5.11
- Spring AI 1.1.2 / Spring AI Alibaba 1.1.2.0
- MyBatis + MySQL
- Qdrant Vector Store

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

## 快速开始

1. 环境准备
   - JDK 17+
   - MySQL 8.0+
   - Qdrant（默认端口使用 6334）

2. 初始化数据库
   - 执行项目根目录 `sql.sql`

3. 配置参数
   - `src/main/resources/application.yml` 中可查看默认配置
   - 建议通过环境变量覆盖敏感项：
     - `DASHSCOPE_API_KEY`
     - `QDRANT_HOST`
     - `DB_USERNAME`
     - `DB_PASSWORD`
     - `APP_JWT_SECRET`

4. 启动项目（Windows）

```powershell
.\mvnw.cmd spring-boot:run
```

5. 运行测试（可选）

```powershell
.\mvnw.cmd test
```

## 主要接口示例

1) 老人端单轮对话

```http
GET /ElderChat?userInput=今天吃什么&userId=u1001
```

2) 家属端单轮对话

```http
GET /RelativesChat?userInput=我妈今天状态怎么样&userId=f2001
```

3) 流式对话（SSE）

```http
GET /stream?userInput=请播报今日活动&userId=u1001
Accept: text/event-stream
```

4) 多模态对话

```http
POST /chat/multimodal
Content-Type: multipart/form-data
字段: userInput, media(可多文件), userId
```

5) 家属登录

```http
POST /auth/family/login
Content-Type: application/json

{
  "account": "demo",
  "password": "demo123"
}
```

登录后可在请求头携带：

```http
Authorization: Bearer <token>
```

## RAG 说明

- 知识源文件：`src/main/resources/rag/ragKonloage.txt`
- 启动时会基于文件内容计算哈希，未变化时跳过重复写入
- 哈希记录默认写入：`target/rag/ragKonloage.sha256`
