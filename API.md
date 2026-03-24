# SunnySide 接口文档

## 1. 基础信息

- Base URL: `http://localhost:8080`
- 默认 Content-Type:
  - JSON 接口：`application/json`
  - 流式接口：`text/event-stream`
  - 多模态接口：`multipart/form-data`
- 字符集：UTF-8

## 2. 认证与登录态

### 2.1 获取 JWT

通过 `POST /auth/family/login` 获取 `token`，后续请求可在 Header 中携带：

```http
Authorization: Bearer <token>
```

### 2.2 当前认证行为说明（重要）

- JWT 由拦截器解析并写入上下文，供 AI 提示词使用。
- 未携带或携带无效 Token 时，接口目前不会被直接拦截为 401，仍可访问大部分接口。
- 是否真正按登录用户执行，取决于具体业务逻辑是否读取登录态上下文。

## 3. 通用响应结构

### 3.1 聊天类接口响应

```json
{
  "answer": "回复文本",
  "suggestedQuestions": []
}
```

字段说明：

- `answer`：模型回复内容
- `suggestedQuestions`：建议追问列表（当前实现通常为空数组）

### 3.2 错误响应（登录接口）

`/auth/family/login` 发生业务异常时：

```json
{
  "message": "错误信息"
}
```

## 4. 接口列表

---

### 4.1 家属登录

- **Method**: `POST`
- **Path**: `/auth/family/login`
- **Content-Type**: `application/json`
- **是否需要登录**: 否

请求体：

```json
{
  "account": "demo",
  "password": "demo123"
}
```

请求字段：

- `account`：账号（支持手机号或用户名）
- `password`：密码

成功响应 `200 OK`：

```json
{
  "token": "<jwt_token>",
  "tokenType": "Bearer",
  "expiresAt": "2026-03-24T12:00:00",
  "familyId": 1,
  "familyPhone": "13800000000",
  "familyUsername": "demo",
  "familyName": "张三"
}
```

失败响应：

- `400 Bad Request`

```json
{
  "message": "账号和密码不能为空。"
}
```

```json
{
  "message": "账号或密码错误。"
}
```

- `500 Internal Server Error`

```json
{
  "message": "服务端未配置JWT密钥。"
}
```

---

### 4.2 老人端单轮对话

- **Method**: `GET`
- **Path**: `/ElderChat`
- **是否需要登录**: 否（可带 JWT）

Query 参数：

- `userInput`（必填）：用户输入
- `userId`（建议填写）
- `UserID`（兼容旧字段，`userId` 优先）

示例：

```http
GET /ElderChat?userInput=今天吃什么&userId=u1001
```

成功响应 `200 OK`：

```json
{
  "answer": "今天建议清淡饮食，注意营养均衡。",
  "suggestedQuestions": []
}
```

参数缺失（`userId` 和 `UserID` 均为空）时仍返回 `200 OK`：

```json
{
  "answer": "userId不能为空。",
  "suggestedQuestions": []
}
```

---

### 4.3 家属端单轮对话

- **Method**: `GET`
- **Path**: `/RelativesChat`
- **是否需要登录**: 否（建议携带 JWT）

Query 参数：

- `userInput`（必填）：用户输入
- `userId`（建议填写）
- `UserID`（兼容旧字段，`userId` 优先）

示例：

```http
GET /RelativesChat?userInput=我妈今天状态怎么样&userId=f2001
Authorization: Bearer <token>
```

成功响应 `200 OK`：

```json
{
  "answer": "今日状态稳定，已完成晨检并参加活动。",
  "suggestedQuestions": []
}
```

参数缺失（`userId` 和 `UserID` 均为空）时仍返回 `200 OK`：

```json
{
  "answer": "userId不能为空。",
  "suggestedQuestions": []
}
```

---

### 4.4 流式对话（SSE）

- **Method**: `GET`
- **Path**: `/stream`
- **Produces**: `text/event-stream`
- **是否需要登录**: 否（可带 JWT）

Query 参数：

- `userInput`（必填）：用户输入
- `userId`（建议填写）
- `UserID`（兼容旧字段，`userId` 优先）

示例：

```http
GET /stream?userInput=请播报今日活动&userId=u1001
Accept: text/event-stream
```

返回说明：

- 按流式分片持续返回文本内容。
- 当 `userId` 和 `UserID` 均为空时，返回单条文本：`userId不能为空。`

---

### 4.5 多模态对话

- **Method**: `POST`
- **Path**: `/chat/multimodal`
- **Content-Type**: `multipart/form-data`
- **是否需要登录**: 否（可带 JWT）

表单字段：

- `userInput`（必填）：文本问题
- `media`（必填，可多文件）：媒体文件列表
- `userId`（建议填写）
- `UserID`（兼容旧字段，`userId` 优先）

支持的媒体类型：

- `image/*`
- `audio/*`
- `video/*`

示例（curl）：

```bash
curl -X POST "http://localhost:8080/chat/multimodal" \
  -H "Authorization: Bearer <token>" \
  -F "userInput=请描述这张图" \
  -F "userId=u1001" \
  -F "media=@C:/tmp/demo.jpg"
```

成功响应 `200 OK`：

```json
{
  "answer": "图片中是一位老人正在参加康复训练。",
  "suggestedQuestions": []
}
```

常见失败场景（均为 `200 OK` + 业务文案）：

```json
{
  "answer": "请上传媒体文件。",
  "suggestedQuestions": []
}
```

```json
{
  "answer": "无法识别媒体类型，请上传带有Content-Type的文件。",
  "suggestedQuestions": []
}
```

```json
{
  "answer": "仅支持 image/*、audio/*、video/* 类型的媒体输入。",
  "suggestedQuestions": []
}
```

## 5. 建议补充（便于前后端联调）

- 建议统一错误返回结构（如 `code/message/data`），避免当前“部分接口错误也返回 200”。
- 建议将 `userId` 统一为单一字段，逐步移除兼容字段 `UserID`。
- 建议补充接口版本前缀（如 `/api/v1`），降低后续迭代破坏性。
- 建议提供 OpenAPI/Swagger 自动文档，减少手工维护成本。
