package com.orzangleli.anivia.support.template;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>description：补丁配置类 模板类
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/11 下午7:54
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class PatchConfigTemplate {
    private static Map<String, String> map = new HashMap<>();
    public static Map<String, String> getAllPatchableMap() {
        return map;
    }
}
