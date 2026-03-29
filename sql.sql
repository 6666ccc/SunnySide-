-- ==========================================
-- 1. 医院科室/病区信息表 (原 nursing_home)
-- ==========================================
CREATE TABLE hospital_department
(
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    dept_name     VARCHAR(128)                       NOT NULL COMMENT '科室/病区名称',
    contact_phone VARCHAR(32)                        NULL COMMENT '护士站/科室电话',
    location      VARCHAR(255)                       NULL COMMENT '病区位置 (如: 门诊楼4楼北侧)',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
) COMMENT '医院科室病区信息表';

-- ==========================================
-- 2. 患者基础信息表 (原 elderly_user)
-- ==========================================
CREATE TABLE patient
(
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    dept_id        BIGINT UNSIGNED                                                             NOT NULL COMMENT '所属科室ID',
    patient_name   VARCHAR(64)                                                                 NOT NULL COMMENT '患者姓名',
    gender         ENUM ('MALE', 'FEMALE', 'OTHER')                  DEFAULT 'OTHER'           NOT NULL COMMENT '性别',
    admission_no   VARCHAR(64)                                                                 NOT NULL COMMENT '住院号/病案号',
    bed_number     VARCHAR(32)                                                                 NOT NULL COMMENT '病床号',
    admission_date DATE                                                                        NOT NULL COMMENT '入院日期',
    status         ENUM ('IN_HOSPITAL', 'DISCHARGED', 'TRANSFERRED') DEFAULT 'IN_HOSPITAL'     NOT NULL COMMENT '在院状态',
    created_at     DATETIME                                          DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     DATETIME                                          DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient_dept FOREIGN KEY (dept_id) REFERENCES hospital_department (id) ON DELETE CASCADE
) COMMENT '患者基础信息表';

CREATE INDEX idx_patient_dept ON patient (dept_id);
CREATE UNIQUE INDEX uk_admission_no ON patient (admission_no);

-- ==========================================
-- 3. 诊疗/护理计划表 (原 activity_schedule)
-- ==========================================
CREATE TABLE treatment_plan
(
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    patient_id   BIGINT UNSIGNED                                                                       NOT NULL COMMENT '患者ID',
    task_name    VARCHAR(128)                                                                          NOT NULL COMMENT '诊疗项目 (如: 抽血、输液、CT检查)',
    description  VARCHAR(255)                                                                          NULL COMMENT '项目说明/注意事项',
    plan_date    DATE                                                                                  NOT NULL COMMENT '计划日期',
    start_time   TIME                                                                                  NOT NULL COMMENT '预计开始时间',
    end_time     TIME                                                                                  NOT NULL COMMENT '预计结束时间',
    location     VARCHAR(128)                                                                          NULL COMMENT '检查地点 (如: 影像科2室)',
    category     ENUM ('SURGERY', 'EXAMINATION', 'INFUSION', 'MEDICATION', 'MEAL', 'NURSING', 'OTHER') NOT NULL COMMENT '计划类型',
    is_completed TINYINT(1) DEFAULT 0                                                                  NOT NULL COMMENT '是否已执行',
    created_at   DATETIME   DEFAULT CURRENT_TIMESTAMP                                                  NOT NULL,
    CONSTRAINT fk_plan_patient FOREIGN KEY (patient_id) REFERENCES patient (id) ON DELETE CASCADE
) COMMENT '诊疗护理计划表';

CREATE INDEX idx_plan_patient_date ON treatment_plan (patient_id, plan_date);

-- ==========================================
-- 4. 生命体征记录表 (原 health_record)
-- ==========================================
CREATE TABLE vital_signs
(
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    patient_id   BIGINT UNSIGNED                    NOT NULL COMMENT '患者ID',
    record_date  DATE                               NOT NULL COMMENT '记录日期',
    record_time  TIME                               NOT NULL COMMENT '记录时间',
    systolic_bp  INT                                NULL COMMENT '收缩压(高压) mmHg',
    diastolic_bp INT                                NULL COMMENT '舒张压(低压) mmHg',
    heart_rate   INT                                NULL COMMENT '心率 bpm',
    blood_sugar  DECIMAL(5, 2)                      NULL COMMENT '血糖 mmol/L',
    temperature  DECIMAL(4, 1)                      NULL COMMENT '体温 ℃',
    blood_oxygen INT                                NULL COMMENT '血氧饱和度 %',
    recorded_by  VARCHAR(64)                        NULL COMMENT '执行护士姓名',
    notes        VARCHAR(255)                       NULL COMMENT '临床表现或异常备注',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_vital_patient FOREIGN KEY (patient_id) REFERENCES patient (id) ON DELETE CASCADE
) COMMENT '患者生命体征记录表';

