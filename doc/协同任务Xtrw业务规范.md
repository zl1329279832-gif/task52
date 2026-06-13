# 协同任务（Xtrw）业务规范

> 本文档从 `XtrwServiceImpl`、`XtrwController`、`XtrwMapper.xml` 及相关实体类反推而成，供前后端对齐接口与业务语义。

---

## 1. 数据模型总览

### 1.1 表关系

```
                         ┌──────────────────┐
                         │      xtrw        │  主任务表
                         │  (协同任务主表)    │
                         └──┬───┬───┬───┬───┘
                            │   │   │   │
              ┌─────────────┘   │   │   └─────────────┐
              ▼                 ▼   ▼                 ▼
   ┌──────────────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────────┐
   │  xtrw_xtuser     │ │xtrw_file │ │xtrw_progress │ │xtrw_visible  │
   │  (协办人关联)     │ │(附件)    │ │(进度日志)     │ │  _dept       │
   └──────────────────┘ └──────────┘ └──────────────┘ │(可见部门)     │
                                                      └──────────────┘
                         ┌──────────────────┐
                         │   xtrw_todo      │  逾期待办提醒
                         │  (关联 xtrw)      │
                         └──────────────────┘
```

### 1.2 核心字段说明

| 表 | 字段 | 类型 | 业务含义 |
|---|---|---|---|
| `xtrw` | `xtrwId` | INT (PK, 自增) | 任务唯一标识 |
| `xtrw` | `xtrwTitle` | VARCHAR | 任务标题 |
| `xtrw` | `xtrwDesc` | VARCHAR | 任务描述 |
| `xtrw` | `zfrUserId` | BIGINT | 主负责人用户 ID |
| `xtrw` | `zfrUserName` | VARCHAR | 主负责人姓名（创建时冗余写入） |
| `xtrw` | `jzTime` | DATETIME | 截止时间 |
| `xtrw` | `progress` | INT | 当前进度百分比（0-100） |
| `xtrw` | `status` | INT | 任务状态，见下方状态机 |
| `xtrw` | `isOverdue` | INT | 是否逾期标记 |
| `xtrw` | `summary` | VARCHAR | 关闭汇总（仅关闭时写入） |
| `xtrw` | `createTime` | DATETIME | 创建时间 |
| `xtrw` | `updateTime` | DATETIME | 最后更新时间 |

---

## 2. Magic Number 字典

### 2.1 xtrw.status — 任务状态

| 值 | 含义 | 谁触发 | 代码位置 |
|---|---|---|---|
| **0** | 进行中 | 系统——创建时自动设置 | `XtrwServiceImpl.java:37` |
| **1** | 已完成 | 系统——进度 ≥ 100 时自动流转 | `XtrwServiceImpl.java:135` |
| **2** | 已关闭 | 主负责人手动调用 `closeXtrw` | `XtrwServiceImpl.java:152` |

### 2.2 xtrw.progress — 任务进度

| 值 | 含义 |
|---|---|
| **0** | 初始值（创建时） |
| **1 ~ 99** | 进行中，可由负责人或协办人多次更新 |
| **≥ 100** | 触发自动完成（status → 1） |

> **注意**：进度是绝对值替换，不是增量累加。每次 `updateProgress` 会用新值覆盖旧值，历史记录保留在 `xtrw_progress` 表中。

### 2.3 xtrw.isOverdue — 逾期标记

| 值 | 含义 | 代码位置 |
|---|---|---|
| **0** | 未逾期（默认） | `XtrwServiceImpl.java:38` |
| **1** | 已逾期（单向标记，不可回退） | `XtrwMapper.xml:101-103` |

### 2.4 xtrw_todo.isRead — 待办已读标记

| 值 | 含义 | 代码位置 |
|---|---|---|
| **0** | 未读（创建时默认） | `XtrwServiceImpl.java:205` |
| **1** | 已读 | `XtrwServiceImpl.java:249` |

### 2.5 gsgg.isZs — 公告展示标记

