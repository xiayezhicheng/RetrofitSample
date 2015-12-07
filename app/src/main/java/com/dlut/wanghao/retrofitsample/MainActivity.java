package com.dlut.wanghao.retrofitsample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public class MainActivity extends AppCompatActivity {

    private Message msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void clickMe(View view){

        normalGetMethod();

//        customErrorHandleMethod();

    }

    public interface GitHub {
        @GET("/repos/{owner}/{repo}/contributors")
        Call<List<Contributor>> contributors(
                @Path("owner") String owner,
                @Path("repo") String repo);

//        //生成/search/repositories?q=retrofit&since=2015-08-27或/search/repositories?q=retrofit&since=20150827
//        @GET("/search/repositories")
//        RepositoriesResponse searchRepos(
//                @Query("q") String query,
//                @Query("since") Date since);
//        复杂的查询参数可以使用Map进行组合
//        @GET("group/{id}/users")
//        List<User> groupList(@Path("id") int groupId, @QueryMap Map<String, String> options);

    }

    public static class Contributor {
        public final String login;
        public final int contributions;

        public Contributor(String login, int contributions) {
            this.login = login;
            this.contributions = contributions;
        }
    }

    /**
     * 普通的Get操作请求
     */
    private void normalGetMethod() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GitHub github = retrofit.create(GitHub.class);
        Call<List<Contributor>> call = github.contributors("square", "retrofit");
        call.enqueue(new Callback<List<Contributor>>() {
            @Override
            public void onResponse(Response<List<Contributor>> response, Retrofit retrofit) {
                //运行在UI线程
                List<Contributor> contributors = response.body();
                for (Contributor contributor : contributors) {
                    System.out.println(contributor.login + " (" + contributor.contributions + ")");
                    System.out.println(Thread.currentThread().getName());
                }
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }


    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(MainActivity.this, msg.obj.toString(),Toast.LENGTH_SHORT).show();
        }
    };

    interface HttpBinService {
        @GET("/ip")
        ErrorHandlingCallAdapter.MyCall<Ip> getIp();
    }

    static class Ip {
        String origin;
    }

    /**
     * 自定义Call，对错误进行更细致的分析，但是要注意回调在worker线程
     */
    private void customErrorHandleMethod() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://httpbin.org")
                        //将 call 实例转换成其他类型
                .addCallAdapterFactory(new ErrorHandlingCallAdapter.ErrorHandlingCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        HttpBinService service = retrofit.create(HttpBinService.class);
        ErrorHandlingCallAdapter.MyCall<Ip> ip = service.getIp();
        msg = handler.obtainMessage();
        ip.enqueue(new ErrorHandlingCallAdapter.MyCallback<Ip>() {
            @Override public void success(Response<Ip> response) {
                msg.obj = "SUCCESS "+response.body().origin;
                msg.sendToTarget();
            }

            @Override public void unauthenticated(Response<?> response) {
                msg.obj = "UNAUTHENTICATED";
                msg.sendToTarget();
            }

            @Override public void clientError(Response<?> response) {
                msg.obj = "CLIENT ERROR " + response.code() + " " + response.message();
                msg.sendToTarget();
            }

            @Override public void serverError(Response<?> response) {
                msg.obj = "SERVER ERROR " + response.code() + " " + response.message();
                msg.sendToTarget();
            }

            @Override public void networkError(IOException e) {
                msg.obj = "NETOWRK ERROR " + e.getMessage();
                msg.sendToTarget();
            }

            @Override public void unexpectedError(Throwable t) {
                msg.obj = "FATAL ERROR " + t.getMessage();
                msg.sendToTarget();
            }
        });
    }

    /**
     * post请求
     */
    interface HttpPostService{
        @FormUrlEncoded
        @POST("/phone/user.do")
        Call register(@Query("method") String register,@Field("flag") String flag
                             ,@Field("userName") String username,@Field("passWord") String password
                             ,@Field("birthDay") String birthDay);
    }


}
