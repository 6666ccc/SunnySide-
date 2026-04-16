create table faq
(
    id         bigint unsigned auto_increment
        primary key,
    category   varchar(64)                        not null comment '分类(如: ADMISSION,EXPENSE,INSURANCE,DISCHARGE,GENERAL)',
    question   varchar(255)                       not null comment '问题',
    answer     text                               not null comment '回答',
    sort_order int      default 0                 not null comment '排序权重',
    created_at datetime default CURRENT_TIMESTAMP not null
);

create table hospital_department
(
    id            bigint unsigned auto_increment comment '主键ID'
        primary key,
    dept_name     varchar(128)                       not null comment '科室/病区名称',
    contact_phone varchar(32)                        null comment '护士站/科室电话',
    location      varchar(255)                       null comment '病区位置 (如: 门诊楼4楼北侧)',
    created_at    datetime default CURRENT_TIMESTAMP not null
)
    comment '医院科室病区信息表';

create table hospital_announcement
(
    id           bigint unsigned auto_increment comment '主键ID'
        primary key,
    dept_id      bigint unsigned                                          not null comment '科室ID (为0表示全院)',
    title        varchar(128)                                             not null comment '公告标题',
    content      text                                                     not null comment '公告内容',
    publish_date date                                                     not null comment '发布日期',
    priority     enum ('LOW', 'MEDIUM', 'HIGH') default 'MEDIUM'          not null,
    created_at   datetime                       default CURRENT_TIMESTAMP not null,
    constraint fk_announce_dept
        foreign key (dept_id) references hospital_department (id)
            on delete cascade
)
    comment '医院/科室公告表';

create table medical_team_duty
(
    id         bigint unsigned auto_increment comment '主键ID'
        primary key,
    dept_id    bigint unsigned                                                         not null comment '所属科室ID',
    duty_date  date                                                                    not null comment '值班日期',
    staff_name varchar(64)                                                             not null comment '医护姓名',
    staff_role enum ('CHIEF_DOCTOR', 'ATTENDING_DOCTOR', 'PRIMARY_NURSE', 'CAREGIVER') not null comment '职责角色',
    duty_time  varchar(32)                                                             null comment '值班班次 (如: 白班, 夜班)',
    phone      varchar(32)                                                             null comment '病区紧急联系电话',
    created_at datetime default CURRENT_TIMESTAMP                                      not null,
    constraint fk_duty_dept
        foreign key (dept_id) references hospital_department (id)
            on delete cascade
)
    comment '医护值班及医疗团队表';

create index idx_duty_dept_date
    on medical_team_duty (dept_id, duty_date);

create table nearby_facility
(
    id            bigint unsigned auto_increment
        primary key,
    dept_id       bigint unsigned                                                              not null comment '关联科室ID',
    facility_name varchar(128)                                                                 not null comment '设施名称',
    facility_type enum ('CANTEEN', 'CONVENIENCE_STORE', 'PARKING', 'ATM', 'PHARMACY', 'OTHER') not null,
    location      varchar(255)                                                                 null comment '位置描述',
    distance      varchar(64)                                                                  null comment '大致距离或步行时间',
    created_at    datetime default CURRENT_TIMESTAMP                                           not null,
    constraint fk_facility_dept
        foreign key (dept_id) references hospital_department (id)
            on delete cascade
);

create table patient
(
    id             bigint unsigned auto_increment comment '主键ID'
        primary key,
    dept_id        bigint unsigned                                                             not null comment '所属科室ID',
    patient_name   varchar(64)                                                                 not null comment '患者姓名',
    gender         enum ('MALE', 'FEMALE', 'OTHER')                  default 'OTHER'           not null comment '性别',
    admission_no   varchar(64)                                                                 not null comment '住院号/病案号',
    bed_number     varchar(32)                                                                 not null comment '病床号',
    admission_date date                                                                        not null comment '入院日期',
    status         enum ('IN_HOSPITAL', 'DISCHARGED', 'TRANSFERRED') default 'IN_HOSPITAL'     not null comment '在院状态',
    username       varchar(64)                                                                 null comment '患者登录账号',
    password       varchar(128)                                                                null comment '患者登录密码(BCrypt)',
    created_at     datetime                                          default CURRENT_TIMESTAMP not null,
    updated_at     datetime                                          default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_admission_no
        unique (admission_no),
    constraint uk_patient_username
        unique (username),
    constraint fk_patient_dept
        foreign key (dept_id) references hospital_department (id)
            on delete cascade
)
    comment '患者基础信息表';

