package com.linzi.httpmanager.tool;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.linzi.httpmanager.R;

/**
 * Created by linzi on 2017/4/26.
 */

public class LoadDialog extends Dialog {
    static Context mContext;
    static LoadDialog dialog;
    static int mSize=100;
    static int backGround_color=android.R.color.white;
    static String mMsg="加载中";
    static View mView;
    static LinearLayout linearLayout;
    public static void init(Context context){
        mContext=context;
    }
    public static void init(Context context,int size,int backGroundcolor,View view){
        mSize=size;
        mContext=context;
        backGround_color=backGroundcolor;
        mView=view;
    }
    public LoadDialog(Context context) {
        super(context);
        linearLayout = new LinearLayout(context);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(context.getResources().getColor(backGround_color));
        linearLayout.setPadding(dip2px(context, mSize / 4), dip2px(context, mSize / 4), dip2px(context, mSize / 4), dip2px(context, mSize / 4));

        ViewGroup.LayoutParams params1 = new ViewGroup.LayoutParams(dip2px(context, mSize / 2), dip2px(context, mSize / 2));
        if (mView == null) {
            ProgressBar progressBar = new ProgressBar(context);
            progressBar.setLayoutParams(params1);
            progressBar.setIndeterminateDrawable(context.getResources().getDrawable(R.drawable.progressbar));
            linearLayout.addView(progressBar);
        } else {
            mView.setLayoutParams(params1);
            linearLayout.addView(mView);
        }
        if (!mMsg.equals("")) {
            TextView textView = new TextView(context);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(params);
            textView.setText(mMsg);
            textView.setGravity(Gravity.CENTER);
            linearLayout.addView(textView);
        }
        this.onBackPressed();
        this.setContentView(linearLayout);
        this.setCanceledOnTouchOutside(false);
        this.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if ((keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_BACK) && event.getRepeatCount() == 0) {
                    dialog.dismiss();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }
    public static void showDialog(String msg){
        mMsg=msg;
        if(mContext!=null) {
            if(dialog==null) {
                dialog = new LoadDialog(mContext);
                dialog.show();
            }else{
                dialog.show();
            }
        }
    }
    public static void showDialog(Context context){
        mContext=context;
        if(mContext!=null) {
            if(dialog==null) {
                dialog = new LoadDialog(mContext);
                dialog.setCancelable(false);
                dialog.show();
            }else{
                dialog.show();
            }
        }
    }
    public static void CancelDialog(){
        if(dialog!=null){
            if(dialog.isShowing()){
                dialog.dismiss();
                dialog=null;
                mContext=null;
            }
        }
    }
    //将dp转换为px
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
