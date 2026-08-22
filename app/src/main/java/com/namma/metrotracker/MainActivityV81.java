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

public class MainActivityV81 extends Activity {
    private static final int BG = Color.rgb(248, 250, 251);
    private static final int INK = Color.rgb(25, 33, 41);
    private static final int MUTED = Color.rgb(103, 114, 124);
    private static final int TEAL = Color.rgb(47, 242, 202);
    private static final int GOLD = Color.rgb(242, 192, 114);
    private static final int INDIGO = Color.rgb(120, 132, 217);
    private static final int CYAN = Color.rgb(20, 167, 207);
    private static final int PURPLE = Color.rgb(126, 70, 168);
    private static final int GREEN = Color.rgb(38, 155, 92);
    private static final int YELLOW = Color.rgb(232, 179, 41);

    private final Map<String, List<String>> stations = new LinkedHashMap<>();
    private final Map<String, LineInfo> lines = new LinkedHashMap<>();
    private Spinner lineSpinner;
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private LinearLayout content;
    private LinearLayout nav;
    private TextView pageTitle;
    private TextView pageSubtitle;
    private TextView noticeText;
    private MetroMapView mapView;
    private int page = 0;
    private final Handler handler = new Handler();
    private final Runnable clock = new Runnable() {
        @Override public void run() {
            refreshJourneyCards();
            if (mapView != null) mapView.invalidate();
            handler.postDelayed(this, 15000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadNetwork();
        buildShell();
        showPage(0);
        handler.post(clock);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(clock);
        super.onDestroy();
    }

    private void loadNetwork() {
        stations.clear();
        stations.put("Purple Line", Arrays.asList("Whitefield","Hopefarm","Kadugodi Tree Park","Pattandur Agrahara","Sri Sathya Sai Hospital","Nallurhalli","Kundalahalli","Seetharampalya","Hoodi","Garudacharpalya","Singayyanapalya","K.R. Pura","Benniganahalli","Baiyappanahalli","Swami Vivekananda Road","Indiranagar","Halasuru","Trinity","MG Road","Cubbon Park","Vidhana Soudha","Central College","Majestic","City Railway Station","Magadi Road","Hosahalli","Vijayanagara","Attiguppe","Deepanjali Nagar","Mysuru Road","Nayandahalli","RR Nagar","Jnanabharathi","Pattanagere","Kengeri Bus Terminal","Kengeri","Challaghatta"));
        stations.put("Green Line", Arrays.asList("Madavara","Chikkabidarakallu","Manjunathanagara","Nagasandra","Dasarahalli","Jalahalli","Peenya Industry","Peenya","Goraguntepalya","Yeshwanthpur","Sandal Soap Factory","Mahalakshmi","Rajajinagar","Kuvempu Road","Srirampura","Sampige Road","Majestic","Chickpete","KR Market","National College","Lalbagh","South End Circle","Jayanagara","RV Road","Banashankari","JP Nagar","Yelachenahalli","Konanakunte Cross","Doddakallasandra","Vajarahalli","Thalaghattapura","Silk Institute"));
        stations.put("Yellow Line", Arrays.asList("RV Road","Ragigudda","Jayadeva Hospital","BTM Layout","Central Silk Board","Bommanahalli","Hongasandra","Kudlu Gate","Singasandra","Hosa Road","Beratena Agrahara","Electronic City","Infosys Agrahara","Huskur Road","Hebbagodi","Bommasandra"));
        lines.clear();
        lines.put("Purple Line", new LineInfo("05:00", "23:05", 8, PURPLE));
        lines.put("Green Line", new LineInfo("05:00", "23:05", 8, GREEN));
        lines.put("Yellow Line", new LineInfo("05:00", "23:00", 7, YELLOW));
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

        LinearLayout brandRow = horizontal();
        TextView logo = text("NM", 17, INK);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setBackground(round(TEAL, 15));
        brandRow.addView(logo, square(48));

        LinearLayout brand = vertical();
        TextView appName = text("Namma Metro Tracker", 21, INK);
        appName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.addView(appName);
        brand.addView(text("Bengaluru", 11, MUTED));
        brandRow.addView(brand, weight(1));
        TextView statusDot = text("●", 17, CYAN);
        statusDot.setGravity(Gravity.CENTER);
        brandRow.addView(statusDot, square(40));
        header.addView(brandRow);
        space(header, 12);

        pageTitle = text("Home", 27, INK);
        pageTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pageSubtitle = text("Everything you need for your trip", 14, MUTED);
        header.addView(pageTitle);
        header.addView(pageSubtitle);
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
            final int selected = i;
            TextView item = text(labels[i], 11, selected == page ? INK : MUTED);
            item.setGravity(Gravity.CENTER);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setBackground(round(selected == page ? Color.rgb(231, 253, 247) : Color.WHITE, 16));
            item.setOnClickListener(v -> showPage(selected));
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
        if (page == 0) { pageTitle.setText("Home"); pageSubtitle.setText("Everything you need for your trip"); }
        if (page == 1) { pageTitle.setText("Your Journey"); pageSubtitle.setText("Fare, route and train timing"); }
        if (page == 2) { pageTitle.setText("Metro Map"); pageSubtitle.setText("Explore the Bengaluru network"); }
        if (page == 3) { pageTitle.setText("Live Trains"); pageSubtitle.setText("See trains moving right now"); }
    }

    private void buildHome() {
        LinearLayout hero = card();
        hero.setBackground(round(INDIGO, 22));
        TextView h = text("Where are you going?", 25, Color.WHITE);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.addView(h);
        hero.addView(text("Plan your trip in a few taps.", 14, Color.WHITE));
        space(hero, 9);
        TextView badge = text("●  TRAINS RUNNING", 11, INK);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setBackground(round(TEAL, 999));
        hero.addView(badge, fill(36));
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
        TextView showJourney = action("SHOW JOURNEY", true);
        planner.addView(showJourney, fill(54));
        showJourney.setOnClickListener(v -> showPage(1));
        TextView swap = action("SWAP", false);
        planner.addView(swap, fill(42));
        swap.setOnClickListener(v -> swapStations());
        content.addView(planner);
        wireSpinners();

        space(content, 12);
        LinearLayout quick = horizontal();
        quick.addView(quickCard("LIVE TRAINS", "Watch trains", INDIGO, 3), weight(1));
        quick.addView(quickCard("METRO MAP", "Full network", CYAN, 2), weight(1));
        content.addView(quick);
        space(content, 10);
        LinearLayout quick2 = horizontal();
        quick2.addView(quickCard("FARES", "Ticket cost", GOLD, 1), weight(1));
        quick2.addView(quickCard("TIMINGS", "First & last", TEAL, 1), weight(1));
        content.addView(quick2);

        space(content, 12);
        LinearLayout notice = card();
        notice.addView(label("SERVICE UPDATE"));
        noticeText = text("No service updates", 15, INK);
        notice.addView(noticeText);
        content.addView(notice);
    }

    private TextView quickCard(String title, String sub, int accent, int target) {
        TextView t = text(title + "\n" + sub, 14, INK);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setBackground(round(Color.WHITE, 18));
        t.setPadding(dp(8), dp(19), dp(8), dp(19));
        t.setOnClickListener(v -> showPage(target));
        return t;
    }

    private void buildJourney() {
        LinearLayout route = card();
        route.setBackground(round(Color.WHITE, 22));
        route.addView(label("YOUR JOURNEY"));
        route.addView(text(routeText(), 22, INK));
        route.addView(text(selectedLine(), 13, lineColor(selectedLine())));
        route.addView(text(stopCount() + " stops  •  " + durationText(), 14, MUTED));
        content.addView(route);

        space(content, 10);
        LinearLayout stats = horizontal();
        stats.addView(stat("FARE", "₹" + fare(), GOLD), weight(1));
        stats.addView(stat("TIME", durationText(), TEAL), weight(1));
        stats.addView(stat("NEXT", nextTrain(), INDIGO), weight(1));
        content.addView(stats);

        space(content, 12);
        LinearLayout service = card();
        service.addView(label("SERVICE"));
        LineInfo info = lines.get(selectedLine());
        service.addView(text("First train  " + info.first, 15, INK));
        service.addView(text("Last train  " + info.last, 15, INK));
        service.addView(text("Typical gap  " + info.headway + " min", 15, INK));
        TextView last = text("Last train in " + countdown(info.last), 16, CYAN);
        last.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        service.addView(last);
        content.addView(service);

        space(content, 12);
        LinearLayout routeCard = card();
        routeCard.addView(label("ROUTE"));
        routeCard.addView(new RouteStripView(this, selectedLine(), fromIndex(), toIndex()), fill(170));
        content.addView(routeCard);

        space(content, 12);
        TextView seeLive = action("SEE TRAINS ON THIS ROUTE", true);
        content.addView(seeLive, fill(54));
        seeLive.setOnClickListener(v -> showPage(3));
    }

    private TextView stat(String title, String value, int accent) {
        TextView t = text(title + "\n" + value, 13, INK);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setBackground(round(Color.WHITE, 18));
        t.setPadding(dp(7), dp(15), dp(7), dp(15));
        return t;
    }

    private void buildMap() {
        LinearLayout card = card();
        card.setPadding(dp(4), dp(4), dp(4), dp(4));
        mapView = new MetroMapView(this, selectedLine());
        card.addView(mapView, fill(540));
        content.addView(card);

        space(content, 10);
        TextView focus = action("SHOW MY JOURNEY ON MAP", true);
        content.addView(focus, fill(52));
        focus.setOnClickListener(v -> {
            if (mapView != null) mapView.setJourney(selectedLine(), fromIndex(), toIndex());
        });
        space(content, 10);
        content.addView(text("Tap a line to explore it. Your selected journey is highlighted when you focus the map.", 12, MUTED));
    }

    private void buildLive() {
        LinearLayout intro = card();
        intro.setBackground(round(Color.rgb(241, 242, 253), 22));
        TextView h = text("Trains running now", 23, INK);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        intro.addView(h);
        intro.addView(text("Follow the moving trains across the network.", 14, MUTED));
        content.addView(intro);

        space(content, 10);
        mapView = new MetroMapView(this, selectedLine());
        content.addView(mapView, fill(470));
        space(content, 12);

        for (Map.Entry<String, LineInfo> entry : lines.entrySet()) {
            LinearLayout row = card();
            TextView name = text(entry.getKey(), 16, entry.getValue().color);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(name);
            row.addView(text("Next train  ~" + entry.getValue().headway + " min", 14, INK));
            row.addView(text("Service until " + entry.getValue().last, 12, MUTED));
            content.addView(row);
            space(content, 8);
        }
        content.addView(text("Train positions are approximate and may vary from actual operating conditions.", 11, MUTED));
    }

    private void wireSpinners() {
        List<String> names = new ArrayList<>(stations.keySet());
        lineSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        lineSpinner.setSelection(0);
        lineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { refreshStations(); }
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
        if (list.size() > 1) toSpinner.setSelection(1);
        fromSpinner.setOnItemSelectedListener(simpleSelectionListener());
        toSpinner.setOnItemSelectedListener(simpleSelectionListener());
        refreshJourneyCards();
    }

    private AdapterView.OnItemSelectedListener simpleSelectionListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { refreshJourneyCards(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
    }

    private void refreshJourneyCards() {
        if (page == 0 && noticeText != null && noticeText.getText().length() == 0) noticeText.setText("No service updates");
        if (page == 1 && mapView != null) mapView.invalidate();
    }

    private String selectedLine() { return lineSpinner == null || lineSpinner.getSelectedItem() == null ? "Purple Line" : String.valueOf(lineSpinner.getSelectedItem()); }
    private int fromIndex() { return stationIndex(fromSpinner == null ? "Whitefield" : String.valueOf(fromSpinner.getSelectedItem())); }
    private int toIndex() { return stationIndex(toSpinner == null ? "Hopefarm" : String.valueOf(toSpinner.getSelectedItem())); }
    private int stationIndex(String station) { List<String> list = stations.get(selectedLine()); return list == null ? 0 : Math.max(0, list.indexOf(station)); }
    private String routeText() { return stationAt(fromIndex()) + " → " + stationAt(toIndex()); }
    private String stationAt(int index) { List<String> list = stations.get(selectedLine()); return list == null || list.isEmpty() ? "Station" : list.get(Math.min(index, list.size() - 1)); }
    private int stopCount() { return Math.abs(toIndex() - fromIndex()); }
    private int fare() { int km = Math.max(1, Math.round(stopCount() * 1.4f)); int[][] slabs = {{2,11},{4,21},{6,32},{8,42},{10,53},{15,63},{20,74},{25,84},{30,90},{999,95}}; for (int[] s : slabs) if (km <= s[0]) return s[1]; return 95; }
    private String durationText() { return "~" + Math.max(2, Math.round(stopCount() * 2.4f)) + " min"; }
    private String nextTrain() { return "~" + lines.get(selectedLine()).headway + " min"; }
    private String countdown(String hhmm) { try { String[] p = hhmm.split(":"); Calendar t = Calendar.getInstance(); t.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0])); t.set(Calendar.MINUTE, Integer.parseInt(p[1])); t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0); if (t.before(Calendar.getInstance())) t.add(Calendar.DAY_OF_MONTH, 1); long m = Math.max(0, (t.getTimeInMillis() - System.currentTimeMillis()) / 60000); return (m / 60) + "h " + (m % 60) + "m"; } catch (Exception e) { return "—"; } }
    private void swapStations() { if (fromSpinner == null || toSpinner == null) return; int a = fromSpinner.getSelectedItemPosition(); int b = toSpinner.getSelectedItemPosition(); fromSpinner.setSelection(b); toSpinner.setSelection(a); }

    private TextView action(String value, boolean primary) { TextView t = text(value, 12, INK); t.setGravity(Gravity.CENTER); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setBackground(round(primary ? TEAL : Color.WHITE, 14)); return t; }
    private TextView text(String value, float size, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(0, dp(4), 0, dp(4)); return t; }
    private TextView label(String value) { TextView t = text(value, 10, MUTED); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setLetterSpacing(.07f); return t; }
    private LinearLayout vertical() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout horizontal() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout card() { LinearLayout v = vertical(); v.setPadding(dp(16), dp(15), dp(16), dp(15)); v.setBackground(round(Color.WHITE, 20)); v.setElevation(dp(1)); return v; }
    private LinearLayout.LayoutParams fill(int h) { return new LinearLayout.LayoutParams(-1, dp(h)); }
    private LinearLayout.LayoutParams weight(float w) { return new LinearLayout.LayoutParams(0, -2, w); }
    private LinearLayout.LayoutParams square(int h) { return new LinearLayout.LayoutParams(dp(h), dp(h)); }
    private void space(LinearLayout p, int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); p.addView(v); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private android.graphics.drawable.GradientDrawable round(int color, int radius) { android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private int lineColor(String line) { return lines.get(line).color; }

    private static class LineInfo {
        final String first; final String last; final int headway; final int color;
        LineInfo(String first, String last, int headway, int color) { this.first = first; this.last = last; this.headway = headway; this.color = color; }
    }

    private class RouteStripView extends View {
        private final String line; private final int from; private final int to; private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RouteStripView(Context c, String line, int from, int to) { super(c); this.line = line; this.from = Math.min(from, to); this.to = Math.max(from, to); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); int w = getWidth(); int h = getHeight(); int color = lineColor(line); float y = h * .60f;
            paint.setColor(color); paint.setStrokeWidth(dp(7)); paint.setStrokeCap(Paint.Cap.ROUND); c.drawLine(dp(24), y, w - dp(24), y, paint);
            int count = Math.max(1, to - from);
            for (int i = 0; i <= count; i++) { float x = dp(24) + (w - dp(48)) * i / (float) count; paint.setColor(i == 0 || i == count ? INK : Color.WHITE); c.drawCircle(x, y, dp(7), paint); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(color); c.drawCircle(x, y, dp(7), paint); paint.setStyle(Paint.Style.FILL); }
            paint.setTextSize(dp(12)); paint.setTypeface(Typeface.DEFAULT_BOLD); paint.setColor(INK); c.drawText(stationAt(from), dp(18), dp(28), paint); paint.setTextAlign(Paint.Align.RIGHT); c.drawText(stationAt(to), w - dp(18), dp(28), paint); paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private class MetroMapView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); private String focusLine; private boolean focusJourney = false; private int focusFrom = 0; private int focusTo = 1;
        MetroMapView(Context c, String line) { super(c); focusLine = line; paint.setStrokeCap(Paint.Cap.ROUND); }
        void setJourney(String line, int from, int to) { focusLine = line; focusFrom = Math.min(from, to); focusTo = Math.max(from, to); focusJourney = true; invalidate(); }
        @Override protected void onDraw(Canvas c) { super.onDraw(c); c.drawColor(Color.WHITE); if (focusJourney) drawFocused(c); else drawNetwork(c); }
        private void drawNetwork(Canvas c) {
            int w = getWidth(), h = getHeight();
            drawCurve(c, GREEN, 0.08f,0.70f,0.40f,0.12f, "Green Line", 1);
            drawCurve(c, PURPLE, 0.12f,0.20f,0.92f,0.62f, "Purple Line", 1);
            drawCurve(c, YELLOW, 0.45f,0.60f,0.88f,0.92f, "Yellow Line", 1);
            hub(c, w*.48f, h*.45f, "Majestic"); hub(c, w*.66f, h*.73f, "RV Road"); hub(c, w*.56f, h*.50f, "KR Pura");
            drawTrains(c, false);
            textAt(c,"Purple",w*.76f,h*.56f,PURPLE); textAt(c,"Green",w*.20f,h*.20f,GREEN); textAt(c,"Yellow",w*.76f,h*.93f,YELLOW);
        }
        private void drawFocused(Canvas c) {
            int w = getWidth(), h = getHeight(); int color = lineColor(focusLine); float y = h*.56f;
            paint.setColor(Color.rgb(248,250,251)); c.drawRoundRect(dp(12),dp(12),w-dp(12),h-dp(12),dp(24),dp(24),paint);
            paint.setColor(color); paint.setStrokeWidth(dp(10)); paint.setStrokeCap(Paint.Cap.ROUND); c.drawLine(dp(40),y,w-dp(40),y,paint);
            int count=Math.max(1,focusTo-focusFrom);
            List<String> list=stations.get(focusLine);
            for(int i=0;i<=count;i++){float x=dp(40)+(w-dp(80))*i/(float)count; paint.setColor(Color.WHITE); c.drawCircle(x,y,dp(8),paint); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(3)); paint.setColor(color); c.drawCircle(x,y,dp(8),paint); paint.setStyle(Paint.Style.FILL); if(list!=null){paint.setTextSize(dp(10));paint.setColor(MUTED);String s=list.get(Math.min(list.size()-1,focusFrom+i)); c.drawText(s,x-dp(18),y+dp(32),paint);}}
            drawTrains(c,true);
        }
        private void drawCurve(Canvas c,int color,float x1,float y1,float x2,float y2,String name,int emphasis){ Path p=new Path(); float w=getWidth(),h=getHeight(); float sx=w*x1, sy=h*y1, ex=w*x2, ey=h*y2, mid=(sx+ex)/2; p.moveTo(sx,sy); p.cubicTo(mid,sy,mid,ey,ex,ey); paint.setColor(color); paint.setStrokeWidth(dp(name.equals(focusLine)?9:6)); paint.setStyle(Paint.Style.STROKE); c.drawPath(p,paint); paint.setStyle(Paint.Style.FILL); for(int i=0;i<=12;i++){float t=i/12f;float x=sx+(ex-sx)*t;float y=sy+(ey-sy)*t;paint.setColor(Color.WHITE);c.drawCircle(x,y,dp(4),paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.5f));paint.setColor(color);c.drawCircle(x,y,dp(4),paint);paint.setStyle(Paint.Style.FILL);} }
        private void hub(Canvas c,float x,float y,String label){paint.setColor(Color.WHITE);paint.setShadowLayer(dp(2),0,dp(1),0x33000000);setLayerType(View.LAYER_TYPE_SOFTWARE,paint);c.drawCircle(x,y,dp(9),paint);paint.clearShadowLayer();paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(INK);c.drawCircle(x,y,dp(9),paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(dp(10));paint.setColor(MUTED);c.drawText(label,x-dp(25),y-dp(13),paint);}
        private void drawTrains(Canvas c,boolean focused){int w=getWidth(),h=getHeight();String[] ls=focused?new String[]{focusLine}:new String[]{"Purple Line","Purple Line","Green Line","Yellow Line","Green Line"};for(int i=0;i<ls.length;i++){String line=ls[i];float phase=((System.currentTimeMillis()/6000f)+(i*.19f))%1f;float x=dp(28)+(w-dp(56))*phase;float y;if(line.equals("Green Line"))y=h*(.20f+phase*.45f);else if(line.equals("Yellow Line"))y=h*(.67f+phase*.22f);else y=h*(.30f+phase*.36f);paint.setColor(lineColor(line));c.drawCircle(x,y,dp(8),paint);paint.setColor(Color.WHITE);c.drawCircle(x,y,dp(3),paint);} }
        private void textAt(Canvas c,String s,float x,float y,int color){paint.setColor(color);paint.setTextSize(dp(11));paint.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(s,x,y,paint);}
    }
}
