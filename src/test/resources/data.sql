INSERT INTO department VALUES (1, '技术部');
INSERT INTO department VALUES (2, '市场部');
INSERT INTO department VALUES (3, '人事部');

INSERT INTO user (userId, userName, nikeName, pwd, zw, permission, departmentId) VALUES (1, 'ycy', 'cymm', '1', '经理', 1, 1);
INSERT INTO user (userId, userName, nikeName, pwd, zw, permission, departmentId) VALUES (3, 'zjw', 'cc', '3', '员工', 3, 2);
INSERT INTO user (userId, userName, nikeName, pwd, zw, permission, departmentId) VALUES (5, 'testUser', 'tu', '5', '员工', 3, 1);
