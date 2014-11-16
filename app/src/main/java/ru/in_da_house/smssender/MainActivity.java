package ru.in_da_house.smssender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

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
                        nameValuePairs.add(new BasicNameValuePair("pwd", getHash(mSettings.getString(APP_PREFERENCES_PASSWORD, "").getBytes())));
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
                                        JSONArray sms_arr = responseJSON.getJSONArray("sys");

                                        String[] sms_arr_str = new String[sms_arr.length()];

                                        ListView smsList = (ListView)findViewById(R.id.smsList);

                                        for (int i = 0; i < sms_arr.length(); i++) {
                                            JSONObject sms = sms_arr.getJSONObject(i);
                                           String id = sms.getString("text");
//                                            debugText.setText(id);
                                            sms_arr_str[i] = id;


                                        }
                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, sms_arr_str);
                                        smsList.setAdapter(adapter);

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







}
