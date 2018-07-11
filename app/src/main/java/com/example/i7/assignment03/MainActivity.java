package com.example.i7.assignment03;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorMg;
    private float[] oneSensorData = new float[3];
    private int typeOfActivity = 0;
    final private long frequencySetting = 50;    //10Hz 100ms
    private long previousTime = -10;    //save the previous time
    private SQLiteDatabase m_s_db;
    String tb_name = null;
    String myActivityType = null;
    private boolean running = false;
    private ArrayList<ArrayList> recordData = null;
    private Thread thread_record = null;
//    private TextView result;

    private TextView scroll_result;
    private EditText table_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scroll_result = (TextView) findViewById(R.id.mytext);
        table_name = (EditText) findViewById(R.id.tableName);

        //setup Spinner
        final Spinner sp_activity = (Spinner) findViewById(R.id.spinner_1);
        ArrayAdapter<String> Adapter_Drop_activity = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.ActivityList));
        sp_activity.setAdapter(Adapter_Drop_activity);
        sp_activity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                myActivityType = sp_activity.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        defineButton();

        sensorMg = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorMg.registerListener(new MySensorListener(),
                sensorMg.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);


    }

    private void defineButton(){
        findViewById(R.id.button_stop).setOnClickListener(new ButtonListener());
        findViewById(R.id.button_record).setOnClickListener(new ButtonListener());
    }

    class DataProductor extends Thread{
        public void run(){
            running = true;
            ArrayList oneRow;
            int size = 50;

            while (running) {
                oneRow = new ArrayList();
                for (int i = 0; i < size; i++){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    oneRow.add(oneSensorData[0]);
                    oneRow.add(oneSensorData[1]);
                    oneRow.add(oneSensorData[2]);
                }
                oneRow.add(myActivityType);
                recordData.add(oneRow);
                oneRow = null;
            }

        }
    }


    class ButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()){
//                case R.id.button_walk:
//
//                    break;

                case R.id.button_record:
                    recordData = null;
                    recordData = new ArrayList();
//                    result.setText("");
                    scroll_result.setText("");
//                    result = null;
//                    scroll_result = null;
                    if (!isTableExist()){
                        m_s_db = openOrCreateDatabase("Group19.db", Context.MODE_PRIVATE, null);
                        m_s_db.beginTransaction();
                        System.out.println("onClick: " + getTableName());
                        final String sql = "CREATE TABLE IF NOT EXISTS " + getTableName() + " (ID INTEGER PRIMARY KEY ASC, " +
                                "Accel_X_1 REAL NOT NULL, Accel_Y_1 REAL NOT NULL, Accel_Z_1 REAL NOT NULL);";
                        m_s_db.execSQL(sql);

                        String temp = "";
                        for (int i = 2; i <= 50; i++){
                            temp = "ALTER TABLE " + getTableName() + " ADD COLUMN Accel_X_" + i + " REAL";
                            m_s_db.execSQL(temp);
                            temp = "ALTER TABLE " + getTableName() + " ADD COLUMN Accel_Y_" + i + " REAL";
                            m_s_db.execSQL(temp);
                            temp = "ALTER TABLE " + getTableName() + " ADD COLUMN Accel_Z_" + i + " REAL";
                            m_s_db.execSQL(temp);
                        }
                        temp = "ALTER TABLE " + getTableName() + " ADD COLUMN Activity_Label TEXT";
                        m_s_db.execSQL(temp);

                        m_s_db.setTransactionSuccessful();
                        m_s_db.endTransaction();
                    }

                    thread_record = new DataProductor();
                    thread_record.start();

                    break;

//                case R.id.button_jump:
//
//                    break;

                case R.id.button_stop:
                    running = false;
                    thread_record = null;

                    scroll_result.setText(arrayToString(recordData));

                    insertDataToDB(recordData);

                    break;
            }
        }
    }

    class MySensorListener implements SensorEventListener{

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                if (System.currentTimeMillis() - previousTime > frequencySetting){
                    previousTime = System.currentTimeMillis();
                    oneSensorData[0] = event.values[0];
                    oneSensorData[1] = event.values[1];
                    oneSensorData[2] = event.values[2];
                }

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private String getTableName(){
        EditText _pName = (EditText)findViewById(R.id.tableName);

        if (_pName == null)
            return "default";
        else
            return _pName.getText().toString();
    }

    private String arrayToString(ArrayList<ArrayList> al){
        String result = "";

        for (int i = 0; i < al.size(); i++){
            result += "[";
            for (int j = 0; j < (al.get(i)).size(); j++){
                result += al.get(i).get(j) + "\t";
            }
            result +="] ";
        }

        //System.out.println(result);
        return result;
    }

    private boolean isTableExist(){
        SQLiteDatabase tempDb = openOrCreateDatabase("Group19.db", Context.MODE_PRIVATE, null);
        Cursor cursor = tempDb.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+getTableName()+"'", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                return true;
            }
            cursor.close();
            tempDb.close();
        }
        return false;
    }

    private void insertDataToDB(ArrayList<ArrayList> al){
        ContentValues cv = new ContentValues();
        ArrayList temp;

        for(int i = 0; i < al.size(); i++){

            for (int j = 1, m = 0; j <= 50; j++, m += 3){

                cv.put(("Accel_X_"+j), (float) al.get(i).get(m));
                cv.put(("Accel_Y_"+j), (float) al.get(i).get(m+1));
                cv.put(("Accel_Z_"+j), (float) al.get(i).get(m+2));
            }
            cv.put("Activity_Label", (String)al.get(i).get(150));

            m_s_db.insert(getTableName(), null, cv);
        }
    }

}
