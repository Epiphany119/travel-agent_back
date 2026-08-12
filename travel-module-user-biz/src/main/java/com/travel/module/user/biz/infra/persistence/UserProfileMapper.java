package com.travel.module.user.biz.infra.persistence;

import org.apache.ibatis.annotations.*;

/**
 * 用户资料 Mapper
 */
@Mapper
public interface UserProfileMapper {
    
    @Select("SELECT * FROM user_profile WHERE user_id = #{userId} LIMIT 1")
    UserProfilePO findByUserId(@Param("userId") String userId);
    
    @Select("SELECT * FROM user_profile WHERE id = #{id}")
    UserProfilePO findById(@Param("id") Long id);
    
    @Insert("INSERT INTO user_profile (user_id, username, nickname, avatar, email, phone, bio) " +
            "VALUES (#{userId}, #{username}, #{nickname}, #{avatar}, #{email}, #{phone}, #{bio})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserProfilePO po);
    
    @Update("UPDATE user_profile SET " +
            "nickname = #{nickname}, " +
            "avatar = #{avatar}, " +
            "email = #{email}, " +
            "phone = #{phone}, " +
            "bio = #{bio} " +
            "WHERE user_id = #{userId}")
    int updateByUserId(UserProfilePO po);
    
    @Delete("DELETE FROM user_profile WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") String userId);
}
