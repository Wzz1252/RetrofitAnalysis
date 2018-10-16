/*
 * Copyright (C) 2015 Square, Inc.
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
package retrofit2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.annotation.Nullable;

/**
 * Creates call adapters for that uses the same thread for both I/O and application-level
 * callbacks. For synchronous calls this is the application thread making the request; for
 * asynchronous calls this is a thread provided by OkHttp's dispatcher.
 * <p>
 * 为其创建调用适配器，为 I/O 和应用程序级回调使用相同的线程。 对于同步调用，这是发出请求的应用程序线程;
 * 对于异步调用，这是 OkHttp 的调度程序提供的一个线程。
 */
final class DefaultCallAdapterFactory extends CallAdapter.Factory {
    static final CallAdapter.Factory INSTANCE = new DefaultCallAdapterFactory();

    /**
     * 获得指定的 CallAdapter
     *
     * @param returnType  返回值类型
     * @param annotations 执行方法的所有注解
     * @param retrofit    Retrofit 对象
     * @return CallAdapter
     */
    @Override
    public @Nullable
    CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != Call.class) {
            return null;
        }

        // 拿到第一个泛型的上界（为T中的上界类型【不管T中是否还有其他的泛型都一并算进去】）
        // 例如: Call<List<SimpleService>> 上界是 List<SimpleService>
        final Type responseType = Utils.getCallResponseType(returnType);
        return new CallAdapter<Object, Call<?>>() {
            @Override
            public Type responseType() {
                return responseType;
            }

            @Override
            public Call<Object> adapt(Call<Object> call) {
                return call;
            }
        };
    }
}
