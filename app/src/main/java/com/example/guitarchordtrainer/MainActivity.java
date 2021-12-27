package com.example.guitarchordtrainer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements MyRecyclerViewAdapter.ItemClickListener {

    MyRecyclerViewAdapter adapter;

    ArrayList<Chord> chords = new ArrayList<Chord>(); //stores all chords
    Map<String, ArrayList<Chord>> chordGroups = new HashMap<String, ArrayList<Chord>>(); //String is for the name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setTitle("Select Chords to Practice");

        //read chords from csv
        InputStream is = getResources().openRawResource(R.raw.chords);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        try {
            line = br.readLine(); //ignore the headings row
            while ((line = br.readLine()) != null) {
                String data[] = line.split(",");
                double pcp[] = new double[12];
                for (int i = 0; i < 12; i++) pcp[i] = Double.parseDouble(data[i + 1]);
                chords.add(new Chord(data[0], pcp, data[13]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Create pre-defined chord groups
        chordGroups.put("Beginner Open Chords", new ArrayList<Chord>());
        chordGroups.put("Major Chords", new ArrayList<Chord>());
        chordGroups.put("Minor Chords", new ArrayList<Chord>());
        chordGroups.put("Dominant 7th", new ArrayList<Chord>());
        chordGroups.put("Major 7th", new ArrayList<Chord>());
        chordGroups.put("Minor 7th", new ArrayList<Chord>());

        //Populate said groups
        for (int i = 0; i < chords.size(); i++)
        {
            if (i < 12) chordGroups.get("Major Chords").add(chords.get(i));
            else if (i < 24) chordGroups.get("Minor Chords").add(chords.get(i));
            else if (i < 36) chordGroups.get("Dominant 7th").add(chords.get(i));
            else if (i < 48) chordGroups.get("Minor 7th").add(chords.get(i));
            else if (i < 60) chordGroups.get("Major 7th").add(chords.get(i));
        }

        //Beginner open chords have been chosen by me
        chordGroups.get("Beginner Open Chords").add(chords.get(0));
        chordGroups.get("Beginner Open Chords").add(chords.get(3));
        chordGroups.get("Beginner Open Chords").add(chords.get(5));
        chordGroups.get("Beginner Open Chords").add(chords.get(7));
        chordGroups.get("Beginner Open Chords").add(chords.get(10));
        chordGroups.get("Beginner Open Chords").add(chords.get(12));
        chordGroups.get("Beginner Open Chords").add(chords.get(17));
        chordGroups.get("Beginner Open Chords").add(chords.get(19));

        //Create text for recycler view, its important that the text is the same as the keys in the map
        ArrayList<String> chordGroupsText = new ArrayList<>();
        chordGroupsText.add("Beginner Open Chords");
        chordGroupsText.add("Major Chords");
        chordGroupsText.add("Minor Chords");
        chordGroupsText.add("Dominant 7th");
        chordGroupsText.add("Major 7th");
        chordGroupsText.add("Minor 7th");

        RecyclerView recyclerView = findViewById(R.id.mainRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRecyclerViewAdapter(this, chordGroupsText);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    public void openGameActivity(String selectedChordGroup) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("selectedChords", chordGroups.get(selectedChordGroup));
        intent.putExtra("chords", chords);
        startActivity(intent);
    }

    @Override
    public void onItemClick(View view, final int position) {
        //Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();

        String message = "Continue with the following chords? ";
        ArrayList<Chord> chordsInGroup = chordGroups.get(adapter.getItem(position));
        for (Chord c : chordsInGroup) {
            System.out.println(c);
            message += c.name + ", ";
        }
        message = message.substring(0, message.length() - 2);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setCancelable(true);

        builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                openGameActivity(adapter.getItem(position));
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //cancels
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}