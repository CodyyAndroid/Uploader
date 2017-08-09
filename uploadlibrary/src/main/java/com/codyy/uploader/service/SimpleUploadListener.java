package com.codyy.uploader.service;

/**
 * Created by lijian on 2017/8/9.
 */

public abstract class SimpleUploadListener implements UploadListener {
    @Override
    public void onComplete(String result) {

    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onProgress(UploadStatus status) {

    }

    @Override
    public void onCancel() {

    }
}
