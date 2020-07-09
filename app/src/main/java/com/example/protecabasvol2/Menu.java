package com.example.protecabasvol2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.TextView;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUsers;
import de.abas.erp.db.util.ContextHelper;

public class Menu extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx;
    String user = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        DbContext ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
        IsPrLoggedUsers lu = ctx.openInfosystem(IsPrLoggedUsers.class);
        user = lu.getYuser();
        TextView loggedUser = findViewById(R.id.loggedUser);
        loggedUser.setText("Aktualnie zalogowany użytkownik: " + user);
        ctx.close();
    }
    public void onBackPressed(){
        super.onBackPressed();
        Intent intent = new Intent(Menu.this, MainActivity.class);
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }

    public void checkStock (View view){
        Intent intent = new Intent(this, StockInformation.class );
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
    public void move (View view){
        Intent intent = new Intent(this, Move.class );
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
}