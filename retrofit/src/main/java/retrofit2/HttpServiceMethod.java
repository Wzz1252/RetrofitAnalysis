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
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;

import static retrofit2.Utils.methodError;

/**
 * 将接口方法的调用调整为HTTP调用
 */
final class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
    /**
     * Inspects the annotations on an interface method to construct a reusable service method that
     * speaks HTTP. This requires potentially-expensive reflection so it is best to build each service
     * method only once and reuse it.
     * <p>
     * 返回此适配器在将HTTP响应主体转换为Java对象时使用的值类型。 例如，Call <Repo>的响应类型是Repo。
     * 此类型用于准备传递给#adapt的调用。
     * 注意：这通常与提供给此调用适配器工厂的returnType类型不同。
     */
    static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
            Retrofit retrofit, Method method, RequestFactory requestFactory) {
        CallAdapter<ResponseT, ReturnT> callAdapter = createCallAdapter(retrofit, method);
        Type responseType = callAdapter.responseType();
        // 上界的类型不能是Response类型，否则直接抛异常
        if (responseType == Response.class || responseType == okhttp3.Response.class) {
            throw methodError(method, "'"
                    + Utils.getRawType(responseType).getName()
                    + "' is not a valid response body type. Did you mean ResponseBody?");
        }

        if (requestFactory.httpMethod.equals("HEAD") && !Void.class.equals(responseType)) {
            // HEAD 方法必须使用 Void 作为响应类型。
            throw methodError(method, "HEAD method must use Void as response type.");
        }

        Converter<ResponseBody, ResponseT> responseConverter =
                createResponseConverter(retrofit, method, responseType);

        okhttp3.Call.Factory callFactory = retrofit.callFactory;
        return new HttpServiceMethod<>(requestFactory, callFactory, callAdapter, responseConverter);
    }

    private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
            Retrofit retrofit, Method method) {
        Type returnType = method.getGenericReturnType(); // 获取方法的返回值类型
        Annotation[] annotations = method.getAnnotations(); // 获得当前方法中的所有注解
        try {
            // 无需检查
            return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType, annotations);
        } catch (RuntimeException e) { // 各种各样的原因，因为工厂是由用户创建的
            throw methodError(method, e, "Unable to create call adapter for %s", returnType);
        }
    }

    // 创建响应转换器
    private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
            Retrofit retrofit, Method method, Type responseType) {
        Annotation[] annotations = method.getAnnotations(); // 获得当前方法的所有注解
        try {
            return retrofit.responseBodyConverter(responseType, annotations);
        } catch (RuntimeException e) { // Wide exception range because factories are user code.
            throw methodError(method, e, "Unable to create converter for %s", responseType);
        }
    }

    private final RequestFactory requestFactory;
    private final okhttp3.Call.Factory callFactory;
    private final CallAdapter<ResponseT, ReturnT> callAdapter;
    private final Converter<ResponseBody, ResponseT> responseConverter;

    /**
     * @param requestFactory    封装着具体的网络请求中的内容（请求类型、url等）
     * @param callFactory       网络请求的底层实现（默认是OkHttp）
     * @param callAdapter       网络请求适配器【还有点迷糊】
     * @param responseConverter 对返回的数据进行转换（转换成泛型指定的格式）
     */
    private HttpServiceMethod(RequestFactory requestFactory, okhttp3.Call.Factory callFactory,
                              CallAdapter<ResponseT, ReturnT> callAdapter,
                              Converter<ResponseBody, ResponseT> responseConverter) {
        this.requestFactory = requestFactory;
        this.callFactory = callFactory;
        this.callAdapter = callAdapter;
        this.responseConverter = responseConverter;
    }

    /**
     * 由动态代理执行
     * @param args 具体的每一个方法
     * @return
     */
    @Override
    ReturnT invoke(Object[] args) {
        return callAdapter.adapt(
                new OkHttpCall<>(requestFactory, args, callFactory, responseConverter));
    }
}
