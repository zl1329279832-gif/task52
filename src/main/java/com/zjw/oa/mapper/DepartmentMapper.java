package com.zjw.oa.mapper;

import com.zjw.oa.entity.Department;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DepartmentMapper {

    List<Department> getDeptList();

    Department getDeptById(Department dept);

    void addDept(Department dept) throws Exception;

    void updateDept(Department dept) throws Exception;

    void delDept(Department dept) throws Exception;
}
