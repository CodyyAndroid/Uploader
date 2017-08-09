package com.codyy.ex;

import android.app.Application;
import android.os.Build;

import com.bilibili.boxing.BoxingMediaLoader;
import com.bilibili.boxing.loader.IBoxingMediaLoader;
import com.codyy.uploader.Uploader;

/**
 * Created by lijian on 2017/8/8.
 */

public class UApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Uploader.init(BuildConfig.DEBUG);
        IBoxingMediaLoader loader = new BoxingFrescoLoader(this);
        BoxingMediaLoader.getInstance().init(loader);
    }
}
