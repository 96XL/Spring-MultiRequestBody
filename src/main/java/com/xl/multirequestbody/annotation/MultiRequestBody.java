package com.xl.multirequestbody.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 中方法接收多个 JSON 对象
 *
 * @author XL
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiRequestBody {

    /**
     * 参数是否必传, 当接收参数是非基本数据类型时生效
     */
    boolean required() default true;

    /**
     * 当 value 的值或者参数名不匹配时, 是否允许解析最外层属性到该对象
     */
    boolean parseAllFields() default true;

    /**
     * 解析时用到的 json key
     */
    String value() default "";
}