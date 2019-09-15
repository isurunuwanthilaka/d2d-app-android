package com.fyp.d2d_android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/*
 * This is for editing user details.
 */
public class EditUserActivity extends Fragment implements View.OnClickListener {

    private Toolbar toolbarEditUser;
    FirebaseAuth mAuth;
    DatabaseReference ref;
    String fname, lname;
    EditText firstName, lastName;
    TextView update;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_profile_screen, container, false);

        //adding toolbar
        rootView.findViewById(R.id.update_user).setOnClickListener(this);
        rootView.findViewById(R.id.sign_out).setOnClickListener(this);

        firstName = (EditText) rootView.findViewById(R.id.first_name);
        lastName = (EditText) rootView.findViewById(R.id.last_name);

        mAuth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance().getReference("/User/" + mAuth.getCurrentUser().getUid());

        //setting text fields
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                firstName.setText(dataSnapshot.child("/firstName").getValue(String.class));
                lastName.setText(dataSnapshot.child("/lastName").getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {

            getActivity().finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_out:
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    mAuth.signOut();
                    startActivity(new Intent(getContext(), MainActivity.class));
                }
                break;
            case R.id.update_user:
                if (!TextUtils.isEmpty(firstName.getText()) && !TextUtils.isEmpty(lastName.getText())) {
                    fname = firstName.getText().toString();
                    lname = lastName.getText().toString();
                    ref.child("/firstName").setValue(fname);
                    ref.child("/lastName").setValue(lname);
                }
                break;
        }
    }
}