package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.TypedValue;
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

import static android.graphics.Color.parseColor;

public class QualityControlToCheck extends AppCompatActivity {
    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    DbContext ctx;
    ProgressDialog LoadingDialog;
    String user, database, userSwd;
    ImageView refreshIcon;
    TableLayout layoutTable;
    Intent intent;

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
        super.onBackPressed();
        new setIntentAsyncTask().execute("QualityControl");
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        super.onStop();
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        if(ctx != null) {
            ctx.close();
        }
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
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(IsPrQualityControlCheck.Row rowQuality){
        String workCard = rowQuality.getWorkcard().getIdno();
        String productSwd = rowQuality.getYproduct().getSwd();
        String productName = ((Product)rowQuality.getYproductdescr()).getDescr6();
        String employee = rowQuality.getYemployee().getDescrOperLang();
        String machineGroup;
        machineGroup = ((WorkCenter)rowQuality.getYmachinegroupname()).getDescr6();
        if(machineGroup.equals("")){
            machineGroup = ((WorkCenter)rowQuality.getYmachinegroup()).getDescrOperLang();
        }

        TableRow tableRowList = new TableRow(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        tableRowList.setLayoutParams(lp);
        tableRowList.setBackgroundColor(parseColor("#BDBBBB"));

        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
        params.setMargins(1, 1, 1, 1);

        TextView id = new TextView(this);
        TextView workCardView = new TextView(this);
        TextView articleView = new TextView(this);
        TextView articleNameView = new TextView(this);
        TextView employeeView = new TextView(this);
        TextView machineGroupView = new TextView(this);

        Integer j = layoutTable.getChildCount();
        //j = j - 1;
        if (j % 2 == 0) {
            id.setBackgroundColor(parseColor("#E5E5E6"));
            workCardView.setBackgroundColor(parseColor("#E5E5E6"));
            articleView.setBackgroundColor(parseColor("#E5E5E6"));
            articleNameView.setBackgroundColor(parseColor("#E5E5E6"));
            employeeView.setBackgroundColor(parseColor("#E5E5E6"));
            machineGroupView.setBackgroundColor(parseColor("#E5E5E6"));

        } else {
            id.setBackgroundColor(parseColor("#FFFFFF"));
            workCardView.setBackgroundColor(parseColor("#FFFFFF"));
            articleView.setBackgroundColor(parseColor("#FFFFFF"));
            articleNameView.setBackgroundColor(parseColor("#FFFFFF"));
            employeeView.setBackgroundColor(parseColor("#FFFFFF"));
            machineGroupView.setBackgroundColor(parseColor("#FFFFFF"));
        }

        id.setText((j).toString());
        id.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        id.setTextColor(Color.parseColor("#808080"));
        id.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        id.setPadding(10, 20, 10, 20);
        id.setLayoutParams(params);

        workCardView.setText(workCard);
        workCardView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        workCardView.setTextColor(parseColor("#808080"));
        workCardView.setTypeface(Typeface.DEFAULT_BOLD);
        workCardView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        workCardView.setPadding(10, 20, 10, 20);
        workCardView.setLayoutParams(params);

        articleView.setText(productSwd);
        articleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        articleView.setTextColor(parseColor("#808080"));
        articleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        articleView.setPadding(10, 20, 10, 20);
        articleView.setLayoutParams(params);

        articleNameView.setText(productName);
        articleNameView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        articleNameView.setTextColor(parseColor("#808080"));
        articleNameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        articleNameView.setPadding(10, 20, 10, 20);
        articleNameView.setLayoutParams(params);

        employeeView.setText(employee);
        employeeView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        employeeView.setTextColor(parseColor("#808080"));
        employeeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        employeeView.setPadding(10, 20, 10, 20);
        employeeView.setLayoutParams(params);

        machineGroupView.setText(machineGroup);
        machineGroupView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        machineGroupView.setTextColor(parseColor("#808080"));
        machineGroupView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        machineGroupView.setPadding(10, 20, 10, 20);
        machineGroupView.setLayoutParams(params);

        tableRowList.addView(id);
        tableRowList.addView(workCardView);
        tableRowList.addView(articleView);
        tableRowList.addView(articleNameView);
        tableRowList.addView(employeeView);
        tableRowList.addView(machineGroupView);

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
                            ctx.close();
                            new setIntentAsyncTask().execute("QualityControlProduction");
                        }
                    });
                addNewControlAlert.setNegativeButton("Anuluj",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ctx.close();
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
                // millisUntilFinished    The amount of time until finished.
            }
        }.start();

    }
}