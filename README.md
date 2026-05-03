# Campus Lost & Found 校园失物招领系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-42b883)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/license-MIT-yellow)](LICENSE)

> 一个面向校园场景的失物招领平台，致力于帮助师生快速有效地发布寻物启事和失物招领信息，解决校园内物品丢失后的信息不对称问题。

## 项目简介

传统的失物招领方式存在信息分散、联系方式暴露、时间地点描述不准确、缺乏有效匹配机制等问题。本项目通过数字化平台化方式，提供统一的失物招领信息发布与检索平台，AI图像识别自动生成物品标签提高匹配精度，关键词订阅与邮件通知实现智能匹配推送，隐私保护机制隐藏真实联系方式。

## 功能特性

### 核心功能

- [x] **用户管理** - 用户注册与登录（邮箱验证码）、个人信息管理
- [x] **帖子发布** - 寻物启事（LOST）和失物招领（FOUND）发布
- [x] **图片上传** - 支持图片上传，自动生成唯一文件名
- [x] **AI图像识别** - 集成火山引擎 ARK API，自动生成物品标签辅助检索
- [x] **综合检索** - 支持按类型、关键词、建筑、日期范围检索
- [x] **相似匹配** - AI智能推荐相似帖子，提高失物找回概率
- [x] **评论互动** - 树形评论结构、点赞互动、图片评论
- [x] **即时通讯** - WebSocket 实时聊天 + REST API 双通道
- [x] **关键词订阅** - 订阅关键词 + 区域，有新匹配时邮件通知
- [x] **站内消息** - 订阅通知、评论通知、系统消息
- [x] **隐私保护** - 验证问题机制隐藏真实联系方式
- [x] **管理后台** - 仪表盘统计、帖子管理、消息管理、审计日志

### 技术亮点

- **AI 图像识别**：上传图片自动识别物品特征，生成3-5个中文标签
- **实时通信**：WebSocket 实现消息秒传，支持页面无刷新聊天
- **智能匹配**：基于关键词和建筑物的订阅推送，精准匹配需求
- **权限控制**：JWT Token 认证 + 管理员权限分层
- **审计日志**：所有管理操作全程记录，支持追溯审查

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.3.0 | 后端核心框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架，强大的 CRUD 能力 |
| MySQL | 8.0 | 关系型数据库 |
| JWT (jjwt) | 0.9.1 | Token 认证 |
| Spring Mail | - | 邮件发送服务 |
| Spring WebSocket | - | 实时通信支持 |
| 火山引擎 ARK API | - | AI 图像识别 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.x | 渐进式 JavaScript 框架 |
| Vite | - | 新一代前端构建工具 |
| Pinia | 3.x | 状态管理库 |
| vue3-storage | - | 本地存储封装 |
| SockJS + STOMP | - | WebSocket 客户端 |

### 环境要求

- JDK 21+
- Node.js 20+
- MySQL 8.0+

## 项目结构

```
Campus-Lost-Found/
├── src/main/java/cn/edu/cug/campuslostfound/
│   ├── config/                 # 配置类
│   │   ├── WebConfig.java      # Web 配置
│   │   └── WebSocketConfig.java # WebSocket 配置
│   ├── controller/              # 控制器层
│   │   ├── AdminController.java        # 管理员接口
│   │   ├── AppCommentController.java   # 评论接口
│   │   ├── CampusBuildingController.java # 建筑字典
│   │   ├── ChatController.java          # 聊天接口
│   │   ├── ChatWebSocketController.java # WebSocket
│   │   ├── FileUploadController.java     # 文件上传
│   │   ├── ItemPostController.java       # 帖子接口
│   │   ├── MessageController.java        # 消息接口
│   │   ├── PostSubscriptionController.java # 订阅接口
│   │   └── UserController.java           # 用户接口
│   ├── entity/                 # 实体类
│   │   ├── AdminLog.java
│   │   ├── AppComment.java
│   │   ├── AppCommentLike.java
│   │   ├── CampusBuilding.java
│   │   ├── ChatMessage.java
│   │   ├── ChatSession.java
│   │   ├── ItemPost.java
│   │   ├── Message.java
│   │   ├── PostSubscription.java
│   │   └── User.java
│   ├── interceptor/            # 拦截器
│   │   ├── AdminInterceptor.java         # 管理员权限校验
│   │   └── AuthenticationInterceptor.java # 认证校验
│   ├── mapper/                # MyBatis Mapper
│   │   ├── AdminLogMapper.java
│   │   ├── AppCommentLikeMapper.java
│   │   ├── AppCommentMapper.java
│   │   ├── CampusBuildingMapper.java
│   │   ├── ChatMessageMapper.java
│   │   ├── ChatSessionMapper.java
│   │   ├── ItemPostMapper.java
│   │   ├── MessageMapper.java
│   │   ├── PostSubscriptionMapper.java
│   │   └── UserMapper.java
│   ├── service/               # 业务服务层
│   │   ├── impl/              # 服务实现
│   │   ├── AdminService.java
│   │   ├── AiRecognitionService.java  # AI 识别服务
│   │   ├── AppCommentService.java
│   │   ├── ChatMessageService.java
│   │   ├── ChatSessionService.java
│   │   ├── EmailService.java
│   │   ├── ItemPostService.java
│   │   ├── MessageService.java
│   │   ├── SubscriptionService.java
│   │   └── UserService.java
│   ├── utils/                 # 工具类
│   │   └── JwtUtils.java      # JWT 工具
│   └── CampusLostFoundApplication.java
├── src/main/resources/
│   ├── application.yaml       # 应用配置
│   └── schema.sql            # 数据库 Schema
├── frontend/                  # Vue 3 前端
│   ├── src/
│   │   ├── assets/           # 静态资源
│   │   ├── components/       # 公共组件
│   │   ├── stores/           # Pinia 状态管理
│   │   ├── views/           # 页面视图
│   │   ├── App.vue          # 根组件
│   │   └── main.js          # 入口文件
│   ├── public/               # 公共资源
│   ├── package.json
│   └── vite.config.js
├── src/test/                 # 测试资源
├── pom.xml                   # Maven 配置
└── README.md
```

