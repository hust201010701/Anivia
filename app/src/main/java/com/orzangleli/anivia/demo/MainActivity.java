package com.orzangleli.anivia.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.orzangleli.anivia.Patchable;

public class MainActivity extends AppCompatActivity {
    
    Patchable patchable;
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        patchable = null;
    }
}
