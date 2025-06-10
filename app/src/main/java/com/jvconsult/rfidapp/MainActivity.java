package com.jvconsult.rfidapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pda.rfid.EPCModel;
import com.pda.rfid.IAsynchronousMessage;
import com.pda.rfid.uhf.UHFReader;
import com.port.Adapt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements IAsynchronousMessage {

    private static final String TAG = "Demo";

    private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    private SimpleAdapter sa = null;

    private static final int REQUEST_READ_PHONE_STATE = 1;
    private boolean isOpened = false;
    private boolean isReading = false;
    private HashMap<String, EPCModel> hmList = new HashMap<>();
    private Object hmList_Lock = new Object();

    private void initView() {
        Adapt.init(this);
        isOpened = UHFReader.getUHFInstance().OpenConnect(this);
        if (!isOpened) {
            Log.d(TAG, "open UHF failed!");
        }
        UHFReader._Config.SetEPCBaseBandParam(255, 0, 1, 0);
        UHFReader._Config.SetANTPowerParam(1, 20);
    }
    private void checkPermission() {
//
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new
                    String[]{android.Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        } else {
            initView();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        list = findViewById(R.id.ltEPCs);
        arrayList = new ArrayList<String>();

        // Adapter: You need three parameters 'the context, id of the layout (it will be where the data is shown),
        // and the array that contains the data
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayList);

        // Here, you set the data in your ListView
        list.setAdapter(adapter);
        checkPermission();
    }

    @Override
    protected void onDestroy() {
        UHFReader.getUHFInstance().CloseConnect();
        super.onDestroy();
    }

    public void onRead(View v) {
        Log.d("Info", "Starting read");
        if (!isOpened) {
            return ;
        }
        if (isReading) {
            return ;
        }
        isReading = UHFReader._Tag6C.GetEPC(1, 1) == 0;
        Log.d("Info", "Started read");
    }

    public void onStop(View v) {
        Log.d("Info", "Stopping read");
        if (!isOpened) {
            return ;
        }
        if (!isReading) {
            return;
        }
        UHFReader.getUHFInstance().Stop();
        isReading = false;
        adapter.notifyDataSetChanged();
        Log.d("Info", "Tags output stopped:" + arrayList);
        Log.d("Info", "Stopped read");
    }

    @Override
    public void OutPutEPC(EPCModel model) {
        try {
            synchronized (hmList_Lock) {
                if (hmList.containsKey(model._EPC + model._TID)) {
                    EPCModel tModel = hmList.get(model._EPC + model._TID);
                    tModel._TotalCount++;
                    model._TotalCount = tModel._TotalCount;
                    hmList.remove(model._EPC + model._TID);
                    hmList.put(model._EPC + model._TID, model);
                } else {
                    hmList.put(model._EPC + model._TID, model);
                    arrayList.add(model._EPC + model._TID);
                    Log.d("Info", "Tags output:" + arrayList);
                }
            }
            //ShowList();
        } catch (Exception ex) {
            Log.d("Debug", "Tags output exceptions:" + ex.getMessage());
        }
    }

    public void exportTagsToCsv(View v) {
        if (arrayList.isEmpty()) {
            Log.d("CSV Export", "Nenhuma tag para exportar.");
            return;
        }

        CsvExporter.exportToCsv(arrayList, "tags_lidas.csv");
    }

    @SuppressWarnings({ "rawtypes", "unused" })
    protected List<Map<String, Object>> GetData() {
        List<Map<String, Object>> rt = new ArrayList<Map<String, Object>>();
        synchronized (hmList_Lock) {
            Iterator iter = hmList.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                EPCModel val = (EPCModel) entry.getValue();
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("EPC", val._EPC);
                map.put("ReadCount", val._TotalCount);
                rt.add(map);
            }
        }
        return rt;
    }

    protected void ShowList() {
        Log.d("Info", "Init showList");


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            initView();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }
}