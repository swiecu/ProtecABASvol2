package protec.pl.protecabasvol2;

import android.app.ProgressDialog;
import android.content.Intent;
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
    String database, user;
    ProgressDialog LoadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality_control);
        getElementsFromIntent();
    }

    public void onBackPressed(){
        super.onBackPressed();
        setIntent("Menu");
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
        setPassword(password);
    }

    public void setIntent(String destination){
        try {
            Intent intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("user", user);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void ProductionControl(View view){
        setIntent("QualityControlProduction");
    }

    public void ReportNonCompliance(View view){
        setIntent("QualityControlReportNonCompliance");
    }

    public void ToCheck(View view){
        LoadingDialog = ProgressDialog.show(QualityControl.this, "",
            "Ładowanie. Proszę czekać...", true);

        setIntent("QualityControlToCheck");
    }
}