create table dietary_advice
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    patient_id      bigint unsigned                                not null comment '患者ID',
    meal_date       date                                           not null comment '日期',
    meal_type       enum ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK') not null comment '餐次',
    food_content    varchar(128)                                   not null comment '饮食内容 (如: 半流食/流食)',
    nutrition_notes varchar(255)                                   null comment '医嘱禁忌 (如: 禁食、低钠、糖尿病餐)',
    created_at      datetime default CURRENT_TIMESTAMP             not null,
    constraint fk_diet_patient
        foreign key (patient_id) references patient (id)
            on delete cascade
)
    comment '病员饮食及医嘱建议表';

create index idx_patient_dept
    on patient (dept_id);

create table relative_user
(
    id         bigint unsigned auto_increment comment '主键ID'
        primary key,
    username   varchar(64)                        not null comment '登录账号',
    password   varchar(128)                       not null comment '登录密码',
    full_name  varchar(64)                        not null comment '姓名',
    phone      varchar(32)                        not null comment '手机号',
    open_id    varchar(128)                       null comment '微信OpenID',
    created_at datetime default CURRENT_TIMESTAMP not null,
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_relative_phone
        unique (phone)
)
    comment '亲属/家属信息表';

create table relative_patient_relation
(
    id             bigint unsigned auto_increment comment '主键ID'
        primary key,
    relative_id    bigint unsigned                      not null comment '亲属ID',
    patient_id     bigint unsigned                      not null comment '患者ID',
    relation_type  varchar(32)                          not null comment '关系 (如: 配偶, 子女, 兄弟)',
    is_legal_proxy tinyint(1) default 0                 not null comment '是否为法律授权代理人/主要陪护',
    created_at     datetime   default CURRENT_TIMESTAMP not null,
    constraint uk_relative_patient
        unique (relative_id, patient_id),
    constraint fk_rel_patient
        foreign key (patient_id) references patient (id)
            on delete cascade,
    constraint fk_rel_user
        foreign key (relative_id) references relative_user (id)
            on delete cascade
)
    comment '亲属与患者关联表';

create table treatment_plan
(
    id           bigint unsigned auto_increment comment '主键ID'
        primary key,
    patient_id   bigint unsigned                                                                       not null comment '患者ID',
    task_name    varchar(128)                                                                          not null comment '诊疗项目 (如: 抽血、输液、CT检查)',
    description  varchar(255)                                                                          null comment '项目说明/注意事项',
    plan_date    date                                                                                  not null comment '计划日期',
    start_time   time                                                                                  not null comment '预计开始时间',
    end_time     time                                                                                  not null comment '预计结束时间',
    location     varchar(128)                                                                          null comment '检查地点 (如: 影像科2室)',
    category     enum ('SURGERY', 'EXAMINATION', 'INFUSION', 'MEDICATION', 'MEAL', 'NURSING', 'OTHER') not null comment '计划类型',
    is_completed tinyint(1) default 0                                                                  not null comment '是否已执行',
    created_at   datetime   default CURRENT_TIMESTAMP                                                  not null,
    constraint fk_plan_patient
        foreign key (patient_id) references patient (id)
            on delete cascade
)
    comment '诊疗护理计划表';

create index idx_plan_patient_date
    on treatment_plan (patient_id, plan_date);

create table vital_signs
(
    id           bigint unsigned auto_increment comment '主键ID'
        primary key,
    patient_id   bigint unsigned                    not null comment '患者ID',
    record_date  date                               not null comment '记录日期',
    record_time  time                               not null comment '记录时间',
    systolic_bp  int                                null comment '收缩压(高压) mmHg',
    diastolic_bp int                                null comment '舒张压(低压) mmHg',
    heart_rate   int                                null comment '心率 bpm',
    blood_sugar  decimal(5, 2)                      null comment '血糖 mmol/L',
    temperature  decimal(4, 1)                      null comment '体温 ℃',
    blood_oxygen int                                null comment '血氧饱和度 %',
    recorded_by  varchar(64)                        null comment '执行护士姓名',
    notes        varchar(255)                       null comment '临床表现或异常备注',
    created_at   datetime default CURRENT_TIMESTAMP not null,
    constraint fk_vital_patient
        foreign key (patient_id) references patient (id)
            on delete cascade
)
    comment '患者生命体征记录表';

create index idx_vital_patient_date
    on vital_signs (patient_id, record_date);

