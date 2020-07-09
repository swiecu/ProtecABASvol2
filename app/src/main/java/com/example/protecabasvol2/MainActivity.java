package com.example.protecabasvol2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

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
    // CHECK STOCK
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void ScanQR(View view){
        try{
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować hasło");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            Intent intent = integrator.createScanIntent();
            startActivityForResult(intent , 101);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
            startActivity(marketIntent);
        }
    }
    // ON ACTIVITY RESULT
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data);
        if(result!= null){
            if (requestCode == 101) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    EditText password_text = findViewById(R.id.password_text);
                    password_text.setText(content);
                    findViewById(R.id.login_btn).callOnClick();
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // on Login Button Click
   public void login(View view)  {
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
            }catch (DBRuntimeException e) {

                if(e.getMessage().contains("password")){
                    AlertDialog.Builder wrongPasswAlert  = new AlertDialog.Builder(MainActivity.this);
                    wrongPasswAlert.setMessage("Podane hasło jest błędne.");
                    wrongPasswAlert.setTitle("Błędne hasło!"); //Brak połączenia!/ Błędne hasło!"
                    wrongPasswAlert.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //dismiss the dialog
                                }
                            });
                    wrongPasswAlert.setCancelable(true);
                    wrongPasswAlert.create().show();
                }else if(e.getMessage().contains("failed")){
                    AlertDialog.Builder InfoAlert  = new AlertDialog.Builder(MainActivity.this);
                    InfoAlert.setMessage("Nie można się aktualnie połączyć z bazą.");
                    InfoAlert.setTitle("Brak połączenia!"); //Brak połączenia!/ Błędne hasło!"
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