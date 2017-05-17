package com.linzi.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.linzi.httpmanager.HttpManager;
import com.linzi.httpmanager.tool.CallBack;
import com.linzi.httpmanager.tool.LoadDialog;
import com.linzi.httpmanager.tool.RequestParams;

import java.io.File;

/**
 * Created by linzi on 2017/4/26.
 */

public class Example extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HttpManager.init(this);//初始化工具
        //初始化，使用Okhttp工具
//        HttpManager.init(this,new HttpManager().Config());
//        LoadDialog.init(this);//LoadDialog默认随着HttpManager的初始化而初始化，该语句可屏蔽
        LoadDialog.init(this,100,android.R.color.white,new TextView(this));//初始化个性化的loaddialog,参数以此为：上下文、大小、背景色、自定义加载的view
    }
    private void initViews(){
        LoadDialog.showDialog();//显示加载dialog
//        LoadDialog.showDialog("加载中");//带自定义信息的加载dialog
        //初始化参数类
        RequestParams params=new RequestParams("www.baidu.com");
        params.addRequestBody("keys","value");//添加请求参数
        //get请求
        HttpManager.doGet(0, params, new CallBack.LoadCallBackListener() {
            @Override
            public void onFinishResult(int what, String result) {
                //访问成功
                LoadDialog.CancelDialog();//关闭提示框
            }
            @Override
            public void onErrResult(int what, Exception e) {
                //失败
            }
        });
        //post请求
        HttpManager.doPost(0, params, new CallBack.LoadCallBackListener() {
            @Override
            public void onFinishResult(int what, String result) {
                //访问成功
            }
            @Override
            public void onErrResult(int what, Exception e) {
                //失败
            }
        });
        //下载请求
        params.setFilePath("file_path");//设置下载路径
        params.setFileName("file_name");//设置下载保存的文件名
        HttpManager.doLoad(0, params, new CallBack.DownLoadListener() {
            @Override
            public void onStart(int what) {
                //开始下载
            }

            @Override
            public void onDownLoading(int what, long progress) {
                //下载中
            }

            @Override
            public void onFinish(int what, String result) {
                //下载完成
            }

            @Override
            public void onErr(int what, Exception e) {
                //下载失败
            }
        });

        //上传请求暂时未测试
        params.addRequestBody("key","values");//文本参数
        params.addUpLoadFile("key",new File("path"));//文件参数
        HttpManager.upLoad(0, params, new CallBack.DownLoadListener() {
            @Override
            public void onStart(int what) {
                //开始上传
            }

            @Override
            public void onDownLoading(int what, long progress) {
                //上传中
            }

            @Override
            public void onFinish(int what, String result) {
                //上传完成
            }

            @Override
            public void onErr(int what, Exception e) {
                //上传失败
            }
        });
    }
}
