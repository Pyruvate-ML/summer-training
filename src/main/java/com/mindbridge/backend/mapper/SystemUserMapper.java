package com.mindbridge.backend.mapper;

import com.mindbridge.backend.entity.SystemUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SystemUserMapper {

    @Select("SELECT * FROM system_user WHERE username = #{username} AND is_deleted = 0")
    SystemUser findByUsername(String username);

    @Select("SELECT * FROM system_user WHERE id = #{id} AND is_deleted = 0")
    SystemUser findById(Long id);

    @Insert("INSERT INTO system_user(username, password_hash, display_name, role) " +
            "VALUES(#{username}, #{passwordHash}, #{displayName}, #{role})")
    void insertUser(SystemUser user);

    @Update("UPDATE system_user SET password_hash = #{newPassword} WHERE id = #{id}")
    void updatePassword(Long id, String newPassword);

    @Update("UPDATE system_user SET role = #{role} WHERE id = #{id}")
    void updateRole(Long id, String role);

    @Select("SELECT * FROM `system_user` WHERE is_deleted = 0")
    List<SystemUser> findAllUsers();

    @Update("UPDATE `system_user` SET status = #{status} WHERE id = #{id}")
    void updateStatus(Long id, String status);
}