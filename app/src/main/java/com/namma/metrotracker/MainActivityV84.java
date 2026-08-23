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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivityV84 extends Activity {
    private static final int BG = Color.rgb(247, 249, 251);
    private static final int TEXT = Color.rgb(28, 36, 44);
    private static final int MUTED = Color.rgb(103, 114, 124);
    private static final int WHITE = Color.WHITE;
    private static final int TEAL = Color.rgb(47, 242, 202);
    private static final int GOLD = Color.rgb(242, 192, 114);
    private static final int INDIGO = Color.rgb(120, 132, 217);
    private static final int PURPLE = Color.rgb(126, 70, 168);
    private static final int GREEN = Color.rgb(38, 155, 92);
    private static final int YELLOW = Color.rgb(232, 179, 41);
    private static final int MUTED_LINE = Color.rgb(214, 218, 224);

    private final Map<String, List<String>> stations = new LinkedHashMap<>();
    private final Map<String, Integer> colors = new LinkedHashMap<>();
    private Spinner lineSpinner;
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private LinearLayout content;
    private LinearLayout nav;
    private TextView title;
    private TextView subtitle;
    private MetroMapView map;
    private int page = 0;
    private int from = 0;
    private int to = 1;
    private final Handler handler = new Handler();
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (map != null) map.invalidate();
            updateLiveInfo();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadNetwork();
        buildShell();
        showHome();
        handler.post(tick);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    private void loadNetwork() {
        stations.put("Purple Line", Arrays.asList("Whitefield","Hopefarm","Kadugodi Tree Park","Pattandur Agrahara","Sri Sathya Sai Hospital","Nallurhalli","Kundalahalli","Seetharampalya","Hoodi","Garudacharpalya","Singayyanapalya","K.R. Pura","Benniganahalli","Baiyappanahalli","Swami Vivekananda Road","Indiranagar","Halasuru","Trinity","MG Road","Cubbon Park","Vidhana Soudha","Central College","Majestic","City Railway Station","Magadi Road","Hosahalli","Vijayanagara","Attiguppe","Deepanjali Nagar","Mysuru Road","Nayandahalli","RR Nagar","Jnanabharathi","Pattanagere","Kengeri Bus Terminal","Kengeri","Challaghatta"));
        stations.put("Green Line", Arrays.asList("Madavara","Chikkabidarakallu","Manjunathanagara","Nagasandra","Dasarahalli","Jalahalli","Peenya Industry","Peenya","Goraguntepalya","Yeshwanthpur","Sandal Soap Factory","Mahalakshmi","Rajajinagar","Kuvempu Road","Srirampura","Sampige Road","Majestic","Chickpete","KR Market","National College","Lalbagh","South End Circle","Jayanagara","RV Road","Banashankari","JP Nagar","Yelachenahalli","Konanakunte Cross","Doddakallasandra","Vajarahalli","Thalaghattapura","Silk Institute"));
        stations.put("Yellow Line", Arrays.asList("RV Road","Ragigudda","Jayadeva Hospital","BTM Layout","Central Silk Board","Bommanahalli","Hongasandra","Kudlu Gate","Singasandra","Hosa Road","Beratena Agrahara","Electronic City","Infosys Agrahara","Huskur Road","Hebbagodi","Bommasandra"));
        colors.put("Purple Line", PURPLE);
        colors.put("Green Line", GREEN);
        colors.put("Yellow Line", YELLOW);
    }

    private void buildShell() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column();
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = column();
        header.setPadding(dp(18), dp(18), dp(18), dp(12));
        header.setBackgroundColor(WHITE);
        LinearLayout brand = row();
        TextView logo = text("N", 23, TEXT);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setBackground(round(TEAL, 15));
        brand.addView(logo, square(50));
        LinearLayout brandText = column();
        TextView app = text("Namma Metro Tracker", 21, TEXT);
        app.setTypeface(Typeface.DEFAULT_BOLD);
        brandText.addView(app);
        brandText.addView(text("Bengaluru", 11, MUTED));
        brand.addView(brandText, weight(1));
        TextView live = text("● LIVE", 11, TEXT);
        live.setGravity(Gravity.CENTER);
        live.setTypeface(Typeface.DEFAULT_BOLD);
        live.setBackground(round(TEAL, 99));
        brand.addView(live, fixed(82, 34));
        header.addView(brand);
        spacer(header, 12);
        title = text("Home", 27, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        subtitle = text("Plan your trip", 14, MUTED);
        header.addView(title);
        header.addView(subtitle);
        root.addView(header);

        content = column();
        content.setPadding(dp(16), dp(12), dp(16), dp(82));
        root.addView(content);
        nav = row();
        nav.setPadding(dp(8), dp(7), dp(8), dp(7));
        nav.setBackgroundColor(WHITE);
        root.addView(nav);
        rebuildNav();
    }

    private void rebuildNav() {
        nav.removeAllViews();
        String[] names = {"Home", "Journey", "Map", "Live"};
        for (int i = 0; i < names.length; i++) {
            final int target = i;
            TextView item = text(names[i], 11, target == page ? TEXT : MUTED);
            item.setGravity(Gravity.CENTER);
            item.setTypeface(Typeface.DEFAULT_BOLD);
            item.setBackground(round(target == page ? Color.rgb(231, 253, 247) : WHITE, 16));
            item.setOnClickListener(v -> showPage(target));
            nav.addView(item, weight(1));
        }
    }

    private void showPage(int target) {
        page = target;
        content.removeAllViews();
        if (target == 0) showHome();
        if (target == 1) showJourney();
        if (target == 2) showMap(false);
        if (target == 3) showLive();
        updateHeader();
        rebuildNav();
    }

    private void updateHeader() {
        String[] titles = {"Home", "Your Journey", "Metro Map", "Live Trains"};
        String[] subs = {"Plan your trip", "Fare, route and train timing", "Your selected line is highlighted", "Moving trains across Bengaluru"};
        title.setText(titles[page]);
        subtitle.setText(subs[page]);
    }

    private void showHome() {
        LinearLayout hero = card();
        hero.setBackground(round(INDIGO, 22));
        TextView h = text("Where are you going?", 25, WHITE);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        hero.addView(h);
        hero.addView(text("Pick a line and two stations.", 14, WHITE));
        spacer(hero, 10);
        TextView status = text("● SERVICE RUNNING", 11, TEXT);
        status.setGravity(Gravity.CENTER);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setBackground(round(TEAL, 99));
        hero.addView(status, fill(36));
        content.addView(hero);
        spacer(content, 12);

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
        show.setOnClickListener(v -> openJourney());
        content.addView(planner);
        setupSpinners();
        spacer(content, 12);
        LinearLayout quick = row();
        quick.addView(tile("LIVE TRAINS", "Moving now", 3), weight(1));
        quick.addView(tile("METRO MAP", "Selected line", 2), weight(1));
        content.addView(quick);
    }

    private TextView tile(String top, String bottom, int target) {
        TextView t = text(top + "\n" + bottom, 14, TEXT);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setBackground(round(WHITE, 18));
        t.setPadding(dp(8), dp(19), dp(8), dp(19));
        t.setOnClickListener(v -> showPage(target));
        return t;
    }

    private void setupSpinners() {
        lineSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, stations.keySet().toArray(new String[0])));
        lineSpinner.setSelection(0);
        lineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) { refreshStations(); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        refreshStations();
    }

    private void refreshStations() {
        List<String> list = stations.get(selectedLine());
        if (list == null) return;
        fromSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list));
        toSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list));
        fromSpinner.setSelection(Math.min(from, list.size() - 1));
        toSpinner.setSelection(Math.min(Math.max(to, 1), list.size() - 1));
    }

    private void openJourney() {
        from = fromIndex();
        to = toIndex();
        if (from == to) to = Math.min(from + 1, stations.get(selectedLine()).size() - 1);
        if (from > to) { int swap = from; from = to; to = swap; }
        page = 1;
        content.removeAllViews();
        showJourney();
        updateHeader();
        rebuildNav();
    }

    private void showJourney() {
        String line = selectedLine();
        LinearLayout summary = card();
        summary.addView(label("YOUR JOURNEY"));
        summary.addView(text(routeText(), 22, TEXT));
        summary.addView(text(line, 13, colors.get(line)));
        summary.addView(text(stopCount() + " stops  •  " + durationText(), 14, MUTED));
        content.addView(summary);

        spacer(content, 10);
        LinearLayout stats = row();
        stats.addView(stat("FARE", "₹" + fare()), weight(1));
        stats.addView(stat("TIME", durationText()), weight(1));
        stats.addView(stat("NEXT", nextTrain()), weight(1));
        content.addView(stats);

        spacer(content, 12);
        LinearLayout train = card();
        train.addView(label("NEXT TRAIN"));
        TextView countdown = text(nextCountdown(), 24, TEXT);
        countdown.setTypeface(Typeface.DEFAULT_BOLD);
        train.addView(countdown);
        train.addView(text("Expected at " + stations.get(line).get(from) + "  •  " + line, 13, MUTED));
        content.addView(train);

        spacer(content, 12);
        LinearLayout route = card();
        route.addView(label("ROUTE"));
        route.addView(new RouteView(this, line, from, to), fill(150));
        content.addView(route);

        spacer(content, 12);
        TextView watch = action("WATCH THIS ROUTE LIVE", true);
        content.addView(watch, fill(54));
        watch.setOnClickListener(v -> { page = 3; content.removeAllViews(); showLive(); updateHeader(); rebuildNav(); });

        spacer(content, 12);
        // The map is already focused automatically inside the journey page.
        map = new MetroMapView(this, line, false, from, to);
        content.addView(map, fill(470));
    }

    private void showMap(boolean allLines) {
        String line = selectedLine();
        LinearLayout info = card();
        info.addView(label("MAP VIEW"));
        info.addView(text(line + " highlighted", 20, colors.get(line)));
        info.addView(text("Other lines are muted so your selected route is easy to follow.", 13, MUTED));
        content.addView(info);
        spacer(content, 10);
        map = new MetroMapView(this, line, allLines, from, to);
        content.addView(map, fill(620));
    }

    private void showLive() {
        LinearLayout header = card();
        header.setBackground(round(Color.rgb(241, 242, 253), 22));
        TextView h = text("Live network", 23, TEXT);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(h);
        header.addView(text("Train markers move continuously and station status updates every second.", 14, MUTED));
        content.addView(header);
        spacer(content, 10);
        map = new MetroMapView(this, selectedLine(), true, from, to);
        content.addView(map, fill(560));
        spacer(content, 12);
        content.addView(liveCard("Purple Line", 0));
        spacer(content, 8);
        content.addView(liveCard("Green Line", 1));
        spacer(content, 8);
        content.addView(liveCard("Yellow Line", 2));
    }

    private LinearLayout liveCard(String line, int offset) {
        LinearLayout card = card();
        int color = colors.get(line);
        TextView head = text(line, 16, color);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(head);
        String[] state = simulatedTrain(line, offset);
        card.addView(text(state[0] + " → " + state[1], 17, TEXT));
        card.addView(text("Next station in " + state[2] + "  •  " + state[3], 13, MUTED));
        TextView pulse = text("● MOVING", 11, color);
        pulse.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(pulse);
        return card;
    }

    private void updateLiveInfo() {
        if (page != 3 || content == null || content.getChildCount() < 6) return;
        int base = content.getChildCount() - 5;
        content.removeViews(base, 5);
        content.addView(liveCard("Purple Line", 0));
        spacer(content, 8);
        content.addView(liveCard("Green Line", 1));
        spacer(content, 8);
        content.addView(liveCard("Yellow Line", 2));
    }

    private String[] simulatedTrain(String line, int offset) {
        List<String> list = stations.get(line);
        double cycle = 150.0;
        double progress = ((System.currentTimeMillis() / 1000.0) / cycle + offset * 0.29) % 1.0;
        int station = Math.min(list.size() - 2, (int) Math.floor(progress * (list.size() - 1)));
        double inside = progress * (list.size() - 1) - station;
        int seconds = Math.max(5, (int) Math.round((1.0 - inside) * 60.0));
        String direction = station < list.size() / 2 ? "towards outer terminal" : "towards city";
        return new String[]{list.get(station), list.get(station + 1), String.format(Locale.US, "%02ds", seconds), direction};
    }

    private TextView stat(String name, String value) {
        TextView t = text(name + "\n" + value, 13, TEXT);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setBackground(round(WHITE, 18));
        t.setPadding(dp(7), dp(15), dp(7), dp(15));
        return t;
    }

    private TextView action(String textValue, boolean primary) {
        TextView t = text(textValue, 12, TEXT);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setBackground(round(primary ? TEAL : WHITE, 14));
        return t;
    }

    private TextView label(String textValue) {
        TextView t = text(textValue, 10, MUTED);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout v = column();
        v.setPadding(dp(16), dp(15), dp(16), dp(15));
        v.setBackground(round(WHITE, 20));
        v.setElevation(dp(1));
        return v;
    }

    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private TextView text(String s, float size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(0, dp(4), 0, dp(4)); return t; }
    private LinearLayout.LayoutParams weight(float value) { return new LinearLayout.LayoutParams(0, -2, value); }
    private LinearLayout.LayoutParams fill(int height) { return new LinearLayout.LayoutParams(-1, dp(height)); }
    private LinearLayout.LayoutParams fixed(int width, int height) { return new LinearLayout.LayoutParams(dp(width), dp(height)); }
    private LinearLayout.LayoutParams square(int size) { return fixed(size, size); }
    private void spacer(LinearLayout parent, int height) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); parent.addView(v); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private android.graphics.drawable.GradientDrawable round(int color, int radius) { android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }

    private String selectedLine() { return lineSpinner == null || lineSpinner.getSelectedItem() == null ? "Purple Line" : String.valueOf(lineSpinner.getSelectedItem()); }
    private int fromIndex() { return fromSpinner == null || fromSpinner.getSelectedItem() == null ? from : stations.get(selectedLine()).indexOf(String.valueOf(fromSpinner.getSelectedItem())); }
    private int toIndex() { return toSpinner == null || toSpinner.getSelectedItem() == null ? to : stations.get(selectedLine()).indexOf(String.valueOf(toSpinner.getSelectedItem())); }
    private int stopCount() { return Math.abs(to - from); }
    private String routeText() { List<String> list = stations.get(selectedLine()); return list.get(from) + " → " + list.get(to); }
    private String durationText() { return "~" + Math.max(2, Math.round(stopCount() * 2.4f)) + " min"; }
    private int fare() { double km = Math.max(0.5, stopCount() * 1.45); int[][] slabs = {{2,11},{4,21},{6,32},{8,42},{10,53},{15,63},{20,74},{25,84},{30,90},{999,95}}; for (int[] slab : slabs) if (km <= slab[0]) return slab[1]; return 95; }
    private String nextTrain() { return selectedLine().startsWith("Yellow") ? "~7 min" : "~8 min"; }
    private String nextCountdown() { return "Arriving in " + (10 + (int)((System.currentTimeMillis() / 1000) % 35)) + " sec"; }

    private class RouteView extends View {
        private final String line;
        private final int a;
        private final int b;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RouteView(Context context, String line, int from, int to) { super(context); this.line = line; a = Math.min(from, to); b = Math.max(from, to); }
        @Override protected void onDraw(Canvas canvas) {
            int color = colors.get(line);
            float y = getHeight() * .58f;
            float left = dp(28);
            float right = getWidth() - dp(28);
            paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(7)); paint.setStrokeCap(Paint.Cap.ROUND); canvas.drawLine(left, y, right, y, paint);
            int count = Math.max(1, b - a);
            for (int i = 0; i <= count; i++) {
                float x = left + (right - left) * i / (float) count;
                paint.setStyle(Paint.Style.FILL); paint.setColor(i == 0 || i == count ? TEXT : WHITE); canvas.drawCircle(x, y, dp(7), paint);
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(color); canvas.drawCircle(x, y, dp(7), paint);
            }
            paint.setStyle(Paint.Style.FILL); paint.setColor(TEXT); paint.setTextSize(dp(12)); paint.setTypeface(Typeface.DEFAULT_BOLD);
            List<String> list = stations.get(line);
            canvas.drawText(list.get(a), left, dp(28), paint);
            paint.setTextAlign(Paint.Align.RIGHT); canvas.drawText(list.get(b), right, dp(28), paint); paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private class MetroMapView extends View {
        private final String selected;
        private final boolean showAll;
        private final int journeyFrom;
        private final int journeyTo;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        MetroMapView(Context context, String selected, boolean showAll, int from, int to) { super(context); this.selected = selected; this.showAll = showAll; journeyFrom = from; journeyTo = to; paint.setStrokeCap(Paint.Cap.ROUND); }
        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(WHITE);
            drawLine(canvas, "Purple Line", purplePath(), 0);
            drawLine(canvas, "Green Line", greenPath(), 1);
            drawLine(canvas, "Yellow Line", yellowPath(), 2);
            drawInterchanges(canvas);
            drawMovingTrains(canvas);
            if (!showAll) drawJourneyLabel(canvas);
        }
        private void drawLine(Canvas c, String line, float[][] points, int id) {
            int color = line.equals(selected) || showAll ? colors.get(line) : MUTED_LINE;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(line.equals(selected) ? 8 : 4));
            paint.setColor(color);
            Path p = new Path();
            p.moveTo(points[0][0] * getWidth(), points[0][1] * getHeight());
            for (int i = 1; i < points.length; i++) p.lineTo(points[i][0] * getWidth(), points[i][1] * getHeight());
            c.drawPath(p, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < points.length; i++) {
                float x = points[i][0] * getWidth(); float y = points[i][1] * getHeight();
                paint.setColor(WHITE); c.drawCircle(x, y, dp(4), paint);
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(color); c.drawCircle(x, y, dp(4), paint); paint.setStyle(Paint.Style.FILL);
                if (line.equals(selected)) drawStationLabel(c, stationForAnchor(line, i), x, y);
            }
        }
        private void drawStationLabel(Canvas c, String label, float x, float y) { paint.setColor(MUTED); paint.setTextSize(dp(8)); paint.setTypeface(Typeface.DEFAULT); if (label != null) c.drawText(label, x + dp(6), y - dp(5), paint); }
        private String stationForAnchor(String line, int anchor) { List<String> list = stations.get(line); if (list == null) return null; int index = Math.round(anchor * (list.size() - 1f) / 8f); return list.get(Math.min(list.size() - 1, index)); }
        private void drawInterchanges(Canvas c) { drawHub(c,.45f,.56f,"Majestic"); drawHub(c,.54f,.72f,"RV Road"); drawHub(c,.63f,.43f,"K.R. Pura"); drawHub(c,.58f,.79f,"Central Silk Board"); }
        private void drawHub(Canvas c,float x,float y,String label){ paint.setStyle(Paint.Style.FILL); paint.setColor(WHITE); c.drawCircle(x*getWidth(),y*getHeight(),dp(8),paint); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(3)); paint.setColor(TEXT); c.drawCircle(x*getWidth(),y*getHeight(),dp(8),paint); paint.setStyle(Paint.Style.FILL); paint.setColor(TEXT); paint.setTextSize(dp(9)); paint.setTypeface(Typeface.DEFAULT_BOLD); c.drawText(label,x*getWidth()+dp(10),y*getHeight()+dp(3),paint); }
        private void drawMovingTrains(Canvas c) { String[] lines={"Purple Line","Green Line","Yellow Line"}; for(int li=0;li<lines.length;li++){ String line=lines[li]; if(!showAll&&!line.equals(selected)) continue; float phase=(float)(((System.currentTimeMillis()/1000.0)/150.0+li*.29)%1.0); for(int train=0;train<3;train++){float t=(phase+train*.31f)%1f; float[] xy=pointAt(line,t); paint.setStyle(Paint.Style.FILL); paint.setColor(colors.get(line)); c.drawCircle(xy[0]*getWidth(),xy[1]*getHeight(),dp(9),paint); paint.setColor(WHITE); c.drawCircle(xy[0]*getWidth(),xy[1]*getHeight(),dp(3),paint); }} }
        private float[] pointAt(String line,float t){ float[][] p=line.equals("Green Line")?greenPath():line.equals("Yellow Line")?yellowPath():purplePath(); float scaled=t*(p.length-1); int i=Math.min(p.length-2,Math.max(0,(int)scaled)); float f=scaled-i; return new float[]{p[i][0]+(p[i+1][0]-p[i][0])*f,p[i][1]+(p[i+1][1]-p[i][1])*f}; }
        private void drawJourneyLabel(Canvas c){ paint.setStyle(Paint.Style.FILL); paint.setColor(TEXT); paint.setTextSize(dp(11)); paint.setTypeface(Typeface.DEFAULT_BOLD); String s=stations.get(selected).get(Math.min(journeyFrom,stations.get(selected).size()-1))+" → "+stations.get(selected).get(Math.min(journeyTo,stations.get(selected).size()-1)); c.drawText(s,dp(16),getHeight()-dp(18),paint); }
        private float[][] purplePath(){ return new float[][]{{.90f,.36f},{.80f,.37f},{.70f,.40f},{.62f,.44f},{.54f,.49f},{.45f,.56f},{.36f,.61f},{.25f,.66f},{.12f,.75f}}; }
        private float[][] greenPath(){ return new float[][]{{.22f,.10f},{.27f,.18f},{.32f,.27f},{.38f,.37f},{.45f,.56f},{.49f,.64f},{.54f,.72f},{.57f,.82f},{.60f,.92f}}; }
        private float[][] yellowPath(){ return new float[][]{{.54f,.72f},{.57f,.76f},{.61f,.80f},{.66f,.84f},{.71f,.87f},{.76f,.90f},{.81f,.93f}}; }
    }
}