## 数据库设计

本系统共设计 **10 张数据表**：

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `app_user` | 用户表 | id, username, password, email, nickname, avatar, role |
| `item_post` | 帖子表 | id, type, title, description, imageUrl, buildingId, verifyQuestion |
| `chat_session` | 聊天会话表 | id, user1Id, user2Id, lastMsgId |
| `chat_message` | 聊天消息表 | id, sessionId, senderId, receiverId, content |
| `app_comment` | 评论表 | id, postId, publisherId, parentCommentId, rootCommentId |
| `app_message` | 站内消息表 | id, userId, type, title, content |
| `post_subscription` | 订阅规则表 | id, userId, keyword, buildingId |
| `campus_building` | 建筑字典表 | id, name, aliases, centerLat, centerLng |
| `app_comment_like` | 评论点赞表 | userId, commentId |
| `admin_log` | 管理员日志表 | id, adminId, actionType, targetId |

详细表结构请参考 [数据库文档](数据库文档.md)。

## 快速开始

### 1. 环境准备

确保已安装以下软件：
- JDK 21+
- Node.js 20+
- MySQL 8.0+

### 2. 数据库配置

创建数据库 `campus_lost_found`：

```sql
CREATE DATABASE campus_lost_found DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行数据库初始化脚本：

```bash
mysql -u root -p campus_lost_found < src/main/resources/schema.sql
```

### 3. 修改配置文件

编辑 `src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_lost_found?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码

  mail:
    host: smtp.qq.com
    username: 你的QQ邮箱
    password: 邮箱授权码
```

### 4. 启动后端服务

```bash
./mvnw spring-boot:run
```

后端服务将运行在 http://localhost:8080

### 5. 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将运行在 http://localhost:5173

### 6. 访问系统

打开浏览器访问 http://localhost:5173 即可使用系统。

## API 接口

详细接口文档请参考 [接口文档](接口文档.md)。

### 认证说明

除公开接口外，其他接口均需要在请求头中携带 Token：

```
Authorization: Bearer <token>
```

### 公开接口（无需认证）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/users/login` | POST | 用户登录 |
| `/api/users/register` | POST | 用户注册 |
| `/api/users/send-code` | POST | 发送验证码 |
| `/api/users/list` | GET | 获取用户列表 |
| `/api/posts` | GET | 获取所有帖子 |
| `/api/posts/type/{type}` | GET | 按类型获取帖子 |
| `/api/posts/search` | GET | 综合检索帖子 |
| `/api/comments/post/{postId}` | GET | 获取帖子评论 |
| `/api/buildings` | GET | 获取建筑字典 |
| `/ws` | WebSocket | WebSocket 聊天 |
| `/uploads/**` | GET | 静态资源 |

### 主要业务接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/posts` | POST | 发布帖子 |
| `/api/posts/my` | GET | 获取我的帖子 |
| `/api/posts/{id}` | PUT | 修改帖子 |
| `/api/posts/{id}` | DELETE | 删除帖子 |
| `/api/posts/{id}/recommendations` | GET | 获取相似推荐 |
| `/api/posts/subscribe` | POST | 订阅关键词 |
| `/api/comments` | POST | 发表评论 |
| `/api/comments/{id}/like` | POST | 点赞评论 |
| `/api/chat/messages` | GET | 获取聊天记录 |
| `/api/chat/send` | POST | 发送消息 |
| `/api/messages/my` | GET | 获取我的消息 |

### 管理员接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/dashboard/stats` | GET | 仪表盘统计 |
| `/api/admin/posts/{id}` | DELETE | 删除帖子 |
| `/api/admin/posts/batch` | DELETE | 批量删除 |
| `/api/admin/logs` | GET | 审计日志 |
| `/api/admin/messages` | GET | 所有私信 |

## 系统截图

> TODO: 添加系统截图

## 开发指南

### 前端开发

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build

# 预览构建结果
npm run preview
```

### 接口测试

接口测试文件位于 `src/test/resources/api-test.http`，可使用 JetBrains Http Client 或 VS Code REST Client 插件进行测试。

### 代码规范

- 遵循 Spring Boot 和 Vue.js 官方最佳实践
- 使用 RESTful API 设计规范
- 前端采用 Composition API

## 项目文档

- [设计说明书](设计说明书.md) - 详细的系统设计和模块说明
- [接口文档](接口文档.md) - 完整的 API 接口文档
- [数据库文档](数据库文档.md) - 数据库表结构设计

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0 | 2026-04-01 | 初始版本，基本功能实现 |
| 1.1 | 2026-05-01 | 完善文档，补充各模块详细设计 |

## 贡献指南

欢迎提交 Issue 和 Pull Request。

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- 项目维护者：同方项目组
- 所属单位：中国地质大学（武汉）
