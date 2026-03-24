# Tasks
- [x] Task 1: 创建工作流基础组件
  - [x] SubTask 1.1: 创建 `cn.lc.sunnyside.Workflow.Node` 包（可选）。
  - [x] SubTask 1.2: 创建输入预处理节点 `InputProcessNode`（实现 `NodeAction`），用于提取和规范化用户输入，并添加注释。
  - [x] SubTask 1.3: 创建 LLM 处理节点 `LLMProcessNode`（实现 `NodeAction`），调用 Spring AI 的 `ChatClient` 生成回复，并添加注释。
- [x] Task 2: 编排与构建工作流
  - [x] SubTask 2.1: 创建 `SunnySideWorkflowService` 类（并添加 `@Service` 注解）。
  - [x] SubTask 2.2: 在 Service 中使用 Spring AI Alibaba Graph 相关的 Builder 构建器编排上述 Node，并定义 Edge（如将 `InputProcessNode` 和 `LLMProcessNode` 串联起来），并添加详细注释。
  - [x] SubTask 2.3: 提供对外的方法 `executeWorkflow(String query)`，接收输入并运行 Graph（使用 `OverAllState` 或相关上下文参数），最终返回处理结果。
- [x] Task 3: 提供测试接口
  - [x] SubTask 3.1: 创建 `WorkflowController`，提供 GET 或 POST `/api/workflow/chat` 接口。
  - [x] SubTask 3.2: 接口调用 `SunnySideWorkflowService` 测试工作流的执行效果，并确保链路正常通畅。

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 2]