package com.namma.metrotracker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivityV82 extends Activity {
    private static final int BG = Color.rgb(247, 249, 251);
    private static final int TEXT = Color.rgb(28, 36, 44);
    private static final int MUTED = Color.rgb(103, 114, 124);
    private static final int TEAL = Color.rgb(47, 242, 202);
    private static final int GOLD = Color.rgb(242, 192, 114);
    private static final int INDIGO = Color.rgb(120, 132, 217);
    private static final int CYAN = Color.rgb(20, 167, 207);
    private static final int PURPLE = Color.rgb(126, 70, 168);
    private static final int GREEN = Color.rgb(38, 155, 92);
    private static final int YELLOW = Color.rgb(232, 179, 41);
    private final Map<String, List<String>> stations = new LinkedHashMap<>();
    private Spinner lineSpinner;
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private LinearLayout content;
    private LinearLayout nav;
    private TextView title;
    private TextView subtitle;
    private int page = 0;
    private MetroMap map;
    private final Handler handler = new Handler();
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (map != null) map.invalidate();
            handler.postDelayed(this, 12000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadStations();
        buildShell();
        showPage(0);
        handler.post(refresh);
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(refresh);
        super.onDestroy();
    }
    private void loadStations() {
        stations.put("Purple Line", Arrays.asList("Whitefield","Hopefarm","Kadugodi Tree Park","Pattandur Agrahara","Sri Sathya Sai Hospital","Nallurhalli","Kundalahalli","Seetharampalya","Hoodi","Garudacharpalya","Singayyanapalya","K.R. Pura","Benniganahalli","Baiyappanahalli","Swami Vivekananda Road","Indiranagar","Halasuru","Trinity","MG Road","Cubbon Park","Vidhana Soudha","Central College","Majestic","City Railway Station","Magadi Road","Hosahalli","Vijayanagara","Attiguppe","Deepanjali Nagar","Mysuru Road","Nayandahalli","RR Nagar","Jnanabharathi","Pattanagere","Kengeri Bus Terminal","Kengeri","Challaghatta"));
        stations.put("Green Line", Arrays.asList("Madavara","Chikkabidarakallu","Manjunathanagara","Nagasandra","Dasarahalli","Jalahalli","Peenya Industry","Peenya","Goraguntepalya","Yeshwanthpur","Sandal Soap Factory","Mahalakshmi","Rajajinagar","Kuvempu Road","Srirampura","Sampige Road","Majestic","Chickpete","KR Market","National College","Lalbagh","South End Circle","Jayanagara","RV Road","Banashankari","JP Nagar","Yelahanalli","Konanakunte Cross","Doddakallasandra","Vajarahalli","Thalaghattapura","Silk Institute"));
        stations.put("Yellow Line", Arrays.asList("RV Road","Ragigudda","Jayadeva Hospital","BTM Layout","Central Silk Board","Bommanahalli","Hongasandra","Kudlu Gate","Singasandra","Hosa Road","Beratena Agrahara","Electronic City","Infosys Agrahara","Huskur Road","Hebbagodi","Bommasandra"));
    }
    private void buildShell() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = vertical();
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);
        LinearLayout header = vertical();
        header.setPadding(dp(18), dp(18), dp(18), dp(12));
        header.setBackgroundColor(Color.WHITE);
        LinearLayout brand = horizontal();
        TextView logo = text("N", 23, TEXT);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setBackground(round(TEAL, 15));
        brand.addView(logo, square(50));
        LinearLayout brandText = vertical();
        TextView app = text("Namma Metro Tracker", 21, TEXT);
        app.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brandText.addView(app);
        brandText.addView(text("Bengaluru", 11, MUTED));
        brand.addView(brandText, weight(1));
        TextView status = text("●", 18, CYAN);
        status.setGravity(Gravity.CENTER);
        brand.addView(status, square(40));
        header.addView(brand);
        space(header, 12);
        title = text("Home", 27, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        subtitle = text("Plan your trip", 14, MUTED);
        header.addView(title);
        header.addView(subtitle);
        root.addView(header);
        content = vertical();
        content.setPadding(dp(16), dp(12), dp(16), dp(84));
        root.addView(content);
        nav = horizontal();
        nav.setPadding(dp(8), dp(7), dp(8), dp(7));
        nav.setBackgroundColor(Color.WHITE);
        root.addView(nav);
        rebuildNav();
    }
    private void rebuildNav() {
        nav.removeAllViews();
        String[] labels = {"Home", "Journey", "Map", "Live"};
        for (int i = 0; i < labels.length; i++) {
            final int target = i;
            TextView item = text(labels[i], 11, target == page ? TEXT : MUTED);
            item.setGravity(Gravity.CENTER);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setBackground(round(target == page ? Color.rgb(231, 253, 247) : Color.WHITE, 16));
            item.setOnClickListener(v -> showPage(target));
            nav.addView(item, weight(1));
        }
    }
    private void showPage(int target) {
        page = target;
        content.removeAllViews();
        if (target == 0) buildHome();
        if (target == 1) buildJourney();
        if (target == 2) buildMap();
        if (target == 3) buildLive();
        updateHeader();
        rebuildNav();
    }
    private void updateHeader() {
        String[] titles = {"Home", "Your Journey", "Metro Map", "Live Trains"};
        String[] subs = {"Plan your trip", "Route, fare and timing", "Bengaluru network", "Trains moving across the network"};
        title.setText(titles[page]);
        subtitle.setText(subs[page]);
    }
    private void buildHome() {
        LinearLayout hero = card();
        hero.setBackground(round(INDIGO, 22));
        TextView h = text("Where are you going?", 25, Color.WHITE);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.addView(h);
        hero.addView(text("Choose a line and stations to see your journey.", 14, Color.WHITE));
        space(hero, 10);
        TextView pill = text("●  TRAINS RUNNING", 11, TEXT);
        pill.setGravity(Gravity.CENTER);
        pill.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pill.setBackground(round(TEAL, 999));
        hero.addView(pill, fill(36));
        content.addView(hero);
        space(content, 12);
        LinearLayout planner = card();
        planner.addView(label("PLAN A JOURNEY"));
        lineSpinner = new Spinner(this);
        fromSpinner = new Spinner(this);
        toSpinner = new Spinner(this);
        planner.addView(text("Line", 13, MUTED));
        planner.addView(lineSpinner, fill(50));
        planner.addView(text("From", 13, MUTED));
        planner.addView(fromSpinner, fill(50));
        planner.addView(text("To", 13, MUTED));
        planner.addView(toSpinner, fill(50));
        TextView show = action("SHOW JOURNEY", true);
        planner.addView(show, fill(54));
        show.setOnClickListener(v -> showPage(1));
        TextView swap = action("SWAP", false);
        planner.addView(swap, fill(42));
        swap.setOnClickListener(v -> swapStations());
        content.addView(planner);
        setupSpinners();
        space(content, 12);
        LinearLayout quick = horizontal();
        quick.addView(tile("LIVE TRAINS", "Watch trains", INDIGO, 3), weight(1));
        quick.addView(tile("METRO MAP", "Explore network", CYAN, 2), weight(1));
        content.addView(quick);
        space(content, 10);
        LinearLayout quick2 = horizontal();
        quick2.addView(tile("FARES", "Ticket cost", GOLD, 1), weight(1));
        quick2.addView(tile("TIMINGS", "First & last", TEAL, 1), weight(1));
        content.addView(quick2);
    }
    private void setupSpinners() {
        List<String> linesList = new ArrayList<>(stations.keySet());
        lineSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, linesList));
        lineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { refreshStations(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        refreshStations();
    }
    private void refreshStations() {
        if (lineSpinner == null) return;
        List<String> list = stations.get(selectedLine());
        if (list == null || list.isEmpty()) return;
        fromSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list));
        toSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list));
        if (list.size() > 1) toSpinner.setSelection(1);
    }
    private void buildJourney() {
        LinearLayout summary = card();
        summary.setBackground(round(Color.WHITE, 22));
        summary.addView(label("YOUR JOURNEY"));
        summary.addView(text(routeText(), 22, TEXT));
        summary.addView(text(selectedLine(), 13, lineColor(selectedLine())));
        summary.addView(text(stopCount() + " stops  •  " + durationText(), 14, MUTED));
        content.addView(summary);
        space(content, 10);
        LinearLayout stats = horizontal();
        stats.addView(stat("FARE", "₹" + fare(), GOLD), weight(1));
        stats.addView(stat("TIME", durationText(), TEAL), weight(1));
        stats.addView(stat("NEXT", nextTrain(), INDIGO), weight(1));
        content.addView(stats);
        space(content, 12);
        LinearLayout service = card();
        service.addView(label("SERVICE"));
        service.addView(text("First train   05:00", 15, TEXT));
        service.addView(text("Last train    23:05", 15, TEXT));
        service.addView(text("Typical gap   8 min", 15, TEXT));
        service.addView(text("Last train in " + countdown("23:05"), 16, CYAN));
        content.addView(service);
        space(content, 12);
        LinearLayout route = card();
        route.addView(label("YOUR ROUTE"));
        route.addView(new RouteView(this, selectedLine(), fromIndex(), toIndex()), fill(170));
        content.addView(route);
        space(content, 12);
        TextView live = action("SEE TRAINS ON THIS ROUTE", true);
        content.addView(live, fill(54));
        live.setOnClickListener(v -> showPage(3));
    }
    private TextView stat(String title, String value, int accent) {
        TextView t = text(title + "\n" + value, 13, TEXT);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setBackground(round(Color.WHITE, 18));
        t.setPadding(dp(7), dp(15), dp(7), dp(15));
        return t;
    }
    private void buildMap() {
        map = new MetroMap(this, selectedLine());
        LinearLayout mapCard = card();
        mapCard.setPadding(dp(4), dp(4), dp(4), dp(4));
        mapCard.addView(map, fill(540));
        content.addView(mapCard);
        space(content, 10);
        TextView focus = action("SHOW MY JOURNEY ON MAP", true);
        content.addView(focus, fill(52));
        focus.setOnClickListener(v -> map.setJourney(selectedLine(), fromIndex(), toIndex()));
        space(content, 8);
        content.addView(text("The full network stays visible. Your selected route is highlighted and focused when you press the button.", 12, MUTED));
    }
    private void buildLive() {
        LinearLayout intro = card();
        intro.setBackground(round(Color.rgb(241, 242, 253), 22));
        TextView h = text("Trains running now", 23, TEXT);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        intro.addView(h);
        intro.addView(text("Follow the moving trains across Bengaluru.", 14, MUTED));
        content.addView(intro);
        space(content, 10);
        map = new MetroMap(this, selectedLine());
        content.addView(map, fill(500));
        space(content, 10);
        content.addView(lineStatus("Purple Line", PURPLE, 8, "23:05"));
        space(content, 8);
        content.addView(lineStatus("Green Line", GREEN, 8, "23:05"));
        space(content, 8);
        content.addView(lineStatus("Yellow Line", YELLOW, 7, "23:00"));
    }
    private LinearLayout lineStatus(String name, int color, int gap, String last) {
        LinearLayout row = card();
        TextView title = text(name, 16, color);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(title);
        row.addView(text("Next train ~" + gap + " min", 14, TEXT));
        row.addView(text("Service until " + last, 12, MUTED));
        return row;
    }
    private String selectedLine() { return lineSpinner == null ? "Purple Line" : String.valueOf(lineSpinner.getSelectedItem()); }
    private int fromIndex() { return fromSpinner == null || fromSpinner.getSelectedItem() == null ? 0 : stationIndex(String.valueOf(fromSpinner.getSelectedItem())); }
    private int toIndex() { return toSpinner == null || toSpinner.getSelectedItem() == null ? 1 : stationIndex(String.valueOf(toSpinner.getSelectedItem())); }
    private int stationIndex(String station) { List<String> list = stations.get(selectedLine()); return list == null ? 0 : Math.max(0, list.indexOf(station)); }
    private int stopCount() { return Math.abs(toIndex() - fromIndex()); }
    private String routeText() { List<String> list = stations.get(selectedLine()); if (list == null || list.isEmpty()) return "Station → Station"; return list.get(Math.min(fromIndex(), list.size() - 1)) + " → " + list.get(Math.min(toIndex(), list.size() - 1)); }
    private int fare() { double km = Math.max(0.5, stopCount() * 1.4); int[][] slabs = {{2,11},{4,21},{6,32},{8,42},{10,53},{15,63},{20,74},{25,84},{30,90},{999,95}}; for (int[] slab : slabs) if (km <= slab[0]) return slab[1]; return 95; }
    private String durationText() { return "~" + Math.max(2, Math.round(stopCount() * 2.4f)) + " min"; }
    private String nextTrain() { return selectedLine().startsWith("Yellow") ? "~7 min" : "~8 min"; }
    private String countdown(String hhmm) { try { String[] p = hhmm.split(":"); Calendar target = Calendar.getInstance(); target.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0])); target.set(Calendar.MINUTE, Integer.parseInt(p[1])); target.set(Calendar.SECOND, 0); target.set(Calendar.MILLISECOND, 0); if (target.before(Calendar.getInstance())) target.add(Calendar.DAY_OF_MONTH, 1); long min = Math.max(0, (target.getTimeInMillis() - System.currentTimeMillis()) / 60000); return (min / 60) + "h " + (min % 60) + "m"; } catch (Exception e) { return "—"; } }
    private void swapStations() { if (fromSpinner == null || toSpinner == null) return; int a = fromSpinner.getSelectedItemPosition(); int b = toSpinner.getSelectedItemPosition(); fromSpinner.setSelection(b); toSpinner.setSelection(a); }
    private int lineColor(String name) { if (name.startsWith("Purple")) return PURPLE; if (name.startsWith("Green")) return GREEN; return YELLOW; }
    private LinearLayout vertical() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout horizontal() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout card() { LinearLayout v = vertical(); v.setPadding(dp(16), dp(15), dp(16), dp(15)); v.setBackground(round(Color.WHITE, 20)); v.setElevation(dp(1)); return v; }
    private TextView label(String s) { TextView t = text(s, 10, MUTED); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setLetterSpacing(.07f); return t; }
    private TextView action(String s, boolean primary) { TextView t = text(s, 12, TEXT); t.setGravity(Gravity.CENTER); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setBackground(round(primary ? TEAL : Color.WHITE, 14)); return t; }
    private TextView tile(String head, String sub, int accent, int target) { TextView t = text(head + "\n" + sub, 14, TEXT); t.setGravity(Gravity.CENTER); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setBackground(round(Color.WHITE, 18)); t.setPadding(dp(8), dp(19), dp(8), dp(19)); t.setOnClickListener(v -> showPage(target)); return t; }
    private TextView text(String s, float size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(0, dp(4), 0, dp(4)); return t; }
    private LinearLayout.LayoutParams weight(float w) { return new LinearLayout.LayoutParams(0, -2, w); }
    private LinearLayout.LayoutParams fill(int h) { return new LinearLayout.LayoutParams(-1, dp(h)); }
    private LinearLayout.LayoutParams square(int s) { return new LinearLayout.LayoutParams(dp(s), dp(s)); }
    private void space(LinearLayout parent, int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); parent.addView(v); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private android.graphics.drawable.GradientDrawable round(int color, int radius) { android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }

    private class RouteView extends View {
        private final String line;
        private final int from;
        private final int to;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RouteView(Context context, String line, int from, int to) { super(context); this.line = line; this.from = Math.min(from, to); this.to = Math.max(from, to); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(dp(7));
            paint.setColor(lineColor(line));
            canvas.drawLine(dp(28), getHeight() / 2f, getWidth() - dp(28), getHeight() / 2f, paint);
            int count = Math.max(1, to - from);
            for (int i = 0; i <= count; i++) {
                float x = dp(28) + (getWidth() - dp(56)) * (i / (float) count);
                paint.setColor(i == 0 || i == count ? TEXT : Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(x, getHeight() / 2f, dp(7), paint);
                paint.setColor(lineColor(line));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                canvas.drawCircle(x, getHeight() / 2f, dp(7), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            paint.setTextSize(dp(12));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            List<String> list = stations.get(line);
            if (list != null && !list.isEmpty()) {
                canvas.drawText(list.get(Math.min(from, list.size() - 1)), dp(18), dp(28), paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(list.get(Math.min(to, list.size() - 1)), getWidth() - dp(18), dp(28), paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }
        }
    }

    private class MetroMap extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String focusLine;
        private int focusFrom = -1;
        private int focusTo = -1;
        MetroMap(Context context, String line) { super(context); focusLine = line; paint.setStrokeCap(Paint.Cap.ROUND); }
        void setJourney(String line, int from, int to) { focusLine = line; focusFrom = Math.min(from, to); focusTo = Math.max(from, to); invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.WHITE);
            int w = getWidth();
            int h = getHeight();
            drawRoute(canvas, PURPLE, "Purple Line", .08f, .23f, .92f, .40f);
            drawRoute(canvas, GREEN, "Green Line", .20f, .82f, .70f, .18f);
            drawRoute(canvas, YELLOW, "Yellow Line", .46f, .88f, .86f, .96f);
            drawHub(canvas, w * .52f, h * .47f, "Majestic");
            drawHub(canvas, w * .42f, h * .74f, "RV Road");
            drawHub(canvas, w * .70f, h * .25f, "KR Pura");
            drawTrains(canvas, w, h);
            if (focusFrom >= 0) drawJourneyHighlight(canvas, w, h);
        }
        private void drawRoute(Canvas canvas, int color, String name, float x1, float y1, float x2, float y2) {
            Path path = new Path();
            float sx = getWidth() * x1;
            float sy = getHeight() * y1;
            float ex = getWidth() * x2;
            float ey = getHeight() * y2;
            float mid = (sx + ex) / 2f;
            path.moveTo(sx, sy);
            path.cubicTo(mid, sy, mid, ey, ex, ey);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(color);
            paint.setStrokeWidth(dp(name.equals(focusLine) ? 9 : 6));
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 16; i++) {
                float t = i / 15f;
                float x = sx + (ex - sx) * t;
                float y = sy + (ey - sy) * t;
                paint.setColor(Color.WHITE);
                canvas.drawCircle(x, y, dp(4), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(color);
                paint.setStrokeWidth(dp(2));
                canvas.drawCircle(x, y, dp(4), paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        private void drawHub(Canvas canvas, float x, float y, String label) {
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, dp(9), paint);
            paint.setColor(TEXT);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            canvas.drawCircle(x, y, dp(9), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(10));
            paint.setColor(MUTED);
            canvas.drawText(label, x - dp(22), y - dp(14), paint);
        }
        private void drawTrains(Canvas canvas, int w, int h) {
            float motion = (System.currentTimeMillis() % 240000L) / 240000f;
            float[] starts = {0.08f, 0.36f, 0.67f};
            for (float start : starts) {
                float t = (start + motion * .65f) % 1f;
                drawTrain(canvas, w * (.12f + .76f * t), h * (.25f + .18f * t), PURPLE);
                drawTrain(canvas, w * (.26f + .52f * t), h * (.75f - .54f * t), GREEN);
                drawTrain(canvas, w * (.46f + .44f * t), h * (.84f + .08f * t), YELLOW);
            }
        }
        private void drawTrain(Canvas canvas, float x, float y, int color) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, dp(8), paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, dp(3), paint);
        }
        private void drawJourneyHighlight(Canvas canvas, int w, int h) {
            if (focusLine == null) return;
            int color = lineColor(focusLine);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(14));
            float left = w * .18f;
            float right = w * .82f;
            float y = h * .52f;
            canvas.drawLine(left, y, right, y, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            canvas.drawCircle(left, y, dp(10), paint);
            canvas.drawCircle(right, y, dp(10), paint);
        }
    }
}
