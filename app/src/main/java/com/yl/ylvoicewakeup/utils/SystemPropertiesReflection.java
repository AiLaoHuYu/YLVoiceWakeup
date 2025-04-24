package com.yl.ylvoicewakeup.utils;

import java.lang.reflect.Method;


public class SystemPropertiesReflection {

    /**
     * 通过反射获取系统属性的值
     *
     * @param key          属性名
     * @param defaultValue 默认值
     * @return 属性值，如果未找到则返回默认值
     */
    public static String get(String key, String defaultValue) {
        try {
            // 获取 SystemProperties 类的 Class 对象
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            // 获取 get 方法
            Method getMethod = systemProperties.getMethod("get", String.class, String.class);
            // 调用 get 方法
            return (String) getMethod.invoke(null, key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * 通过反射设置系统属性的值
     *
     * @param key   属性名
     * @param value 属性值
     */
    public static void set(String key, String value) {
        try {
            // 获取 SystemProperties 类的 Class 对象
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            // 获取 set 方法
            Method setMethod = systemProperties.getMethod("set", String.class, String.class);
            // 调用 set 方法
            setMethod.invoke(null, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
