package com.zjw.oa.mapper;

import com.zjw.oa.entity.Hytz;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 会议通知 Mapper
 */
@Mapper
public interface HytzMapper {

    /**
     * 查询指定会议室在 [ksTime, jsTime) 时间段内存在重叠的预约数量。
     * 重叠判断: existing.ksTime < newJsTime AND existing.jsTime > newKsTime
     * （严格不等式保证相邻会议不算冲突）
     */
    int countOverlappingReservations(@Param("hydd") String hydd,
                                     @Param("ksTime") Date ksTime,
                                     @Param("jsTime") Date jsTime);

    /**
     * 查询指定用户在 [ksTime, jsTime) 时间段内被预约的会议数量（任意会议室）。
     */
    int countUserConflicts(@Param("userId") int userId,
                           @Param("ksTime") Date ksTime,
                           @Param("jsTime") Date jsTime);

    /**
     * 插入一条会议预约记录。
     */
    void insertReservation(Hytz hytz);

    /**
     * 按会议室查询预约列表。
     */
    List<Hytz> selectByRoom(@Param("hydd") String hydd);
}
