package dclnb.appmanager;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 设置界面
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String GITHUB_URL = "https://github.com/qetuo518/app_manager";

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
        title.setText("设置");
        title.setTextSize(24);
        title.setTextColor(Color.argb(255, 30, 30, 30));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);
        root.addView(title);

        // 关于区块
        addSectionHeader(root, "关于");

        addSettingItem(root, "应用名称", "DCL 软件管理");
        addSettingItem(root, "版本", "v1.2");
        addSettingItem(root, "开源协议", "MIT License");

        // 分隔线
        addDivider(root);

        // 开源区块
        addSectionHeader(root, "开源");

        Button githubBtn = addFeatureButton(root, "查看 GitHub 仓库", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGitHub();
            }
        });

        // 底部提示
        TextView footer = new TextView(this);
        footer.setText("DCL 软件管理 - 保护你的设备安全");
        footer.setTextSize(11);
        footer.setTextColor(Color.argb(120, 90, 90, 90));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 24, 0, 0);
        root.addView(footer);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void openGitHub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addSectionHeader(LinearLayout root, String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(13);
        header.setTextColor(Color.argb(150, 90, 90, 90));
        header.setGravity(Gravity.START);
        header.setPadding(0, 16, 0, 8);
        root.addView(header);
    }

    private void addSettingItem(LinearLayout root, String key, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 8, 0, 8);

        TextView keyView = new TextView(this);
        keyView.setText(key);
        keyView.setTextSize(14);
        keyView.setTextColor(Color.argb(255, 60, 60, 60));
        LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        keyView.setLayoutParams(keyParams);
        row.addView(keyView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(Color.argb(150, 90, 90, 90));
        valueView.setGravity(Gravity.END);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        valueView.setLayoutParams(valueParams);
        row.addView(valueView);

        root.addView(row);
    }

    private Button addFeatureButton(LinearLayout root, String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setPadding(0, 12, 0, 12);
        btn.setOnClickListener(listener);
        root.addView(btn);
        return btn;
    }

    private void addDivider(LinearLayout root) {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.topMargin = 8;
        params.bottomMargin = 8;
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.argb(30, 0, 0, 0));
        root.addView(divider);
    }
}
