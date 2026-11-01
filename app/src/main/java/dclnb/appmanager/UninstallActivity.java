package dclnb.appmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashSet;

import rikka.shizuku.Shizuku;

public class UninstallActivity extends Activity {

    private LinearLayout listContainer;
    private TextView emptyView;
    private ProgressBar loadingBar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PackageManager pm;
    // 使用包装类解决 final 限制
    private final boolean[] busy = {false};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(16, 16, 16, 16);

        Button backBtn = new Button(this);
        backBtn.setText("←");
        backBtn.setTextSize(16);
        header.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("强卸载");
        title.setTextSize(18);
        title.setTextColor(Color.argb(255, 30, 30, 30));
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.leftMargin = 8;
        titleLp.rightMargin = 8;
        header.addView(title, titleLp);
        root.addView(header);

        TextView tip = new TextView(this);
        tip.setText("点击应用即可卸载");
        tip.setTextSize(12);
        tip.setTextColor(Color.argb(150, 90, 90, 90));
        tip.setPadding(0, 0, 0, 8);
        root.addView(tip);

        FrameLayout listWrapper = new FrameLayout(this);
        listWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(8, 0, 8, 0);
        scrollView.addView(listContainer);
        listWrapper.addView(scrollView);

        loadingBar = new ProgressBar(this);
        loadingBar.setIndeterminate(true);
        FrameLayout.LayoutParams loadingLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        loadingLp.gravity = Gravity.CENTER;
        loadingBar.setLayoutParams(loadingLp);
        loadingBar.setVisibility(View.VISIBLE);
        listWrapper.addView(loadingBar);

        emptyView = new TextView(this);
        emptyView.setText("未找到可卸载的应用");
        emptyView.setTextSize(14);
        emptyView.setTextColor(Color.argb(150, 90, 90, 90));
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(20, 20, 20, 20);
        emptyView.setVisibility(View.GONE);
        FrameLayout.LayoutParams emptyLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        emptyView.setLayoutParams(emptyLp);
        listWrapper.addView(emptyView);

