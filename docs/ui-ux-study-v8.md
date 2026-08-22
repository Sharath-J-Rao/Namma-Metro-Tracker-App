# Namma Metro Tracker v8.1 — UX Review

## Important note

This is a **simulated usability study**, not a real recruitment study. The 400-person sample is modeled to stress-test the interface across likely Bengaluru metro rider personas.

## Simulated panel

400 participants across:
- 120 daily commuters
- 80 students
- 60 occasional riders
- 50 first-time Bengaluru metro riders
- 40 airport/visitor riders
- 30 senior riders
- 20 accessibility-focused riders

## Expert review roles

1. Transit product designer
2. Mobile interaction designer
3. Information architect
4. Accessibility specialist
5. Visual identity designer
6. Cartography / transit-map designer
7. Motion / interaction designer
8. Behavioral UX researcher
9. Android usability specialist
10. Bengaluru commuter domain reviewer

## Simulated tasks

- Select a line and two stations.
- Understand the fare and journey time.
- Find the next train estimate.
- Open the complete metro map.
- Focus the map on the chosen journey.
- Find moving trains.
- Return home without losing the journey.

## Primary findings

### P1 — Journey planning must be the dominant action
The old "Show Journey" action provided feedback but no journey result. That violates the user expectation created by the button label.

**Change:** the action now opens a dedicated Journey page with route, stop count, fare, duration, service times, last-train countdown and route strip.

### P1 — Users do not want developer/infrastructure information
Terms such as GTFS, backend, model, GPS telemetry and sync status were removed from the customer UI.

**Change:** customer-facing screens show only useful trip information. Technical implementation remains in the backend/repository.

### P1 — Metro maps are read as navigation tools, not decoration
A generic line sketch is insufficient.

**Change:** the map now uses a schematic-network treatment with all operational lines, interchange hubs, station markers and moving train markers. The journey route can be focused on the selected segment.

### P1 — Live train positions must be visually obvious
The train layer should dominate the map rather than appearing as a secondary legend item.

**Change:** animated train markers are rendered directly on each line and refresh continuously.

### P2 — Page distinction should be immediate
Each primary page now has a distinct title/subtitle and a persistent four-item navigation bar:

Home | Journey | Map | Live

### P2 — Brand recognition needs stronger ownership
The app is consistently named **Namma Metro Tracker** and uses the requested palette:

- #2FF2CA
- #F2C072
- #7884D9
- #14A7CF

## Outcome target

The redesign is considered successful when a new rider can complete the core task — line → from → to → understand journey — without explanatory text or developer-oriented terminology.
