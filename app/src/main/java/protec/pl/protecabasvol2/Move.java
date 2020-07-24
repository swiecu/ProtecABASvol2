package protec.pl.protecabasvol2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class Move extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    String database;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move);
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        database = (getIntent().getStringExtra("database"));
    }
    public void onBackPressed(){
        super.onBackPressed();
        Intent intent = new Intent(Move.this, Menu.class);
        intent.putExtra("password", getPassword());
        intent.putExtra("database", database);
        startActivity(intent);
    }
    public void takeArticles(View view){
        Intent intent = new Intent(Move.this, MoveTakeArticle.class);
        intent.putExtra("password", getPassword());
        intent.putExtra("database", database);
        startActivity(intent);
    }
    public void leaveArticles(View view){
        Intent intent = new Intent(Move.this, MoveLeaveArticle.class);
        intent.putExtra("password", getPassword());
        intent.putExtra("database", database);
        startActivity(intent);
    }
}