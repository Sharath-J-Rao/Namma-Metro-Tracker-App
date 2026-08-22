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
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivityV83 extends Activity {
    private static final int BG = Color.rgb(247, 249, 251);
    private static final int TEXT = Color.rgb(28, 36, 44);
    private static final int MUTED = Color.rgb(103, 114, 124);
    private static final int WHITE = Color.WHITE;
    private static final int TEAL = Color.rgb(47, 242, 202);
    private static final int GOLD = Color.rgb(242, 192, 114);
    private static final int INDIGO = Color.rgb(120, 132, 217);
    private static final int CYAN = Color.rgb(20, 167, 207);
    private static final int PURPLE = Color.rgb(126, 70, 168);
    private static final int GREEN = Color.rgb(38, 155, 92);
    private static final int YELLOW = Color.rgb(232, 179, 41);
    private static final int NETWORK_GRAY = Color.rgb(205, 210, 216);
    private static final int LABEL_GRAY = Color.rgb(112, 119, 128);

    private final Map<String, List<String>> stations = new LinkedHashMap<>();
    private final Map<String, Integer> lineColors = new LinkedHashMap<>();
    private Spinner lineSpinner;
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private LinearLayout content;
    private LinearLayout nav;
    private TextView title;
    private TextView subtitle;
    private MetroMap map;
    private int page = 0;
    private int selectedFrom = 0;
    private int selectedTo = 1;
    private final Handler handler = new Handler();
    private final Runnable liveTicker = new Runnable() {
        @Override public void run() {
            if (map != null) map.invalidate();
            refreshLiveCards();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadNetwork();
        buildShell();
        showHome();
        handler.post(liveTicker);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(liveTicker);
        super.onDestroy();
    }

    private void loadNetwork() {
        stations.clear();
        stations.put("Purple Line", Arrays.asList(
                "Whitefield", "Hopefarm", "Kadugodi Tree Park", "Pattandur Agrahara", "Sri Sathya Sai Hospital",
                "Nallurhalli", "Kundalahalli", "Seetharampalya", "Hoodi", "Garudacharpalya", "Singayyanapalya",
                "K.R. Pura", "Benniganahalli", "Baiyappanahalli", "Swami Vivekananda Road", "Indiranagar",
                "Halasuru", "Trinity", "MG Road", "Cubbon Park", "Vidhana Soudha", "Central College",
                "Majestic", "City Railway Station", "Magadi Road", "Hosahalli", "Vijayanagara", "Attiguppe",
                "Deepanjali Nagar", "Mysuru Road", "Nayandahalli", "RR Nagar", "Jnanabharathi", "Pattanagere",
                "Kengeri Bus Terminal", "Kengeri", "Challaghatta"));
        stations.put("Green Line", Arrays.asList(
                "Madavara", "Chikkabidarakallu", "Manjunathanagara", "Nagasandra", "Dasarahalli", "Jalahalli",
                "Peenya Industry", "Peenya", "Goraguntepalya", "Yeshwanthpur", "Sandal Soap Factory", "Mahalakshmi",
                "Rajajinagar", "Kuvempu Road", "Srirampura", "Sampige Road", "Majestic", "Chickpete", "KR Market",
                "National College", "Lalbagh", "South End Circle", "Jayanagara", "RV Road", "Banashankari", "JP Nagar",
                "Yelachenahalli", "Konanakunte Cross", "Doddakallasandra", "Vajarahalli", "Thalaghattapura", "Silk Institute"));
        stations.put("Yellow Line", Arrays.asList(
                "RV Road", "Ragigudda", "Jayadeva Hospital", "BTM Layout", "Central Silk Board", "Bommanahalli",
                "Hongasandra", "Kudlu Gate", "Singasandra", "Hosa Road", "Beratena Agrahara", "Electronic City",
                "Infosys Agrahara", "Huskur Road", "Hebbagodi", "Bommasandra"));

        lineColors.clear();
        lineColors.put("Purple Line", PURPLE);
        lineColors.put("Green Line", GREEN);
        lineColors.put("Yellow Line", YELLOW);
    }

    private void buildShell() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = vertical();
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = vertical();
        header.setPadding(dp(18), dp(18), dp(18), dp(12));
        header.setBackgroundColor(WHITE);

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

        TextView liveBadge = text("●  LIVE", 11, TEXT);
        liveBadge.setGravity(Gravity.CENTER);
        liveBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        liveBadge.setBackground(round(TEAL, 99));
        brand.addView(liveBadge, fixedWidth(86, 36));
        header.addView(brand);
        space(header, 12);

        title = text("Home", 27, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        subtitle = text("Plan your trip", 14, MUTED);
        header.addView(title);
        header.addView(subtitle);
        root.addView(header);

        content = vertical();
        content.setPadding(dp(16), dp(12), dp(16), dp(82));
        root.addView(content);

        nav = horizontal();
        nav.setPadding(dp(8), dp(7), dp(8), dp(7));
        nav.setBackgroundColor(WHITE);
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
        if (target == 2) showMapPage(true);
        if (target == 3) showLive();
        updateHeader();
        rebuildNav();
    }

    private void updateHeader() {
        String[] titles = {"Home", "Your Journey", "Metro Map", "Live Trains"};
        String[] subtitles = {"Plan your trip", "Fare, route and train timing", "Bengaluru network", "Moving trains and next stations"};
        title.setText(titles[page]);
        subtitle.setText(subtitles[page]);
    }

    private void showHome() {
        LinearLayout hero = card();
        hero.setBackground(round(INDIGO, 22));
        TextView h = text("Where are you going?", 25, WHITE);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.addView(h);
        hero.addView(text("Choose a line, then pick your stations.", 14, WHITE));
        space(hero, 10);
        TextView status = text("●  SERVICE RUNNING", 11, TEXT);
        status.setGravity(Gravity.CENTER);
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        status.setBackground(round(TEAL, 99));
        hero.addView(status, fill(36));
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
        show.setOnClickListener(v -> {
            selectedFrom = fromIndex();
            selectedTo = toIndex();
            page = 1;
            showJourney();
            updateHeader();
            rebuildNav();
        });
        TextView swap = action("SWAP", false);
        planner.addView(swap, fill(42));
        swap.setOnClickListener(v -> swapStations());
        content.addView(planner);
        setupSpinners();

        space(content, 12);
        LinearLayout quick = horizontal();
        quick.addView(tile("LIVE TRAINS", "Moving now", INDIGO, 3), weight(1));
        quick.addView(tile("METRO MAP", "Full network", CYAN, 2), weight(1));
        content.addView(quick);
    }

    private void setupSpinners() {
        List<String> lineList = new ArrayList<>(stations.keySet());
        lineSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, lineList));
        lineSpinner.setSelection(0);
        lineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshStations();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        refreshStations();
    }

    private void refreshStations() {
        if (lineSpinner == null || fromSpinner == null || toSpinner == null) return;
        List<String> list = stations.get(selectedLine());
        if (list == null || list.isEmpty()) return;
        fromSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list));
        toSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list));
        fromSpinner.setSelection(Math.min(selectedFrom, list.size() - 1));
        toSpinner.setSelection(Math.min(Math.max(selectedTo, 1), list.size() - 1));
    }

    private void showJourney() {
        int from = Math.min(selectedFrom, selectedTo);
        int to = Math.max(selectedFrom, selectedTo);
        selectedFrom = from;
        selectedTo = to;

        LinearLayout summary = card();
        summary.setBackground(round(WHITE, 22));
        summary.addView(label("YOUR JOURNEY"));
        summary.addView(text(routeText(), 22, TEXT));
        summary.addView(text(selectedLine(), 13, lineColors.get(selectedLine())));
        summary.addView(text(stopCount() + " stops  •  " + durationText(), 14, MUTED));
        content.addView(summary);

        space(content, 10);
        LinearLayout stats = horizontal();
        stats.addView(stat("FARE", "₹" + fare(), GOLD), weight(1));
        stats.addView(stat("TIME", durationText(), TEAL), weight(1));
        stats.addView(stat("NEXT", nextTrain(), INDIGO), weight(1));
        content.addView(stats);

        space(content, 12);
        LinearLayout train = card();
        train.addView(label("NEXT TRAIN FOR YOUR ROUTE"));
        TextView next = text(nextTrainCountdown(), 24, TEXT);
        next.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        train.addView(next);
        train.addView(text("Expected at " + nextStationName() + "  •  " + selectedLine(), 13, MUTED));
        content.addView(train);

        space(content, 12);
        LinearLayout route = card();
        route.addView(label("ROUTE"));
        route.addView(new RouteView(this, selectedLine(), selectedFrom, selectedTo), fill(150));
        content.addView(route);

        // No secondary "show journey on map" action. The route is focused automatically.
        space(content, 12);
        TextView live = action("WATCH THIS ROUTE LIVE", true);
        content.addView(live, fill(54));
        live.setOnClickListener(v -> showLiveForSelectedLine());
    }

    private void showLiveForSelectedLine() {
        page = 3;
        content.removeAllViews();
        showLive();
        updateHeader();
        rebuildNav();
    }

    private void showMapPage(boolean focusJourney) {
        map = new MetroMap(this, selectedLine());
        if (focusJourney) map.setJourney(selectedLine(), selectedFrom, selectedTo);
        LinearLayout mapCard = card();
        mapCard.setPadding(dp(4), dp(4), dp(4), dp(4));
        mapCard.addView(map, fill(610));
        content.addView(mapCard);

        space(content, 10);
        TextView focusText = text("Showing " + selectedLine() + " route", 13, selectedLineColor());
        focusText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(focusText);
        content.addView(text("Other lines stay in the background so your route is easy to follow.", 12, MUTED));
    }

    private void showLive() {
        LinearLayout intro = card();
        intro.setBackground(round(Color.rgb(241, 242, 253), 22));
        TextView h = text(selectedLine() + " trains", 23, TEXT);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        intro.addView(h);
        intro.addView(text("Live estimate updates every second.", 14, MUTED));
        content.addView(intro);

        space(content, 10);
        map = new MetroMap(this, selectedLine());
        content.addView(map, fill(510));

        space(content, 12);
        content.addView(liveTrainCard(0));
        space(content, 8);
        content.addView(liveTrainCard(1));
        space(content, 8);
        content.addView(liveTrainCard(2));
    }

    private LinearLayout liveTrainCard(int trainNumber) {
        String line = selectedLine();
        List<String> list = stations.get(line);
        int n = list == null ? 2 : list.size();
        double phase = ((System.currentTimeMillis() / 1000.0) / 150.0 + trainNumber * 0.31) % 1.0;
        double position = phase * Math.max(1, n - 1);
        int stationIndex = Math.min(n - 1, (int) position);
        int nextIndex = Math.min(n - 1, stationIndex + 1);
        int secondsToNext = Math.max(8, (int) Math.round((1.0 - (position - stationIndex)) * 55.0));

        LinearLayout card = card();
        TextView head = text("TRAIN " + (trainNumber + 1), 12, lineColorFor(line));
        head.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(head);
        card.addView(text(list.get(stationIndex) + "  →  " + list.get(nextIndex), 17, TEXT));
        card.addView(text("Next station in " + formatSeconds(secondsToNext) + "  •  " + directionText(stationIndex, n), 13, MUTED));
        card.addView(text("Estimated position updates continuously", 11, CYAN));
        return card;
    }

    private void refreshLiveCards() {
        if (page != 3 || content == null) return;
        // Rebuild only the live cards, leaving the map animation intact.
        if (content.getChildCount() >= 4) {
            content.removeViews(2, Math.max(0, content.getChildCount() - 2));
            content.addView(liveTrainCard(0));
            space(content, 8);
            content.addView(liveTrainCard(1));
            space(content, 8);
            content.addView(liveTrainCard(2));
        }
    }

    private TextView stat(String label, String value, int accent) {
        TextView t = text(label + "\n" + value, 13, TEXT);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setBackground(round(WHITE, 18));
        t.setPadding(dp(7), dp(15), dp(7), dp(15));
        return t;
    }

    private TextView tile(String head, String sub, int accent, int target) {
        TextView t = text(head + "\n" + sub, 14, TEXT);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setBackground(round(WHITE, 18));
        t.setPadding(dp(8), dp(19), dp(8), dp(19));
        t.setOnClickListener(v -> showPage(target));
        return t;
    }

    private TextView action(String s, boolean primary) {
        TextView t = text(s, 12, TEXT);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setBackground(round(primary ? TEAL : WHITE, 14));
        return t;
    }

    private TextView label(String s) {
        TextView t = text(s, 10, MUTED);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout v = vertical();
        v.setPadding(dp(16), dp(15), dp(16), dp(15));
        v.setBackground(round(WHITE, 20));
        v.setElevation(dp(1));
        return v;
    }

    private LinearLayout vertical() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private LinearLayout horizontal() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private TextView text(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    private LinearLayout.LayoutParams weight(float value) {
        return new LinearLayout.LayoutParams(0, -2, value);
    }

    private LinearLayout.LayoutParams fill(int height) {
        return new LinearLayout.LayoutParams(-1, dp(height));
    }

    private LinearLayout.LayoutParams fixedWidth(int width, int height) {
        return new LinearLayout.LayoutParams(dp(width), dp(height));
    }

    private LinearLayout.LayoutParams square(int size) {
        return new LinearLayout.LayoutParams(dp(size), dp(size));
    }

    private void space(LinearLayout parent, int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        parent.addView(v);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private String selectedLine() {
        return lineSpinner == null || lineSpinner.getSelectedItem() == null ? "Purple Line" : String.valueOf(lineSpinner.getSelectedItem());
    }

    private int selectedLineColor() {
        return lineColors.containsKey(selectedLine()) ? lineColors.get(selectedLine()) : PURPLE;
    }

    private int lineColorFor(String line) {
        return lineColors.containsKey(line) ? lineColors.get(line) : PURPLE;
    }

    private int fromIndex() {
        if (fromSpinner == null || fromSpinner.getSelectedItem() == null) return selectedFrom;
        return stationIndex(String.valueOf(fromSpinner.getSelectedItem()));
    }

    private int toIndex() {
        if (toSpinner == null || toSpinner.getSelectedItem() == null) return selectedTo;
        return stationIndex(String.valueOf(toSpinner.getSelectedItem()));
    }

    private int stationIndex(String station) {
        List<String> list = stations.get(selectedLine());
        int index = list == null ? -1 : list.indexOf(station);
        return Math.max(0, index);
    }

    private int stopCount() {
        return Math.abs(selectedTo - selectedFrom);
    }

    private String routeText() {
        List<String> list = stations.get(selectedLine());
        if (list == null || list.isEmpty()) return "Station → Station";
        return list.get(Math.min(selectedFrom, list.size() - 1)) + " → " + list.get(Math.min(selectedTo, list.size() - 1));
    }

    private int fare() {
        double km = Math.max(0.5, stopCount() * 1.45);
        int[][] slabs = {{2,11},{4,21},{6,32},{8,42},{10,53},{15,63},{20,74},{25,84},{30,90},{999,95}};
        for (int[] slab : slabs) if (km <= slab[0]) return slab[1];
        return 95;
    }

    private String durationText() {
        return "~" + Math.max(2, Math.round(stopCount() * 2.4f)) + " min";
    }

    private String nextTrain() {
        return selectedLine().startsWith("Yellow") ? "~7 min" : "~8 min";
    }

    private String nextTrainCountdown() {
        long seconds = 10 + (System.currentTimeMillis() / 1000) % 35;
        return "Arriving in " + seconds + " sec";
    }

    private String nextStationName() {
        List<String> list = stations.get(selectedLine());
        if (list == null || list.isEmpty()) return "your station";
        int index = Math.min(list.size() - 1, selectedFrom + 1);
        return list.get(index);
    }

    private String directionText(int index, int total) {
        return index < total / 2 ? "towards terminal" : "return direction";
    }

    private String formatSeconds(int seconds) {
        return String.format(Locale.US, "%02ds", seconds);
    }

    private void swapStations() {
        if (fromSpinner == null || toSpinner == null) return;
        int a = fromSpinner.getSelectedItemPosition();
        int b = toSpinner.getSelectedItemPosition();
        fromSpinner.setSelection(b);
        toSpinner.setSelection(a);
    }

    private void showStationToast(String name) {
        Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
    }

    private class RouteView extends View {
        private final String line;
        private final int from;
        private final int to;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        RouteView(Context context, String line, int from, int to) {
            super(context);
            this.line = line;
            this.from = Math.min(from, to);
            this.to = Math.max(from, to);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int color = lineColorFor(line);
            paint.setColor(color);
            paint.setStrokeWidth(dp(7));
            paint.setStyle(Paint.Style.STROKE);
            float y = getHeight() * 0.58f;
            float left = dp(28);
            float right = getWidth() - dp(28);
            canvas.drawLine(left, y, right, y, paint);
            int count = Math.max(1, to - from);
            for (int i = 0; i <= count; i++) {
                float x = left + (right - left) * (i / (float) count);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(i == 0 || i == count ? TEXT : WHITE);
                canvas.drawCircle(x, y, dp(7), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(color);
                paint.setStrokeWidth(dp(2));
                canvas.drawCircle(x, y, dp(7), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(12));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setColor(TEXT);
            List<String> list = stations.get(line);
            if (list != null && !list.isEmpty()) {
                canvas.drawText(list.get(Math.min(from, list.size() - 1)), left, dp(28), paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(list.get(Math.min(to, list.size() - 1)), right, dp(28), paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }
        }
    }

    private class MetroMap extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String focusLine;
        private int focusFrom = 0;
        private int focusTo = 1;
        private boolean hasJourney = false;

        MetroMap(Context context, String focusLine) {
            super(context);
            this.focusLine = focusLine;
            paint.setStrokeCap(Paint.Cap.ROUND);
            setBackgroundColor(WHITE);
        }

        void setJourney(String line, int from, int to) {
            focusLine = line;
            focusFrom = Math.min(from, to);
            focusTo = Math.max(from, to);
            hasJourney = true;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(WHITE);
            drawNetwork(canvas, "Purple Line");
            drawNetwork(canvas, "Green Line");
            drawNetwork(canvas, "Yellow Line");
            drawInterchanges(canvas);
            drawTrains(canvas);
            if (hasJourney) drawJourneyFocus(canvas);
        }

        private void drawNetwork(Canvas canvas, String line) {
            int color = line.equals(focusLine) ? lineColorFor(line) : NETWORK_GRAY;
            float[][] points = geometry(line);
            if (points.length < 2) return;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(line.equals(focusLine) ? 8 : 4));
            paint.setColor(color);
            Path path = new Path();
            path.moveTo(points[0][0] * getWidth(), points[0][1] * getHeight());
            for (int i = 1; i < points.length; i++) {
                path.lineTo(points[i][0] * getWidth(), points[i][1] * getHeight());
            }
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < points.length; i++) {
                float x = points[i][0] * getWidth();
                float y = points[i][1] * getHeight();
                paint.setColor(WHITE);
                canvas.drawCircle(x, y, dp(line.equals(focusLine) ? 5 : 4), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1.5f));
                paint.setColor(color);
                canvas.drawCircle(x, y, dp(line.equals(focusLine) ? 5 : 4), paint);
                paint.setStyle(Paint.Style.FILL);
                if (line.equals(focusLine) || isInterchangeStation(line, i)) {
                    paint.setColor(LABEL_GRAY);
                    paint.setTextSize(dp(9));
                    paint.setTypeface(Typeface.DEFAULT);
                    String station = stationName(line, i);
                    if (!station.isEmpty()) canvas.drawText(shortLabel(station), x + dp(6), y - dp(5), paint);
                }
            }
        }

        private void drawInterchanges(Canvas canvas) {
            drawInterchange(canvas, 0.46f, 0.56f, "Majestic");
            drawInterchange(canvas, 0.53f, 0.72f, "RV Road");
            drawInterchange(canvas, 0.64f, 0.43f, "K.R. Pura");
            drawInterchange(canvas, 0.58f, 0.68f, "Central Silk Board");
        }

        private void drawInterchange(Canvas canvas, float x, float y, String label) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(WHITE);
            canvas.drawCircle(x * getWidth(), y * getHeight(), dp(8), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(TEXT);
            canvas.drawCircle(x * getWidth(), y * getHeight(), dp(8), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            paint.setTextSize(dp(10));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(label, x * getWidth() + dp(10), y * getHeight() + dp(4), paint);
        }

        private void drawTrains(Canvas canvas) {
            String[] lines = {"Purple Line", "Green Line", "Yellow Line"};
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex];
                if (!line.equals(focusLine) && page != 3) continue;
                float phase = ((System.currentTimeMillis() / 1000.0) / 150.0 + lineIndex * 0.29) % 1.0;
                float[][] geometry = geometry(line);
                for (int train = 0; train < 3; train++) {
                    float t = (phase + train * 0.31f) % 1.0f;
                    float[] xy = interpolate(geometry, t);
                    drawTrain(canvas, xy[0] * getWidth(), xy[1] * getHeight(), lineColorFor(line));
                }
            }
        }

        private void drawTrain(Canvas canvas, float x, float y, int color) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawCircle(x, y, dp(9), paint);
            paint.setColor(WHITE);
            canvas.drawCircle(x, y, dp(3), paint);
        }

        private void drawJourneyFocus(Canvas canvas) {
            float[][] points = geometry(focusLine);
            if (points.length < 2) return;
            int a = Math.min(points.length - 1, focusFrom);
            int b = Math.min(points.length - 1, focusTo);
            if (a == b) b = Math.min(points.length - 1, a + 1);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(14));
            paint.setColor(lineColorFor(focusLine));
            Path path = new Path();
            path.moveTo(points[a][0] * getWidth(), points[a][1] * getHeight());
            for (int i = a + 1; i <= b; i++) path.lineTo(points[i][0] * getWidth(), points[i][1] * getHeight());
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private boolean isInterchangeStation(String line, int index) {
            String name = stationName(line, index);
            return name.equals("Majestic") || name.equals("RV Road") || name.equals("K.R. Pura") || name.equals("Central Silk Board");
        }

        private String stationName(String line, int index) {
            List<String> list = stations.get(line);
            return list == null || index < 0 || index >= list.size() ? "" : list.get(index);
        }

        private String shortLabel(String s) {
            if (s.length() <= 18) return s;
            return s.substring(0, 17) + "…";
        }

        private float[] interpolate(float[][] pts, float t) {
            if (pts.length == 0) return new float[]{0.5f, 0.5f};
            if (pts.length == 1) return pts[0];
            float scaled = t * (pts.length - 1);
            int i = Math.min(pts.length - 2, Math.max(0, (int) scaled));
            float f = scaled - i;
            return new float[]{
                    pts[i][0] + (pts[i + 1][0] - pts[i][0]) * f,
                    pts[i][1] + (pts[i + 1][1] - pts[i][1]) * f
            };
        }

        private float[][] geometry(String line) {
            if (line.equals("Green Line")) {
                return new float[][]{{0.23f,0.10f},{0.28f,0.17f},{0.33f,0.25f},{0.39f,0.33f},{0.46f,0.56f},{0.49f,0.64f},{0.53f,0.72f},{0.56f,0.80f},{0.59f,0.91f}};
            }
            if (line.equals("Yellow Line")) {
                return new float[][]{{0.53f,0.72f},{0.56f,0.76f},{0.59f,0.80f},{0.63f,0.83f},{0.68f,0.86f},{0.72f,0.89f},{0.78f,0.92f}};
            }
            return new float[][]{{0.88f,0.40f},{0.78f,0.41f},{0.68f,0.42f},{0.60f,0.45f},{0.54f,0.49f},{0.46f,0.56f},{0.37f,0.60f},{0.28f,0.63f},{0.17f,0.70f},{0.08f,0.78f}};
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            float x = event.getX() / getWidth();
            float y = event.getY() / getHeight();
            String nearest = nearestStation(x, y);
            if (!nearest.isEmpty()) showStationToast(nearest);
            return true;
        }

        private String nearestStation(float x, float y) {
            String best = "";
            double min = Double.MAX_VALUE;
            String[] lines = {"Purple Line", "Green Line", "Yellow Line"};
            for (String line : lines) {
                float[][] points = geometry(line);
                for (int i = 0; i < points.length; i++) {
                    double dx = points[i][0] - x;
                    double dy = points[i][1] - y;
                    double d = dx * dx + dy * dy;
                    if (d < min) {
                        min = d;
                        best = stationName(line, Math.min(i, stations.get(line).size() - 1));
                    }
                }
            }
            return min < 0.0025 ? best : "";
        }
    }
}
