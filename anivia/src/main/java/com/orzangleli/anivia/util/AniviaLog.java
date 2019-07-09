package com.orzangleli.anivia.util;

import android.util.Log;
import com.orzangleli.anivia.Anivia;

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
    public static void i(String content) {
        if (Anivia.isDebug()) {
            Log.i("Anivia", " ---> " + content);
        }
    }
    
    public static void w(String content) {
        if (Anivia.isDebug()) {
            Log.w("Anivia", " ---> " + content);
        }
    }
    
    public static void e(String content) {
        if (Anivia.isDebug()) {
            Log.e("Anivia", " ---> " + content);
        }
    }
    
    public static void d(String content) {
        if (Anivia.isDebug()) {
            Log.d("Anivia", " ---> " + content);
        }
    }
}
