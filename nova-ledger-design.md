# Nova-Ledger 记账系统 · 设计文档

> 基于 nova-task 项目框架扩展，Spring Boot 3.4.5 + JDK 21 + MySQL + Vue3
> 设计日期：2026-05-29

---

## 1. 概述

### 1.1 项目定位
个人 / 家庭记账系统，从 nova-task 的微服务架构项目中剥离为独立项目 `nova-ledger`，保留现有的认证体系（JWT + Spring Security + Redis），在原有代码风格和分层结构上做功能扩展。

### 1.2 技术栈

| 层 | 技术 | 说明 |
|---|---|---|
| 后端框架 | Spring Boot 3.4.5 + JDK 21 | 沿用 nova-task 配置 |
| ORM | Spring Data JPA (Hibernate) | 沿用 |
| 数据库 | MySQL 8.x | 新库 `nova_ledger` |
| 缓存 | Redis | 沿用，用于 JWT token + 统计缓存 |
|认证 | JWT + Spring Security | 沿用 nova-task 实现 |
| 前端 | Vue 3 + Vite + Element Plus / Naive UI | 新建项目 |
| 部署 | Docker Compose | 后期 |

### 1.3 设计原则
- **模块化**：功能按模块分包，互不耦合
- **RESTful**：接口风格统一
- **数据安全**：所有接口鉴权，数据按用户隔离
- **渐进增强**：第一阶段做核心功能，第二阶段上增强模块

---

## 2. 数据库设计

### 2.1 命名规范
- 表名：`ledger_` 前缀 + 业务名（复数）
- 字段：snake_case
- 主键：`id` BIGINT AUTO_INCREMENT
- 时间字段：`created_at`、`updated_at`，统一 LocalDateTime
- 软删除：`deleted` tinyint(1) default 0

### 2.2 核心表

#### `ledger_users`
沿用 `User` 实体结构，新增字段可选。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| username | VARCHAR(50) UNIQUE | |
| password | VARCHAR(255) | bcrypt 加密 |
| email | VARCHAR(100) | |
| avatar | VARCHAR(255) | 头像 URL |
| created_at | DATETIME | |
| updated_at | DATETIME | |

#### `ledger_books` — 账本
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | 所属用户 |
| name | VARCHAR(100) | 账本名称，如"日常"、"旅行" |
| description | VARCHAR(255) | 描述 |
| currency | VARCHAR(10) DEFAULT 'CNY' | 币种 |
| cover_color | VARCHAR(20) | 封面颜色 |
| sort_order | INT DEFAULT 0 | 排序 |
| deleted | TINYINT | 软删除 |
| created_at/updated_at | DATETIME | |

#### `ledger_categories` — 分类
树形结构，支持无限层级。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| parent_id | BIGINT FK NULL | 父分类 ID |
| name | VARCHAR(50) | 名称 |
| icon | VARCHAR(100) | 图标标识 |
| color | VARCHAR(20) | 颜色值 |
| type | ENUM('INCOME', 'EXPENSE') | 收入/支出 |
| sort_order | INT | 排序 |
| deleted | TINYINT | |
| created_at/updated_at | DATETIME | |

#### `ledger_accounts` — 账户
代表资金存放处：现金、银行卡、支付宝、微信等。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| book_id | BIGINT FK | 所属账本 |
| name | VARCHAR(50) | 账户名称 |
| type | ENUM('CASH', 'BANK', 'ALIPAY', 'WECHAT', 'CREDIT_CARD', 'INVESTMENT', 'OTHER') | |
| balance | DECIMAL(15,2) DEFAULT 0.00 | 当前余额 |
| initial_balance | DECIMAL(15,2) DEFAULT 0.00 | 初始余额 |
| currency | VARCHAR(10) DEFAULT 'CNY' | |
| icon | VARCHAR(100) | |
| deleted | TINYINT | |
| created_at/updated_at | DATETIME | |

#### `ledger_transactions` — 交易流水（核心表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| book_id | BIGINT FK | 所属账本 |
| account_id | BIGINT FK | 账户 |
| category_id | BIGINT FK NULL | 分类 |
| type | ENUM('INCOME', 'EXPENSE', 'TRANSFER') | |
| amount | DECIMAL(15,2) | 金额（正数） |
| transaction_time | DATETIME | 实际交易时间 |
| note | VARCHAR(500) | 备注 |
| merchant | VARCHAR(100) | 商家 |
| tag_ids | JSON | 关联标签 ID 列表 |
| image_urls | JSON | 凭证图片 |
| bill_id | BIGINT FK NULL | 关联周期账单（非空表示由周期账单生成） |
| deleted | TINYINT | |
| created_at/updated_at | DATETIME | |

