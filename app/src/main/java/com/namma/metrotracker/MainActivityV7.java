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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Calendar;

public class MainActivityV7 extends Activity {
    private final Map<String, List<String>> stations = new LinkedHashMap<>();
    private final Map<String, JSONObject> lineConfig = new LinkedHashMap<>();
    private Spinner lineSpinner, fromSpinner, toSpinner;
    private TextView fareText, timeText, nextText, lastText, countdownText, noticeText, sourceText, versionText;
    private SharedPreferences prefs;
    private final Handler handler = new Handler();
    private long lastServiceMillis = 0L;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("namma_metro_v7", MODE_PRIVATE);
        loadFallback();
        buildUi();
        applyLocalConfig();
        refreshConfigFromBackend();
        handler.post(tick);
    }

    private void loadFallback() {
        stations.clear();
        stations.put("Purple Line", Arrays.asList("Whitefield","Hopefarm","Kadugodi Tree Park","Pattandur Agrahara","Sri Sathya Sai Hospital","Nallurhalli","Kundalahalli","Seetharampalya","Hoodi","Garudacharpalya","Singayyanapalya","K.R. Pura","Benniganahalli","Baiyappanahalli","Swami Vivekananda Road","Indiranagar","Halasuru","Trinity","MG Road","Cubbon Park","Vidhana Soudha","Central College","Majestic","City Railway Station","Magadi Road","Hosahalli","Vijayanagara","Attiguppe","Deepanjali Nagar","Mysuru Road","Nayandahalli","RR Nagar","Jnanabharathi","Pattanagere","Kengeri Bus Terminal","Kengeri","Challaghatta"));
        stations.put("Green Line", Arrays.asList("Madavara","Chikkabidarakallu","Manjunathanagara","Nagasandra","Dasarahalli","Jalahalli","Peenya Industry","Peenya","Goraguntepalya","Yeshwanthpur","Sandal Soap Factory","Mahalakshmi","Rajajinagar","Kuvempu Road","Srirampura","Sampige Road","Majestic","Chickpete","KR Market","National College","Lalbagh","South End Circle","Jayanagara","RV Road","Banashankari","JP Nagar","Yelachenahalli","Konanakunte Cross","Doddakallasandra","Vajarahalli","Thalaghattapura","Silk Institute"));
        stations.put("Yellow Line", Arrays.asList("RV Road","Ragigudda","Jayadeva Hospital","BTM Layout","Central Silk Board","Bommanahalli","Hongasandra","Kudlu Gate","Singasandra","Hosa Road","Beratena Agrahara","Electronic City","Infosys Agrahara","Huskur Road","Hebbagodi","Bommasandra"));
        lineConfig.clear();
        putFallbackLine("Purple Line","05:00","23:05",8);
        putFallbackLine("Green Line","05:00","23:05",8);
        putFallbackLine("Yellow Line","05:00","23:00",7);
    }

    private void putFallbackLine(String name, String first, String last, int headway) {
        JSONObject line = new JSONObject();
        try { line.put("name", name).put("first_train", first).put("last_train", last).put("headway_min", headway); } catch (Exception ignored) { }
        lineConfig.put(name, line);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = col();
        root.setPadding(dp(20), dp(16), dp(20), dp(28));
        root.setBackgroundColor(Color.rgb(246,247,250));
        scroll.addView(root);

        LinearLayout header = row();
        LinearLayout brand = col();
        TextView title = text("Namma Metro",28,Color.rgb(22,27,35));
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        brand.addView(title);
        brand.addView(text("BENGALURU • SMART COMMUTER",10,Color.rgb(105,114,126)));
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        versionText = text("LOCAL",10,Color.rgb(25,125,70));
        header.addView(pillView("SYNC",Color.rgb(232,247,238),Color.rgb(25,125,70)));
        root.addView(header);

        space(root,18);
        root.addView(text("Select your metro line",22,Color.rgb(22,27,35)));
        root.addView(text("Choose a line first. The station dropdowns update automatically.",14,Color.rgb(96,106,119)));
        space(root,12);

        LinearLayout lineCard = card();
        lineCard.addView(label("METRO LINE"));
        lineSpinner = new Spinner(this);
        lineCard.addView(lineSpinner,new LinearLayout.LayoutParams(-1,dp(52)));
        root.addView(lineCard);

        space(root,12);
        LinearLayout journeyCard = card();
        journeyCard.addView(label("JOURNEY"));
        journeyCard.addView(label("FROM"));
        fromSpinner = new Spinner(this);
        journeyCard.addView(fromSpinner,new LinearLayout.LayoutParams(-1,dp(52)));
        journeyCard.addView(label("TO"));
        toSpinner = new Spinner(this);
        journeyCard.addView(toSpinner,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView swap = action("SWAP STATIONS",false);
        journeyCard.addView(swap,new LinearLayout.LayoutParams(-1,dp(46)));
        swap.setOnClickListener(v -> swapStations());
        root.addView(journeyCard);

        space(root,14);
        LinearLayout info = card();
        TextView h = text("TRAVEL INFO",18,Color.rgb(22,27,35));
        h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        info.addView(h);
        fareText=text("Ticket: —",17,Color.rgb(22,27,35)); info.addView(fareText);
        timeText=text("Journey: —",15,Color.rgb(55,65,78)); info.addView(timeText);
        nextText=text("Next train: scheduled estimate",15,Color.rgb(55,65,78)); info.addView(nextText);
        lastText=text("Last train: —",15,Color.rgb(55,65,78)); info.addView(lastText);
        countdownText=text("Time until last train: —",18,Color.rgb(18,104,208)); countdownText.setTypeface(Typeface.DEFAULT,Typeface.BOLD); info.addView(countdownText);
        root.addView(info);

        space(root,12);
        LinearLayout status = card();
        noticeText=text("",14,Color.rgb(30,40,50)); status.addView(noticeText);
        sourceText=text("Using offline configuration",12,Color.rgb(115,124,136)); status.addView(sourceText);
        root.addView(status);

        space(root,10);
        LinearLayout buttons=row();
        TextView refresh=action("REFRESH",true);
        TextView settings=action("BACKEND",false);
        buttons.addView(refresh,new LinearLayout.LayoutParams(0,dp(50),1));
        buttons.addView(settings,new LinearLayout.LayoutParams(0,dp(50),1));
        root.addView(buttons);
        refresh.setOnClickListener(v->refreshConfigFromBackend());
        settings.setOnClickListener(v->showBackendDialog());

        TextView disclaimer=text("Approximate timings and fares are provided as planning guidance. Service conditions may change.",11,Color.rgb(122,130,141));
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2); np.topMargin=dp(15); root.addView(disclaimer,np);
        setContentView(scroll);

        lineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){ refreshStations(); }
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        fromSpinner.setOnItemSelectedListener(simpleListener());
        toSpinner.setOnItemSelectedListener(simpleListener());
        refreshLineSpinner();
    }

    private android.widget.AdapterView.OnItemSelectedListener simpleListener(){
        return new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){ refreshInfo(); }
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        };
    }

    private void refreshLineSpinner(){
        List<String> names=new ArrayList<>(stations.keySet());
        lineSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
        String saved=prefs.getString("line","Purple Line");
        int index=Math.max(0,names.indexOf(saved));
        lineSpinner.setSelection(index);
        refreshStations();
    }

    private void refreshStations(){
        if(lineSpinner==null) return;
        String line=String.valueOf(lineSpinner.getSelectedItem());
        List<String> list=stations.get(line);
        if(list==null) return;
        setSpinner(fromSpinner,list);
        setSpinner(toSpinner,list);
        if(list.size()>1) toSpinner.setSelection(1);
        prefs.edit().putString("line",line).apply();
        refreshInfo();
    }

    private void refreshInfo(){
        if(fromSpinner==null||toSpinner==null||lineSpinner==null) return;
        String line=String.valueOf(lineSpinner.getSelectedItem());
        String from=String.valueOf(fromSpinner.getSelectedItem());
        String to=String.valueOf(toSpinner.getSelectedItem());
        List<String> list=stations.get(line);
        if(list==null||from.equals("null")||to.equals("null")) return;
        int stops=Math.abs(list.indexOf(to)-list.indexOf(from));
        int minutes=Math.max(2,Math.round(stops*2.4f));
        int fare=estimateFare(stops*1.4);
        fareText.setText("Ticket: ₹"+fare+" approx.");
        timeText.setText("Journey: ~"+minutes+" min • "+stops+" stops");
        JSONObject cfg=lineConfig.get(line);
        String first=cfg==null?"05:00":cfg.optString("first_train","05:00");
        String last=cfg==null?"23:00":cfg.optString("last_train","23:00");
        int headway=cfg==null?8:cfg.optInt("headway_min",8);
        nextText.setText("Next train: ~"+headway+" min headway");
        lastText.setText("Service: "+first+" – "+last);
        updateCountdown(last);
        noticeText.setText(prefs.getString("notice",""));
        sourceText.setText("Config v"+prefs.getInt("config_version",1)+" • last sync: "+prefs.getString("updated_at","local") );
    }

    private int estimateFare(double km){
        List<double[]> slabs=new ArrayList<>();
        slabs.add(new double[]{2,11}); slabs.add(new double[]{4,21}); slabs.add(new double[]{6,32}); slabs.add(new double[]{8,42}); slabs.add(new double[]{10,53}); slabs.add(new double[]{15,63}); slabs.add(new double[]{20,74}); slabs.add(new double[]{25,84}); slabs.add(new double[]{30,90}); slabs.add(new double[]{999,95});
        for(double[] s:slabs) if(km<=s[0]) return (int)s[1];
        return 95;
    }

    private void updateCountdown(String hhmm){
        try{
            String[] parts=hhmm.split(":");
            Calendar now=Calendar.getInstance();
            Calendar target=Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY,Integer.parseInt(parts[0])); target.set(Calendar.MINUTE,Integer.parseInt(parts[1])); target.set(Calendar.SECOND,0); target.set(Calendar.MILLISECOND,0);
            if(target.before(now)) target.add(Calendar.DAY_OF_MONTH,1);
            lastServiceMillis=target.getTimeInMillis();
            long mins=Math.max(0,(lastServiceMillis-System.currentTimeMillis())/60000);
            countdownText.setText("Time until last train: "+(mins/60)+"h "+(mins%60)+"m");
        }catch(Exception ignored){ countdownText.setText("Time until last train: —"); }
    }

    private final Runnable tick=()->{ refreshInfo(); handler.postDelayed(tick,30000); };

    private void refreshConfigFromBackend(){
        String base=prefs.getString("backend_url","").trim();
        if(base.isEmpty()) return;
        new Thread(()->{
            try{
                URL url=new URL(base.replaceAll("/$","")+"/api/config");
                HttpURLConnection c=(HttpURLConnection)url.openConnection(); c.setConnectTimeout(5000); c.setReadTimeout(7000); c.setRequestMethod("GET");
                if(c.getResponseCode()!=200) throw new IllegalStateException("HTTP "+c.getResponseCode());
                StringBuilder body=new StringBuilder();
                try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8))){ String line; while((line=r.readLine())!=null) body.append(line); }
                JSONObject json=new JSONObject(body.toString());
                applyRemoteConfig(json);
                runOnUiThread(()->{ sourceText.setText("Config v"+prefs.getInt("config_version",1)+" • synced from server"); refreshLineSpinner(); Toast.makeText(this,"Metro data updated",Toast.LENGTH_SHORT).show(); });
            }catch(Exception e){ runOnUiThread(()->sourceText.setText("Offline mode • using last saved configuration")); }
        }).start();
    }

    private void applyRemoteConfig(JSONObject json){
        try{
            prefs.edit().putInt("config_version",json.optInt("version",1)).putString("updated_at",json.optString("updated_at","local")).putString("notice",json.optString("notice","")).putString("config_json",json.toString()).apply();
            JSONArray fare=json.optJSONArray("fares");
            if(fare!=null){ prefs.edit().putString("fares_json",fare.toString()).apply(); }
            JSONObject lines=json.optJSONObject("lines");
            if(lines==null) return;
            stations.clear(); lineConfig.clear();
            for(String key:JSONObject.getNames(lines)){
                JSONObject item=lines.getJSONObject(key); String name=item.optString("name",key); List<String> list=new ArrayList<>(); JSONArray arr=item.optJSONArray("stations"); if(arr!=null) for(int i=0;i<arr.length();i++) list.add(arr.getString(i)); stations.put(name,list); lineConfig.put(name,item);
            }
        }catch(Exception ignored){}
    }

    private void applyLocalConfig(){
        String raw=prefs.getString("config_json","");
        if(raw.isEmpty()) return;
        try{ applyRemoteConfig(new JSONObject(raw)); }catch(Exception ignored){}
        versionText.setText("v"+prefs.getInt("config_version",1));
    }

    private void showBackendDialog(){
        LinearLayout box=col(); box.setPadding(dp(16),dp(4),dp(16),0);
        TextView help=text("Enter the server URL once. Example: http://192.168.1.50:8000",12,Color.GRAY); box.addView(help);
        android.widget.EditText input=new android.widget.EditText(this); input.setSingleLine(true); input.setText(prefs.getString("backend_url","")); input.setHint("http://server:8000"); box.addView(input);
        new AlertDialog.Builder(this).setTitle("Backend URL").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{ prefs.edit().putString("backend_url",input.getText().toString().trim()).apply(); refreshConfigFromBackend(); }).show();
    }

    private void swapStations(){ int a=fromSpinner.getSelectedItemPosition(),b=toSpinner.getSelectedItemPosition(); fromSpinner.setSelection(b); toSpinner.setSelection(a); }
    private void setSpinner(Spinner s,List<String> values){ s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values)); }
    private LinearLayout col(){ LinearLayout v=new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row(){ LinearLayout v=new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout card(){ LinearLayout v=col(); v.setPadding(dp(16),dp(15),dp(16),dp(15)); v.setBackground(round(Color.WHITE,18)); v.setElevation(dp(1)); return v; }
    private TextView text(String v,float size,int color){ TextView t=new TextView(this); t.setText(v); t.setTextSize(size); t.setTextColor(color); t.setPadding(0,dp(4),0,dp(4)); return t; }
    private TextView label(String v){ TextView t=text(v,10,Color.rgb(112,121,133)); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setLetterSpacing(.07f); return t; }
    private TextView action(String v,boolean primary){ TextView t=text(v,12,primary?Color.WHITE:Color.rgb(45,57,70)); t.setGravity(Gravity.CENTER); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setBackground(round(primary?Color.rgb(18,104,208):Color.WHITE,14)); t.setClickable(true); return t; }
    private TextView pillView(String v,int bg,int fg){ TextView t=text(v,10,fg); t.setGravity(Gravity.CENTER); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setPadding(dp(10),dp(8),dp(10),dp(8)); t.setBackground(round(bg,999)); return t; }
    private android.graphics.drawable.GradientDrawable round(int color,int radius){ android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private void space(LinearLayout r,int h){ View v=new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h))); r.addView(v); }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }
}
