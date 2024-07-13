package com.kalyan.documan.entitiy;

import com.kalyan.documan.enums.RoleEnum;
import com.kalyan.documan.enums.SemesterEnum;
import com.kalyan.documan.enums.UserStatus;
import com.kalyan.documan.enums.YearEnum;
import org.hibernate.annotations.Comments;

import java.util.Date;
import java.util.List;

public class User {
    private String first_name;
    private String last_name;
    private String email;
    private YearEnum year;
    private SemesterEnum semeser;
    private RoleEnum role;
    private String username;
    private String password;
    private boolean consent;
    private Department department;
    private UserStatus status;
    private String image_url;
    private Date registration_date;
    private Date last_login_date;
    private List<Post> posts;
    private List<Comment> comments;

}