#### `ledger_tags` — 标签
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| name | VARCHAR(30) | |
| color | VARCHAR(20) | |
| created_at/updated_at | DATETIME | |

#### `ledger_budgets` — 预算
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| book_id | BIGINT FK | |
| category_id | BIGINT FK NULL | NULL 表示总预算 |
| amount | DECIMAL(15,2) | 预算额度 |
| period | ENUM('MONTHLY', 'WEEKLY', 'YEARLY', 'CUSTOM') | 周期 |
| start_date | DATE | 生效日期 |
| end_date | DATE NULL | 结束日期（不填则持续） |
| notify_threshold | DECIMAL(5,2) | 预警阈值百分比，如 80 表示 80% 时预警 |
| deleted | TINYINT | |
| created_at/updated_at | DATETIME | |

#### `ledger_bills` — 周期账单（固定支出）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| book_id | BIGINT FK | |
| account_id | BIGINT FK | 扣款账户 |
| category_id | BIGINT FK | |
| name | VARCHAR(100) | 订阅名称，如"房租"、"Netflix" |
| amount | DECIMAL(15,2) | |
| frequency | ENUM('MONTHLY', 'YEARLY', 'WEEKLY', 'DAILY', 'CUSTOM') | |
| custom_cron | VARCHAR(50) | 自定义频率（cron 表达式） |
| next_due_date | DATE | 下次到期日 |
| auto_create | TINYINT DEFAULT 1 | 到期是否自动创建交易 |
| deleted | TINYINT | |
| created_at/updated_at | DATETIME | |

#### `ledger_debts` — 债权债务
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| book_id | BIGINT FK | |
| type | ENUM('LEND', 'BORROW') | 借出 / 借入 |
| counterparty | VARCHAR(100) | 对方姓名/昵称 |
| amount | DECIMAL(15,2) | 总金额 |
| repaid_amount | DECIMAL(15,2) DEFAULT 0.00 | 已还金额 |
| note | VARCHAR(500) | |
| due_date | DATE NULL | 到期日 |
| status | ENUM('ACTIVE', 'SETTLED', 'CANCELLED') | |
| deleted | TINYINT | |
| created_at/updated_at | DATETIME | |

### 2.3 ER 关系图（文字描述）

```
User 1 ── N Book
Book 1 ── N Account
Book 1 ── N Category (树形自关联 parent_id)
Book 1 ── N Budget
Book 1 ── N Bill
Book 1 ── N Debt
Book 1 ── N Transaction
Transaction N ── 1 Account
Transaction N ── 1 Category
Transaction N ── N Tag (通过 tag_ids JSON 列表)
User 1 ── N Tag
Bill 1 ── N Transaction (bill_id 关联)
```

---

## 3. API 接口设计

### 3.1 认证模块（复用 nova-task）
`POST /api/auth/register` — 注册
`POST /api/auth/login` — 登录

### 3.2 账本管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ledger/books` | 获取用户的所有账本 |
| POST | `/api/ledger/books` | 创建账本 |
| GET | `/api/ledger/books/{id}` | 获取单个账本详情 |
| PUT | `/api/ledger/books/{id}` | 更新账本 |
| DELETE | `/api/ledger/books/{id}` | 删除账本（软删除） |

### 3.3 分类管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ledger/books/{bookId}/categories` | 获取分类树（树形结构返回） |
| POST | `/api/ledger/books/{bookId}/categories` | 创建分类 |
| PUT | `/api/ledger/books/{bookId}/categories/{id}` | 更新分类 |
| DELETE | `/api/ledger/books/{bookId}/categories/{id}` | 删除分类（有子分类则禁止删除） |

### 3.4 账户管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ledger/books/{bookId}/accounts` | 获取账户列表 |
| POST | `/api/ledger/books/{bookId}/accounts` | 创建账户 |
| PUT | `/api/ledger/books/{bookId}/accounts/{id}` | 更新账户（余额通过交易自动变化不手动改） |
| DELETE | `/api/ledger/books/{bookId}/accounts/{id}` | 删除账户 |

### 3.5 交易流水（核心接口）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ledger/books/{bookId}/transactions` | 分页查询（支持日期范围、分类、账户、类型、金额范围、关键词搜索） |
| POST | `/api/ledger/books/{bookId}/transactions` | 创建交易 |
| GET | `/api/ledger/books/{bookId}/transactions/{id}` | 获取详情 |
| PUT | `/api/ledger/books/{bookId}/transactions/{id}` | 更新交易 |
| DELETE | `/api/ledger/books/{bookId}/transactions/{id}` | 删除交易（软删除，更新账户余额） |

查询参数：`page`, `size`, `startDate`, `endDate`, `categoryId`, `accountId`, `type`, `minAmount`, `maxAmount`, `keyword`, `tagIds`, `sortBy`, `sortDir`

