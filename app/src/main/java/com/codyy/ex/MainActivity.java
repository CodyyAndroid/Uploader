package com.codyy.ex;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bilibili.boxing.Boxing;
import com.bilibili.boxing.model.config.BoxingConfig;
import com.bilibili.boxing.model.entity.BaseMedia;
import com.bilibili.boxing_impl.ui.BoxingActivity;
import com.codyy.uploader.Uploader;
import com.codyy.uploader.service.FormFile;
import com.codyy.uploader.service.SimpleUploadListener;
import com.codyy.uploader.service.SocketHttpRequester;
import com.codyy.uploader.service.UploadConnectedListener;
import com.codyy.uploader.service.UploadStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.tv_state);
        Uploader.getInstance().bind(this, new UploadConnectedListener() {
            @Override
            public void onConnected() {
                Uploader.getInstance().setUploadListener(new SimpleUploadListener() {
                    @Override
                    public void onComplete(String result) {
                        super.onComplete(result);
                        mTextView.setText(result);
                        Log.d(TAG, result);
                    }

                    @Override
                    public void onError(Exception e) {
                        super.onError(e);
                        mTextView.setText(e.getMessage());
                        Log.e(TAG, "Error", e.getCause());
                    }

                    @Override
                    public void onProgress(UploadStatus status) {
                        super.onProgress(status);
                        mTextView.setText(status.getPercent());
                        Log.d(TAG, status.getPercent());
                    }

                    @Override
                    public void onCancel() {
                        super.onCancel();
                        mTextView.setText("取消文件上传");

                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Uploader.getInstance().unbind(this);
    }

    public void onClick(View view) {
        Boxing.of((new BoxingConfig(BoxingConfig.Mode.MULTI_IMG)).withMaxCount(50)).withIntent(this, BoxingActivity.class).start(this, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            ArrayList<BaseMedia> mImageList = Boxing.getResult(data);
            if (mImageList == null) return;
            final List<FormFile> formFiles = new ArrayList<>();
            for (final BaseMedia media : mImageList) {
                FormFile formFile = new FormFile(media.getPath());
                formFiles.add(formFile);
            }
            Map<String, String> params = new HashMap<>();
            params.put("uuid", "MOBILE:cd3b602457914bb0b2b0f0f2bd478124");
            Uploader.getInstance().post("http://10.5.227.32:9999/upload", params, formFiles.toArray(new FormFile[formFiles.size()]));

//                    SocketHttpRequester.post("http://10.5.227.32:9999/upload", params, formFiles.toArray(new FormFile[formFiles.size()]));
//                    SocketHttpRequester.post("http://devdebug.9itest.com/OM/mobile/managercenter/uploadAvatar.do", params, formFile);
        }
    }

    public void onCancel(View view) {
        Uploader.getInstance().cancel();
    }
}
