package com.codyy.uploader.service;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 文件上传状态
 * Created by lijian on 2017/6/7.
 * @version 1.1.8
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface UploadFlag {
    int PROGRESS = 9996;
    int COMPLETED = 9997;
    int CANCEL = 9998;
    int ERROR = 9999;
}
