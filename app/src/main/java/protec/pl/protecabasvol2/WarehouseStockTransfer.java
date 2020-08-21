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
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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
import de.abas.erp.db.schema.location.LocationHeader;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.warehouse.WarehouseGroup;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

import static protec.pl.protecabasvol2.GlobalClass.FindProductByIdno;

public class WarehouseStockTransfer extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx ;
    ProgressDialog LoadingDialog;
    String database, back_article, artIDNO, platz;
    CheckBox lockIcon;
    TextView unit_textView, article_textInfo, location_textInfo, qty_textInfo;
    EditText article_textEdit, fromLocation_textEdit, toLocation_textEdit, qty_textEdit;
    GlobalClass myGlob;
    TableLayout stockLayout;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_stock_transfer);

        getElementsFromIntent();
        getElementsById();
        setLook();

        //jeśli wraca z ArticleNameList
        if(back_article != null) {
            String password = (getIntent().getStringExtra("password"));
            setPassword(password);
            LoadingDialog = ProgressDialog.show(WarehouseStockTransfer.this, "",
                    "Ładowanie. Proszę czekać...", true);
            searchArticle(back_article);
        }

    }

    public void onBackPressed() {
        super.onBackPressed();
        setIntent("Menu",  "");
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        super.onStop();
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
    }

    public void getElementsById() {
        lockIcon = (CheckBox) findViewById(R.id.lockIcon);
        unit_textView = (TextView) findViewById(R.id.unit_textView);
        article_textEdit = (EditText) findViewById(R.id.article_textEdit);
        fromLocation_textEdit = (EditText) findViewById(R.id.fromLocation_textEdit);
        toLocation_textEdit = (EditText) findViewById(R.id.toLocation_textEdit);
        qty_textEdit = (EditText) findViewById(R.id.qty_TOtextEdit);
        article_textInfo = (TextView) findViewById(R.id.article_textInfo);
        location_textInfo = (TextView) findViewById(R.id.location_textInfo);
        qty_textInfo = (TextView) findViewById(R.id.qty_textInfo);
    }

    public void setLook() {
        unit_textView.setVisibility(View.INVISIBLE);
        article_textInfo.setVisibility(View.INVISIBLE);
        location_textInfo.setVisibility(View.INVISIBLE);
        qty_textInfo.setVisibility(View.INVISIBLE);
        fromLocation_textEdit.setInputType(0);
        toLocation_textEdit.setInputType(0);
        article_textEdit.setInputType(0);
    }

    public void getElementsFromIntent() {
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        database = (getIntent().getStringExtra("database"));
        back_article = (getIntent().getStringExtra("art_idno"));
    }

    public void setIntent(String destination, String article) {
        try {
            Intent intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("content", article);
            intent.putExtra("destination", "WarehouseStockTransfer");
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // CHECK STOCK
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void scanArticle(View view) {
        try {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować artykuł");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            Intent intent = integrator.createScanIntent();
            startActivityForResult(intent, 101);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
        }
    }

    public void scanLocation(View view) {
        try {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować lokalizację");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            Intent intent = integrator.createScanIntent();
            startActivityForResult(intent, 70);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
        }
    }

    // ON ACTIVITY RESULT
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data);
        if (result != null) {
            if (requestCode == 101) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    searchArticle(content);
                }
            }
            if (requestCode == 70) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    getLocation(content);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void enterFreehandArticle(View view){
        AlertDialog.Builder enterArticleDialog = new AlertDialog.Builder(WarehouseStockTransfer.this);
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
                    GlobalClass.showDialog(WarehouseStockTransfer.this, "Brak wpisanego artykułu!", "Proszę wprowadzić artykuł.", "OK",
                            new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {} });
                }else {
                    articleDialog.dismiss();
                    LoadingDialog = ProgressDialog.show(WarehouseStockTransfer.this, "",
                            "Ładowanie. Proszę czekać...", true);
                    searchArticle(article_name);
                }
            }
        });
        articleDialog.show();
        articleDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    //      SEARCH ARTICLE
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void searchArticle(String content) {
        myGlob = new GlobalClass(getApplicationContext());
        try{
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");   //?? potrzebne policy?
            if(myGlob.FindProductByIdno(ctx, content) != null) {  //jeśli Find by IDNO nie równa się null
                drawTable(ctx, content);
                if (LoadingDialog != null){
                    LoadingDialog.dismiss();
                }
                //jeśli nie znajdzie by IDNO
            }else if (myGlob.FindProductByDescr(ctx, content) != null){
                ctx.close();
                setIntent("ArticleNameList", content);

                // jeśli nie znajdzie by DESCR
            } else if (myGlob.FindProductBySwd(ctx, content) != null) {   //jeśli Find by SWD nie równa się null
                ctx.close();
                setIntent("ArticleNameList", content);

                // jeśli nie znajdzie ani tu ani tu
            } else {
                ctx.close();
                LoadingDialog.dismiss();
                GlobalClass.showDialog(WarehouseStockTransfer.this, "Brak artykułu!", "W bazie nie ma takeigo artykłu!", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {} });
            }

        } catch (Exception e) {
            if(LoadingDialog != null) {
                LoadingDialog.dismiss();
            }
            GlobalClass.showDialog(WarehouseStockTransfer.this, "Brak połączenia!", "Nie można aktualnie połączyć z bazą.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(DbContext ctx, String content){

        AlertDialog.Builder stockInformationDialog = new AlertDialog.Builder(WarehouseStockTransfer.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_stock_information, viewGroup, false);
        stockInformationDialog.setView(dialogView);
        AlertDialog stockDialog = stockInformationDialog.create();
        stockDialog.show();
        stockDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stockLayout = (TableLayout) stockDialog.findViewById(R.id.stockNameTable);
        StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
        sli.setArtikel(FindProductByIdno(ctx, content));
        sli.setKlgruppe((WarehouseGroup) null);
        sli.setNullmge(false);
        sli.invokeStart();

        Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
        Integer nr_Rows = sli.getRowCount();
        if (nr_Rows != 0) {
            for (StockLevelInformation.Row row : sliRows) {
                if (!row.getLplatz().getSwd().equals("WDR")) {  // nie wyświetla lokalizacji WDR
                    TableRow tableRowStock = new TableRow(this);
                    //ustawianie wyglądu dla row
                    TableRow.LayoutParams rowParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                    tableRowStock.setLayoutParams(rowParam);
                    tableRowStock.setBackgroundColor(Color.parseColor("#BDBBBB"));

                    //ustawianie wyglądu dla table cell
                    TableRow.LayoutParams cellParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                    cellParam.setMargins(1, 1, 1, 1);

                    TextView place_textViewTable = new TextView(this);
                    TextView article_textViewTable = new TextView(this);
                    TextView qty_textViewTable = new TextView(this);
                    TextView unit_textViewTable = new TextView(this);
                    String unit;

                    Integer j = stockLayout.getChildCount();
                    j = j - 1; //  table header nie ma być brany pod uwagę więc -1

                    if (j % 2 == 0) {  // zmiana koloru w rowach dla parzystych
                        place_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                        qty_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                        unit_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                        article_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    } else {
                        place_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        qty_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        unit_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        article_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    }

                    // Lokalizacja
                    place_textViewTable.setText(row.getLplatz().getSwd());
                    place_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    place_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                    place_textViewTable.setPadding(5, 10, 5, 10);
                    place_textViewTable.setLayoutParams(cellParam);
                    //Artykuł
                    String art = FindProductByIdno(ctx, content).getSwd();
                    String art_descr = FindProductByIdno(ctx, content).getDescr6();
                    TextView articleName_text = (TextView) stockDialog.findViewById(R.id.articleName_textView);
                    articleName_text.setText(Html.fromHtml("<b> " + art_descr + "<b> "));
                    artIDNO = FindProductByIdno(ctx, content).getIdno();
                    if (j == 1) {
                        article_textViewTable.setText(art);
                    }  // ustawia Klucz artykułu tylko dla pierwszego wiersza
                    article_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    article_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                    article_textViewTable.setTypeface(Typeface.DEFAULT_BOLD);
                    article_textViewTable.setPadding(5, 10, 5, 10);
                    article_textViewTable.setLayoutParams(cellParam);
                    //  Jednostka
                    unit_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    unit_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                    unit_textViewTable.setPadding(5, 10, 5, 10);
                    unit_textViewTable.setLayoutParams(cellParam);
                    //  Ilość
                    BigDecimal qtyDecimal = row.getLemge().stripTrailingZeros();  //by po przecinku usunąć niepotrzebne zera
                    String Qty = qtyDecimal.toPlainString();    //by wyświetlało liczbę w formacie 20 a nie 2E+1
                    qty_textViewTable.setText(Qty);
                    qty_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    qty_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                    qty_textViewTable.setPadding(5, 10, 5, 10);
                    qty_textViewTable.setLayoutParams(cellParam);

                    // Ilość i jednostka .setText na podstawie szt. kg kpl
                    unit = row.getLeinheit().toString();
                    if (unit.equals("(20)")) { // jeśli jednostka to szt.
                        unit_textViewTable.setText("szt.");
                        unit = "szt.";
                    } else if (unit.equals("(7)")) { //jeśli jednostka to kg
                        unit_textViewTable.setText("kg");
                        unit = "kg";
                    } else if (unit.equals("(21)")) { // jeśli jednostka to kpl
                        unit_textViewTable.setText("kpl");
                        unit = "kpl";
                    } else if (unit.equals("(1)")) { // jeśli jednostka to kpl
                        unit_textViewTable.setText("m");
                        unit = "m";
                    }
                    tableRowStock.addView(article_textViewTable);
                    tableRowStock.addView(qty_textViewTable);
                    tableRowStock.addView(unit_textViewTable);
                    tableRowStock.addView(place_textViewTable);
                    stockLayout.addView(tableRowStock, j);

                    String articleName = ((Product) row.getTartikel()).getDescr6();
                    String finalUnit = unit;
                    String finalQty = Qty;
                    tableRowStock.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            stockDialog.dismiss();
                            AlertDialog.Builder choosedLocAlert = new AlertDialog.Builder(WarehouseStockTransfer.this);
                            platz = row.getLplatz().getSwd();
                            String mge = row.getLemge().toString();
                            String articleString = "<b>" + platz + "</b><br/>" + finalQty + " " + finalUnit;
                            choosedLocAlert.setMessage(Html.fromHtml(articleString));
                            choosedLocAlert.setTitle("Wybrana lokalizacja: ");
                            choosedLocAlert.setPositiveButton("Wybierz lokalizację",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            //dismiss the dialog
                                            fromLocation_textEdit.setText(platz);
                                            article_textEdit.setText(art);
                                            unit_textView.setText(finalUnit);
                                            qty_textEdit.setHint(finalQty);
                                            unit_textView.setVisibility(View.VISIBLE);
                                            article_textInfo.setVisibility(View.VISIBLE);
                                            location_textInfo.setVisibility(View.VISIBLE);
                                            qty_textInfo.setVisibility(View.VISIBLE);
                                        }
                                    });
                            choosedLocAlert.setNegativeButton("Anuluj",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            //dismiss the dialog
                                        }
                                    });
                            choosedLocAlert.setCancelable(true);
                            choosedLocAlert.create().show();
                            ctx.close();
                        }
                    });
                }
            }
        }else{
            GlobalClass.showDialog(this, "Brak stanu!", "Artykuł ten nie jest obecnie w zapasie.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
            stockDialog.dismiss();
            ctx.close();
        }

    }

    public void getLocation(String content) {
        LocationHeader location = LocationExists(content);
        if (location != null) {
            String location_name = location.getSwd();
            toLocation_textEdit.setText(location_name);
            article_textInfo.setVisibility(View.VISIBLE);
            qty_textInfo.setVisibility(View.VISIBLE);
            location_textInfo.setVisibility(View.VISIBLE);
            ctx.close();
        }else {
            toLocation_textEdit.setText("");
            GlobalClass.showDialog(this, "Brak lokalizacji!", "Zeskanowana lokalizacja nie istnieje.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
        }
    }

    public LocationHeader LocationExists(String location) {
        LocationHeader loc = null;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<LocationHeader> locationSB = SelectionBuilder.create(LocationHeader.class);
            locationSB.add(Conditions.eq(LocationHeader.META.swd, location));
            loc = QueryUtil.getFirst(ctx, locationSB.build());
            ctx.close();
        } catch (Exception e) {
            Log.d("error", e.getMessage());
        }
        return loc;
    }
}