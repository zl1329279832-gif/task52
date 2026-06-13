# 协同任务 Xtrw 业务规范

> 本文档从后端代码反推，供前后端对齐字段含义与业务规则。
>
> 代码依据：`XtrwServiceImpl.java`、`XtrwController.java`、`XtrwMapper.xml`、实体类 `Xtrw` / `XtrwXtUser` / `XtrwVisibleDept` / `XtrwProgress` / `XtrwTodo` / `XtrwFile`。

---

## 1. 数据模型总览

| 数据库表 | Java 实体 | 作用 |
|----------|-----------|------|
| `xtrw` | `Xtrw` | 协同任务主表 |
| `xtrw_xtuser` | `XtrwXtUser` | 协办人关联表（多对多） |
| `xtrw_visible_dept` | `XtrwVisibleDept` | 可见部门关联表（多对多） |
| `xtrw_progress` | `XtrwProgress` | 进度更新日志 |
| `xtrw_file` | `XtrwFile` | 附件 |
| `xtrw_todo` | `XtrwTodo` | 逾期待办提醒 |

### 1.1 Xtrw 主表字段

| 字段 | Java 类型 | DB 类型 | 说明 |
|------|-----------|---------|------|
| `xtrwId` | `int` | `INT AUTO_INCREMENT` | 主键 |
| `xtrwTitle` | `String` | `VARCHAR` | 任务标题 |
| `xtrwDesc` | `String` | `TEXT` | 任务描述 |
| `zfrUserId` | `long` | `BIGINT` | **主要负责人**用户 ID（创建者/负责人） |
| `zfrUserName` | `String` | `VARCHAR` | 负责人姓名（冗余，创建时由 Service 查 `user` 表填充） |
| `jzTime` | `Date` | `DATETIME` | 截止时间（截止 = 逾期判断基准） |
| `createTime` | `Date` | `DATETIME` | 创建时间（`addXtrw` 中 `new Date()` 写入） |
| `updateTime` | `Date` | `DATETIME` | 更新时间（每次进度/状态变更时 `NOW()`） |
| `progress` | `int` | `INT` | 当前进度百分比，**0 ~ 100** |
| `status` | `int` | `INT` | 任务状态，**见下方状态机** |
| `summary` | `String` | `TEXT` | 关闭时填写的任务总结 |
| `isOverdue` | `int` | `INT` | 是否逾期标记，**见下方逾期规则** |

### 1.2 XtrwXtUser 协办人关联

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `id` | `int` | 自增主键 |
| `xtrwId` | `int` | 关联的任务 ID |
| `xtUserId` | `long` | 协办人用户 ID |
| `xtUserName` | `String` | 协办人姓名（冗余） |

### 1.3 XtrwVisibleDept 可见部门关联

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `id` | `int` | 自增主键 |
| `xtrwId` | `int` | 关联的任务 ID |
| `departmentId` | `int` | 可见部门 ID |
| `departmentName` | `String` | 部门名称（冗余） |

### 1.4 XtrwProgress 进度日志

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `progressId` | `int` | 自增主键 |
| `xtrwId` | `int` | 关联的任务 ID |
| `userId` | `long` | 更新人 ID |
| `userName` | `String` | 更新人姓名（冗余） |
| `progress` | `int` | 本次更新后的进度值（0-100） |
| `note` | `String` | 更新备注 |
| `updateTime` | `Date` | 更新时间 |

### 1.5 XtrwTodo 逾期待办提醒

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `todoId` | `int` | 自增主键 |
| `xtrwId` | `int` | 关联的任务 ID |
| `xtrwTitle` | `String` | 任务标题（冗余） |
| `userId` | `long` | 接收人（= 负责人 zfrUserId） |
| `createTime` | `Date` | 待办创建时间 |
| `isRead` | `int` | 是否已读：**0 = 未读，1 = 已读** |
| `ggId` | `Integer` | 关联的公告 ID（可空，生成逾期公告后回填） |

### 1.6 XtrwFile 附件

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `id` | `int` | 自增主键 |
| `xtrwId` | `int` | 关联的任务 ID |
| `fileName` | `String` | 文件名 |
| `filePath` | `String` | 磁盘路径（硬编码 `D:/test/xtrw/`） |
| `uploadTime` | `Date` | 上传时间 |
| `uploadUserId` | `long` | 上传人 ID |

