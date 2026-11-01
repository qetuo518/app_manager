package dclnb.appmanager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 主管理界面（权限通过后进入）
 * UI 模仿终末神明：简洁设计，无多余标签
 */
public class ManagerActivity extends AppCompatActivity {

    private boolean shizukuReady = false;
    private boolean rootReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);

        // 标题
        TextView title = new TextView(this);
        title.setText("DCL 软件管理");
        title.setTextSize(24);
        title.setTextColor(Color.argb(255, 30, 30, 30));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        root.addView(title);

        // 状态说明
        TextView statusView = new TextView(this);
        String shizukuStatus = checkShizukuStatus();
        String rootStatus = checkRootStatus();
        statusView.setText("Shizuku: " + shizukuStatus + "\nRoot: " + rootStatus);
        statusView.setTextSize(12);
        statusView.setTextColor(Color.argb(150, 90, 90, 90));
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 0, 0, 24);
        root.addView(statusView);

        // 功能按钮区
        TextView sectionLabel = new TextView(this);
        sectionLabel.setText("功能入口");
        sectionLabel.setTextSize(13);
        sectionLabel.setTextColor(Color.argb(150, 90, 90, 90));
        sectionLabel.setGravity(Gravity.CENTER);
        sectionLabel.setPadding(0, 0, 0, 12);
        root.addView(sectionLabel);

        addFeatureButton(root, "强卸载", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ManagerActivity.this, UninstallActivity.class));
            }
        });

        addFeatureButton(root, "冻结管理", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ManagerActivity.this, FreezeActivity.class));
            }
        });

        addFeatureButton(root, "Shell 执行器", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ManagerActivity.this, ShellExecutorActivity.class));
            }
        });

        addFeatureButton(root, "设置", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ManagerActivity.this, "设置（待实现）", Toast.LENGTH_SHORT).show();
            }
        });

        // 底部提示
        TextView footer = new TextView(this);
        footer.setText("DCL 软件管理 v1.0");
        footer.setTextSize(11);
        footer.setTextColor(Color.argb(120, 90, 90, 90));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 24, 0, 0);
        root.addView(footer);

        scrollView.addView(root);
        setContentView(scrollView);

        // 适配预测性返回手势
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                getWindow().getDecorView().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                                    new android.window.OnBackInvokedCallback() {
                                        @Override
                                        public void onBackInvoked() {
                                            finish();
                                        }
                                    });
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
        }
    }

    private String checkShizukuStatus() {
        if (!Shizuku.pingBinder()) {
            return "未运行";
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return "已授权";
        }
        return "未授权";
    }

    private String checkRootStatus() {
        return isRootAvailable() ? "已授权" : "未授权";
    }

    private boolean isRootAvailable() {
        try {
            Process p = Runtime.getRuntime().exec("su -c id");
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            br.close();
            p.destroy();
            return line != null && line.contains("uid=0");
        } catch (Exception e) {
            return false;
        }
    }

    private void addFeatureButton(LinearLayout root, String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setPadding(0, 12, 0, 12);
        btn.setOnClickListener(listener);
        root.addView(btn);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 状态通过 onCreate 已展示，无需刷新
    }
}