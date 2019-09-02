package com.fyp.d2d_android;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FileScreen extends Fragment {

    private List<String> fileList = new ArrayList<String>();
    void ListDir(File f) {
//        Log.e("Files1",f.exists()+"");
//        Log.e("Files2",f.isDirectory()+"");
//        Log.e("Files3",f.listFiles()+"");
        File[] files = f.listFiles();
        fileList.clear();
        for (File file : files) {
            fileList.add(file.getPath());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_screen, container, false);
        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/D2D");
        ListDir(root);
        ListView listView = (ListView) view.findViewById(R.id.listView);
        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, fileList);
        listView.setAdapter(listViewAdapter);
        return view;
    }

}