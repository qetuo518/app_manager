package dclnb.appmanager;

import android.app.Activity;
import android.content.Intent;
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

/**
 * 工作模式选择 + 权限申请
 * 
 * 逻辑参考 xtsafe-main 的 ModeSelectionActivity 和 ShizukuActivity：
 * - Shizuku 模式：pingBinder() + checkSelfPermission()
 * - Root 模式：exec("su -c id") 检查 uid=0
 * - 有权限后跳转到 ManagerActivity
 */
public class ModeSelectionActivity extends Activity {

    private static final int SHIZUKU_REQUEST_CODE = 10086;

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

        // 副标题
        TextView subtitle = new TextView(this);
        subtitle.setText("选择工作模式以继续");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.argb(150, 90, 90, 90));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 32);
        root.addView(subtitle);

        // ===== Shizuku 模式 =====
        addModeCard(root, "Shizuku 模式",
                "通过 Shizuku 框架获取 uid=2000 权限\n适合已设置好 Shizuku 的设备",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestShizukuPermission();
                    }
                });

        // ===== Root 模式 =====
        addModeCard(root, "Root 模式",
                "通过 su 命令获取 root 权限\n适合已获取 root 权限的设备",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkRootPermission();
                    }
                });

        // 提示信息
        TextView tip = new TextView(this);
        tip.setText("请选择一种工作模式，系统将自动检测并申请对应权限");
        tip.setTextColor(Color.argb(120, 90, 90, 90));
        tip.setTextSize(11);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(0, 24, 0, 0);
        root.addView(tip);

        scrollView.addView(root);
        setContentView(scrollView);

        // 适配预测性返回手势（Android 13+）
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

    /**
     * 添加模式卡片
     */
    private void addModeCard(LinearLayout root, String title, String desc, View.OnClickListener onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        card.setGravity(Gravity.START);
        card.setBackgroundColor(Color.argb(255, 250, 250, 250));
        card.setClickable(true);
        card.setOnClickListener(onClick);

        // 标题
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(15);
        titleView.setTextColor(Color.argb(255, 30, 30, 30));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(titleView);

        // 描述
        TextView descView = new TextView(this);
        descView.setText(desc);
        descView.setTextSize(11);
        descView.setTextColor(Color.argb(150, 90, 90, 90));
        descView.setPadding(0, 6, 0, 0);
        card.addView(descView);

        root.addView(card);
    }

    /**
     * 申请 Shizuku 权限
     * 参考 xtsafe-main MainActivity.java 中的 Shizuku 权限申请逻辑
     */
    private void requestShizukuPermission() {
        Toast.makeText(this, "正在检查 Shizuku...", Toast.LENGTH_SHORT).show();

        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku 未运行，请先安装并启动 Shizuku", Toast.LENGTH_LONG).show();
            return;
        }

        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizuku 已授权", Toast.LENGTH_SHORT).show();
            navigateToManager();
            return;
        }

        // 请求权限（带回调）
        Shizuku.addRequestPermissionResultListener(new Shizuku.OnRequestPermissionResultListener() {
            @Override
            public void onRequestPermissionResult(int requestCode, int grantResult) {
                if (requestCode == SHIZUKU_REQUEST_CODE) {
                    Shizuku.removeRequestPermissionResultListener(this);
                    if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(ModeSelectionActivity.this, "Shizuku 授权成功", Toast.LENGTH_SHORT).show();
                        navigateToManager();
                    } else {
                        Toast.makeText(ModeSelectionActivity.this, "Shizuku 授权被拒绝", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
    }

    /**
     * 检查 Root 权限
     * 参考 xtsafe-main MainActivity.java 中的 isRootAvailable() 方法
     */
    private void checkRootPermission() {
        Toast.makeText(this, "正在检查 Root...", Toast.LENGTH_SHORT).show();

        if (isRootAvailable()) {
            Toast.makeText(this, "Root 权限已就绪", Toast.LENGTH_SHORT).show();
            navigateToManager();
        } else {
            Toast.makeText(this, "未检测到 Root 权限，请确保设备已获取 root", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 判断是否有 Root 权限
     * 参考 xtsafe-main MainActivity.java:422-434
     */
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

    /**
     * 跳转到 ManagerActivity
     */
    private void navigateToManager() {
        Intent intent = new Intent(this, ManagerActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理 Shizuku 监听器
        Shizuku.removeRequestPermissionResultListener(null);
    }
}