package com.namma.metrotracker; // Keeps all app code inside one simple package.

import android.app.Activity; // Gives us the Android activity base class.
import android.graphics.Color; // Lets us set line and text colors.
import android.graphics.Canvas; // Lets the custom schematic map draw lines and labels.
import android.graphics.Paint; // Lets the custom schematic map draw styled elements.
import android.os.Bundle; // Holds Android activity startup data.
import android.view.Gravity; // Controls alignment of views.
import android.view.View; // Base type for clickable UI widgets.
import android.widget.ArrayAdapter; // Supplies station names to autocomplete boxes.
import android.widget.AutoCompleteTextView; // Provides searchable station inputs.
import android.widget.Button; // Provides the app's action buttons.
import android.widget.LinearLayout; // Provides vertical and horizontal layouts.
import android.widget.ScrollView; // Lets long route results scroll vertically.
import android.widget.TextView; // Displays labels and route information.
import android.widget.Toast; // Shows lightweight messages to the user.
import android.content.Intent; // Lets the app share a planned journey.
import android.net.Uri; // Lets the app open web pages and map searches.

import java.util.ArrayList; // Stores stations and route results.
import java.util.Arrays; // Makes station list creation compact.
import java.util.Collections; // Provides priority queue helpers and result ordering.
import java.util.Comparator; // Sorts station candidates.
import java.util.HashMap; // Stores graph distances and parent information.
import java.util.HashSet; // Tracks visited nodes and unique station names.
import java.util.List; // Represents station and route collections.
import java.util.Map; // Represents graph dictionaries.
import java.util.PriorityQueue; // Implements Dijkstra route planning.
import java.util.Set; // Represents unique sets of names and lines.

public class MainActivity extends Activity { // Defines the main screen of the metro tracker app.

    private final List<Line> lines = new ArrayList<>(); // Holds all currently operational metro lines.
    private final Map<String, Station> stations = new HashMap<>(); // Stores one canonical station object per station name.
    private AutoCompleteTextView fromInput; // Stores the source station input box.
    private AutoCompleteTextView toInput; // Stores the destination station input box.
    private LinearLayout resultBox; // Holds dynamic route result cards.
    private TextView trackerText; // Displays the current-trip tracker state.
    private List<String> activeTrip = new ArrayList<>(); // Stores the stations in the currently tracked trip.
    private int activeTripIndex = 0; // Stores the current station index in the tracked trip.
    private android.content.SharedPreferences prefs; // Stores the user's favourite stations locally.
    private TextView favouriteText; // Displays the saved favourite station.

    @Override public void onCreate(Bundle savedInstanceState) { // Runs when Android creates the activity.
        super.onCreate(savedInstanceState); // Lets Android initialize the base activity first.
        prefs = getSharedPreferences("metro_tracker", MODE_PRIVATE); // Opens local app settings for favourites.
        buildNetwork(); // Loads the current Purple, Green and Yellow station network.
        buildUi(); // Creates the app interface programmatically so no extra libraries are required.
    }

    private void buildNetwork() { // Creates the station graph used for route calculations.
        addLine("Purple Line", "Whitefield", new String[]{"Whitefield", "Hopefarm", "Kadugodi Tree Park", "Pattandur Agrahara", "Sri Sathya Sai Hospital", "Nallurhalli", "Kundalahalli", "Seetharampalya", "Hoodi", "Garudacharpalya", "Singayyanapalya", "K.R. Pura", "Benniganahalli", "Baiyappanahalli", "Swami Vivekananda Road", "Indiranagar", "Halasuru", "Trinity", "MG Road", "Cubbon Park", "Vidhana Soudha", "Central College", "Majestic", "City Railway Station", "Magadi Road", "Hosahalli", "Vijayanagara", "Attiguppe", "Deepanjali Nagar", "Mysuru Road", "Nayandahalli", "RR Nagar", "Jnanabharathi", "Pattanagere", "Kengeri Bus Terminal", "Kengeri", "Challaghatta"}); // Adds the Purple Line station sequence.
        addLine("Green Line", "Madavara", new String[]{"Madavara", "Chikkabidarakallu", "Manjunathanagara", "Nagasandra", "Dasarahalli", "Jalahalli", "Peenya Industry", "Peenya", "Goraguntepalya", "Yeshwanthpur", "Sandal Soap Factory", "Mahalakshmi", "Rajajinagar", "Kuvempu Road", "Srirampura", "Sampige Road", "Majestic", "Chickpete", "KR Market", "National College", "Lalbagh", "South End Circle", "Jayanagara", "RV Road", "Banashankari", "JP Nagar", "Yelachenahalli", "Konanakunte Cross", "Doddakallasandra", "Vajarahalli", "Thalaghattapura", "Silk Institute"}); // Adds the Green Line station sequence.
        addLine("Yellow Line", "RV Road", new String[]{"RV Road", "Ragigudda", "Jayadeva Hospital", "BTM Layout", "Central Silk Board", "Bommanahalli", "Hongasandra", "Kudlu Gate", "Singasandra", "Hosa Road", "Beratena Agrahara", "Electronic City", "Infosys Agrahara", "Huskur Road", "Hebbagodi", "Bommasandra"}); // Adds the Yellow Line station sequence.
        for (Station station : stations.values()) station.neighbours.sort(Comparator.comparing(edge -> edge.to.name)); // Keeps the graph deterministic for repeatable routes.
    }

