/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.retrofit;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * 非常简单的使用实例
 */
public final class SimpleService {
    public static final String API_URL = "https://api.github.com";

    /**
     * 贡献者 Entity
     */
    public static class Contributor {
        public final String login;
        public final int contributions;

        public Contributor(String login, int contributions) {
            this.login = login;
            this.contributions = contributions;
        }
    }

    public interface GitHub {
        @GET("/repos/{owner}/{repo}/contributors")
        Call<List<Contributor>> contributors(@Path("owner") String owner, @Path("repo") String repo);
    }

    public static void main(String... args) throws IOException {
        Retrofit retrofit = new Retrofit.Builder() // Builder 会创建当前的平台
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GitHub github = retrofit.create(GitHub.class);

        // 创建一个贡献者实例
        // 当执行 contributors 时，将通过动态代理调用 invoke 方法
        // Call --> OkHttpCall
        Call<List<Contributor>> call = github.contributors("square", "retrofit");

        // 异步 打印返回结果
        call.enqueue(new Callback<List<Contributor>>() {
            @Override
            public void onResponse(Call<List<Contributor>> call, Response<List<Contributor>> response) {
                System.out.println("onResponse: success");
                printResponse(response.body());
            }

            @Override
            public void onFailure(Call<List<Contributor>> call, Throwable t) {
                System.out.println("onFailure: " + t.getMessage());
            }
        });
//        // 同步 打印返回结果
//        printResponse(call.execute().body());
    }

    private static void printResponse(List<Contributor> contributors) {
        for (Contributor contributor : contributors) {
            System.out.println("结果：" + contributor.login + " (" + contributor.contributions + ")");
        }
    }
}
