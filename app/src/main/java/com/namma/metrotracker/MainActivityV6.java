package com.namma.metrotracker; // Keeps the v6 screen inside the app package.

import android.app.Activity; // Provides the Android activity base class.
import android.app.AlertDialog; // Provides the backend URL configuration dialog.
import android.graphics.Color; // Provides UI colors.
import android.graphics.Typeface; // Provides text styles.
import android.os.Bundle; // Provides activity lifecycle state.
import android.os.Handler; // Runs the countdown timer on the UI thread.
import android.view.Gravity; // Controls view alignment.
import android.view.View; // Provides the base Android view type.
import android.widget.ArrayAdapter; // Supplies values to dropdowns.
import android.widget.EditText; // Lets the user configure the backend URL.
import android.widget.LinearLayout; // Builds the interface using simple native layouts.
import android.widget.ScrollView; // Allows the long mobile screen to scroll.
import android.widget.Spinner; // Provides the requested plain dropdown controls.
import android.widget.TextView; // Displays the app information.
import android.widget.Toast; // Displays short user messages.
import org.json.JSONObject; // Parses the backend JSON response.
import java.io.BufferedReader; // Reads the backend HTTP response.
import java.io.InputStreamReader; // Converts response bytes to text.
import java.net.HttpURLConnection; // Makes simple HTTP requests without extra libraries.
import java.net.URL; // Represents the backend URL.
import java.util.ArrayList; // Stores station names.
import java.util.Arrays; // Creates compact fallback station lists.
import java.util.LinkedHashMap; // Preserves line ordering in the UI.
import java.util.List; // Represents station collections.
import java.util.Locale; // Formats strings consistently.
import java.util.Map; // Stores line-to-station mappings.
import android.content.SharedPreferences; // Stores the backend URL locally.

public class MainActivityV6 extends Activity { // Defines the v6 metro tracker screen.
    private final Map<String, List<String>> lineStations = new LinkedHashMap<>(); // Stores the offline fallback line data.
    private Spinner lineSpinner; // Holds the selected metro line.
    private Spinner fromSpinner; // Holds the selected origin station.
    private Spinner toSpinner; // Holds the selected destination station.
    private TextView fareText; // Displays the fare estimate.
    private TextView stopsText; // Displays the number of stops.
    private TextView durationText; // Displays the journey-time estimate.
    private TextView nextTrainText; // Displays the next-train estimate.
    private TextView lastTrainText; // Displays the last scheduled service.
    private TextView countdownText; // Displays the countdown to last service.
    private TextView liveText; // Displays live-data availability.
    private TextView sourceText; // Displays whether data comes from GTFS or fallback data.
    private SharedPreferences prefs; // Stores the configured backend address.
    private long lastServiceMillis; // Stores the next closing timestamp for the selected line.
    private final Handler handler = new Handler(); // Creates the UI countdown scheduler.

    @Override public void onCreate(Bundle state) { // Runs when Android creates the activity.
        super.onCreate(state); // Initializes the Android activity first.
        prefs = getSharedPreferences("metro_tracker_v6", MODE_PRIVATE); // Opens local settings storage.
        loadFallbackNetwork(); // Loads the offline station fallback immediately.
        buildUi(); // Creates the redesigned v6 interface.
        updateStationDropdowns(); // Populates station dropdowns using the default Purple line.
        updateStatusCards(); // Calculates the initial status information.
        startCountdown(); // Starts the last-service countdown loop.
    }

