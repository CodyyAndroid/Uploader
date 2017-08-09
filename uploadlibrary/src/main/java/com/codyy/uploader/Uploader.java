package com.codyy.uploader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.codyy.uploader.service.FormFile;
import com.codyy.uploader.service.UploadConnectedListener;
import com.codyy.uploader.service.UploadListener;
import com.codyy.uploader.service.UploadService;

import java.util.List;
import java.util.Map;

/**
 * Created by lijian on 2017/8/9.
 */

public class Uploader {
    private static volatile Uploader INSTANCE;
    private static volatile boolean BOUND = false;
    public static boolean DEBUG = true;

    public static Uploader getInstance() {
        if (INSTANCE == null) {
            synchronized (Uploader.class) {
                INSTANCE = new Uploader();
            }
        }
        return INSTANCE;
    }

    public static void init(boolean debug) {
        DEBUG = debug;
    }

    private ServiceConnection mServiceConnection;
    private UploadService mUploadService;

    public void bind(Context context, final UploadConnectedListener uploadConnectedListener) {
        if (BOUND) return;
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                BOUND = true;
                mUploadService = ((UploadService.UploaderBinder) service).getUploadService();
                if (uploadConnectedListener != null) {
                    uploadConnectedListener.onConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                BOUND = false;
            }
        };
        Intent intent = new Intent(context, UploadService.class);
        context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbind(Context context) {
        if (mServiceConnection != null)
            context.unbindService(mServiceConnection);
    }

    public void cancel() {
        if (mUploadService == null) {
            throw new NullPointerException("Uploader was unbind,you need to bind before upload files");
        }
        mUploadService.cancel();
    }

    /**
     * 单文件上传
     *
     * @param uploadUrl 上传地址
     * @param params    上传参数
     * @param file      文件
     */
    public void post(String uploadUrl, Map<String, String> params, FormFile file) {
        post(uploadUrl, params, new FormFile[]{file});
    }

    /**
     * 多文件上传
     *
     * @param uploadUrl 上传地址
     * @param params    参数
     * @param files     文件列表
     */
    public void post(String uploadUrl, Map<String, String> params, List<FormFile> files) {
        post(uploadUrl, params, files.toArray(new FormFile[files.size()]));
    }

    /**
     * 多文件上传
     *
     * @param uploadUrl 上传地址
     * @param params    上传参数
     * @param files     文件列表
     */
    public void post(String uploadUrl, Map<String, String> params, FormFile[] files) {
        if (mUploadService == null) {
            throw new NullPointerException("Uploader was unbind,you need to bind before upload files");
        }
        mUploadService.post(uploadUrl, params, files);
    }

    public void setUploadListener(UploadListener uploadListener) {
        if (uploadListener == null) return;
        if (mUploadService == null) {
            throw new NullPointerException("Uploader was unbind,you need to bind before upload files");
        } else {
            mUploadService.setUploadListener(uploadListener);
        }
    }
}