---

## 2. Magic Number 速查表

| 位置 | 字段 | 值 | 业务含义 | 代码出处 |
|------|------|----|----------|----------|
| Xtrw.status | `int` | `0` | **进行中** — 任务刚创建或尚未完结 | `addXtrw()` L36: `xtrw.setStatus(0)` |
| Xtrw.status | `int` | `1` | **已完成** — 进度 ≥100 自动触发 | `updateProgress()` L134: `updateXtrwStatus(id, 1, null)` |
| Xtrw.status | `int` | `2` | **已关闭** — 负责人手动关闭 | `closeXtrw()` L152: `updateXtrwStatus(id, 2, summary)` |
| Xtrw.progress | `int` | `0` | 初始进度 | `addXtrw()` L37: `xtrw.setProgress(0)` |
| Xtrw.progress | `int` | `≥100` | 触发自动完成 | `updateProgress()` L134: `if (progress.getProgress() >= 100)` |
| Xtrw.isOverdue | `int` | `0` | **未逾期** — 初始值 / 未触发逾期检测 | `addXtrw()` L38 |
| Xtrw.isOverdue | `int` | `1` | **已逾期** — 逾期检测标记 | `checkAndGenerateOverdueTodos()` L197 |
| XtrwTodo.isRead | `int` | `0` | **未读** — 待办刚生成 | `checkAndGenerateOverdueTodos()` L206 |
| XtrwTodo.isRead | `int` | `1` | **已读** — 用户标记已读 | `markTodoRead()` L249 |
| Gsgg.isZs | `int` | `1` | **展示** — 逾期公告设为可见 | `generateAnnouncement()` L231 |

---

## 3. 状态机

### 3.1 状态定义

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│   ┌──────────┐    progress ≥ 100    ┌──────────┐    │
│   │          │ ───────────────────> │          │    │
│   │ status=0 │                      │ status=1 │    │
│   │  进行中   │                      │  已完成   │    │
│   │          │                      │          │    │
│   └──────────┘                      └──────────┘    │
│        │                                             │
│        │  负责人手动 closeXtrw                        │
│        │  (status=0 → status=2)                      │
│        │                                             │
│        v                                             │
│   ┌──────────┐                                       │
│   │          │                                       │
│   │ status=2 │                                       │
│   │  已关闭   │                                       │
│   │          │                                       │
│   └──────────┘                                       │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### 3.2 状态转换规则

| 转换 | 触发操作 | 前置条件 | 代码方法 | 副作用 |
|------|----------|----------|----------|--------|
| `→ 0(进行中)` | `addXtrw` 创建任务 | 无 | `addXtrw()` | progress=0, isOverdue=0, createTime/updateTime=now |
| `0 → 1(已完成)` | `updateProgress` 且 progress ≥ 100 | 操作人是负责人或协办人 | `updateProgress()` L134 | updateTime=NOW()，summary 为 null |
| `0 → 2(已关闭)` | `closeXtrw` 手动关闭 | **仅负责人（zfrUserId）可操作** | `closeXtrw()` L148-152 | 写入 summary，updateTime=NOW() |

> **注意**：status=1(已完成) 和 status=2(已关闭) 是**终态**，代码中没有逆向转换路径。已完成的任务不能再关闭，已关闭的任务不能再更新进度。

### 3.3 状态流转的 SQL 条件

- `getOverdueTasks`：`WHERE status=0 AND jzTime < NOW()` — 仅进行中的任务才可能逾期
- `getCompletedTasks`：`WHERE status=1 OR status=2` — 已完成 + 已关闭
- `findNewlyOverdueTasks`：`WHERE status=0 AND isOverdue=0 AND jzTime < NOW()` — 新逾期（未标记过）

---

## 4. 逾期检测与待办生成（checkAndGenerateOverdueTodos）

### 4.1 触发时机

此方法在**每次查询类操作**前被调用（被动式、惰性检测）：

| 调用位置 | 方法 |
|----------|------|
| `getMyOwnedTasks()` L163 | 查询我负责的任务 |
| `getMyCollabTasks()` L169 | 查询我协办的任务 |
| `getOverdueTasks()` L175 | 查询已逾期任务 |
| `getCompletedTasks()` L181 | 查询已完成任务 |
| `getVisibleTasksByDeptId()` L187 | 按部门查询可见任务 |

