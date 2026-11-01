package dclnb.appmanager;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 主入口 Activity
 * 启动后立即跳转到 ModeSelectionActivity
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 直接进入工作模式选择
        Intent intent = new Intent(this, ModeSelectionActivity.class);
        startActivity(intent);
        finish();
    }
}