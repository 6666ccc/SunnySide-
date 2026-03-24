package cn.lc.sunnyside.Controller;

import cn.lc.sunnyside.Auth.FamilyLoginContextHolder;
import cn.lc.sunnyside.Workflow.Health.HealthWorkflowService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * 工作流测试控制器
 */
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final HealthWorkflowService healthWorkflowService;

    public WorkflowController(HealthWorkflowService healthWorkflowService) {

        this.healthWorkflowService = healthWorkflowService;
    }

    /**
     * 体验家属健康查询专属工作流
     * @param query 用户输入，例如：“查一下我爸昨天的健康状况”
     * @param phone 手动传入的手机号（测试用），如果不传则尝试从登录上下文中获取
     * @return 工作流执行结果
     */
    @GetMapping("/health-chat")
    public String healthWorkflowChat(
            @RequestParam(name = "query", defaultValue = "查一下我爸昨天的健康状况") String query,
            @RequestParam(name = "phone", required = false) String phone) {
        
        String familyPhone = phone;
        // 如果未手动传入手机号，尝试从全局登录态中获取
        if (familyPhone == null || familyPhone.isBlank()) {
            familyPhone = FamilyLoginContextHolder.get().map(ctx -> ctx.phone()).orElse(null);
        }
        
        return healthWorkflowService.executeWorkflow(query, familyPhone);
    }

}
