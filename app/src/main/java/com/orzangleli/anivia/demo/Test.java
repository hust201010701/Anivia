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
    private int num = 0;
    @Repair
    public String test(String str) {
        num += 1;
        Log.i("lxc", str + " ---> test repaired" + num);
        return "" + num;
    }
}
