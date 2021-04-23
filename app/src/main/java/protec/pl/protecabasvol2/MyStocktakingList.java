package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.schema.custom.protec.StocktakingProtec;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class MyStocktakingList extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx, sessionCtx;ProgressDialog LoadingDialog;
    String database, stockID, userSwd;
    TableLayout myStoctakingkTable; TableRow stockTableRow;
    StocktakingProtec stocktaking; Intent intent; Handler handler;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stocktaking_list);
        getElementsFromIntent();
        getElementsById();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MyStocktakingList.this, userSwd));
        drawTable();
    }

    public void onBackPressed() {
        new setIntentAsyncTask().execute("Stocktaking");
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

    public void getElementsById() {
        myStoctakingkTable = (TableLayout) findViewById(R.id.myStoctakingkTable);
        stockTableRow = (TableRow) findViewById(R.id.StockTableRow);
    }

    public void getElementsFromIntent() {
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        database = (getIntent().getStringExtra("database"));
        stockID = (getIntent().getStringExtra("stockID"));
        userSwd = getIntent().getStringExtra("userSwd");
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(MyStocktakingList.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(MyStocktakingList.this, "",
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

    public void setIntent(String destination) {
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("stockID", stockID);
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(){
        stocktaking = getStocktaing();
        Iterable<StocktakingProtec.Row> stocktakingRows = stocktaking.getTableRows();
        Integer nrRows = stocktaking.getRowCount();
        if (nrRows != 0) {
            for (StocktakingProtec.Row row : stocktakingRows) {
                String article_text = row.getYarticle().getSwd(), countedQty_text = row.getYcountedqty().stripTrailingZeros().toPlainString(),
                        unit_text = row.getYarticle().getString("SU"), storagePlace_text = row.getYstorageplace().getSwd(),
                        articleName_text = row.getYarticle().getDescr6();
                unit_text = GlobalClass.getProperUnit(unit_text);

                TableLayout stocktakingList = (TableLayout) findViewById(R.id.myStoctakingkTable);
                TableRow tableRowList = GlobalClass.setTableRowList(this);

                TextView article = new TextView(this);
                TextView countedQty = new TextView(this);
                TextView unit= new TextView(this);
                TextView storagePlace = new TextView(this);
                TextView articleName = new TextView(this);

                TextView[] textViewArray = {article, countedQty, unit, storagePlace, articleName};
                Integer j = stocktakingList.getChildCount();
                for (TextView textView:textViewArray) {
                    if (j % 2 == 0) {
                        textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    } else {
                        textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    }
                }
                GlobalClass.setParamForTextView(article, article_text, 13, 22, 7, true);
                GlobalClass.setParamForTextView(countedQty, countedQty_text, 13, 22, 7, false);
                GlobalClass.setParamForTextView(unit, unit_text, 13, 22, 7, false);
                GlobalClass.setParamForTextView(storagePlace, storagePlace_text, 13, 22, 7, false);
                GlobalClass.setParamForTextView(articleName, articleName_text, 13, 22, 7, false);

                for (TextView textView :textViewArray) {
                    tableRowList.addView(textView);
                }
                stocktakingList.addView(tableRowList, j);
            }
            GlobalClass.ctxClose(ctx);
        }
    }

    public StocktakingProtec getStocktaing(){
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<StocktakingProtec> stocktakingSB = SelectionBuilder.create(StocktakingProtec.class);
            stocktakingSB.add(Conditions.eq(StocktakingProtec.META.idno, stockID));
            stocktaking = QueryUtil.getFirst(ctx, stocktakingSB.build());
            GlobalClass.ctxClose(ctx);
        }catch (DBRuntimeException e) {
            GlobalClass.dismissLoadingDialog(LoadingDialog);
            catchExceptionCases(e);
        }
        return stocktaking;
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e) {
        GlobalClass.catchExceptionCases(e, this);
        if (e.getMessage().contains("FULL")) { //przekroczona liczba licencji
            LoadingDialog = GlobalClass.getDialogForLicences(this);
            LoadingDialog.show();
            new Thread(() -> {
                sessionCtx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", "sesje", "mobileApp");  // hasło sesje aby mieć dostęp
                GlobalClass.licenceCleaner(sessionCtx);
                GlobalClass.ctxClose(sessionCtx);
                handler.sendEmptyMessage(0);
            }).start();
            handler = new Handler() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                public void handleMessage(Message msg) {
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                drawTable();
                }
            };
        }
    }
}