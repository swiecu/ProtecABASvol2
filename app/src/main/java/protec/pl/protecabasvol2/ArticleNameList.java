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
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.Query;
import de.abas.erp.db.exception.DBRuntimeException;
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
    String destination, database, name, proof_Nr, stockID, vendor, purchaseOrder, userSwd, docQty;
    TextView suma, article_name; TableRow no_art;
    HashMap<String, String> tableRowsHM; ProgressDialog LoadingDialog;
    Handler handler; Method method; Intent intent;

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
        GlobalClass.dismissLoadingDialog(LoadingDialog);
        new setIntentAsyncTask().execute("");// cofnij do activity
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        GlobalClass.ctxClose(ctx);
        super.onPause();
    }

    public void getElementsFromIntent() {
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        userSwd = getIntent().getStringExtra("userSwd");
        destination = getIntent().getStringExtra("destination");
        database = (getIntent().getStringExtra("database"));
        name = (getIntent().getStringExtra("content"));
        stockID = (getIntent().getStringExtra("stockID"));
        vendor = (getIntent().getStringExtra("vendor"));
        proof_Nr = (getIntent().getStringExtra("proof_Nr"));
        purchaseOrder = (getIntent().getStringExtra("purchaseOrder"));
        docQty = getIntent().getStringExtra("docQty");
        tableRowsHM = (HashMap<String, String>)getIntent().getSerializableExtra("tableRowsHM");
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
            intent.putExtra("docQty", docQty);
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
            GlobalClass.ctxClose(ctx);
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
            GlobalClass.ctxClose(ctx);
        }catch (DBRuntimeException e) {
            catchExceptionCases(e, "drawTableBySwd");
        }
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function){

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
        String article = product.getSwd(), article_name = product.getDescr6(), articleIDNO = product.getIdno();

        TableLayout layoutList = (TableLayout) findViewById(R.id.articleNameTable);
        TableRow tableRowList = GlobalClass.setTableRowList(this);

        TextView id = new TextView(this);
        TextView articleView = new TextView(this);
        TextView article_nameView = new TextView(this);
        TextView[] textViewArray = {id, articleView, article_nameView};
        Integer j = layoutList.getChildCount();

        for (TextView textView :textViewArray) {
            if (j % 2 == 0) {
                textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
            } else {
                textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }

        GlobalClass.setParamForTextView(id, j.toString(), 12, 20, 5, false);
        GlobalClass.setParamForTextView(articleView, article, 12, 20, 5, false);
        GlobalClass.setParamForTextView(article_nameView, article_name, 12, 20, 5, false);
        for (TextView textView :textViewArray) {
            tableRowList.addView(textView);
        }
        layoutList.addView(tableRowList, j);

        String finalArticleIDNO = articleIDNO;
        tableRowList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new setIntentAsyncTask().execute(finalArticleIDNO);
            }
        });
    }
}