    private void addLine(String name, String colorName, String[] names) { // Adds one line and connects each adjacent station.
        Line line = new Line(name, colorName); // Creates a metro line object.
        lines.add(line); // Adds the line to the operational network.
        for (String nameItem : names) { // Walks through all station names on this line.
            Station station = stations.computeIfAbsent(nameItem, Station::new); // Reuses shared interchange stations automatically.
            station.lines.add(name); // Records that the station belongs to this metro line.
            line.stations.add(station); // Records the station in the ordered line sequence.
        }
        for (int i = 0; i < line.stations.size() - 1; i++) { // Adds graph edges between adjacent stations.
            Station a = line.stations.get(i); // Gets the current station.
            Station b = line.stations.get(i + 1); // Gets the next station.
            a.neighbours.add(new Edge(b, name)); // Adds the forward travel edge.
            b.neighbours.add(new Edge(a, name)); // Adds the reverse travel edge.
        }
    }

    private void buildUi() { // Builds the complete Android screen.
        ScrollView scroll = new ScrollView(this); // Makes the screen scroll on small phones.
        LinearLayout root = new LinearLayout(this); // Creates the main vertical page layout.
        root.setOrientation(LinearLayout.VERTICAL); // Stacks sections from top to bottom.
        root.setPadding(28, 24, 28, 28); // Adds comfortable screen margins.
        root.setBackgroundColor(Color.rgb(247, 249, 252)); // Gives the app a light background.
        scroll.addView(root); // Places the vertical layout inside the scroll view.

        TextView title = text("Namma Metro Tracker", 28, Color.rgb(21, 101, 192)); // Creates the app title.
        title.setTypeface(null, 1); // Makes the title bold.
        root.addView(title); // Adds the title to the page.
        TextView subtitle = text("Bengaluru • Purple • Green • Yellow", 15, Color.DKGRAY); // Creates a compact network subtitle.
        root.addView(subtitle); // Adds the subtitle below the title.

        root.addView(infoCard("Current network", "85 operational stations across 3 lines. Pink and Blue are shown as upcoming, not live service.")); // Explains the initial scope clearly.

        TextView fromLabel = text("FROM", 13, Color.GRAY); // Creates the source label.
        fromLabel.setTypeface(null, 1); // Makes the label bold.
        root.addView(fromLabel); // Adds the label.
        fromInput = stationInput("Choose starting station"); // Creates the source autocomplete field.
        root.addView(fromInput); // Adds the source field.

        TextView toLabel = text("TO", 13, Color.GRAY); // Creates the destination label.
        toLabel.setTypeface(null, 1); // Makes the label bold.
        LinearLayout.LayoutParams labelTop = new LinearLayout.LayoutParams(-1, -2); // Creates reusable spacing for the destination label.
        labelTop.topMargin = 18; // Adds visual spacing between fields.
        root.addView(toLabel, labelTop); // Adds the destination label.
        toInput = stationInput("Choose destination station"); // Creates the destination autocomplete field.
        root.addView(toInput); // Adds the destination field.

        Button plan = new Button(this); // Creates the route planning button.
        plan.setText("PLAN JOURNEY"); // Gives the button a clear action label.
        LinearLayout.LayoutParams planParams = new LinearLayout.LayoutParams(-1, -2); // Creates full-width button parameters.
        planParams.topMargin = 18; // Separates the button from the destination field.
        root.addView(plan, planParams); // Adds the button to the page.
        plan.setOnClickListener(v -> planJourney()); // Runs route planning when the user taps the button.

        Button swap = new Button(this); // Creates a button for quickly reversing the journey.
        swap.setText("SWAP FROM / TO"); // Labels the reverse-route action.
        root.addView(swap); // Adds the swap button below the plan button.
        swap.setOnClickListener(v -> { String value = fromInput.getText().toString(); fromInput.setText(toInput.getText().toString()); toInput.setText(value); }); // Exchanges the two station fields.

        Button saveFavourite = new Button(this); // Creates a button for saving a favourite station.
        saveFavourite.setText("SAVE START AS FAVOURITE"); // Labels the favourite action.
        root.addView(saveFavourite); // Adds the favourite button to the screen.
        saveFavourite.setOnClickListener(v -> saveFavouriteStation()); // Saves the selected start station locally.

        favouriteText = text("Favourite: none", 14, Color.DKGRAY); // Creates the favourite station status.
        root.addView(favouriteText); // Shows the saved favourite.
        refreshFavourite(); // Loads the saved favourite into the screen.
        Button useFavourite = new Button(this); // Creates a shortcut that fills the saved favourite into FROM.
        useFavourite.setText("USE FAVOURITE AS START"); // Labels the favourite shortcut.
        root.addView(useFavourite); // Adds the shortcut below the favourite label.
        useFavourite.setOnClickListener(v -> useFavouriteStation()); // Fills the saved favourite station when tapped.

        Button stationInfo = new Button(this); // Creates a button for station details.
        stationInfo.setText("SHOW STATION INFO"); // Labels the station-information action.
        root.addView(stationInfo); // Adds the station information button.
        stationInfo.setOnClickListener(v -> showStationInfo()); // Shows line/interchange information for the selected FROM station.

        Button share = new Button(this); // Creates a route sharing button.
        share.setText("SHARE JOURNEY"); // Labels the sharing action.
        root.addView(share); // Adds the sharing button.
        share.setOnClickListener(v -> shareJourney()); // Shares the currently planned route.

        TextView mapTitle = text("NETWORK MAP", 15, Color.DKGRAY); // Creates the map section heading.
        mapTitle.setTypeface(null, 1); // Makes the heading bold.
        LinearLayout.LayoutParams mapTitleParams = new LinearLayout.LayoutParams(-1, -2); // Creates spacing for the map heading.
        mapTitleParams.topMargin = 24; // Adds space before the map.
        root.addView(mapTitle, mapTitleParams); // Adds the map heading.
                root.addView(infoCard("Network overview", "Purple: Whitefield ↔ Challaghatta | Green: Madavara ↔ Silk Institute | Yellow: RV Road ↔ Bommasandra")); // Shows the operational network at a glance.

        LinearLayout quickActions = new LinearLayout(this); // Creates a horizontal area for the most useful v3 shortcuts.
        quickActions.setOrientation(LinearLayout.HORIZONTAL); // Places shortcut buttons side by side.
        Button service = new Button(this); // Creates the official-service shortcut.
        service.setText("SERVICE INFO"); // Labels the official service button.
        quickActions.addView(service, new LinearLayout.LayoutParams(0, -2, 1)); // Gives the service button half of the available width.
        Button maps = new Button(this); // Creates the external maps shortcut.
        maps.setText("OPEN MAPS"); // Labels the maps shortcut.
        quickActions.addView(maps, new LinearLayout.LayoutParams(0, -2, 1)); // Gives the maps button the other half of the width.
        root.addView(quickActions); // Adds the shortcut row to the page.
        service.setOnClickListener(v -> openUrl("https://english.bmrc.co.in/metro-timings/")); // Opens the current official BMRCL timing page.
        maps.setOnClickListener(v -> openMaps()); // Opens a Bengaluru Metro search in the phone's map application.

        TextView mapHeading = text("NETWORK SCHEMATIC", 15, Color.DKGRAY); // Creates the schematic-map heading.
        mapHeading.setTypeface(null, 1); // Makes the map heading bold.
        LinearLayout.LayoutParams mapHeadingParams = new LinearLayout.LayoutParams(-1, -2); // Creates spacing for the map heading.
        mapHeadingParams.topMargin = 22; // Adds space above the schematic.
        root.addView(mapHeading, mapHeadingParams); // Adds the heading.
        root.addView(new MetroMapView(this)); // Adds a dependency-free schematic metro map.


        TextView statusTitle = text("TODAY'S SERVICE", 15, Color.DKGRAY); // Creates the service section heading.
        statusTitle.setTypeface(null, 1); // Makes the heading bold.
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2); // Creates spacing for the service section.
        statusParams.topMargin = 24; // Adds space above the section.
        root.addView(statusTitle, statusParams); // Adds the service heading.
        root.addView(infoCard("Operational hours", "Purple & Green: about 05:00–23:05 (Monday starts earlier). Yellow: about 06:00–23:55; Sunday services start later.")); // Adds current operating-hour guidance.