| 值 | 含义 | 代码位置 |
|---|---|---|
| **1** | 逾期公告需展示 | `XtrwServiceImpl.java:230` |

---

## 3. 状态机

### 3.1 任务生命周期状态机

```
  ┌─────────────────────────────────────────────────────────────────┐
  │                        创建任务 addXtrw                         │
  │              status=0, progress=0, isOverdue=0                  │
  └────────────────────────────┬────────────────────────────────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   status = 0        │
                    │   进行中             │◄──────────────────────┐
                    └──┬──────┬───────┬───┘                       │
                       │      │       │                           │
          ┌────────────┘      │       └────────────┐              │
          ▼                   ▼                    ▼              │
  ┌───────────────┐  ┌────────────────┐   ┌────────────────┐     │
  │ updateProgress│  │ 截止时间到期    │   │  closeXtrw     │     │
  │ (负责人/协办人)│  │ (惰性检测)     │   │ (仅主负责人)    │     │
  └───────┬───────┘  └───────┬────────┘   └───────┬────────┘     │
          │                  │                    │               │
          ▼                  ▼                    ▼               │
  progress < 100?     isOverdue: 0→1        status: 0→2          │
   ├─ 是 ───────────────────────────────────────────────┘         │
   │                  + 生成 XtrwTodo         已关闭              │
   └─ 否 (≥100)      + 仅通知主负责人        + 写入 summary      │
          │                                                       │
          ▼                                                       │
    ┌───────────────┐                                             │
    │  status = 1   │                                             │
    │  已完成 (自动) │                                             │
    └───────────────┘                                             │
```

### 3.2 关键流转规则

| 起始状态 | 事件 | 目标状态 | 条件 | 副作用 |
|---------|------|---------|------|--------|
| status=0 | `updateProgress(p)` 且 p < 100 | status=0 | 调用者是负责人或协办人 | 写入 `xtrw_progress` 日志 |
| status=0 | `updateProgress(p)` 且 p ≥ 100 | **status=1** | 调用者是负责人或协办人 | 写入日志 + 自动完成 |
| status=0 | `closeXtrw` | **status=2** | 调用者 **必须是主负责人** | 写入 `summary` |
| status=0 | `jzTime < NOW()` 且 `isOverdue=0` | status=0, **isOverdue=1** | 惰性检测（查询时触发） | 生成 `XtrwTodo` 给主负责人 |

### 3.3 不可逆特性

- `isOverdue` 一旦置为 **1**，不会回退为 0。已逾期的任务即便后续完成或关闭，逾期标记仍保留。
- `status=1`（已完成）和 `status=2`（已关闭）为终态，代码中无回退到 `status=0` 的逻辑。
- 已完成的任务仍可被主负责人 `closeXtrw` 操作覆盖为 `status=2`（代码未显式阻止）。

---

## 4. 角色权限矩阵

系统中 Xtrw 涉及两种角色，**没有显式 role 字段**，角色由用户 ID 出现的位置决定：

- **主负责人**：`xtrw.zfrUserId` 记录的用户
- **协办人**：`xtrw_xtuser.xtUserId` 记录的用户

| 操作 | 主负责人 | 协办人 | 其他用户 | 鉴权代码位置 |
|------|:-------:|:------:|:-------:|---|
| 创建任务 `addXtrw` | -- | -- | 任意登录用户均可创建 | 无鉴权 |
| 查看详情 `xtrwDetail` | YES | YES | YES（无鉴权） | 无鉴权 |
| 更新进度 `updateProgress` | **YES** | **YES** | **NO** — 抛异常 | `XtrwServiceImpl.java:107-117` |
| 关闭任务 `closeXtrw` | **YES** | **NO** — 抛异常 | **NO** — 抛异常 | `XtrwServiceImpl.java:148-149` |
| 上传附件 `uploadFile` | YES | YES | YES（无鉴权） | 无鉴权 |
| 查看"我负责的" `myOwned` | 按 `zfrUserId` 过滤 | — | — | 无鉴权，传谁的 ID 看谁的 |
| 查看"我协办的" `myCollab` | — | 按 `xtUserId` 过滤 | — | 无鉴权 |
| 生成逾期公告 `generateAnnouncement` | YES | YES | YES（无鉴权） | 无鉴权 |
| 标记待办已读 `markTodoRead` | YES | — | — | 无鉴权（但 todo 只发给负责人） |

