package ru.in_da_house.smssender;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.OnLongClickListener;

public class MainActivity extends Activity {

    public static final String APP_PREFERENCES = "sms_settings";
    public static final String APP_PREFERENCES_LOGIN = "login";
    public static final String APP_PREFERENCES_PASSWORD = "password";
    public static final String APP_PREFERENCES_CNT1 = "cnt1";
    public static final String APP_PREFERENCES_CNT2 = "cnt2";
    SharedPreferences mSettings;
    final String LOG_TAG = "myLogs";
    public String[] sms_arr_str;
    public ArrayAdapter<String> adapter;
    public JSONArray sms_arr;

    String SENT      = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    private BroadcastReceiver sent      = null;
    private BroadcastReceiver delivered = null;

    SmsManager smsManager = SmsManager.getDefault();

    @Override
    protected void onDestroy()
    {
        if(sent != null)
            unregisterReceiver(sent);
        if(delivered != null)
            unregisterReceiver(delivered);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);


        if(!mSettings.contains(APP_PREFERENCES_LOGIN) || !mSettings.contains(APP_PREFERENCES_PASSWORD)) {
            setContentView(R.layout.need_settings);
        } else {
            setContentView(R.layout.activity_main);




            Button btnCnt1 = (Button)findViewById(R.id.btnCnt1);
            if(mSettings.contains(APP_PREFERENCES_CNT1)) {
                btnCnt1.setText(mSettings.getString(APP_PREFERENCES_CNT1, ""));
            }
            else
            {
                btnCnt1.setText(mSettings.getString("0", ""));
            }

            Button btnCnt2 = (Button)findViewById(R.id.btnCnt2);
            if(mSettings.contains(APP_PREFERENCES_CNT2)) {
                btnCnt2.setText(mSettings.getString(APP_PREFERENCES_CNT2, ""));
            }
            else
            {
                btnCnt2.setText(mSettings.getString("0", ""));
            }




            Button buttonGetData = (Button)findViewById(R.id.getDataBtn);
            buttonGetData.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {

                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                            .permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                                        // Создадим HttpClient и PostHandler
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost("http://sms2.in-da-house.ru/mobile/get_unsend.json");

                    try {
                        // Добавим данные (пара - "название - значение")
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                        nameValuePairs.add(new BasicNameValuePair("login", mSettings.getString(APP_PREFERENCES_LOGIN, "")));
                        nameValuePairs.add(new BasicNameValuePair("pwd", MD5(mSettings.getString(APP_PREFERENCES_PASSWORD, ""))));
                        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                        // Выполним запрос
                        HttpResponse response = httpclient.execute(httppost);

                        int responseCode = response.getStatusLine().getStatusCode();
                        switch(responseCode)
                        {
                            case 200:
                                HttpEntity entity = response.getEntity();
                                if(entity != null)
                                {
                                    String responseBody = EntityUtils.toString(entity);
//                                    TextView debugText = (TextView)findViewById(R.id.debugText);

                                    try {
                                        JSONObject responseJSON = new JSONObject(responseBody);
                                        sms_arr = responseJSON.getJSONArray("sys");


                                        sms_arr_str = new String[sms_arr.length()];

                                        final ListView smsList = (ListView)findViewById(R.id.smsList);

                                        for (int i = 0; i < sms_arr.length(); i++) {
                                           JSONObject sms = sms_arr.getJSONObject(i);
                                           String id = sms.getString("text");
//                                            debugText.setText(id);
                                            sms_arr_str[i] = id;


                                        }

                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, sms_arr_str);
                                        smsList.setAdapter(adapter);

                                        smsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                                        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                                                Log.d(LOG_TAG, "itemClick: position = " + position + ", id = "
                                                        + view.getContext());

                                                try {





                                                    //Регистрация широковещательного приемника: Отправка
                                                    IntentFilter in_sent = new IntentFilter(SENT);
                                                    sent = new BroadcastReceiver()
                                                    {
                                                        @Override
                                                        public void onReceive(Context context, Intent intent)
                                                        {
//                                                            tv.append(intent.getStringExtra("PARTS")+": ");
//                                                            tv.append(intent.getStringExtra("MSG")+": ");
                                                            switch(getResultCode())
                                                            {
                                                                case Activity.RESULT_OK:
                                                                    Toast.makeText(getBaseContext(), "SMS Отправлено\n", Toast.LENGTH_SHORT).show();
                                                                    break;
                                                                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                                                    Toast.makeText(getBaseContext(), "Общий сбой\n", Toast.LENGTH_SHORT).show();
                                                                    break;
                                                                case SmsManager.RESULT_ERROR_NO_SERVICE:
                                                                    Toast.makeText(getBaseContext(), "Нет сети\n", Toast.LENGTH_SHORT).show();
                                                                    break;
                                                                case SmsManager.RESULT_ERROR_NULL_PDU:
                                                                    Toast.makeText(getBaseContext(), "Null PDU\n", Toast.LENGTH_SHORT).show();
                                                                    break;
                                                                case SmsManager.RESULT_ERROR_RADIO_OFF:
                                                                    Toast.makeText(getBaseContext(), "Нет связи\n", Toast.LENGTH_SHORT).show();
                                                                    break;
                                                            }

                                                        }
                                                    };
                                                    registerReceiver(sent, in_sent);

                                                    //Регистрация широковещательного приемника: Доставка
                                                    IntentFilter in_delivered = new IntentFilter(DELIVERED);
                                                    delivered = new BroadcastReceiver()
                                                    {
                                                        @Override
                                                        public void onReceive(Context context, Intent intent)
                                                        {
//                                                            tv.append(intent.getStringExtra("PARTS")+": ");
//                                                            tv.append(intent.getStringExtra("MSG")+": ");
                                                            switch (getResultCode())
                                                            {
                                                                case Activity.RESULT_OK:
                                                                    Toast.makeText(getBaseContext(), "SMS Доставлено\n", Toast.LENGTH_SHORT).show();
                                                                    break;
                                                                case Activity.RESULT_CANCELED:
                                                                    Toast.makeText(getBaseContext(), "SMS Не доставлено\n", Toast.LENGTH_SHORT).show();
                                                                    break;
                                                            }
                                                        }
                                                    };
                                                    registerReceiver(delivered, in_delivered);



















                                                    JSONObject sms = sms_arr.getJSONObject((int) id);
//                                                    Log.d(LOG_TAG, "text = " + sms.getString("text") + ", id = "
//                                                            + sms.getString("phone"));

                                                    String text_sms = sms.getString("text");
//                                                    String text_sms = "фыва";
                                                    String number_sms = "+7"+sms.getString("phone");

//                                                    PendingIntent sentPI;
//                                                    String SENT = "SMS_SENT";
//                                                    sentPI = PendingIntent.getBroadcast(MainActivity.this, 0,new Intent(SENT), 0);

                                                    ArrayList smsContructedList = smsManager.divideMessage(text_sms);
                                                    ArrayList<PendingIntent> al_piSent = new ArrayList<PendingIntent>();
                                                    ArrayList<PendingIntent> al_piDelivered = new ArrayList<PendingIntent>();











                                                    for (int i = 0; i < smsContructedList.size(); i++)
                                                    {
                                                        Intent sentIntent = new Intent(SENT);
                                                        sentIntent.putExtra("PARTS", "Часть: "+i);
                                                        sentIntent.putExtra("MSG", "Сообщение: "+smsContructedList.get(i));
                                                        PendingIntent pi_sent = PendingIntent.getBroadcast(MainActivity.this, i, sentIntent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
                                                        al_piSent.add(pi_sent);

                                                        Intent deliveredIntent = new Intent(DELIVERED);
                                                        deliveredIntent.putExtra("PARTS", "Часть: "+i);
                                                        deliveredIntent.putExtra("MSG", "Сообщение: "+smsContructedList.get(i));
                                                        PendingIntent pi_delivered = PendingIntent.getBroadcast(MainActivity.this, i, deliveredIntent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
                                                        al_piDelivered.add(pi_delivered);
                                                    }







                                                    smsManager.sendMultipartTextMessage(number_sms, null, smsContructedList, al_piSent, al_piDelivered);


//                                                    Toast.makeText(getBaseContext(), "SMS Доставлено\n", Toast.LENGTH_SHORT).show();


                                                    Log.d(LOG_TAG, "text = " + text_sms + ", id = " + number_sms);




                                                    if(mSettings.contains(APP_PREFERENCES_CNT1)) {
                                                        int cnt1 = Integer.parseInt(mSettings.getString(APP_PREFERENCES_CNT1, "")) + 1;
                                                        SharedPreferences.Editor editor = mSettings.edit();
                                                        editor.putString(APP_PREFERENCES_CNT1, Integer.toString(cnt1)  );
                                                        editor.commit();
                                                        Button btnCnt1 = (Button)findViewById(R.id.btnCnt1);
                                                        btnCnt1.setText(mSettings.getString(APP_PREFERENCES_CNT1, ""));
                                                    }
                                                    else
                                                    {
                                                        SharedPreferences.Editor editor = mSettings.edit();
                                                        editor.putString(APP_PREFERENCES_CNT1, Integer.toString(1)  );
                                                        editor.commit();
                                                        Button btnCnt1 = (Button)findViewById(R.id.btnCnt1);
                                                        btnCnt1.setText(mSettings.getString(APP_PREFERENCES_CNT1, ""));
                                                    }

                                                    if(mSettings.contains(APP_PREFERENCES_CNT2)) {
                                                        int cnt2 = Integer.parseInt(mSettings.getString(APP_PREFERENCES_CNT2, "")) + 1;
                                                        SharedPreferences.Editor editor = mSettings.edit();
                                                        editor.putString(APP_PREFERENCES_CNT2, Integer.toString(cnt2)  );
                                                        editor.commit();
                                                        Button btnCnt2 = (Button)findViewById(R.id.btnCnt2);
                                                        btnCnt2.setText(mSettings.getString(APP_PREFERENCES_CNT2, ""));
                                                    }
                                                    else
                                                    {
                                                        SharedPreferences.Editor editor = mSettings.edit();
                                                        editor.putString(APP_PREFERENCES_CNT2, Integer.toString(1)  );
                                                        editor.commit();
                                                        Button btnCnt2 = (Button)findViewById(R.id.btnCnt2);
                                                        btnCnt2.setText(mSettings.getString(APP_PREFERENCES_CNT2, ""));
                                                    }




                                                    sms_arr_str = (String[]) ArrayUtils.remove(sms_arr_str, position);
                                                    sms_arr = RemoveJSONArray(sms_arr, position);
//                                                    sms_arr.remove(position);
//                                                    Log.d(LOG_TAG, "sms_arr_str size = " + sms_arr_str.length);
//                                                    Log.d(LOG_TAG, "sms_arr_str = " + Arrays.asList(sms_arr_str));

                                                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, sms_arr_str);

                                                    adapter.notifyDataSetChanged();
                                                    adapter.notifyDataSetInvalidated();

                                                    smsList.setAdapter(adapter);



                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            return true;
                                            }
                                        });

//                                        debugText.setText(sms_arr.length());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }
                                break;
                        }



                    } catch (ClientProtocolException e) {
                        // Ошибка :(
                    } catch (IOException e) {
                        // Ошибка :(
                    }


//                    finish();
                }
            });

