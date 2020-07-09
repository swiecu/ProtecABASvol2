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

public class StockInformation extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx;
    ProgressDialog LoadingDialog;
    TableLayout layout;
    TextView article_name;
    TextView suma;
    TableRow no_art;
    SearchArticleClass globFunctions;  //zdeklarowanie globalnej klasy
//02040042/1
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_information);
        layout = (TableLayout) findViewById(R.id.articleNameTable); //tabelka
        article_name = (TextView) findViewById(R.id.article_name); //nazwa artkułu nad tabelką
        suma = (TextView) findViewById(R.id.suma); //suma ilości pod tabelką
        no_art = (TableRow) findViewById(R.id.no_articles); //table row "Brak artykułów"
        article_name.setVisibility(View.GONE);
        suma.setVisibility(View.GONE);
        no_art.setVisibility(View.VISIBLE);
        String passwd = (getIntent().getStringExtra("password"));
        setPassword(passwd);
        String back_article = (getIntent().getStringExtra("art_idno"));
        //jeśli wraca z ArticleNameList
        if(back_article != null) {
            String password = (getIntent().getStringExtra("password"));
            setPassword(password);
            LoadingDialog = ProgressDialog.show(StockInformation.this, "",
                    "Ładowanie. Proszę czekać...", true);
            searchArticle(back_article);
        }
    }
    // na wyjście z actvity
    @Override
    protected void onStop(){
        super.onStop();
        if (LoadingDialog != null){
            LoadingDialog.dismiss();
        }
    }
    //na cofnięcie Back do tyłu
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        Intent intent = new Intent(StockInformation.this, Menu.class);
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
    // CHECK STOCK
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
                LoadingDialog = ProgressDialog.show(StockInformation.this, "",
                        "Ładowanie. Proszę czekać...", true);
                searchArticle(content);
            }
        }
    }
    //CHECK FREEHAND STOCK
    public void checkFreehandStock(View view){
        AlertDialog.Builder enterArticleDialog = new AlertDialog.Builder(StockInformation.this);
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
                    AlertDialog.Builder emptyArticleDialog = new AlertDialog.Builder(StockInformation.this);
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
                    LoadingDialog = ProgressDialog.show(StockInformation.this, "",
                            "Ładowanie. Proszę czekać...", true);
                    searchArticle(article_name);
                }
            }
        });
        articleDialog.show();
        articleDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    // SEARCH ARTICLE
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void searchArticle(String content) {
        globFunctions = new SearchArticleClass(getApplicationContext());
        //jeśli row "Brak artykułow" istnieje to schowaj
        if (no_art != null) {
            no_art.setVisibility(View.GONE);
        }
        layout.removeViews(1, (layout.getChildCount() -2)); //zmień liczbę View dla layoutu ("Brak artykułu" jest schowane ale dalej jest child od layout)
        try{
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");   //?? potrzebne policy?

            if(globFunctions.FindProductByIdno(ctx, content) != null) {  //jeśli Find by IDNO nie równa się null
                drawTable(ctx, content);
                LoadingDialog.dismiss();
                //jeśli nie znajdzie by IDNO
            }else if (globFunctions.FindProductByDescr(ctx, content) != null){

                Intent intent = new Intent(this, ArticleNameList.class);
                intent.putExtra("password", getPassword());
                intent.putExtra("content", content);
                intent.putExtra("destination", "StockInformation");
                startActivity(intent);

                // jeśli nie znajdzie by DESCR
            } else if (globFunctions.FindProductBySwd(ctx, content) != null) {   //jeśli Find by SWD nie równa się null

                Intent intent = new Intent(this, ArticleNameList.class);
                intent.putExtra("password", getPassword());
                intent.putExtra("content", content);
                intent.putExtra("destination", "StockInformation");
                startActivity(intent);

                // jeśli nie znajdzie ani tu ani tu
            } else {
                LoadingDialog.dismiss();
                no_art.setVisibility(View.VISIBLE);
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
        BigDecimal sum = BigDecimal.ZERO;
        StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
        sli.setArtikel(FindProductByIdno(ctx, content));
        sli.setKlgruppe((WarehouseGroup) null);
        sli.setNullmge(false);
        sli.invokeStart();
        Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
        Integer test = sli.getRowCount();
        if (test != 0) {
            for (StockLevelInformation.Row row : sliRows) {
                TableRow tableRow = new TableRow(this);
                //ustawianie wyglądu dla row
                TableRow.LayoutParams rowParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                tableRow.setLayoutParams(rowParam);
                tableRow.setBackgroundColor(Color.parseColor("#BDBBBB"));
                //ustawianie wyglądu dla table cell
                TableRow.LayoutParams cellParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                cellParam.setMargins(1, 1, 1, 1);

                TextView place = new TextView(this);
                TextView article = new TextView(this);
                TextView qty = new TextView(this);
                TextView unit = new TextView(this);
                TextView id = new TextView(this);
                String Unit;

                Integer j = layout.getChildCount();
                j = j - 1; //  table header nie ma być brany pod uwagę więc -1

                if (j % 2 == 0) {  // zmiana koloru w rowach dla parzystych
                    id.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    place.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    qty.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    unit.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    article.setBackgroundColor(Color.parseColor("#E5E5E6"));
                } else {
                    id.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    place.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    qty.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    unit.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    article.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }
                // Id
                id.setText((j).toString());
                id.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                id.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                id.setPadding(5, 10, 5, 10);
                id.setLayoutParams(cellParam);
                // Lokalizacja
                place.setText(row.getLplatz().getSwd());
                place.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                place.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                place.setPadding(5, 10, 5, 10);
                place.setLayoutParams(cellParam);
                //Artykuł
                if (j == 1) {
                    article.setText(FindProductByIdno(ctx, content).getSwd());
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
                tableRow.addView(id);
                tableRow.addView(article);
                tableRow.addView(qty);
                tableRow.addView(unit);
                tableRow.addView(place);
                layout.addView(tableRow, j);

                sum = sum.add(new BigDecimal(Qty));
                String articleName = ((Product) row.getTartikel()).getDescr6();
                String sumString = sumString = "Suma: <b> " + sum + "<b> " + Unit;
                article_name.setText(Html.fromHtml("Nazwa: <b>" + articleName));
                article_name.setVisibility(View.VISIBLE);

                suma.setText(Html.fromHtml(sumString));
                suma.setVisibility(View.VISIBLE);
            }
        }else{
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage("Artykuł ten nie jest obecnie w zapasie.");
                dlgAlert.setTitle("Brak stanu!");
                dlgAlert.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //dismiss the dialog
                            }
                        });
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
                no_art.setVisibility(View.VISIBLE);
        }

    }
}