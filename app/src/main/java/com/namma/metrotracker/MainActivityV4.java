package com.namma.metrotracker;

import android.app.Activity;
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
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivityV4 extends Activity {
    private Spinner lineSpinner, fromSpinner, toSpinner;
    private TextView fareText, timingText, nextText, lastText, countdownText, tripText;
    private String line = "Purple Line";
    private final Handler handler = new Handler();
    private final Map<String,String[]> data = new LinkedHashMap<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadStations();
        buildUi();
        refreshStations();
        refreshInfo();
        handler.post(tick);
    }

    private void loadStations() {
        data.put("Purple Line", new String[]{"Whitefield","Hopefarm","Kadugodi Tree Park","Pattandur Agrahara","Sri Sathya Sai Hospital","Nallurhalli","Kundalahalli","Seetharampalya","Hoodi","Garudacharpalya","Singayyanapalya","K.R. Pura","Benniganahalli","Baiyappanahalli","Swami Vivekananda Road","Indiranagar","Halasuru","Trinity","MG Road","Cubbon Park","Vidhana Soudha","Central College","Majestic","City Railway Station","Magadi Road","Hosahalli","Vijayanagara","Attiguppe","Deepanjali Nagar","Mysuru Road","Nayandahalli","RR Nagar","Jnanabharathi","Pattanagere","Kengeri Bus Terminal","Kengeri","Challaghatta"});
        data.put("Green Line", new String[]{"Madavara","Chikkabidarakallu","Manjunathanagara","Nagasandra","Dasarahalli","Jalahalli","Peenya Industry","Peenya","Goraguntepalya","Yeshwanthpur","Sandal Soap Factory","Mahalakshmi","Rajajinagar","Kuvempu Road","Srirampura","Sampige Road","Majestic","Chickpete","KR Market","National College","Lalbagh","South End Circle","Jayanagara","RV Road","Banashankari","JP Nagar","Yelachenahalli","Konanakunte Cross","Doddakallasandra","Vajarahalli","Thalaghattapura","Silk Institute"});
        data.put("Yellow Line", new String[]{"RV Road","Ragigudda","Jayadeva Hospital","BTM Layout","Central Silk Board","Bommanahalli","Hongasandra","Kudlu Gate","Singasandra","Hosa Road","Beratena Agrahara","Electronic City","Infosys Agrahara","Huskur Road","Hebbagodi","Bommasandra"});
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = col();
        root.setPadding(dp(20),dp(18),dp(20),dp(28));
        root.setBackgroundColor(Color.rgb(246,247,250));
        scroll.addView(root);

        TextView title = text("Namma Metro",28,Color.rgb(25,30,38));
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        root.addView(title);
        root.addView(text("BENGALURU • CHOOSE A LINE, THEN A STATION",11,Color.rgb(105,114,126)));
        space(root,16);

        LinearLayout lineCard = card();
        lineCard.addView(label("METRO LINE"));
        lineSpinner = spinner(new String[]{"Purple Line","Green Line","Yellow Line"});
        lineCard.addView(lineSpinner);
        root.addView(lineCard);

        space(root,12);
        LinearLayout stationCard = card();
        stationCard.addView(label("START STATION"));
        fromSpinner = new Spinner(this);
        stationCard.addView(fromSpinner,new LinearLayout.LayoutParams(-1,dp(52)));
        stationCard.addView(label("DESTINATION STATION"));
        toSpinner = new Spinner(this);
        stationCard.addView(toSpinner,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView swap = action("SWAP STATIONS",false);
        stationCard.addView(swap,new LinearLayout.LayoutParams(-1,dp(46)));
        swap.setOnClickListener(v -> swap());
        root.addView(stationCard);

        space(root,14);
        LinearLayout info = card();
        TextView head = text("TODAY'S METRO INFO",18,Color.rgb(25,30,38));
        head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        info.addView(head);
        fareText=text("Ticket fare: —",17,Color.rgb(25,30,38)); info.addView(fareText);
        nextText=text("Next train: —",15,Color.rgb(54,64,76)); info.addView(nextText);
        timingText=text("Service: —",14,Color.rgb(54,64,76)); info.addView(timingText);
        lastText=text("Last scheduled train: —",14,Color.rgb(54,64,76)); info.addView(lastText);
        countdownText=text("Time until last train: —",18,Color.rgb(18,104,208));
        countdownText.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        info.addView(countdownText);
        root.addView(info);

        space(root,12);
        LinearLayout trip = card();
        trip.addView(text("JOURNEY",18,Color.rgb(25,30,38)));
        tripText=text("Select your stations to see the journey.",14,Color.rgb(70,80,92));
        trip.addView(tripText);
        TextView start=action("START TRIP",true);
        trip.addView(start,new LinearLayout.LayoutParams(-1,dp(50)));
        start.setOnClickListener(v -> startTrip());
        root.addView(trip);

        space(root,12);
        TextView note=text("Fare is an estimate based on the current published distance slabs; timings can change. Use BMRCL's official service information for final confirmation.",11,Color.rgb(115,124,136));
        root.addView(note);
        setContentView(scroll);

        lineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){ line=p.getItemAtPosition(pos).toString(); refreshStations(); }
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        fromSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){ public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){ refreshInfo(); } public void onNothingSelected(android.widget.AdapterView<?> p){} });
        toSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){ public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){ refreshInfo(); } public void onNothingSelected(android.widget.AdapterView<?> p){} });
    }

    private void refreshStations(){
        String[] stations=data.get(line);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,stations);
        fromSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,stations));
        toSpinner.setAdapter(a);
        if(stations.length>1) toSpinner.setSelection(1);
        refreshInfo();
    }

    private void refreshInfo(){
        if(fromSpinner==null||toSpinner==null||fromSpinner.getSelectedItem()==null)return;
        int from=fromSpinner.getSelectedItemPosition(), to=toSpinner.getSelectedItemPosition();
        int stops=Math.abs(to-from);
        int fare=estimateFare(stops);
        fareText.setText("Ticket fare: ₹"+fare+" approx.");
        int travel=Math.max(2,(int)Math.ceil(stops*2.2));
        tripText.setText(fromSpinner.getSelectedItem()+" → "+toSpinner.getSelectedItem()+"  •  "+stops+" stops  •  ~"+travel+" min");
        if(line.equals("Yellow Line")){ timingText.setText("Service: about 06:00–23:55"); lastText.setText("Last scheduled train: about 23:55"); }
        else { timingText.setText("Service: about 05:00–23:05"); lastText.setText("Last scheduled train: about 23:05"); }
        nextText.setText("Next train: scheduled headway estimate ~5–10 min");
        updateCountdown();
    }

    private int estimateFare(int stops){
        double km=Math.max(0.5,stops*1.15);
        if(km<=2)return 11; if(km<=4)return 21; if(km<=6)return 32; if(km<=8)return 42;
        if(km<=10)return 53; if(km<=15)return 63; if(km<=20)return 74; if(km<=25)return 84;
        return 95;
    }

    private void updateCountdown(){
        Calendar now=Calendar.getInstance();
        Calendar last=(Calendar)now.clone();
        if(line.equals("Yellow Line")){last.set(Calendar.HOUR_OF_DAY,23);last.set(Calendar.MINUTE,55);}
        else {last.set(Calendar.HOUR_OF_DAY,23);last.set(Calendar.MINUTE,5);}
        last.set(Calendar.SECOND,0);last.set(Calendar.MILLISECOND,0);
        long diff=last.getTimeInMillis()-now.getTimeInMillis();
        if(diff<0) countdownText.setText("Time until last train: service ended for today");
        else countdownText.setText("Time until last train: "+(diff/3600000)+"h "+((diff/60000)%60)+"m");
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (countdownText != null) {
                updateCountdown();
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void swap(){ int a=fromSpinner.getSelectedItemPosition(), b=toSpinner.getSelectedItemPosition(); fromSpinner.setSelection(b); toSpinner.setSelection(a); refreshInfo(); }
    private void startTrip(){Toast.makeText(this,"Trip started from "+fromSpinner.getSelectedItem()+" to "+toSpinner.getSelectedItem(),Toast.LENGTH_SHORT).show();}

    private LinearLayout col(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout card(){LinearLayout v=col();v.setPadding(dp(16),dp(14),dp(16),dp(14));v.setBackground(round(Color.WHITE,18));v.setElevation(dp(1));return v;}
    private TextView text(String s,float size,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(c);v.setPadding(0,dp(5),0,dp(5));return v;}
    private TextView label(String s){TextView v=text(s,10,Color.rgb(108,118,131));v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private Spinner spinner(String[] items){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items));s.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(54)));return s;}
    private TextView action(String s,boolean primary){TextView v=text(s,12,primary?Color.WHITE:Color.rgb(45,56,69));v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setBackground(round(primary?Color.rgb(18,104,208):Color.rgb(244,246,249),14));v.setClickable(true);return v;}
    private android.graphics.drawable.GradientDrawable round(int c,int r){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private void space(LinearLayout root,int h){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));root.addView(v);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