### 3.6 标签管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ledger/tags` | 获取标签列表 |
| POST | `/api/ledger/tags` | 创建标签 |
| PUT | `/api/ledger/tags/{id}` | 更新标签 |
| DELETE | `/api/ledger/tags/{id}` | 删除标签 |

### 3.7 预算管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ledger/books/{bookId}/budgets` | 获取预算列表 |
| POST | `/api/ledger/books/{bookId}/budgets` | 创建预算 |
| PUT | `/api/ledger/books/{bookId}/budgets/{id}` | 更新预算 |
| DELETE | `/api/ledger/books/{bookId}/budgets/{id}` | 删除预算 |
| GET | `/api/ledger/books/{bookId}/budgets/overview` | 预算执行概览（已花 / 总额 / 剩余） |

### 3.8 周期账单
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ledger/books/{bookId}/bills` | 列表 |
| POST | `/api/ledger/books/{bookId}/bills` | 创建 |
| PUT | `/api/ledger/books/{bookId}/bills/{id}` | 更新 |
| DELETE | `/api/ledger/books/{bookId}/bills/{id}` | 删除 |

### 3.9 债权债务
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ledger/books/{bookId}/debts` | 列表 |
| POST | `/api/ledger/books/{bookId}/debts` | 创建 |
| PUT | `/api/ledger/books/{bookId}/debts/{id}` | 更新（含还款记录） |
| DELETE | `/api/ledger/books/{bookId}/debts/{id}` | 删除 |

### 3.10 统计报表
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ledger/books/{bookId}/stats/overview` | 总览（本月收支、结余、对比上月） |
| GET | `/api/ledger/books/{bookId}/stats/category` | 分类汇总（饼图数据） |
| GET | `/api/ledger/books/{bookId}/stats/trend` | 趋势（月/周/日按时间序列的收支，折线图数据） |
| GET | `/api/ledger/books/{bookId}/stats/account` | 各账户余额 |
| GET | `/api/ledger/books/{bookId}/stats/net-worth` | 净资产变化趋势 |

### 3.11 导入导出
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ledger/books/{bookId}/export` | 导出（CSV/Excel，支持时间范围选择） |
| POST | `/api/ledger/books/{bookId}/import` | 导入 CSV/Excel（字段映射） |

---

## 4. 后端项目结构

```
com.nova.ledger
├── NovaLedgerApplication.java
├── config/
│   ├── JwtAuthFilter.java        (复用 nova-task)
│   ├── JwtUtil.java              (复用)
│   ├── SecurityConfig.java       (复用)
│   ├── RedisConfig.java          (复用)
│   ├── CorsConfig.java           (复用)
│   ├── GlobalExceptionHandler.java (复用)
│   └── JacksonConfig.java        (新增：序列化配置)
├── entity/
│   ├── User.java                 (复用)
│   ├── Book.java                 (账本)
│   ├── Category.java             (分类)
│   ├── Account.java              (账户)
│   ├── Transaction.java          (交易)
│   ├── Tag.java                  (标签)
│   ├── Budget.java               (预算)
│   ├── Bill.java                 (周期账单)
│   └── Debt.java                 (债权债务)
├── repository/
│   ├── (每个实体对应一个 Repository)
├── dto/
│   ├── ApiResponse.java          (复用)
│   ├── request/                  (请求 DTO)
│   │   ├── BookRequest.java
│   │   ├── CategoryRequest.java
│   │   ├── AccountRequest.java
│   │   ├── TransactionRequest.java
│   │   ├── TransactionQuery.java (查询参数封装)
│   │   ├── BudgetRequest.java
│   │   ├── BillRequest.java
│   │   └── DebtRequest.java
│   └── response/                 (响应 DTO)
│       ├── BookVO.java
│       ├── CategoryVO.java       (含子分类列表)
│       ├── TransactionVO.java
│       ├── BudgetOverviewVO.java
│       └── StatsVO.java          (各类统计视图)
├── service/
│   ├── BookService.java
│   ├── CategoryService.java
│   ├── AccountService.java
│   ├── TransactionService.java   (核心，含余额更新逻辑)
│   ├── TagService.java
│   ├── BudgetService.java
│   ├── BillService.java
│   ├── DebtService.java
│   └── StatsService.java
├── controller/
│   ├── BookController.java
│   ├── CategoryController.java
│   ├── AccountController.java
│   ├── TransactionController.java
│   ├── TagController.java
│   ├── BudgetController.java
│   ├── BillController.java
│   ├── DebtController.java
│   └── StatsController.java
└── common/                       (工具/通用)
    ├── PageResult.java           (分页结果封装)
    └── Constants.java            (常量)
```