//            Button btnCnt1 = (Button)findViewById(R.id.btnCnt1);
            btnCnt1.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    if(mSettings.contains(APP_PREFERENCES_CNT1)) {
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putString(APP_PREFERENCES_CNT1, Integer.toString(0)  );
                        editor.commit();
                        Button btnCnt1 = (Button)findViewById(R.id.btnCnt1);
                        btnCnt1.setText(mSettings.getString(APP_PREFERENCES_CNT1, ""));
                    }
                    else
                    {
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putString(APP_PREFERENCES_CNT1, Integer.toString(0)  );
                        editor.commit();
                        Button btnCnt1 = (Button)findViewById(R.id.btnCnt1);
                        btnCnt1.setText(mSettings.getString(APP_PREFERENCES_CNT1, ""));
                    }
                    return false;
                }
            });

//            Button btnCnt2 = (Button)findViewById(R.id.btnCnt2);
            btnCnt2.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    if(mSettings.contains(APP_PREFERENCES_CNT2)) {
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putString(APP_PREFERENCES_CNT2, Integer.toString(0)  );
                        editor.commit();
                        Button btnCnt2 = (Button)findViewById(R.id.btnCnt2);
                        btnCnt2.setText(mSettings.getString(APP_PREFERENCES_CNT2, ""));
                    }
                    else
                    {
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putString(APP_PREFERENCES_CNT2, Integer.toString(0)  );
                        editor.commit();
                        Button btnCnt2 = (Button)findViewById(R.id.btnCnt2);
                        btnCnt2.setText(mSettings.getString(APP_PREFERENCES_CNT2, ""));
                    }
                    return false;
                }
            });


        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private String getHash(byte[] hash_data) {
        //Create MD5 Hash
        MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(hash_data);
            byte messageDigest[] = digest.digest();
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            Log.i("Exception", "NoSuchAlgorithmException " + ex.getMessage());
        }
        return "";
    }


    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static JSONArray RemoveJSONArray( JSONArray jarray,int pos) {

        JSONArray Njarray = new JSONArray();
        try {
            for (int i = 0; i < jarray.length(); i++) {
                if (i != pos)
                    Njarray.put(jarray.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Njarray;
    }








}