        TextView routeTitle = text("JOURNEY RESULT", 15, Color.DKGRAY); // Creates the result heading.
        routeTitle.setTypeface(null, 1); // Makes the result heading bold.
        LinearLayout.LayoutParams routeTitleParams = new LinearLayout.LayoutParams(-1, -2); // Creates result heading spacing.
        routeTitleParams.topMargin = 24; // Adds vertical separation.
        root.addView(routeTitle, routeTitleParams); // Adds the heading.

        resultBox = new LinearLayout(this); // Creates the area where route cards will appear.
        resultBox.setOrientation(LinearLayout.VERTICAL); // Stacks multiple result cards vertically.
        root.addView(resultBox); // Adds the dynamic result area.

        TextView trackTitle = text("TRIP TRACKER", 15, Color.DKGRAY); // Creates the trip tracker heading.
        trackTitle.setTypeface(null, 1); // Makes the heading bold.
        LinearLayout.LayoutParams trackTitleParams = new LinearLayout.LayoutParams(-1, -2); // Creates tracker spacing.
        trackTitleParams.topMargin = 24; // Separates the tracker section.
        root.addView(trackTitle, trackTitleParams); // Adds the tracker heading.
        trackerText = text("Plan a journey to activate the station-by-station tracker.", 15, Color.DKGRAY); // Creates the initial tracker message.
        root.addView(trackerText); // Adds the tracker message.
        Button nextStop = new Button(this); // Creates the next-station tracker button.
        nextStop.setText("I REACHED THE NEXT STATION"); // Labels the tracker action.
        root.addView(nextStop); // Adds the tracker button.
        nextStop.setOnClickListener(v -> advanceTracker()); // Advances the tracked trip when tapped.