---

## 5. 关键业务逻辑

### 5.1 交易对余额的影响
```
创建交易：
  INCOME  → account.balance += amount
  EXPENSE → account.balance -= amount
  TRANSFER → from_account.balance -= amount, to_account.balance += amount

删除交易（软删除）：
  INCOME  → account.balance -= amount
  EXPENSE → account.balance += amount
  TRANSFER → 反向操作

更新交易（涉及金额或账户变化）：
  需记录原交易快照，先回滚旧交易影响，再应用新交易。
```

关键点：更新交易时要保存旧值，用事务保证原子性。

### 5.2 周期账单自动生成
```
定时任务（Spring @Scheduled / Quartz）：
  每日零点扫描 bills 表，对 next_due_date <= today 且 auto_create=1 的：
  1. 创建一笔 EXPENSE 交易
  2. 更新 bill.next_due_date（按 frequency 计算下一期日期）
```

### 5.3 预算预警
```
查询某预算在 period 时间范围内已发生的支出总和，
与 budget.amount 比较，如果超过 notify_threshold 则返回预警标记。
预算概览接口一次性返回所有 budget 的执行情况和预警状态。
```

### 5.4 分类树查询
```
一次性加载某账本下的所有分类（按 type 区分），
在 Java 层组装为树形结构返回（利用 Stream + 递归）。
采用 @Cacheable 缓存以减少重复查询。
```

---

## 6. 前端（Vue 3）规划

### 6.1 技术选型
- Vue 3 + Composition API + TypeScript
- 路由：Vue Router 4
- 状态管理：Pinia
- UI 组件库：Element Plus（成熟稳定）
- HTTP：Axios + 请求拦截（自动带 token）
- 图表：ECharts（报表统计）
- 构建：Vite

### 6.2 页面结构
```
pages/
├── Layout.vue                    (主布局：侧边栏 + 顶栏 + 内容区)
├── Login.vue
├── Register.vue
├── Dashboard.vue                 (首页仪表盘：本月概览、预览图表)
├── transactions/
│   ├── TransactionList.vue       (交易流水列表 - 核心页面)
│   ├── TransactionForm.vue       (新增/编辑交易弹窗)
├── accounts/
│   └── AccountList.vue           (账户管理)
├── categories/
│   └── CategoryManager.vue       (分类管理，树形展示)
├── budgets/
│   └── BudgetList.vue            (预算列表 + 概览)
├── bills/
│   └── BillList.vue              (周期账单管理)
├── debts/
│   └── DebtList.vue              (债权债务)
├── stats/
│   ├── ReportPage.vue            (统计报表页面，多种图表)
│   └── ExportImport.vue          (导入导出)
├── settings/
│   ├── ProfileSetting.vue        (个人设置)
│   ├── BookSetting.vue           (账本管理)
│   └── TagManager.vue            (标签管理)
```

### 6.3 设计要点
- **多账本切换**：顶栏/侧边栏显示当前账本，下拉切换
- **交易录入快捷入口**：Dashboard 上浮窗，快速记一笔
- **日期选择器**：预设"今天"、"本周"、"本月"、"上月"、"自定义"
- **金额输入**：数字键盘优化体验
- **余额实时更新**：交易操作后自动刷新账户余额

---

## 7. 第一阶段开发计划

### Sprint 1 — 基础框架（2-3天）
- 新建项目 `nova-ledger`（从 nova-task 复制框架）
- 搭建数据库，应用 application.yml
- 完成：User 复用 + Book / Account / Category / Transaction 实体 + 基础 CRUD

### Sprint 2 — 核心记账功能（3-4天）
- 交易流水增删改查（含分页、筛选、搜索）
- 账户余额联动
- 分类树实现
- 标签管理

### Sprint 3 — 增强功能（3-4天）
- 预算管理 + 预警
- 周期账单 + 定时任务
- 债权债务
- 统计报表（Service 层 + 图表接口）

### Sprint 4 — 前端联调（4-5天）
- 新建 Vue 3 前端项目
- 路由、登录注册
- 核心页面：记账列表、仪表盘、账户管理
- 增强页面：预算、账单、报表、导入导出

### Sprint 5 — 打磨收尾（2-3天）
- Docker 镜像 + docker-compose 部署
- 错误处理、边界情况
- 文档完善

---

## 8. 扩展预留

- **多用户共享账本**：加入 BookMember 表，支持家庭共同记账
- **AI 自动分类**：接入 Spring AI，根据备注自动推荐分类
- **OCR 识别**：拍照识别小票
- **汇率换算**：多币种自动换算
- **微信/支付宝账单导入**：解析导出的 CSV

---

*以上为完整设计文档，评审通过后进入 Sprint 1 编码阶段。*
