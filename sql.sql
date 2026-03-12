create table nursing_home
(
    id         bigint unsigned auto_increment comment '主键ID'
        primary key,
    name       varchar(128)                       not null comment '养老院名称',
    phone      varchar(32)                        null comment '联系电话',
    address    varchar(255)                       null comment '地址',
    created_at datetime default CURRENT_TIMESTAMP not null
)
    comment '养老院信息表';

create table activity_schedule
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    nursing_home_id bigint unsigned                                                               not null comment '所属养老院ID',
    activity_name   varchar(128)                                                                  not null comment '活动名称',
    description     varchar(255)                                                                  null comment '活动描述',
    activity_date   date                                                                          not null comment '活动日期',
    start_time      time                                                                          not null comment '开始时间',
    end_time        time                                                                          not null comment '结束时间',
    location        varchar(128)                                                                  null comment '活动地点',
    category        enum ('EXERCISE', 'MEAL', 'ENTERTAINMENT', 'HEALTH_CHECK', 'SOCIAL', 'OTHER') not null comment '活动类型',
    capacity        int unsigned                                                                  null comment '容纳人数',
    is_mandatory    tinyint(1) default 0                                                          not null comment '是否必参',
    created_at      datetime   default CURRENT_TIMESTAMP                                          not null,
    updated_at      datetime   default CURRENT_TIMESTAMP                                          not null on update CURRENT_TIMESTAMP,
    constraint fk_schedule_home
        foreign key (nursing_home_id) references nursing_home (id)
            on delete cascade
)
    comment '活动日程表';

create index idx_schedule_date_time
    on activity_schedule (activity_date, start_time);

create index idx_schedule_home_date
    on activity_schedule (nursing_home_id, activity_date);

create table announcement
(
    id                bigint unsigned auto_increment comment '主键ID'
        primary key,
    nursing_home_id   bigint unsigned                                          not null comment '所属养老院ID',
    title             varchar(128)                                             not null comment '公告标题',
    content           text                                                     not null comment '公告内容',
    announcement_date date                                                     not null comment '发布日期',
    priority          enum ('LOW', 'MEDIUM', 'HIGH') default 'MEDIUM'          not null comment '优先级',
    is_active         tinyint(1)                     default 1                 not null comment '是否有效',
    created_at        datetime                       default CURRENT_TIMESTAMP not null,
    constraint fk_announce_home
        foreign key (nursing_home_id) references nursing_home (id)
            on delete cascade
)
    comment '通知公告表';

create index idx_announce_home_date
    on announcement (nursing_home_id, announcement_date);

create table elderly_user
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    nursing_home_id bigint unsigned                                            not null comment '所属养老院ID',
    full_name       varchar(64)                                                not null comment '姓名',
    gender          enum ('MALE', 'FEMALE', 'OTHER') default 'OTHER'           not null comment '性别',
    phone           varchar(32)                                                null comment '联系电话',
    created_at      datetime                         default CURRENT_TIMESTAMP not null,
    updated_at      datetime                         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_elderly_home
        foreign key (nursing_home_id) references nursing_home (id)
            on delete cascade
)
    comment '老人基础信息表';

create table activity_participation
(
    id                   bigint unsigned auto_increment comment '主键ID'
        primary key,
    elder_id             bigint unsigned                                                                 not null comment '老人ID',
    activity_id          bigint unsigned                                                                 not null comment '活动ID',
    participation_status enum ('REGISTERED', 'ATTENDED', 'ABSENT', 'CANCELED') default 'REGISTERED'      not null comment '参与状态',
    created_at           datetime                                              default CURRENT_TIMESTAMP not null,
    activity_date_start  time                                                                            null comment '活动开始时间',
    activity_date_end    time                                                                            null comment '活动结束时间',
    activity_date        date                                                                            null comment '活动日期',
    constraint uk_elder_activity
        unique (elder_id, activity_id),
    constraint fk_participation_activity
        foreign key (activity_id) references activity_schedule (id)
            on delete cascade,
    constraint fk_participation_elder
        foreign key (elder_id) references elderly_user (id)
            on delete cascade
)
    comment '活动参与记录表';

create index idx_participation_elder_date
    on activity_participation (elder_id, activity_date);

create index idx_elderly_home
    on elderly_user (nursing_home_id);

create table medical_duty
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    nursing_home_id bigint unsigned                       not null comment '所属养老院ID',
    duty_date       date                                  not null comment '值班日期',
    staff_name      varchar(64)                           not null comment '值班人员姓名',
    staff_role      enum ('DOCTOR', 'NURSE', 'CAREGIVER') not null comment '职位',
    duty_time       varchar(32)                           null comment '值班时段(如: 08:00-16:00)',
    phone           varchar(32)                           null comment '紧急联系电话',
    created_at      datetime default CURRENT_TIMESTAMP    not null,
    constraint fk_duty_home
        foreign key (nursing_home_id) references nursing_home (id)
            on delete cascade
)
    comment '医疗值班表';

create index idx_duty_home_date
    on medical_duty (nursing_home_id, duty_date);

create table menu
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    nursing_home_id bigint unsigned                                not null comment '所属养老院ID',
    meal_date       date                                           not null comment '菜单日期',
    meal_type       enum ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK') not null comment '餐次类型',
    dish_name       varchar(128)                                   not null comment '菜品名称',
    nutrition_notes varchar(255)                                   null comment '营养说明(如: 低盐, 清淡)',
    created_at      datetime default CURRENT_TIMESTAMP             not null,
    constraint fk_menu_home
        foreign key (nursing_home_id) references nursing_home (id)
            on delete cascade
)
    comment '每日菜单表';

create index idx_menu_date_type
    on menu (meal_date, meal_type);

create index idx_menu_home_date
    on menu (nursing_home_id, meal_date);

create table visit_appointment
(
    id            bigint unsigned auto_increment comment '主键ID'
        primary key,
    elder_id      bigint unsigned                                                            not null comment '被看望老人ID',
    visitor_name  varchar(64)                                                                not null comment '来访人姓名',
    visitor_phone varchar(32)                                                                null comment '来访人联系电话',
    visit_time    datetime                                                                   not null comment '预约看望时间',
    status        enum ('PENDING', 'APPROVED', 'CANCELED', 'DONE') default 'PENDING'         not null comment '预约状态',
    created_at    datetime                                         default CURRENT_TIMESTAMP not null,
    relation      varchar(50)                                                                not null comment '与老人的关系',
    constraint fk_visit_elder
        foreign key (elder_id) references elderly_user (id)
            on delete cascade
)
    comment '看望老人预约表';

create index idx_visit_elder_time
    on visit_appointment (elder_id, visit_time);

