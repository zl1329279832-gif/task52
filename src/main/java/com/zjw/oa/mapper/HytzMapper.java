package com.zjw.oa.mapper;

import com.zjw.oa.entity.Hytz;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface HytzMapper {

    int countOverlappingReservations(@Param("hydd") String hydd,
                                     @Param("ksTime") Date ksTime,
                                     @Param("jsTime") Date jsTime);

    int countUserConflicts(@Param("userId") int userId,
                           @Param("ksTime") Date ksTime,
                           @Param("jsTime") Date jsTime);

    void insertReservation(Hytz hytz);

    List<Hytz> selectByRoom(@Param("hydd") String hydd);
}
