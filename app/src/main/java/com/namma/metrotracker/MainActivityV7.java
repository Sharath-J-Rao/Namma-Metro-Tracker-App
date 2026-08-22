package com.namma.metrotracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

public class MainActivityV7 extends Activity {
    private final Map<String, List<String>> stations = new LinkedHashMap<>();
    private final Map<String, JSONObject> lineConfig = new LinkedHashMap<>();
    private Spinner lineSpinner, fromSpinner, toSpinner;
    private TextView fareText, journeyText, nextText, serviceText, countdownText, noticeText, sourceText;
    private SharedPreferences prefs;
    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshInfo();
            handler.postDelayed(this, 30000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("namma_metro_v7", MODE_PRIVATE);
        loadFallbackNetwork();
        buildUi();
        applyCachedConfig();
        fetchConfig();
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
        JSONObject config = new JSONObject();
        try {
            config.put("name", name);
            config.put("first_train", first);
            config.put("last_train", last);
            config.put("headway_min", headway);
        } catch (Exception ignored) { }
        lineConfig.put(name, config);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = vertical();
        root.setPadding(dp(20), dp(18), dp(20), dp(28));
        root.setBackgroundColor(Color.rgb(246, 247, 250));
        scroll.addView(root);

        TextView title = text("Namma Metro", 28, Color.rgb(22, 27, 35));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(text("BENGALURU • SMART COMMUTER", 10, Color.rgb(105, 114, 126)));
        space(root, 16);

        root.addView(text("Plan your journey", 22, Color.rgb(22, 27, 35)));
        root.addView(text("Select a line first, then choose your stations.", 14, Color.rgb(96, 106, 119)));
        space(root, 12);

        LinearLayout lineCard = card();
        lineCard.addView(label("METRO LINE"));
        lineSpinner = new Spinner(this);
        lineCard.addView(lineSpinner, fillHeight(52));
        root.addView(lineCard);

        space(root, 12);
        LinearLayout journeyCard = card();
        journeyCard.addView(label("FROM"));
        fromSpinner = new Spinner(this);
        journeyCard.addView(fromSpinner, fillHeight(52));
        journeyCard.addView(label("TO"));
        toSpinner = new Spinner(this);
        journeyCard.addView(toSpinner, fillHeight(52));
        TextView swap = action("SWAP STATIONS", false);
        journeyCard.addView(swap, fillHeight(46));
        swap.setOnClickListener(v -> swapStations());
        root.addView(journeyCard);

        space(root, 14);
        LinearLayout infoCard = card();
        TextView heading = text("TRAVEL INFO", 18, Color.rgb(22, 27, 35));
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        infoCard.addView(heading);
        fareText = text("Ticket: —", 17, Color.rgb(22, 27, 35));
        journeyText = text("Journey: —", 15, Color.rgb(55, 65, 78));
        nextText = text("Next train: —", 15, Color.rgb(55, 65, 78));
        serviceText = text("Service: —", 15, Color.rgb(55, 65, 78));
        countdownText = text("Time until last train: —", 18, Color.rgb(18, 104, 208));
        countdownText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        infoCard.addView(fareText);
        infoCard.addView(journeyText);
        infoCard.addView(nextText);
        infoCard.addView(serviceText);
        infoCard.addView(countdownText);
        root.addView(infoCard);

        space(root, 12);
        LinearLayout noticeCard = card();
        noticeText = text("", 14, Color.rgb(30, 40, 50));
        sourceText = text("Offline configuration", 12, Color.rgb(115, 124, 136));
        noticeCard.addView(noticeText);
        noticeCard.addView(sourceText);
        root.addView(noticeCard);

        space(root, 10);
        LinearLayout actions = horizontal();
        TextView refresh = action("REFRESH", true);
        TextView backend = action("BACKEND", false);
        actions.addView(refresh, weight(50));
        actions.addView(backend, weight(50));
        root.addView(actions);
        refresh.setOnClickListener(v -> fetchConfig());
        backend.setOnClickListener(v -> showBackendDialog());

        space(root, 10);
        root.addView(text("Approximate timings and manually maintained fares are planning guidance only.", 11, Color.rgb(122, 130, 141)));
        setContentView(scroll);

        lineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { refreshStations(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        fromSpinner.setOnItemSelectedListener(simpleListener());
        toSpinner.setOnItemSelectedListener(simpleListener());
        refreshLineSpinner();
    }

    private android.widget.AdapterView.OnItemSelectedListener simpleListener() {
        return new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { refreshInfo(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        };
    }

    private void refreshLineSpinner() {
        List<String> names = new ArrayList<>(stations.keySet());
        lineSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        int selected = Math.max(0, names.indexOf(prefs.getString("line", "Purple Line")));
        lineSpinner.setSelection(selected);
        refreshStations();
    }

    private void refreshStations() {
        if (lineSpinner == null) return;
        String line = String.valueOf(lineSpinner.getSelectedItem());
        List<String> list = stations.get(line);
        if (list == null || list.isEmpty()) return;
        setSpinner(fromSpinner, list);
        setSpinner(toSpinner, list);
        if (list.size() > 1) toSpinner.setSelection(1);
        prefs.edit().putString("line", line).apply();
        refreshInfo();
    }

    private void refreshInfo() {
        if (lineSpinner == null || fromSpinner == null || toSpinner == null) return;
        String line = String.valueOf(lineSpinner.getSelectedItem());
        String from = String.valueOf(fromSpinner.getSelectedItem());
        String to = String.valueOf(toSpinner.getSelectedItem());
        List<String> list = stations.get(line);
        if (list == null || !list.contains(from) || !list.contains(to)) return;

        int stops = Math.abs(list.indexOf(to) - list.indexOf(from));
        int duration = Math.max(2, Math.round(stops * 2.4f));
        fareText.setText("Ticket: ₹" + estimateFare(stops * 1.4) + " approx.");
        journeyText.setText("Journey: ~" + duration + " min • " + stops + " stops");

        JSONObject config = lineConfig.get(line);
        String first = config == null ? "05:00" : config.optString("first_train", "05:00");
        String last = config == null ? "23:00" : config.optString("last_train", "23:00");
        int headway = config == null ? 8 : config.optInt("headway_min", 8);
        nextText.setText("Next train: ~" + headway + " min typical headway");
        serviceText.setText("Service: " + first + " – " + last);
        updateCountdown(last);

        String notice = prefs.getString("notice", "").trim();
        noticeText.setText(notice.isEmpty() ? "No service notices" : notice);
        sourceText.setText("Config v" + prefs.getInt("config_version", 1) + " • " + prefs.getString("updated_at", "local"));
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
        int[][] fallback = {{2,11},{4,21},{6,32},{8,42},{10,53},{15,63},{20,74},{25,84},{30,90},{999,95}};
        for (int[] slab : fallback) if (km <= slab[0]) return slab[1];
        return 95;
    }

    private void updateCountdown(String hhmm) {
        try {
            String[] parts = hhmm.split(":");
            Calendar target = Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            target.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
            if (target.before(Calendar.getInstance())) target.add(Calendar.DAY_OF_MONTH, 1);
            long minutes = Math.max(0, (target.getTimeInMillis() - System.currentTimeMillis()) / 60000);
            countdownText.setText("Time until last train: " + (minutes / 60) + "h " + (minutes % 60) + "m");
        } catch (Exception ignored) {
            countdownText.setText("Time until last train: —");
        }
    }

    private void fetchConfig() {
        String base = prefs.getString("backend_url", "").trim();
        if (base.isEmpty()) {
            sourceText.setText("Offline mode • backend not configured");
            return;
        }
        new Thread(() -> {
            try {
                URL url = new URL(base.replaceAll("/$", "") + "/api/config");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(7000);
                connection.setRequestMethod("GET");
                int code = connection.getResponseCode();
                if (code != 200) throw new IllegalStateException("HTTP " + code);
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String row;
                    while ((row = reader.readLine()) != null) body.append(row);
                }
                JSONObject remote = new JSONObject(body.toString());
                applyRemoteConfig(remote);
                runOnUiThread(() -> {
                    sourceText.setText("Config v" + prefs.getInt("config_version", 1) + " • synced from server");
                    refreshLineSpinner();
                    Toast.makeText(this, "Metro configuration updated", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> sourceText.setText("Offline mode • using last saved configuration"));
            }
        }).start();
    }

    private void applyRemoteConfig(JSONObject json) {
        try {
            prefs.edit()
                    .putInt("config_version", json.optInt("version", 1))
                    .putString("updated_at", json.optString("updated_at", "local"))
                    .putString("notice", json.optString("notice", ""))
                    .putString("config_json", json.toString())
                    .apply();
            JSONArray fares = json.optJSONArray("fares");
            if (fares != null) prefs.edit().putString("fares_json", fares.toString()).apply();
            JSONObject lines = json.optJSONObject("lines");
            if (lines == null) return;
            stations.clear();
            lineConfig.clear();
            JSONArray keys = lines.names();
            if (keys == null) return;
            for (int i = 0; i < keys.length(); i++) {
                String key = keys.getString(i);
                JSONObject line = lines.getJSONObject(key);
                String name = line.optString("name", key);
                List<String> values = new ArrayList<>();
                JSONArray stationArray = line.optJSONArray("stations");
                if (stationArray != null) for (int j = 0; j < stationArray.length(); j++) values.add(stationArray.getString(j));
                if (!values.isEmpty()) {
                    stations.put(name, values);
                    lineConfig.put(name, line);
                }
            }
        } catch (Exception ignored) { }
    }

    private void applyCachedConfig() {
        String cached = prefs.getString("config_json", "");
        if (!cached.isEmpty()) {
            try { applyRemoteConfig(new JSONObject(cached)); } catch (Exception ignored) { }
        }
    }

    private void showBackendDialog() {
        LinearLayout box = vertical();
        box.setPadding(dp(16), dp(4), dp(16), 0);
        box.addView(text("Enter the secure backend URL.", 12, Color.GRAY));
        android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setText(prefs.getString("backend_url", ""));
        input.setHint("https://your-api.example.com");
        box.addView(input);
        new AlertDialog.Builder(this)
                .setTitle("Backend URL")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    prefs.edit().putString("backend_url", value).apply();
                    fetchConfig();
                })
                .show();
    }

    private void swapStations() {
        int from = fromSpinner.getSelectedItemPosition();
        int to = toSpinner.getSelectedItemPosition();
        fromSpinner.setSelection(to);
        toSpinner.setSelection(from);
    }

    private void setSpinner(Spinner spinner, List<String> values) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
    }

    private LinearLayout vertical() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private LinearLayout horizontal() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.HORIZONTAL); layout.setGravity(Gravity.CENTER_VERTICAL); return layout; }
    private LinearLayout card() { LinearLayout layout = vertical(); layout.setPadding(dp(16), dp(15), dp(16), dp(15)); layout.setBackground(round(Color.WHITE, 18)); layout.setElevation(dp(1)); return layout; }
    private TextView text(String value, float size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setPadding(0, dp(4), 0, dp(4)); return view; }
    private TextView label(String value) { TextView view = text(value, 10, Color.rgb(112,121,133)); view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); view.setLetterSpacing(.07f); return view; }
    private TextView action(String value, boolean primary) { TextView view = text(value, 12, primary ? Color.WHITE : Color.rgb(45,57,70)); view.setGravity(Gravity.CENTER); view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); view.setBackground(round(primary ? Color.rgb(18,104,208) : Color.WHITE, 14)); view.setClickable(true); return view; }
    private LinearLayout.LayoutParams fillHeight(int height) { return new LinearLayout.LayoutParams(-1, dp(height)); }
    private LinearLayout.LayoutParams weight(int height) { return new LinearLayout.LayoutParams(0, dp(height), 1); }
    private TextView pillView(String value, int bg, int fg) { TextView view = text(value, 10, fg); view.setGravity(Gravity.CENTER); view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); view.setPadding(dp(10), dp(8), dp(10), dp(8)); view.setBackground(round(bg, 999)); return view; }
    private void space(LinearLayout parent, int height) { View spacer = new View(this); spacer.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); parent.addView(spacer); }
    private android.graphics.drawable.GradientDrawable round(int color, int radius) { android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