> **安全提示**：除 `updateProgress` 和 `closeXtrw` 外，其余接口均无服务端鉴权。前端通过 `sessionStorage` 传递 `userId`，后端直接信任，存在越权风险。

---

## 5. XtrwXtUser（协办人）写入规则

**写入时机**：仅在 `addXtrw` 创建任务时一次性写入，后续不可增删。

**写入流程**（`XtrwServiceImpl.java:52-65`）：

```
前端传入 xtUserIds = "3,5,7"  (逗号分隔的用户ID字符串)
         │
         ▼
    按 "," 分割为数组 ["3", "5", "7"]
         │
         ▼  对每个 userId:
    ┌────┴────────────────────────────┐
    │ 1. userMapper.getUser(userId)   │ ← 查询用户表获取姓名
    │ 2. 构造 XtrwXtUser 对象:       │
    │    - xtrwId = 新生成的任务 ID    │
    │    - xtUserId = 当前用户 ID     │
    │    - xtUserName = 查到的姓名    │
    │ 3. xtrwMapper.addXtUser(xtUser) │ ← INSERT
    └─────────────────────────────────┘
```

**XtrwXtUser 表结构**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT (PK, 自增) | 记录 ID |
| `xtrwId` | INT (FK) | 关联任务 ID |
| `xtUserId` | BIGINT | 协办人用户 ID |
| `xtUserName` | VARCHAR | 协办人姓名（冗余快照，不随用户改名更新） |

---

## 6. XtrwVisibleDept（可见部门）写入规则

**写入时机**：仅在 `addXtrw` 创建任务时一次性写入，后续不可增删。

**写入流程**（`XtrwServiceImpl.java:68-81`）：

```
前端传入 visibleDeptIds = "1,2"  (逗号分隔的部门ID字符串)
         │
         ▼
    按 "," 分割为数组 ["1", "2"]
         │
         ▼  对每个 departmentId:
    ┌────┴──────────────────────────────────────┐
    │ 1. departmentMapper.getDeptById(deptId)    │ ← 查询部门表获取名称
    │ 2. 构造 XtrwVisibleDept 对象:              │
    │    - xtrwId = 新生成的任务 ID              │
    │    - departmentId = 当前部门 ID            │
    │    - departmentName = 查到的部门名称        │
    │ 3. xtrwMapper.addVisibleDept(vd)           │ ← INSERT
    └───────────────────────────────────────────┘
```

**XtrwVisibleDept 表结构**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT (PK, 自增) | 记录 ID |
| `xtrwId` | INT (FK) | 关联任务 ID |
| `departmentId` | INT | 可见部门 ID |
| `departmentName` | VARCHAR | 部门名称（冗余快照） |

**查询用途**：`/xtrw/visibleTasks?departmentId=X` 通过 JOIN `xtrw_visible_dept` 查出该部门可见的所有任务。

---

## 7. 逾期待办（XtrwTodo）生成机制

### 7.1 触发方式：惰性检测（非定时任务）

系统**没有** `@Scheduled` 定时扫描。逾期检测通过"搭便车"方式，在以下 5 个查询方法被调用时触发：

| 方法 | 代码位置 |
|------|---------|
| `getMyOwnedTasks()` | `XtrwServiceImpl.java:163` |
| `getMyCollabTasks()` | `XtrwServiceImpl.java:169` |
| `getOverdueTasks()` | `XtrwServiceImpl.java:175` |
| `getCompletedTasks()` | `XtrwServiceImpl.java:181` |
| `getVisibleTasksByDeptId()` | `XtrwServiceImpl.java:187` |

> **局限**：如果没有用户查询任务列表，逾期任务将不会被检测到，也不会生成待办提醒。

### 7.2 检测 SQL

