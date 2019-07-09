package com.orzangleli.anivia;

import android.util.Log;
import com.orzangleli.anivia.util.AniviaLog;

/**
 * <p>description：打补丁的代理类,在调用 {@link Patchable} 的方法前可以一些公共的事情，如日志
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/9 下午3:36
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class PatchProxy {
    /**
     * 代理是否可以打补丁的方法
     * @param methodId          方法id
     * @param isStaticMethod    是否是静态方法
     * @param object            调用这个方法的对象
     * @param patchable         打补丁的类
     * @param paramValues       参数值数组
     * @param paramTypes        参数类型数组
     * @param returnType        返回值类型
     * @return
     */
    public static boolean isPatchable(String methodId, boolean isStaticMethod, Object object, Patchable patchable, Object[] paramValues, Class[] paramTypes, Class returnType) {
        AniviaLog.i("check isPatchable --> methodId = " + methodId);
        if (patchable == null) {
            return false;
        }
        return patchable.isPatchable(methodId);
    }
    
    /**
     * 代理执行补丁修复方法
     * @param methodId          方法id
     * @param isStaticMethod    是否是静态方法
     * @param object            调用这个方法的对象
     * @param patchable         打补丁的类
     * @param paramValues       参数值数组
     * @param paramTypes        参数类型数组
     * @param returnType        返回值类型
     * @return
     */
    public static Object transferAction(String methodId, boolean isStaticMethod, Object object, Patchable patchable, Object[] paramValues, Class[] paramTypes, Class returnType) {
        AniviaLog.i("transferAction --> methodId = " + methodId);
        if (patchable == null) {
            return null;
        }
        return patchable.transferAction(methodId, isStaticMethod, object, paramValues);
    }
}
