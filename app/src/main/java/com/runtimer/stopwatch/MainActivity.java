package com.runtimer.stopwatch;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Toast;

import com.runtimer.stopwatch.R;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // Fields for displaying the current time (stopwatch)
    TextView textView;
    Button start, pause, reset, lap;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    Handler handler;
    int Seconds, Minutes, MilliSeconds;

    // Fields for the list of times
    ListView listView;
    String[] ListElements = new String[]{};
    List<String> ListElementsArrayList;
    ArrayAdapter<String> adapter;

    // Fields for temporary storage
    String ids = "";
    String name;

    boolean alreadyExists;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up views for buttons, timer, and list of times
        textView = (TextView) findViewById(R.id.textView);
        start = (Button) findViewById(R.id.button);
        pause = (Button) findViewById(R.id.button2);
        reset = (Button) findViewById(R.id.button3);
        lap = (Button) findViewById(R.id.button4);
        listView = (ListView) findViewById(R.id.listview1);

        handler = new Handler();

        // Initialize list of stored times and adapter for displaying them
        ListElementsArrayList = new ArrayList<String>(Arrays.asList(ListElements));

        adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1,
                ListElementsArrayList
        );

        listView.setAdapter(adapter);

        // Start button listener; disables start and reset buttons, enables pause button
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

        // Pause button listener; disables pause button, re-enables other two buttons
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

        // Reset button listener; doesn't change which buttons are enabled
        // Resets timer, clears list of times
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

                ids = "";
            }
        });

        // Record time or "lap" button listener; adds a time to the list of times
        lap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ListElementsArrayList.add(textView.getText().toString());
                adapter.notifyDataSetChanged();

            }
        });

        // Listener for the list of times itself, so that if one is tapped we can send that time
        // to database.  Refers to the time that was tapped by index in the ArrayList of times
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Test to ensure correct
                // System.out.println(ListElementsArrayList.get(position));
                namePrompt(position);

            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // A prompt that asks for the name of the runner via text entry
    // Has an "OK" and "Cancel" option
    // If the name entered is empty, cancels and displays a toast
    // Otherwise, calls showAlert to ask for the ID number next
    // Parameter represents the index of the time that was tapped to produce
    // this dialog
    public void namePrompt(final int timeTapped) {

        // Set up dialog
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        // Dialog title
        alert.setTitle("Input Runner Name");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        alert.setView(input);

        // Positive button, to submit name
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String nameInput = input.getText().toString();
                name = nameInput;

                // If the name is empty, display a toast and do nothing else
                if(name.isEmpty()) {
                    nameEmpty();
                }
                // Otherwise, call the prompt for an ID number
                else {
                    showAlert(timeTapped);
                }
            }
        });

        // Negative button, to cancel out
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                name = "";
                dialog.cancel();
            }
        });
        alert.show();
    }


    // Prompt for ID number.  Has two options, 5K and 10K
    // Both submit a time for their respective race, but dialog can still be canceled using
    // the Android back button
    public void showAlert(final int timeTapped) {

        // Set up dialog
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        // Dialog title
        alert.setTitle("Input ID");
        final EditText input = new EditText(this);
        // Set up for numerical entry via virtual numpad only
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        alert.setView(input);

        // Listener for 10K button
        alert.setPositiveButton("10K", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Get number that was entered
                final String idInput = input.getText().toString();

                // If the ID input is empty, display a toast and do nothing else
                if(idInput.isEmpty()) {
                    idEmpty();
                } else {

                    // Check if the time already has a checkmark at the end
                    // Sets a boolean according to whether it does or not, and if it does,
                    // removes the checkmark and trailing spaces
                    boolean wasSent;
                    if(ListElementsArrayList.get(timeTapped).contains("✓")) {
                        String newTime = ListElementsArrayList.get(timeTapped).substring(0,ListElementsArrayList.get(timeTapped).length() - 3);
                        ListElementsArrayList.set(timeTapped,newTime);
                        wasSent = true;
                    } else {
                        wasSent = false;
                    }

                    // Set up database references
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference myRef;

                    // Check local memory to see if ID entered was already sent from this phone
                    if (ids.contains("ID=" + idInput + ",")) {

                        // If so, display a toast
                        duplicateIDToast();

                        // Restore the checkmark to the time if it had one when it was tapped
                        if(wasSent) {
                            checkMark(timeTapped);
                        }

                    } else {
                        //If the ID entered was not a duplicate, send all the data to the database

                        // Send the ID entered
                        myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/ID");
                        myRef.setValue("" + idInput);

                        // Send the time that was tapped
                        myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/Time");
                        myRef.setValue("00:" + ListElementsArrayList.get(timeTapped));

                        // Send the name that was entered
                        myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/Name");
                        myRef.setValue(name);

                        // Send the type of race that was picked
                        myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/RaceType");
                        myRef.setValue("10kRace");

                        // Add the ID that was entered to the local list of IDs
                        ids += "ID=" + idInput + ",";

                        // Add a checkmark to the end of the time
                        checkMark(timeTapped);
                    }
                }
            }
        });

        // Listener for 5K button. Almost identical to 10K button, but code is separate in case
        // database is reformatted/separated in future, in which case the 5K and 10K may have to be
        // handled very differently
        alert.setNegativeButton("5K", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                final String idInput = input.getText().toString();

                if(idInput.isEmpty()) {
                    idEmpty();
                    dialog.cancel();
                } else {
                    boolean wasSent;
                    if(ListElementsArrayList.get(timeTapped).contains("✓")) {
                        String newTime = ListElementsArrayList.get(timeTapped).substring(0,ListElementsArrayList.get(timeTapped).length() - 3);
                        ListElementsArrayList.set(timeTapped,newTime);
                        wasSent = true;
                    } else {
                        wasSent = false;
                    }

                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference myRef;

                    if (ids.contains("ID=" + idInput + ",")) {
                        duplicateIDToast();

                        if(wasSent) {
                            checkMark(timeTapped);
                        }
                    } else {
                        myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/ID");
                        myRef.setValue("" + idInput);

                        myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/Time");
                        myRef.setValue("00:" + ListElementsArrayList.get(timeTapped));

                        myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/Name");
                        myRef.setValue(name);

                        myRef = database.getReference("/Year/" + "2016/" + "ID/" + idInput + "/RaceType");
                        myRef.setValue("5kRace");

                        ids += "ID=" + idInput + ",";
                        checkMark(timeTapped);
                    }
                }


            }
        });
        alert.show();
    }

    // Add a checkmark to the end of one of the times on the list of times, and update the adapter
    // so the checkmark is immediately visible.
    // Takes in the index of the time to add a checkmark to as a parameter
    public void checkMark(int timeTapped) {
        String newTime = ListElementsArrayList.get(timeTapped) + "  ✓";
        ListElementsArrayList.set(timeTapped, newTime);
        adapter.notifyDataSetChanged();
    }

    // Displays a toast indicating that the ID entered has already been entered before
    public void duplicateIDToast() {
        Context context = getApplicationContext();
        CharSequence text = "ID already entered";
        int duration = Toast.LENGTH_SHORT;
        Toast.makeText(context, text, duration).show();
    }

    // Displays a toast warning the user that they cannot leave the name empty
    public void nameEmpty() {
        Context context = getApplicationContext();
        CharSequence text = "Name cannot be empty";
        int duration = Toast.LENGTH_SHORT;
        Toast.makeText(context, text, duration).show();
    }

    // Displays a toast warning the user that they cannot leave the ID empty
    public void idEmpty() {
        Context context = getApplicationContext();
        CharSequence text = "ID cannot be empty";
        int duration = Toast.LENGTH_SHORT;
        Toast.makeText(context, text, duration).show();
    }

    // Runs the timer
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

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
