package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.infosystem.custom.owpl.IsPrQualityControlCheck;
import de.abas.erp.db.schema.capacity.WorkCenter;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.util.ContextHelper;

public class QualityControlToCheck extends AppCompatActivity {
    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    DbContext ctx; ProgressDialog LoadingDialog;
    String user, database, userSwd;
    ImageView refreshIcon; TableLayout layoutTable; Intent intent;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality_control_to_check);
        getElementsFromIntent();
        getElementsById();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(QualityControlToCheck.this, userSwd));
        LoadingDialog = ProgressDialog.show(QualityControlToCheck.this, "",
                "Ładowanie. Proszę czekać...", true);
        openInfosystem();
    }

    // na kliknięcie cofnij
    public void onBackPressed() {
        new setIntentAsyncTask().execute("QualityControl");
        super.onBackPressed();
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        GlobalClass.dismissLoadingDialog(LoadingDialog);
        super.onStop();
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        GlobalClass.ctxClose(ctx);
        super.onPause();
    }

    public void getElementsFromIntent(){
        String password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        setPassword(password);
        user = (getIntent().getStringExtra("user"));
        userSwd = getIntent().getStringExtra("userSwd");
    }

    public void getElementsById(){
        refreshIcon = (ImageView)findViewById(R.id.refreshImage);
        layoutTable = (TableLayout) findViewById(R.id.qualityToCheckTable);
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(QualityControlToCheck.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(QualityControlToCheck.this, "",
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void openInfosystem(){
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", getPassword(), "mobileApp");
        IsPrQualityControlCheck qualityControl = ctx.openInfosystem(IsPrQualityControlCheck.class);
        qualityControl.invokeStart();
        Iterable<IsPrQualityControlCheck.Row> qualityControlRows = qualityControl.getTableRows();
        for (IsPrQualityControlCheck.Row rowQuality : qualityControlRows){
            drawTable(rowQuality);
        }
        GlobalClass.dismissLoadingDialog(LoadingDialog);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(IsPrQualityControlCheck.Row rowQuality){
        String workCard = rowQuality.getWorkcard().getIdno(), productSwd = rowQuality.getYproduct().getSwd(), productName = ((Product)rowQuality.getYproductdescr()).getDescr6(),
                employee = rowQuality.getYemployee().getDescrOperLang(),
                machineGroup = ((WorkCenter)rowQuality.getYmachinegroupname()).getDescr6();
        if(machineGroup.equals("")){
            machineGroup = ((WorkCenter)rowQuality.getYmachinegroup()).getDescrOperLang();
        }
        TableRow tableRowList = GlobalClass.setTableRowList(this);
        TextView id = new TextView(this);
        TextView workCardView = new TextView(this);
        TextView articleView = new TextView(this);
        TextView articleNameView = new TextView(this);
        TextView employeeView = new TextView(this);
        TextView machineGroupView = new TextView(this);

        TextView[] textViewArray = {id, workCardView, articleView, articleNameView, employeeView, machineGroupView};
        Integer j = layoutTable.getChildCount();

        for (TextView textView :textViewArray) {
            if (j % 2 == 0) {
                textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
            } else {
                textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }
        GlobalClass.setParamForTextView(id, (j).toString(), 12, 20, 10, false);
        GlobalClass.setParamForTextView(workCardView, workCard, 12, 20, 10, true);
        GlobalClass.setParamForTextView(articleView, productSwd, 12, 20, 10, false);
        GlobalClass.setParamForTextView(articleNameView, productName, 12, 20, 10, false);
        GlobalClass.setParamForTextView(employeeView, employee, 12, 20, 10, false);
        GlobalClass.setParamForTextView(machineGroupView, machineGroup, 12, 20, 10, false);

        for (TextView textView :textViewArray) {
            tableRowList.addView(textView);
        }
        layoutTable.addView(tableRowList, j);
        tableRowList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder addNewControlAlert = new AlertDialog.Builder(new ContextThemeWrapper(QualityControlToCheck.this, R.style.MyDialog));
                String elementeString = "<b>Dodaj nową kontrolę jakości.</b>";
                addNewControlAlert.setMessage(Html.fromHtml(elementeString));
                addNewControlAlert.setPositiveButton("Dodaj",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                GlobalClass.ctxClose(ctx);
                                new setIntentAsyncTask().execute("QualityControlProduction");
                            }
                        });
                addNewControlAlert.setNegativeButton("Anuluj",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                GlobalClass.ctxClose(ctx);
                            }
                        });
                addNewControlAlert.setCancelable(true);
                addNewControlAlert.create().show();
            }
        });
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void refresh (View view){
        LoadingDialog = new ProgressDialog(this , R.style.MyDialog);
        LoadingDialog.setMessage("Ładowanie. Proszę czekać...");
        LoadingDialog.show();

        new CountDownTimer(500, 100) {
            public void onFinish() {
                layoutTable.removeViews(1, layoutTable.getChildCount()-1);
                openInfosystem();
            }
            public void onTick(long millisUntilFinished) {
            }
        }.start();
    }
}