> 这意味着：如果没有人调用上述接口，逾期状态不会被刷新。不存在定时任务或调度器。

### 4.2 检测逻辑

```
findNewlyOverdueTasks()
    SQL: SELECT * FROM xtrw WHERE status=0 AND isOverdue=0 AND jzTime < NOW()

对每条新逾期任务执行:
    1. markOverdue(xtrwId)        → UPDATE xtrw SET isOverdue=1 WHERE xtrwId=?
    2. addTodo(...)               → INSERT INTO xtrw_todo(xtrwId, xtrwTitle, userId, createTime, isRead=0)
       其中 userId = task.zfrUserId（仅给负责人发待办）
```

### 4.3 容错机制

每条任务的处理被 try-catch 包裹（L207-209），单条失败不影响其他任务的逾期标记。

### 4.4 逾期公告生成（generateAnnouncement）

由前端显式调用 `POST /xtrw/generateAnnouncement?xtrwId=xxx` 触发：

1. 创建一条 `Gsgg`（公司公告），标题格式 `【任务逾期提醒】{任务标题}`，`isZs=1`（展示）
2. 回填 `xtrw_todo.ggId`，将待办与公告关联

---

## 5. 协办人（XtrwXtUser）写入规则

### 5.1 写入时机

**仅在 `addXtrw` 创建任务时写入**，代码中不存在后续增删协办人的接口。

### 5.2 写入流程

```
前端参数: xtUserIds = "1,3,5"    (逗号分隔的 userId 字符串)

1. split(",") 拆分
2. 逐个 Long.parseLong(id) 解析
3. 查 user 表获取 userName（冗余存储）
4. INSERT INTO xtrw_xtuser(xtrwId, xtUserId, xtUserName)
```

### 5.3 关键约束

- 协办人 ID 来源 `user.userId`（BIGINT → Java `long`）
- 用户不存在时 `xtUserName` 存空字符串（不会报错）
- 协办人可参与 `updateProgress`（见权限矩阵）
- 没有去重逻辑：同一 userId 传两次会插入两条记录

---

## 6. 可见部门（XtrwVisibleDept）写入规则

### 6.1 写入时机

**仅在 `addXtrw` 创建任务时写入**，代码中不存在后续修改可见部门的接口。

### 6.2 写入流程

```
前端参数: visibleDeptIds = "1,2"    (逗号分隔的 departmentId 字符串)

1. split(",") 拆分
2. 逐个 Integer.parseInt(id) 解析
3. 查 department 表获取 departmentName（冗余存储）
4. INSERT INTO xtrw_visible_dept(xtrwId, departmentId, departmentName)
```

### 6.3 查询语义

`getVisibleTasksByDeptId(departmentId)` 返回该部门可见的所有任务（JOIN xtrw_visible_dept）。
不校验请求者是否属于该部门——任何知道 departmentId 的人都能查。

---

## 7. 角色权限矩阵

### 7.1 操作权限

| 操作 | 负责人 (zfrUserId) | 协办人 (xtUserId ∈ xtrw_xtuser) | 其他用户 | 代码校验逻辑 |
|------|:---:|:---:|:---:|------|
| **创建任务** addXtrw | ✓ (任意用户) | — | ✓ (任意用户) | 无鉴权，任何登录用户可创建 |
| **查看任务详情** xtrwDetail | ✓ | ✓ | ✓ | 无鉴权，知道 xtrwId 即可查看 |
| **更新进度** updateProgress | ✓ | ✓ | ✗ | L108-118: 先查 zfrUserId 是否匹配，再查 xtrw_xtuser 是否存在，两者都不是则抛异常 |
| **关闭任务** closeXtrw | ✓ | ✗ | ✗ | L148: `xtrw.getZfrUserId() != task.getZfrUserId()` → 抛异常 |
| **上传附件** uploadFile | ✓ | ✓ | ✓ | 无鉴权 |
| **查看我的负责** myOwned | ✓ (仅自己) | — | — | SQL: `WHERE zfrUserId=?` |
| **查看我的协办** myCollab | — | ✓ (仅自己) | — | SQL: `JOIN xtrw_xtuser WHERE xtUserId=?` |
| **查看逾期任务** overdue | ✓ | ✓ | ✓ | 无鉴权，全局可见 |
| **查看已完成** completed | ✓ | ✓ | ✓ | 无鉴权，全局可见 |
| **按部门查看** visibleTasks | ✓ | ✓ | ✓ | 无鉴权，知道 departmentId 即可查 |
| **生成逾期公告** generateAnnouncement | ✓ | ✓ | ✓ | 无鉴权 |
| **查看待办** todoList | ✓ (仅自己) | — | — | SQL: `WHERE userId=?` |
| **标记已读** markTodoRead | ✓ | — | — | 无额外鉴权 |

