package com.linzi.httpmanager;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.linzi.httpmanager.tool.CallBack;
import com.linzi.httpmanager.tool.LoadDialog;
import com.linzi.httpmanager.tool.RequestParams;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by linzi on 2017/4/21.
 */

public class HttpManager {
    private static Context mContext;
    private static CallBack.LoadCallBackListener mListener;//请求结果回调
    private static CallBack.DownLoadListener mDownListener;//下载和上传结果回调
    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();//线程池
    private String[] Context_type={"application/x-www-form-urlencoded","application/json"};
    private static Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    mListener.onFinishResult(msg.arg1,msg.obj.toString());
                    break;
                case 1:
                    mListener.onErrResult(msg.arg1,new Exception("请求失败"));
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

    public static void doGet(final int what, final RequestParams params, final CallBack.LoadCallBackListener listener){
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                doGetForJson(what,params,listener);
            }
        });
    }
    public static void doPost(final int what, final RequestParams params, final CallBack.LoadCallBackListener listener){
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                doPostForJson(what,params,listener);
            }
        });
    }
    public static void doLoad(final int what, final RequestParams params, final CallBack.DownLoadListener listener){
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                doDownLoad(what,params,listener);
            }
        });
    }
    public static void upLoad(final int what, final RequestParams params, final CallBack.DownLoadListener listener){
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                doUpload(what,params,listener);
            }
        });
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
            String request_url=params.getBaseUrl()+"?"+params.getParams();
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
            dos.write(postData);
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
                FileOutputStream fos = new FileOutputStream(descFile);;
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
            //获取需要上传的文件路径，并实例化
            File file = new File(params.getFilePath());
            //设置文件类型
            urlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "*****");
            //设置维持长连接
            urlConn.setRequestProperty("connection", "Keep-Alive");
            //设置文件字符集
            urlConn.setRequestProperty("Accept-Charset", "UTF-8");
            //设置文件类型
            urlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "*****");
//            String name = file.getName();
            DataOutputStream requestStream = new DataOutputStream(urlConn.getOutputStream());
            requestStream.writeBytes("--" + "*****" + "\r\n");
            requestStream.writeBytes(params.getUpLoadParams());
            //发送文件数据
            FileInputStream fileInput = new FileInputStream(file);
            int bytesRead;
            byte[] buffer = new byte[1024];

            long totalReaded =0;
            int file_length=fileInput.available();
            Message msg=new Message();
            msg.what=2;
            msg.arg1=what;
            handler.sendMessage(msg);

            DataInputStream in = new DataInputStream(new FileInputStream(file));
            while ((bytesRead = in.read(buffer)) != -1) {
                totalReaded+=bytesRead;
                long progress = totalReaded * 100 / file_length;
                msg.what = 3;
                msg.obj = progress;
                msg.arg1 = what;
                handler.sendMessage(msg);
                requestStream.write(buffer, 0, bytesRead);
            }
            requestStream.writeBytes("\r\n");
            requestStream.flush();
            requestStream.writeBytes("--" + "*****" + "--" + "\r\n");
            requestStream.flush();
            fileInput.close();
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
     * log日志输出
     * @param keys
     * @param msg
     */
    public static void log(String keys,String msg){
        Log.d(keys, keys+"--------->"+msg);
    }

    /**
     * 简单的页面跳转
     * @param cla
     */
    public static void intent(Class<?>cla){
        Intent intent=new Intent(mContext,cla);
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

}
