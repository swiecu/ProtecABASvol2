package protec.pl.protecabasvol2;

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

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.math.BigDecimal;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.infosystem.standard.la.StockLevelInformation;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.warehouse.WarehouseGroup;
import de.abas.erp.db.util.ContextHelper;

import static protec.pl.protecabasvol2.GlobalClass.FindProductByIdno;

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
    GlobalClass globFunctions;  //zdeklarowanie globalnej klasy
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
        password = (getIntent().getStringExtra("password"));
        setPassword(password);
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void checkStock(View view){
        try{
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować artykuł");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            Intent intent = integrator.createScanIntent();
            startActivityForResult(intent , 101);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
            startActivity(marketIntent);
        }
    }
    // ON ACTIVITY RESULT
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data);
        if(result!= null){
            if (requestCode == 101) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    LoadingDialog = ProgressDialog.show(StockInformation.this, "",
                            "Ładowanie. Proszę czekać...", true);
                    searchArticle(content);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
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
                    GlobalClass.showDialog(StockInformation.this, "Brak wpisanego artykułu!", "Proszę wprowadzić artykuł.", "OK", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
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
        globFunctions = new GlobalClass(getApplicationContext());
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
                GlobalClass.showDialog(this, "Brak artykułu!", "W bazie nie ma takiego artykłu!", "OK",
                    new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {} });
            }
        } catch (Exception e) {
            GlobalClass.showDialog(this, "Brak połączenia!", "Nie można aktualnie połączyć z bazą.", "OK",
                new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {} });
            LoadingDialog.dismiss();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(DbContext ctx, String content){
        BigDecimal sum = BigDecimal.ZERO;
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
        StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
        sli.setArtikel(FindProductByIdno(ctx, content));
        sli.setKlgruppe((WarehouseGroup) null);
        sli.setNullmge(false);
        sli.invokeStart();
        Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
        Integer nrRows = sli.getRowCount();
        if (nrRows != 0) {
            for (StockLevelInformation.Row row : sliRows) {
                TableRow tableRow = new TableRow(this);
                //ustawianie wyglądu dla row
                TableRow.LayoutParams rowParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                tableRow.setLayoutParams(rowParam);
                tableRow.setBackgroundColor(Color.parseColor("#BDBBBB"));
                //ustawianie wyglądu dla table cell
                TableRow.LayoutParams cellParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                cellParam.setMargins(1, 1, 1, 1);

                TextView place_textViewTable = new TextView(this);
                TextView article_textViewTable = new TextView(this);
                TextView qty_textViewTable = new TextView(this);
                TextView unit_textViewTable = new TextView(this);
                TextView id_textViewTable = new TextView(this);
                String Unit;

                Integer j = layout.getChildCount();
                j = j - 1; //  table header nie ma być brany pod uwagę więc -1

                if (j % 2 == 0) {  // zmiana koloru w rowach dla parzystych
                    id_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    place_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    qty_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    unit_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    article_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                } else {
                    id_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    place_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    qty_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    unit_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    article_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }
                // Id
                id_textViewTable.setText((j).toString());
                id_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                id_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                id_textViewTable.setPadding(5, 10, 5, 10);
                id_textViewTable.setLayoutParams(cellParam);
                // Lokalizacja
                place_textViewTable.setText(row.getLplatz().getSwd());
                place_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                place_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                place_textViewTable.setPadding(5, 10, 5, 10);
                place_textViewTable.setLayoutParams(cellParam);
                //Artykuł
                if (j == 1) {
                    article_textViewTable.setText(FindProductByIdno(ctx, content).getSwd());
                }  // ustawia Klucz artykułu tylko dla pierwszego wiersza
                article_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                article_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                article_textViewTable.setTypeface(Typeface.DEFAULT_BOLD);
                article_textViewTable.setPadding(5, 10, 5, 10);
                article_textViewTable.setLayoutParams(cellParam);
                //  Ilość
                String Qty = row.getLemge().stripTrailingZeros().toPlainString();
                qty_textViewTable.setText(Qty);
                qty_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                qty_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                qty_textViewTable.setPadding(5, 10, 5, 10);
                qty_textViewTable.setLayoutParams(cellParam);
                //  Jednostka
                unit_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                unit_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                unit_textViewTable.setPadding(5, 10, 5, 10);
                unit_textViewTable.setLayoutParams(cellParam);
                // Ilość i jednostka .setText na podstawie szt. kg kpl

                Unit = row.getLeinheit().toString();
                if (Unit.equals("(20)")) { // jeśli jednostka to szt.
                    unit_textViewTable.setText("szt.");
                    Unit = "szt.";
                } else if (Unit.equals("(7)")) { //jeśli jednostka to kg
                    unit_textViewTable.setText("kg");
                    Unit = "kg";
                } else if (Unit.equals("(21)")) { // jeśli jednostka to kpl
                    unit_textViewTable.setText("kpl");
                    Unit = "kpl";
                }

                tableRow.addView(id_textViewTable);
                tableRow.addView(article_textViewTable);
                tableRow.addView(qty_textViewTable);
                tableRow.addView(unit_textViewTable);
                tableRow.addView(place_textViewTable);
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
            GlobalClass.showDialog(this, "Brak stanu!", "Artykuł ten nie jest obecnie w zapasie.", "OK", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {} });
                no_art.setVisibility(View.VISIBLE);
        }
    }
}