package com.template.service.system;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.template.entity.registry.SchoolUser;
import com.template.entity.user.User;
import com.template.mapper.user.UserMapper;
import com.template.registry.mapper.SchoolUserMapper;
import com.template.util.SecurityUtils;

/**
 * Creates the school login account + registry index for a teacher/student.
 * Requires the routing DS to be the target school (school admin flow).
 */
@Component
public class LoginAccountHelper {

    private final PasswordEncoder passwordEncoder;
    private final SchoolUserMapper schoolUserMapper;

    public LoginAccountHelper(PasswordEncoder passwordEncoder, SchoolUserMapper schoolUserMapper) {
        this.passwordEncoder = passwordEncoder;
        this.schoolUserMapper = schoolUserMapper;
    }

    /**
     * Inserts the user (school DB) + the school_users index (registry) and
     * returns the new user id.
     */
    public Long createUserAndIndex(String username, String password, String fullname,
                                   String email, UserMapper userMapper) {
        if (userMapper.findByUsername(username) != null) {
            throw new com.template.exception.BusinessException(
                    "Username already exists in this school: " + username,
                    "/teachers/new");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullname(fullname);
        user.setEmail(email);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setLoginAttempts(0);
        user.setCreatedBy(SecurityUtils.getCurrentUsername());
        user.setCreatedDate(LocalDateTime.now());
        user.setDeleted(false);
        user.setVersion(0);
        userMapper.insert(user);

        SchoolUser index = new SchoolUser();
        index.setSchoolId(SecurityUtils.getCurrentSchoolId());
        index.setUserId(user.getId());
        index.setUsername(username);
        index.setEnabled(true);
        index.setCreatedBy(SecurityUtils.getCurrentUsername());
        index.setDeleted(false);
        schoolUserMapper.insertSelective(index);

        return user.getId();
    }
}