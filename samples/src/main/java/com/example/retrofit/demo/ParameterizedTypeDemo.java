package com.example.retrofit.demo;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ParameterizedTypeDemo<MyClass, MyInvoke> {
    public static void main(String... args) {
//        ParameterizedTypeDemo demo = new ParameterizedTypeDemo();
        param param = new param();
    }
}

class param<T1, T2> {
    class A {
    }

    class B extends A {
    }

    private Class<T1> entityClass;

    public param() {
        Type type = getClass().getGenericSuperclass();
        System.out.println("getClass() == " + getClass());
        System.out.println("type = " + type);

        Type trueType = ((ParameterizedType) type).getActualTypeArguments()[0];
        System.out.println("trueType1 = " + trueType);
        Type trueType2 = ((ParameterizedType) type).getActualTypeArguments()[1];
        System.out.println("trueType2 = " + trueType2);

        this.entityClass = (Class<T1>) trueType;
        System.out.println("entityClass = " + entityClass);

        B t = new B();
        type = t.getClass().getGenericSuperclass();
        System.out.println("A is B is supper class: " + ((ParameterizedType) type).getActualTypeArguments());
    }
}

class MyClass {

}

class MyInvoke {

}