CREATE INDEX idx_vital_patient_date ON vital_signs (patient_id, record_date);

-- ==========================================
-- 5. 医疗值班表 (原 medical_duty)
-- ==========================================
CREATE TABLE medical_team_duty
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    dept_id    BIGINT UNSIGNED                                                         NOT NULL COMMENT '所属科室ID',
    duty_date  DATE                                                                    NOT NULL COMMENT '值班日期',
    staff_name VARCHAR(64)                                                             NOT NULL COMMENT '医护姓名',
    staff_role ENUM ('CHIEF_DOCTOR', 'ATTENDING_DOCTOR', 'PRIMARY_NURSE', 'CAREGIVER') NOT NULL COMMENT '职责角色',
    duty_time  VARCHAR(32)                                                             NULL COMMENT '值班班次 (如: 白班, 夜班)',
    phone      VARCHAR(32)                                                             NULL COMMENT '病区紧急联系电话',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP                                      NOT NULL,
    CONSTRAINT fk_duty_dept FOREIGN KEY (dept_id) REFERENCES hospital_department (id) ON DELETE CASCADE
) COMMENT '医护值班及医疗团队表';

CREATE INDEX idx_duty_dept_date ON medical_team_duty (dept_id, duty_date);

-- ==========================================
-- 6. 病员饮食清单 (原 menu)
-- ==========================================
CREATE TABLE dietary_advice
(
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    patient_id      BIGINT UNSIGNED                                NOT NULL COMMENT '患者ID',
    meal_date       DATE                                           NOT NULL COMMENT '日期',
    meal_type       ENUM ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK') NOT NULL COMMENT '餐次',
    food_content    VARCHAR(128)                                   NOT NULL COMMENT '饮食内容 (如: 半流食/流食)',
    nutrition_notes VARCHAR(255)                                   NULL COMMENT '医嘱禁忌 (如: 禁食、低钠、糖尿病餐)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP             NOT NULL,
    CONSTRAINT fk_diet_patient FOREIGN KEY (patient_id) REFERENCES patient (id) ON DELETE CASCADE
) COMMENT '病员饮食及医嘱建议表';

-- ==========================================
-- 7. 亲属/家属信息表 (原 family_user)
-- ==========================================
CREATE TABLE relative_user
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username   VARCHAR(64)                        NOT NULL COMMENT '登录账号',
    password   VARCHAR(128)                       NOT NULL COMMENT '登录密码',
    full_name  VARCHAR(64)                        NOT NULL COMMENT '姓名',
    phone      VARCHAR(32)                        NOT NULL COMMENT '手机号',
    open_id    VARCHAR(128)                       NULL COMMENT '微信OpenID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP
) COMMENT '亲属/家属信息表';

CREATE UNIQUE INDEX uk_relative_phone ON relative_user (phone);

-- ==========================================
-- 8. 亲属与患者关联表 (原 family_elder_relation)
-- ==========================================
CREATE TABLE relative_patient_relation
(
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    relative_id    BIGINT UNSIGNED                      NOT NULL COMMENT '亲属ID',
    patient_id     BIGINT UNSIGNED                      NOT NULL COMMENT '患者ID',
    relation_type  VARCHAR(32)                          NOT NULL COMMENT '关系 (如: 配偶, 子女, 兄弟)',
    is_legal_proxy TINYINT(1) DEFAULT 0                 NOT NULL COMMENT '是否为法律授权代理人/主要陪护',
    created_at     DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_relative_patient UNIQUE (relative_id, patient_id),
    CONSTRAINT fk_rel_user FOREIGN KEY (relative_id) REFERENCES relative_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_patient FOREIGN KEY (patient_id) REFERENCES patient (id) ON DELETE CASCADE
) COMMENT '亲属与患者关联表';

