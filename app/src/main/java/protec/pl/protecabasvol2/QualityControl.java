package protec.pl.protecabasvol2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class QualityControl extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    String database, user, userSwd;
    ProgressDialog LoadingDialog;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality_control);
        getElementsFromIntent();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(QualityControl.this, userSwd));
    }

    public void onBackPressed(){
        super.onBackPressed();
        new setIntentAsyncTask().execute("Menu");
    }
    // na wyjście z actvity
    @Override
    protected void onStop(){
        super.onStop();
        if (LoadingDialog != null){
            LoadingDialog.dismiss();
        }
    }

    public void getElementsFromIntent(){
        String password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        user = (getIntent().getStringExtra("user"));
        userSwd = getIntent().getStringExtra("userSwd");
        setPassword(password);
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(QualityControl.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(QualityControl.this, "",
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
            intent.putExtra("user", user);
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void ProductionControl(View view){
        new setIntentAsyncTask().execute("QualityControlProduction");
    }

    public void ReportNonCompliance(View view){
        new setIntentAsyncTask().execute("QualityControlReportNonCompliance");
    }

    public void ToCheck(View view){
        LoadingDialog = ProgressDialog.show(QualityControl.this, "",
            "Ładowanie. Proszę czekać...", true);
        new setIntentAsyncTask().execute("QualityControlToCheck");
    }
}