        TextView footer = text("Data basis: BMRCL information and current public route/timetable sources. This app is independent and not an official BMRCL app.", 12, Color.GRAY); // Creates a transparent data-source note.
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2); // Creates footer spacing.
        footerParams.topMargin = 24; // Separates the footer from the tracker.
        root.addView(footer, footerParams); // Adds the footer.

        setContentView(scroll); // Displays the finished page on the phone.
    }

    private AutoCompleteTextView stationInput(String hint) { // Creates one searchable station field.
        AutoCompleteTextView input = new AutoCompleteTextView(this); // Creates the autocomplete widget.
        input.setHint(hint); // Shows a friendly prompt before typing.
        input.setTextSize(16); // Makes station names easy to read.
        input.setSingleLine(true); // Keeps the field compact.
        input.setPadding(20, 14, 20, 14); // Adds touch-friendly internal spacing.
        ArrayList<String> names = new ArrayList<>(stations.keySet()); // Copies all station names into a list.
        Collections.sort(names); // Sorts stations alphabetically for autocomplete.
        input.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names)); // Installs station autocomplete suggestions.
        return input; // Returns the configured input field.
    }

    private TextView text(String value, float size, int color) { // Creates a text view with common settings.
        TextView view = new TextView(this); // Creates the Android text widget.
        view.setText(value); // Sets the displayed text.
        view.setTextSize(size); // Sets the requested font size.
        view.setTextColor(color); // Sets the requested text color.
        view.setPadding(0, 6, 0, 6); // Adds slight vertical spacing.
        return view; // Returns the configured text view.
    }

    private TextView infoCard(String title, String message) { // Creates a compact information card without extra dependencies.
        TextView card = new TextView(this); // Uses one text widget for a lightweight card.
        card.setText(title + "\n" + message); // Combines the card heading and explanation.
        card.setTextSize(14); // Sets readable card text size.
        card.setTextColor(Color.rgb(35, 42, 48)); // Sets card text color.
        card.setPadding(18, 16, 18, 16); // Makes the card touch-friendly.
        card.setBackgroundColor(Color.WHITE); // Uses a white card over the light background.
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); // Creates full-width card sizing.
        p.topMargin = 10; // Separates cards from nearby content.
        card.setLayoutParams(p); // Applies the card spacing.
        return card; // Returns the information card.
    }

    private void planJourney() { // Finds the best route between the selected stations.
        String fromName = fromInput.getText().toString().trim(); // Reads the source station text.
        String toName = toInput.getText().toString().trim(); // Reads the destination station text.
        if (!stations.containsKey(fromName) || !stations.containsKey(toName)) { // Checks that both names are valid stations.
            Toast.makeText(this, "Select stations from the suggestions.", Toast.LENGTH_SHORT).show(); // Tells the user how to correct the input.
            return; // Stops route planning on invalid input.
        }
        if (fromName.equals(toName)) { // Handles the trivial case of the same station.
            Toast.makeText(this, "Choose two different stations.", Toast.LENGTH_SHORT).show(); // Explains the issue.
            return; // Stops unnecessary routing work.
        }
        Route route = findRoute(stations.get(fromName), stations.get(toName)); // Calculates the best route with transfer penalties.
        resultBox.removeAllViews(); // Clears any previous result.
        if (route == null) { // Checks whether a route was found.
            resultBox.addView(infoCard("No route", "No connected route was found in the current operational network.")); // Shows a useful failure message.
            return; // Ends the method when there is no route.
        }
        TextView summary = text(route.stationNames.size() - 1 + " stops • " + route.transfers + " transfer(s) • ~" + route.estimatedMinutes + " min", 18, Color.rgb(21, 101, 192)); // Builds the route summary.
        summary.setTypeface(null, 1); // Makes the route summary prominent.
        resultBox.addView(summary); // Shows the route summary.
        resultBox.addView(infoCard("Estimated token fare", "₹" + estimateFare(route.stationNames.size() - 1) + "  •  based on stop-distance slabs; verify the fare shown at the station.")); // Shows a transparent fare estimate.
        for (int i = 0; i < route.stationNames.size(); i++) { // Walks through every station on the selected journey.
            String prefix = i == 0 ? "START  " : (i == route.stationNames.size() - 1 ? "END    " : (String.format("%02d     ", i))); // Adds a simple progress marker.
            TextView stop = text(prefix + route.stationNames.get(i) + (i < route.lines.size() ? "  •  " + route.lines.get(i) : ""), 15, Color.DKGRAY); // Creates each route row.
            resultBox.addView(stop); // Adds the station row to the result card.
        }
        activeTrip = new ArrayList<>(route.stationNames); // Stores the route for the in-trip tracker.
        activeTripIndex = 0; // Resets the tracker to the start station.
        trackerText.setText("Current: " + activeTrip.get(0) + "\nNext: " + activeTrip.get(1)); // Shows the first tracker state.
    }

    private void openUrl(String url) { // Opens a web address using any browser available on the phone.
        try { // Starts a protected block so missing browser apps do not crash the tracker.
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); // Launches the requested web address.
        } catch (Exception error) { // Handles devices without a matching browser activity.
            Toast.makeText(this, "Unable to open the page on this device.", Toast.LENGTH_SHORT).show(); // Shows a friendly failure message.
        }
    }

    private void openMaps() { // Opens a map search focused on Namma Metro in Bengaluru.
        String query = Uri.encode("Namma Metro Bengaluru stations"); // Encodes the map search text safely for a URL.
        openUrl("https://www.google.com/maps/search/?api=1&query=" + query); // Sends the search to Google Maps or the default browser.
    }

    private void useFavouriteStation() { // Fills the saved station into the FROM field.
        String favourite = prefs.getString("favourite", ""); // Loads the locally saved favourite station.
        if (favourite.isEmpty() || !stations.containsKey(favourite)) { // Checks that a valid favourite exists.
            Toast.makeText(this, "No valid favourite station is saved.", Toast.LENGTH_SHORT).show(); // Explains why nothing was filled.
            return; // Ends the action when no valid favourite exists.
        }
        fromInput.setText(favourite); // Places the saved station into the source field.
        fromInput.setSelection(fromInput.length()); // Moves the cursor to the end of the inserted station name.
        Toast.makeText(this, "Starting station set to " + favourite, Toast.LENGTH_SHORT).show(); // Confirms the shortcut action.
    }

    private void showStationInfo() { // Displays the metro lines and interchange status for the selected FROM station.
        String stationName = fromInput.getText().toString().trim(); // Reads the station selected in the FROM field.
        Station station = stations.get(stationName); // Looks up the canonical station node.
        if (station == null) { // Checks that a real station was selected.
            Toast.makeText(this, "Select a valid station in FROM first.", Toast.LENGTH_SHORT).show(); // Tells the user what to do.
            return; // Stops the info dialog on invalid input.
        }
        String linesText = String.join(", ", new java.util.TreeSet<>(station.lines)); // Sorts and formats all lines serving this station.
        String interchange = station.lines.size() > 1 ? "Interchange station" : "Single-line station"; // Determines whether the station is an interchange.
        new android.app.AlertDialog.Builder(this) // Creates a native Android information dialog.
                .setTitle(station.name) // Uses the station name as the dialog title.
                .setMessage(interchange + "\n\nLines: " + linesText) // Shows the line membership and interchange status.
                .setPositiveButton("OK", null) // Adds a simple close action.
                .show(); // Displays the dialog immediately.
    }

    private void saveFavouriteStation() { // Saves the selected starting station on the device.
        String station = fromInput.getText().toString().trim(); // Reads the current starting station.
        if (!stations.containsKey(station)) { Toast.makeText(this, "Select a valid starting station first.", Toast.LENGTH_SHORT).show(); return; } // Validates the station before saving it.
        prefs.edit().putString("favourite", station).apply(); // Stores the favourite locally without requiring an account.
        refreshFavourite(); // Refreshes the displayed favourite.
        Toast.makeText(this, "Favourite saved: " + station, Toast.LENGTH_SHORT).show(); // Confirms the save.
    }

    private void refreshFavourite() { // Loads the saved favourite station into the UI.
        if (favouriteText != null) favouriteText.setText("Favourite: " + prefs.getString("favourite", "none")); // Updates the favourite label.
    }

    private void shareJourney() { // Shares the planned journey through any compatible Android app.
        if (activeTrip.size() < 2) { Toast.makeText(this, "Plan a journey first.", Toast.LENGTH_SHORT).show(); return; } // Requires an active route before sharing.
        Intent intent = new Intent(Intent.ACTION_SEND); // Creates the Android share action.
        intent.setType("text/plain"); // Requests plain text sharing.
        intent.putExtra(Intent.EXTRA_TEXT, "Namma Metro journey: " + activeTrip.get(0) + " → " + activeTrip.get(activeTrip.size() - 1) + " | " + (activeTrip.size() - 1) + " stops"); // Creates compact share text.
        startActivity(Intent.createChooser(intent, "Share journey")); // Opens the standard Android share sheet.
    }

    private void advanceTracker() { // Advances the current trip by one station when the passenger confirms movement.
        if (activeTrip.size() < 2) { // Checks whether a trip has been planned.
            Toast.makeText(this, "Plan a journey first.", Toast.LENGTH_SHORT).show(); // Explains why the button cannot advance yet.
            return; // Stops when no active trip exists.
        }
        if (activeTripIndex >= activeTrip.size() - 1) { // Checks whether the destination has already been reached.
            trackerText.setText("Destination reached: " + activeTrip.get(activeTrip.size() - 1)); // Shows completion status.
            return; // Keeps the tracker at the destination.
        }
        activeTripIndex++; // Moves to the next station in the route.
        if (activeTripIndex == activeTrip.size() - 1) trackerText.setText("Current: " + activeTrip.get(activeTripIndex) + "\nDestination reached."); // Shows completion at the destination.
        else trackerText.setText("Current: " + activeTrip.get(activeTripIndex) + "\nNext: " + activeTrip.get(activeTripIndex + 1)); // Otherwise shows the next station.
    }

    private int estimateFare(int stops) { // Estimates the current distance-based token fare using public fare slabs.
        if (stops <= 2) return 10; // Applies the first fare slab.
        if (stops <= 4) return 20; // Applies the second fare slab.
        if (stops <= 6) return 30; // Applies the third fare slab.
        if (stops <= 10) return 40; // Applies the fourth fare slab.
        if (stops <= 14) return 50; // Applies the fifth fare slab.
        if (stops <= 18) return 60; // Applies the sixth fare slab.
        if (stops <= 24) return 70; // Applies the seventh fare slab.
        if (stops <= 28) return 80; // Applies the eighth fare slab.
        return 90; // Caps the estimate at the published upper slab.
    }

    private Route findRoute(Station start, Station goal) { // Finds a least-cost route that balances stops and interchanges.
        PriorityQueue<State> queue = new PriorityQueue<>(Comparator.comparingInt(s -> s.cost)); // Creates the Dijkstra priority queue.
        Map<String, Integer> best = new HashMap<>(); // Stores the cheapest known state cost.
        Map<String, Prev> parent = new HashMap<>(); // Stores the previous state for path reconstruction.
        for (String line : start.lines) { // Allows boarding any line serving the starting station.
            State s = new State(start, line, 0); // Creates the zero-cost starting state for that line.
            queue.add(s); // Inserts the state into Dijkstra.
            best.put(s.key(), 0); // Records its best cost.
        }
        State end = null; // Holds the final best destination state.
        while (!queue.isEmpty()) { // Continues until every useful state has been processed.
            State current = queue.poll(); // Takes the cheapest state from the queue.
            if (current.cost != best.getOrDefault(current.key(), Integer.MAX_VALUE)) continue; // Ignores stale queue entries.
            if (current.station == goal) { end = current; break; } // Stops at the first minimum-cost destination state.
            for (Edge edge : current.station.neighbours) { // Examines all connected adjacent stations.
                int transferPenalty = edge.line.equals(current.line) ? 0 : 8; // Makes unnecessary interchanges more expensive than direct travel.
                int nextCost = current.cost + 1 + transferPenalty; // Adds one stop plus any line-change penalty.
                State next = new State(edge.to, edge.line, nextCost); // Creates the next graph state on this edge's line.
                if (nextCost < best.getOrDefault(next.key(), Integer.MAX_VALUE)) { // Updates the state only when it is cheaper.
                    best.put(next.key(), nextCost); // Stores the new best cost.
                    parent.put(next.key(), new Prev(current, edge.line)); // Stores how to reconstruct the route.
                    queue.add(next); // Pushes the improved state into Dijkstra.
                }
            }
        }
        if (end == null) return null; // Returns no route when Dijkstra could not reach the destination.
        List<String> stationNames = new ArrayList<>(); // Stores reconstructed station names.
        List<String> routeLines = new ArrayList<>(); // Stores the line used to reach each station.
        Set<String> transferMarkers = new HashSet<>(); // Tracks line changes for the transfer count.
        State cursor = end; // Starts reconstruction at the destination.
        while (cursor != null) { // Walks backward until the starting station is reached.
            stationNames.add(cursor.station.name); // Adds the current station.
            routeLines.add(cursor.line); // Adds the line used for the current state.
            Prev p = parent.get(cursor.key()); // Looks up the previous state.
            cursor = p == null ? null : p.previous; // Moves to the preceding state.
        }
        Collections.reverse(stationNames); // Restores forward station order.
        Collections.reverse(routeLines); // Restores forward line order.
        int transfers = 0; // Starts the transfer counter at zero.
        for (int i = 1; i < routeLines.size(); i++) { // Checks each adjacent pair of line states.
            if (!routeLines.get(i).equals(routeLines.get(i - 1))) transfers++; // Counts actual line changes.
        }
        int minutes = (stationNames.size() - 1) * 2 + transfers * 5; // Provides a simple approximate travel time model.
        return new Route(stationNames, routeLines, transfers, Math.max(minutes, 2)); // Returns the completed route result.
    }

    private static class MetroMapView extends View { // Draws a lightweight schematic view of the three operational lines.
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); // Creates one anti-aliased paint object for all map drawing.
        MetroMapView(android.content.Context context) { super(context); setMinimumHeight(260); } // Creates the view and reserves enough space for the schematic.
        @Override protected void onDraw(Canvas canvas) { // Draws the metro schematic whenever Android renders the view.
            super.onDraw(canvas); // Lets the base view draw its background first.
            float width = getWidth(); // Reads the current view width so the map adapts to different phones.
            paint.setStrokeWidth(14f); // Makes the line strokes clearly visible.
            paint.setStrokeCap(Paint.Cap.ROUND); // Rounds the line ends for a cleaner schematic style.
            paint.setColor(Color.rgb(106, 27, 154)); // Uses the Purple Line color.
            canvas.drawLine(40, 50, width - 40, 50, paint); // Draws the Purple Line.
            paint.setColor(Color.rgb(46, 125, 50)); // Uses the Green Line color.
            canvas.drawLine(40, 130, width - 40, 130, paint); // Draws the Green Line.
            paint.setColor(Color.rgb(249, 168, 37)); // Uses the Yellow Line color.
            canvas.drawLine(width * 0.23f, 210, width * 0.82f, 210, paint); // Draws the Yellow Line.
            paint.setColor(Color.WHITE); // Switches the marker fill to white.
            float r = 9f; // Sets the interchange marker radius.
            canvas.drawCircle(width * 0.50f, 50, r, paint); // Marks Majestic on the Purple Line.
            canvas.drawCircle(width * 0.50f, 130, r, paint); // Marks Majestic on the Green Line.
            canvas.drawCircle(width * 0.34f, 210, r, paint); // Marks RV Road on the Yellow Line.
            paint.setColor(Color.DKGRAY); // Uses dark text for labels.
            paint.setTextSize(28f); // Sets a readable label size.
            canvas.drawText("PURPLE", 42, 42, paint); // Labels the Purple Line.
            canvas.drawText("GREEN", 42, 122, paint); // Labels the Green Line.
            canvas.drawText("YELLOW", 42, 202, paint); // Labels the Yellow Line.
            paint.setTextSize(22f); // Shrinks labels for endpoint/interchange annotations.
            canvas.drawText("Whitefield", 40, 82, paint); // Labels the Purple Line origin side.
            canvas.drawText("Challaghatta", width - 150, 82, paint); // Labels the Purple Line destination side.
            canvas.drawText("Madavara", 40, 162, paint); // Labels the Green Line origin side.
            canvas.drawText("Silk Institute", width - 155, 162, paint); // Labels the Green Line destination side.
            canvas.drawText("RV Road", width * 0.20f, 242, paint); // Labels the Yellow Line origin side.
            canvas.drawText("Bommasandra", width * 0.66f, 242, paint); // Labels the Yellow Line destination side.
        }
    }

    private static class Line { // Stores one metro line definition.
        final String name; // Holds the display line name.
        final String colorName; // Holds a simple color label.
        final List<Station> stations = new ArrayList<>(); // Holds stations in line order.
        Line(String name, String colorName) { this.name = name; this.colorName = colorName; } // Creates a line definition.
    }

    private static class Station { // Stores a canonical station node.
        final String name; // Holds the station display name.
        final Set<String> lines = new HashSet<>(); // Holds all metro lines serving this station.
        final List<Edge> neighbours = new ArrayList<>(); // Holds adjacent graph edges.
        Station(String name) { this.name = name; } // Creates a station node.
    }

    private static class Edge { // Stores one directed graph edge.
        final Station to; // Holds the connected destination station.
        final String line; // Holds the metro line used for this edge.
        Edge(Station to, String line) { this.to = to; this.line = line; } // Creates the graph edge.
    }

    private static class State { // Stores a station-plus-line state for Dijkstra.
        final Station station; // Holds the current station.
        final String line; // Holds the current line.
        final int cost; // Holds the weighted route cost.
        State(Station station, String line, int cost) { this.station = station; this.line = line; this.cost = cost; } // Creates a routing state.
        String key() { return station.name + "|" + line; } // Creates a unique state key.
    }

    private static class Prev { // Stores one parent pointer for route reconstruction.
        final State previous; // Holds the preceding state.
        final String viaLine; // Holds the edge line that connected the states.
        Prev(State previous, String viaLine) { this.previous = previous; this.viaLine = viaLine; } // Creates a parent pointer.
    }

    private static class Route { // Stores a complete planned journey.
        final List<String> stationNames; // Holds ordered station names.
        final List<String> lines; // Holds ordered line states.
        final int transfers; // Holds the number of line changes.
        final int estimatedMinutes; // Holds an approximate travel time.
        Route(List<String> stationNames, List<String> lines, int transfers, int estimatedMinutes) { this.stationNames = stationNames; this.lines = lines; this.transfers = transfers; this.estimatedMinutes = estimatedMinutes; } // Creates a route result.
    }
}