### 7.2 数据可见性汇总

| 数据 | 可见范围 |
|------|----------|
| 任务详情 | **全局**（无鉴权） |
| 我负责的任务 | 仅负责人本人 |
| 我协办的任务 | 仅协办人本人 |
| 逾期任务列表 | **全局** |
| 已完成/已关闭列表 | **全局** |
| 部门可见任务 | 知道 departmentId 的人 |
| 待办提醒 | 仅接收人（= 负责人） |

---

## 8. 前后端 API 对照（差异说明）

### 8.1 当前前端 `api.js` 缺失的 Xtrw 接口

前端 `oa/src/api/api.js` **尚未定义任何 `/xtrw/` 接口**。以下为后端已有但前端需补齐的端点：

| 后端端点 | HTTP 方法 | 参数 | 返回 |
|----------|-----------|------|------|
| `POST /xtrw/addXtrw` | POST | `Xtrw` 表单 + `xtUserIds`(String) + `visibleDeptIds`(String) | `{success, msg}` |
| `POST /xtrw/xtrwDetail` | POST | `xtrwId` | `{xtrwId, ..., xtUserList, fileList, progressList, visibleDeptList}` |
| `POST /xtrw/updateProgress` | POST | `xtrwId, userId, progress, note` | `{success, msg}` |
| `POST /xtrw/closeXtrw` | POST | `xtrwId, zfrUserId, summary` | `{success, msg}` |
| `POST /xtrw/uploadFile` | POST(multipart) | `xtrwId, uploadUserId, file` | `{success, msg}` |
| `POST /xtrw/myOwned` | POST | `zfrUserId` | `JSONArray` |
| `POST /xtrw/myCollab` | POST | `xtUserId` | `JSONArray` |
| `POST /xtrw/overdue` | POST | 无 | `JSONArray` |
| `POST /xtrw/completed` | POST | 无 | `JSONArray` |
| `POST /xtrw/visibleTasks` | POST | `departmentId` | `JSONArray` |
| `POST /xtrw/generateAnnouncement` | POST | `xtrwId` | `{success, msg, ggId}` |
| `POST /xtrw/todoList` | POST | `userId` | `JSONArray` |
| `POST /xtrw/markTodoRead` | POST | `todoId, isRead` | `{success, msg}` |

### 8.2 类型注意点

| 字段 | 后端 Java 类型 | 前端传参注意 |
|------|---------------|-------------|
| `zfrUserId` | `long` (BIGINT) | 不要用 32-bit int 接收，JS Number 安全范围内但需警惕 |
| `xtUserId` | `long` | 同上 |
| `xtUserIds` | `String` (逗号分隔) | 前端需拼成 `"1,3,5"` 格式 |
| `visibleDeptIds` | `String` (逗号分隔) | 前端需拼成 `"1,2"` 格式 |
| `status` | `int` | 0/1/2，**不是字符串** |
| `progress` | `int` | 0-100 |
| `isOverdue` | `int` | 0/1 |
| `isRead` | `int` | 0/1 |

---

## 9. 已知设计局限与隐患

1. **协办人和可见部门不可修改**：创建后无法增删，只能通过新建任务变通。
2. **逾期检测是惰性的**：依赖查询触发，无后台定时任务，若无人访问则逾期状态不刷新。
3. **无并发保护**：`updateProgress` 和 `closeXtrw` 没有加锁或乐观锁，极端并发下可能产生数据不一致。
4. **全局可见**：任务详情、逾期列表、已完成列表均无鉴权，任何登录用户可见。
5. **逾期待办只发给负责人**：协办人不会收到逾期提醒。
6. **附件路径硬编码**：`D:/test/xtrw/`，无配置化。
7. **xtUserIds / visibleDeptIds 无去重**：重复传入同一 ID 会插入多条记录。
