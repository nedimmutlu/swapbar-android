package com.swapbar.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String PREFS = "swapbar_accounts_v1";
    private static final String THEME_KEY = "theme_mode";
    private static final int THEME_SYSTEM = 0;
    private static final int THEME_LIGHT = 1;
    private static final int THEME_DARK = 2;
    private static final int WEB_SESSION_SLOTS = 8;
    private final ArrayList<Account> accounts = new ArrayList<>();
    private LinearLayout list;
    private int themeMode;
    private boolean darkTheme;
    private int ink, muted, purple, paper, surface, border, soft, tint;

    private static final class Account {
        String id;
        String service;
        String label;
        int slot;
        Account(String id, String service, String label, int slot) {
            this.id = id;
            this.service = service;
            this.label = label;
            this.slot = slot;
        }
    }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        themeMode = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(THEME_KEY, THEME_SYSTEM);
        applyAppearance();
        load();
        render();
    }

    private void applyAppearance() {
        boolean systemDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        darkTheme = themeMode == THEME_DARK || (themeMode == THEME_SYSTEM && systemDark);
        setTheme(darkTheme ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        ink = darkTheme ? Color.rgb(240, 238, 247) : Color.rgb(38, 37, 45);
        muted = darkTheme ? Color.rgb(169, 167, 181) : Color.rgb(112, 111, 122);
        purple = Color.rgb(125, 119, 255);
        paper = darkTheme ? Color.rgb(23, 23, 29) : Color.rgb(246, 245, 242);
        surface = darkTheme ? Color.rgb(34, 34, 42) : Color.WHITE;
        border = darkTheme ? Color.rgb(56, 56, 67) : Color.rgb(233, 231, 226);
        soft = darkTheme ? Color.rgb(48, 48, 58) : Color.rgb(243, 242, 239);
        tint = darkTheme ? Color.rgb(48, 45, 75) : Color.rgb(237, 235, 255);
        getWindow().setStatusBarColor(paper);
        getWindow().setNavigationBarColor(paper);
        getWindow().getDecorView().setSystemUiVisibility(darkTheme ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    @Override protected void onResume() {
        super.onResume();
        if (themeMode == THEME_SYSTEM) {
            boolean systemDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
            if (darkTheme != systemDark) {
                applyAppearance();
                render();
            }
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (strokeColor != 0) d.setStroke(dp(1), strokeColor);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = column();
        page.setPadding(dp(22), dp(25), dp(22), dp(28));
        page.setBackgroundColor(paper);
        scroll.addView(page);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("S", 20, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(shape(purple, 14, 0));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout brandWords = column();
        brandWords.setPadding(dp(12), 0, 0, 0);
        brandWords.addView(text("SWAPBAR", 12, muted, true));
        brandWords.addView(text("Hesapların tek yerde", 15, ink, true));
        brand.addView(brandWords, new LinearLayout.LayoutParams(0, -2, 1));
        Button themeButton = button(themeLabel(), false);
        brand.addView(themeButton, new LinearLayout.LayoutParams(-2, dp(42)));
        themeButton.setOnClickListener(v -> chooseTheme());
        page.addView(brand);

        TextView title = text("Hesap seç", 30, ink, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(34);
        page.addView(title, titleLp);
        TextView subtitle = text("Claude ve ChatGPT hesaplarını ayrı oturumlarda aç.", 15, muted, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(5);
        page.addView(subtitle, subLp);

        Button add = button("＋   Hesap ekle", true);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(-1, dp(52));
        addLp.topMargin = dp(22);
        page.addView(add, addLp);
        add.setOnClickListener(v -> addDialog());

        TextView section = text("KAYITLI HESAPLAR", 11, muted, true);
        LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(-1, -2);
        secLp.topMargin = dp(29);
        secLp.bottomMargin = dp(11);
        page.addView(section, secLp);

        list = column();
        page.addView(list);
        if (accounts.isEmpty()) {
            LinearLayout empty = column();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(27), dp(20), dp(27));
            empty.setBackground(shape(surface, 18, border));
            empty.addView(text("Henüz hesap eklenmedi", 16, ink, true));
            TextView helper = text("Bir etiket ekle; Swapbar servisin uygulamasını açsın.", 13, muted, false);
            helper.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams helperLp = new LinearLayout.LayoutParams(-1, -2);
            helperLp.topMargin = dp(7);
            empty.addView(helper, helperLp);
            list.addView(empty);
        } else {
            for (Account account : accounts) list.addView(accountCard(account));
        }

        LinearLayout note = column();
        note.setPadding(dp(15), dp(13), dp(15), dp(13));
        note.setBackground(shape(tint, 15, 0));
        TextView noteTitle = text("Prototip notu", 13, purple, true);
        TextView noteBody = text("Her hesap ayri WebView oturumunda saklanir. Sosyal giris saglayicilari desteklenmez.", 12, ink, false);
        noteBody.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams noteTextLp = new LinearLayout.LayoutParams(-1, -2);
        noteTextLp.topMargin = dp(4);
        note.addView(noteTitle);
        note.addView(noteBody, noteTextLp);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.topMargin = dp(20);
        page.addView(note, noteLp);
        setContentView(scroll);
    }

    private LinearLayout accountCard(Account account) {
        LinearLayout card = column();
        card.setPadding(dp(15), dp(14), dp(15), dp(13));
        card.setBackground(shape(surface, 17, border));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = text(account.service.equals("Claude") ? "C" : "GPT", 12, purple, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(shape(tint, 11, 0));
        top.addView(badge, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout names = column();
        names.setPadding(dp(11), 0, 0, 0);
        names.addView(text(account.label, 16, ink, true));
        names.addView(text("Ayrı WebView oturumu", 12, muted, false));
        top.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView more = text("•••", 18, muted, true);
        more.setGravity(Gravity.CENTER);
        top.addView(more, new LinearLayout.LayoutParams(dp(38), dp(38)));
        more.setOnClickListener(v -> accountMenu(account));
        card.addView(top);

        Button open = button(account.service + " WebView'i aç  →", false);
        LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(-1, dp(44));
        openLp.topMargin = dp(12);
        card.addView(open, openLp);
        open.setOnClickListener(v -> launch(account));
        return card;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : ink);
        b.setBackground(shape(primary ? purple : soft, 13, 0));
        b.setPadding(dp(10), 0, dp(10), 0);
        return b;
    }

    private void addDialog() {
        String[] services = {"Claude", "ChatGPT"};
        new AlertDialog.Builder(this)
                .setTitle("Servis seç")
                .setItems(services, (dialog, which) -> labelDialog(services[which]))
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void labelDialog(String service) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(service.equals("Claude") ? "Örn. İş Claude" : "Örn. Kişisel ChatGPT");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int pad = dp(20);
        input.setPadding(pad, dp(12), pad, dp(12));
        new AlertDialog.Builder(this)
                .setTitle("Hesaba bir ad ver")
                .setView(input)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Ekle", (dialog, which) -> {
                    String label = input.getText().toString().trim();
                    if (label.isEmpty()) label = service;
                    int slot = nextSessionSlot();
                    if (slot >= WEB_SESSION_SLOTS) {
                        new AlertDialog.Builder(this)
                                .setMessage("Bu prototip toplam 8 WebView oturumuyla sinirlidir. Guvenlik icin silinen oturum yuvalari yeniden kullanilmaz. Tum oturumlari sifirlamak icin Swapbar uygulama verisini temizle.")
                                .setPositiveButton("Tamam", null).show();
                        return;
                    }
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt("next_web_session_slot", slot + 1).apply();
                    accounts.add(new Account(UUID.randomUUID().toString(), service, label, slot));
                    save();
                    render();
                })
                .show();
    }

    private void accountMenu(Account account) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Adını değiştir", "Sil"}, (dialog, which) -> {
                    if (which == 0) rename(account);
                    else new AlertDialog.Builder(this).setMessage("“" + account.label + "” kaydı silinsin mi?")
                            .setNegativeButton("Vazgeç", null)
                            .setPositiveButton("Sil", (d, w) -> {
                                accounts.remove(account);
                                save();
                                render();
                            }).show();
                }).show();
    }

    private void rename(Account account) {
        EditText input = new EditText(this);
        input.setText(account.label);
        input.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("Hesap adını değiştir").setView(input)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet", (d, w) -> {
                    String label = input.getText().toString().trim();
                    if (!label.isEmpty()) account.label = label;
                    save();
                    render();
                }).show();
    }

    private void launch(Account account) {
        Class<?>[] sessions = {
                ClaudeSession0Activity.class, ClaudeSession1Activity.class,
                ClaudeSession2Activity.class, ClaudeSession3Activity.class,
                ClaudeSession4Activity.class, ClaudeSession5Activity.class,
                ClaudeSession6Activity.class, ClaudeSession7Activity.class
        };
        if (account.slot < 0 || account.slot >= sessions.length) {
            new AlertDialog.Builder(this).setMessage("Oturum profili bulunamadi. Hesabi silip yeniden ekle.")
                    .setPositiveButton("Tamam", null).show();
            return;
        }
        Intent session = new Intent(this, sessions[account.slot]);
        session.putExtra("account_label", account.label);
        session.putExtra("account_service", account.service);
        session.putExtra("theme_dark", darkTheme);
        startActivity(session);
    }

    private void load() {
        String encoded = getSharedPreferences(PREFS, MODE_PRIVATE).getString("accounts", "[]");
        try {
            JSONArray array = new JSONArray(encoded);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String service = item.optString("service", "Claude");
                if (!service.equals("Claude") && !service.equals("ChatGPT")) continue;
                int slot = item.optInt("slot", i);
                if (slot < 0 || slot >= WEB_SESSION_SLOTS) continue;
                accounts.add(new Account(item.optString("id", UUID.randomUUID().toString()), service,
                        item.optString("label", service), slot));
                if (slot >= 0) {
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    if (prefs.getInt("next_web_session_slot", prefs.getInt("next_claude_slot", 0)) <= slot) {
                        prefs.edit().putInt("next_web_session_slot", slot + 1).apply();
                    }
                }
            }
        } catch (Exception ignored) { accounts.clear(); }
    }

    private void save() {
        JSONArray array = new JSONArray();
        for (Account account : accounts) {
            JSONObject item = new JSONObject();
            try {
                item.put("id", account.id);
                item.put("service", account.service);
                item.put("label", account.label);
                if (account.slot >= 0) item.put("slot", account.slot);
                array.put(item);
            } catch (Exception ignored) { }
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString("accounts", array.toString()).apply();
    }

    private int nextSessionSlot() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        return prefs.getInt("next_web_session_slot", prefs.getInt("next_claude_slot", 0));
    }

    private String themeLabel() {
        if (themeMode == THEME_LIGHT) return "Açık";
        if (themeMode == THEME_DARK) return "Koyu";
        return "Sistem";
    }

    private void chooseTheme() {
        String[] choices = {"Sistem", "Açık", "Koyu"};
        new AlertDialog.Builder(this)
                .setTitle("Görünüm")
                .setSingleChoiceItems(choices, themeMode, (dialog, which) -> {
                    themeMode = which;
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(THEME_KEY, themeMode).apply();
                    applyAppearance();
                    render();
                    dialog.dismiss();
                })
                .setNegativeButton("Kapat", null)
                .show();
    }
}
