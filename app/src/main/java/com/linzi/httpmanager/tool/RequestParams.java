package com.linzi.httpmanager.tool;

import android.os.Environment;
import android.util.Log;

import java.net.URLEncoder;
import java.util.HashMap;

/**
 * Created by linzi on 2017/4/21.
 */

public class RequestParams {
    private String mUrl;//接口地址
    private StringBuilder tempParams = new StringBuilder();//请求参数
    private HashMap<String, String> paramsMap=new HashMap<>();//请求参数的键值
    private int timeout=100000;//访问超时的时间，默认10s
    private boolean mUseCache=false;//是否使用缓存策略，默认不使用
    private String file_path= Environment.getExternalStorageDirectory().getPath()+"/DownLoad/";
    private String file_name;

    public RequestParams(String url){
        this.mUrl=url;
    }

    /**
     * 添加请求参数
     * @param key 参数关键字
     * @param value 参数值
     */
    public void addRequestBody(String key,String value){
        paramsMap.put(key,value);
    }

    /**
     * 获取请求参数
     * @return
     * @throws Exception
     */
    public String getParams()throws Exception{
        int pos = 0;
        for (String key : paramsMap.keySet()) {
            if (pos > 0) {
                tempParams.append("&");
            }
            tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
            Log.d("请求参数", key+":"+paramsMap.get(key));
            pos++;
        }
        return tempParams.toString();
    }
    /**
     * 获取请求参数
     * @return
     * @throws Exception
     */
    public String getUpLoadParams()throws Exception{
        int pos = 0;
        int size=paramsMap.size();
        for (String key : paramsMap.keySet()) {
            tempParams.append( String.format("%s=\"%s\"", key, paramsMap.get(key), "utf-8"));
            if (pos < size-1) {
                tempParams.append("; ");
            }
            Log.d("请求参数", key+":"+paramsMap.get(key));
            pos++;
        }
        tempParams.append("\r\n");
        tempParams.append("Content-Type: application/octet-stream\r\n");
        tempParams.append("\r\n");
        return tempParams.toString();
    }

    /**
     * 获取请求服务器的地址头部
     * @return
     */
    public String getBaseUrl(){
        Log.d("请求链接", mUrl);
        return mUrl;
    }

    /**
     * 设置请求超时时间
     * @param time
     */
    public void setTimeOut(int time){
        timeout=time;
    }

    /**
     * 获取请求超时的时间
     * @return
     */
    public int getTimeOut(){
        Log.d("超时时间", ""+timeout);
        return timeout;
    }

    /**
     * 设置是否缓存
     * @param isUse
     */
    public void setUseCache(boolean isUse){
        mUseCache=isUse;
    }

    /**
     * 获取缓存策略
     * @return
     */
    public boolean getUseCache(){
        Log.d("是否使用缓存", ""+mUseCache);
        return mUseCache;
    }

    /**
     * 设置下载文件的位置路径
     * @param path
     */
    public void setFilePath(String path){
        this.file_path=path;
    }

    /**
     *获取自定义的下载路径
     * @return
     */
    public String getFilePath(){
        Log.d("文件下载路径", ""+file_path);
        return file_path;
    }

    /**
     * 设置下载的文件的名字
     * @param name
     */
    public void setFileName(String name){
        this.file_name=name;
    }

    /**
     * 获取自定义的文件名
     * @return
     */
    public String getFileName(){
        Log.d("文件名", ""+file_name);
        return file_name;
    }
}
