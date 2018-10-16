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

import java.io.IOException;
import okhttp3.Request;

/**
 * An invocation of a Retrofit method that sends a request to a webserver and returns a response.
 * Each call yields its own HTTP request and response pair. Use {@link #clone} to make multiple
 * calls with the same parameters to the same webserver; this may be used to implement polling or
 * to retry a failed call.
 *
 * <p>Calls may be executed synchronously with {@link #execute}, or asynchronously with {@link
 * #enqueue}. In either case the call can be canceled at any time with {@link #cancel}. A call that
 * is busy writing its request or reading its response may receive a {@link IOException}; this is
 * working as designed.
 *
 * 调用Retrofit方法，该方法将请求发送到Web服务器并返回响应。 每次调用都会产生自己的HTTP请求和响应对。
 * 使用clone使用相同的参数对同一个Web服务器进行多次调用; 这可用于实现轮询或重试失败的呼叫。
 *
 * 调用可以与execute同步执行，也可以与enqueue异步执行。在任何一种情况下，都可以随时取消呼叫取消。
 * 正在忙于写入其请求或读取其响应的调用可能会收到IOException; 这是按设计工作的。
 *
 * @param <T> Successful response body type.
 */
public interface Call<T> extends Cloneable {
  /**
   * 同步发送请求并返回其响应
   *
   * @throws IOException 如果发生与服务器通信的问题
   * @throws RuntimeException （和子类）如果在创建请求或解码响应时发生意外错误。
   */
  Response<T> execute() throws IOException;

  /**
   * Asynchronously send the request and notify {@code callback} of its response or if an error
   * occurred talking to the server, creating the request, or processing the response.
   */
  void enqueue(Callback<T> callback);

  /**
   * Returns true if this call has been either {@linkplain #execute() executed} or {@linkplain
   * #enqueue(Callback) enqueued}. It is an error to execute or enqueue a call more than once.
   */
  boolean isExecuted();

  /**
   * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not
   * yet been executed it never will be.
   */
  void cancel();

  /** True if {@link #cancel()} was called. */
  boolean isCanceled();

  /**
   * Create a new, identical call to this one which can be enqueued or executed even if this call
   * has already been.
   */
  Call<T> clone();

  /** The original HTTP request. */
  Request request();
}
