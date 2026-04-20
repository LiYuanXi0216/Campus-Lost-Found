# Campus Lost & Found 校园失物招领系统

一个面向校园场景的失物招领平台，支持寻物启事和失物招领发布。

## 技术栈

**后端**
- Spring Boot 3.3.0
- MyBatis-Plus 3.5.5
- MySQL 8.0
- JWT (jjwt 0.9.1)
- Spring Mail

**前端**
- Vue 3
- Vite

## 功能特性

- [x] 用户注册与登录（邮箱验证码）
- [x] 帖子发布（寻物/招领）
- [x] 图片上传
- [x] AI 图像识别（自动生成物品标签）
- [x] 综合检索（类型/关键词/建筑/日期）
- [x] 关键词订阅与邮件通知
- [x] 站内消息系统
- [x] 管理后台

## 快速开始

### 环境要求

- JDK 21+
- Node.js 20+
- MySQL 8.0+

### 1. 配置数据库

创建数据库 `campus_lost_found`，执行 `src/main/resources/schema.sql` 初始化表结构。

修改 `src/main/resources/application.yaml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_lost_found?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
```

### 2. 启动后端

```bash
./mvnw spring-boot:run
```

后端服务将运行在 http://localhost:8080

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务将运行在 http://localhost:5173

### 4. 邮件配置

在 `application.yaml` 中配置 QQ 邮箱授权码（用于发送订阅通知）：

```yaml
spring:
  mail:
    host: smtp.qq.com
    username: 你的QQ邮箱
    password: 邮箱授权码
```

## 项目结构

```
├── src/main/java/cn/edu/cug/campuslostfound/
│   ├── config/          # 配置类
│   ├── controller/      # 控制器
│   ├── entity/          # 实体类
│   ├── interceptor/     # 拦截器
│   ├── mapper/          # MyBatis 映射器
│   ├── service/         # 业务服务
│   └── utils/           # 工具类
├── frontend/            # Vue 3 前端
├── src/main/resources/
│   ├── application.yaml # 应用配置
│   └── schema.sql       # 数据库 Schema
└── pom.xml
```

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/users/register` | POST | 用户注册 |
| `/api/users/login` | POST | 用户登录 |
| `/api/users/send-code` | POST | 发送验证码 |
| `/api/posts` | GET/POST | 查询/发布帖子 |
| `/api/posts/type/{type}` | GET | 按类型查询 |
| `/api/posts/search` | GET | 综合检索 |
| `/api/posts/subscribe` | POST | 订阅关键词 |
| `/api/messages/my` | GET | 获取消息 |

## 接口文档

测试接口见 `src/test/resources/api-test.http`
