# SunnySide 后端 HTTP 接口说明

面向前端对接。内容依据当前 Spring Boot 控制器与配置整理；默认 **无** `context-path`，端口以 `application.yml` 中 `server.port` 为准（常见为 `8080`）。

系统中有两类终端用户：**亲属端**（`relative_user`）与 **患者端**（患者在 `patient` 表上自助注册登录）。二者均使用 `Authorization: Bearer <JWT>`，JWT 的 subject 均为**用户名**字符串，但业务数据隔离，请使用对应角色的登录接口获取 Token，并调用对应功能模块。

---

## 1. 基础约定

| 项 | 说明 |
|----|------|
| Base URL | `http://<host>:<port>`，例如 `http://localhost:8080` |
| 统一 JSON 封装 | 多数接口返回 `Result<T>`：`{ "code": number, "message": string, "data": T \| null }`。业务成功时 `code` 一般为 **200** |
| 鉴权 | 除下方 **JWT 白名单** 中的路径外，请求需带：`Authorization: Bearer <JWT>` |
| 预检 | `OPTIONS` 请求由拦截器直接放行 |
| 跨域 CORS | 后端已配置全局 CORS，允许的来源、是否带 Cookie 等见 `application.yml` 中 `app.cors`（默认放行本机 `localhost` / `127.0.0.1` 任意端口） |

**JWT 白名单（不校验 Token）：**

- `POST /relative/login`
- `POST /relative/register`
- `POST /patient/login`
- `POST /patient/register`
- Spring 默认 `/error`

**其余路径均需有效 Bearer Token**（含 `/chat`、`/patient/chat`、`/bindPatient/*`、`/relative/getInfo` 等）。

---

## 2. 亲属账户 `/relative`

### 2.1 登录（无需 Bearer）

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `POST /relative/login` |
| Content-Type | `application/json` |

**请求体 `AuthRequest`：**

| 字段 | 类型 | 说明 |
|------|------|------|
| username | string | 登录名 |
| password | string | 密码 |

**响应：** `Result<LoginData>`

| 字段（位于 `data`） | 类型 | 说明 |
|---------------------|------|------|
| token | string | JWT（subject 为亲属用户名） |
| username | string | 登录名 |

失败时 `code` 多为 **400**，`message` 含业务说明（如「用户名或密码错误」）。

---

### 2.2 注册（无需 Bearer）

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `POST /relative/register` |
| Content-Type | `application/json` |
| 请求体 | 同 `AuthRequest` |

**响应：** `Result<Void>`，成功时 `data` 常为 `null`。

---

### 2.3 查询账户信息（需 Bearer：亲属 Token）

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `GET /relative/getInfo` |
| Query | `username`（必填） |

**响应：** **纯字符串**（非 `Result` 包装）。

- 成功：内容为 **JSON 字符串**，字段大致包括：`id`、`username`、`full_name`、`phone`、`open_id`、`created_at`、`updated_at`
- 失败：中文提示，如「用户不存在」「用户名不能为空」

---

### 2.4 修改密码（需 Bearer：亲属 Token）

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `POST /relative/updateInfo` |
| Query | `username`、`oldPassword`、`newPassword`（均必填） |

**响应：** 纯字符串，如「密码修改成功」或错误说明。

---

### 2.5 注销账号（需 Bearer：亲属 Token）

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `POST /relative/deleteInfo` |
| Query | `username`、`password` |

**响应：** 纯字符串，如「账号已注销」或错误说明。

---

## 3. 患者账户 `/patient`

患者账号通过**住院号**与院内已有患者记录关联后注册；登录使用患者自有用户名、密码（存于 `patient` 表）。

### 3.1 登录（无需 Bearer）

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `POST /patient/login` |
| Content-Type | `application/json` |

**请求体：** 同 `AuthRequest`（`username`、`password` 为患者端用户名与密码）。

**响应：** `Result<PatientLoginData>`

| 字段（位于 `data`） | 类型 | 说明 |
|---------------------|------|------|
| token | string | JWT（subject 为患者用户名） |
| username | string | 登录名 |
| patientId | number | 患者主键 ID |

---

### 3.2 注册（无需 Bearer）

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `POST /patient/register` |
| Content-Type | `application/json` |

**请求体 `PatientRegisterRequest`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| admissionNo | string | 是 | 住院号，用于匹配院内患者 |
| username | string | 是 | 自助注册的用户名 |
| password | string | 是 | 密码 |

**响应：** `Result<PatientLoginData>`（成功时同样返回 `token`、`username`、`patientId`，与登录结构一致）。

常见失败原因（`message`）：住院号不存在、该住院号对应患者已注册、用户名已被占用等。

---

## 4. 患者绑定 `/bindPatient`（均需 Bearer：亲属 Token）

用于**已登录的亲属**搜索患者、建立/解除与患者的绑定关系。

