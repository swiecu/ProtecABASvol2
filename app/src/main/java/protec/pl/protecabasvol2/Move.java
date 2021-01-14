package protec.pl.protecabasvol2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
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
    String database, userSwd;
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move);
        getElementsFromIntent();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(Move.this, userSwd));
    }

    public void onBackPressed(){
        super.onBackPressed();
        new setIntentAsyncTask().execute("Menu");
    }

    public void getElementsFromIntent(){
        String password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        userSwd = getIntent().getStringExtra("userSwd");
        setPassword(password);
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(Move.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(Move.this, "",
                    "Ładowanie. Proszę czekać...", true);
        }

        @Override
        protected String doInBackground(String... strings) {
            String destination = strings[0];
            setIntent(destination);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
        }
    }
    public void setIntent(String destination){
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void takeArticles(View view){
        new setIntentAsyncTask().execute("MoveTakeArticle");
    }
    public void leaveArticles(View view){
        new setIntentAsyncTask().execute("MoveLeaveArticle");
    }
}