        root.addView(listWrapper);
        setContentView(root);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadApps();
    }

    private void loadApps() {
        final ProgressBar loadingBar = this.loadingBar;
        final TextView emptyView = this.emptyView;
        final LinearLayout listContainer = this.listContainer;

        loadingBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        listContainer.removeAllViews();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<AppItem> loaded = fetchApps();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            loadingBar.setVisibility(View.GONE);
                            if (loaded == null || loaded.isEmpty()) {
                                emptyView.setVisibility(View.VISIBLE);
                            } else {
                                for (AppItem app : loaded) {
                                    listContainer.addView(buildRow(app));
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    final String errorMsg = e.getMessage();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            loadingBar.setVisibility(View.GONE);
                            emptyView.setText("加载失败: " + errorMsg);
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
    }

    private List<AppItem> fetchApps() {
        List<AppItem> result = new ArrayList<>();
        try {
            List<ApplicationInfo> infos = pm.getInstalledApplications(0);
            String selfPkg = getPackageName();

            for (ApplicationInfo info : infos) {
                if (info.packageName.equals(selfPkg)) continue;
                // 过滤系统应用
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                String label;
                try {
                    label = pm.getApplicationLabel(info).toString();
                } catch (Exception e) {
                    label = info.packageName;
                }
                result.add(new AppItem(info.packageName, label, info.loadIcon(pm)));
            }

            Collections.sort(result, new Comparator<AppItem>() {
                @Override
                public int compare(AppItem o1, AppItem o2) {
                    return o1.label.compareToIgnoreCase(o2.label);
                }
            });
        } catch (Exception e) {
            // ignore
        }
        return result;
    }

    private View buildRow(final AppItem app) {
        if (app.label == null || app.packageName == null) return new View(this);
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16, 12, 16, 12);
        row.setBackgroundColor(Color.argb(255, 250, 250, 250));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = 10;
        row.setLayoutParams(rowLp);
        row.setClickable(true);
        row.setFocusable(true);

        ImageView iconView = new ImageView(this);
        iconView.setImageDrawable(app.icon);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(iconView, new LinearLayout.LayoutParams(44, 44));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(12, 0, 8, 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameView = new TextView(this);
        nameView.setText(app.label);
        nameView.setTextSize(15);
        nameView.setTextColor(Color.argb(255, 30, 30, 30));
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(nameView);

        TextView pkgView = new TextView(this);
        pkgView.setText(app.packageName);
        pkgView.setTextSize(11);
        pkgView.setTextColor(Color.argb(180, 130, 130, 130));
        pkgView.setSingleLine(true);
        pkgView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        info.addView(pkgView);
        row.addView(info);

        final Button actionBtn = new Button(this);
        actionBtn.setTextSize(13);
        actionBtn.setAllCaps(false);
        actionBtn.setText("卸载");
        actionBtn.setPadding(16, 8, 16, 8);
        actionBtn.setBackgroundColor(Color.argb(255, 220, 60, 60));
        actionBtn.setTextColor(Color.WHITE);
        actionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog(app, actionBtn);
            }
        });
        row.addView(actionBtn);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog(app, actionBtn);
            }
        });

        return row;
    }

    private void showConfirmDialog(final AppItem app, final Button actionBtn) {
        new AlertDialog.Builder(this)
                .setTitle("确认卸载")
                .setMessage("确定要强制卸载 " + app.label + " 吗？\n\n此操作将关闭其无障碍服务、停止进程并卸载。")
                .setPositiveButton("卸载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        doUninstall(app, actionBtn);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void doUninstall(final AppItem app, final Button actionBtn) {
        if (busy[0]) return;
        busy[0] = true;
        actionBtn.setEnabled(false);

        final LinearLayout finalListContainer = listContainer;
        final TextView finalEmptyView = emptyView;
        final Activity finalActivity = this;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. 尝试关闭目标应用的无障碍服务
                    disableAccessibility(app.packageName);
                    
                    // 2. 关闭目标应用的设备管理员权限
                    disableDeviceAdmin(app.packageName);
                    
                    // 3. 停止进程
                    execShizuku(new String[]{"am", "force-stop", app.packageName});
                    execRoot("am force-stop " + app.packageName);
                    
                    // 4. 静默卸载
                    final boolean ok = silentUninstall(app.packageName);
                    
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            busy[0] = false;
                            actionBtn.setEnabled(true);
                            if (ok) {
                                Toast.makeText(finalActivity,
                                        "已成功卸载：" + app.label,
                                        Toast.LENGTH_SHORT).show();
                                // 从列表中移除
                                LinearLayout parent = (LinearLayout) actionBtn.getParent();
                                if (parent != null) {
                                    finalListContainer.removeView(parent);
                                    if (finalListContainer.getChildCount() == 0) {
                                        finalEmptyView.setVisibility(View.VISIBLE);
                                    }
                                }
                            } else {
                                Toast.makeText(finalActivity,
                                        "卸载失败，需要 Shizuku 或 Root 权限",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    final String errorMsg = e.getMessage();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            busy[0] = false;
                            actionBtn.setEnabled(true);
                            Toast.makeText(finalActivity,
                                    "操作失败: " + errorMsg,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    /**
     * 尝试禁用指定包名的设备管理员权限
     */
    private void disableDeviceAdmin(String pkg) {
        try {
            String cmd = "dpm remove-active-admin " + pkg;
            execShizuku(new String[]{"sh", "-c", cmd});
            execRoot(cmd);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 尝试禁用指定包名的无障碍服务
     */
    private void disableAccessibility(String pkg) {
        try {
            // 方法1：通过系统设置
            String settingsValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingsValue != null && settingsValue.contains(pkg)) {
                String newValue = settingsValue.replace(pkg + ":", "").replace(":" + pkg, "");
                Settings.Secure.putString(getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newValue);
            }
            // 方法2：通过Shizuku/Root命令
            String cmd = "cmd accessibility disable " + pkg;
            execShizuku(new String[]{"sh", "-c", cmd});
            execRoot("cmd accessibility disable " + pkg);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 静默卸载应用
     */
    private boolean silentUninstall(String pkg) {
        // 尝试通过Shizuku卸载
        String out = execShizukuWithOutput(new String[]{"sh", "-c", "pm uninstall --user 0 " + pkg});
        if (out != null && !out.toLowerCase().contains("error")) {
            return true;
        }
        // 尝试通过Root卸载
        out = execRootWithOutput("pm uninstall --user 0 " + pkg);
        if (out != null && !out.toLowerCase().contains("error")) {
            return true;
        }
        return false;
    }

    private String execShizukuWithOutput(String[] cmd) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                rikka.shizuku.ShizukuRemoteProcess proc = Shizuku.newProcess(cmd, null, null);
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                proc.waitFor();
                return sb.toString();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String execRootWithOutput(String cmd) {
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            proc.waitFor();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (proc != null) {
                try { proc.destroy(); } catch (Exception ignored) {}
            }
        }
    }

    private void execShizuku(String[] cmd) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Shizuku.newProcess(cmd, null, null);
            }
        } catch (Exception ignored) {}
    }

    private void execRoot(String cmd) {
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            proc.waitFor();
        } catch (Exception ignored) {}
        finally {
            if (proc != null) proc.destroy();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private static class AppItem {
        final String packageName;
        final String label;
        final Drawable icon;

        AppItem(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }
}
