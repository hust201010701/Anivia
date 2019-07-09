package com.orzangleli.anivia;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import java.lang.reflect.Method;

/**
 * <p>description：Anivia 的配置入口类
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/8 上午11:19
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class Anivia {
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
}
