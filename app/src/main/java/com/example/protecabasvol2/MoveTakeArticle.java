package com.example.protecabasvol2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.math.BigDecimal;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.infosystem.standard.la.StockLevelInformation;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.warehouse.WarehouseGroup;
import de.abas.erp.db.util.ContextHelper;

import static com.example.protecabasvol2.SearchArticleClass.FindProductByIdno;

public class MoveTakeArticle extends AppCompatActivity {
    private String password;
    public String getPassword() { return password;}
    public void setPassword(String password) {this.password = password; }
    DbContext ctx;
    ProgressDialog LoadingDialog;
    TableLayout stockLayout;
    TableRow no_art;
    SearchArticleClass myGlob;  //zdeklarowanie globalnej klasy
    TextView artInfo;
    TextView lokInfo;
    TextView qtyInfo;
    EditText article_text;
    EditText location_text;
    EditText qty_text;
    TextView unit_text;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_take_article);
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        artInfo = findViewById(R.id.artInfo_textView);
        lokInfo = findViewById(R.id.lokInfo_textView);
        qtyInfo = findViewById(R.id.qtyInfo_textView);
        article_text = findViewById(R.id.article_textEdit);
        location_text = findViewById(R.id.from_textEdit);
        qty_text = findViewById(R.id.qty_textEdit);
        unit_text = findViewById(R.id.unit_textView);
        artInfo.setVisibility(View.INVISIBLE);
        lokInfo.setVisibility(View.INVISIBLE);
        qtyInfo.setVisibility(View.INVISIBLE);
        location_text.setFocusable(false);
        article_text.setFocusable(false);
        String back_article = (getIntent().getStringExtra("art_idno"));
        if(back_article != null) {
            String back_password = (getIntent().getStringExtra("password"));
            setPassword(back_password);
            searchArticle(back_article);
        }
    }
    public void onBackPressed(){
        super.onBackPressed();
        Intent intent = new Intent(MoveTakeArticle.this, Move.class);
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
    // na wyjście z actvity
    @Override
    protected void onStop(){
        super.onStop();
        if (LoadingDialog != null){
            LoadingDialog.dismiss();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void checkStock(View view){
        try{
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
            startActivityForResult(intent, 0);
            onActivityResult(0, 0, intent);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
            startActivity(marketIntent);
        }
    }
    // ON ACTIVITY RESULT
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String content = data.getStringExtra("SCAN_RESULT");
                //LoadingDialog = ProgressDialog.show(MoveTakeArticle.this, "",
                //        "Ładowanie. Proszę czekać...", true);
                searchArticle(content);
            }
        }
    }

    public void enterFreehandArticle(View view){
        AlertDialog.Builder enterArticleDialog = new AlertDialog.Builder(MoveTakeArticle.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_enter_article, viewGroup, false);
        enterArticleDialog.setView(dialogView);
        AlertDialog articleDialog = enterArticleDialog.create();
        Button button_cancel = (Button)dialogView.findViewById(R.id.button_cancel);
        button_cancel .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                articleDialog.dismiss();}
        });
        Button button_ok = (Button)dialogView.findViewById(R.id.button_ok);
        button_ok.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v){
                EditText article = articleDialog.findViewById(R.id.articleEnter);
                String article_name = article.getText().toString();
                if(article_name.matches("")){ //jeśli jest pusty
                    articleDialog.dismiss();
                    AlertDialog.Builder emptyArticleDialog = new AlertDialog.Builder(MoveTakeArticle.this);
                    emptyArticleDialog.setMessage("Proszę wprowadzić artykuł.");
                    emptyArticleDialog.setTitle("Brak wpisanego artykułu!");
                    emptyArticleDialog.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //dismiss the dialog
                                    articleDialog.show();
                                }
                            });
                    emptyArticleDialog.setCancelable(true);
                    emptyArticleDialog.create().show();
                }else {
                    articleDialog.dismiss();
                    LoadingDialog = ProgressDialog.show(MoveTakeArticle.this, "",
                            "Ładowanie. Proszę czekać...", true);
                    searchArticle(article_name);
                }
            }
        });
        articleDialog.show();
        articleDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void searchArticle(String content) {
        myGlob = new SearchArticleClass(getApplicationContext());
        try{
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");   //?? potrzebne policy?
            if(myGlob.FindProductByIdno(ctx, content) != null) {  //jeśli Find by IDNO nie równa się null
                drawTable(ctx, content);
                //jeśli nie znajdzie by IDNO
            }else if (myGlob.FindProductByDescr(ctx, content) != null){

                Intent intent = new Intent(this, ArticleNameList.class);
                intent.putExtra("password", getPassword());
                intent.putExtra("content", content);
                intent.putExtra("destination", "MoveTakeArticle");
                startActivity(intent);

                // jeśli nie znajdzie by DESCR
            } else if (myGlob.FindProductBySwd(ctx, content) != null) {   //jeśli Find by SWD nie równa się null

                Intent intent = new Intent(this, ArticleNameList.class);
                intent.putExtra("password", getPassword());
                intent.putExtra("content", content);
                intent.putExtra("destination", "MoveTakeArticle");
                startActivity(intent);

                // jeśli nie znajdzie ani tu ani tu
            } else {
                LoadingDialog.dismiss();
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage("W bazie nie ma takeigo artykłu!");
                dlgAlert.setTitle("Brak artykułu!");
                dlgAlert.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //dismiss the dialog
                            }
                        });
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(DbContext ctx, String content){

        AlertDialog.Builder stockInformationDialog = new AlertDialog.Builder(MoveTakeArticle.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_stock_information, viewGroup, false);
        stockInformationDialog.setView(dialogView);
        AlertDialog stockDialog = stockInformationDialog.create();
        stockDialog.show();
        stockDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stockLayout = (TableLayout) stockDialog.findViewById(R.id.stockNameTable);
        no_art =  (TableRow) stockDialog.findViewById(R.id.no_articles);
        if(no_art != null){
            no_art.setVisibility(View.GONE);
        }

        BigDecimal sum = BigDecimal.ZERO;
        StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
        sli.setArtikel(FindProductByIdno(ctx, content));
        sli.setKlgruppe((WarehouseGroup) null);
        sli.setNullmge(false);
        sli.invokeStart();

        Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
        Integer nr_Rows = sli.getRowCount();
        if (nr_Rows != 0) {
            for (StockLevelInformation.Row row : sliRows) {
                TableRow tableRowStock = new TableRow(this);
                //ustawianie wyglądu dla row
                TableRow.LayoutParams rowParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                tableRowStock.setLayoutParams(rowParam);
                tableRowStock.setBackgroundColor(Color.parseColor("#BDBBBB"));

                //ustawianie wyglądu dla table cell
                TableRow.LayoutParams cellParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                cellParam.setMargins(1, 1, 1, 1);

                TextView place = new TextView(this);
                TextView article = new TextView(this);
                TextView qty = new TextView(this);
                TextView unit = new TextView(this);
                String Unit;

                Integer j = stockLayout.getChildCount();
                j = j - 1; //  table header nie ma być brany pod uwagę więc -1

                if (j % 2 == 0) {  // zmiana koloru w rowach dla parzystych
                    place.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    qty.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    unit.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    article.setBackgroundColor(Color.parseColor("#E5E5E6"));
                } else {
                    place.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    qty.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    unit.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    article.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }

                // Lokalizacja

                place.setText(row.getLplatz().getSwd());
                place.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                place.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                place.setPadding(5, 10, 5, 10);
                place.setLayoutParams(cellParam);
                //Artykuł
                String art = FindProductByIdno(ctx, content).getSwd();
                if (j == 1) {
                    article.setText(art);
                }  // ustawia Klucz artykułu tylko dla pierwszego wiersza
                article.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                article.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                article.setTypeface(Typeface.DEFAULT_BOLD);
                article.setPadding(5, 10, 5, 10);
                article.setLayoutParams(cellParam);
                //  Ilość
                qty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                qty.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                qty.setPadding(5, 10, 5, 10);
                qty.setLayoutParams(cellParam);
                //  Jednostka
                unit.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                unit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                unit.setPadding(5, 10, 5, 10);
                unit.setLayoutParams(cellParam);
                // Ilość i jednostka .setText na podstawie szt. kg kpl
                String Qty = row.getLemge().toString();
                Unit = row.getLeinheit().toString();
                if (Unit.equals("(20)")) { // jeśli jednostka to szt.
                    unit.setText("szt.");
                    Unit = "szt.";
                    Qty = Qty.substring(0, Qty.length() - 4);
                    qty.setText(Qty); // usuwa 4 liczby po przecinku
                } else if (Unit.equals("(7)")) { //jeśli jednostka to kg
                    unit.setText("kg");
                    Unit = "kg";
                    qty.setText(Qty.substring(0, Qty.length()));
                } else if (Unit.equals("(21)")) { // jeśli jednostka to kpl
                    unit.setText("kpl");
                    Unit = "kpl";
                    qty.setText(Qty.substring(0, Qty.length()));
                } else {
                    qty.setText(row.getLemge().toString());
                }
                tableRowStock.addView(article);
                tableRowStock.addView(qty);
                tableRowStock.addView(unit);
                tableRowStock.addView(place);
                stockLayout.addView(tableRowStock, j);

                sum = sum.add(new BigDecimal(Qty));
                String articleName = ((Product) row.getTartikel()).getDescr6();
                String sumString = sumString = "Suma: <b> " + sum + "<b> " + Unit;
                //article_name.setText(Html.fromHtml("Nazwa: <b>" + articleName));
               // article_name.setVisibility(View.VISIBLE);

                //suma.setText(Html.fromHtml(sumString));
                //suma.setVisibility(View.VISIBLE);
                String finalUnit = Unit;
                String finalQty = Qty;
                tableRowStock.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        stockDialog.dismiss();
                        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(MoveTakeArticle.this);
                        String platz = row.getLplatz().getSwd();
                        String mge = row.getLemge().toString();
                        String articleString = "<b>" + platz + "</b><br/>" + finalQty + " " +  finalUnit;
                        dlgAlert.setMessage(Html.fromHtml(articleString));
                        dlgAlert.setTitle("Wybrana lokalizacja: ");
                        dlgAlert.setPositiveButton("Wybierz lokalizację",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        //dismiss the dialog
                                        location_text.setFocusable(false);
                                        article_text.setFocusable(false);
                                        location_text.setText(platz);
                                        article_text.setText(art);
                                        unit_text.setText(finalUnit);
                                        qty_text.setHint(finalQty);
                                        unit_text.setVisibility(View.VISIBLE);
                                        artInfo.setVisibility(View.VISIBLE);
                                        lokInfo.setVisibility(View.VISIBLE);
                                        qtyInfo.setVisibility(View.VISIBLE);
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
        }else{
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setMessage("Artykuł ten nie jest obecnie w zapasie.");
            dlgAlert.setTitle("Brak stanu!");
            dlgAlert.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //dismiss the dialog
                            stockDialog.dismiss();
                        }
                    });
            dlgAlert.setCancelable(true);
            dlgAlert.create().show();
        }
    }

    public void takeArticles(View view) {
        String qty = qty_text.getText().toString();
        if(qty.equals("")){
            qty_text.setText(qty_text.getHint());
        }
    }
}