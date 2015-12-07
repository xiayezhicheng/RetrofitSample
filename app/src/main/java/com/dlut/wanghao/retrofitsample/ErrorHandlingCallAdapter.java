package com.dlut.wanghao.retrofitsample;

import com.google.common.reflect.TypeToken;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit.Call;
import retrofit.CallAdapter;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by wanghao on 2015/12/4.
 */
public final class ErrorHandlingCallAdapter {

    /** 提供不同粒度条件下的反馈. */
    interface MyCallback<T> {
        /** Called for [200, 300) responses. */
        void success(Response<T> response);
        /** Called for 401 responses. */
        void unauthenticated(Response<?> response);
        /** Called for [400, 500) responses, except 401. */
        void clientError(Response<?> response);
        /** Called for [500, 600) response. */
        void serverError(Response<?> response);
        /** Called for network errors while making the call. */
        void networkError(IOException e);
        /** Called for unexpected errors while making the call. */
        void unexpectedError(Throwable t);
    }

    interface MyCall<T> {
        void cancel();
        void enqueue(MyCallback<T> callback);
        MyCall<T> clone();

    }

    public static class ErrorHandlingCallAdapterFactory implements CallAdapter.Factory {
        @Override
        public CallAdapter<MyCall<?>> get(Type returnType, Annotation[] annotations,
                                                    Retrofit retrofit) {
            TypeToken<?> token = TypeToken.of(returnType);
            if (token.getRawType() != MyCall.class) {
                return null;
            }
            if (!(returnType instanceof ParameterizedType)) {
                throw new IllegalStateException(
                        "MyCall must have generic type (e.g., MyCall<ResponseBody>)");
            }
            final Type responseType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
            return new CallAdapter<MyCall<?>>() {
                @Override
                public Type responseType() {
                    return responseType;
                }

                @Override
                public <R> MyCall<R> adapt(Call<R> call) {
                    return new MyCallAdapter<>(call);
                }
            };
        }
    }

    /** Adapts a {@link Call} to {@link MyCall}. */
    static class MyCallAdapter<T> implements MyCall<T> {
        private final Call<T> call;

        MyCallAdapter(Call<T> call) {
            this.call = call;
        }

        @Override
        public void cancel() {
            call.cancel();
        }

        @Override
        public void enqueue(final MyCallback<T> callback) {
            call.enqueue(new Callback<T>() {

                @Override
                public void onResponse(Response<T> response, Retrofit retrofit) {
                    //注意这里还是运行在工作者线程，不能对UI进行操作
                    int code = response.code();
                    if (code >= 200 && code < 300) {
                        callback.success(response);
                    } else if (code == 401) {
                        callback.unauthenticated(response);
                    } else if (code >= 400 && code < 500) {
                        callback.clientError(response);
                    } else if (code >= 500 && code < 600) {
                        callback.serverError(response);
                    } else {
                        callback.unexpectedError(new RuntimeException("Unexpected response " + response));
                    }
                }

                @Override public void onFailure(Throwable t) {
                    //注意这里还是运行在工作者线程，不能对UI进行操作
                    if (t instanceof IOException) {
                        callback.networkError((IOException) t);
                    } else {
                        callback.unexpectedError(t);
                    }
                }
            });
        }

        @Override
        public MyCall<T> clone() {
            return new MyCallAdapter<>(call.clone());
        }
    }
}
