package com.orzangleli.anivia;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/8/8 下午2:57
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class Test {
    public static void main(String[] args) {
        add(1, 2);
        multi(1, 3);
    }
    
    private final static int add(int a, int b) {
        return a + b;
    }
    
    private static int multi(int a, int b) {
        if (a == 0 ) {
            return -1;
        }
        return a * b;
    }
}
