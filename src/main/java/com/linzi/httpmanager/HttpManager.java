package com.linzi.httpmanager;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.linzi.httpmanager.tool.CallBack;
import com.linzi.httpmanager.tool.LoadDialog;
import com.linzi.httpmanager.tool.RequestParams;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by linzi on 2017/4/21.
 */

public class HttpManager {
    private static Context mContext;
    private static CallBack.LoadCallBackListener mListener;//请求结果回调
    private static CallBack.DownLoadListener mDownListener;//下载和上传结果回调
    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();//线程池
    private String[] Context_type={"application/x-www-form-urlencoded","application/json"};
    private static boolean isUseOkHttp=false;
    private static OkHttpClient mOkHttpClient;
    private static Request request;
    static List<CallBack.LoadCallBackListener>listener_list=new ArrayList<>();
    static List<Call>call_list=new ArrayList<>();
    static List<Integer>tag_list=new ArrayList<>();
    static int tag=-1;
    private static Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    listener_list.get(msg.arg2).onFinishResult(msg.arg1,msg.obj.toString());
                    call_list.get(msg.arg2).cancel();
                    break;
                case 1:
                    listener_list.get(msg.arg2).onErrResult(msg.arg1,new Exception("请求失败"));
                    call_list.get(msg.arg2).cancel();
                    break;
                case 2:
                    mDownListener.onStart(msg.arg1);
                    break;
                case 3:
                    mDownListener.onDownLoading(msg.arg1,(long)msg.obj);
                    break;
                case 4:
                    mDownListener.onErr(msg.arg1,new Exception("失败"));
                    break;
                case 5:
                    mDownListener.onFinish(msg.arg1,msg.obj.toString());
                    break;
            }
        }
    };
    /**
     * 构造函数，实例化管理器
     * @param context
     */
    public static void init(Context context){
        mContext=context;
        LoadDialog.init(context);
    }
    public static void init(Context context,Boolean isUse){
        mContext=context;
        isUseOkHttp=isUse;

    }
    public boolean Config(){
        mOkHttpClient=new OkHttpClient();
        return true;
    }

    private static void isfull(){
        if(tag_list.size()>20){
            boolean isc=true;
            for(int x=0;x<call_list.size();x++) {
                if(call_list.get(x).isCanceled()){
                    isc=true;
                }else{
                    isc=false;
                }
            }
            if(isc) {
                tag_list.clear();
                listener_list.clear();
                call_list.clear();
            }
        }
    }

    public static void doGet(final int what, final RequestParams params, final CallBack.LoadCallBackListener listener){
        if(!isUseOkHttp) {
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    doGetForJson(what, params, listener);
                }
            });
        }else{
            try {
                //判断请求队列是否达到请求上限//上限20
                isfull();
                listener_list.add(listener);
                tag++;
                tag_list.add(tag);
                if(!params.getParams().isEmpty()) {
                    request = new Request.Builder()
                            .url(params.getBaseUrl() + "?" + params.getParams())
                            .build();
                }else{
                    request = new Request.Builder()
                            .url(params.getBaseUrl())
                            .build();
                }
                mOkHttpClient.setConnectTimeout(params.getTimeOut(), TimeUnit.MILLISECONDS);
                final Call call=mOkHttpClient.newCall(request);
                call_list.add(call);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        Message msg=new Message();
                        msg.what=1;
                        msg.arg1=what;
                        handler.sendMessage(msg);
                        call.cancel();
                    }
                    @Override
                    public void onResponse(Response response) throws IOException {
                        Message msg=new Message();
                        msg.what=0;
                        msg.arg1=what;
                        msg.obj=response.body().string();
                        handler.sendMessage(msg);
                        call.cancel();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static void doPost(final int what, final RequestParams params, final CallBack.LoadCallBackListener listener){
        if(!isUseOkHttp) {
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    doPostForJson(what,params,listener);
                }
            });
        }else{
            isfull();
            listener_list.add(listener);
            tag++;
            tag_list.add(tag);
            FormEncodingBuilder builder = new FormEncodingBuilder();
            if(!params.getParamsMaps().isEmpty()) {
                for (String key : params.getParamsMaps().keySet()) {
                    builder.add(key, params.getParamsMaps().get(key));
                }
                request = new Request.Builder()
                        .url(params.getBaseUrl())
                        .post(builder.build())
                        .addHeader("tag",""+tag)
                        .build();
            }else{
                request = new Request.Builder()
                        .url(params.getBaseUrl())
                        .addHeader("tag",""+tag)
                        .build();
            }
            final Call call=mOkHttpClient.newCall(request);
            call_list.add(call);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.d("head", "onFailure: "+request.header("tag"));
                    if(tag_list.size()>0){
                        for(int x=0;x<tag_list.size();x++){
                            if(tag_list.get(x).toString().equals(request.header("tag"))){
                                Message msg=new Message();
                                msg.what=1;
                                msg.arg1=what;
                                msg.arg2=x;
                                handler.sendMessage(msg);
                            }
                        }
                    }
                }
                @Override
                public void onResponse(Response response) throws IOException {
                    Log.d("head", "onResponse: "+response.request().header("tag"));
                    if(tag_list.size()>0){
                        for(int x=0;x<tag_list.size();x++){
                            if(tag_list.get(x).toString().equals(response.request().header("tag"))){
                                Message msg=new Message();
                                msg.what=0;
                                msg.arg2=x;
                                msg.arg1=what;
                                msg.obj=response.body().string();
                                handler.sendMessage(msg);
                            }
                        }
                    }
                }
            });
        }
    }
    public static void doLoad(final int what, final RequestParams params, final CallBack.DownLoadListener listener){
        if(!isUseOkHttp) {
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    doDownLoad(what,params,listener);
                }
            });
        }else{
            mDownListener=listener;
            request=new Request.Builder()
                    .url(params.getBaseUrl())
                    .build();
            final Call call=mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Message msg=new Message();
                    msg.what=4;
                    msg.arg1=what;
                    handler.sendMessage(msg);
                    call.cancel();
                }
                @Override
                public void onResponse(Response response) throws IOException {
                    long contentLength = response.body().contentLength();
                    String filePath=params.getFilePath()+params.getFileName();
                    File descFile = new File(filePath);
                    FileOutputStream fos = new FileOutputStream(descFile);
                    byte[] buffer = new byte[1024];
                    int len;
                    long totalReaded =0;
                    Message msg=new Message();
                    msg.what=2;
                    msg.arg1=what;
                    handler.sendMessage(msg);
                    InputStream inputStream = response.body().byteStream();
                    while ((len = inputStream.read(buffer)) != -1) {
                        // 写到本地
                        totalReaded+=len;
                        long progress = totalReaded * 100 / contentLength;
                        msg.what = 3;
                        msg.obj = progress;
                        msg.arg1 = what;
                        handler.sendMessage(msg);
                        if(progress==100){
                            msg.what = 5;
                            msg.obj = "{\"msg\":\"下载成功\",\"path\":\""+filePath+"\"}";
                            msg.arg1 = what;
                            handler.sendMessage(msg);
                        }
                        fos.write(buffer, 0, len);
                    }
                    call.cancel();
                }
            });
        }
    }
    public static void upLoad(final int what, final RequestParams params, final CallBack.DownLoadListener listener){
        int flag=0;
        if(!isUseOkHttp) {
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    doUpload(what,params,listener);
                }
            });
        }else{
            mDownListener=listener;
//            RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
            MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM);
            if(!params.getParamsMaps().isEmpty()) {
                for (String key : params.getParamsMaps().keySet()) {
                    builder.addFormDataPart(key, params.getParamsMaps().get(key));
                }
            }else{flag++;}
            if(params.getUpLoadMap()!=null) {
                for (String key : params.getUpLoadMap().keySet()) {
                    builder.addFormDataPart(key, params.getUpLoadMap().get(key).getName()
                            , createProgressRequestBody(MediaType.parse("application/octet-stream"), params.getUpLoadMap().get(key), what, listener));
                }
            }else{
                flag++;
            }
            if(flag!=2) {
                request = new Request.Builder()
                        .url(params.getBaseUrl())
                        .post(builder.build())
                        .build();
            }else{
                request = new Request.Builder()
                        .url(params.getBaseUrl())
                        .build();
            }
            final Call call=mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Message msg=new Message();
                    msg.what=4;
                    msg.arg1=what;
                    handler.sendMessage(msg);
                    call.cancel();
                }
                @Override
                public void onResponse(Response response) throws IOException {
                    Message msg=new Message();
                    if(response.isSuccessful()){
                        msg.what = 5;
                        msg.obj = response.body().string();
                        msg.arg1 = what;
                        handler.sendMessage(msg);
                    }else{
                        msg.what=4;
                        msg.arg1=what;
                        handler.sendMessage(msg);
                    }
                    call.cancel();
                }
            });
        }
    }
    /**
     * get请求方式
     * @param what 队列标识
     * @param params 请求参数
     * @param listener 请求回调监听
     */
    private static void doGetForJson(int what, RequestParams params, CallBack.LoadCallBackListener listener){
        mListener=listener;
        try {
            String request_url="";
            if(!params.getParams().isEmpty()) {
                request_url = params.getBaseUrl() + "?" + params.getParams();
            }else{
                request_url = params.getBaseUrl();
            }
            // 新建一个URL对象
            URL url = new URL(request_url);
            // 打开一个HttpURLConnection连接
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            // 设置连接主机超时时间
            urlConn.setConnectTimeout(params.getTimeOut());
            //设置从主机读取数据超时
            urlConn.setReadTimeout(params.getTimeOut());
            // 设置是否使用缓存  默认是true
            urlConn.setUseCaches(params.getUseCache());
            // 设置为Post请求
            urlConn.setRequestMethod("GET");
            //urlConn设置请求头信息
            //设置请求中的媒体类型信息。
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //设置客户端与服务连接类型
            urlConn.addRequestProperty("Connection", "Keep-Alive");
            // 开始连接
            urlConn.connect();
            // 判断请求是否成功
            if (urlConn.getResponseCode() == 200) {
                // 获取返回的数据
                String result = streamToString(urlConn.getInputStream());
                //通过监听进行回调
                Message msg=new Message();
                msg.what=0;
                msg.arg1=what;
                msg.obj=result;
                handler.sendMessage(msg);
                Log.e("HttpManager", "Get方式请求成功，result--->");
            } else {
                Message msg=new Message();
                msg.what=1;
                msg.arg1=what;
                handler.sendMessage(msg);
                Log.e("HttpManager", "Get方式请求失败"+urlConn.getResponseCode());
            }
            // 关闭连接
            urlConn.disconnect();

        }catch (Exception e){
            mListener.onErrResult(what,e);
        }
    }

    /**
     *post
     * @param what
     * @param params
     * @param listener
     */
    private static void doPostForJson(int what, RequestParams params, CallBack.LoadCallBackListener listener){
        mListener=listener;
        try{
            String param=params.getParams();
            // 请求的参数转换为byte数组
            byte[] postData = param.getBytes();
            // 新建一个URL对象
            URL url = new URL(params.getBaseUrl());
            // 打开一个HttpURLConnection连接
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            // 设置连接超时时间
            urlConn.setConnectTimeout(params.getTimeOut());
            //设置从主机读取数据超时
            urlConn.setReadTimeout(params.getTimeOut());
            // Post请求必须设置允许输出 默认false
            urlConn.setDoOutput(true);
            //设置请求允许输入 默认是true
            urlConn.setDoInput(true);
            // Post请求不能使用缓存
            urlConn.setUseCaches(params.getUseCache());
            // 设置为Post请求
            urlConn.setRequestMethod("POST");
            //设置本次连接是否自动处理重定向
            urlConn.setInstanceFollowRedirects(true);
            // 配置请求Content-Type(提交表单数据编码方式)
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConn.setRequestProperty("Connection", "Keep-Alive");// 维持长连接
            // 开始连接
            urlConn.connect();
            // 发送请求参数
            DataOutputStream dos = new DataOutputStream(urlConn.getOutputStream());
            if(!param.isEmpty()) {
                dos.write(postData);
            }
            dos.flush();
            dos.close();
            // 判断请求是否成功
            // 判断请求是否成功
            if (urlConn.getResponseCode() == 200) {
                // 获取返回的数据
                String result = streamToString(urlConn.getInputStream());
                //通过监听进行回调
                Message msg=new Message();
                msg.what=0;
                msg.arg1=what;
                msg.obj=result;
                handler.sendMessage(msg);
                Log.e("HttpManager", "Post方式请求成功，result--->");
            } else {
                Message msg=new Message();
                msg.what=1;
                msg.arg1=what;
                handler.sendMessage(msg);
                Log.e("HttpManager", "Post方式请求失败"+urlConn.getResponseCode());
            }
            // 关闭连接
            urlConn.disconnect();

        }catch (Exception e){
            mListener.onErrResult(what,e);
        }
    }

    /**
     * 下载
     * @param what
     * @param params
     * @param listener
     */
    private static void doDownLoad(int what, RequestParams params, CallBack.DownLoadListener listener) {
        mDownListener = listener;
        try{
            // 新建一个URL对象
            URL url = new URL(params.getBaseUrl());
            // 打开一个HttpURLConnection连接
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            // 设置连接主机超时时间
            urlConn.setConnectTimeout(params.getTimeOut());
            //设置从主机读取数据超时
            urlConn.setReadTimeout(params.getTimeOut());
            // 设置是否使用缓存  默认是true
            urlConn.setUseCaches(params.getUseCache());
            // 设置为Post请求
            urlConn.setRequestMethod("GET");
            //urlConn设置请求头信息
            //设置请求中的媒体类型信息。
            urlConn.setRequestProperty("Content-Type", "application/json");
            //设置客户端与服务连接类型
            urlConn.addRequestProperty("Connection", "Keep-Alive");
            // 开始连接
            urlConn.connect();
            //获取内容长度
            int contentLength = urlConn.getContentLength();

            // 判断请求是否成功
            if (urlConn.getResponseCode() == 200) {
                String filePath=params.getFilePath()+params.getFileName();
                File descFile = new File(filePath);
                FileOutputStream fos = new FileOutputStream(descFile);
                byte[] buffer = new byte[1024];
                int len;
                long totalReaded =0;
                Message msg=new Message();
                msg.what=2;
                msg.arg1=what;
                handler.sendMessage(msg);
                InputStream inputStream = urlConn.getInputStream();
                while ((len = inputStream.read(buffer)) != -1) {
                    // 写到本地
                    totalReaded+=len;
                    long progress = totalReaded * 100 / contentLength;
                    msg.what = 3;
                    msg.obj = progress;
                    msg.arg1 = what;
                    handler.sendMessage(msg);
                    if(progress==100){
                        msg.what = 5;
                        msg.obj = "下载成功";
                        msg.arg1 = what;
                        handler.sendMessage(msg);
                    }
                    fos.write(buffer, 0, len);
                }
            } else {
                Message msg1 = new Message();
                msg1.what = 4;
                msg1.arg1 = what;
                handler.sendMessage(msg1);
                Log.e("HttpManager", "文件下载失败");
            }
            // 关闭连接
            urlConn.disconnect();

        }catch (Exception e){
            Log.e("HttpManager", "文件下载失败"+e);
            mDownListener.onErr(what,new Exception("下载失败"));
        }
    }

    /**
     * 上传
     * @param what
     * @param params
     * @param listener
     */
    private static void doUpload(int what, RequestParams params, CallBack.DownLoadListener listener) {
        mDownListener = listener;
        try{
            // 新建一个URL对象
            URL url = new URL(params.getBaseUrl());
            // 打开一个HttpURLConnection连接
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            // 设置连接主机超时时间
            urlConn.setConnectTimeout(params.getTimeOut());
            //设置从主机读取数据超时
            urlConn.setReadTimeout(params.getTimeOut());
            // 设置是否使用缓存  默认是true
            urlConn.setUseCaches(params.getUseCache());
            // 设置为Post请求
            urlConn.setRequestMethod("POST");
            //urlConn设置请求头信息
            //设置请求中的媒体类型信息。
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //设置客户端与服务连接类型
            urlConn.addRequestProperty("Connection", "Keep-Alive");
            // 开始连接
            urlConn.connect();
            //设置文件类型
            urlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "*****");
            //设置维持长连接
            urlConn.setRequestProperty("connection", "Keep-Alive");
            //设置文件字符集
            urlConn.setRequestProperty("Accept-Charset", "UTF-8");
            //设置文件类型
            urlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "*****");
            DataOutputStream requestStream = new DataOutputStream(urlConn.getOutputStream());
            requestStream.writeBytes("--" + "*****" + "\r\n");
            requestStream.writeBytes(params.getUpLoadParams());

            int bytesRead;
            byte[] buffer = new byte[1024];

            long totalReaded =0;
            int file_length=0;
            Message msg=new Message();
            msg.what=2;
            msg.arg1=what;
            handler.sendMessage(msg);
            if(params.getUpLoadMap()!=null) {
                for (String key : params.getUpLoadMap().keySet()) {
                    FileInputStream fileInput=new FileInputStream(params.getUpLoadMap().get(key));
                    DataInputStream in = new DataInputStream(fileInput);
                    file_length=fileInput.available();
                    while ((bytesRead = in.read(buffer)) != -1) {
                        totalReaded += bytesRead;
                        long progress = totalReaded * 100 / file_length;
                        msg.what = 3;
                        msg.obj = progress;
                        msg.arg1 = what;
                        handler.sendMessage(msg);
                        requestStream.write(buffer, 0, bytesRead);
                    }
                    fileInput.close();
                }
            }
            requestStream.writeBytes("\r\n");
            requestStream.flush();
            requestStream.writeBytes("--" + "*****" + "--" + "\r\n");
            requestStream.flush();

            int statusCode = urlConn.getResponseCode();
            if (statusCode == 200) {
                // 获取返回的数据
                String result = streamToString(urlConn.getInputStream());
                msg.what = 5;
                msg.obj = result;
                msg.arg1 = what;
                handler.sendMessage(msg);
                Log.e("HttpManager", "上传成功，result--->" + result);
            } else {
                Log.e("HttpManager", "上传失败");
                msg.what = 4;
                msg.arg1 = what;
                handler.sendMessage(msg);
            }


        }catch (Exception e){
            Log.e("HttpManager", "上传失败");
            Message msg=new Message();
            msg.what = 4;
            msg.arg1 = what;
            handler.sendMessage(msg);
        }
    }

    /**
     * 将输入流转换成字符串
     *
     * @param is 从网络获取的输入流
     * @return
     */
    private static String streamToString(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            baos.close();
            is.close();
            byte[] byteArray = baos.toByteArray();
            return new String(byteArray);
        } catch (Exception e) {
            Log.e("HttpManager", e.toString());
            return null;
        }
    }

    /**
     * 提示消息
     * @param msg
     */
    public static void showToast(String msg){
        Toast.makeText(mContext,msg,Toast.LENGTH_SHORT).show();
    }

    /**
     * 调用拨号界面
     * @param phone 电话号码
     */
    public static void call(String phone) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+phone));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /**
     * log日志输出
     * @param keys
     * @param msg
     */
    public static void log(String keys,String msg){
        Log.d("HttpManager", keys+"--------->"+msg);
    }

    /**
     * 简单的页面跳转
     * @param cla
     */
    public static void intent(Class<?>cla){
        Intent intent=new Intent(mContext,cla);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /**
     * 简单的页面跳转和数据
     * @param cla
     */
    public static void intent(Class<?>cla,String s){
        Intent intent=new Intent(mContext,cla);
        intent.putExtra("tag",s);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /**
     * 判断网络连接状态
     * @return
     */
    public static boolean isNetworkAvailable() {
        Context context = mContext.getApplicationContext();
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        } else {
            // 获取NetworkInfo对象
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

            if (networkInfo != null && networkInfo.length > 0) {
                for (int i = 0; i < networkInfo.length; i++) {
                    // 判断当前网络状态是否为连接状态
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 创建带进度的RequestBody
     * @param contentType MediaType
     * @param file  准备上传的文件
     * @param listener 回调
     * @param <T>
     * @return
     */
    public static <T> RequestBody createProgressRequestBody(final MediaType contentType, final File file, final int what, CallBack.DownLoadListener listener) {
        final Message msg=new Message();
        msg.what=2;
        msg.arg1=what;
        handler.sendMessage(msg);
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return file.length();
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source;
                try {
                    source = Okio.source(file);
                    Buffer buf = new Buffer();
                    long remaining = contentLength();
                    long current = 0;
                    for (long readCount; (readCount = source.read(buf, 2048)) != -1; ) {
                        sink.write(buf, readCount);
                        current += readCount;
//                        progressCallBack(remaining, current, callBack);
                        long progress = current * 100 / remaining;
                        final Message msg2=new Message();
                        msg2.what = 3;
                        msg2.obj = progress;
                        msg2.arg1 = what;
                        handler.sendMessage(msg2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private static List<String> removeDuplicate(List<String> list)
    {
        Set set = new LinkedHashSet<String>();
        set.addAll(list);
        list.clear();
        list.addAll(set);
        return list;
    }

}