```sql
-- XtrwMapper.xml:97-99
SELECT * FROM xtrw
WHERE status = 0          -- 仅"进行中"的任务
  AND isOverdue = 0       -- 尚未被标记为逾期
  AND jzTime < NOW()      -- 截止时间已过
```

### 7.3 处理流程（`checkAndGenerateOverdueTodos`，行 192-212）

```
对每条新检测到的逾期任务:
    │
    ├─ 1. markOverdue(xtrwId)         ← isOverdue 置为 1
    │
    └─ 2. 生成 XtrwTodo:
         - xtrwId    = 任务 ID
         - xtrwTitle = 任务标题（冗余）
         - userId    = zfrUserId（★ 仅通知主负责人，协办人不收到）
         - isRead    = 0（未读）
         - createTime = NOW()
```

- 每条逾期任务的处理独立 try-catch，单条失败不阻塞其余任务。
- 错误仅 `e.printStackTrace()`，无持久化告警。

### 7.4 逾期公告生成（`generateAnnouncement`，行 215-240）

手动触发（需调用 `/xtrw/generateAnnouncement?xtrwId=X`），生成一条公司公告（`gsgg` 表）：

- 标题格式：`【任务逾期提醒】{任务标题}`
- 内容格式：`任务【{标题}】已逾期，截止时间：{jzTime}，当前进度：{progress}%，负责人：{zfrUserName}。请尽快处理！`
- `isZs = 1`
- 生成后将公告 ID 回写到对应 `XtrwTodo.ggId`（取 `LIMIT 1` 的第一条 todo）

---

## 8. API 接口清单

| 路径 | 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| `/xtrw/addXtrw` | POST | Xtrw 字段 + `xtUserIds`(逗号分隔) + `visibleDeptIds`(逗号分隔) | `{success, msg}` | 创建任务 |
| `/xtrw/xtrwDetail` | GET | `xtrwId` | Xtrw JSON（含嵌套 list） | 任务详情 |
| `/xtrw/updateProgress` | GET | `xtrwId, userId, progress, note` | `{success, msg}` | 更新进度 |
| `/xtrw/closeXtrw` | GET | `xtrwId, zfrUserId, summary` | `{success, msg}` | 关闭任务 |
| `/xtrw/uploadFile` | POST | `xtrwId, uploadUserId, file`(multipart) | `{success, msg}` | 上传附件 |
| `/xtrw/myOwned` | GET | `zfrUserId` | JSONArray | 我负责的任务 |
| `/xtrw/myCollab` | GET | `xtUserId` | JSONArray | 我协办的任务 |
| `/xtrw/overdue` | GET | 无 | JSONArray | 所有逾期任务 |
| `/xtrw/completed` | GET | 无 | JSONArray | 已完成 + 已关闭任务 |
| `/xtrw/visibleTasks` | GET | `departmentId` | JSONArray | 按部门可见任务 |
| `/xtrw/generateAnnouncement` | GET | `xtrwId` | `{success, msg, ggId}` | 生成逾期公告 |
| `/xtrw/todoList` | GET | `userId` | JSONArray | 待办列表 |
| `/xtrw/markTodoRead` | GET | `todoId` | `{success, msg}` | 标记已读 |

> **注意**：前端 `api.js` 中尚未添加 Xtrw 相关接口定义，后端 Controller 已就绪。

---

## 9. 已知设计问题

| 编号 | 问题 | 影响 |
|------|------|------|
| 1 | `addXtrw` 未加 `@Transactional` | 插入主表成功但协办人/部门写入失败时，会产生孤立记录 |
| 2 | 协办人和可见部门创建后不可修改 | 业务变更时无法调整人员和可见范围 |
| 3 | 姓名/部门名冗余快照不同步 | 用户或部门改名后，Xtrw 记录仍显示旧名 |
| 4 | 逾期检测为惰性触发 | 长时间无人查询时逾期任务不被发现 |
| 5 | 附件存储路径硬编码 `D:/test/xtrw/` | 仅适用于开发环境 |
| 6 | 多数接口无服务端鉴权 | `userId` 由前端传入，可被伪造 |
