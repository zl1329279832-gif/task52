package com.zjw.oa.service;

import com.zjw.oa.entity.Department;

import java.util.List;

public interface DepartmentService {

    List<Department> getDeptList();

    Department getDeptById(Department dept);

    void addDept(Department dept) throws Exception;

    void updateDept(Department dept) throws Exception;

    void delDept(Department dept) throws Exception;
}
