package com.github.hcsp.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MapBeanConverter {
    // 传入一个遵守Java Bean约定的对象，读取它的所有属性，存储成为一个Map
    // 例如，对于一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 应当返回一个Map { id -> 1, name -> "ABC", longName -> false }
    // 提示：
    //  1. 读取传入参数bean的Class
    //  2. 通过反射获得它包含的所有名为getXXX/isXXX，且无参数的方法（即getter方法）
    //  3. 通过反射调用这些方法并将获得的值存储到Map中返回
    public static Map<String, Object> beanToMap(Object bean) {
        Map<String, Object> map = new HashMap<>();
        try {
            Arrays.stream(bean.getClass().getMethods()).forEach((method -> {
                String methodName = method.getName();
                if (methodName.startsWith("is")) {
                    String fieldName = methodName.substring(2);
                    String name = getFormatMethodName(fieldName);
                    try {
                        map.put(name, method.invoke(bean));
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));

            for (Field field : bean.getClass().getDeclaredFields()) {
                String fieldName = field.getName();
                Method method = bean.getClass().getDeclaredMethod(getFormatMethodName("get", "toUpperCase", fieldName));
                Object value = method.invoke(bean);
                map.put(fieldName, value);
            }
            return map;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        try {
            T obj = klass.getDeclaredConstructor().newInstance();
            Field[] fields = klass.getDeclaredFields();
            for (Field field : fields) {
                String fieldName = field.getName();
                Object value = map.get(fieldName);
                Method method = klass.getDeclaredMethod(getFormatMethodName("set", "toUpperCase", fieldName), value.getClass());
                method.invoke(obj, value);
            }
            return obj;

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        DemoJavaBean bean = new DemoJavaBean();
        bean.setId(100);
        bean.setName("AAAAAAAAAAAAAAAAAAA");
        System.out.println(beanToMap(bean));

        Map<String, Object> map = new HashMap<>();
        map.put("id", 123);
        map.put("name", "ABCDEFG");
        System.out.println(mapToBean(DemoJavaBean.class, map));
    }

    static class DemoJavaBean {
        Integer id;
        String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isLongName() {
            return name.length() > 10;
        }

        @Override
        public String toString() {
            return "DemoJavaBean{"
                    + "id="
                    + id
                    + ", name='"
                    + name
                    + '\''
                    + ", longName="
                    + isLongName()
                    + '}';
        }
    }

    public static String getFormatMethodName(String key, String mode, String name) {
        if (mode.equals("toUpperCase")) {
            return key + (name.charAt(0) + "").toUpperCase() + name.substring(1);
        }
        return key + (name.charAt(0) + "").toLowerCase() + name.substring(1);
    }

    public static String getFormatMethodName(String name) {
        return (name.charAt(0) + "").toLowerCase() + name.substring(1);
    }
}
