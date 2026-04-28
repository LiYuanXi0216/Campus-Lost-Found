-- 1. 用户表 (保持不变，补充了邮箱和头像)
CREATE TABLE `app_user` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `username` varchar(50) NOT NULL COMMENT '账号',
                            `password` varchar(100) NOT NULL COMMENT '加密密码',
                            `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                            `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
                            `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
                            `role` varchar(20) DEFAULT 'USER' COMMENT '角色: USER/ADMIN',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`),
                            UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 校园建筑物字典表 (解决痛点4：经纬度到建筑的映射)
CREATE TABLE `campus_building` (
                                   `id` bigint NOT NULL AUTO_INCREMENT,
                                   `name` varchar(100) NOT NULL COMMENT '建筑物名称 (如：西区第一食堂)',
                                   `aliases` varchar(255) DEFAULT NULL COMMENT '别名，逗号分隔 (如：一食堂,西一)',
                                   `center_lat` double DEFAULT NULL COMMENT '中心纬度',
                                   `center_lng` double DEFAULT NULL COMMENT '中心经度',
                                   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园建筑数据字典';

-- 插入几条测试字典数据
INSERT INTO `campus_building` (`name`, `aliases`) VALUES
                                                      ('未来图书馆', '图书馆'),
                                                      ('聚英园', '学一食堂'),
                                                      ('崇道楼', '教二楼'),
                                                      ('致知楼', '教一楼'),
                                                      ('崇智楼', '计算机学院'),
                                                      ('弘雅堂', '学生活动中心'),
                                                      ('汇萃园', '学二食堂');

-- 3. 核心帖子表 (解决痛点1、2、3：重构时间与地点字段)
CREATE TABLE `item_post` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `type` varchar(20) NOT NULL COMMENT 'LOST(寻物) / FOUND(招领)',
                             `title` varchar(255) NOT NULL COMMENT '标题',
                             `description` text COMMENT '详细描述(含AI标签)',
                             `contact` varchar(100) COMMENT '联系方式',
                             `image_url` varchar(255) COMMENT '图片URL',
                             `item_status` varchar(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/RESOLVED',
                             `publisher_id` varchar(50) NOT NULL COMMENT '发布者ID',
                             `create_time` datetime NOT NULL COMMENT '系统发帖时间',

    -- 【重构的时间字段】支持区间查询与模糊描述
                             `incident_start_date` date DEFAULT NULL COMMENT '可能发生的起始日期',
                             `incident_end_date` date DEFAULT NULL COMMENT '可能发生的结束日期',
                             `incident_time_desc` varchar(100) DEFAULT NULL COMMENT '模糊时间描述 (如：下午二节课后)',

    -- 【重构的地点字段】字典关联 + 具体描述 + 可选GPS
                             `building_id` bigint DEFAULT NULL COMMENT '关联的建筑物ID (字典表FK)',
                             `location_desc` varchar(255) DEFAULT NULL COMMENT '具体位置描述 (如：二楼靠窗桌子)',
                             `latitude` double DEFAULT NULL COMMENT '发生时的精确纬度(FOUND时可选获取)',
                             `longitude` double DEFAULT NULL COMMENT '发生时的精确经度(FOUND时可选获取)',

                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领帖子表';

-- 4. 订阅规则预留表 (为迭代二的智能匹配做准备)
CREATE TABLE `post_subscription` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint NOT NULL COMMENT '订阅人',
                                     `keyword` varchar(100) NOT NULL COMMENT '订阅关键词 (如：AirPods, 黑色书包)',
                                     `building_id` bigint DEFAULT NULL COMMENT '关注的区域 (可选)',
                                     `is_active` tinyint(1) DEFAULT 1 COMMENT '是否开启推送',
                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅规则表';

-- 1. 全局消息表 (存储所有通知)
CREATE TABLE `app_message` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `user_id` bigint NOT NULL COMMENT '接收者ID',
                               `type` varchar(20) NOT NULL COMMENT '消息类型: SUBSCRIPTION(订阅匹配), COMMENT(评论), PRIVATE(私信)',
                               `title` varchar(100) NOT NULL COMMENT '消息标题',
                               `content` text NOT NULL COMMENT '消息正文',
                               `related_id` bigint DEFAULT NULL COMMENT '关联ID (如帖子ID或私信ID，用于点击跳转)',
                               `is_read` tinyint(1) DEFAULT 0 COMMENT '是否已读 (0-未读, 1-已读)',
                               `create_time` datetime NOT NULL COMMENT '发送时间',
                               PRIMARY KEY (`id`),
                               INDEX `idx_user_read` (`user_id`, `is_read`) -- 优化查询性能
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局站内消息表';

-- 2. 订阅管理增强 (确保索引)
ALTER TABLE `post_subscription` ADD INDEX `idx_user_id` (`user_id`);

-- 会话表：存储用户间的聊天会话
CREATE TABLE `chat_session` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
                                `user1_id` bigint NOT NULL COMMENT '用户1ID',
                                `user2_id` bigint NOT NULL COMMENT '用户2ID',
                                `last_msg_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_user_pair` (`user1_id`, `user2_id`) COMMENT '保证用户对唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 消息表：存储用户间的聊天消息
CREATE TABLE `chat_message` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                                `session_id` bigint NOT NULL COMMENT '会话ID',
                                `sender_id` bigint NOT NULL COMMENT '发送者ID',
                                `receiver_id` bigint NOT NULL COMMENT '接收者ID',
                                `content` varchar(2000) NOT NULL COMMENT '消息内容',
                                `send_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
                                `is_read` tinyint DEFAULT 0 COMMENT '是否已读（0-未读，1-已读）',
                                PRIMARY KEY (`id`),
                                KEY `idx_session_id` (`session_id`),
                                KEY `idx_sender_receiver` (`sender_id`, `receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 5. 评论表（评论与回复共用同一张表，便于统一查询与扩展）
-- 设计思路：
-- 1. 顶级评论和回复都存在同一张表里，避免拆两张表带来的查询和维护复杂度
-- 2. parent_comment_id 表示“我直接回复的是谁”
-- 3. root_comment_id 表示“我属于哪条顶级评论线程”
--
-- 示例：
-- A 发表顶级评论 -> id=10, parent_comment_id=null, root_comment_id=10
-- B 回复 A       -> id=11, parent_comment_id=10,   root_comment_id=10
-- C 回复 B       -> id=12, parent_comment_id=11,   root_comment_id=10
--
-- 这样做的好处是：
-- - 查询整个帖子评论时，只需要一次查出当前 post_id 的全部评论
-- - 再按 root_comment_id 分组，就能快速恢复评论树
-- - 同时也保留了“回复的是谁”的语义，前端可以展示“回复 xxx”
CREATE TABLE `app_comment` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `publisher_id` bigint NOT NULL COMMENT '评论发布者用户ID',
                               `post_id` bigint NOT NULL COMMENT '所依附的帖子ID',
                               `is_reply` tinyint(1) DEFAULT 0 COMMENT '是否为回复：0-顶级评论，1-回复',
                               `parent_comment_id` bigint DEFAULT NULL COMMENT '直接回复的那条评论ID',
                               `root_comment_id` bigint DEFAULT NULL COMMENT '所属顶级评论ID，便于把多层回复归拢在同一评论串下',
                               `content` text COMMENT '评论文字内容',
                               `image_url` varchar(255) DEFAULT NULL COMMENT '评论图片URL（最多一张）',
                               `create_time` datetime NOT NULL COMMENT '发布时间',
                               `like_count` int DEFAULT 0 COMMENT '点赞数',
                               PRIMARY KEY (`id`),
                               INDEX `idx_comment_post` (`post_id`, `create_time`),
                               INDEX `idx_comment_root` (`root_comment_id`, `create_time`),
                               INDEX `idx_comment_parent` (`parent_comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子评论表（评论与回复统一存储）';

-- 6. 评论点赞关系表（同一用户对同一评论只能存在一条点赞记录）
-- 这里不把“谁点过赞”塞进 app_comment 表里，而是单独拆一张关系表，原因有三点：
-- 1. 点赞是典型的多对多关系（一个用户能点赞很多评论，一条评论也能被很多用户点赞）
-- 2. 需要快速判断“当前用户是否点过赞”
-- 3. 后续如果要做点赞记录、点赞通知、点赞排行，单独建表更容易扩展
CREATE TABLE `app_comment_like` (
                                    `user_id` bigint NOT NULL COMMENT '点赞用户ID',
                                    `comment_id` bigint NOT NULL COMMENT '被点赞评论ID',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                    PRIMARY KEY (`user_id`, `comment_id`),
                                    INDEX `idx_comment_like_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞关系表';

CREATE TABLE `admin_log` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `admin_id` bigint NOT NULL COMMENT '执行操作的管理员ID',
                             `action_type` varchar(50) NOT NULL COMMENT '操作类型(如: DELETE_POST)',
                             `target_id` varchar(255) COMMENT '被操作的对象ID(如帖子ID)',
                             `detail` text COMMENT '操作详情描述',
                             `create_time` datetime NOT NULL COMMENT '操作时间',
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作审计日志';