    private void loadFallbackNetwork() { // Loads the prototype station data used when GTFS is unavailable.
        lineStations.put("Purple Line", Arrays.asList("Whitefield", "Hopefarm", "Kadugodi Tree Park", "Pattandur Agrahara", "Sri Sathya Sai Hospital", "Nallurhalli", "Kundalahalli", "Seetharampalya", "Hoodi", "Garudacharpalya", "Singayyanapalya", "K.R. Pura", "Benniganahalli", "Baiyappanahalli", "Swami Vivekananda Road", "Indiranagar", "Halasuru", "Trinity", "MG Road", "Cubbon Park", "Vidhana Soudha", "Central College", "Majestic", "City Railway Station", "Magadi Road", "Hosahalli", "Vijayanagara", "Attiguppe", "Deepanjali Nagar", "Mysuru Road", "Nayandahalli", "RR Nagar", "Jnanabharathi", "Pattanagere", "Kengeri Bus Terminal", "Kengeri", "Challaghatta")); // Adds Purple Line stations.
        lineStations.put("Green Line", Arrays.asList("Madavara", "Chikkabidarakallu", "Manjunathanagara", "Nagasandra", "Dasarahalli", "Jalahalli", "Peenya Industry", "Peenya", "Goraguntepalya", "Yeshwanthpur", "Sandal Soap Factory", "Mahalakshmi", "Rajajinagar", "Kuvempu Road", "Srirampura", "Sampige Road", "Majestic", "Chickpete", "KR Market", "National College", "Lalbagh", "South End Circle", "Jayanagara", "RV Road", "Banashankari", "JP Nagar", "Yelachenahalli", "Konanakunte Cross", "Doddakallasandra", "Vajarahalli", "Thalaghattapura", "Silk Institute")); // Adds Green Line stations.
        lineStations.put("Yellow Line", Arrays.asList("RV Road", "Ragigudda", "Jayadeva Hospital", "BTM Layout", "Central Silk Board", "Bommanahalli", "Hongasandra", "Kudlu Gate", "Singasandra", "Hosa Road", "Beratena Agrahara", "Electronic City", "Infosys Agrahara", "Huskur Road", "Hebbagodi", "Bommasandra")); // Adds Yellow Line stations.
    }

