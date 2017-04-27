package com.linzi.httpmanager.tool;

/**
 * Created by linzi on 2017/4/21.
 */

public class CallBack {
    /**
     * 自定义接口，用于接收回调信息
     */
    public static interface LoadCallBackListener {
        /**
         * 接口访问成功回调方法
         * @param what 返回的队列标识
         * @param result 返回的json
         */
        public void onFinishResult(int what,String result);

        /**
         * 接口访问失败回调方法
         * @param what 返回的队列标识
         */
        public void onErrResult(int what,Exception e);

    }

    /**
     * 文件下载回调监听
     */
    public static interface DownLoadListener{
        /**
         * 开始下载
         * @param what
         */
        public void onStart(int what);

        /**
         * 进度回调
         * @param what
         * @param progress
         */
        public void onDownLoading(int what,long progress);

        /**
         * 完成
         * @param what
         * @param result
         */
        public void onFinish(int what,String result);

        /**
         * 下载失败回调
         * @param what
         */
        public void onErr(int what,Exception e);
    }
}
