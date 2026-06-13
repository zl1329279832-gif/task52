package com.zjw.oa.service.impl;

import com.zjw.oa.entity.Department;
import com.zjw.oa.mapper.DepartmentMapper;
import com.zjw.oa.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public List<Department> getDeptList() {
        return departmentMapper.getDeptList();
    }

    @Override
    public Department getDeptById(Department dept) {
        return departmentMapper.getDeptById(dept);
    }

    @Override
    public void addDept(Department dept) throws Exception {
        departmentMapper.addDept(dept);
    }

    @Override
    public void updateDept(Department dept) throws Exception {
        departmentMapper.updateDept(dept);
    }

    @Override
    public void delDept(Department dept) throws Exception {
        departmentMapper.delDept(dept);
    }
}
