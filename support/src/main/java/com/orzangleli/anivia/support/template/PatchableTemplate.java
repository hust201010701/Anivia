package com.orzangleli.anivia.support.template;

import com.orzangleli.anivia.support.Patchable;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/12 上午11:28
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class PatchableTemplate implements Patchable {
    private static final Map<Object, Object> patchedMap = new WeakHashMap<>();
    private static final Set<String> allPatchableMethodIds = new HashSet<>();
    
    public PatchableTemplate() {
        appendPatchableMethodIds();
    }
    
    /**
     * 在这个方法里添加所有需要打补丁的方法
     */
    public void appendPatchableMethodIds() {
    
    }
    
    @Override public boolean isPatchable(String methodId) {
        return allPatchableMethodIds.contains(methodId);
    }
    
    @Override public Object transferAction(String methodId, boolean isStaticMethod, Object object, Object[] paramValues) {
        return null;
    }
}
