package com.orzangleli.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Test test = new Test();
        this.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Toast.makeText(MainActivity.this, test.test("hahaha"), Toast.LENGTH_LONG).show();
            }
        });
    
        this.findViewById(R.id.load_patch).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 0);
                    return ;
                }
                patch();
            }
        });
        
        
    }
    
    private void patch() {
        boolean nothingHasBeenPatched = true;
        try {
            DexClassLoader classLoader = new DexClassLoader("/sdcard/patched.dex",this.getCacheDir().getAbsolutePath(),
                null, MainActivity.class.getClassLoader());
        
            Class patchConfigInfoClass = classLoader.loadClass("com.orzangleli.anivia.patch.PatchConfig");
            Method getAllPatchableMap = patchConfigInfoClass.getDeclaredMethod("getAllPatchableMap");
            if (getAllPatchableMap != null) {
                getAllPatchableMap.setAccessible(true);
                Map<String, String> invoke = (Map<String, String>) getAllPatchableMap.invoke(null);
                Set<Map.Entry<String, String>> entries = invoke.entrySet();
                Iterator<Map.Entry<String, String>> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> next = iterator.next();
                    String originalClassStr = next.getKey();
                    String patchableClassStr = next.getValue();
                    
                    Class originalClass = classLoader.loadClass(originalClassStr);
                    Class patchableClass = classLoader.loadClass(patchableClassStr);
                    Field patchable = originalClass.getField("patchable");
                    Object patchableInstance = patchableClass.newInstance();
                    patchable.set(null, patchableInstance);
                    nothingHasBeenPatched = false;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        if (!nothingHasBeenPatched) {
            Toast.makeText(this, "补丁包加载成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "补丁包加载失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            patch();
        }
    }
    
    public void copy(String srcPath,String dstPath) throws IOException {
        File src=new File(srcPath);
        if(!src.exists()){
            throw new RuntimeException("source patch does not exist ");
        }
        File dst=new File(dstPath);
        if(!dst.getParentFile().exists()){
            dst.getParentFile().mkdirs();
        }
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
