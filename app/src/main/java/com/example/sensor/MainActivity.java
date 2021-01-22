package com.example.sensor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationManager;
import android.app.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    ListView lv; //연락처 목록을 보여줄 리스트뷰.
    SensorManager sm;
    private List<String> list = new ArrayList<>(); //adapter로 연결할 List 선언
    boolean enableFall = false;
    private Handler handler;
    private String phoneNum = "";
    private String textMsg;
    private String prevNumber;
    private int GPS_ENABLE_REQUEST_CODE = 1003;
    private Button btn3;
    private boolean naksang = false;
    private Intent dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        lv = findViewById(R.id.listview1);
        Button btn1 = findViewById(R.id.button);
        Button btn2 = findViewById(R.id.button2);
        btn3 = findViewById(R.id.button3);
        Toast.makeText(MainActivity.this, "20160658\n강상우", Toast.LENGTH_LONG).show();

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.SEND_SMS, android.Manifest.permission.INTERNET};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 접근 권한 받아오기
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 연락처 추가 버튼
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(intent, 1002);
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 연락처 전체 삭제 버튼
                final SMSDBhelper smsdBhelper = new SMSDBhelper(MainActivity.this);
                smsdBhelper.open();
                smsdBhelper.removeAllContact();
                smsdBhelper.close();
                onUpdateNumberList();
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 낙상 감지 버튼
                if(!enableFall) {
                    btn3.setText("낙상 감지 비활성화");
                    enableFall = true;

                    final SMSDBhelper smsdBhelper = new SMSDBhelper(MainActivity.this);
                    smsdBhelper.open();

                    dialog = new Intent(getApplicationContext(), DialogActivity.class);
                    startActivityForResult(dialog, 1001);

                    handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void run() {
                            if (naksang) {
                                Intent intent = getIntent();
                                try {
                                    GpsTracker gpsTracker = new GpsTracker(MainActivity.this);
                                    double latitude = gpsTracker.getLatitude();
                                    double longitude = gpsTracker.getLongitude(); //경도
                                    List<String> itemIds = new ArrayList<>();
                                    Cursor cursor = smsdBhelper.getAllContacts();
                                    cursor.moveToFirst();

                                    if (cursor.moveToFirst()) {
                                        do {
                                            String data = cursor.getString(cursor.getColumnIndex("contact"));
                                            itemIds.add(data);
                                        } while (cursor.moveToNext());
                                    }
                                    cursor.close();
                                    if (itemIds.size() == 0) {
                                        Toast.makeText(getApplicationContext(), "메세지를 전송할 번호가 없습니다. 번호를 등록해주세요.", Toast.LENGTH_LONG).show();
                                        handler.removeCallbacksAndMessages(null);
                                        smsdBhelper.close();
                                        return;
                                    }
                                    for (String s : itemIds) {
                                        phoneNum = s;
                                        if (!phoneNum.equals(prevNumber) && phoneNum != null && ContextCompat.checkSelfPermission(getApplicationContext(),
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_DENIED &&
                                                ContextCompat.checkSelfPermission(getApplicationContext(),
                                                        android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_DENIED) {
                                            textMsg = "낙상 감지 위치 : " + getCurrentAddress(latitude, longitude);
                                            try {
                                                SmsManager sms = SmsManager.getDefault();
                                                sms.sendTextMessage(phoneNum.substring(phoneNum.lastIndexOf("전화번호 : ") + 7), null, textMsg, null, null);
                                            } catch (Exception e) {
                                                Toast.makeText(getApplicationContext(), "메세지를 전송하는 중 오류가 발생하였습니다.", Toast.LENGTH_SHORT).show();
                                                e.printStackTrace();
                                            }
                                            prevNumber = phoneNum;
                                        }
                                    }
                                    Toast.makeText(getApplicationContext(), "메세지가 전송되었습니다.", Toast.LENGTH_LONG).show();
                                    handler.removeCallbacksAndMessages(null);
                                    smsdBhelper.close();
                                    naksang = true;
                                    //finish();
                                } catch (Exception e) {
                                    Toast.makeText(getApplicationContext(), "메세지를 전송하는 중 오류가 발생하였습니다.", Toast.LENGTH_SHORT).show();
                                }
                            }
                            //약 7초간 응답이 없을 경우 실행된다.
                        }
                    }, 7000);
               } else {
                    btn3.setText("낙상 감지 활성화");
                    enableFall = false;
                    handler.removeCallbacksAndMessages(null);
                    naksang = false;
                }
            }
        });

        onUpdateNumberList();
    }

    public String getCurrentAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 100);
        } catch (IOException ioException) {
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            showDialogForLocationServiceSetting();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            showDialogForLocationServiceSetting();
            return "잘못된 GPS 좌표";
        } if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            showDialogForLocationServiceSetting();
            return "주소 미발견";
        }
        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";
    }

    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    public void onUpdateNumberList () {
        list.clear();
        final SMSDBhelper smsdBhelper = new SMSDBhelper(MainActivity.this);
        smsdBhelper.open();


        Cursor cursor = smsdBhelper.getAllContacts();

        if (cursor.moveToFirst()) {
            do {
                //연락처 정보를 list에 저장한다.
                String data = cursor.getString(cursor.getColumnIndex(SMSDBhelper.COLUMN_CONTACT));
                list.add(data);
            } while (cursor.moveToNext());
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.items, R.id.textview);
        arrayAdapter.addAll(list);
        lv.setAdapter(arrayAdapter);
        cursor.close();
        smsdBhelper.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) {
            handler.removeCallbacksAndMessages(null);
            btn3.setText("낙상 감지 활성화");
            enableFall = false;
            naksang = false;
        } else if (requestCode == 1002) {
            Cursor cursor = getContentResolver().query(data.getData(),
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, null);

            cursor.moveToFirst();
            //이름획득
            String receiveName = cursor.getString(0);
            //전화번호 획득
            String receivePhone = cursor.getString(1);
            cursor.close();

            final SMSDBhelper smsdBhelper = new SMSDBhelper(MainActivity.this);
            smsdBhelper.open();
            smsdBhelper.addNewContact("연락처 이름 : " + receiveName + " 연락처 전화번호 : " + receivePhone);
            smsdBhelper.close();
            onUpdateNumberList();
        } else if (requestCode == 1003) {
            //if (checkLocationServicesStatus()) {
            Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
            //checkRunTimePermission();
            return;
            //}
        }
    }

    private static final float SHAKE_THRESHOLD = 20.0f;
    private long lastTime;
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long diff = (currentTime - lastTime);
            if (diff > 500) {
                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];

                System.out.println("x : " + x + " y : " + y + " z : " + z);
                double abs = Math.sqrt(x * x + y * y + z * z);
                if (abs > SHAKE_THRESHOLD) {
                    lastTime = currentTime;
                    naksang = true;
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Sensor s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean check = sm.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        if(!check)
            Toast.makeText(this, "가속도 센서를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        super.onStop();
        sm.unregisterListener(this);
    }
}
