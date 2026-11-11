package com.dumplua.myapp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.*;

public class MainActivity extends Activity {
    static final int BG = 0xFF0F1115, CARD = 0xFF1A1D24, TXT = 0xFFE6E8EB, DIM = 0xFF8B93A1;
    static final int RED = 0xFFB3261E, AMBER = 0xFFB26A00, GREEN = 0xFF1B7F3B, GRAY = 0xFF2A2F3A;
    static final int BLUE = 0xFF3D6EF5;

    File home;
    String decNote = "";
    String linkNote = "";
    boolean linkWarn;
    LinearLayout statusCard, pageRingkas, pageTeknis;
    TextView errText, stTitle, stSub, fileName, summary, blockText, apiCount, apiText, unkText, tabA, tabB;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    GradientDrawable box(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    TextView tv(String t, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    TextView card(int size, boolean mono) {
        TextView v = tv("", size, TXT, false);
        if (mono) v.setTypeface(Typeface.MONOSPACE);
        v.setTextIsSelectable(true);
        v.setPadding(dp(16), dp(16), dp(16), dp(16));
        v.setBackgroundDrawable(box(CARD, 14));
        return v;
    }

    LinearLayout.LayoutParams lp(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    TextView tab(String t, final int idx) {
        TextView v = tv(t, 13, Color.WHITE, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(12), 0, dp(12));
        v.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) { sel(idx); }
        });
        return v;
    }

