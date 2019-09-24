package com.fyp.d2d_android;

import android.os.Bundle;
import android.os.Environment;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FileScreen extends Fragment {

    private List<String> fileList = new ArrayList<String>();
    void ListDir(File f) {
        boolean success = false;
        // create folder if not exists
        if (!f.exists()){
            success=f.mkdir();
        }
        if (success){
            Toast toast=Toast.makeText(getActivity(),"New folder created",Toast.LENGTH_LONG);
            toast.show();
        }else if(!f.exists()){
            Toast toast=Toast.makeText(getActivity(),"Something went wrong when creating the folder",Toast.LENGTH_LONG);
            toast.show();
        }
        // fill the list from folder content
        File[] files = f.listFiles();
        fileList.clear();
        for (File file : files) {
            String[] nameArr =file.getPath().split("/");
            String fileName=nameArr[nameArr.length-1];
            fileList.add(fileName);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator +"D2D");
        View view = inflater.inflate(R.layout.fragment_file_screen, container, false);
        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/D2D");
        ListDir(root);
        ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setChoiceMode(2);
        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_checked, fileList);
        listView.setAdapter(listViewAdapter);

        return view;
    }

}