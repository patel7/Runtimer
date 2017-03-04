package com.android_examples.stopwatch;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.os.Handler;

import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // Talal comment test
    // Ryan comment test
    TextView textView ;

    Button start, pause, reset, lap ;

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;

    Handler handler;

    int Seconds, Minutes, MilliSeconds ;

    ListView listView ;

    String[] ListElements = new String[] {  };

    List<String> ListElementsArrayList ;

    ArrayAdapter<String> adapter ;

    boolean alreadyExists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        start = (Button) findViewById(R.id.button);
        pause = (Button) findViewById(R.id.button2);
        reset = (Button) findViewById(R.id.button3);
        lap = (Button) findViewById(R.id.button4);
        listView = (ListView) findViewById(R.id.listview1);

        handler = new Handler();

        ListElementsArrayList = new ArrayList<String>(Arrays.asList(ListElements));

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("/Year/" + "2016/" + "ID/0");
        myRef.setValue(null);

        adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1,
                ListElementsArrayList
        );

        listView.setAdapter(adapter);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                StartTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);

                reset.setEnabled(false);
                start.setEnabled(false);
                pause.setEnabled(true);




            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TimeBuff += MillisecondTime;

                handler.removeCallbacks(runnable);

                reset.setEnabled(true);
                start.setEnabled(true);
                pause.setEnabled(false);

            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                MillisecondTime = 0L;
                StartTime = 0L;
                TimeBuff = 0L;
                UpdateTime = 0L;
                Seconds = 0;
                Minutes = 0;
                MilliSeconds = 0;

                textView.setText("00:00:00");

                ListElementsArrayList.clear();

                adapter.notifyDataSetChanged();
            }
        });

        lap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ListElementsArrayList.add(textView.getText().toString());

                adapter.notifyDataSetChanged();

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Test to ensure correct
                // System.out.println(ListElementsArrayList.get(position));
                showAlert(position);

            }
        });

    }

    public void showAlert(final int timeTapped) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Input ID");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        alert.setView(input);

        alert.setPositiveButton("10K", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String idInput = input.getText().toString();

                // Console print for testing, to be removed
                System.out.println(ListElementsArrayList.get(timeTapped) + " " + idInput);

                // Upload to database
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput);

                alreadyExists = false;
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if(snapshot.exists()) {
                            alreadyExists = true;
                        }
                    }

                    public void onCancelled(DatabaseError error) {
                    }
                });

                if(alreadyExists) {
                    System.out.println("ID Number " + idInput + " already exists!");
                } else {
                    myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/Time");
                    myRef.setValue("" + ListElementsArrayList.get(timeTapped));


                    myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/ID");
                    myRef.setValue("" + idInput);

                    myRef = database.getReference("/Year/" + "2016/" +"ID/" + idInput + "/Name");
                    myRef.setValue("Name " + idInput);

                    myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/RaceType");
                    myRef.setValue("10kRace");
                }

            }
        });

        alert.setNegativeButton("5K", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String idInput = input.getText().toString();
                System.out.println(ListElementsArrayList.get(timeTapped) + " " + idInput);

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/Time");

                myRef.setValue("" + ListElementsArrayList.get(timeTapped));


                myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/ID");
                myRef.setValue("" + idInput);

                myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/Name");
                myRef.setValue("Name " + idInput);

                myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/RaceType");
                myRef.setValue("5kRace");
            }
        });

        alert.show();
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            textView.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);



        }

    };

}