-- ==========================================
-- 9. 医院通知公告 (原 announcement)
-- ==========================================
CREATE TABLE hospital_announcement
(
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    dept_id      BIGINT UNSIGNED                                          NOT NULL COMMENT '科室ID (为0表示全院)',
    title        VARCHAR(128)                                             NOT NULL COMMENT '公告标题',
    content      TEXT                                                     NOT NULL COMMENT '公告内容',
    publish_date DATE                                                     NOT NULL COMMENT '发布日期',
    priority     ENUM ('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM'          NOT NULL,
    created_at   DATETIME                       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_announce_dept FOREIGN KEY (dept_id) REFERENCES hospital_department (id) ON DELETE CASCADE
) COMMENT '医院/科室公告表';


#测试sql脚本
-- 1. 插入科室：心内科与呼吸科
INSERT INTO hospital_department (dept_name, contact_phone, location)
VALUES ('心内科病区', '021-6666001', '住院部大楼A座4楼'),
       ('呼吸内科病区', '021-6666002', '住院部大楼A座5楼');

-- 2. 插入患者：张大爷（心内科）与王阿姨（呼吸科）
-- 假设心内科 ID 为 1，呼吸科 ID 为 2
INSERT INTO patient (dept_id, patient_name, gender, admission_no, bed_number, admission_date, status)
VALUES (1, '张建国', 'MALE', 'ZY20260329001', 'A401', '2026-03-20', 'IN_HOSPITAL'),
       (2, '王美兰', 'FEMALE', 'ZY20260329002', 'A512', '2026-03-25', 'IN_HOSPITAL');

-- 3. 插入亲属账号 (密码均为加密占位符)
INSERT INTO relative_user (username, password, full_name, phone)
VALUES ('zhang_son', 'e10adc3949ba59abbe56e057f20f883e', '张志强', '13811112222'),
       ('wang_daughter', 'e10adc3949ba59abbe56e057f20f883e', '李小燕', '13933334444');

-- 4. 建立亲属与患者关联
-- 张志强 是 张建国 的儿子 (主要陪护)
-- 李小燕 是 王美兰 的女儿
INSERT INTO relative_patient_relation (relative_id, patient_id, relation_type, is_legal_proxy)
VALUES (1, 1, '儿子', 1),
       (2, 2, '女儿', 0);

-- 5. 插入诊疗计划（模拟今日与明天的任务）
INSERT INTO treatment_plan (patient_id, task_name, description, plan_date, start_time, end_time, category, location,
                            is_completed)
VALUES (1, '空腹采血', '需禁食禁水8小时', CURDATE(), '07:30:00', '08:00:00', 'EXAMINATION', '病房内', 1),
       (1, '心脏彩超', '排队人数较多，请提前准备', CURDATE(), '14:00:00', '15:00:00', 'EXAMINATION', '医技楼2楼彩超室',
        0),
       (2, '雾化治疗', '使用布地奈德混悬液', CURDATE(), '09:00:00', '09:30:00', 'INFUSION', '病房内', 1),
       (2, '胸部CT', '复查肺部炎症吸收情况', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', '10:30:00', 'EXAMINATION',
        '影像科1室', 0);

-- 6. 插入生命体征数据 (模拟今日多次测量)
INSERT INTO vital_signs (patient_id, record_date, record_time, systolic_bp, diastolic_bp, heart_rate, temperature,
                         blood_oxygen, recorded_by, notes)
VALUES (1, CURDATE(), '08:15:00', 135, 88, 72, 36.6, 98, '林护士', '血压略高，嘱患者静卧'),
       (1, CURDATE(), '12:30:00', 128, 82, 68, 36.7, 99, '林护士', '状态平稳'),
       (2, CURDATE(), '08:30:00', 120, 75, 85, 37.8, 94, '陈护士', '有轻微发热，血氧波动');

-- 7. 插入今日医护值班信息
INSERT INTO medical_team_duty (dept_id, duty_date, staff_name, staff_role, duty_time, phone)
VALUES (1, CURDATE(), '李明远', 'CHIEF_DOCTOR', '白班', '13566667777'),
       (1, CURDATE(), '林小溪', 'PRIMARY_NURSE', '24小时', '021-6666001'),
       (2, CURDATE(), '赵医生', 'ATTENDING_DOCTOR', '白班', '13899990000');

-- 8. 插入病员饮食医嘱
INSERT INTO dietary_advice (patient_id, meal_date, meal_type, food_content, nutrition_notes)
VALUES (1, CURDATE(), 'LUNCH', '低盐低脂餐', '禁食辛辣，控制油脂摄入'),
       (2, CURDATE(), 'LUNCH', '半流食', '多补充水分');

-- 9. 插入医院公告
-- 假设全院公告 dept_id 在代码逻辑中处理，这里为了符合外键先插到科室 1
INSERT INTO hospital_announcement (dept_id, title, content, publish_date, priority)
VALUES (1, '关于病房探视时间的通知', '为保障患者休息，每日探视时间调整为 15:00-18:00。', CURDATE(), 'HIGH');