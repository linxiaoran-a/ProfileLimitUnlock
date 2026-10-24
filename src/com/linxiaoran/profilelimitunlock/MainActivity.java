package com.linxiaoran.profilelimitunlock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    private static class Row {
        final String key;
        final String title;
        final String desc;
        final int def;
        EditText input;
        SeekBar seek;

        Row(String key, String title, String desc, int def) {
            this.key = key;
            this.title = title;
            this.desc = desc;
            this.def = def;
        }
    }

    private final Row[] rows = new Row[]{
            new Row(HookEntry.KEY_PROFILE_LIMIT, "分身数量上限",
                    "每个用户可创建的应用分身（工作资料）数量。\n报错 \"Cannot add more profiles of type MANAGED\" 就是这个。", HookEntry.DEFAULT_PROFILE),
            new Row(HookEntry.KEY_MAX_USERS, "用户总数上限",
                    "config_multiuserMaximumUsers：系统中最多可存在的用户/分身总数。", HookEntry.DEFAULT_MAX_USERS),
            new Row(HookEntry.KEY_MAX_RUNNING, "同时运行上限",
                    "config_multiuserMaxRunningUsers：可同时处于运行状态的用户数。", HookEntry.DEFAULT_MAX_RUNNING),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MODE_WORLD_READABLE 在 targetSdk>=24 直接抛 SecurityException（打开即闪退的根因）。
        // 改为 MODE_PRIVATE + 由 App 自己对偏好文件 chmod 0644，供 XSharedPreferences 读取。
        prefs = getSharedPreferences(HookEntry.PREFS, Context.MODE_PRIVATE);
        makeWorldReadable();

        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("分身上限设置");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView tip = new TextView(this);
        tip.setText("范围 " + HookEntry.MIN_VALUE + " ~ " + HookEntry.MAX_VALUE + "，修改立即生效，无需重启。\n设为 1（部分项）即恢复系统默认限制。");
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tip.setTextColor(Color.GRAY);
        tip.setPadding(0, dp(12), 0, dp(12));
        root.addView(tip, new LinearLayout.LayoutParams(-1, -2));

        for (Row row : rows) {
            addRow(root, row);
        }

        Button btnReset = new Button(this);
        btnReset.setText("全部重置为默认 (99)");
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, -2);
        btnLp.setMargins(0, dp(16), 0, 0);
        root.addView(btnReset, btnLp);
        btnReset.setOnClickListener(v -> {
            for (Row row : rows) {
                setValue(row, row.def);
            }
        });

        TextView status = new TextView(this);
        status.setPadding(0, dp(20), 0, 0);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        status.setTextColor(Color.GRAY);
        status.setGravity(Gravity.CENTER);
        status.setText("模块包名：" + HookEntry.PKG + "\n若未在 LSPosed 中启用并勾选「系统框架」，以上设置不会生效");
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        setTitle("ProfileLimitUnlock");
    }

    private void addRow(LinearLayout parent, Row row) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(-1, -2);
        boxLp.setMargins(0, dp(12), 0, 0);
        parent.addView(box, boxLp);

        TextView label = new TextView(this);
        label.setText(row.title);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        box.addView(label, new LinearLayout.LayoutParams(-1, -2));

        TextView desc = new TextView(this);
        desc.setText(row.desc);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        desc.setTextColor(Color.GRAY);
        desc.setPadding(0, dp(4), 0, dp(4));
        box.addView(desc, new LinearLayout.LayoutParams(-1, -2));

        row.input = new EditText(this);
        row.input.setInputType(InputType.TYPE_CLASS_NUMBER);
        row.input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        row.input.setGravity(Gravity.CENTER);
        row.input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        box.addView(row.input, new LinearLayout.LayoutParams(-1, -2));

        row.seek = new SeekBar(this);
        row.seek.setMax(HookEntry.MAX_VALUE - HookEntry.MIN_VALUE);
        box.addView(row.seek, new LinearLayout.LayoutParams(-1, -2));

        int saved = prefs.getInt(row.key, row.def);
        setValue(row, saved);

        row.input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int v = Integer.parseInt(s.toString());
                    if (v >= HookEntry.MIN_VALUE && v <= HookEntry.MAX_VALUE) {
                        row.seek.setProgress(v - HookEntry.MIN_VALUE);
                        save(row, v);
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        row.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    setValue(row, progress + HookEntry.MIN_VALUE);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void setValue(Row row, int v) {
        if (!row.input.getText().toString().equals(String.valueOf(v))) {
            row.input.setText(String.valueOf(v));
            row.input.setSelection(row.input.getText().length());
        }
        row.seek.setProgress(v - HookEntry.MIN_VALUE);
        save(row, v);
    }

    private void save(Row row, int v) {
        prefs.edit().putInt(row.key, v).apply();
        makeWorldReadable();
        Toast.makeText(this, row.title + ": " + v + "（已生效）", Toast.LENGTH_SHORT).show();
    }

    private void makeWorldReadable() {
        try {
            java.io.File f = new java.io.File(getDataDir(), "shared_prefs/" + HookEntry.PREFS + ".xml");
            if (f.exists()) android.system.Os.chmod(f.getAbsolutePath(), 0644);
        } catch (Throwable ignored) {
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}
