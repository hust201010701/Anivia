package com.orzangleli.anivia.demo;

import android.util.Log;
import com.orzangleli.anivia.support.annotation.Repair;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/10 下午2:38
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class Test {
    @Repair
    public static void test(String str) {
        Log.i("lxc", str + " ---> test repaired");
    }
}
