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

public class FreezeActivity extends Activity {

    private LinearLayout listContainer;
    private TextView emptyView;
    private ProgressBar loadingBar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PackageManager pm;
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();

        // 根布局
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // 标题栏
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(16, 16, 16, 16);

        Button backBtn = new Button(this);
        backBtn.setText("←");
        backBtn.setTextSize(16);
        header.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("冻结管理");
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

        // 提示
        TextView tip = new TextView(this);
        tip.setText("点击应用即可冻结 / 解冻");
        tip.setTextSize(12);
        tip.setTextColor(Color.argb(150, 90, 90, 90));
        tip.setPadding(0, 0, 0, 8);
        root.addView(tip);

        // 列表容器（带 weight 占据剩余空间）
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
        
        // 加载进度（居中覆盖在列表上）
        loadingBar = new ProgressBar(this);
        loadingBar.setIndeterminate(true);
        FrameLayout.LayoutParams loadingLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        loadingLp.gravity = Gravity.CENTER;
        loadingBar.setLayoutParams(loadingLp);
        loadingBar.setVisibility(View.VISIBLE);
        listWrapper.addView(loadingBar);

        // 空视图（居中覆盖在列表上）
        emptyView = new TextView(this);
        emptyView.setText("未找到可管理的应用");
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
            Set<String> frozenSet = getDisabledPackages();

            for (ApplicationInfo info : infos) {
                if (info.packageName.equals(selfPkg)) continue;
                boolean frozen = frozenSet.contains(info.packageName);
                String label;
                try {
                    label = pm.getApplicationLabel(info).toString();
                } catch (Exception e) {
                    label = info.packageName;
                }
                result.add(new AppItem(info.packageName, label, info.loadIcon(pm), frozen));
            }

            Collections.sort(result, new Comparator<AppItem>() {
                @Override
                public int compare(AppItem o1, AppItem o2) {
                    if (o1.frozen != o2.frozen) return o1.frozen ? -1 : 1;
                    return o1.label.compareToIgnoreCase(o2.label);
                }
            });
        } catch (Exception e) {
            // ignore
        }
        return result;
    }

    private Set<String> getDisabledPackages() {
        Set<String> set = new HashSet<>();
        String out = execShizukuWithOutput(new String[]{"sh", "-c", "pm list packages -d 2>/dev/null | sed 's/^package://'"});
        if (out == null || out.trim().isEmpty()) {
            out = execRootWithOutput("pm list packages -d 2>/dev/null | sed 's/^package://'");
        }
        if (out != null) {
            for (String line : out.split("\\r?\\n")) {
                String p = line.trim();
                if (!p.isEmpty()) set.add(p);
            }
        }
        return set;
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

        final TextView statusView = new TextView(this);
        statusView.setTextSize(12);
        info.addView(statusView);
        row.addView(info);

        final Button actionBtn = new Button(this);
        actionBtn.setTextSize(13);
        actionBtn.setAllCaps(false);
        actionBtn.setPadding(16, 8, 16, 8);
        actionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFreeze(app, actionBtn, statusView);
            }
        });
        row.addView(actionBtn);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFreeze(app, actionBtn, statusView);
            }
        });

        updateRowState(row, app, actionBtn, statusView, app.frozen);
        return row;
    }

    private void updateRowState(LinearLayout row, AppItem app,
                                Button actionBtn, TextView statusView, boolean frozen) {
        app.frozen = frozen;
        actionBtn.setText(frozen ? "解冻" : "冻结");
        actionBtn.setEnabled(true);
        statusView.setText(frozen ? "已冻结" : "运行中");
        statusView.setTextColor(frozen ? Color.argb(255, 200, 80, 80)
                : Color.argb(255, 80, 160, 80));
        row.setAlpha(frozen ? 0.6f : 1f);
    }

    private void toggleFreeze(final AppItem app, final Button actionBtn, final TextView statusView) {
        if (busy) return;
        if (!app.frozen) {
            // 冻结需要确认弹窗
            new AlertDialog.Builder(this)
                    .setTitle("确认冻结")
                    .setMessage("确定要冻结 " + app.label + " 吗？冻结后将无法运行。")
                    .setPositiveButton("冻结", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            doToggleFreeze(app, actionBtn, statusView);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            // 解冻无需确认
            doToggleFreeze(app, actionBtn, statusView);
        }
    }

    private void doToggleFreeze(final AppItem app, final Button actionBtn, final TextView statusView) {
        if (busy) return;
        busy = true;
        actionBtn.setEnabled(false);

        final boolean wasFrozen = app.frozen;
        final boolean targetFrozen = !wasFrozen;
        final String pkg = app.packageName;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final boolean ok = targetFrozen ? freezeApp(pkg) : unfreezeApp(pkg);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        busy = false;
                        LinearLayout row = (LinearLayout) actionBtn.getParent();
                        updateRowState(row, app, actionBtn, statusView, ok ? targetFrozen : wasFrozen);
                        if (ok) {
                            Toast.makeText(FreezeActivity.this,
                                    (ok ? "已冻结：" : "已解冻：") + app.label,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            app.frozen = wasFrozen;
                            updateRowState(row, app, actionBtn, statusView, wasFrozen);
                            Toast.makeText(FreezeActivity.this,
                                    "操作失败，需要 Shizuku 或 Root 权限",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private boolean freezeApp(String pkg) {
        execShizuku(new String[]{"am", "force-stop", pkg});
        execRoot("am force-stop " + pkg);
        if (looksDisabled(execShizukuWithOutput(new String[]{"sh", "-c", "pm disable-user --user 0 " + pkg}))) return true;
        if (looksDisabled(execShizukuWithOutput(new String[]{"sh", "-c", "pm disable --user 0 " + pkg}))) return true;
        if (looksDisabled(execRootWithOutput("pm disable-user --user 0 " + pkg))) return true;
        if (looksDisabled(execRootWithOutput("pm disable --user 0 " + pkg))) return true;
        return false;
    }

    private boolean unfreezeApp(String pkg) {
        execShizuku(new String[]{"am", "force-stop", pkg});
        execRoot("am force-stop " + pkg);
        if (looksEnabled(execShizukuWithOutput(new String[]{"sh", "-c", "pm enable --user 0 " + pkg}))) return true;
        if (looksEnabled(execShizukuWithOutput(new String[]{"sh", "-c", "pm enable " + pkg}))) return true;
        if (looksEnabled(execRootWithOutput("pm enable --user 0 " + pkg))) return true;
        if (looksEnabled(execRootWithOutput("pm enable " + pkg))) return true;
        return false;
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

    private boolean looksDisabled(String out) {
        if (out != null) {
            String t = out.toLowerCase();
            if (t.contains("new state: disabled")) return true;
            if (t.contains("error") || t.contains("exception") || t.contains("denial")) return false;
        }
        return false;
    }

    private boolean looksEnabled(String out) {
        if (out != null) {
            String t = out.toLowerCase();
            if (t.contains("new state: enabled")) return true;
            if (t.contains("error") || t.contains("exception") || t.contains("denial")) return false;
        }
        return false;
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
        boolean frozen;

        AppItem(String packageName, String label, Drawable icon, boolean frozen) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.frozen = frozen;
        }
    }
}
