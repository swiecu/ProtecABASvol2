package protec.pl.protecabasvol2;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;

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
    ProgressDialog LoadingDialog;
    GlobalClass SearchArticleGlob;
    DbContext ctx;
    String destination, database, name, positiveButtonText, stockID;
    TextView suma, article_name;
    TableRow no_art;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_name_list);
       // LoadingDialog = ProgressDialog.show(ArticleNameList.this, "",
            //    "Ładowanie. Proszę czekać...", true);
        getElementsFromIntent();
        getElementsById();
        doRestDescr();
        doRestSwd();
       // LoadingDialog.dismiss();
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
        setIntent(""); //cofnij do StockInformation
    }

    public void getElementsFromIntent() {
        password = (getIntent().getStringExtra("password"));
        setPassword(password);
        destination = getIntent().getStringExtra("destination");
        database = (getIntent().getStringExtra("database"));
        name = (getIntent().getStringExtra("content"));
        stockID = (getIntent().getStringExtra("stockID"));
    }

    public void getElementsById(){
         no_art = (TableRow) findViewById(R.id.no_articles);
         article_name = (TextView) findViewById(R.id.article_name);
         suma = (TextView) findViewById(R.id.suma);
    }

    public void setIntent(String finalArticleIDNO) {
        try {
            Intent intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("art_idno", finalArticleIDNO);
            intent.putExtra("stockID", stockID);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void doRestDescr(){
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
        }catch (DBRuntimeException e) {
            GlobalClass.showDialog(this, "Brak połączenia!", "Nie można aktualnie połączyć z bazą.", "OK", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {}});
        }
        ctx.close();
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void doRestSwd() {
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        SelectionBuilder<Product> productSB = SelectionBuilder.create(Product.class);
        Query<Product> productQuery = ctx.createQuery(productSB.build());

        try {
            productSB.add(Conditions.matchIgCase(Product.META.swd.toString(), name));
            for (Product product : productQuery) {
                drawTable(product);
            }
            ctx.close();
        }catch (DBRuntimeException e) {
            Log.d("error", e.getMessage());
            GlobalClass.showDialog(this, "Brak połączenia!", "Nie można aktualnie połączyć z bazą.", "OK", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {} });
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
        id.setPadding(5, 10, 5, 10);
        id.setLayoutParams(params);

        articleView.setText(article);
        articleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        articleView.setTextColor(Color.parseColor("#808080"));
        articleView.setTypeface(Typeface.DEFAULT_BOLD);
        articleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        articleView.setPadding(5, 10, 5, 10);
        articleView.setLayoutParams(params);

        article_nameView.setText(article_name);
        article_nameView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        article_nameView.setTextColor(Color.parseColor("#808080"));
        article_nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        article_nameView.setPadding(5, 10, 5, 10);
        article_nameView.setLayoutParams(params);

        tableRowList.addView(id);
        tableRowList.addView(articleView);
        tableRowList.addView(article_nameView);
        layoutList.addView(tableRowList, j);

        String finalArticle = article;
        String finalArticle_name = article_name;
        String finalArticleIDNO = articleIDNO;
        tableRowList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ctx.close();
                if(destination.equals("Stocktaking")){
                    positiveButtonText = "Wybierz";
                }else{
                    positiveButtonText = "Sprawdź stan";
                }

                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(ArticleNameList.this, R.style.Theme_AppCompat_Light_Dialog_Alert);
                String articleString = "<b>" + finalArticle + "</b><br/>" + finalArticle_name;
                dlgAlert.setMessage(Html.fromHtml(articleString));
                dlgAlert.setTitle("Wybrany artykuł: ");
                dlgAlert.setPositiveButton(positiveButtonText,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //dismiss the dialog
                                setIntent(finalArticleIDNO);
                            }
                        });
                dlgAlert.setNegativeButton("Anuluj",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //dismiss the dialog
                            }
                        });
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
            }
        });

    }
}