-- 跨部门任务协作看板 - 数据库表结构

-- 协作任务主表
CREATE TABLE IF NOT EXISTS xtrw (
    xtrwId      INT PRIMARY KEY AUTO_INCREMENT,
    xtrwTitle   VARCHAR(255) NOT NULL COMMENT '任务标题',
    xtrwDesc    VARCHAR(2000) COMMENT '任务描述',
    zfrUserId   INT NOT NULL COMMENT '主负责人ID',
    zfrUserName VARCHAR(50) COMMENT '主负责人姓名',
    jzTime      DATETIME NOT NULL COMMENT '截止时间',
    createTime  DATETIME NOT NULL COMMENT '创建时间',
    updateTime  DATETIME NOT NULL COMMENT '最近更新时间',
    progress    INT DEFAULT 0 COMMENT '总进度(0-100)',
    status      INT DEFAULT 0 COMMENT '状态: 0=进行中, 1=已完成, 2=已关闭',
    isOverdue   INT DEFAULT 0 COMMENT '是否逾期: 0=否, 1=是',
    summary     VARCHAR(2000) COMMENT '任务总结(关闭时填写)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨部门协作任务';

-- 协办人表
CREATE TABLE IF NOT EXISTS xtrw_xtuser (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    xtrwId      INT NOT NULL COMMENT '任务ID',
    xtUserId    INT NOT NULL COMMENT '协办人ID',
    xtUserName  VARCHAR(50) COMMENT '协办人姓名',
    INDEX idx_xtrwId (xtrwId),
    INDEX idx_xtUserId (xtUserId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作任务-协办人';

-- 进度更新记录表
CREATE TABLE IF NOT EXISTS xtrw_progress (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    xtrwId      INT NOT NULL COMMENT '任务ID',
    userId      INT NOT NULL COMMENT '更新人ID',
    userName    VARCHAR(50) COMMENT '更新人姓名',
    progress    INT COMMENT '进度(0-100)',
    note        VARCHAR(1000) COMMENT '进度说明',
    updateTime  DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_xtrwId (xtrwId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作任务-进度记录';

-- 任务附件表
CREATE TABLE IF NOT EXISTS xtrw_file (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    xtrwId        INT NOT NULL COMMENT '任务ID',
    fileName      VARCHAR(255) NOT NULL COMMENT '文件名',
    filePath      VARCHAR(500) NOT NULL COMMENT '文件路径',
    uploadTime    DATETIME NOT NULL COMMENT '上传时间',
    uploadUserId  INT NOT NULL COMMENT '上传人ID',
    INDEX idx_xtrwId (xtrwId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作任务-附件';

-- 可见部门表
CREATE TABLE IF NOT EXISTS xtrw_visible_dept (
    id             INT PRIMARY KEY AUTO_INCREMENT,
    xtrwId         INT NOT NULL COMMENT '任务ID',
    departmentId   INT NOT NULL COMMENT '部门ID',
    departmentName VARCHAR(100) COMMENT '部门名称',
    INDEX idx_xtrwId (xtrwId),
    INDEX idx_deptId (departmentId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作任务-可见部门';

-- 待办提醒表
CREATE TABLE IF NOT EXISTS xtrw_todo (
    todoId      INT PRIMARY KEY AUTO_INCREMENT,
    xtrwId      INT NOT NULL COMMENT '任务ID',
    xtrwTitle   VARCHAR(255) COMMENT '任务标题',
    userId      INT NOT NULL COMMENT '接收人ID',
    createTime  DATETIME NOT NULL COMMENT '创建时间',
    isRead      INT DEFAULT 0 COMMENT '是否已读: 0=未读, 1=已读',
    ggId        INT COMMENT '关联公告ID(逾期生成公告时填写)',
    INDEX idx_userId (userId),
    INDEX idx_xtrwId (xtrwId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作任务-待办提醒';
