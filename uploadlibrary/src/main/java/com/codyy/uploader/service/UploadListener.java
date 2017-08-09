package com.codyy.uploader.service;

/**
 * Created by lijian on 2017/6/7.
 */

public interface UploadListener {


    //返回当前下载进度的百分比
    void onProgress(UploadStatus status);

    /*下载完成*/
    void onComplete(String result);

    void onError(Exception e);

    void onCancel();
}
