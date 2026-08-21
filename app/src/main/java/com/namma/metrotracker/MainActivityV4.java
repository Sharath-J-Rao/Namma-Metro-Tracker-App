package com.namma.metrotracker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * V4 presentation layer for Namma Metro Tracker.
 * The existing routing engine remains in MainActivity; this screen replaces
 * the old long-form interface with a commuter-first mobile experience.
 */
public class MainActivityV4 extends MainActivity {
    private AutoCompleteTextView from;
    private AutoCompleteTextView to;
    private LinearLayout results;
    private TextView tracker;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        from = field("fromInput");
        to = field("toInput");
        results = field("resultBox");
        tracker = field("trackerText");
        buildV4();
    }

    @SuppressWarnings("unchecked")
    private <T> T field(String name) {
        try {
            Field f = MainActivity.class.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(this);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to access app state: " + name, e);
        }
    }

    private void call(String name) {
        try {
            Method m = MainActivity.class.getDeclaredMethod(name);
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception e) {
            Toast.makeText(this, "Action unavailable.", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildV4() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = col();
        root.setPadding(dp(20), dp(12), dp(20), dp(28));
        root.setBackgroundColor(Color.rgb(246, 247, 250));
        scroll.addView(root);

        LinearLayout header = row();
        LinearLayout brand = col();
        TextView title = text("Namma Metro", 30, Color.rgb(20, 25, 33));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.addView(title);
        TextView city = text("BENGALURU", 10, Color.rgb(100, 109, 121));
        city.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        city.setLetterSpacing(.10f);
        brand.addView(city);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView live = pill("3 LINES LIVE", Color.rgb(231, 248, 239), Color.rgb(24, 128, 72));
        header.addView(live);
        root.addView(header);
        space(root, 18);

        TextView heading = text("Where are you going?", 23, Color.rgb(20, 25, 33));
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(heading);
        root.addView(text("Plan your metro journey in seconds.", 14, Color.rgb(96, 106, 119)));
        space(root, 12);

        LinearLayout journey = card();
        journey.setPadding(dp(16), dp(16), dp(16), dp(16));
        journey.addView(label("FROM"));
        styleInput(from);
        journey.addView(from);

        LinearLayout middle = row();
        TextView routeLabel = label("YOUR JOURNEY");
        middle.addView(routeLabel, new LinearLayout.LayoutParams(0, -2, 1));
        TextView swap = pill("SWAP", Color.rgb(238, 241, 246), Color.rgb(57, 67, 80));
        middle.addView(swap);
        swap.setOnClickListener(v -> {
            String a = from.getText().toString();
            from.setText(to.getText().toString());
            to.setText(a);
        });
        journey.addView(middle);

        journey.addView(label("TO"));
        styleInput(to);
        journey.addView(to);

        TextView plan = primary("PLAN JOURNEY");
        LinearLayout.LayoutParams planP = new LinearLayout.LayoutParams(-1, dp(52));
        planP.topMargin = dp(14);
        journey.addView(plan, planP);
        plan.setOnClickListener(v -> call("planJourney"));

        LinearLayout actions = row();
        TextView favourite = secondary("USE FAVOURITE");
        TextView save = secondary("SAVE START");
        actions.addView(favourite, new LinearLayout.LayoutParams(0, dp(46), 1));
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(8);
        journey.addView(actions, ap);
        favourite.setOnClickListener(v -> call("useFavouriteStation"));
        save.setOnClickListener(v -> call("saveFavouriteStation"));
        root.addView(journey);

        space(root, 18);
        root.addView(section("NETWORK"));
        LinearLayout lines = row();
        lines.addView(line("PURPLE", Color.rgb(106, 27, 154)));
        lines.addView(line("GREEN", Color.rgb(46, 125, 50)));
        lines.addView(line("YELLOW", Color.rgb(190, 132, 0)));
        root.addView(lines);

        LinearLayout network = card();
        network.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView nt = text("Operational network", 16, Color.rgb(27, 33, 41));
        nt.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        network.addView(nt);
        network.addView(text("Purple   Whitefield ↔ Challaghatta", 13, Color.rgb(79, 88, 101)));
        network.addView(text("Green    Madavara ↔ Silk Institute", 13, Color.rgb(79, 88, 101)));
        network.addView(text("Yellow   RV Road ↔ Bommasandra", 13, Color.rgb(79, 88, 101)));
        root.addView(network);

        space(root, 18);
        root.addView(section("YOUR JOURNEY"));
        results.setPadding(dp(4), dp(4), dp(4), dp(4));
        results.setBackground(round(Color.WHITE, 18));
        TextView share = secondary("SHARE JOURNEY");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(46));
        sp.topMargin = dp(8);
        root.addView(share, sp);
        share.setOnClickListener(v -> call("shareJourney"));

        space(root, 18);
        root.addView(section("TRIP MODE"));
        LinearLayout trip = card();
        trip.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView tripTitle = text("Stay on your route", 18, Color.rgb(27, 33, 41));
        tripTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        trip.addView(tripTitle);
        trip.addView(tracker);
        TextView next = primary("I REACHED THE NEXT STATION");
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(50));
        np.topMargin = dp(12);
        trip.addView(next, np);
        next.setOnClickListener(v -> call("advanceTracker"));
        root.addView(trip);

        space(root, 18);
        root.addView(section("TOOLS"));
        LinearLayout tools = row();
        TextView info = secondary("STATION INFO");
        TextView maps = secondary("OPEN MAPS");
        tools.addView(info, new LinearLayout.LayoutParams(0, dp(48), 1));
        tools.addView(maps, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(tools);
        info.setOnClickListener(v -> call("showStationInfo"));
        maps.setOnClickListener(v -> openMaps());
        TextView service = secondary("OFFICIAL BMRCL SERVICE INFORMATION");
        LinearLayout.LayoutParams svc = new LinearLayout.LayoutParams(-1, dp(48));
        svc.topMargin = dp(8);
        root.addView(service, svc);
        service.setOnClickListener(v -> openUrl("https://english.bmrc.co.in/metro-timings/"));

        TextView note = text("Independent app • Verify current fares, timings and service notices with BMRCL before important journeys.", 11, Color.rgb(122, 130, 141));
        LinearLayout.LayoutParams noteP = new LinearLayout.LayoutParams(-1, -2);
        noteP.topMargin = dp(18);
        root.addView(note, noteP);
        setContentView(scroll);
    }

    private void styleInput(AutoCompleteTextView input) {
        input.setHintTextColor(Color.rgb(145, 153, 164));
        input.setTextColor(Color.rgb(27, 33, 41));
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setBackground(round(Color.rgb(248, 249, 251), 14));
    }

    private void openMaps() {
        try {
            String q = Uri.encode("Namma Metro Bengaluru stations");
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + q)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open maps.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open the page.", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout col() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private LinearLayout row() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private LinearLayout card() {
        LinearLayout v = col();
        v.setBackground(round(Color.WHITE, 18));
        v.setElevation(dp(1));
        return v;
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }

    private TextView label(String value) {
        TextView v = text(value, 10, Color.rgb(112, 121, 133));
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLetterSpacing(.07f);
        return v;
    }

    private TextView section(String value) {
        TextView v = text(value, 11, Color.rgb(112, 121, 133));
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLetterSpacing(.08f);
        return v;
    }

    private TextView primary(String value) {
        TextView v = text(value, 13, Color.WHITE);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setLetterSpacing(.04f);
        v.setBackground(round(Color.rgb(18, 104, 208), 16));
        v.setClickable(true);
        v.setFocusable(true);
        v.setContentDescription(value);
        return v;
    }

    private TextView secondary(String value) {
        TextView v = text(value, 11, Color.rgb(46, 57, 70));
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setBackground(round(Color.WHITE, 14));
        v.setClickable(true);
        v.setFocusable(true);
        v.setContentDescription(value);
        return v;
    }

    private TextView pill(String value, int bg, int fg) {
        TextView v = text(value, 10, fg);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(10), dp(8), dp(10), dp(8));
        v.setBackground(round(bg, 999));
        return v;
    }

    private TextView line(String value, int color) {
        TextView v = pill(value, Color.WHITE, color);
        v.setBackground(outline(color));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.rightMargin = dp(7);
        v.setLayoutParams(p);
        return v;
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private android.graphics.drawable.GradientDrawable outline(int color) {
        android.graphics.drawable.GradientDrawable d = round(Color.WHITE, 999);
        d.setStroke(dp(1), color);
        return d;
    }

    private void space(LinearLayout root, int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        root.addView(v);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
