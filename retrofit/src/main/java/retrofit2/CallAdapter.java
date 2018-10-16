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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nullable;

/**
 * Adapts a {@link Call} with response type {@code R} into the type of {@code T}. Instances are
 * created by {@linkplain Factory a factory} which is
 * {@linkplain Retrofit.Builder#addCallAdapterFactory(Factory) installed} into the {@link Retrofit}
 * instance.
 * <p>
 * 将响应类型为 R 调整为 T 类型。实例由安装在 Retrofit 实例中的工厂创建。
 */
public interface CallAdapter<R, T> {
    /**
     * Returns the value type that this adapter uses when converting the HTTP response body to a Java
     * object. For example, the response type for {@code Call<Repo>} is {@code Repo}. This type
     * is used to prepare the {@code call} passed to {@code #adapt}.
     * <p>
     * Note: This is typically not the same type as the {@code returnType} provided to this call
     * adapter's factory.
     * <p>
     * 返回此适配器在将HTTP响应主体转换为Java对象时使用的值类型。 例如，Call <Repo>的响应类型是Repo。 此类型用于准备
     * 传递给#adapt的调用。
     * 注意：这通常与提供给此调用适配器工厂的returnType类型不同。
     */
    Type responseType();

    /**
     * Returns an instance of {@code T} which delegates to {@code call}.
     * <p>
     * For example, given an instance for a hypothetical utility, {@code Async}, this instance would
     * return a new {@code Async<R>} which invoked {@code call} when run.
     * <pre><code>
     * &#64;Override
     * public &lt;R&gt; Async&lt;R&gt; adapt(final Call&lt;R&gt; call) {
     *   return Async.create(new Callable&lt;Response&lt;R&gt;&gt;() {
     *     &#64;Override
     *     public Response&lt;R&gt; call() throws Exception {
     *       return call.execute();
     *     }
     *   });
     * }
     * </code></pre>
     * <p>
     * 返回委托调用的T实例。
     * 例如，给定假设实用程序Async的实例，此实例将返回一个新的Async <R>，它在运行时调用了调用。
     */
    T adapt(Call<R> call);

    /**
     * 根据服务接口方法的返回类型创建 {@link CallAdapter} 实例
     * Creates {@link CallAdapter} instances based on the return type of {@linkplain
     * Retrofit#create(Class) the service interface} methods.
     */
    abstract class Factory {
        /**
         * 返回returnType的接口方法的调用适配器，如果此工厂无法处理，则返回 null。
         * Returns a call adapter for interface methods that return {@code returnType}, or null if it
         * cannot be handled by this factory.
         */
        public abstract @Nullable
        CallAdapter<?, ?> get(
                Type returnType, Annotation[] annotations, Retrofit retrofit);

        /**
         * Extract the upper bound of the generic parameter at {@code index} from {@code type}. For
         * example, index 1 of {@code Map<String, ? extends Runnable>} returns {@code Runnable}.
         */
        protected static Type getParameterUpperBound(int index, ParameterizedType type) {
            return Utils.getParameterUpperBound(index, type);
        }

        /**
         * 从类型中提取原始类类型。 例如，表示List<？ extends Runnable> 返回 List.class。
         * Extract the raw class type from {@code type}. For example, the type representing
         * {@code List<? extends Runnable>} returns {@code List.class}.
         */
        protected static Class<?> getRawType(Type type) {
            return Utils.getRawType(type);
        }
    }
}
