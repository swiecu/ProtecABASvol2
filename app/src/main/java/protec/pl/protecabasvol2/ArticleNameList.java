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
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.Query;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUser;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;

public class ArticleNameList extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx, sessionCtx;
    String destination, database, name, proof_Nr, stockID, vendor, purchaseOrder, userSwd;
    TextView suma, article_name;
    TableRow no_art;
    HashMap<String, String> tableRowsHM;
    ProgressDialog LoadingDialog;
    Handler handler;
    Method method;
    Intent intent;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_name_list);
        getElementsFromIntent();
        getElementsById();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(ArticleNameList.this, userSwd));
        drawTableByDescr();
        drawTableBySwd();
    }
    //na cofnięcie Back do tyłu
    public void onBackPressed() {
        getElementsById();
        if(no_art != null) {
            no_art.setVisibility(View.VISIBLE);
        }
        if (article_name != null) {
            article_name.setVisibility(View.GONE);
        }
        if (suma != null) {
            suma.setVisibility(View.GONE);
        }
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
        new setIntentAsyncTask().execute("");// cofnij do activity
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        if(ctx != null) {
            ctx.close();
        }
        super.onPause();
    }

    public void getElementsFromIntent() {
        password = (getIntent().getStringExtra("password"));
        setPassword(password);
        userSwd = getIntent().getStringExtra("userSwd");
        destination = getIntent().getStringExtra("destination");
        database = (getIntent().getStringExtra("database"));
        name = (getIntent().getStringExtra("content"));
        stockID = (getIntent().getStringExtra("stockID"));
        vendor = (getIntent().getStringExtra("vendor"));
        proof_Nr = (getIntent().getStringExtra("proof_Nr"));
        purchaseOrder = (getIntent().getStringExtra("purchaseOrder"));
        tableRowsHM = (HashMap<String, String>)getIntent().getSerializableExtra("tableRowsHM");
        if(tableRowsHM != null) {
            for (Map.Entry<String, String> entryTableRowsHM : tableRowsHM.entrySet()) {
                Log.d("productKey", entryTableRowsHM.getKey());
                Log.d("qtyValue", entryTableRowsHM.getValue());
            }
        }
    }

    public void getElementsById(){
         no_art = (TableRow) findViewById(R.id.no_articles);
         article_name = (TextView) findViewById(R.id.article_name);
         suma = (TextView) findViewById(R.id.suma);
    }

    public void setIntent(String finalArticleIDNO) {
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("art_idno", finalArticleIDNO);
            intent.putExtra("stockID", stockID);
            intent.putExtra("vendor", vendor);
            intent.putExtra("tableRowsHM", tableRowsHM);
            intent.putExtra("proof_Nr", proof_Nr);
            intent.putExtra("purchaseOrder", purchaseOrder);
            intent.putExtra("userSwd", userSwd);
            // startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(ArticleNameList.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = new ProgressDialog(ArticleNameList.this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
            loadDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loadDialog.setTitle("");
            loadDialog.setMessage("Ładowanie. Proszę czekać...");
            loadDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            String content = strings[0];
            setIntent(content);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTableByDescr(){
        try{
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<Product> productSB = SelectionBuilder.create(Product.class);
            Query<Product> productQuery = ctx.createQuery(productSB.build());
            productSB.add(Conditions.matchIgCase(Product.META.descr6.toString(), name));
            for (Product product : productQuery) {
                if(destination.equals("MoveTakeArticle")){
                    if(product.getSchedStock().compareTo(BigDecimal.ZERO) == 1 ){
                        drawTable(product);
                    }
                }else{
                    drawTable(product);
                }
            }
            ctx.close();
        }catch (DBRuntimeException e) {
            catchExceptionCases(e, "drawTableByDescr");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTableBySwd() {
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<Product> productSB = SelectionBuilder.create(Product.class);
            Query<Product> productQuery = ctx.createQuery(productSB.build());
            productSB.add(Conditions.matchIgCase(Product.META.swd.toString(), name));
            for (Product product : productQuery) {
                drawTable(product);
            }
            ctx.close();
        }catch (DBRuntimeException e) {
            Log.d("error", e.getMessage());
            catchExceptionCases(e, "drawTableBySwd");
        }
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function){
        if(e.getMessage().contains("failed")){
            GlobalClass.showDialog(this,"Brak połączenia!","Nie można się aktualnie połączyć z bazą.", "OK",new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { } });
        }else if(e.getMessage().contains("FULL")){
            LoadingDialog = new ProgressDialog(ArticleNameList.this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
            LoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            LoadingDialog.setTitle("     Przekroczono liczbę licencji.");
            LoadingDialog.setMessage("Zwalniam miejsce w ABAS. Proszę czekać...");
            LoadingDialog.show();
            new Thread(() -> {
                sessionCtx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", "sesje", "mobileApp");  // hasło sesje aby mieć dostęp
                GlobalClass.licenceCleaner(sessionCtx);
                sessionCtx.close();
                handler.sendEmptyMessage(0);
            }).start();
            handler = new Handler() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                public void handleMessage(Message msg) {
                    LoadingDialog.dismiss();
                    try {
                        method = Class.forName("protec.pl.protecabasvol2.ArticleNameList").getMethod(function, String.class);
                        method.invoke("protec.pl.protecabasvol2.ArticleNameList", function);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(Product product){
        String article = product.getSwd();
        String article_name = product.getDescr6();
        String articleIDNO = product.getIdno();

        TableLayout layoutList = (TableLayout) findViewById(R.id.articleNameTable);
        TableRow tableRowList = new TableRow(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        tableRowList.setLayoutParams(lp);
        tableRowList.setBackgroundColor(Color.parseColor("#BDBBBB"));

        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
        params.setMargins(1, 1, 1, 1);

        TextView id = new TextView(this);
        TextView articleView = new TextView(this);
        TextView article_nameView = new TextView(this);

        Integer j = layoutList.getChildCount();
        //j = j - 1;
        if (j % 2 == 0) {

            id.setBackgroundColor(Color.parseColor("#E5E5E6"));
            articleView.setBackgroundColor(Color.parseColor("#E5E5E6"));
            article_nameView.setBackgroundColor(Color.parseColor("#E5E5E6"));

        } else {
            id.setBackgroundColor(Color.parseColor("#FFFFFF"));
            articleView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            article_nameView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }

        id.setText((j).toString());
        id.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        id.setTextColor(Color.parseColor("#808080"));
        id.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        id.setPadding(5, 20, 5, 20);
        id.setLayoutParams(params);

        articleView.setText(article);
        articleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        articleView.setTextColor(Color.parseColor("#808080"));
        articleView.setTypeface(Typeface.DEFAULT_BOLD);
        articleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        articleView.setPadding(5, 20, 5, 20);
        articleView.setLayoutParams(params);

        article_nameView.setText(article_name);
        article_nameView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        article_nameView.setTextColor(Color.parseColor("#808080"));
        article_nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        article_nameView.setPadding(5, 20, 5, 20);
        article_nameView.setLayoutParams(params);

        tableRowList.addView(id);
        tableRowList.addView(articleView);
        tableRowList.addView(article_nameView);
        layoutList.addView(tableRowList, j);

        String finalArticleIDNO = articleIDNO;
        tableRowList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ctx.close();
                new setIntentAsyncTask().execute(finalArticleIDNO);
            }
        });
    }
    public String getEmployeeSwd(){
        DbContext ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", getPassword(), "mobileApp");
        IsPrLoggedUser lu = ctx.openInfosystem(IsPrLoggedUser.class);
        String userSwd = lu.getYuser();
        ctx.close();
        return userSwd;
    }
}