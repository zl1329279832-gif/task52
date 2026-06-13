-- H2-compatible DDL for all tables

-- 部门表
CREATE TABLE IF NOT EXISTS department (
  departmentId   INT AUTO_INCREMENT PRIMARY KEY,
  departmentName VARCHAR(255)
);

-- 用户表
CREATE TABLE IF NOT EXISTS user (
  userId       BIGINT AUTO_INCREMENT PRIMARY KEY,
  userName     VARCHAR(255),
  nikeName     VARCHAR(255),
  pwd          VARCHAR(255),
  zw           VARCHAR(255),
  permission   BIGINT,
  sjUserId     BIGINT,
  phone        VARCHAR(255),
  departmentId INT
);

-- 任务表
CREATE TABLE IF NOT EXISTS rw (
  rwId        INT AUTO_INCREMENT PRIMARY KEY,
  rwmc        VARCHAR(255),
  rwms        VARCHAR(1000),
  rwTime      DATE,
  jsUserId    INT,
  fbUserId    INT,
  jsUserName  VARCHAR(255),
  fbUserName  VARCHAR(255),
  jjcd        INT,
  isComplete  INT
);

-- 日志表
CREATE TABLE IF NOT EXISTS rz (
  rzId    INT AUTO_INCREMENT PRIMARY KEY,
  rdnr    VARCHAR(1000),
  rzTime  DATETIME,
  userId  BIGINT
);

-- 公告表
CREATE TABLE IF NOT EXISTS gsgg (
  ggId    INT AUTO_INCREMENT PRIMARY KEY,
  ggTitle VARCHAR(255),
  ggNr    VARCHAR(2000),
  ggTime  DATETIME,
  isZs    INT
);

-- 项目进度表
CREATE TABLE IF NOT EXISTS xmjd (
  xmId  INT AUTO_INCREMENT PRIMARY KEY,
  xmmc  VARCHAR(255),
  xmjd  INT
);

-- 打卡表
CREATE TABLE IF NOT EXISTS dk (
  userId  BIGINT,
  dkTime  DATETIME
);

-- 会议室表
CREATE TABLE IF NOT EXISTS hys (
  hysbh  VARCHAR(50),
  hyszt  VARCHAR(50),
  bz     VARCHAR(255)
);

-- 会议通知表
CREATE TABLE IF NOT EXISTS hytz (
  hyId    INT AUTO_INCREMENT PRIMARY KEY,
  hynr    VARCHAR(1000),
  hyTime  DATETIME,
  hydd    VARCHAR(255),
  hybtzr  VARCHAR(255),
  hyztr   VARCHAR(255)
);

-- 请假申请表
CREATE TABLE IF NOT EXISTS qjsq (
  qjId    INT AUTO_INCREMENT PRIMARY KEY,
  qjyy    VARCHAR(255),
  qjms    VARCHAR(1000),
  ksTime  DATETIME,
  jsTime  DATETIME,
  qjzt    VARCHAR(50),
  userId  BIGINT
);

-- 协作任务主表
CREATE TABLE IF NOT EXISTS xtrw (
  xtrwId      INT AUTO_INCREMENT PRIMARY KEY,
  xtrwTitle   VARCHAR(255),
  xtrwDesc    VARCHAR(1000),
  zfrUserId   BIGINT,
  zfrUserName VARCHAR(255),
  jzTime      DATETIME,
  createTime  DATETIME,
  updateTime  DATETIME,
  progress    INT DEFAULT 0,
  status      INT DEFAULT 0,
  summary     VARCHAR(1000),
  isOverdue   INT DEFAULT 0
);

-- 协办人关联表
CREATE TABLE IF NOT EXISTS xtrw_xtuser (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  xtrwId     INT,
  xtUserId   BIGINT,
  xtUserName VARCHAR(255)
);

-- 进度日志表
CREATE TABLE IF NOT EXISTS xtrw_progress (
  progressId INT AUTO_INCREMENT PRIMARY KEY,
  xtrwId     INT,
  userId     BIGINT,
  userName   VARCHAR(255),
  progress   INT,
  note       VARCHAR(1000),
  updateTime DATETIME
);

-- 任务附件表
CREATE TABLE IF NOT EXISTS xtrw_file (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  xtrwId       INT,
  fileName     VARCHAR(255),
  filePath     VARCHAR(500),
  uploadTime   DATETIME,
  uploadUserId BIGINT
);

-- 可见部门关联表
CREATE TABLE IF NOT EXISTS xtrw_visible_dept (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  xtrwId         INT,
  departmentId   INT,
  departmentName VARCHAR(255)
);

-- 待办提醒表
CREATE TABLE IF NOT EXISTS xtrw_todo (
  todoId     INT AUTO_INCREMENT PRIMARY KEY,
  xtrwId     INT,
  xtrwTitle  VARCHAR(255),
  userId     BIGINT,
  createTime DATETIME,
  isRead     INT DEFAULT 0,
  ggId       INT
);
