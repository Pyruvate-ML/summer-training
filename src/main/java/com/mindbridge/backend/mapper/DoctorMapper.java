package com.mindbridge.backend.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorMapper {

    @Insert("INSERT INTO Doctor(name, title, specialties, user_id) " +
            "VALUES(#{name}, #{title}, #{specialties}, #{userId})")
    void insertDoctor(String name, String title, String specialties, Long userId);
}