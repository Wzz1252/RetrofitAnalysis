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
package retrofit2;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.Url;

import static java.util.Collections.unmodifiableList;
import static retrofit2.Utils.checkNotNull;

/**
 * Retrofit adapts a Java interface to HTTP calls by using annotations on the declared methods to
 * define how requests are made. Create instances using {@linkplain Builder
 * the builder} and pass your interface to {@link #create} to generate an implementation.
 * <p>
 * For example,
 * <pre><code>
 * Retrofit retrofit = new Retrofit.Builder()
 *     .baseUrl("https://api.example.com/")
 *     .addConverterFactory(GsonConverterFactory.create())
 *     .build();
 *
 * MyApi api = retrofit.create(MyApi.class);
 * Response&lt;User&gt; user = api.getUser().execute();
 * </code></pre>
 * <p>
 * TODO https://www.jianshu.com/p/0c055ad46b6c 文章分析 可以参考
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public final class Retrofit {
    // 网络请求配置对象（对网络请求接口中方法注解进行解析后得到的对象）
    // 作用：存储网络请求相关的配置，如网络请求的方法、数据转换器、网络请求适配器、网络请求工厂、基地址等
    private final Map<Method, ServiceMethod<?>> serviceMethodCache = new ConcurrentHashMap<>();

    // 网络请求器的工厂
    // 作用：生产网络请求器（Call）
    // Retrofit是默认使用okhttp
    final okhttp3.Call.Factory callFactory;

    // 网络请求的url地址
    final HttpUrl baseUrl;

    // 数据转换器工厂的集合
    // 作用：放置数据转换器工厂
    // 数据转换器工厂作用：生产数据转换器（converter）
    final List<Converter.Factory> converterFactories;

    // 网络请求适配器工厂的集合
    // 作用：放置网络请求适配器工厂
    // 网络请求适配器工厂作用：生产网络请求适配器（CallAdapter）
    // 下面会详细说明
    final List<CallAdapter.Factory> callAdapterFactories;

    // 回调方法执行器
    final @Nullable
    Executor callbackExecutor;

    // 标志位
    // 作用：是否提前对业务接口中的注解进行验证转换的标志位
    final boolean validateEagerly;

    Retrofit(okhttp3.Call.Factory callFactory, HttpUrl baseUrl,
             List<Converter.Factory> converterFactories, List<CallAdapter.Factory> callAdapterFactories,
             @Nullable Executor callbackExecutor, boolean validateEagerly) {
        this.callFactory = callFactory;
        this.baseUrl = baseUrl;
        this.converterFactories = converterFactories; // Copy+unmodifiable at call site.
        this.callAdapterFactories = callAdapterFactories; // Copy+unmodifiable at call site.
        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
    }

    /**
     * Create an implementation of the API endpoints defined by the {@code service} interface.
     * <p>
     * The relative path for a given method is obtained from an annotation on the method describing
     * the request type. The built-in methods are {@link retrofit2.http.GET GET},
     * {@link retrofit2.http.PUT PUT}, {@link retrofit2.http.POST POST}, {@link retrofit2.http.PATCH
     * PATCH}, {@link retrofit2.http.HEAD HEAD}, {@link retrofit2.http.DELETE DELETE} and
     * {@link retrofit2.http.OPTIONS OPTIONS}. You can use a custom HTTP method with
     * {@link HTTP @HTTP}. For a dynamic URL, omit the path on the annotation and annotate the first
     * parameter with {@link Url @Url}.
     * <p>
     * Method parameters can be used to replace parts of the URL by annotating them with
     * {@link retrofit2.http.Path @Path}. Replacement sections are denoted by an identifier
     * surrounded by curly braces (e.g., "{foo}"). To add items to the query string of a URL use
     * {@link retrofit2.http.Query @Query}.
     * <p>
     * The body of a request is denoted by the {@link retrofit2.http.Body @Body} annotation. The
     * object will be converted to request representation by one of the {@link Converter.Factory}
     * instances. A {@link RequestBody} can also be used for a raw representation.
     * <p>
     * Alternative request body formats are supported by method annotations and corresponding
     * parameter annotations:
     * <ul>
     * <li>{@link retrofit2.http.FormUrlEncoded @FormUrlEncoded} - Form-encoded data with key-value
     * pairs specified by the {@link retrofit2.http.Field @Field} parameter annotation.
     * <li>{@link retrofit2.http.Multipart @Multipart} - RFC 2388-compliant multipart data with
     * parts specified by the {@link retrofit2.http.Part @Part} parameter annotation.
     * </ul>
     * <p>
     * Additional static headers can be added for an endpoint using the
     * {@link retrofit2.http.Headers @Headers} method annotation. For per-request control over a
     * header annotate a parameter with {@link Header @Header}.
     * <p>
     * By default, methods return a {@link Call} which represents the HTTP request. The generic
     * parameter of the call is the response body type and will be converted by one of the
     * {@link Converter.Factory} instances. {@link ResponseBody} can also be used for a raw
     * representation. {@link Void} can be used if you do not care about the body contents.
     * <p>
     * For example:
     * <pre>
     * public interface CategoryService {
     *   &#64;POST("category/{cat}/")
     *   Call&lt;List&lt;Item&gt;&gt; categoryList(@Path("cat") String a, @Query("page") int b);
     * }
     * </pre>
     * <p>
     * 当执行完create方法后，会创建HttpServiceMethod类（这个类中包含了所有的所需内容）
     */
    @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
    public <T> T create(final Class<T> service) {
        // 判断是不是接口类型，如果不是抛出异常
        Utils.validateServiceInterface(service);
        if (validateEagerly) {
            // 具体方法作用：
            // 1. 给接口中每个方法的注解进行解析并得到一个 ServiceMethod 对象
            // 2. 以 Method 为键将该对象存入 ConcurrentHashMap 集合中（ConcurrentHashMap 支持并发并通过锁分段技术提高性能）
            // 特别注意：如果不是提前验证则进行动态解析对应方法（下面会详细说明），得到一个ServiceMethod对象，最后存入到 ConcurrentHashMap 集合中，类似延迟加载（默认）
            eagerlyValidateMethods(service);
        }

        // 创建了网络请求接口的动态代理对象，即通过动态代理创建网络请求接口的实例 （并最终返回）
        // 该动态代理是为了拿到网络请求接口实例上所有注解
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
                new InvocationHandler() {
                    private final Platform platform = Platform.get();
                    private final Object[] emptyArgs = new Object[0];

                    /**
                     *
                     * @param proxy
                     * @param method 执行的方法名
                     * @param args 方法所对应的形参列表
                     * @return
                     * @throws Throwable
                     */
                    @Override
                    public Object invoke(Object proxy, Method method, @Nullable Object[] args)
                            throws Throwable {
                        // 如果这个方法来自 Object，则走默认逻辑
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args);
                        }

                        // 如果是默认方法，则单独处理（在 Android 中直接抛异常）
                        if (platform.isDefaultMethod(method)) {
                            return platform.invokeDefaultMethod(method, service, proxy, args);
                        }
                        return loadServiceMethod(method).invoke(args != null ? args : emptyArgs);
                    }
                });
    }

    private void eagerlyValidateMethods(Class<?> service) {
        Platform platform = Platform.get();
        // getDeclaredMethods() 获取本类中的所有方法，包括私有的(private、protected、默认以及public)的方法。
        for (Method method : service.getDeclaredMethods()) {
            if (!platform.isDefaultMethod(method)) {
                loadServiceMethod(method);
            }
        }
    }

    ServiceMethod<?> loadServiceMethod(Method method) {
        ServiceMethod<?> result = serviceMethodCache.get(method);
        if (result != null) {
            return result;
        }

        synchronized (serviceMethodCache) {
            result = serviceMethodCache.get(method);
            if (result == null) {
                result = ServiceMethod.parseAnnotations(this, method);
                serviceMethodCache.put(method, result);
            }
        }
        return result;
    }

    /**
     * The factory used to create {@linkplain okhttp3.Call OkHttp calls} for sending a HTTP requests.
     * Typically an instance of {@link OkHttpClient}.
     */
    public okhttp3.Call.Factory callFactory() {
        return callFactory;
    }

    /**
     * The API base URL.
     */
    public HttpUrl baseUrl() {
        return baseUrl;
    }

    /**
     * Returns a list of the factories tried when creating a
     * {@linkplain #callAdapter(Type, Annotation[])} call adapter}.
     */
    public List<CallAdapter.Factory> callAdapterFactories() {
        return callAdapterFactories;
    }

    /**
     * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
     * #callAdapterFactories() factories}.
     *
     * @throws IllegalArgumentException if no call adapter available for {@code type}.
     */
    public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }

    /**
     * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
     * #callAdapterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no call adapter available for {@code type}.
     */
    public CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType,
                                             Annotation[] annotations) {
        checkNotNull(returnType, "returnType == null");
        checkNotNull(annotations, "annotations == null");

        // 检索出指定的适配器
        int start = callAdapterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
            if (adapter != null) {
                return adapter;
            }
        }

        // 通过异常的方式告知没有找到指定的适配器
        StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
                .append(returnType)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns an unmodifiable list of the factories tried when creating a
     * {@linkplain #requestBodyConverter(Type, Annotation[], Annotation[]) request body converter}, a
     * {@linkplain #responseBodyConverter(Type, Annotation[]) response body converter}, or a
     * {@linkplain #stringConverter(Type, Annotation[]) string converter}.
     */
    public List<Converter.Factory> converterFactories() {
        return converterFactories;
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link RequestBody} from the available
     * {@linkplain #converterFactories() factories}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<T, RequestBody> requestBodyConverter(Type type,
                                                              Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        return nextRequestBodyConverter(null, type, parameterAnnotations, methodAnnotations);
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link RequestBody} from the available
     * {@linkplain #converterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<T, RequestBody> nextRequestBodyConverter(
            @Nullable Converter.Factory skipPast, Type type, Annotation[] parameterAnnotations,
            Annotation[] methodAnnotations) {
        checkNotNull(type, "type == null");
        checkNotNull(parameterAnnotations, "parameterAnnotations == null");
        checkNotNull(methodAnnotations, "methodAnnotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter.Factory factory = converterFactories.get(i);
            Converter<?, RequestBody> converter =
                    factory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, RequestBody>) converter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate RequestBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #converterFactories() factories}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(null, type, annotations);
    }

    /**
     * 获得一个指定的转化器
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #converterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<ResponseBody, T> nextResponseBodyConverter(
            @Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        // 获得指定的转化器
        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter<ResponseBody, ?> converter =
                    converterFactories.get(i).responseBodyConverter(type, annotations, this);
            if (converter != null) {
                // noinspection unchecked
                return (Converter<ResponseBody, T>) converter;
            }
        }

        // 没有找到指定的转化器，通过异常的方式提示给用户
        StringBuilder builder = new StringBuilder("Could not locate ResponseBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link String} from the available
     * {@linkplain #converterFactories() factories}.
     */
    public <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        for (int i = 0, count = converterFactories.size(); i < count; i++) {
            Converter<?, String> converter =
                    converterFactories.get(i).stringConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, String>) converter;
            }
        }

        // Nothing matched. Resort to default converter which just calls toString().
        //noinspection unchecked
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    /**
     * The executor used for {@link Callback} methods on a {@link Call}. This may be {@code null},
     * in which case callbacks should be made synchronously on the background thread.
     */
    public @Nullable
    Executor callbackExecutor() {
        return callbackExecutor;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Build a new {@link Retrofit}.
     * <p>
     * Calling {@link #baseUrl} is required before calling {@link #build()}. All other methods
     * are optional.
     */
    public static final class Builder {
        private final Platform platform;
        private @Nullable
        okhttp3.Call.Factory callFactory;
        private HttpUrl baseUrl;
        private final List<Converter.Factory> converterFactories = new ArrayList<>();
        private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
        private @Nullable
        Executor callbackExecutor;
        private boolean validateEagerly;

        Builder(Platform platform) {
            this.platform = platform;
        }

        public Builder() {
            // 会创建一个默认的平台（这里为Android）
            this(Platform.get());
        }

        Builder(Retrofit retrofit) {
            platform = Platform.get();
            callFactory = retrofit.callFactory;
            baseUrl = retrofit.baseUrl;

            // Do not add the default BuiltIntConverters and platform-aware converters added by build().
            for (int i = 1,
                 size = retrofit.converterFactories.size() - platform.defaultConverterFactoriesSize();
                 i < size; i++) {
                converterFactories.add(retrofit.converterFactories.get(i));
            }

            // Do not add the default, platform-aware call adapters added by build().
            for (int i = 0,
                 size = retrofit.callAdapterFactories.size() - platform.defaultCallAdapterFactoriesSize();
                 i < size; i++) {
                callAdapterFactories.add(retrofit.callAdapterFactories.get(i));
            }

            callbackExecutor = retrofit.callbackExecutor;
            validateEagerly = retrofit.validateEagerly;
        }

        /**
         * The HTTP client used for requests.
         * <p>
         * This is a convenience method for calling {@link #callFactory}.
         */
        public Builder client(OkHttpClient client) {
            return callFactory(checkNotNull(client, "client == null"));
        }

        /**
         * Specify a custom call factory for creating {@link Call} instances.
         * <p>
         * Note: Calling {@link #client} automatically sets this value.
         */
        public Builder callFactory(okhttp3.Call.Factory factory) {
            this.callFactory = checkNotNull(factory, "factory == null");
            return this;
        }

        /**
         * Set the API base URL.
         *
         * @see #baseUrl(HttpUrl)
         */
        public Builder baseUrl(String baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            return baseUrl(HttpUrl.get(baseUrl));
        }

        /**
         * Set the API base URL.
         * <p>
         * The specified endpoint values (such as with {@link GET @GET}) are resolved against this
         * value using {@link HttpUrl#resolve(String)}. The behavior of this matches that of an
         * {@code <a href="">} link on a website resolving on the current URL.
         * <p>
         * <b>Base URLs should always end in {@code /}.</b>
         * <p>
         * A trailing {@code /} ensures that endpoints values which are relative paths will correctly
         * append themselves to a base which has path components.
         * <p>
         * <b>Correct:</b><br>
         * Base URL: http://example.com/api/<br>
         * Endpoint: foo/bar/<br>
         * Result: http://example.com/api/foo/bar/
         * <p>
         * <b>Incorrect:</b><br>
         * Base URL: http://example.com/api<br>
         * Endpoint: foo/bar/<br>
         * Result: http://example.com/foo/bar/
         * <p>
         * This method enforces that {@code baseUrl} has a trailing {@code /}.
         * <p>
         * <b>Endpoint values which contain a leading {@code /} are absolute.</b>
         * <p>
         * Absolute values retain only the host from {@code baseUrl} and ignore any specified path
         * components.
         * <p>
         * Base URL: http://example.com/api/<br>
         * Endpoint: /foo/bar/<br>
         * Result: http://example.com/foo/bar/
         * <p>
         * Base URL: http://example.com/<br>
         * Endpoint: /foo/bar/<br>
         * Result: http://example.com/foo/bar/
         * <p>
         * <b>Endpoint values may be a full URL.</b>
         * <p>
         * Values which have a host replace the host of {@code baseUrl} and values also with a scheme
         * replace the scheme of {@code baseUrl}.
         * <p>
         * Base URL: http://example.com/<br>
         * Endpoint: https://github.com/square/retrofit/<br>
         * Result: https://github.com/square/retrofit/
         * <p>
         * Base URL: http://example.com<br>
         * Endpoint: //github.com/square/retrofit/<br>
         * Result: http://github.com/square/retrofit/ (note the scheme stays 'http')
         */
        public Builder baseUrl(HttpUrl baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            // 对 URL 进行分割
            List<String> pathSegments = baseUrl.pathSegments();
            // 判断是不是以"/"结尾，如果不是抛出异常
            if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
                throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl);
            }
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Add converter factory for serialization and deserialization of objects.
         */
        public Builder addConverterFactory(Converter.Factory factory) {
            converterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }

        /**
         * Add a call adapter factory for supporting service method return types other than {@link
         * Call}.
         */
        public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
            callAdapterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }

        /**
         * The executor on which {@link Callback} methods are invoked when returning {@link Call} from
         * your service method.
         * <p>
         * Note: {@code executor} is not used for {@linkplain #addCallAdapterFactory custom method
         * return types}.
         */
        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = checkNotNull(executor, "executor == null");
            return this;
        }

        /**
         * Returns a modifiable list of call adapter factories.
         */
        public List<CallAdapter.Factory> callAdapterFactories() {
            return this.callAdapterFactories;
        }

        /**
         * Returns a modifiable list of converter factories.
         */
        public List<Converter.Factory> converterFactories() {
            return this.converterFactories;
        }

        /**
         * When calling {@link #create} on the resulting {@link Retrofit} instance, eagerly validate
         * the configuration of all methods in the supplied interface.
         */
        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }

        /**
         * Create the {@link Retrofit} instance using the configured values.
         * <p>
         * Note: If neither {@link #client} nor {@link #callFactory} is called a default {@link
         * OkHttpClient} will be created and used.
         */
        public Retrofit build() {
            if (baseUrl == null) {
                throw new IllegalStateException("Base URL required.");
            }

            // 配置网络请求执行器
            // 如果没有设置，默认使用 OkHttp
            okhttp3.Call.Factory callFactory = this.callFactory;
            if (callFactory == null) {
                callFactory = new OkHttpClient();
            }

            // 配置回调方法执行器
            Executor callbackExecutor = this.callbackExecutor;
            // 如果没指定，则默认使用Platform检测环境时的默认callbackExecutor
            // 即Android默认的callbackExecutor
            if (callbackExecutor == null) {
                callbackExecutor = platform.defaultCallbackExecutor();
            }

            // 配置网络请求适配器工厂
            List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
            // 向该集合中添加了步骤2中创建的CallAdapter.Factory请求适配器（添加在集合器末尾）
            // 请求适配器工厂集合存储顺序：自定义1适配器工厂、自定义2适配器工厂...默认适配器工厂（ExecutorCallAdapterFactory）
            callAdapterFactories.addAll(platform.defaultCallAdapterFactories(callbackExecutor));

            // Make a defensive copy of the converters.
            List<Converter.Factory> converterFactories = new ArrayList<>(
                    1 + this.converterFactories.size() + platform.defaultConverterFactoriesSize());

            // 配置数据转换器工厂
            converterFactories.add(new BuiltInConverters()); // 添加默认转换器
            converterFactories.addAll(this.converterFactories); // 添加传入的转换器
            converterFactories.addAll(platform.defaultConverterFactories()); // 添加平台默认转换器

            return new Retrofit(callFactory, baseUrl, unmodifiableList(converterFactories),
                    unmodifiableList(callAdapterFactories), callbackExecutor, validateEagerly);
        }
    }
}
