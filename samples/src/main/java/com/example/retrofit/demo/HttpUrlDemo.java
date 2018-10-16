package com.example.retrofit.demo;

import okhttp3.HttpUrl;

import java.util.List;

public class HttpUrlDemo {
    public static void main(String... args) {
        HttpUrl httpUrl = HttpUrl.get("https://api.github.com/");
        List<String> pathSegments = httpUrl.pathSegments();
        System.out.println("pathSegments: " + pathSegments);
        System.out.println("pathSegments: " + pathSegments.size());
        if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
            throw new IllegalArgumentException("baseUrl must end in /: " + httpUrl);
        }
        System.out.println("HttpUrlDemo End");
    }
}
