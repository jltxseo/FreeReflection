package me.weishu.freereflection.app;

import android.app.Application;
import android.content.Context;

import me.weishu.reflection.Reflection;

/**
 * @author jltxseo
 * Created by junlintianxia on 2019/05/23.
 */
public class CommonApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);
    }
}
