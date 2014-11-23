package ru.in_da_house.smssender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.ListView;

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

public class MainActivity extends Activity {

    public static final String APP_PREFERENCES = "sms_settings";
    public static final String APP_PREFERENCES_LOGIN = "login";
    public static final String APP_PREFERENCES_PASSWORD = "password";
    SharedPreferences mSettings;
    final String LOG_TAG = "myLogs";
    public String[] sms_arr_str;

    SmsManager smsManager = SmsManager.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        if(!mSettings.contains(APP_PREFERENCES_LOGIN) || !mSettings.contains(APP_PREFERENCES_PASSWORD)) {
            setContentView(R.layout.need_settings);
        } else {
            setContentView(R.layout.activity_main);

            ImageButton buttonGetData = (ImageButton)findViewById(R.id.getData);
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
                                        final JSONArray sms_arr = responseJSON.getJSONArray("sys");


                                        sms_arr_str = new String[sms_arr.length()];

                                        final ListView smsList = (ListView)findViewById(R.id.smsList);

                                        for (int i = 0; i < sms_arr.length(); i++) {
                                           JSONObject sms = sms_arr.getJSONObject(i);
                                           String id = sms.getString("text");
//                                            debugText.setText(id);
                                            sms_arr_str[i] = id;


                                        }

                                        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, sms_arr_str);
                                        smsList.setAdapter(adapter);

                                        smsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                                                Log.d(LOG_TAG, "itemClick: position = " + position + ", id = "
                                                        + id);

                                                try {
                                                    JSONObject sms = sms_arr.getJSONObject(position);
//                                                    Log.d(LOG_TAG, "text = " + sms.getString("text") + ", id = "
//                                                            + sms.getString("phone"));

                                                    String text_sms = sms.getString("text");
//                                                    String text_sms = "фыва";
                                                    String number_sms = "+7"+sms.getString("phone");

//                                                    PendingIntent sentPI;
//                                                    String SENT = "SMS_SENT";
//                                                    sentPI = PendingIntent.getBroadcast(MainActivity.this, 0,new Intent(SENT), 0);

                                                    ArrayList smsContructedList = smsManager.divideMessage(text_sms);

                                                    smsManager.sendMultipartTextMessage(number_sms, null, smsContructedList, null, null);
                                                    Log.d(LOG_TAG, "text = " + text_sms + ", id = " + number_sms);


                                                    sms_arr_str = (String[]) ArrayUtils.remove(sms_arr_str, position);

                                                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, (String[]) sms_arr_str);
                                                    smsList.setAdapter(adapter);

                                                    adapter.notifyDataSetChanged();
                                                    adapter.notifyDataSetInvalidated();





                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

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



}
