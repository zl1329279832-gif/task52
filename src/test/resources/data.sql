INSERT INTO department VALUES (1, '技术部');
INSERT INTO department VALUES (2, '市场部');
INSERT INTO department VALUES (3, '人事部');

INSERT INTO user (userId, userName, nikeName, pwd, zw, permission, departmentId, phone) VALUES (1, 'ycy', 'cymm', '1', '经理', 1, 1, '13800000001');
INSERT INTO user (userId, userName, nikeName, pwd, zw, permission, departmentId, phone) VALUES (3, 'zjw', 'cc', '3', '员工', 3, 2, '13800000003');
INSERT INTO user (userId, userName, nikeName, pwd, zw, permission, departmentId, phone) VALUES (5, 'testUser', 'tu', '5', '员工', 3, 1, '13800000005');

INSERT INTO hys (hysbh, hyszt, bz) VALUES ('A101', '空闲', '小会议室');
INSERT INTO hys (hysbh, hyszt, bz) VALUES ('B202', '空闲', '中会议室');
INSERT INTO hys (hysbh, hyszt, bz) VALUES ('C303', '空闲', '大会议室');
