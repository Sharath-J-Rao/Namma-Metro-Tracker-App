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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivityV7 extends Activity {
    private final Map<String,List<String>> stations = new LinkedHashMap<>();
    private final Map<String,JSONObject> lineConfig = new LinkedHashMap<>();
    private Spinner lineSpinner,fromSpinner,toSpinner;
    private TextView fareText,timeText,nextText,lastText,countdownText,noticeText,sourceText;
    private SharedPreferences prefs;
    private final Handler handler = new Handler();

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("namma_metro_v7",MODE_PRIVATE);
        loadFallback();
        buildUi();
        applyLocalConfig();
        refreshConfigFromBackend();
        handler.post(tick);
    }

    private void loadFallback(){
        stations.clear();
        stations.put("Purple Line",Arrays.asList("Whitefield","Hopefarm","Kadugodi Tree Park","Pattandur Agrahara","Sri Sathya Sai Hospital","Nallurhalli","Kundalahalli","Seetharampalya","Hoodi","Garudacharpalya","Singayyanapalya","K.R. Pura","Benniganahalli","Baiyappanahalli","Swami Vivekananda Road","Indiranagar","Halasuru","Trinity","MG Road","Cubbon Park","Vidhana Soudha","Central College","Majestic","City Railway Station","Magadi Road","Hosahalli","Vijayanagara","Attiguppe","Deepanjali Nagar","Mysuru Road","Nayandahalli","RR Nagar","Jnanabharathi","Pattanagere","Kengeri Bus Terminal","Kengeri","Challaghatta"));
        stations.put("Green Line",Arrays.asList("Madavara","Chikkabidarakallu","Manjunathanagara","Nagasandra","Dasarahalli","Jalahalli","Peenya Industry","Peenya","Goraguntepalya","Yeshwanthpur","Sandal Soap Factory","Mahalakshmi","Rajajinagar","Kuvempu Road","Srirampura","Sampige Road","Majestic","Chickpete","KR Market","National College","Lalbagh","South End Circle","Jayanagara","RV Road","Banashankari","JP Nagar","Yelachenahalli","Konanakunte Cross","Doddakallasandra","Vajarahalli","Thalaghattapura","Silk Institute"));
        stations.put("Yellow Line",Arrays.asList("RV Road","Ragigudda","Jayadeva Hospital","BTM Layout","Central Silk Board","Bommanahalli","Hongasandra","Kudlu Gate","Singasandra","Hosa Road","Beratena Agrahara","Electronic City","Infosys Agrahara","Huskur Road","Hebbagodi","Bommasandra"));
        lineConfig.clear();
        fallbackLine("Purple Line","05:00","23:05",8);
        fallbackLine("Green Line","05:00","23:05",8);
        fallbackLine("Yellow Line","05:00","23:00",7);
    }

    private void fallbackLine(String name,String first,String last,int headway){
        JSONObject o=new JSONObject();
        try{o.put("name",name).put("first_train",first).put("last_train",last).put("headway_min",headway);}catch(Exception ignored){}
        lineConfig.put(name,o);
    }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=col();
        root.setPadding(dp(20),dp(16),dp(20),dp(28));
        root.setBackgroundColor(Color.rgb(246,247,250));
        scroll.addView(root);
        TextView title=text("Namma Metro",28,Color.rgb(22,27,35));
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(title);
        root.addView(text("BENGALURU • OFFLINE-FIRST",10,Color.rgb(105,114,126)));
        space(root,18);
        root.addView(text("Select your metro line",22,Color.rgb(22,27,35)));
        root.addView(text("The station lists update when you change the line.",14,Color.rgb(96,106,119)));
        space(root,12);
        LinearLayout lineCard=card(); lineCard.addView(label("METRO LINE"));
        lineSpinner=new Spinner(this); lineCard.addView(lineSpinner,new LinearLayout.LayoutParams(-1,dp(52))); root.addView(lineCard);
        space(root,12);
        LinearLayout journey=card();
        journey.addView(label("FROM")); fromSpinner=new Spinner(this); journey.addView(fromSpinner,new LinearLayout.LayoutParams(-1,dp(52)));
        journey.addView(label("TO")); toSpinner=new Spinner(this); journey.addView(toSpinner,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView swap=action("SWAP STATIONS",false); journey.addView(swap,new LinearLayout.LayoutParams(-1,dp(46))); swap.setOnClickListener(v->swapStations()); root.addView(journey);
        space(root,14);
        LinearLayout info=card();
        TextView heading=text("TRAVEL INFO",18,Color.rgb(22,27,35)); heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD); info.addView(heading);
        fareText=text("Ticket: —",17,Color.rgb(22,27,35)); info.addView(fareText);
        timeText=text("Journey: —",15,Color.rgb(55,65,78)); info.addView(timeText);
        nextText=text("Next train: —",15,Color.rgb(55,65,78)); info.addView(nextText);
        lastText=text("Service: —",15,Color.rgb(55,65,78)); info.addView(lastText);
        countdownText=text("Time until last train: —",18,Color.rgb(18,104,208)); countdownText.setTypeface(Typeface.DEFAULT,Typeface.BOLD); info.addView(countdownText);
        root.addView(info);
        space(root,12);
        LinearLayout status=card();
        noticeText=text("",14,Color.rgb(30,40,50)); status.addView(noticeText);
        sourceText=text("Offline configuration",12,Color.rgb(115,124,136)); status.addView(sourceText); root.addView(status);
        space(root,10);
        LinearLayout actions=row();
        TextView refresh=action("REFRESH",true), backend=action("BACKEND",false);
        actions.addView(refresh,new LinearLayout.LayoutParams(0,dp(50),1)); actions.addView(backend,new LinearLayout.LayoutParams(0,dp(50),1)); root.addView(actions);
        refresh.setOnClickListener(v->refreshConfigFromBackend()); backend.setOnClickListener(v->showBackendDialog());
        TextView disclaimer=text("Approximate timings and manually maintained fares are planning guidance only.",11,Color.rgb(122,130,141)); root.addView(disclaimer);
        setContentView(scroll);
        lineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refreshStations();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        fromSpinner.setOnItemSelectedListener(listener()); toSpinner.setOnItemSelectedListener(listener()); refreshLineSpinner();
    }

    private android.widget.AdapterView.OnItemSelectedListener listener(){return new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refreshInfo();}public void onNothingSelected(android.widget.AdapterView<?> p){}};}

    private void refreshLineSpinner(){
        List<String> names=new ArrayList<>(stations.keySet());
        lineSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
        String saved=prefs.getString("line","Purple Line");
        lineSpinner.setSelection(Math.max(0,names.indexOf(saved)));
        refreshStations();
    }

    private void refreshStations(){
        if(lineSpinner==null)return;
        String line=String.valueOf(lineSpinner.getSelectedItem()); List<String> list=stations.get(line); if(list==null)return;
        setSpinner(fromSpinner,list); setSpinner(toSpinner,list); if(list.size()>1)toSpinner.setSelection(1);
        prefs.edit().putString("line",line).apply(); refreshInfo();
    }

    private void refreshInfo(){
        if(lineSpinner==null||fromSpinner==null||toSpinner==null)return;
        String line=String.valueOf(lineSpinner.getSelectedItem()),from=String.valueOf(fromSpinner.getSelectedItem()),to=String.valueOf(toSpinner.getSelectedItem());
        List<String> list=stations.get(line); if(list==null||!list.contains(from)||!list.contains(to))return;
        int stops=Math.abs(list.indexOf(to)-list.indexOf(from));
        int mins=Math.max(2,Math.round(stops*2.4f));
        fareText.setText("Ticket: ₹"+estimateFare(stops*1.4)+" approx.");
        timeText.setText("Journey: ~"+mins+" min • "+stops+" stops");
        JSONObject cfg=lineConfig.get(line); String first=cfg==null?"05:00":cfg.optString("first_train","05:00"); String last=cfg==null?"23:00":cfg.optString("last_train","23:00"); int headway=cfg==null?8:cfg.optInt("headway_min",8);
        nextText.setText("Next train: ~"+headway+" min typical headway"); lastText.setText("Service: "+first+" – "+last); updateCountdown(last);
        noticeText.setText(prefs.getString("notice","")); sourceText.setText("Config v"+prefs.getInt("config_version",1)+" • last sync: "+prefs.getString("updated_at","local"));
    }

    private int estimateFare(double km){
        String raw=prefs.getString("fares_json","");
        if(!raw.isEmpty()){
            try{
                JSONArray fares=new JSONArray(raw); List<JSONObject> rows=new ArrayList<>();
                for(int i=0;i<fares.length();i++)rows.add(fares.getJSONObject(i));
                rows.sort(Comparator.comparingDouble(o->o.optDouble("max_km",999999)));
                for(JSONObject row:rows)if(km<=row.optDouble("max_km",999999))return row.optInt("fare",95);
            }catch(Exception ignored){}
        }
        int[][] fallback={{2,11},{4,21},{6,32},{8,42},{10,53},{15,63},{20,74},{25,84},{30,90},{999,95}};
        for(int[] s:fallback)if(km<=s[0])return s[1];
        return 95;
    }

    private void updateCountdown(String hhmm){
        try{
            String[] p=hhmm.split(":"); Calendar now=Calendar.getInstance(),target=Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY,Integer.parseInt(p[0])); target.set(Calendar.MINUTE,Integer.parseInt(p[1])); target.set(Calendar.SECOND,0); target.set(Calendar.MILLISECOND,0);
            if(target.before(now))target.add(Calendar.DAY_OF_MONTH,1);
            long mins=Math.max(0,(target.getTimeInMillis()-System.currentTimeMillis())/60000);
            countdownText.setText("Time until last train: "+(mins/60)+"h "+(mins%60)+"m");
        }catch(Exception ignored){countdownText.setText("Time until last train: —");}
    }

    private final Runnable tick=()->{refreshInfo();handler.postDelayed(tick,30000);};

    private void refreshConfigFromBackend(){
        String base=prefs.getString("backend_url","").trim(); if(base.isEmpty())return;
        new Thread(()->{
            try{
                URL url=new URL(base.replaceAll("/$","")+"/api/config"); HttpURLConnection c=(HttpURLConnection)url.openConnection(); c.setConnectTimeout(5000); c.setReadTimeout(7000); c.setRequestMethod("GET");
                if(c.getResponseCode()!=200)throw new IllegalStateException("HTTP "+c.getResponseCode());
                StringBuilder body=new StringBuilder(); try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8))){String s;while((s=r.readLine())!=null)body.append(s);}
                JSONObject json=new JSONObject(body.toString()); applyRemoteConfig(json);
                runOnUiThread(()->{sourceText.setText("Config v"+prefs.getInt("config_version",1)+" • synced from server");refreshLineSpinner();Toast.makeText(this,"Metro configuration updated",Toast.LENGTH_SHORT).show();});
            }catch(Exception ignored){runOnUiThread(()->sourceText.setText("Offline mode • using last saved configuration"));}
        }).start();
    }

    private void applyRemoteConfig(JSONObject json){
        try{
            prefs.edit().putInt("config_version",json.optInt("version",1)).putString("updated_at",json.optString("updated_at","local")).putString("notice",json.optString("notice","")).putString("config_json",json.toString()).apply();
            JSONArray fares=json.optJSONArray("fares"); if(fares!=null)prefs.edit().putString("fares_json",fares.toString()).apply();
            JSONObject lines=json.optJSONObject("lines"); if(lines==null)return; stations.clear(); lineConfig.clear();
            JSONArray names=lines.names(); if(names==null)return;
            for(int i=0;i<names.length();i++){String key=names.getString(i);JSONObject item=lines.getJSONObject(key);String name=item.optString("name",key);List<String> list=new ArrayList<>();JSONArray arr=item.optJSONArray("stations");if(arr!=null)for(int j=0;j<arr.length();j++)list.add(arr.getString(j));stations.put(name,list);lineConfig.put(name,item);}
        }catch(Exception ignored){}
    }

    private void applyLocalConfig(){String raw=prefs.getString("config_json","");if(!raw.isEmpty())try{applyRemoteConfig(new JSONObject(raw));}catch(Exception ignored){} }

    private void showBackendDialog(){
        LinearLayout box=col();box.setPadding(dp(16),dp(4),dp(16),0);box.addView(text("Example: http://192.168.1.50:8000",12,Color.GRAY));
        android.widget.EditText input=new android.widget.EditText(this);input.setSingleLine(true);input.setText(prefs.getString("backend_url",""));input.setHint("http://server:8000");box.addView(input);
        new AlertDialog.Builder(this).setTitle("Backend URL").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{prefs.edit().putString("backend_url",input.getText().toString().trim()).apply();refreshConfigFromBackend();}).show();
    }

    private void swapStations(){int a=fromSpinner.getSelectedItemPosition(),b=toSpinner.getSelectedItemPosition();fromSpinner.setSelection(b);toSpinner.setSelection(a);}
    private void setSpinner(Spinner s,List<String> values){s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));}
    private LinearLayout col(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private LinearLayout card(){LinearLayout v=col();v.setPadding(dp(16),dp(15),dp(16),dp(15));v.setBackground(round(Color.WHITE,18));v.setElevation(dp(1));return v;}
    private TextView text(String v,float size,int color){TextView t=new TextView(this);t.setText(v);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(4));return t;}
    private TextView label(String v){TextView t=text(v,10,Color.rgb(112,121,133));t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setLetterSpacing(.07f);return t;}
    private TextView action(String v,boolean primary){TextView t=text(v,12,primary?Color.WHITE:Color.rgb(45,57,70));t.setGravity(Gravity.CENTER);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setBackground(round(primary?Color.rgb(18,104,208):Color.WHITE,14));t.setClickable(true);return t;}
    private void space(LinearLayout r,int h){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));r.addView(v);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private android.graphics.drawable.GradientDrawable round(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
}
