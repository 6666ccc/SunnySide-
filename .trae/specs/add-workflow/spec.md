# Workflow Implementation Spec

## Why
用户希望在项目中引入工作流（Workflow），以便实现复杂的应用程序编排。基于 Spring AI Alibaba Graph，工作流可以将智能体编排为由节点串联而成的有向无环图（DAG），从而对复杂的业务逻辑（如多步骤的 AI 交互流程）提供更细粒度的控制和扩展能力。

## What Changes
- 引入 Spring AI Alibaba Agent Framework 中的底层 Graph API。
- 定义自定义状态（State）处理逻辑，并在节点间传递数据。
- 创建多个自定义节点（Node），如输入预处理节点（NodeAction）和 LLM 调用节点（带配置或基础 Node）。
- 编排节点间的边（Edge），实现基于状态流转的分支或顺序执行逻辑。
- 封装一个专门的 Workflow Service，提供对外调用的接口。
- 新增 Controller 接口供外部测试和调用该工作流。
- 新添加的代码必须包含详细的中文注释。

## Impact
- Affected specs: AI 服务能力、业务流程编排。
- Affected code: 
  - `src/main/java/cn/lc/sunnyside/Workflow/*` (新建工作流相关包和类)
  - `src/main/java/cn/lc/sunnyside/Controller/WorkflowController.java` (新建)

## ADDED Requirements
### Requirement: Spring AI Alibaba Graph Workflow
系统应提供一个基于 Graph API 的工作流实现：
- 能够接收用户的 query。
- 经过多个 Node 处理（如前置处理节点、LLM 生成节点）。
- 返回处理后的最终结果。

#### Scenario: Success case
- **WHEN** 用户通过 Controller 发起请求，传入 `query` 参数。
- **THEN** 工作流按照预定的 Node 和 Edge 顺序执行，并将最终生成的结果返回给用户。