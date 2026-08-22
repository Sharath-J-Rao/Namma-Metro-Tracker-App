package com.namma.metrotracker; // Keeps the corrected launcher inside the app package.

import android.os.Bundle; // Provides activity lifecycle state.
import android.widget.ArrayAdapter; // Supplies the metro-line dropdown values.
import android.widget.Spinner; // Represents the line dropdown.
import java.lang.reflect.Field; // Lets this small wrapper reach the private v6 dropdown.
import java.lang.reflect.Method; // Lets this wrapper refresh the private station-selection logic.
import java.util.Arrays; // Creates the three-line list compactly.

public class MainActivityV6Fixed extends MainActivityV6 { // Reuses the complete v6 UI and only fixes initialization ordering.
    @Override public void onCreate(Bundle state) { // Runs when Android launches the corrected v6 screen.
        super.onCreate(state); // Builds the full v6 screen first.
        try { // Protects the launcher from reflection failures.
            Field field = MainActivityV6.class.getDeclaredField("lineSpinner"); // Finds the private line dropdown created by v6.
            field.setAccessible(true); // Allows this compatibility wrapper to access the field.
            Spinner spinner = (Spinner) field.get(this); // Retrieves the actual line dropdown widget.
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList("Purple Line", "Green Line", "Yellow Line")); // Creates the requested line choices.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // Uses Android's normal expanded dropdown rows.
            spinner.setAdapter(adapter); // Populates the line dropdown so the user can actually select a line.
            Method refresh = MainActivityV6.class.getDeclaredMethod("updateStationDropdowns"); // Finds the existing private station-refresh method.
            refresh.setAccessible(true); // Allows this wrapper to invoke the method.
            refresh.invoke(this); // Rebuilds FROM and TO using the selected line.
        } catch (Exception ignored) { } // Leaves the already-built offline screen available if a compatibility lookup fails.
    }
}