    void sel(int idx) {
        pageRingkas.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        pageTeknis.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        tabA.setBackgroundDrawable(box(idx == 0 ? BLUE : CARD, 10));
        tabB.setBackgroundDrawable(box(idx == 1 ? BLUE : CARD, 10));
        tabA.setTextColor(idx == 0 ? Color.WHITE : DIM);
        tabB.setTextColor(idx == 1 ? Color.WHITE : DIM);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getActionBar() != null) getActionBar().hide();
        home = getFilesDir();
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(28), dp(16), dp(28));
        sv.addView(root);

        Button btn = new Button(this);
        btn.setText("PILIH FILE SCRIPT");
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundDrawable(box(BLUE, 14));
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, 1);
            }
        });
        LinearLayout.LayoutParams bp = lp(0);
        bp.height = dp(56);
        root.addView(btn, bp);

        fileName = tv("", 12, DIM, false);
        fileName.setGravity(Gravity.CENTER);
        root.addView(fileName, lp(8));

        statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setGravity(Gravity.CENTER_HORIZONTAL);
        statusCard.setPadding(dp(20), dp(28), dp(20), dp(28));
        stTitle = tv("", 22, Color.WHITE, true);
        stTitle.setGravity(Gravity.CENTER);
        stSub = tv("", 14, 0xFFEDEDED, false);
        stSub.setGravity(Gravity.CENTER);
        statusCard.addView(stTitle);
        statusCard.addView(stSub, lp(8));
        root.addView(statusCard, lp(14));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabA = tab("RINGKASAN", 0);
        tabB = tab("TEKNIS", 1);
        LinearLayout.LayoutParams t1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout.LayoutParams t2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        t2.leftMargin = dp(8);
        tabs.addView(tabA, t1);
        tabs.addView(tabB, t2);
        root.addView(tabs, lp(16));

        pageRingkas = new LinearLayout(this);
        pageRingkas.setOrientation(LinearLayout.VERTICAL);
        summary = card(14, false);
        summary.setLineSpacing(0, 1.25f);
        pageRingkas.addView(summary, lp(0));
        root.addView(pageRingkas, lp(12));

        pageTeknis = new LinearLayout(this);
        pageTeknis.setOrientation(LinearLayout.VERTICAL);
        pageTeknis.addView(tv("ERROR SCRIPT", 11, DIM, true), lp(0));
        errText = card(12, true);
        pageTeknis.addView(errText, lp(6));
        pageTeknis.addView(tv("PERCOBAAN YANG DIBLOKIR", 11, DIM, true), lp(18));
        blockText = card(12, true);
        pageTeknis.addView(blockText, lp(6));
        apiCount = tv("", 11, DIM, true);
        pageTeknis.addView(apiCount, lp(18));
        apiText = card(12, true);
        pageTeknis.addView(apiText, lp(6));
        pageTeknis.addView(tv("FUNGSI TIDAK DIKENAL", 11, DIM, true), lp(18));
        unkText = card(12, true);
        pageTeknis.addView(unkText, lp(6));
        root.addView(pageTeknis, lp(12));

        setContentView(sv);
        sel(0);
        show("SIAP DIPERIKSA", "Pilih file script (.lua) untuk dicek", GRAY);
        summary.setText("Belum ada file yang diperiksa.");
        errText.setText("-");
        blockText.setText("-");
        apiCount.setText("FUNGSI YANG DIPAKAI SCRIPT");
        apiText.setText("-");
        unkText.setText("-");
    }

    void show(String t, String s, int color) {
        stTitle.setText(t);
        stSub.setText(s);
        statusCard.setBackgroundDrawable(box(color, 18));
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req != 1 || res != RESULT_OK || data == null) return;
        final Uri u = data.getData();
        fileName.setText(nameOf(u));
        show("MEMERIKSA...", "Mohon tunggu sebentar", GRAY);
        sel(0);
        new Thread(new Runnable() {
            public void run() {
                String dump;
                try { dump = go(u); } catch (Exception e) { dump = "GAGAL: " + e; }
                final String d = dump;
                runOnUiThread(new Runnable() {
                    public void run() { render(d); }
                });
            }
        }).start();
    }

    String nameOf(Uri u) {
        Cursor c = null;
        try {
            c = getContentResolver().query(u, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception e) {
        } finally {
            if (c != null) c.close();
        }
        return "script.lua";
    }

    void render(String dump) {
        if (dump.startsWith("GAGAL")) {
            show("GAGAL DIPERIKSA", "Pemeriksaan tidak bisa dijalankan", AMBER);
            summary.setText(dump);
            errText.setText("-");
            blockText.setText("-");
            apiText.setText("-");
            unkText.setText("-");
            return;
        }
        String warn = section(dump, "== PERINGATAN ==", "== RINGKASAN");
        String err = section(dump, "== ERROR ==", "== PERINGATAN ==");
        errText.setText(err.length() == 0 ? "(tidak ada error)" : (err.length() > 1500 ? err.substring(0, 1500) + "\n..." : err));

        boolean key = warn.contains("KEYLOG:");
        String url = "";
        int ui = warn.indexOf("URL terlihat: ");
        if (ui >= 0) {
            url = warn.substring(ui + 14);
            int nl = url.indexOf('\n');
            if (nl >= 0) url = url.substring(0, nl);
            url = url.trim();
        }
        int nb = 0;
        int bi = warn.indexOf("BLOCK: ");
        if (bi >= 0) {
            int j = bi + 7, k = j;
            while (k < warn.length() && Character.isDigit(warn.charAt(k))) k++;
            try { nb = Integer.parseInt(warn.substring(j, k)); } catch (Exception e) { nb = 1; }
        }

        if (key)
            show("BERBAHAYA", "Script ini mencuri ketikan (misalnya password) dan mengirimnya ke luar. Jangan dipakai.", RED);
        else if (err.contains("SYNTAX:"))
            show("GAGAL MEMBACA KODE", "Isi script ini tidak bisa diperiksa. Jangan anggap aman.", AMBER);
        else if (url.length() > 0 || nb > 0)
            show("HATI-HATI", "Ada hal mencurigakan. Baca rincian di bawah sebelum memakai script ini.", AMBER);
        else if (err.contains("TOPLEVEL:"))
            show("TIDAK TERBACA PENUH", "Script berhenti di tengah jalan, sebagian kodenya belum diperiksa. Jangan anggap aman.", AMBER);
        else if (err.contains("STATIK:"))
            show("DIPINDAI STATIS", "Script tidak dijalankan, hanya string-nya dipindai. Bukan jaminan aman.", AMBER);
        else
            show("TIDAK ADA TANDA BAHAYA", "Tapi ini bukan jaminan 100% aman. Pakai hanya script dari sumber yang kamu percaya.", GREEN);

        StringBuilder s = new StringBuilder();
        s.append("Mencuri ketikan / password\n");
        s.append(key ? "YA, terdeteksi\n\n" : "Tidak terdeteksi\n\n");
        s.append("Mengirim data ke internet\n");
        s.append(url.length() > 0 ? url + "\n\n" : "Tidak terlihat\n\n");
        s.append("Akses yang mencurigakan\n");
        s.append(nb > 0 ? nb + " percobaan (sudah diblokir)" : "Tidak ada");
        if (err.length() > 0)
        {
            String fl = err.split("\n")[0];
            if (fl.length() > 160) fl = fl.substring(0, 160);
            s.append("\n\nCatatan: script sempat berhenti karena error, jadi sebagian kodenya mungkin belum terperiksa.\nPenyebab: " + fl + "\n(rincian lengkap di tab TEKNIS)");
        }
        summary.setText(s.toString());
        if (err.contains("SYNTAX:")) {
            String fl2 = err.split("\n")[0];
            if (fl2.length() > 160) fl2 = fl2.substring(0, 160);
            summary.setText("Kode script ini tidak bisa dibaca oleh pemeriksa. Kemungkinan file sudah dikompilasi (bukan teks biasa) atau rusak, jadi isinya tidak diperiksa sama sekali.\n\nPenyebab: " + fl2 + "\n\nJangan pakai di akun utama kalau sumbernya tidak kamu percaya.");
        }

        if (err.contains("STATIK:"))
            summary.setText("Script ini bytecode non-GC64, jadi TIDAK dijalankan. Hanya string di dalamnya yang dipindai.\n\nLink ditemukan: " + (url.length() > 0 ? url : "tidak ada") + "\n\nKata kunci berbahaya: " + (nb > 0 ? nb + " (lihat tab TEKNIS)" : "tidak ada") + "\n\nPerilaku sebenarnya belum diperiksa.");
        String tr = section(dump, "== TRACE ==", "@@@@");
        StringBuilder bl = new StringBuilder();
        int cnt = 0;
        for (String ln : tr.split("\n")) {
            if (ln.contains("] BLOCK ") && cnt++ < 30)
                bl.append(ln.length() > 300 ? ln.substring(0, 300) : ln).append('\n');
        }
        blockText.setText(bl.length() == 0 ? "(tidak ada)" : bl.toString().trim());

        String api = section(dump, "== RINGKASAN PANGGILAN ==", "== GLOBAL TAK DIKENAL");
        String unk = section(dump, "== GLOBAL TAK DIKENAL (API game / typo) ==", "== MODUL DI-MOCK");
        int n = api.length() == 0 ? 0 : api.split("\n").length;
        apiCount.setText("FUNGSI YANG DIPAKAI SCRIPT (" + n + ")");
        apiText.setText(api.length() == 0 ? "(kosong)" : api);
        unkText.setText(unk.length() == 0 ? "(kosong)" : unk);
        if (decNote.length() > 0) summary.setText(summary.getText() + "\n\n" + decNote);
        if (linkNote.length() > 0) summary.setText(summary.getText() + "\n\n" + linkNote);
        if (linkWarn && !key && !err.contains("SYNTAX:"))
            show("HATI-HATI", "Ada link yang berpotensi dipakai mengirim data keluar atau mengunduh file. Lihat rincian di RINGKASAN.", AMBER);
    }

    String section(String s, String a, String b) {
        int i = s.indexOf(a);
        if (i < 0) return "";
        i += a.length();
        int j = s.indexOf(b, i);
        if (j < 0) j = s.length();
        return s.substring(i, j).trim();
    }

    String go(Uri u) throws Exception {
        decNote = "";
        linkNote = "";
        linkWarn = false;
        copyAsset("mock.lua", new File(home, "mock.lua"));
        copyAsset("api_dump.txt", new File(home, "api_dump.txt"));
        File sb = new File(home, "sandbox");
        sb.mkdirs();
        File s = new File(sb, "script.lua");
        copy(getContentResolver().openInputStream(u), new FileOutputStream(s));
        linkNote = links(s);
        if (isBc(s)) {
            File d = new File(sb, "decompiled.lua");
            boolean ok = decompile(s, d) == null;
            if (ok) linkNote = links(d);
            if (ok) saveDec(d, nameOf(u));
            if (nonGc64(s)) {
                if (ok) runCopy(d, s);
                else return scan(s);
            }
        }
        new File(home, "dump_script.lua.txt").delete();
        String lib = getApplicationInfo().nativeLibraryDir + "/libluajit_exec.so";
        ProcessBuilder pb = new ProcessBuilder(lib,
            new File(home, "mock.lua").getPath(), s.getPath());
        pb.directory(sb);
        pb.environment().put("HOME", home.getPath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = read(p.getInputStream());
        int code = p.waitFor();
        File df = new File(home, "dump_script.lua.txt");
        if (!df.exists()) return "GAGAL: exit " + code + "\n" + out;
        return read(new FileInputStream(df));
    }

    boolean nonGc64(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        byte[] h = new byte[5];
        int n = in.read(h);
        in.close();
        return n == 5 && h[0] == 0x1B && h[1] == 'L' && h[2] == 'J' && (h[4] & 8) == 0;
    }

    String scan(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        in.readFully(b);
        in.close();
        StringBuilder all = new StringBuilder(), cur = new StringBuilder();
        for (int i = 0; i <= b.length; i++) {
            int c = i < b.length ? b[i] & 0xFF : 0;
            if (c >= 32 && c < 127) cur.append((char) c);
            else { if (cur.length() >= 4) all.append(cur).append('\n'); cur.setLength(0); }
        }
        String s = all.toString();
        String[] bad = {"socket", "io.open", "os.execute", "popen", "loadstring",
            "downloadUrlToFile", "ffi", "webhook", "discord.com/api", "token", "password"};
        int nb = 0;
        StringBuilder hit = new StringBuilder();
        for (String k : bad) if (java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(k) + "\\b").matcher(s).find()) { nb++; hit.append(k).append(' '); }
        String url = "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("https?://\\S+").matcher(s);
        if (m.find()) url = m.group();
        return "== ERROR ==\nSTATIK: bytecode non-GC64, tidak dijalankan, hanya string dipindai\n"
            + "== PERINGATAN ==\n" + (url.length() > 0 ? "URL terlihat: " + url + "\n" : "")
            + (nb > 0 ? "BLOCK: " + nb + " kata kunci: " + hit + "\n" : "")
            + "== RINGKASAN PANGGILAN ==\n" + s
            + "== GLOBAL TAK DIKENAL (API game / typo) ==\n\n== MODUL DI-MOCK\n";
    }

    String decompile(File in, File out) {
        try {
            out.delete();
            if (!com.chaquo.python.Python.isStarted())
                com.chaquo.python.Python.start(new com.chaquo.python.android.AndroidPlatform(this));
            String r = com.chaquo.python.Python.getInstance().getModule("decwrap")
                .callAttr("run", in.getPath(), out.getPath()).toString();
            return (r.length() == 0 && out.length() > 0) ? null : "dec: " + r;
        } catch (Throwable t) {
            return "dec: " + t;
        }
    }

    boolean isBc(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        byte[] h = new byte[3];
        int n = in.read(h);
        in.close();
        return n == 3 && h[0] == 0x1B && h[1] == 'L' && h[2] == 'J';
    }

    void saveDec(File d, String name) {
        try {
            File dir = new File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS), "dumplua");
            dir.mkdirs();
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            File o = new File(dir, base + "_dec.lua");
            copy(new FileInputStream(d), new FileOutputStream(o));
            decNote = "Hasil decompile (.lua) disimpan di:\n" + o.getPath();
        } catch (Exception e) {
            decNote = "Hasil decompile gagal disimpan ke Download: " + e;
        }
    }

    void runCopy(File d, File s) throws IOException {
        String head = "local __n=setmetatable({},{__index=function(t) return t end,__call=function(t) return t end,__tostring=function() return '' end});for __i=0,255 do _G['uv'..__i]=__n end; ";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        copy(new FileInputStream(d), bo);
        String t = bo.toString("UTF-8");
        t = t.replaceAll("(for\\s+\\w+\\s*=\\s*[^,\\n]+,\\s*)uv\\d+(\\s+do\\b)", "$1 3$2");
        t = t.replaceAll("(for\\s+\\w+\\s*=\\s*)uv\\d+(\\s*,)", "$1 1$2");
        FileOutputStream o = new FileOutputStream(s);
        o.write(head.getBytes("UTF-8"));
        o.write(t.getBytes("UTF-8"));
        o.close();
    }

    String classify(String url) {
        String u = url.toLowerCase();
        if (u.contains("discord.com/api/webhooks") || u.contains("discordapp.com/api/webhooks")) return "WEBHOOK Discord (bisa kirim data keluar)";
        if (u.contains("api.telegram.org")) return "Bot Telegram API (bisa kirim data keluar)";
        if (u.matches("(?s).*\\.(apk|zip|exe|so|lua|luac|dex|bin)([?#].*)?$")) return "File unduhan";
        if (u.matches("(?s)^(https?://)?\\d{1,3}(\\.\\d{1,3}){3}.*")) return "Alamat IP langsung";
        if (u.matches("(?s)^(https?://)?(www\\.)?(bit\\.ly|tinyurl\\.com|cutt\\.ly|is\\.gd|t\\.co|rb\\.gy|shorturl\\.at)/.*")) return "Pemendek link (tujuan tersembunyi)";
        if (u.contains("discord.gg") || u.contains("discord.com/invite") || u.contains("discordapp.com/invite")) return "Undangan Discord";
        if (u.contains("discord.com") || u.contains("discordapp.com")) return "Discord";
        if (u.contains("t.me/") || u.contains("telegram.me/")) return "Telegram";
        if (u.contains("raw.githubusercontent.com") || u.contains("gist.github")) return "GitHub (file/kode mentah)";
        if (u.contains("github.com")) return "GitHub";
        if (u.contains("pastebin.com") || u.contains("hastebin") || u.contains("paste.")) return "Situs paste (bisa berisi kode)";
        if (u.contains("youtube.com") || u.contains("youtu.be")) return "YouTube";
        if (u.contains("wa.me") || u.contains("whatsapp.com")) return "WhatsApp";
        if (u.contains("tiktok.com")) return "TikTok";
        if (u.contains("instagram.com")) return "Instagram";
        return "Lainnya";
    }

    boolean risky(String k) {
        return k.startsWith("WEBHOOK") || k.startsWith("Bot Telegram") || k.equals("File unduhan")
            || k.startsWith("Alamat IP") || k.startsWith("Pemendek") || k.startsWith("GitHub (file")
            || k.startsWith("Situs paste");
    }

    String links(File f) {
        try {
            linkWarn = false;
            byte[] b = new byte[(int) f.length()];
            DataInputStream in = new DataInputStream(new FileInputStream(f));
            in.readFully(b);
            in.close();
            StringBuilder all = new StringBuilder(), cur = new StringBuilder();
            for (int i = 0; i <= b.length; i++) {
                int c = i < b.length ? b[i] & 0xFF : 0;
                if (c >= 32 && c < 127) cur.append((char) c);
                else { if (cur.length() >= 6) all.append(cur).append('\n'); cur.setLength(0); }
            }
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?i)(?:https?://|www\\.)[^\\s\"'<>\\\\)\\]]+|\\b(?:discord\\.gg|t\\.me|wa\\.me|bit\\.ly|youtu\\.be)/[^\\s\"'<>\\\\)\\]]+").matcher(all);
            java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<String>();
            while (m.find() && set.size() < 30) set.add(m.group().replaceAll("[.,;:!?]+$", ""));
            if (set.isEmpty()) return "Link di dalam script: tidak ada";
            StringBuilder o = new StringBuilder("Link yang tertulis di script (" + set.size() + "), hanya teks, belum tentu diakses:");
            for (String u : set) {
                String k = classify(u);
                boolean r = risky(k);
                if (r) linkWarn = true;
                o.append("\n").append(r ? "\u26A0 " : "\u2022 ").append("[").append(k).append("] ").append(u);
            }
            return o.toString();
        } catch (Exception e) {
            return "";
        }
    }

    String read(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder o = new StringBuilder();
        String l;
        while ((l = br.readLine()) != null) o.append(l).append('\n');
        br.close();
        return o.toString();
    }

    void copyAsset(String n, File dst) throws IOException {
        copy(getAssets().open(n), new FileOutputStream(dst));
    }

    void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
        out.close();
        in.close();
    }
}
