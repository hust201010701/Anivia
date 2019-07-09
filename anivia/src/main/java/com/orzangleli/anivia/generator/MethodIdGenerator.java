package com.orzangleli.anivia.generator;

import com.orzangleli.anivia.util.ListUtil;
import com.orzangleli.anivia.util.Md5Util;

/**
 * <p>description：方法id 生成器
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/9 下午2:44
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class MethodIdGenerator implements Generator {
    /**
     * 约定：
     * 第一个参数为 完整类名
     * 第二个参数为 方法名
     * 第三个参数开始为 参数类型数组
     * @param args
     * @return
     */
    @Override public String generate(String... args) {
        if (args == null || args.length < 2) {
            return "";
        }
        String paramTypes = "";
        for (int i = 2; i < args.length; i++) {
            paramTypes += args[2] + ",";
        }
        String paramTypesMd5 = Md5Util.encode(paramTypes);
        return args[0] + "#" + args[1] + "@" + paramTypesMd5;
    }
}
