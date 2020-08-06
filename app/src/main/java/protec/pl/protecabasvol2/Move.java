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
        getElementsFromIntent();
    }

    public void onBackPressed(){
        super.onBackPressed();
        Intent intent = new Intent(Move.this, Menu.class);
        intent.putExtra("password", getPassword());
        intent.putExtra("database", database);
        startActivity(intent);
    }

    public void getElementsFromIntent(){
        String password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        setPassword(password);
    }

    public void setIntent(String destination){
        try {
            Intent intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void takeArticles(View view){
        setIntent("MoveTakeArticle");
    }
    public void leaveArticles(View view){
        setIntent("MoveLeaveArticle");
    }
}