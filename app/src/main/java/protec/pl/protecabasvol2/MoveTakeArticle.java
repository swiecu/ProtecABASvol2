package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.math.BigDecimal;

import de.abas.erp.common.type.AbasDate;
import de.abas.erp.common.type.enums.EnumEntryTypeStockAdjustment;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorCommand;
import de.abas.erp.db.EditorCommandFactory;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.standard.la.StockLevelInformation;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.storagequantity.StockAdjustmentEditor;
import de.abas.erp.db.schema.warehouse.WarehouseGroup;
import de.abas.erp.db.util.ContextHelper;

import static protec.pl.protecabasvol2.GlobalClass.FindProductByIdno;

public class MoveTakeArticle extends Activity {
    private String password;
    public String getPassword() { return password;}
    public void setPassword(String password) {this.password = password; }
    DbContext ctx, sessionCtx;
    ProgressDialog LoadingDialog;
    TableLayout stockLayout;
    TableRow no_art;
    GlobalClass myGlob;  //zdeklarowanie globalnej klasy
    TextView artInfo, lokInfo, qtyInfo, unit_textEdit;
    EditText article_textEdit, location_textEdit, qty_textEdit;
    String artIDNO, platz, database, back_article, userSwd;
    View take_btn;
    Handler handler;
    Intent intent;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_take_article);
        getElementsFromIntent();
        getElementsById();
        setLook();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MoveTakeArticle.this, userSwd));

        if(back_article != null) {
            String back_password = (getIntent().getStringExtra("password"));
            setPassword(back_password);
            searchArticle(back_article);
        }
    }

    //na kliknięcie cofnij
    public void onBackPressed(){
        super.onBackPressed();
        if(ctx != null) {
            ctx.close();
        }
        new setIntentAsyncTask().execute("Move", "");
    }

    // na wyjście z actvity
    @Override
    protected void onStop(){
        super.onStop();
        if (LoadingDialog != null){
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

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(MoveTakeArticle.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(MoveTakeArticle.this, "",
                    "Ładowanie. Proszę czekać...", true);
        }

        @Override
        protected String doInBackground(String... strings) {
            String destination = strings[0];
            String content = strings[1];
            setIntent(destination, content);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
//            if(loadDialog!= null){
//                loadDialog.dismiss();
//            }
        }
    }

    public void setIntent(String destination, String content){
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("content", content);
            intent.putExtra("destination", "MoveTakeArticle");
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void getElementsById(){
        artInfo = findViewById(R.id.artInfo_textView);
        lokInfo = findViewById(R.id.lokInfo_textView);
        qtyInfo = findViewById(R.id.qtyInfo_textView);
        article_textEdit = findViewById(R.id.article_textEdit);
        location_textEdit = findViewById(R.id.from_textEdit);
        qty_textEdit = findViewById(R.id.qty_textEdit);
        unit_textEdit = findViewById(R.id.unit_textView);
        take_btn = findViewById(R.id.take_btn);
    }

    public void getElementsFromIntent(){
        String password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        back_article = (getIntent().getStringExtra("art_idno"));
        userSwd = getIntent().getStringExtra("userSwd");

        setPassword(password);
    }

    public void setLook(){
        artInfo.setVisibility(View.INVISIBLE);
        lokInfo.setVisibility(View.INVISIBLE);
        qtyInfo.setVisibility(View.INVISIBLE);
        location_textEdit.setInputType(0);
        article_textEdit.setInputType(0);
        take_btn.setEnabled(true);
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // CHECK STOCK
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void scanArticle(View view){
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
                qty_textEdit.setText("");
                take_btn.setEnabled(true);
                searchArticle(content);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //   ENTER FREEHAND ARTICLE
    public void enterFreehandArticle(View view){
        AlertDialog.Builder enterArticleDialog = new AlertDialog.Builder(MoveTakeArticle.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_enter_article, viewGroup, false);
        enterArticleDialog.setView(dialogView);
        AlertDialog articleDialog = enterArticleDialog.create();
        Button button_cancel = (Button)dialogView.findViewById(R.id.button_indivStocktaking);
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
                    GlobalClass.showDialog(MoveTakeArticle.this, "Brak wpisanego artykułu!", "Proszę wprowadzić artykuł.", "OK",
                        new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
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

    //      SEARCH ARTICLE
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void searchArticle(String content) {
        take_btn.setEnabled(true);
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
                if(ctx != null) {
                    ctx.close();
                }
                if(LoadingDialog != null) {
                    LoadingDialog.dismiss();
                }
                new setIntentAsyncTask().execute("ArticleNameList", content);

                // jeśli nie znajdzie by DESCR
            } else if (myGlob.FindProductBySwd(ctx, content) != null) {   //jeśli Find by SWD nie równa się null
                if(ctx != null) {
                    ctx.close();
                }
                if(LoadingDialog != null) {
                    LoadingDialog.dismiss();
                }
                new setIntentAsyncTask().execute("ArticleNameList", content);

                // jeśli nie znajdzie ani tu ani tu
            } else {
                if(ctx != null) {
                    ctx.close();
                }
                if(LoadingDialog != null) {
                    LoadingDialog.dismiss();
                }
                GlobalClass.showDialog(MoveTakeArticle.this, "Brak artykułu!", "W bazie nie ma takeigo artykłu!", "OK",
                     new DialogInterface.OnClickListener() {
                     @Override public void onClick(DialogInterface dialog, int which) {} });
            }

        } catch (DBRuntimeException e) {
            if(LoadingDialog != null) {
                LoadingDialog.dismiss();
            }
            catchExceptionCases(e, "searchArticle", content);
        }
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function, String parameter){
        if(e.getMessage().contains("failed")){
            GlobalClass.showDialog(this,"Brak połączenia!","Nie można się aktualnie połączyć z bazą.", "OK",new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { } });

            //przekroczona liczba licencji
        }else if(e.getMessage().contains("FULL")){
            LoadingDialog = ProgressDialog.show(MoveTakeArticle.this, "     Przekroczono liczbę licencji.",
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
                    if (LoadingDialog != null){
                        LoadingDialog.dismiss();
                    }
                    if(function.equals("searchArticle")){
                        searchArticle(parameter);
                    }else if(function.equals("save")){
                        take_btn.callOnClick();
                    }
                }
            };
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
        StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
        sli.setArtikel(FindProductByIdno(ctx, content));
        sli.setKlgruppe((WarehouseGroup) null);
        sli.setNullmge(false);
        sli.invokeStart();

        Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
        Integer nr_Rows = sli.getRowCount();
        Integer wdrRowCount = 0;
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
                    place_textViewTable.setPadding(5, 20, 5, 20);
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
                    article_textViewTable.setPadding(5, 20, 5, 20);
                    article_textViewTable.setLayoutParams(cellParam);
                    //  Jednostka
                    unit_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    unit_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                    unit_textViewTable.setPadding(5, 20, 5, 20);
                    unit_textViewTable.setLayoutParams(cellParam);
                    //  Ilość
                    BigDecimal qtyDecimal = row.getLemge().stripTrailingZeros();  //by po przecinku usunąć niepotrzebne zera
                    String Qty = qtyDecimal.toPlainString();    //by wyświetlało liczbę w formacie 20 a nie 2E+1
                    qty_textViewTable.setText(Qty);
                    qty_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    qty_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                    qty_textViewTable.setPadding(5, 20, 5, 20);
                    qty_textViewTable.setLayoutParams(cellParam);

                    // Ilość i jednostka .setText na podstawie szt. kg kpl
                    unit = row.getString("leinheit");
                    if (unit.equals("(20)")) {
                        unit = "szt.";
                    } else if (unit.equals("(7)")) {
                        unit = "kg";
                    } else if (unit.equals("(21)")) {
                        unit = "kpl";
                    } else if (unit.equals("(1)")) {
                        unit = "m";
                    }else if (unit.equals("(10)")) {
                        unit = "tona";
                    }else if (unit.equals("(28)")) {
                        unit = "arkusz";
                    }
                    unit_textViewTable.setText(unit);
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
                            platz = row.getLplatz().getSwd();
                            location_textEdit.setText(platz);
                            article_textEdit.setText(art);
                            unit_textEdit.setText(finalUnit);
                            qty_textEdit.setHint(finalQty);
                            unit_textEdit.setVisibility(View.VISIBLE);
                            artInfo.setVisibility(View.VISIBLE);
                            lokInfo.setVisibility(View.VISIBLE);
                            qtyInfo.setVisibility(View.VISIBLE);
                            ctx.close();
                        }
                    });
                }
                else{
                    wdrRowCount++;
                }
            }
            Log.d("wdrRowCount", wdrRowCount.toString());
            Log.d("nr_Rows", nr_Rows.toString());

        }
        if ((wdrRowCount == nr_Rows) || (nr_Rows == 0)){
            GlobalClass.showDialog(this, "Brak stanu!", "Artykuł ten nie jest obecnie w zapasie.", "OK",
                new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    location_textEdit.setText("");
                    article_textEdit.setText("");
                    unit_textEdit.setText("");
                    qty_textEdit.setHint("Ilość");
                } });
            stockDialog.dismiss();
            ctx.close();
        }
    }

    @SuppressLint("HandlerLeak")
    public void save(View view) {
        String qty = qty_textEdit.getText().toString();
        String article = article_textEdit.getText().toString();
        String location = location_textEdit.getText().toString();

        Boolean emptyFields = checkIfFieldsEmpty(article, location, qty);
        if(emptyFields == false){
            try {
                take_btn.setEnabled(false);
                qty = qty_textEdit.getText().toString();
                ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
                EditorCommand cmd = EditorCommandFactory.typedCmd("Lbuchung", "");
                StockAdjustmentEditor stockAdjustmentEditor = (StockAdjustmentEditor) ctx.openEditor(cmd);
                AbasDate today = new AbasDate();
                stockAdjustmentEditor.setString("product", artIDNO);
                stockAdjustmentEditor.setDocNo("MOBILE");
                stockAdjustmentEditor.setDateDoc(today);
                stockAdjustmentEditor.setEntType(EnumEntryTypeStockAdjustment.Transfer);
                StockAdjustmentEditor.Row sadRow = stockAdjustmentEditor.table().getRow(1);
                sadRow.setUnitQty(Double.parseDouble(qty));
                sadRow.setString("location", platz);
                sadRow.setString("location2", "WDR");
                stockAdjustmentEditor.commit();
                article_textEdit.setText("");
                location_textEdit.setText("");
                qty_textEdit.setText("");
                GlobalClass.showDialog(this, "Pobrano!", "Materiał został pobrany i dodany do bazy.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(ctx != null) {
                                ctx.close();
                            }
                            new setIntentAsyncTask().execute("Move", "");
                        }
                    });

            } catch (NumberFormatException e) {
                e.printStackTrace();
                GlobalClass.showDialog(this, "Błąd!", "Podczas zmiany formatu wystąpił błąd.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override  public void onClick(DialogInterface dialog, int which) { }
                    });
            } catch (DBRuntimeException e) {
               catchExceptionCases(e, "save", "");

            } catch (CommandException e) {
                e.printStackTrace();
            }
        }
    }

    public Boolean checkIfFieldsEmpty(String article, String location, String qty){
        Boolean emptyFields= false;
        if (article.equals("")) {
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak artykułu!", "Proszę wprowadzić artykuł", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { }
                    });
        } else if (location.equals("")) {
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak lokalizacji!", "Proszę wprowadzić lokalizację", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { }
                    });
        } else {
            if (qty.equals("")) {
                qty = qty_textEdit.getHint().toString();
                qty_textEdit.setText(qty);
            }
            //jeśli ilość wpisana jest WIĘKSZA niż na stanie
            if (Double.parseDouble(qty) > Double.parseDouble(qty_textEdit.getHint().toString())) {
                emptyFields = true;
                GlobalClass.showDialog(this, "Wykroczenie poza stan!", "Wpisana ilość przekracza ilość dostępną na stanie.", "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
            }
        }
        return emptyFields;
    }
}