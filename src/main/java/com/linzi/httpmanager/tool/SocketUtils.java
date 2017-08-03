package com.linzi.httpmanager.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.squareup.okhttp.OkHttpClient;

public class SocketUtils {
	public static interface ServiceMsgListener{
		public void SuccessMsg(String msg);
		public void ErrMsg(String msg);
	}
	private static ServiceMsgListener mListener;//请求结果回调

	private static String Host=null;//长连接主机ip
	private static int Tcp_id;//长连接开放端口
	static Socket socket = null;
	static String buffer = "";
	static OutputStream ou;
	static InputStreamReader in;
	/**
	 * 初始化工具类
	 * @param host 主机ip
	 * @param tcp_id 开放端口
	 * @param listener 结果回调监听
	 */
	public static void init(String host,int tcp_id,ServiceMsgListener listener){
		Host=host;
		Tcp_id=tcp_id;
		mListener=listener;
	}
	/**
	 * 发送消息（在初始化之后调用）
	 * @param msg 消息体
	 */
	public static void SendAndGetMsg(String msg){
			MyThread thread = new MyThread(msg);
			thread.start();
	}



	/**
	 * 用handler进行消息传递
	 */
	public static Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			if (msg.what == 0x11) {

				mListener.SuccessMsg(bundle.getString("msg"));
			}else{
				mListener.ErrMsg(bundle.getString("msg"));
			}
		}

	};

	/**
	 * 子线程 用于访问socket服务
	 * @author linzi
	 *
	 */
	static class MyThread extends Thread {

		public String txt1;

		public MyThread(String str) {
			txt1 = str;
		}

		@Override
		public void run() {
			//定义消息
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.clear();
			try {
				//连接服务器 并设置连接超时为5秒
				if(socket==null) {
					if(!socket.isConnected()) {
						socket = new Socket();
						socket.connect(new InetSocketAddress(Host, Tcp_id), 5000);
						//获取输入输出流
						 ou = socket.getOutputStream();
						 in=new InputStreamReader(socket.getInputStream());
					}
				}
				BufferedReader bff = new BufferedReader(in);
				//读取发来服务器信息
				String line = null;
				buffer="";
				while ((line = bff.readLine()) != null) {
					buffer = line + buffer;
				}

				//向服务器发送信息
				ou.write(txt1.getBytes("utf-8"));
				ou.flush();
				bundle.putString("msg", buffer.toString());
				msg.what = 0x11;
				msg.setData(bundle);
				//发送消息 修改UI线程中的组件
				myHandler.sendMessage(msg);
				//关闭各种输入输出流
//				bff.close();
//				ou.close();
//				socket.close();
			} catch (SocketTimeoutException aa) {
				//连接超时 在UI界面显示消息
				//链接服务器

				bundle.putString("msg", "服务器连接失败！请检查网络是否打开");
				msg.what = 0x12;
				msg.setData(bundle);
				//发送消息 修改UI线程中的组件
				myHandler.sendMessage(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
