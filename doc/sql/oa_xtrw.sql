-- =============================================
-- 跨部门任务协作看板 数据库变更
-- =============================================

-- 1. 部门表
CREATE TABLE `department` (
  `departmentId`   INT NOT NULL AUTO_INCREMENT COMMENT '部门编号',
  `departmentName` VARCHAR(255) DEFAULT NULL COMMENT '部门名称',
  PRIMARY KEY (`departmentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `department` VALUES (1, '技术部'), (2, '市场部'), (3, '人事部');

-- 2. user 表增加部门字段
ALTER TABLE `user` ADD COLUMN `departmentId` INT DEFAULT NULL;

-- 3. gsgg 表修正自增
ALTER TABLE `gsgg` MODIFY `ggId` INT(5) NOT NULL AUTO_INCREMENT;

-- 4. 协作任务主表
CREATE TABLE `xtrw` (
  `xtrwId`      INT NOT NULL AUTO_INCREMENT,
  `xtrwTitle`   VARCHAR(255) DEFAULT NULL COMMENT '任务标题',
  `xtrwDesc`    VARCHAR(1000) DEFAULT NULL COMMENT '任务描述',
  `zfrUserId`   BIGINT DEFAULT NULL COMMENT '主负责人ID',
  `zfrUserName` VARCHAR(255) DEFAULT NULL COMMENT '主负责人名称',
  `jzTime`      DATETIME DEFAULT NULL COMMENT '截止时间',
  `createTime`  DATETIME DEFAULT NULL COMMENT '创建时间',
  `updateTime`  DATETIME DEFAULT NULL COMMENT '最近更新时间',
  `progress`    INT DEFAULT 0 COMMENT '进度0-100',
  `status`      INT DEFAULT 0 COMMENT '0进行中 1已完成 2已关闭',
  `summary`     VARCHAR(1000) DEFAULT NULL COMMENT '关闭汇总',
  `isOverdue`   INT DEFAULT 0 COMMENT '0否 1是',
  PRIMARY KEY (`xtrwId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 5. 协办人关联表
CREATE TABLE `xtrw_xtuser` (
  `id`         INT NOT NULL AUTO_INCREMENT,
  `xtrwId`     INT DEFAULT NULL,
  `xtUserId`   BIGINT DEFAULT NULL,
  `xtUserName` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 6. 进度日志表
CREATE TABLE `xtrw_progress` (
  `progressId` INT NOT NULL AUTO_INCREMENT,
  `xtrwId`     INT DEFAULT NULL,
  `userId`     BIGINT DEFAULT NULL,
  `userName`   VARCHAR(255) DEFAULT NULL,
  `progress`   INT DEFAULT NULL,
  `note`       VARCHAR(1000) DEFAULT NULL,
  `updateTime` DATETIME DEFAULT NULL,
  PRIMARY KEY (`progressId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 7. 任务附件表
CREATE TABLE `xtrw_file` (
  `id`           INT NOT NULL AUTO_INCREMENT,
  `xtrwId`       INT DEFAULT NULL,
  `fileName`     VARCHAR(255) DEFAULT NULL,
  `filePath`     VARCHAR(500) DEFAULT NULL,
  `uploadTime`   DATETIME DEFAULT NULL,
  `uploadUserId` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 8. 可见部门关联表
CREATE TABLE `xtrw_visible_dept` (
  `id`             INT NOT NULL AUTO_INCREMENT,
  `xtrwId`         INT DEFAULT NULL,
  `departmentId`   INT DEFAULT NULL,
  `departmentName` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 9. 待办提醒表
CREATE TABLE `xtrw_todo` (
  `todoId`     INT NOT NULL AUTO_INCREMENT,
  `xtrwId`     INT DEFAULT NULL,
  `xtrwTitle`  VARCHAR(255) DEFAULT NULL,
  `userId`     BIGINT DEFAULT NULL,
  `createTime` DATETIME DEFAULT NULL,
  `isRead`     INT DEFAULT 0,
  `ggId`       INT DEFAULT NULL COMMENT '关联公告ID',
  PRIMARY KEY (`todoId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
