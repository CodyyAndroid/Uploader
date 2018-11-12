package com.codyy.uploader.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.codyy.uploader.exception.CancelUploadException;
import com.codyy.uploader.threadpool.ThreadPoolType;
import com.codyy.uploader.threadpool.ThreadPoolUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Map;

/**
 * upload Service
 * Created by lijian on 2017/8/9.
 */

public class UploadService extends Service implements Handler.Callback {
    private Handler mHandler;
    private ThreadPoolUtils mThreadPoolUtils = new ThreadPoolUtils(ThreadPoolType.SINGLE_THREAD, 1);

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUploadListener = null;
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new UploaderBinder();
    }

    public void post(String uploadUrl, Map<String, String> params, FormFile[] files) {
        isCancel = false;
        mThreadPoolUtils.execute(new UploadThread(uploadUrl, params, files));
    }

    private volatile boolean isCancel;

    public void cancel() {
        isCancel = true;
    }

    private class UploadThread implements Runnable {
        private String uploadUrl;
        private Map<String, String> params;
        private FormFile[] files;

        public UploadThread(String uploadUrl, Map<String, String> params, FormFile[] files) {
            this.uploadUrl = uploadUrl;
            this.params = params;
            this.files = files;
        }

        @Override
        public void run() {
            if (isCancel) return;
            final String BOUNDARY = "---------------------------7da2137580612"; //数据分隔线
            final String endLine = "--" + BOUNDARY + "--\r\n";//数据结束标志
            Socket socket = null;
            OutputStream outStream = null;
            long fileDataLength = 0;
            long onlyFileDataLength = 0;
            try {
                for (FormFile uploadFile : files) {//得到文件类型数据的总长度
                    StringBuilder fileExplain = new StringBuilder();
                    fileExplain.append("--");
                    fileExplain.append(BOUNDARY);
                    fileExplain.append("\r\n");
                    fileExplain.append("Content-Disposition: form-data;name=\"" + uploadFile.getParameterName() + "\";filename=\"" + uploadFile.getFileName() + "\"\r\n");
                    fileExplain.append("Content-Type: " + uploadFile.getContentType() + "\r\n\r\n");
                    fileExplain.append("\r\n");
                    fileDataLength += fileExplain.length();
                    if (uploadFile.getInputStream() != null) {
                        fileDataLength += uploadFile.getFile().length();
                        onlyFileDataLength += uploadFile.getFile().length();
                    }
                }
                StringBuilder textEntity = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {//构造文本类型参数的实体数据
                    textEntity.append("--");
                    textEntity.append(BOUNDARY);
                    textEntity.append("\r\n");
                    textEntity.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                    textEntity.append(entry.getValue());
                    textEntity.append("\r\n");
                }
                //计算传输给服务器的实体数据总长度
                long dataLength = textEntity.toString().getBytes().length + fileDataLength + endLine.getBytes().length;

                URL url = new URL(uploadUrl);
                int port = url.getPort() == -1 ? 80 : url.getPort();
                socket = new Socket(InetAddress.getByName(url.getHost()), port);
                outStream = socket.getOutputStream();
                //下面完成HTTP请求头的发送
                String requestMethod = "POST " + url.getPath() + " HTTP/1.1\r\n";
                outStream.write(requestMethod.getBytes());
                String accept = "Accept: image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*\r\n";
                outStream.write(accept.getBytes());
                String language = "Accept-Language: zh-CN\r\n";
                outStream.write(language.getBytes());
                String contentType = "Content-Type: multipart/form-data; boundary=" + BOUNDARY + "\r\n";
                outStream.write(contentType.getBytes());
                String contentLength = "Content-Length: " + dataLength + "\r\n";
                outStream.write(contentLength.getBytes());
                String alive = "Connection: Keep-Alive\r\n";
                outStream.write(alive.getBytes());
                String host = "Host: " + url.getHost() + ":" + port + "\r\n";
                outStream.write(host.getBytes());
                //写完HTTP请求头后根据HTTP协议再写一个回车换行
                outStream.write("\r\n".getBytes());
                //把所有文本类型的实体数据发送出来
                outStream.write(textEntity.toString().getBytes());
                long sendLength = 0;
                //把所有文件类型的实体数据发送出来
                for (FormFile uploadFile : files) {
                    StringBuilder fileEntity = new StringBuilder();
                    fileEntity.append("--");
                    fileEntity.append(BOUNDARY);
                    fileEntity.append("\r\n");
                    fileEntity.append("Content-Disposition: form-data;name=\"" + uploadFile.getParameterName() + "\";filename=\"" + uploadFile.getFileName() + "\"\r\n");
                    fileEntity.append("Content-Type: " + uploadFile.getContentType() + "\r\n\r\n");
                    outStream.write(fileEntity.toString().getBytes());
                    if (uploadFile.getInputStream() != null) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = uploadFile.getInputStream().read(buffer, 0, 1024)) != -1) {
                            if (isCancel) throw new CancelUploadException("cancel upload");
                            outStream.write(buffer, 0, len);
                            sendLength += len;
                            sendProgressMessage(new UploadStatus(sendLength, onlyFileDataLength));
                        }
                        uploadFile.getInputStream().close();
                    }
                    outStream.write("\r\n".getBytes());
                }
                //下面发送数据结束标志，表示数据已经结束
                outStream.write(endLine.getBytes());
                outStream.flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                String line;
                String source = "";
                boolean isUploadSuccess = false;
                boolean isOver = false;
                while ((line = reader.readLine()) != null && !isOver) {
                    Uog.d("line", line);
                    if (!isUploadSuccess) {
                        isUploadSuccess = (!line.contains("200"));
                    } else {
                        try {
                            JSONObject jsonObject = new JSONObject(line);
                            source = jsonObject.toString();
                            isOver = true;
                        } catch (JSONException e) {
                        }
                    }
                }
                reader.close();
                Uog.d("Source", source);
                if (isUploadSuccess) {
                    sendCompletedMessage(source);
                    Uog.d("Result", "成功");
                } else {
                    sendErrorMessage(new Exception("upload file error"));
                    Uog.d("Result", "失败");
                }

            } catch (Exception e) {
                if (e instanceof CancelUploadException) {
                    sendCancelMessage();
                } else {
                    sendErrorMessage(e);
                }
            } finally {
                try {
                    for (FormFile formFile : files) {
                        if (formFile.getInputStream() != null) {
                            formFile.getInputStream().close();
                        }
                    }
                    if (outStream != null) {
                        outStream.close();
                    }
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendProgressMessage(UploadStatus status) {
            Message message = new Message();
            message.what = UploadFlag.PROGRESS;
            message.obj = status;
            sendMessage(message);
        }

        private void sendCompletedMessage(String result) {
            Message message = new Message();
            message.what = UploadFlag.COMPLETED;
            message.obj = result;
            sendMessage(message);
        }

        private void sendErrorMessage(Exception e) {
            Message message = new Message();
            message.what = UploadFlag.ERROR;
            message.obj = e;
            sendMessage(message);
        }

        private void sendCancelMessage() {
            Message message = new Message();
            message.what = UploadFlag.CANCEL;
            sendMessage(message);
        }

        private void sendMessage(Message message) {
            if (mHandler != null) {
                mHandler.sendMessage(message);
            }
        }
    }

    private UploadListener mUploadListener;

    public void setUploadListener(UploadListener uploadListener) {
        mUploadListener = uploadListener;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (mUploadListener == null) return false;
        switch (msg.what) {
            case UploadFlag.PROGRESS:
                mUploadListener.onProgress((UploadStatus) msg.obj);
                break;
            case UploadFlag.COMPLETED:
                mUploadListener.onComplete((String) msg.obj);
                break;
            case UploadFlag.CANCEL:
                mUploadListener.onCancel();
                break;
            case UploadFlag.ERROR:
                mUploadListener.onError((Exception) msg.obj);
                break;
        }
        return true;
    }

    public class UploaderBinder extends Binder {
        public UploadService getUploadService() {
            return UploadService.this;
        }
    }


}
