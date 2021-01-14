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
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.View;
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
    DbContext ctx, sessionCtx;
    ProgressDialog LoadingDialog;
    String database, stockID, userSwd;
    TableLayout myStoctakingkTable;
    TableRow stockTableRow;
    StocktakingProtec stocktaking;
    Intent intent;
    Handler handler;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stocktaking_list);
        getElementsFromIntent();
        getElementsById();
        setLook();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MyStocktakingList.this, userSwd));
        drawTable();
    }

    public void onBackPressed() {
        super.onBackPressed();
        new setIntentAsyncTask().execute("Stocktaking");
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

    public void getElementsById() {
        myStoctakingkTable = (TableLayout) findViewById(R.id.myStoctakingkTable);
        stockTableRow = (TableRow) findViewById(R.id.StockTableRow);
    }

    public void setLook() {
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
                String article_text = row.getYarticle().getSwd();
                String countedQty_text = row.getYcountedqty().stripTrailingZeros().toPlainString();
                //String unit_text = row.getYarticle().getSU().toString();
                String unit_text = row.getYarticle().getString("SU");
                String storagePlace_text = row.getYstorageplace().getSwd();
                String articleName_text = row.getYarticle().getDescr6();
                if (unit_text.equals("(20)")) { // jeśli jednostka to szt.
                    unit_text = "szt.";
                } else if (unit_text.equals("(7)")) { //jeśli jednostka to kg
                    unit_text = "kg";
                } else if (unit_text.equals("(21)")) { // jeśli jednostka to kpl
                    unit_text = "kpl";
                }else if (unit_text.equals("(1)")) { // jeśli jednostka to m
                    unit_text = "m";
                }else if (unit_text.equals("(10)")) { // jeśli jednostka to tona
                    unit_text = "tona";
                }else if (unit_text.equals("(28)")) { // jeśli jednostka to arkusz
                    unit_text = "arkusz";
                }

                TableLayout stocktakingList = (TableLayout) findViewById(R.id.myStoctakingkTable);
                TableRow tableRowList = new TableRow(this);
                TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                tableRowList.setLayoutParams(lp);
                tableRowList.setBackgroundColor(Color.parseColor("#BDBBBB"));

                TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                params.setMargins(1, 1, 1, 1);

                TextView article = new TextView(this);
                TextView countedQty = new TextView(this);
                TextView unit= new TextView(this);
                TextView storagePlace = new TextView(this);
                TextView articleName = new TextView(this);

                Integer j = stocktakingList.getChildCount();
                //j = j - 1;
                if (j % 2 == 0) {
                    article.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    countedQty.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    unit.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    storagePlace.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    articleName.setBackgroundColor(Color.parseColor("#E5E5E6"));
                } else {
                    article.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    countedQty.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    unit.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    storagePlace.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    articleName.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }

                article.setText(article_text);
                article.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                article.setTypeface(Typeface.DEFAULT_BOLD);
                article.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
                article.setPadding(5, 20, 5, 20);
                article.setLayoutParams(params);

                countedQty.setText(countedQty_text);
                countedQty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                countedQty.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
                countedQty.setPadding(5, 20, 5, 20);
                countedQty.setLayoutParams(params);

                unit.setText(unit_text);
                unit.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                unit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
                unit.setPadding(5, 20, 5, 20);
                unit.setLayoutParams(params);

                storagePlace.setText(storagePlace_text);
                storagePlace.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                storagePlace.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
                storagePlace.setPadding(5, 20, 5, 20);
                storagePlace.setLayoutParams(params);

                articleName.setText(articleName_text);
                articleName.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                articleName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
                articleName.setPadding(5, 20, 5, 20);
                articleName.setLayoutParams(params);

                tableRowList.addView(article);
                tableRowList.addView(countedQty);
                tableRowList.addView(unit);
                tableRowList.addView(storagePlace);
                tableRowList.addView(articleName);
                stocktakingList.addView(tableRowList, j);
            }
            if(ctx != null) {
                ctx.close();
            }
        }
    }

    public StocktakingProtec getStocktaing(){
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<StocktakingProtec> stocktakingSB = SelectionBuilder.create(StocktakingProtec.class);
            stocktakingSB.add(Conditions.eq(StocktakingProtec.META.idno, stockID));
            stocktaking = QueryUtil.getFirst(ctx, stocktakingSB.build());
            if(ctx != null) {
                ctx.close();
            }
        }catch (DBRuntimeException e) {
            if(LoadingDialog != null) {
                LoadingDialog.dismiss();
            }
            catchExceptionCases(e);
        }
        return stocktaking;
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e) {
        if (e.getMessage().contains("failed")) {
            GlobalClass.showDialog(this, "Brak połączenia!", "Nie można się aktualnie połączyć z bazą.", "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            //przekroczona liczba licencji
        } else if (e.getMessage().contains("FULL")) {
            LoadingDialog = ProgressDialog.show(MyStocktakingList.this, "     Przekroczono liczbę licencji.",
                    "Zwalniam miejsce w ABAS. Proszę czekać...", true);
            new Thread(() -> {
                sessionCtx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", "sesje", "mobileApp");  // hasło sesje aby mieć dostęp
                GlobalClass.licenceCleaner(sessionCtx);
                sessionCtx.close();
                handler.sendEmptyMessage(0);
            }).start();
            handler = new Handler() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                public void handleMessage(Message msg) {
                    if (LoadingDialog != null) {
                        LoadingDialog.dismiss();
                    }
                    drawTable();
                }
            };
        }
    }
}