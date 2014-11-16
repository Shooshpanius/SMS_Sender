package ru.in_da_house.smssender;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


public class SettingsActivity extends Activity {

    public static final String APP_PREFERENCES = "sms_settings";
    public static final String APP_PREFERENCES_LOGIN = "login";
    public static final String APP_PREFERENCES_PASSWORD = "password";
    SharedPreferences mSettings;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);


        if(mSettings.contains(APP_PREFERENCES_LOGIN)) {
            TextView editLogin = (TextView)findViewById(R.id.editLogin);
            editLogin.setText(mSettings.getString(APP_PREFERENCES_LOGIN, ""));
        }
        if(mSettings.contains(APP_PREFERENCES_PASSWORD)) {
            TextView editPassword = (TextView)findViewById(R.id.editPassword);
            editPassword.setText(mSettings.getString(APP_PREFERENCES_PASSWORD, ""));
        }

        ImageButton buttonCloseSettings = (ImageButton)findViewById(R.id.closeSettings);
        buttonCloseSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });


        ImageButton buttonSaveSettings = (ImageButton)findViewById(R.id.saveSettings);
        buttonSaveSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences.Editor editor = mSettings.edit();

                TextView editLogin = (TextView)findViewById(R.id.editLogin);
                if (editLogin.getText().toString().length() !=0) {
                    editor.putString(APP_PREFERENCES_LOGIN, editLogin.getText().toString());
                } else {
                    editor.remove(APP_PREFERENCES_LOGIN);
                }

                TextView editPassword = (TextView)findViewById(R.id.editPassword);
                if (editPassword.getText().toString().length() !=0) {
                    editor.putString(APP_PREFERENCES_PASSWORD, editPassword.getText().toString());
                } else {
                    editor.remove(APP_PREFERENCES_PASSWORD);
                }

                editor.commit();

                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                finish();
                startActivity(intent);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.closeSettings) {
            setResult(Activity.RESULT_OK);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
