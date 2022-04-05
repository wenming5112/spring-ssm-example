package com.ssm.example.domain;

import lombok.Data;
import lombok.ToString;

/**
 * @author ming
 * @version 1.0.0
 * @date 2022/4/5 15:20
 **/

@Data
@ToString
public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private boolean valid;
    private int status;
    private String createTime;
    private String modifyTime;
    private String creator;
    private String modifier;
}
