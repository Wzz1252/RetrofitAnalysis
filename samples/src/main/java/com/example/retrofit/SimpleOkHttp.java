package com.example.retrofit;

import okhttp3.*;

import java.io.IOException;

public class SimpleOkHttp {

    public static void main(String... args) {
//        String url = "http://wwww.baidu.com";
//        OkHttpClient okHttpClient = new OkHttpClient();
//        final Request request = new Request.Builder()
//                .url(url)
//                .get()//默认就是GET请求，可以不写
//                .build();
//        Call call = okHttpClient.newCall(request);
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                System.out.println("Simple onFailure");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                System.out.println("Simple onResponse: " + response.body().string());
//            }
//        });

        try {
            execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void execute() throws IOException {
        String url = "http://wwww.baidu.com";
        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .get()//默认就是GET请求，可以不写
                .build();
        Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        System.out.println("execute: " + response.body().string());
    }
}
