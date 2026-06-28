package org.taobao.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.taobao.dto.UserQueryDTO;
import org.taobao.pojo.User;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 根据账号和密码查询用户
     */
    @Select("SELECT user_id, account, password, user_type, status, username, gender, birthday, phone, email, avatar_url, create_time, update_time FROM user WHERE account = #{account} AND password = #{password}")
    User findByAccountAndPassword(String account, String password);

    /**
     * 根据账号查询用户
     */
    @Select("SELECT user_id, account, password, user_type, status, username, gender, birthday, phone, email, avatar_url, create_time, update_time FROM user WHERE account = #{account}")
    User findByAccount(String account);

    /**
     * 根据id查询用户
     */
    @Select("SELECT user_id, account, password, user_type, status, username, gender, birthday, phone, email, avatar_url, create_time, update_time FROM user WHERE user_id = #{id}")
    User findById(Long id);

    /**
     * 更新用户信息
     */
    @Update("UPDATE user SET username = #{username}, gender = #{gender}, birthday = #{birthday}, phone = #{phone}, email = #{email}, avatar_url = #{avatarUrl}, update_time = #{updateTime} WHERE user_id = #{userId}")
    void update(User user);

    /**
     * 插入用户
     */
    @Insert("INSERT INTO user (account, password, user_type, status, username, gender, birthday, phone, email, avatar_url, create_time, update_time) "
            + "VALUES (#{account}, #{password}, #{userType}, #{status}, #{username}, #{gender}, #{birthday}, #{phone}, #{email}, #{avatarUrl}, #{createTime}, #{updateTime})")
    //让MyBatis自动获取数据库生成的自增主键ID，并赋值给User对象的userId属性。
    @Options(useGeneratedKeys = true, keyProperty = "userId")    void insert(User user);

    /**
     * 获取用户列表
     */
    List<User> getUserList(UserQueryDTO userQueryDTO);

    /**
     * 获取用户总数
     */
    Integer getUserCount(UserQueryDTO userQueryDTO);

    /**
     * 更新用户状态
     */
    void updateStatus(Long userId, String status);
}