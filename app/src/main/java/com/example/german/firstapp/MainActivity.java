package com.example.german.firstapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    String login ="before inputing log in";
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                login = data.getStringExtra("login");
                System.out.println("german1: " + login);
            }
            if (resultCode == RESULT_CANCELED) {
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton libraryButton = (ImageButton) findViewById(R.id.libraryButton);
        ImageButton fablabButton = (ImageButton) findViewById(R.id.fabLabButton);
        ImageButton mensaButton = (ImageButton) findViewById(R.id.mensaButton);

        final AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.8F);

        final Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivityForResult(loginIntent, 1);

        System.out.println("german1: " + this.login);

        libraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openLibrary = new Intent(getApplicationContext(), LibraryActivity.class);
                openLibrary.putExtra("login", login);
                view.startAnimation(buttonClick);
                startActivity(openLibrary);
            }
        });

        fablabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openFablab = new Intent(getApplicationContext(), FablabActivity.class);
                openFablab.putExtra("login", login);
                view.startAnimation(buttonClick);
                startActivity(openFablab);
            }
        });

        mensaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openMensa = new Intent(getApplicationContext(), MensaActivity.class);
                openMensa.putExtra("login", login);
                view.startAnimation(buttonClick);
                startActivity(openMensa);
            }
        });
    }
}