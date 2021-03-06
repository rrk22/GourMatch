package umut.gourmatch;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import umut.gourmatch.User;


public class ProfileBuilderActivity extends AppCompatActivity {
    private ArrayList<String> allergy_names = new ArrayList<String>();
    private ArrayList<String> allergy_info = new ArrayList<String>();
    private ArrayList<String> diet_names = new ArrayList<String>();
    private ArrayList<String> diet_info = new ArrayList<String>();
    // lacto, lacto_ovo, ovo, pesce, vegan
    Map<String, String> allergy = new HashMap<String, String>();
    private ArrayList<Boolean> allergies = new ArrayList<Boolean>(); //final result
    private String myDiet;
    private int dietIndex = -1;
    private DatabaseReference mDatabase;
    private String username;
    private String firstName;
    private String lastName;
    private int birthYear;
    private int birthMonth;
    private int birthDay;
    private int genderIndex;
    private String gender;
    private String TAG = "ProfileBuilderActivity.java";
//    private DatePicker datePicker;
//    private EditText mUsername;
//    private Button mNextButton;
//    private EditText mFirstName;
//    private EditText mLastName;
    private FirebaseAuth mAuth;
    private String userId;
    private boolean success = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("allergies").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot allergy : dataSnapshot.getChildren()){
                            String allergy_name = allergy.getKey();
                            allergy_names.add(allergy_name);
                            allergies.add(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                        Toast.makeText(ProfileBuilderActivity.this, "Failed to pull allergies from database.",
                                Toast.LENGTH_SHORT).show();

                    }
                }
        );

        mDatabase.child("dietary").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot diet : dataSnapshot.getChildren()){
                            String info = (String) diet.getValue();
                            String allergy_name = diet.getKey();
                            diet_info.add(info);
                            diet_names.add(allergy_name);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                        Toast.makeText(ProfileBuilderActivity.this, "Failed to load diets.",
                                Toast.LENGTH_SHORT).show();

                    }
                }
        );
        create_basic();
    }



    public void check_user(){
        mDatabase.child("usernames").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "Got data");
                        //get username
                        if(dataSnapshot.hasChild(username)){
                            Log.e(TAG, "User " + username + " is taken");
                            Toast.makeText(ProfileBuilderActivity.this,
                                    "Username is taken, please choose another.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Log.d(TAG, "Username not taken.");
                            saveBasic();
                            create_allergies();
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Username error");

                    }
                }
        );

    }

    private void create_diets(){
        setContentView(R.layout.activity_profile_builder_dietary);
        ScrollView sv = new ScrollView(this);

        Button mFinishButton = (Button) findViewById(R.id.Finish);
        Button mBackButton = (Button) findViewById(R.id.Back);
        ScrollView scrollView = (ScrollView) findViewById(R.id.List);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        for(int i = 0; i < diet_names.size(); i++) {
            String diet_name = diet_names.get(i);
            String info = diet_info.get(i);
            RadioButton btn = new RadioButton(this);
            btn.setText(diet_name + ": " + info);
            btn.setId(i);
            group.addView(btn);
        }
        if(dietIndex != -1) {
            group.check(dietIndex);
        }
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                dietIndex = i;
                myDiet = diet_names.get(i);
            }
        });
        ll.addView(group);
        scrollView.addView(ll);

        mFinishButton.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View view){
                        if(dietIndex == -1) {
                            Toast.makeText(ProfileBuilderActivity.this, "Please pick one.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            //Store data and move on
                            finishProfile();
                        }
                    }
                });

        mBackButton.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View view){
                        create_allergies();
                    }
                });
    }


    private void finishProfile() {
        /*
        What needs to be done:
        Collect all of the stored information and store to a new user under "users" using the auth ID
        Before doing so, once again confirm that the username hasn't been taken. If it somehow has, let the user know
        If there is an error adding the user, let them know and do not continue to the main screen
        Once successfully added, continue to main using the same 4 lines as below
         */


        mDatabase.child("usernames").addListenerForSingleValueEvent(
                new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Log.d(TAG, "Got data");
                            //get username
                            if (dataSnapshot.hasChild(username)) {
                                Log.e(TAG, "User " + username + " is taken");
                                Toast.makeText(ProfileBuilderActivity.this,
                                        "Username was taken between now and before, please choose another.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "last username check: Username not taken.");
                                //write to database
                                //mDatabase.child("users").child(userId).child("username").setValue(username);
                                mDatabase.child("usernames").child(username).setValue(userId, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            success = false;
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });
                                mDatabase.child("users").child(userId).child("username").setValue(username, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            success = false;
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });

                                mDatabase.child("users").child(userId).child("firstName").setValue(firstName, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            success = false;
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });
                                mDatabase.child("users").child(userId).child("lastName").setValue(lastName, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            success = false;
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });
                                mDatabase.child("users").child(userId).child("birthYear").setValue(birthYear, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            success = false;
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });
                                mDatabase.child("users").child(userId).child("birthMonth").setValue(birthMonth, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            success = false;
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });
                                mDatabase.child("users").child(userId).child("birthDay").setValue(birthDay, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            success = false;
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });
                                mDatabase.child("users").child(userId).child("gender").setValue(gender, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                            success = false;
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });
                                mDatabase.child("users").child(userId).child("currLocation").setValue(true, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                            success = false;
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });
                                mDatabase.child("users").child(userId).child("diet").setValue(myDiet, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                            success = false;
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });
                                System.out.println(allergy);
                                mDatabase.child("users").child(userId).child("allergies").setValue(allergy, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            System.out.println("Data could not be saved " + databaseError.getMessage());
                                            success = false;
                                        } else {
                                            System.out.println("Data saved successfully.");
                                        }
                                    }
                                });


                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "Username error");

                        }
                    }
        );


        if(success) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
        //maybe have it so when you click the button, we set success to true
        success = true;
    }

    public void create_basic() {
        setContentView(R.layout.activity_profile_builder_basic);
            final DatePicker datePicker = (DatePicker) findViewById(R.id.birthdayPick);
            final EditText mUsername = (EditText)findViewById(R.id.basic_username);
            Button mNextButton = (Button) findViewById(R.id.Next);
            final EditText mFirstName = (EditText)findViewById(R.id.basic_firstname);
            final EditText mLastName = (EditText)findViewById(R.id.basic_lastname);
            final Spinner mGender = (Spinner) findViewById(R.id.gender);
        if(username != null) {
            mUsername.setText(username);
            mFirstName.setText(firstName);
            mLastName.setText(lastName);
            datePicker.init(birthYear, birthMonth, birthDay, null);
            mGender.setSelection(genderIndex);
        } else {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            datePicker.init(year, month, day, null);
        }

        mNextButton.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View view){
                        if(mUsername.getText().toString().indexOf(' ') != -1) {
                            Toast.makeText(ProfileBuilderActivity.this, "No spaces in Username.",
                                    Toast.LENGTH_SHORT).show();
                        }else
                        if(userId.isEmpty()) {
                            Toast.makeText(ProfileBuilderActivity.this,
                                    "Please enter a username.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            username = mUsername.getText().toString();
                            Log.d(TAG, "Checking user...");
                            check_user();
                        }
                    }
                });
    }

    private void saveBasic() {
        final DatePicker datePicker = (DatePicker) findViewById(R.id.birthdayPick);
        final EditText mUsername = (EditText)findViewById(R.id.basic_username);
        final EditText mFirstName = (EditText)findViewById(R.id.basic_firstname);
        final EditText mLastName = (EditText)findViewById(R.id.basic_lastname);
        final Spinner mGender = (Spinner) findViewById(R.id.gender);

        username = mUsername.getText().toString();
        firstName = mFirstName.getText().toString();
        lastName = mLastName.getText().toString();
        birthDay = datePicker.getDayOfMonth();
        birthMonth = datePicker.getMonth();
        birthYear = datePicker.getYear();
        gender = (String) mGender.getSelectedItem();
        genderIndex = mGender.getSelectedItemPosition();
    }

    public void create_allergies(){
        setContentView(R.layout.activity_profile_builder_allergies);
        ScrollView sv = new ScrollView(this);

        Button mNextButton = (Button) findViewById(R.id.Next);
        Button mBackButton = (Button) findViewById(R.id.Back);
        ScrollView scrollView = (ScrollView) findViewById(R.id.List);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);

        for( int i = 0; i < allergy_names.size(); i++){
            CheckBox myCheck = new CheckBox(this);
            myCheck.setText(allergy_names.get(i));
            myCheck.setId(i);
            myCheck.setChecked(allergies.get(i));
            allergy.put(allergy_names.get(i), "false");
            myCheck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CheckBox b = (CheckBox) view;
                    int allergy_id = b.getId();
                    allergies.set(allergy_id, b.isChecked());
                    String allergy_name = allergy_names.get(allergy_id);
                    System.out.println("ALLERGY IS " + allergy_name);
                    allergy.put(allergy_name, String.valueOf(b.isChecked()));

                    System.out.println(allergy);
                }

            });
            ll.addView(myCheck);
        }

        mNextButton.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View view){
                        //Move on to diets
                        create_diets();
                    }
                });

        mBackButton.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View view){
                        create_basic();;
                    }
                });

        scrollView.addView(ll);

    }


}
