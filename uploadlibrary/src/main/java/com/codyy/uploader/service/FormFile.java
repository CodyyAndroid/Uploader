package com.codyy.uploader.service;

import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by lijian on 2017/8/8.
 */

public class FormFile {
    /* 文件名称 */
    private String fileName;
    private String filePath;
    /* 请求参数名称*/
    private String parameterName = "file";
    /* 内容类型 */
    private String contentType = "multipart/form-data";

    private InputStream inputStream;
    private File file;

    public FormFile() {
    }

    public FormFile(String filePath) {
        this.filePath = filePath;
        this.file = new File(filePath);
        this.fileName = file.getName();
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            inputStream = null;
        }

    }

    public FormFile(String filePath, String fileName) {
        this.file = new File(filePath);
        this.fileName = fileName;
        this.filePath = filePath;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            inputStream = null;
        }
    }

    public FormFile(String filePath, String fileName, String parameterName) {
        this.file = new File(filePath);
        this.fileName = fileName;
        this.filePath = filePath;
        this.parameterName = parameterName;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            inputStream = null;
        }
    }

    public FormFile(String filePath, String fileName, String parameterName, String contentType) {
        this.file = new File(filePath);
        this.fileName = fileName;
        this.filePath = filePath;
        this.parameterName = parameterName;
        this.contentType = contentType;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            inputStream = null;
        }
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return Uri.encode(fileName);
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getParameterName() {
        return Uri.encode(parameterName);
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
