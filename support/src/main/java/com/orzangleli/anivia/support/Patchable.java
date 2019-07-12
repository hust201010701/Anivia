package com.orzangleli.anivia.support;

/**
 * <p>description：代表可修补的接口
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/9 下午2:35
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public interface Patchable {
    /**
     * 是否需要修补某个方法方法
     * @param methodId 方法的id
     * @return
     */
    boolean isPatchable(String methodId);
    
    /**
     * 转交参数和对象，执行补丁中的方法
     * @param methodId
     * @param isStaticMethod
     * @param object
     * @param paramValues
     * @return 返回执行补丁方法的结果
     */
    Object transferAction(String methodId, boolean isStaticMethod, Object object, Object[] paramValues);
}
