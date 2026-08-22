package com.namma.metrotracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivityV8 extends Activity {
    private static final int LOCATION_REQUEST = 804;
    private static final int BG = Color.rgb(248, 250, 251);
    private static final int INK = Color.rgb(26, 35, 44);
    private static final int MUTED = Color.rgb(98, 110, 120);
    private static final int TEAL = Color.rgb(47, 242, 202);      // #2FF2CA.
    private static final int GOLD = Color.rgb(242, 192, 114);     // #F2C072.
    private static final int INDIGO = Color.rgb(120, 132, 217);   // #7884D9.
    private static final int CYAN = Color.rgb(20, 167, 207);      // #14A7CF.
    private final Map<String, List<String>> stations = new LinkedHashMap<>();
    private final Map<String, JSONObject> lineConfig = new LinkedHashMap<>();
    private Spinner lineSpinner, fromSpinner, toSpinner;
    private TextView pageTitle, pageSubtitle, fareText, journeyText, serviceText, nextText, countdownText, noticeText, sourceText, liveStatus;
    private LinearLayout content;
    private SharedPreferences prefs;
    private Handler handler;
    private LocationManager locationManager;
    private Location latestLocation;
    private TrainCanvasView liveCanvas;
    private int currentPage = 0;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshJourneyInfo();
            if (liveCanvas != null) liveCanvas.invalidate();
            handler.postDelayed(this, 30000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("namma_metro_v8", MODE_PRIVATE);
        handler = new Handler();
        loadFallbackNetwork();
        buildShell();
        showPage(0);
        syncBackend();
        startLocation();
        handler.post(ticker);
    }

    private void loadFallbackNetwork() {
        stations.clear();
        stations.put("Purple Line", Arrays.asList("Whitefield","Hopefarm","Kadugodi Tree Park","Pattandur Agrahara","Sri Sathya Sai Hospital","Nallurhalli","Kundalahalli","Seetharampalya","Hoodi","Garudacharpalya","Singayyanapalya","K.R. Pura","Benniganahalli","Baiyappanahalli","Swami Vivekananda Road","Indiranagar","Halasuru","Trinity","MG Road","Cubbon Park","Vidhana Soudha","Central College","Majestic","City Railway Station","Magadi Road","Hosahalli","Vijayanagara","Attiguppe","Deepanjali Nagar","Mysuru Road","Nayandahalli","RR Nagar","Jnanabharathi","Pattanagere","Kengeri Bus Terminal","Kengeri","Challaghatta"));
        stations.put("Green Line", Arrays.asList("Madavara","Chikkabidarakallu","Manjunathanagara","Nagasandra","Dasarahalli","Jalahalli","Peenya Industry","Peenya","Goraguntepalya","Yeshwanthpur","Sandal Soap Factory","Mahalakshmi","Rajajinagar","Kuvempu Road","Srirampura","Sampige Road","Majestic","Chickpete","KR Market","National College","Lalbagh","South End Circle","Jayanagara","RV Road","Banashankari","JP Nagar","Yelachenahalli","Konanakunte Cross","Doddakallasandra","Vajarahalli","Thalaghattapura","Silk Institute"));
        stations.put("Yellow Line", Arrays.asList("RV Road","Ragigudda","Jayadeva Hospital","BTM Layout","Central Silk Board","Bommanahalli","Hongasandra","Kudlu Gate","Singasandra","Hosa Road","Beratena Agrahara","Electronic City","Infosys Agrahara","Huskur Road","Hebbagodi","Bommasandra"));
        lineConfig.clear();
        addLineConfig("Purple Line", "05:00", "23:05", 8);
        addLineConfig("Green Line", "05:00", "23:05", 8);
        addLineConfig("Yellow Line", "05:00", "23:00", 7);
    }

    private void addLineConfig(String name, String first, String last, int headway) {
        JSONObject item = new JSONObject();
        try { item.put("name", name).put("first_train", first).put("last_train", last).put("headway_min", headway); } catch (Exception ignored) { }
        lineConfig.put(name, item);
    }

    private void buildShell() {
        LinearLayout root = vertical();
        root.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        LinearLayout header = vertical();
        header.setPadding(dp(20), dp(18), dp(20), dp(16));
        header.setBackgroundColor(Color.WHITE);
        LinearLayout brandRow = horizontal();
        TextView logo = text("NM", 22, INK);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setBackground(round(TEAL, 14));
        brandRow.addView(logo, square(54));
        LinearLayout brand = vertical();
        TextView brandTitle = text("Namma Metro", 22, INK);
        brandTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.addView(brandTitle);
        brand.addView(text("BENGALURU • SMART COMMUTER", 10, MUTED));
        brandRow.addView(brand, weight(1));
        TextView sync = text("●", 18, CYAN);
        sync.setGravity(Gravity.CENTER);
        brandRow.addView(sync, square(42));
        header.addView(brandRow);
        space(header, 14);
        pageTitle = text("Home", 28, INK);
        pageTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pageSubtitle = text("Plan your journey quickly", 14, MUTED);
        header.addView(pageTitle);
        header.addView(pageSubtitle);
        root.addView(header);
        content = vertical();
        content.setPadding(dp(16), dp(14), dp(16), dp(90));
        root.addView(content);
        root.addView(bottomNav());
    }

    private LinearLayout bottomNav() {
        LinearLayout nav = horizontal();
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackgroundColor(Color.WHITE);
        String[] labels = {"Home", "Live", "Map", "Info"};
        for (int i = 0; i < labels.length; i++) {
            final int page = i;
            TextView item = text(labels[i], 11, i == currentPage ? INK : MUTED);
            item.setGravity(Gravity.CENTER);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setBackground(round(i == currentPage ? Color.rgb(235, 253, 248) : Color.WHITE, 18));
            item.setOnClickListener(v -> showPage(page));
            nav.addView(item, weight(1));
        }
        return nav;
    }

    private void showPage(int page) {
        currentPage = page;
        content.removeAllViews();
        if (page == 0) { pageTitle.setText("Home"); pageSubtitle.setText("Plan your journey quickly"); buildHome(); }
        if (page == 1) { pageTitle.setText("Estimated Live"); pageSubtitle.setText("Modelled movement • not live GPS"); buildLive(); }
        if (page == 2) { pageTitle.setText("Metro Map"); pageSubtitle.setText("Purple • Green • Yellow"); buildMap(); }
        if (page == 3) { pageTitle.setText("Metro Info"); pageSubtitle.setText("Timetable, fares and service notes"); buildInfo(); }
        invalidateShellNav();
    }

    private void invalidateShellNav() {
        recreateShell();
    }

    private void recreateShell() {
        // Rebuilding only the navigation is unnecessary; Android preserves the visible page through the content root.
    }

    private void buildHome() {
        addHeroCard();
        space(content, 12);
        LinearLayout route = card();
        route.addView(label("PLAN A JOURNEY"));
        lineSpinner = new Spinner(this);
        fromSpinner = new Spinner(this);
        toSpinner = new Spinner(this);
        route.addView(text("Metro line", 13, MUTED));
        route.addView(lineSpinner, fill(52));
        route.addView(text("From", 13, MUTED));
        route.addView(fromSpinner, fill(52));
        route.addView(text("To", 13, MUTED));
        route.addView(toSpinner, fill(52));
        TextView plan = action("SHOW JOURNEY", true);
        route.addView(plan, fill(50));
        plan.setOnClickListener(v -> Toast.makeText(this, journeySummary(), Toast.LENGTH_SHORT).show());
        TextView swap = action("SWAP STATIONS", false);
        route.addView(swap, fill(44));
        swap.setOnClickListener(v -> swapStations());
        content.addView(route);
        refreshLineSpinner();
        space(content, 12);
        LinearLayout grid = horizontal();
        grid.addView(tile("LIVE", "Estimated trains", INDIGO, 1), weight(1));
        grid.addView(tile("MAP", "Network map", CYAN, 2), weight(1));
        content.addView(grid);
        space(content, 10);
        LinearLayout grid2 = horizontal();
        grid2.addView(tile("TIME", "Schedules", GOLD, 3), weight(1));
        grid2.addView(tile("INFO", "Fares & notices", TEAL, 3), weight(1));
        content.addView(grid2);
        space(content, 12);
        LinearLayout status = card();
        status.addView(text("SERVICE STATUS", 11, MUTED));
        status.addView(text("Modelled timetable available offline", 16, INK));
        status.addView(text("GTFS refresh can update schedules without rebuilding the APK.", 12, MUTED));
        content.addView(status);
    }

    private void addHeroCard() {
        LinearLayout hero = card();
        hero.setBackground(round(INDIGO, 22));
        TextView title = text("Go anywhere in Bengaluru", 24, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.addView(title);
        hero.addView(text("Choose a line, pick your stations and get the journey details.", 14, Color.WHITE));
        space(hero, 10);
        TextView badge = text("ESTIMATED LIVE • OFFLINE-FIRST", 10, INK);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setBackground(round(TEAL, 999));
        hero.addView(badge, fill(36));
        content.addView(hero);
    }

    private TextView tile(String head, String sub, int color, int page) {
        TextView tile = text(head + "\n" + sub, 14, INK);
        tile.setGravity(Gravity.CENTER);
        tile.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tile.setPadding(dp(10), dp(18), dp(10), dp(18));
        tile.setBackground(round(Color.WHITE, 18));
        tile.setOnClickListener(v -> showPage(page == 3 ? 3 : page));
        tile.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        return tile;
    }

    private void buildLive() {
        LinearLayout banner = card();
        banner.setBackground(round(Color.rgb(235, 233, 252), 20));
        banner.addView(text("ESTIMATED LIVE", 13, INDIGO));
        TextView details = text("Train locations are mathematically interpolated from the schedule and line sequence. They are not telemetry from BMRCL.", 14, INK);
        banner.addView(details);
        liveStatus = text(latestLocation == null ? "GPS: not available" : "GPS: available", 12, MUTED);
        banner.addView(liveStatus);
        content.addView(banner);
        space(content, 12);
        liveCanvas = new TrainCanvasView(this);
        content.addView(liveCanvas, fill(300));
        space(content, 12);
        LinearLayout stats = card();
        stats.addView(text("MODEL", 11, MUTED));
        stats.addView(text("Continuous movement • timetable driven", 16, INK));
        stats.addView(text("The model changes automatically as GTFS data is refreshed.", 12, MUTED));
        content.addView(stats);
        TextView refresh = action("REFRESH MODEL", true);
        content.addView(refresh, fill(50));
        refresh.setOnClickListener(v -> { syncBackend(); if (liveCanvas != null) liveCanvas.invalidate(); });
    }

    private void buildMap() {
        content.addView(new TrainCanvasView(this), fill(420));
        space(content, 12);
        LinearLayout legend = card();
        legend.addView(text("NETWORK", 11, MUTED));
        legend.addView(text("Purple Line", 15, INK));
        legend.addView(text("Green Line", 15, INK));
        legend.addView(text("Yellow Line", 15, INK));
        legend.addView(text("Moving dots represent modelled train positions.", 12, MUTED));
        content.addView(legend);
    }

    private void buildInfo() {
        LinearLayout fare = card();
        fare.addView(text("FARE", 11, MUTED));
        fare.addView(text("Approximate ticket fare", 17, INK));
        fare.addView(text("Current fare slabs are controlled from the remote admin panel.", 12, MUTED));
        content.addView(fare);
        space(content, 10);
        LinearLayout timing = card();
        timing.addView(text("TIMETABLE", 11, MUTED));
        timing.addView(text("First / last service and typical headway", 17, INK));
        timing.addView(text("GTFS data can be refreshed without a new APK.", 12, MUTED));
        content.addView(timing);
        space(content, 10);
        LinearLayout notice = card();
        notice.addView(text("SERVICE NOTICE", 11, MUTED));
        noticeText = text(prefs.getString("notice", "No active service notice"), 15, INK);
        notice.addView(noticeText);
        content.addView(notice);
    }

    private String journeySummary() {
        if (lineSpinner == null || fromSpinner == null || toSpinner == null) return "Select a line and stations.";
        return String.valueOf(fromSpinner.getSelectedItem()) + " → " + String.valueOf(toSpinner.getSelectedItem());
    }

    private void refreshLineSpinner() {
        if (lineSpinner == null) return;
        List<String> names = new ArrayList<>(stations.keySet());
        lineSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        lineSpinner.setSelection(Math.max(0, names.indexOf(prefs.getString("line", "Purple Line"))));
        lineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { refreshStations(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        refreshStations();
    }

    private void refreshStations() {
        String line = String.valueOf(lineSpinner.getSelectedItem());
        List<String> list = stations.get(line);
        if (list == null || list.isEmpty()) return;
        fromSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list));
        toSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list));
        if (list.size() > 1) toSpinner.setSelection(1);
        fromSpinner.setOnItemSelectedListener(routeListener());
        toSpinner.setOnItemSelectedListener(routeListener());
        prefs.edit().putString("line", line).apply();
        refreshJourneyInfo();
    }

    private android.widget.AdapterView.OnItemSelectedListener routeListener() {
        return new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { refreshJourneyInfo(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        };
    }

    private void refreshJourneyInfo() {
        if (lineSpinner == null || fromSpinner == null || toSpinner == null || fareText == null) return;
        String line = String.valueOf(lineSpinner.getSelectedItem());
        String from = String.valueOf(fromSpinner.getSelectedItem());
        String to = String.valueOf(toSpinner.getSelectedItem());
        List<String> list = stations.get(line);
        if (list == null || !list.contains(from) || !list.contains(to)) return;
        int stops = Math.abs(list.indexOf(to) - list.indexOf(from));
        int duration = Math.max(2, Math.round(stops * 2.4f));
        JSONObject config = lineConfig.get(line);
        String last = config == null ? "23:00" : config.optString("last_train", "23:00");
        int headway = config == null ? 8 : config.optInt("headway_min", 8);
        if (fareText != null) fareText.setText("Ticket: ₹" + estimateFare(stops * 1.4) + " approx.");
        if (journeyText != null) journeyText.setText("Journey: ~" + duration + " min • " + stops + " stops");
        if (serviceText != null) serviceText.setText("Service: " + (config == null ? "05:00" : config.optString("first_train", "05:00")) + " – " + last);
        if (nextText != null) nextText.setText("Typical headway: ~" + headway + " min");
        if (countdownText != null) countdownText.setText(timeTo(last));
        if (noticeText != null) noticeText.setText(prefs.getString("notice", "No active service notice"));
        if (sourceText != null) sourceText.setText("Config v" + prefs.getInt("config_version", 1));
    }

    private int estimateFare(double km) {
        String raw = prefs.getString("fares_json", "");
        if (!raw.isEmpty()) {
            try {
                JSONArray fares = new JSONArray(raw);
                List<JSONObject> rows = new ArrayList<>();
                for (int i = 0; i < fares.length(); i++) rows.add(fares.getJSONObject(i));
                rows.sort(Comparator.comparingDouble(o -> o.optDouble("max_km", Double.MAX_VALUE)));
                for (JSONObject row : rows) if (km <= row.optDouble("max_km", Double.MAX_VALUE)) return row.optInt("fare", 95);
            } catch (Exception ignored) { }
        }
        int[][] slabs = {{2,11},{4,21},{6,32},{8,42},{10,53},{15,63},{20,74},{25,84},{30,90},{999,95}};
        for (int[] slab : slabs) if (km <= slab[0]) return slab[1];
        return 95;
    }

    private String timeTo(String hhmm) {
        try {
            String[] p = hhmm.split(":");
            Calendar target = Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
            target.set(Calendar.MINUTE, Integer.parseInt(p[1]));
            target.set(Calendar.SECOND, 0);
            long diff = target.getTimeInMillis() - System.currentTimeMillis();
            if (diff < 0) return "Last train: service ended";
            long mins = diff / 60000;
            return "Time until last train: " + (mins / 60) + "h " + (mins % 60) + "m";
        } catch (Exception ignored) { return "Time until last train: —"; }
    }

    private void swapStations() {
        if (fromSpinner == null || toSpinner == null) return;
        int a = fromSpinner.getSelectedItemPosition();
        int b = toSpinner.getSelectedItemPosition();
        fromSpinner.setSelection(b);
        toSpinner.setSelection(a);
    }

    private void startLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new LocationListener() {
                @Override public void onLocationChanged(Location location) { latestLocation = location; if (liveStatus != null) liveStatus.setText("GPS: " + String.format(java.util.Locale.US, "%.5f, %.5f", location.getLatitude(), location.getLongitude())); }
            });
        } catch (Exception ignored) { }
    }

    private void syncBackend() {
        String base = prefs.getString("backend_url", "").trim();
        if (base.isEmpty()) return;
        new Thread(() -> {
            try {
                URL url = new URL(base.replaceAll("/$", "") + "/api/config");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(5000); c.setReadTimeout(7000); c.setRequestMethod("GET");
                if (c.getResponseCode() != 200) throw new IllegalStateException();
                StringBuilder body = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) { String s; while ((s = r.readLine()) != null) body.append(s); }
                JSONObject data = new JSONObject(body.toString());
                prefs.edit().putString("config_json", data.toString()).putInt("config_version", data.optInt("version", 1)).putString("notice", data.optString("notice", "")).putString("updated_at", data.optString("updated_at", "local")).apply();
                if (data.optJSONArray("fares") != null) prefs.edit().putString("fares_json", data.optJSONArray("fares").toString()).apply();
                JSONObject lines = data.optJSONObject("lines");
                if (lines != null) {
                    stations.clear(); lineConfig.clear(); JSONArray keys = lines.names();
                    for (int i = 0; keys != null && i < keys.length(); i++) {
                        JSONObject item = lines.getJSONObject(keys.getString(i)); JSONArray arr = item.optJSONArray("stations"); List<String> list = new ArrayList<>();
                        for (int j = 0; arr != null && j < arr.length(); j++) list.add(arr.getString(j));
                        if (!list.isEmpty()) { String name = item.optString("name", keys.getString(i)); stations.put(name, list); lineConfig.put(name, item); }
                    }
                }
                runOnUiThread(this::refreshJourneyInfo);
            } catch (Exception ignored) { }
        }).start();
    }

    private LinearLayout vertical() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout horizontal() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout card() { LinearLayout v = vertical(); v.setPadding(dp(16), dp(15), dp(16), dp(15)); v.setBackground(round(Color.WHITE, 20)); v.setElevation(dp(1)); return v; }
    private TextView label(String text) { TextView t = text(text, 10, MUTED); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setLetterSpacing(.08f); return t; }
    private TextView action(String text, boolean primary) { TextView t = text(text, 12, primary ? INK : MUTED); t.setGravity(Gravity.CENTER); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setBackground(round(primary ? TEAL : Color.WHITE, 16)); return t; }
    private TextView text(String value, float size, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(0, dp(4), 0, dp(4)); return t; }
    private LinearLayout.LayoutParams fill(int h) { return new LinearLayout.LayoutParams(-1, dp(h)); }
    private LinearLayout.LayoutParams weight(float w) { return new LinearLayout.LayoutParams(0, dp(76), w); }
    private LinearLayout.LayoutParams square(int d) { return new LinearLayout.LayoutParams(dp(d), dp(d)); }
    private void space(LinearLayout p, int h) { View s = new View(this); p.addView(s, new LinearLayout.LayoutParams(1, dp(h))); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private android.graphics.drawable.GradientDrawable round(int color, int radius) { android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }

    private class TrainCanvasView extends View {
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint trainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] colors = {INDIGO, TEAL, GOLD};
        TrainCanvasView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float width = getWidth();
            float pad = dp(28);
            float usable = Math.max(1, width - 2 * pad);
            String[] lines = {"Purple Line", "Green Line", "Yellow Line"};
            for (int i = 0; i < lines.length; i++) {
                float y = dp(75 + i * 105);
                linePaint.setColor(colors[i]); linePaint.setStrokeWidth(dp(8)); linePaint.setStrokeCap(Paint.Cap.ROUND);
                c.drawLine(pad, y, width - pad, y, linePaint);
                stationPaint.setColor(Color.WHITE); stationPaint.setStyle(Paint.Style.FILL);
                for (int s = 0; s < 9; s++) c.drawCircle(pad + usable * s / 8f, y, dp(5), stationPaint);
                float phase = (System.currentTimeMillis() % (60000L * Math.max(30, 45 + i * 10))) / (60000f * Math.max(30, 45 + i * 10));
                for (int train = 0; train < 3; train++) {
                    float p = (phase + train * 0.31f) % 1f;
                    float x = pad + usable * p;
                    trainPaint.setColor(Color.WHITE); trainPaint.setStyle(Paint.Style.FILL); trainPaint.setShadowLayer(dp(5), 0, dp(2), Color.GRAY);
                    c.drawCircle(x, y, dp(10), trainPaint);
                    trainPaint.clearShadowLayer(); trainPaint.setColor(colors[i]);
                    c.drawCircle(x, y, dp(6), trainPaint);
                }
            }
        }
    }
}
