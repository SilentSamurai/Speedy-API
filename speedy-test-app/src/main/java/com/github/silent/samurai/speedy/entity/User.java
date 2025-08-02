package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyAction;
import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Table(name = "users", indexes = {
        @Index(name = "users_phone_no_key", columnList = "phone_no", unique = true),
        @Index(name = "users_email_key", columnList = "email", unique = true)
})
@Entity
public class User extends AbstractBaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(User.class);

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @Column(name = "email", nullable = false, length = 250)
    private String email;

    @Column(name = "type", nullable = false, length = 512)
    private String type;

    @SpeedyAction(ActionType.READ)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @SpeedyAction(ActionType.READ)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @SpeedyAction(ActionType.READ)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_login_at")
    private LocalDate lastLoginDate;

    @Column(name = "login_count")
    private Integer loginCount;

}