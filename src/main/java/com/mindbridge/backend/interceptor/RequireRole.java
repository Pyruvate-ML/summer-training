package com.mindbridge.backend.interceptor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
// 因为这里 管理员大于医生大于病人
// 医生能看到病人看到的东西，管理员能看到医生能看到的东西，这个就像三级二级一级一样
// 所以只要定义一下某个东西至少要几级就能控制了
public @interface RequireRole {
    String value();
}