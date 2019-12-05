package com.fyp.d2d_android;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CloudFileScreen extends Fragment {

    private List<String> fileList = new ArrayList<String>();
    FirebaseAuth mAuth;
    DatabaseReference ref;

    //TODO : Remove root folder path from the screen

    void getFilesFromCloud() {
        //authenticating firebase database
        mAuth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance().getReference("/fileStoreDetails");

        //adding fields to the fileList
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fileList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String filename = snapshot.getKey();
                    fileList.add(filename);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator +"D2D");
        View view = inflater.inflate(R.layout.fragment_file_screen, container, false);
        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/D2D");
        getFilesFromCloud();
        ListView listView = view.findViewById(R.id.listView);
        // Set selection mode to multiple choices
        listView.setChoiceMode(2);
        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_checked, fileList);
        listView.setAdapter(listViewAdapter);
        // Set all items checked
        for (int i=0;i<fileList.size();i++){
            listView.setItemChecked(i,true);
        }
        return view;
    }
}