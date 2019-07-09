package com.orzangleli.anivia.util;

import android.util.Log;

/**
 * <p>description：日志工具类
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/9 下午3:44
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class AniviaLog {
    
    /**
     * 是否是 debug 状态
     */
    private static boolean DEBUG = false;
    
    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }
    
    public static boolean isDebug() {
        return DEBUG;
    }
    
    public static void i(String content) {
        if (isDebug()) {
            Log.i("Anivia", " ---> " + content);
        }
    }
    
    public static void w(String content) {
        if (isDebug()) {
            Log.w("Anivia", " ---> " + content);
        }
    }
    
    public static void e(String content) {
        if (isDebug()) {
            Log.e("Anivia", " ---> " + content);
        }
    }
    
    public static void d(String content) {
        if (isDebug()) {
            Log.d("Anivia", " ---> " + content);
        }
    }
}
