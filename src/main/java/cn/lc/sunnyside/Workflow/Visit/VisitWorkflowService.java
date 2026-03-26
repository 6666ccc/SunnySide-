package cn.lc.sunnyside.Workflow.Visit;

import cn.lc.sunnyside.POJO.DO.VisitAppointment;
import cn.lc.sunnyside.Service.FamilyAccessService;
import cn.lc.sunnyside.Service.VisitAppointmentService;
import cn.lc.sunnyside.Workflow.common.BaseWorkflowExecutor;
import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VisitWorkflowService extends BaseWorkflowExecutor {

    private final CompiledGraph workflow;

    @Component
    public static class AnalyzeVisitIntentNode implements NodeAction {
        private final ChatClient chatClient;
        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 构造探访意图分析节点。
         *
         * @param builder ChatClient 构造器
         */
        public AnalyzeVisitIntentNode(ChatClient.Builder builder) {
            this.chatClient = builder.build();
        }

        /**
         * 解析用户探访请求，抽取操作类型与相关参数。
         * 优先使用模型结构化抽取，失败时回退关键字规则。
         *
         * @param state 当前工作流状态
         * @return 写入意图、操作与参数字段的状态增量
         */
        @Override
        public Map<String, Object> apply(OverAllState state) {
            String query = state.value(WorkflowStateKeys.QUERY).map(Object::toString).orElse("");
            Map<String, Object> result = new HashMap<>();
            String prompt = "请从用户输入中抽取探访意图并严格输出JSON，不要输出其他文字。\n" +
                    "{\"is_visit_query\":true/false,\"operation\":\"QUERY|BOOK|CANCEL\",\"elder_name\":\"\",\"status\":\"\",\"from\":\"\",\"to\":\"\",\"appointment_id\":0,\"visitor_name\":\"\",\"relation\":\"\",\"phone\":\"\",\"visit_time\":\"\"}\n"
                    +
                    "用户输入：" + query;
            try {
                String json = chatClient.prompt().user(prompt).call().content();
                json = json.replace("```json", "").replace("```", "").trim();
                JsonNode node = objectMapper.readTree(json);
                boolean isVisitQuery = node.path("is_visit_query").asBoolean(false);
                String operation = node.path("operation").asText("QUERY");
                String elderName = node.path("elder_name").asText("");
                String status = node.path("status").asText("");
                String from = node.path("from").asText("");
                String to = node.path("to").asText("");
                long appointmentId = node.path("appointment_id").asLong(0L);
                String visitorName = node.path("visitor_name").asText("");
                String relation = node.path("relation").asText("");
                String phone = node.path("phone").asText("");
                String visitTime = node.path("visit_time").asText("");

                result.put(WorkflowStateKeys.INTENT, isVisitQuery ? "VISIT_QUERY" : "GENERAL");
                result.put(WorkflowStateKeys.OPERATION, normalizeOperation(operation, query));
                result.put(WorkflowStateKeys.ELDER_NAME, elderName);
                result.put("status", status);
                result.put("from", from);
                result.put("to", to);
                result.put("appointment_id", appointmentId);
                result.put("visitor_name", visitorName);
                result.put("relation", relation);
                result.put("visitor_phone", phone);
                result.put("visit_time", visitTime);
            } catch (Exception e) {
                result.put(WorkflowStateKeys.INTENT, keywordVisitIntent(query) ? "VISIT_QUERY" : "GENERAL");
                result.put(WorkflowStateKeys.OPERATION, normalizeOperation("", query));
                result.put(WorkflowStateKeys.ELDER_NAME, "");
            }
            return result;
        }

        /**
         * 使用关键字判断是否属于探访域请求。
         *
         * @param query 用户输入
         * @return 是否为探访相关问题
         */
        private boolean keywordVisitIntent(String query) {
            return query.contains("探访") || query.contains("看望") || query.contains("来访") || query.contains("预约");
        }

        /**
         * 归一化操作类型。
         *
         * @param operation 模型抽取结果
         * @param query     用户原始文本
         * @return BOOK、CANCEL 或 QUERY
         */
        private String normalizeOperation(String operation, String query) {
            if ("BOOK".equalsIgnoreCase(operation)) {
                return "BOOK";
            }
            if ("CANCEL".equalsIgnoreCase(operation)) {
                return "CANCEL";
            }
            if (query.contains("取消")) {
                return "CANCEL";
            }
            if (query.contains("预约")) {
                return "BOOK";
            }
            return "QUERY";
        }
    }

    @Component
    public static class ResolveVisitContextNode implements NodeAction {
        private final FamilyAccessService familyAccessService;

        /**
         * 构造探访上下文解析节点。
         *
         * @param familyAccessService 家属访问控制服务
         */
        public ResolveVisitContextNode(FamilyAccessService familyAccessService) {
            this.familyAccessService = familyAccessService;
        }

        /**
         * 基于登录家属和老人姓名解析具体老人ID。
         *
         * @param state 当前工作流状态
         * @return 写入 ELDER_ID 或错误信息的状态增量
         */
        @Override
        public Map<String, Object> apply(OverAllState state) {
            Map<String, Object> result = new HashMap<>();
            String intent = state.value(WorkflowStateKeys.INTENT).map(Object::toString).orElse("GENERAL");
            if (!"VISIT_QUERY".equalsIgnoreCase(intent)) {
                result.put(WorkflowStateKeys.ERROR_CODE, "NOT_VISIT_DOMAIN");
                result.put(WorkflowStateKeys.ERROR_MESSAGE, "当前请求不属于探访业务。");
                return result;
            }

            String familyPhone = state.value(WorkflowStateKeys.FAMILY_PHONE).map(Object::toString).orElse(null);
            if (!StringUtils.hasText(familyPhone)) {
                result.put(WorkflowStateKeys.ERROR_CODE, "FAMILY_NOT_LOGIN");
                result.put(WorkflowStateKeys.ERROR_MESSAGE, "未获取到家属手机号，请先登录。");
                return result;
            }

            String elderName = state.value(WorkflowStateKeys.ELDER_NAME).map(Object::toString).orElse("");
            Long elderId = null;
            if (StringUtils.hasText(elderName)) {
                elderId = familyAccessService.resolveElderIdByName(familyPhone, elderName.trim());
            }
            if (elderId == null) {
                elderId = familyAccessService.resolveDefaultElderId(familyPhone);
            }
            if (elderId == null) {
                result.put(WorkflowStateKeys.ERROR_CODE, "ELDER_NOT_BOUND");
                result.put(WorkflowStateKeys.ERROR_MESSAGE, "未找到您绑定的老人信息，请先确认绑定关系。");
                return result;
            }
            result.put(WorkflowStateKeys.ELDER_ID, elderId);
            return result;
        }
    }

    @Component
    public static class ExecuteVisitNode implements NodeAction {
        private final VisitAppointmentService visitAppointmentService;

        /**
         * 构造探访业务执行节点。
         *
         * @param visitAppointmentService 探访预约服务
         */
        public ExecuteVisitNode(VisitAppointmentService visitAppointmentService) {
            this.visitAppointmentService = visitAppointmentService;
        }

        /**
         * 执行探访业务动作。
         * 支持预约新增、取消与查询三类操作。
         *
         * @param state 当前工作流状态
         * @return 写入业务结果或错误信息的状态增量
         */
        @Override
        public Map<String, Object> apply(OverAllState state) {
            Map<String, Object> result = new HashMap<>();
            String errorMessage = state.value(WorkflowStateKeys.ERROR_MESSAGE).map(Object::toString).orElse(null);
            if (StringUtils.hasText(errorMessage)) {
                return result;
            }

            String operation = state.value(WorkflowStateKeys.OPERATION).map(Object::toString).orElse("QUERY");
            Long elderId = state.value(WorkflowStateKeys.ELDER_ID).map(v -> (Long) v).orElse(null);
            if (elderId == null) {
                result.put(WorkflowStateKeys.ERROR_CODE, "ELDER_ID_MISSING");
                result.put(WorkflowStateKeys.ERROR_MESSAGE, "无法确认老人身份，请补充老人姓名。");
                return result;
            }

            if ("BOOK".equalsIgnoreCase(operation)) {
                String visitorName = state.value("visitor_name").map(Object::toString).orElse("");
                String relation = state.value("relation").map(Object::toString).orElse("");
                String visitorPhone = state.value("visitor_phone").map(Object::toString).orElse("");
                String visitTimeText = state.value("visit_time").map(Object::toString).orElse("");
                if (!StringUtils.hasText(visitorName) || !StringUtils.hasText(relation)
                        || !StringUtils.hasText(visitTimeText)) {
                    result.put(WorkflowStateKeys.ERROR_CODE, "VISIT_BOOK_PARAM_MISSING");
                    result.put(WorkflowStateKeys.ERROR_MESSAGE, "预约失败，请补充访客姓名、关系和来访时间（ISO格式）。");
                    return result;
                }
                try {
                    LocalDateTime visitTime = LocalDateTime.parse(visitTimeText);
                    String text = visitAppointmentService.bookVisit(elderId, visitorName, visitorPhone, visitTime,
                            relation);
                    result.put(WorkflowStateKeys.BIZ_RESULT, text);
                    return result;
                } catch (Exception e) {
                    result.put(WorkflowStateKeys.ERROR_CODE, "VISIT_BOOK_TIME_INVALID");
                    result.put(WorkflowStateKeys.ERROR_MESSAGE, "预约失败，来访时间格式错误，请使用ISO格式。");
                    return result;
                }
            }

            if ("CANCEL".equalsIgnoreCase(operation)) {
                Long appointmentId = state.value("appointment_id").map(v -> ((Number) v).longValue()).orElse(0L);
                if (appointmentId <= 0L) {
                    result.put(WorkflowStateKeys.ERROR_CODE, "VISIT_CANCEL_PARAM_MISSING");
                    result.put(WorkflowStateKeys.ERROR_MESSAGE, "取消失败，请提供预约ID。");
                    return result;
                }
                String text = visitAppointmentService.cancelVisitAppointment(elderId, appointmentId);
                result.put(WorkflowStateKeys.BIZ_RESULT, text);
                return result;
            }

            String status = state.value("status").map(Object::toString).orElse("");
            String from = state.value("from").map(Object::toString).orElse("");
            String to = state.value("to").map(Object::toString).orElse("");
            List<VisitAppointment> appointments = visitAppointmentService.queryVisitAppointments(
                    elderId,
                    normalizeText(status),
                    parseDateTimeOrNull(from),
                    parseDateTimeOrNull(to));
            if (appointments == null || appointments.isEmpty()) {
                result.put(WorkflowStateKeys.BIZ_RESULT, "没有符合条件的预约记录。");
                return result;
            }
            String text = appointments.stream()
                    .map(v -> "预约ID:" + v.getId() + " " + v.getVisitorName() + " (" + v.getRelation() + ") 来访时间: "
                            + v.getVisitTime() + " 状态: " + v.getStatus())
                    .collect(Collectors.joining("; "));
            result.put(WorkflowStateKeys.BIZ_RESULT, text);
            return result;
        }

        /**
         * 尝试将文本解析为 LocalDateTime。
         *
         * @param text 时间文本
         * @return 解析成功返回时间对象，否则返回 null
         */
        private LocalDateTime parseDateTimeOrNull(String text) {
            if (!StringUtils.hasText(text)) {
                return null;
            }
            try {
                return LocalDateTime.parse(text.trim());
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * 归一化状态文本。
         *
         * @param text 状态文本
         * @return 去空格并转大写后的状态；为空时返回 null
         */
        private String normalizeText(String text) {
            if (!StringUtils.hasText(text)) {
                return null;
            }
            return text.trim().toUpperCase();
        }
    }

    @Component
    public static class GenerateVisitReplyNode implements NodeAction {
        /**
         * 生成探访域最终回复。
         *
         * @param state 当前工作流状态
         * @return 写入 FINAL_REPLY 字段的状态增量
         */
        @Override
        public Map<String, Object> apply(OverAllState state) {
            Map<String, Object> result = new HashMap<>();
            String errorMessage = state.value(WorkflowStateKeys.ERROR_MESSAGE).map(Object::toString).orElse(null);
            if (StringUtils.hasText(errorMessage)) {
                result.put(WorkflowStateKeys.FINAL_REPLY, errorMessage);
                return result;
            }
            String bizResult = state.value(WorkflowStateKeys.BIZ_RESULT).map(Object::toString).orElse("");
            result.put(WorkflowStateKeys.FINAL_REPLY, bizResult);
            return result;
        }
    }

    /**
     * 构建探访工作流图：意图分析 -> 上下文解析 -> 业务执行 -> 回复输出。
     *
     * @param analyzeNode 意图分析节点
     * @param resolveNode 上下文解析节点
     * @param executeNode 业务执行节点
     * @param replyNode   回复生成节点
     */
    public VisitWorkflowService(AnalyzeVisitIntentNode analyzeNode,
            ResolveVisitContextNode resolveNode,
            ExecuteVisitNode executeNode,
            GenerateVisitReplyNode replyNode) {
        StateGraph graph = new StateGraph();
        try {
            graph.addNode("analyzeNode", AsyncNodeAction.node_async(analyzeNode));
            graph.addNode("resolveNode", AsyncNodeAction.node_async(resolveNode));
            graph.addNode("executeNode", AsyncNodeAction.node_async(executeNode));
            graph.addNode("replyNode", AsyncNodeAction.node_async(replyNode));
            graph.addEdge(StateGraph.START, "analyzeNode");
            graph.addEdge("analyzeNode", "resolveNode");
            graph.addEdge("resolveNode", "executeNode");
            graph.addEdge("executeNode", "replyNode");
            graph.addEdge("replyNode", StateGraph.END);
            this.workflow = graph.compile();
        } catch (Exception e) {
            throw new RuntimeException("构建探访工作流失败", e);
        }
    }

    /**
     * 执行探访工作流。
     *
     * @param query          用户输入
     * @param familyPhone    家属手机号
     * @param conversationId 会话ID
     * @return 工作流执行结果
     */
    public String executeWorkflow(String query, String familyPhone, String conversationId) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(WorkflowStateKeys.QUERY, query);
        if (StringUtils.hasText(familyPhone)) {
            inputs.put(WorkflowStateKeys.FAMILY_PHONE, familyPhone.trim());
        }
        if (StringUtils.hasText(conversationId)) {
            inputs.put(WorkflowStateKeys.CONVERSATION_ID, conversationId);
        }
        return executeWorkflow(workflow, inputs, WorkflowStateKeys.FINAL_REPLY, "探访工作流执行失败。");
    }
}
