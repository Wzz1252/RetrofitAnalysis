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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static retrofit2.Utils.methodError;

abstract class ServiceMethod<T> {
    /**
     * 解析注解
     *
     * @param retrofit 核心对象
     * @param method   XXXService 接口所定义的方法
     * @param <T>
     * @return
     */
    static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
        // 获得了具体的请求体内容
        RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);

        // 获得方法的返回值类型
        Type returnType = method.getGenericReturnType();
        // 返回值不能是泛型类型，否则直接抛异常
        if (Utils.hasUnresolvableType(returnType)) {
            throw methodError(method,
                    "Method return type must not include a type variable or wildcard: %s", returnType);
        }
        // 必须要有返回值类型，否则直接抛异常
        if (returnType == void.class) {
            throw methodError(method, "Service methods cannot return void.");
        }

        return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
    }

    abstract T invoke(Object[] args);
}
