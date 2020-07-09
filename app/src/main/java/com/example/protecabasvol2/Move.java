package com.example.protecabasvol2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import de.abas.erp.db.DbContext;

public class Move extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move);
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
    }
    public void onBackPressed(){
        super.onBackPressed();
        Intent intent = new Intent(Move.this, Menu.class);
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
    public void takeArticles(View view){
        Intent intent = new Intent(Move.this, MoveTakeArticle.class);
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
    public void leaveArticles(View view){
        Intent intent = new Intent(Move.this, MoveLeaveArticle.class);
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
}