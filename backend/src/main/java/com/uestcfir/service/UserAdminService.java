package com.uestcfir.service;

import com.uestcfir.pojo.entity.User;

import java.util.List;

public interface UserAdminService {

    User getUserById(Integer id);

    List<User> getAllUsers();

    boolean addUser(User user) throws Exception;

    boolean updateUser(User user);

    boolean deleteUser(Integer id);





    User getUserByUsername(String username);

    List<User> getUsersByType(Integer userType);






}
