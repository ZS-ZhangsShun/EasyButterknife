package com.zs.easybutterknife;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import com.zs.annotation.BindView;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.text)
    TextView textView;
    @BindView(R.id.button)
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        textView.setText("111");
    }
}