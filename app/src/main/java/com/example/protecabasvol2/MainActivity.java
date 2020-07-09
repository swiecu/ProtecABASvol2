package com.example.protecabasvol2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.util.ContextHelper;

public class MainActivity extends AppCompatActivity {
   DbContext ctx;
   ProgressDialog LoadingDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //po wyjściu z ekranu chowa Loading Dialog
    @Override
    protected void  onStop(){
        super.onStop();
        if (LoadingDialog != null){
            LoadingDialog.dismiss();
        }
    }
    //on QR Button Click
   public void ScanQR(View view) {
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
            startActivityForResult(intent, 0);
            onActivityResult(0, 0, intent);

        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
            startActivity(marketIntent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                EditText password_text = findViewById(R.id.password_text);
                password_text.setText(contents);
                findViewById(R.id.login_btn).callOnClick();
            }
            if(resultCode == RESULT_CANCELED){
                //handle cancel
            }
        }
    }

    // on Login Button Click
   public void login(View view){
        EditText password_text = findViewById(R.id.password_text);
        String password = password_text.getText().toString();
        if(!password.equals("")){
            try {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", password, "mobileApp");
                Intent intent = new Intent(this, Menu.class);
                intent.putExtra("password", password);
                startActivity(intent);
                LoadingDialog =  ProgressDialog.show(MainActivity.this, "",
                        "Ładowanie. Proszę czekać...", true);
               //ctx.close();

            }catch (DBRuntimeException e) {
                AlertDialog.Builder InfoAlert  = new AlertDialog.Builder(MainActivity.this);
                InfoAlert.setMessage("Nie można aktualnie połączyć z bazą lub podane hasło jest błędne." + e.getMessage());
                InfoAlert.setTitle(e.getMessage()); //Brak połączenia!/ Błędne hasło!"
                InfoAlert.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //dismiss the dialog
                            }
                        });
                InfoAlert.setCancelable(true);
                InfoAlert.create().show();
            }

         // jeśli hasło nie zostało wpisane
        }else{
            AlertDialog.Builder InfoAlert  = new AlertDialog.Builder(MainActivity.this);
            InfoAlert.setMessage("Proszę wpisać hasło.");
            InfoAlert.setTitle("Brak hasła!");
            InfoAlert.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //dismiss the dialog
                        }
                    });
            InfoAlert.setCancelable(true);
            InfoAlert.create().show();
        }
   }
}