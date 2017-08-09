package com.codyy.uploader.service;

import android.util.Log;

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
 * Created by lijian on 2017/8/8.
 */

public class SocketHttpRequester {
    /**
     * 多文件上传
     * 直接通过HTTP协议提交数据到服务器,实现如下面表单提交功能:
     * <FORM METHOD=POST ACTION="http://192.168.1.101:8083/upload/servlet/UploadServlet" enctype="multipart/form-data">
     * <INPUT TYPE="text" NAME="name">
     * <INPUT TYPE="text" NAME="id">
     * <input type="file" name="imagefile"/>
     * <input type="file" name="zip"/>
     * </FORM>
     *
     * @param uploadUrl 上传路径(注：避免使用localhost或127.0.0.1这样的路径测试，因为它会指向手机模拟器，你可以使用http://www.iteye.cn或http://192.168.1.101:8083这样的路径测试)
     * @param params    请求参数 key为参数名,value为参数值
     * @param files     上传文件
     */
    public static boolean post(String uploadUrl, Map<String, String> params, FormFile[] files) {
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
                        outStream.write(buffer, 0, len);
                        sendLength += len;
                        Uog.d("progress", new UploadStatus(sendLength, onlyFileDataLength).getPercent()+":"+sendLength+":"+onlyFileDataLength);
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
            if (isUploadSuccess) Log.d("Result", "成功");
            else Log.d("Result", "失败");
            /*if (TextUtils.isEmpty(source) || !source.contains("200")) {//读取web服务器返回的数据，判断请求码是否为200，如果不是200，代表请求失败
                return false;
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                for (FormFile formFile : files) {
                    if (formFile.getInputStream() != null) {
                        formFile.getInputStream().close();
                    }
                }
                if (outStream != null)
                    outStream.close();
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 单文件上传
     * 提交数据到服务器
     *
     * @param uploadUrl 上传路径(注：避免使用localhost或127.0.0.1这样的路径测试，因为它会指向手机模拟器，你可以使用http://www.itcast.cn或http://192.168.1.10:8080这样的路径测试)
     * @param params    请求参数 key为参数名,value为参数值
     * @param file      上传文件
     */
    public static boolean post(String uploadUrl, Map<String, String> params, FormFile file) {
        return post(uploadUrl, params, new FormFile[]{file});
    }
}
