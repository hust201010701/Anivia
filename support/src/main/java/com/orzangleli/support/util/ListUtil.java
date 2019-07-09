package com.orzangleli.support.util;

import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <p>description：数组工具类
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/7/9 下午3:06
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class ListUtil {
   
    public static boolean isNullOrEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }
    
   
    public static int getSize(@Nullable Collection collection) {
        return collection == null ? 0 : collection.size();
    }
    
   
    public static <V> int getSize(V[] source) {
        return null == source ? 0 : source.length;
    }
    
   
    public static <V> boolean isEmpty(List<V> sourceList) {
        return (sourceList == null || sourceList.size() == 0);
    }
    
   
    public static <V> boolean isEmpty(V[] sourceArray) {
        return (sourceArray == null || sourceArray.length == 0);
    }
    
   
    public static <T> String join(@Nullable Collection<T> src, @Nullable String separator) {
        if (src == null) {
            return "";
        }
        if (separator == null) {
            separator = "";
        }
        
        StringBuilder result = new StringBuilder();
        int index = 0;
        for (T item : src) {
            index++;
            if (item == null) {
                result.append("");
            } else {
                result.append(item.toString());
            }
            if (index != src.size()) {
                result.append(separator);
            }
        }
        
        return result.toString();
    }
    
   
    public static @Nullable List<String> split(String raw, String separator) {
        if (null == raw) {
            return null;
        }
        if (null == separator) {
            List<String> list = new ArrayList<String>();
            list.add(raw);
            return list;
        }
        String[] result = raw.split(separator);
        return new ArrayList<String>(Arrays.asList(result));
    }
    
   
    public static @Nullable <V> V getItem(List<V> list, int position) {
        if (isEmpty(list)) {
            return null;
        }
        if (position < 0 || position >= list.size()) {
            return null;
        }
        return list.get(position);
    }
    
   
    public static <V> V getItem(V[] source, int position) {
        if (isEmpty(source)) {
            return null;
        }
        if (position < 0 || position >= source.length) {
            return null;
        }
        return source[position];
    }
    
}