    private void buildUi() { // Builds the complete v6 home screen.
        ScrollView scroll = new ScrollView(this); // Makes the interface usable on small screens.
        scroll.setFillViewport(true); // Lets the content fill the viewport when it is short.
        LinearLayout root = column(); // Creates the main vertical page.
        root.setPadding(dp(20), dp(16), dp(20), dp(28)); // Adds comfortable screen spacing.
        root.setBackgroundColor(Color.rgb(246, 247, 250)); // Uses a soft neutral background.
        scroll.addView(root); // Places the page inside the scroll view.

        LinearLayout header = row(); // Creates the compact header row.
        LinearLayout brand = column(); // Creates the title stack.
        TextView title = text("Namma Metro", 28, Color.rgb(20, 25, 33)); // Creates the app title.
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); // Makes the title prominent.
        brand.addView(title); // Adds the title to the header.
        brand.addView(text("BENGALURU • SMART COMMUTER", 10, Color.rgb(105, 115, 128))); // Adds a product subtitle.
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1)); // Gives the brand the available width.
        TextView source = pill("GTFS + LIVE", Color.rgb(232, 247, 238), Color.rgb(24, 128, 72)); // Creates the data status pill.
        header.addView(source); // Adds the status pill to the header.
        root.addView(header); // Adds the header to the screen.

        space(root, 18); // Adds visual separation.
        root.addView(text("Select your metro line", 22, Color.rgb(20, 25, 33))); // Adds the primary instruction.
        root.addView(text("The station dropdowns will use only the selected line.", 14, Color.rgb(96, 106, 119))); // Explains the interaction simply.
        space(root, 12); // Adds a small gap before the controls.

        LinearLayout lineCard = card(); // Creates the line-selection card.
        lineCard.setPadding(dp(16), dp(15), dp(16), dp(15)); // Gives the card inner spacing.
        lineCard.addView(label("METRO LINE")); // Adds the line label.
        lineSpinner = spinner(); // Creates the line dropdown.
        lineCard.addView(lineSpinner, new LinearLayout.LayoutParams(-1, dp(52))); // Makes the dropdown touch friendly.
        root.addView(lineCard); // Adds the line card to the page.

        space(root, 12); // Adds a gap before station selection.
        LinearLayout journeyCard = card(); // Creates the station-selection card.
        journeyCard.setPadding(dp(16), dp(15), dp(16), dp(16)); // Gives the card inner spacing.
        journeyCard.addView(label("JOURNEY")); // Adds the journey heading.
        journeyCard.addView(label("FROM")); // Adds the origin label.
        fromSpinner = spinner(); // Creates the origin dropdown.
        journeyCard.addView(fromSpinner, new LinearLayout.LayoutParams(-1, dp(52))); // Adds the origin dropdown.
        spaceInto(journeyCard, 8); // Adds a small gap between the dropdowns.
        journeyCard.addView(label("TO")); // Adds the destination label.
        toSpinner = spinner(); // Creates the destination dropdown.
        journeyCard.addView(toSpinner, new LinearLayout.LayoutParams(-1, dp(52))); // Adds the destination dropdown.
        root.addView(journeyCard); // Adds the journey card to the screen.

        space(root, 14); // Adds space before the information cards.
        LinearLayout infoGrid = column(); // Creates the vertical information grid.
        infoGrid.addView(metricRow("TICKET", fareText = text("₹--", 20, Color.rgb(20, 25, 33)), "Estimated fare")); // Adds the fare information card.
        infoGrid.addView(metricRow("STOPS", stopsText = text("--", 20, Color.rgb(20, 25, 33)), "Between selected stations")); // Adds the stop count card.
        infoGrid.addView(metricRow("TIME", durationText = text("-- min", 20, Color.rgb(20, 25, 33)), "Estimated journey")); // Adds the journey-time card.
        infoGrid.addView(metricRow("NEXT TRAIN", nextTrainText = text("Scheduled estimate", 18, Color.rgb(20, 25, 33)), "Live when feed is available")); // Adds the next-train card.
        root.addView(infoGrid); // Adds the information grid to the page.

        space(root, 12); // Adds separation before service status.
        LinearLayout serviceCard = card(); // Creates the service-status card.
        serviceCard.setPadding(dp(16), dp(15), dp(16), dp(15)); // Gives the service card padding.
        serviceCard.addView(label("SERVICE STATUS")); // Adds the service heading.
        lastTrainText = text("Last service: --", 18, Color.rgb(20, 25, 33)); // Creates the last-service display.
        lastTrainText.setTypeface(Typeface.DEFAULT, Typeface.BOLD); // Makes the last service prominent.
        serviceCard.addView(lastTrainText); // Adds the last-service line.
        countdownText = text("Time left: --", 16, Color.rgb(24, 128, 72)); // Creates the countdown display.
        serviceCard.addView(countdownText); // Adds the countdown line.
        liveText = text("● Live feed: not connected", 13, Color.rgb(115, 123, 134)); // Creates the live-feed status.
        serviceCard.addView(liveText); // Adds the live-feed status.
        sourceText = text("Data source: embedded fallback", 12, Color.rgb(125, 133, 145)); // Creates the data-source note.
        serviceCard.addView(sourceText); // Adds the data-source note.
        root.addView(serviceCard); // Adds the service card to the page.

        space(root, 10); // Adds a small gap before the action buttons.
        LinearLayout actions = row(); // Creates the action row.
        TextView refresh = primary("REFRESH"); // Creates the refresh button.
        TextView dataSource = secondary("DATA SOURCE"); // Creates the backend configuration button.
        actions.addView(refresh, new LinearLayout.LayoutParams(0, dp(50), 1)); // Gives refresh half of the row.
        actions.addView(dataSource, new LinearLayout.LayoutParams(0, dp(50), 1)); // Gives data source the other half.
        root.addView(actions); // Adds the action row to the screen.
        refresh.setOnClickListener(v -> updateStatusCards()); // Refreshes station information when tapped.
        dataSource.setOnClickListener(v -> configureBackend()); // Opens the backend URL configuration dialog.

        TextView note = text("Fare is shown as an estimate until official GTFS fare/distance data is loaded. Live train information requires an authorized backend/IUDX feed.", 11, Color.rgb(122, 130, 141)); // Explains the current data boundary.
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2); // Creates footer spacing parameters.
        noteParams.topMargin = dp(15); // Adds separation from the buttons.
        root.addView(note, noteParams); // Adds the data transparency note.

        setContentView(scroll); // Displays the completed interface.
        lineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() { // Watches the selected line.
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { updateStationDropdowns(); } // Refreshes station dropdowns after a line change.
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { } // Safely ignores empty selections.
        }); // Finishes the line-selection listener.
        fromSpinner.setOnItemSelectedListener(simpleSelectionListener()); // Refreshes journey information when origin changes.
        toSpinner.setOnItemSelectedListener(simpleSelectionListener()); // Refreshes journey information when destination changes.
    }

    private android.widget.AdapterView.OnItemSelectedListener simpleSelectionListener() { // Creates a reusable station-selection listener.
        return new android.widget.AdapterView.OnItemSelectedListener() { // Returns the listener implementation.
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { updateStatusCards(); } // Refreshes the information cards.
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { } // Safely ignores empty selections.
        }; // Returns the completed listener.
    }

    private void updateStationDropdowns() { // Rebuilds the station dropdowns for the chosen line.
        String line = lineSpinner == null || lineSpinner.getSelectedItem() == null ? "Purple Line" : lineSpinner.getSelectedItem().toString(); // Reads the selected line or uses Purple by default.
        List<String> stations = lineStations.get(line); // Gets only the stations belonging to that line.
        if (stations == null) stations = new ArrayList<>(); // Prevents a null station list from breaking the UI.
        setSpinner(fromSpinner, stations); // Populates the origin dropdown.
        setSpinner(toSpinner, stations); // Populates the destination dropdown.
        if (stations.size() > 1) toSpinner.setSelection(1); // Chooses the next station as a useful default destination.
        updateStatusCards(); // Refreshes all dependent information.
    }

    private void updateStatusCards() { // Calculates and displays current journey information.
        if (lineSpinner == null || fromSpinner == null || toSpinner == null) return; // Waits until the widgets exist.
        String line = String.valueOf(lineSpinner.getSelectedItem()); // Gets the selected line.
        String from = String.valueOf(fromSpinner.getSelectedItem()); // Gets the selected origin station.
        String to = String.valueOf(toSpinner.getSelectedItem()); // Gets the selected destination station.
        List<String> stations = lineStations.get(line); // Gets the station order for the line.
        if (stations == null || from == null || to == null || !stations.contains(from) || !stations.contains(to)) return; // Stops when selection is incomplete.
        int stops = Math.abs(stations.indexOf(to) - stations.indexOf(from)); // Calculates station-to-station hops.
        double kmEstimate = Math.max(0, stops * 1.4); // Uses fallback average spacing only when exact GTFS distance is unavailable.
        fareText.setText("₹" + estimateFare(kmEstimate)); // Shows the current fallback fare estimate.
        stopsText.setText(String.valueOf(stops)); // Shows the stop count.
        durationText.setText(Math.max(2, Math.round(stops * 2.4f)) + " min"); // Shows the journey-time estimate.
        int[] close = closeTime(line); // Gets the scheduled last-service time for the selected line.
        java.util.Calendar now = java.util.Calendar.getInstance(); // Reads local current time.
        java.util.Calendar service = java.util.Calendar.getInstance(); // Creates a mutable service-time calendar.
        service.set(java.util.Calendar.HOUR_OF_DAY, close[0]); // Sets the closing hour.
        service.set(java.util.Calendar.MINUTE, close[1]); // Sets the closing minute.
        service.set(java.util.Calendar.SECOND, 0); // Clears the closing seconds.
        service.set(java.util.Calendar.MILLISECOND, 0); // Clears the closing milliseconds.
        if (service.before(now)) service.add(java.util.Calendar.DAY_OF_MONTH, 1); // Moves to tomorrow after today's last service.
        lastServiceMillis = service.getTimeInMillis(); // Stores the countdown endpoint.
        lastTrainText.setText(String.format(Locale.US, "Last service: %02d:%02d", close[0], close[1])); // Displays the scheduled last service.
        long minutesLeft = Math.max(0, (lastServiceMillis - now.getTimeInMillis()) / 60000); // Calculates time remaining.
        countdownText.setText("Time left: " + minutesLeft / 60 + "h " + minutesLeft % 60 + "m"); // Displays the countdown in hours and minutes.
        nextTrainText.setText("~" + scheduledHeadway() + " min"); // Shows a schedule/headway estimate until live arrival data is available.
        liveText.setText("● Live feed: not connected"); // Resets the live state before the optional backend refresh.
        sourceText.setText("Data source: embedded fallback"); // Shows that the local fallback is active.
        fetchBackendStatus(line, from, to); // Attempts an optional live backend request in the background.
    }

    private void fetchBackendStatus(String line, String from, String to) { // Requests optional backend status information.
        String base = prefs.getString("api_base", "").trim(); // Reads the configured backend URL.
        if (base.isEmpty()) return; // Keeps the app fully offline when no backend is configured.
        new Thread(() -> { // Performs network work away from the Android UI thread.
            try { // Protects the UI from network errors.
                String path = base.replaceAll("/+$", "") + "/api/status?line=" + java.net.URLEncoder.encode(line, "UTF-8") + "&source=" + java.net.URLEncoder.encode(from, "UTF-8") + "&destination=" + java.net.URLEncoder.encode(to, "UTF-8"); // Builds the backend request URL.
                HttpURLConnection connection = (HttpURLConnection) new URL(path).openConnection(); // Opens the backend connection.
                connection.setRequestMethod("GET"); // Uses a read-only request.
                connection.setConnectTimeout(5000); // Limits connection waiting time.
                connection.setReadTimeout(8000); // Limits response waiting time.
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())); // Reads the response text.
                StringBuilder body = new StringBuilder(); // Stores the response text.
                String lineText; // Holds one response line at a time.
                while ((lineText = reader.readLine()) != null) body.append(lineText); // Reads the full JSON response.
                reader.close(); // Closes the response reader.
                connection.disconnect(); // Releases the HTTP connection.
                JSONObject json = new JSONObject(body.toString()); // Parses the JSON response.
                runOnUiThread(() -> applyBackendStatus(json)); // Updates the screen safely on the UI thread.
            } catch (Exception ignored) { } // Keeps offline fallback working when live data is unreachable.
        }).start(); // Starts the background request.
    }

    private void applyBackendStatus(JSONObject json) { // Applies backend values to the information cards.
        try { // Keeps malformed optional data from breaking the UI.
            int fare = json.optInt("estimated_fare", -1); // Reads the backend fare estimate.
            if (fare >= 0) fareText.setText("₹" + fare); // Uses the backend fare when available.
            durationText.setText(json.optInt("estimated_minutes", 0) + " min"); // Uses the backend journey-time estimate.
            stopsText.setText(String.valueOf(json.optInt("stops", 0))); // Uses the backend stop count.
            lastTrainText.setText("Last service: " + json.optString("last_service", "--")); // Uses the backend service time.
            long minutes = json.optLong("minutes_to_last_service", -1); // Reads the backend countdown.
            if (minutes >= 0) countdownText.setText("Time left: " + minutes / 60 + "h " + minutes % 60 + "m"); // Uses the backend countdown when available.
            JSONObject live = json.optJSONObject("live"); // Reads the live feed status object.
            if (live != null && live.optBoolean("enabled", false)) { // Checks whether the backend has an authorized live stream.
                liveText.setText("● Live feed: connected"); // Shows the connected live state.
                liveText.setTextColor(Color.rgb(24, 128, 72)); // Colors the live state green.
                sourceText.setText("Data source: GTFS + authorized live feed"); // Makes the source transparent.
                nextTrainText.setText("Live feed available"); // Avoids inventing an exact arrival without a normalized prediction.
            } else { // Handles static-only backend mode.
                liveText.setText("● Live feed: unavailable"); // Communicates that live data is not active.
                liveText.setTextColor(Color.rgb(115, 123, 134)); // Uses a neutral color for inactive live data.
                sourceText.setText("Data source: GTFS backend"); // Shows that the backend supplied static data.
            }
        } catch (Exception ignored) { } // Keeps the offline display safe if a field is malformed.
    }

    private void configureBackend() { // Lets the user configure a backend URL without rebuilding the APK.
        EditText input = new EditText(this); // Creates the backend URL entry field.
        input.setHint("http://192.168.1.50:8000"); // Shows a typical LAN backend example.
        input.setText(prefs.getString("api_base", "")); // Restores the saved URL.
        input.setSelectAllOnFocus(true); // Makes editing the address easy.
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Data source").setMessage("Enter your Namma Metro backend URL. Keep IUDX credentials on the backend, never in the APK.").setView(input).setNegativeButton("OFFLINE", (d, w) -> prefs.edit().remove("api_base").apply()).setPositiveButton("SAVE", (d, w) -> { prefs.edit().putString("api_base", input.getText().toString().trim()).apply(); updateStatusCards(); }).create(); // Creates the configuration dialog.
        dialog.show(); // Displays the configuration dialog.
    }

    private int estimateFare(double kilometres) { // Estimates the published 2026 token/QR fare slab.
        if (kilometres <= 2) return 11; // Covers the 0-2 km slab.
        if (kilometres <= 4) return 21; // Covers the 2-4 km slab.
        if (kilometres <= 6) return 32; // Covers the 4-6 km slab.
        if (kilometres <= 8) return 42; // Covers the 6-8 km slab.
        if (kilometres <= 10) return 53; // Covers the 8-10 km slab.
        if (kilometres <= 15) return 63; // Covers the 10-15 km slab.
        if (kilometres <= 20) return 74; // Covers the 15-20 km slab.
        if (kilometres <= 25) return 84; // Covers the 20-25 km slab.
        if (kilometres <= 30) return 90; // Covers the 25-30 km slab.
        return 95; // Covers trips above 30 km.
    }

    private int scheduledHeadway() { // Returns the conservative next-train estimate used without live data.
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY); // Reads the current hour.
        return hour >= 7 && hour <= 10 || hour >= 17 && hour <= 20 ? 5 : 10; // Uses shorter peak and longer off-peak intervals.
    }

    private int[] closeTime(String line) { // Returns the scheduled service end time used by the UI.
        if (line.startsWith("Yellow")) return new int[]{23, 55}; // Uses Yellow Line's published guidance.
        return new int[]{23, 5}; // Uses Purple/Green guidance.
    }

    private void startCountdown() { // Keeps the last-service countdown updating every minute.
        handler.postDelayed(new Runnable() { // Schedules the first countdown update.
            @Override public void run() { updateCountdownOnly(); handler.postDelayed(this, 60000); } // Refreshes and reschedules once per minute.
        }, 1000); // Starts the countdown after one second.
    }

    private void updateCountdownOnly() { // Updates only the time-left label without hitting the backend.
        if (countdownText == null) return; // Stops until the UI exists.
        long minutes = Math.max(0, (lastServiceMillis - System.currentTimeMillis()) / 60000); // Calculates the remaining minutes.
        countdownText.setText("Time left: " + minutes / 60 + "h " + minutes % 60 + "m"); // Displays the remaining service window.
    }

    private Spinner spinner() { // Creates a standard Android dropdown.
        Spinner spinner = new Spinner(this); // Creates the native Spinner control.
        spinner.setBackgroundColor(Color.WHITE); // Keeps the dropdown visually clean.
        return spinner; // Returns the configured dropdown.
    }

    private void setSpinner(Spinner spinner, List<String> values) { // Replaces a dropdown's values.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(values)); // Creates a simple native dropdown adapter.
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // Uses the standard expanded dropdown rows.
        spinner.setAdapter(adapter); // Applies the adapter to the dropdown.
    }

    private LinearLayout metricRow(String title, TextView value, String subtitle) { // Builds one information metric card.
        LinearLayout card = card(); // Creates a white card.
        card.setPadding(dp(15), dp(13), dp(15), dp(13)); // Adds card spacing.
        LinearLayout row = row(); // Creates the card's inner row.
        LinearLayout left = column(); // Creates the title/value stack.
        left.addView(label(title)); // Adds the metric heading.
        left.addView(value); // Adds the metric value.
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1)); // Gives the metric content most of the width.
        row.addView(text(subtitle, 11, Color.rgb(115, 123, 134))); // Adds the explanatory subtitle.
        card.addView(row); // Adds the row inside the card.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); // Creates card spacing parameters.
        params.bottomMargin = dp(8); // Adds separation between metric cards.
        card.setLayoutParams(params); // Applies the spacing.
        return card; // Returns the metric card.
    }

    private LinearLayout column() { // Creates a vertical layout helper.
        LinearLayout view = new LinearLayout(this); // Creates the Android layout.
        view.setOrientation(LinearLayout.VERTICAL); // Stacks children vertically.
        return view; // Returns the vertical layout.
    }

    private LinearLayout row() { // Creates a horizontal layout helper.
        LinearLayout view = new LinearLayout(this); // Creates the Android layout.
        view.setOrientation(LinearLayout.HORIZONTAL); // Places children horizontally.
        view.setGravity(Gravity.CENTER_VERTICAL); // Vertically centers child controls.
        return view; // Returns the horizontal layout.
    }

    private LinearLayout card() { // Creates a white rounded card.
        LinearLayout view = column(); // Creates the card layout.
        view.setBackground(round(Color.WHITE, 18)); // Gives the card a rounded white background.
        view.setElevation(dp(1)); // Adds subtle elevation.
        return view; // Returns the styled card.
    }

    private TextView label(String value) { // Creates a compact uppercase section label.
        TextView view = text(value, 10, Color.rgb(112, 121, 133)); // Creates the label text.
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); // Makes the label bold.
        view.setLetterSpacing(.07f); // Adds useful visual tracking.
        return view; // Returns the label.
    }

    private TextView text(String value, float size, int color) { // Creates a standard text view.
        TextView view = new TextView(this); // Creates the Android text view.
        view.setText(value); // Sets the displayed value.
        view.setTextSize(size); // Sets the font size.
        view.setTextColor(color); // Sets the text color.
        view.setPadding(0, dp(4), 0, dp(4)); // Adds small vertical text padding.
        return view; // Returns the text view.
    }

    private TextView primary(String value) { // Creates a primary action button.
        TextView view = text(value, 12, Color.WHITE); // Creates the button text.
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); // Makes the action prominent.
        view.setGravity(Gravity.CENTER); // Centers the label.
        view.setBackground(round(Color.rgb(18, 104, 208), 15)); // Uses the primary blue action color.
        view.setClickable(true); // Enables touch interaction.
        view.setFocusable(true); // Allows keyboard/accessibility focus.
        return view; // Returns the primary button.
    }

    private TextView secondary(String value) { // Creates a secondary action button.
        TextView view = text(value, 11, Color.rgb(46, 57, 70)); // Creates the button label.
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); // Makes the action label clear.
        view.setGravity(Gravity.CENTER); // Centers the label.
        view.setBackground(round(Color.WHITE, 15)); // Uses a neutral secondary surface.
        view.setClickable(true); // Enables touch interaction.
        view.setFocusable(true); // Allows focus and accessibility.
        return view; // Returns the secondary button.
    }

    private TextView pill(String value, int background, int foreground) { // Creates a compact status pill.
        TextView view = text(value, 10, foreground); // Creates the pill text.
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); // Makes the status strong.
        view.setGravity(Gravity.CENTER); // Centers the pill label.
        view.setPadding(dp(10), dp(8), dp(10), dp(8)); // Adds pill padding.
        view.setBackground(round(background, 999)); // Creates a pill shape.
        return view; // Returns the pill view.
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radius) { // Creates a rounded background drawable.
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable(); // Creates the drawable object.
        drawable.setColor(color); // Sets the fill color.
        drawable.setCornerRadius(dp(radius)); // Sets the corner radius.
        return drawable; // Returns the configured drawable.
    }

    private void space(LinearLayout root, int height) { // Adds vertical space to the root layout.
        View view = new View(this); // Creates an invisible spacer view.
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); // Gives the spacer its height.
        root.addView(view); // Adds the spacer to the root.
    }

    private void spaceInto(LinearLayout root, int height) { // Adds vertical space inside a nested card.
        View view = new View(this); // Creates an invisible spacer view.
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); // Gives the spacer its height.
        root.addView(view); // Adds the spacer to the nested card.
    }

    private int dp(int value) { // Converts density-independent pixels to actual pixels.
        return Math.round(value * getResources().getDisplayMetrics().density); // Returns the device-scaled pixel value.
    }
}
