package com.orzangleli.anivia.patchgenerateplugin;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/11 下午9:53
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class Constants {
    
    public static final Set RFileClassSet = new HashSet();
    
    static {
        RFileClassSet.add("R$array");
        RFileClassSet.add("R$xml");
        RFileClassSet.add("R$styleable");
        RFileClassSet.add("R$style");
        RFileClassSet.add("R$string");
        RFileClassSet.add("R$raw");
        RFileClassSet.add("R$menu");
        RFileClassSet.add("R$layout");
        RFileClassSet.add("R$integer");
        RFileClassSet.add("R$id");
        RFileClassSet.add("R$drawable");
        RFileClassSet.add("R$dimen");
        RFileClassSet.add("R$color");
        RFileClassSet.add("R$bool");
        RFileClassSet.add("R$attr");
        RFileClassSet.add("R$anim");
    }
}