### 4.1 按患者 ID 搜索患者

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `GET /bindPatient/searchPatient` |
| Query | `patientId`（number，患者主键） |

**响应：** `Result<List<PatientSearchVo>>`

`PatientSearchVo`：

| 字段 | 类型 | 说明 |
|------|------|------|
| patientId | number | 患者主键 |
| patientName | string | 姓名（服务端脱敏） |
| bedNumber | string | 床号 |
| admissionNo | string | 住院号（脱敏） |
| deptName | string | 科室名称 |

未找到患者或 `patientId` 无效时 `data` 为空数组 `[]`。若未登录或账号无法解析为亲属：`code` **401**，`message`「未登录或账号无效」。

---

### 4.2 按患者 ID 绑定

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `POST /bindPatient/bindByPatientId` |
| Content-Type | `application/json` |

**请求体 `BindPatientRequest`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| patientId | number | 是 | 患者主键（与患者注册后 ID 一致） |
| relationType | string | 是 | 与患者关系，如：配偶、子女、父母 |
| isLegalProxy | boolean | 否 | 是否法律授权代理人/主要陪护，默认 false |

**响应：** `Result<Void>`（成功时 `message` 如「绑定成功」）。

---

### 4.3 当前登录亲属已绑定患者列表

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `GET /bindPatient/myPatients` |

**响应：** `Result<List<AuthorizedPatientVo>>`

`AuthorizedPatientVo`：

| 字段 | 类型 | 说明 |
|------|------|------|
| patientId | number | 患者主键 |
| patientName | string | 姓名 |
| bedNumber | string | 床号 |
| admissionNo | string | 住院号 |
| relationType | string | 与患者关系 |
| isLegalProxy | boolean | 是否法律授权代理人 |
| deptId | number | 科室 ID |
| deptName | string | 科室名称 |

---

### 4.4 解除绑定

| 项 | 值 |
|----|-----|
| 方法 / 路径 | `DELETE /bindPatient/unbind/{relationId}` |
| Path | `relationId`：表 `relative_patient_relation` 的**主键 id**（不是 `patientId`） |

**响应：** `Result<Void>`。

**说明：** 当前 `myPatients` 返回的 `AuthorizedPatientVo` **不包含** `relationId`。若前端需从列表发起解绑，需后端在列表中增加 `relationId` 字段，或提供按 `patientId` 解绑等接口。

---

## 5. AI 聊天（SSE，均需对应角色 Token）

两类聊天路径不同，请使用**对应角色**登录获得的 JWT。

### 5.1 亲属端 `GET /chat`

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer：**亲属** Token |
| Query | `timeId`：会话/记忆维度 ID；`message`：用户输入 |
| 响应类型 | `text/event-stream`（SSE） |

后端会将 JWT 解析为亲属用户，并加载亲属上下文（含已绑定患者等）后调用模型。**超时：** 约 **600000 ms**（10 分钟）。

---

### 5.2 患者端 `GET /patient/chat`

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer：**患者** Token |
| Query | `timeId`、`message`（含义同亲属端） |
| 响应类型 | `text/event-stream`（SSE） |

控制器内使用 `PatientAIChat` 流式输出；**超时：** 约 **600000 ms**（10 分钟）。

---

**SSE 通用说明：** 每个 SSE `data` 事件为一段 **UTF-8 纯文本**（模型输出片段）。浏览器原生 `EventSource` 无法设置 `Authorization`，若必须用 Bearer，请使用支持自定义头的 SSE 客户端或代理。

---

## 6. HTTP 状态与业务码

| 场景 | 表现 |
|------|------|
| 未带 `Authorization` 或格式非 `Bearer ` | HTTP **401**，响应体为拦截器文案（如「未登录」），**非** `Result` JSON |
| Token 解析失败 | HTTP **401**，「Token无效」 |
| 业务失败（多数 JSON 接口） | HTTP 可能仍为 200，以 `Result.code`、`Result.message` 为准（如 400、401、403） |

---

## 7. 对接顺序建议

1. **亲属端：** `POST /relative/login` → 使用返回的 `token` 调用 `/bindPatient/*`、`GET /chat`。
2. **患者端：** `POST /patient/login` 或 `POST /patient/register` → 使用返回的 `token` 调用 `GET /patient/chat`。
3. 后续请求统一增加：`Authorization: Bearer <token>`（勿混用两类 Token 调错模块）。
4. 对返回 `Result` 的接口：以 `code === 200` 作为业务成功判断，并处理 `message`。
5. `getInfo`、`updateInfo`、`deleteInfo` 按**纯文本**处理，其中 `getInfo` 成功时需对字符串做 `JSON.parse`。
6. 聊天使用 SSE；`timeId` 由前端与会话状态一致即可（与后端会话记忆关联）。

---

*文档与仓库代码同步维护；端口以 `application.yml` 与实际部署为准。*
