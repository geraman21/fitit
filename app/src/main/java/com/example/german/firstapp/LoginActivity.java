package com.example.german.firstapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {
    @Override
    public void onBackPressed() {
        Intent minimize = new Intent(Intent.ACTION_MAIN);
        minimize.addCategory(Intent.CATEGORY_HOME);
        startActivity(minimize);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int)(width*.8), (int)(height*.3));

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y =  -20;

        getWindow().setAttributes(params);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Write your alias");
        setSupportActionBar(toolbar);

        Button loginButton = (Button) findViewById(R.id.loginButton);
        final TextView loginInformation = (TextView) findViewById(R.id.loginInformation);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String login = loginInformation.getText().toString();
                if(login.length()<=20 && !login.isEmpty() && !login.contains("]") && !login.contains("["))
                {
                    Intent loginIntent = new Intent();
                    loginIntent.putExtra("login", login);
                    setResult(RESULT_OK, loginIntent);
                    finish();
                }
                else if(login.length()>=20) {
                    Toast.makeText(LoginActivity.this, "Alias is too long", Toast.LENGTH_SHORT).show();
                }
                else if(login.toString().isEmpty()){
                    Toast.makeText(LoginActivity.this, "Alias cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
