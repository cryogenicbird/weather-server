package com.example.weather_server;

import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO users(username, password_hash) VALUES(#{username}, #{passwordHash})")
    int insert(@Param("username") String username, @Param("passwordHash") String hash);

    @Select("SELECT id, username, password_hash FROM users WHERE username = #{username}")
    Map<String, Object> findByUsername(@Param("username") String username);
}