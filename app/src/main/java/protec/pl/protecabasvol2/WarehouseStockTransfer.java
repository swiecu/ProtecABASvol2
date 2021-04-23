package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.math.BigDecimal;
import java.util.List;

import de.abas.erp.common.type.AbasDate;
import de.abas.erp.common.type.enums.EnumEntryTypeStockAdjustment;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorAction;
import de.abas.erp.db.EditorCommand;
import de.abas.erp.db.EditorCommandFactory;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUser;
import de.abas.erp.db.infosystem.standard.la.StockLevelInformation;
import de.abas.erp.db.schema.employee.Employee;
import de.abas.erp.db.schema.employee.EmployeeEditor;
import de.abas.erp.db.schema.location.LocationHeader;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.storagequantity.StockAdjustmentEditor;
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
    DbContext ctx, sessionCtx; ProgressDialog LoadingDialog;
    String database, back_article, artIDNO, user_short_name = "", userSwd ;
    TextView unit_textView, article_textInfo, location_textInfo, qty_textInfo;
    EditText article_textEdit, fromLocation_textEdit, toLocation_textEdit, qty_textEdit;
    GlobalClass myGlob; TableLayout stockLayout; Employee employee;
    View save_btn; Handler handler; Intent intent;  CheckBox lockIcon; ImageView editIcon;

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_stock_transfer);
        getElementsFromIntent();
        getElementsById();
        setLook();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(WarehouseStockTransfer.this, userSwd));

        //jeśli wraca z ArticleNameList
        if(back_article != null) {
            String password = (getIntent().getStringExtra("password"));
            setPassword(password);
            LoadingDialog = ProgressDialog.show(WarehouseStockTransfer.this, "",
                    "Ładowanie. Proszę czekać...", true);
            searchArticle(back_article, ctx);
        }
    }

    public void onBackPressed() {
        new setIntentAsyncTask().execute("Menu", "");
        super.onBackPressed();
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        GlobalClass.dismissLoadingDialog(LoadingDialog);
        GlobalClass.ctxClose(ctx);
        super.onStop();
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        GlobalClass.ctxClose(ctx);
        super.onPause();
    }
    
    @SuppressLint("WrongViewCast")
    public void lockLocation(View view) {
        lockIcon = (CheckBox) view;
        if (lockIcon.isChecked()) {
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_closed_icon);
        } else {
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_icon);
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
        location_textInfo = (TextView) findViewById(R.id.locationFrom_textInfo);
        qty_textInfo = (TextView) findViewById(R.id.qty_textInfo);
        save_btn = findViewById(R.id.save_btn);
        editIcon = findViewById(R.id.editIcon);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void setLook() {
        List selectedUsers = List.of("LSMOLINS", "DCOSSBAU", "SPAMPUCH", "BKOCHAN");
        unit_textView.setVisibility(View.INVISIBLE);
        fromLocation_textEdit.setInputType(0);
        toLocation_textEdit.setInputType(0);
        article_textEdit.setInputType(0);
        save_btn.setEnabled(true);

        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            IsPrLoggedUser lu = ctx.openInfosystem(IsPrLoggedUser.class);
            user_short_name = lu.getYuser();
            employee = FindEmployeeBySwd(ctx, user_short_name);
        }catch (DBRuntimeException e){
            catchExceptionCases(e, "setLook", "", ctx);
        }
        if(employee != null){
            if(selectedUsers.contains(employee.getSwd())){
                editIcon.setVisibility(View.VISIBLE);
            }else{
                editIcon.setEnabled(false);
            }
            if(!employee.getYapplocklocation().equals("")){
                lockIcon.setChecked(true);
                toLocation_textEdit.setHint(employee.getYapplocklocation());
                lockIcon.setBackgroundResource(R.drawable.ic_new_lock_closed_icon);   //OK

            }else{
                lockIcon.setChecked(false);
                toLocation_textEdit.setHint("Lokalizacja");
                lockIcon.setBackgroundResource(R.drawable.ic_new_lock_icon);
            }
            GlobalClass.ctxClose(ctx);
        }
    }

    public void getElementsFromIntent() {
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        database = (getIntent().getStringExtra("database"));
        back_article = (getIntent().getStringExtra("art_idno"));
        userSwd = getIntent().getStringExtra("userSwd");
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(WarehouseStockTransfer.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(WarehouseStockTransfer.this, "",
                    "Ładowanie. Proszę czekać...", true);
        }

        @Override
        protected String doInBackground(String... strings) {
            String destination = strings[0],
                   article = strings[1];
            setIntent(destination, article);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
        }
    }

    public void setIntent(String destination, String article) {
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("content", article);
            intent.putExtra("destination", "WarehouseStockTransfer");
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
                    save_btn.setEnabled(true);
                    searchArticle(content, ctx);
                }
            }
            if (requestCode == 70) {
                if (resultCode == RESULT_OK) {
                    save_btn.setEnabled(true);
                    String content = result.getContents();
                    getLocation(content, ctx);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void enterFreehandLocation(View view){
        AlertDialog.Builder enterLocationDialog = new AlertDialog.Builder(WarehouseStockTransfer.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_enter_location, viewGroup, false);
        enterLocationDialog.setView(dialogView);
        AlertDialog locationDialog = enterLocationDialog.create();
        Button button_cancel = (Button)dialogView.findViewById(R.id.button_cancel);
        button_cancel .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationDialog.dismiss();}
        });
        Button button_ok = (Button)dialogView.findViewById(R.id.button_ok);
        button_ok.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v){
                EditText location = locationDialog.findViewById(R.id.locationEnter);
                String locationName = location.getText().toString();
                if(locationName.matches("")){ //jeśli jest pusty
                    locationDialog.dismiss();
                    GlobalClass.showDialog(WarehouseStockTransfer.this, "Brak wpisanej lokalizacji!", "Proszę wprowadzić lokalizację.", "OK",
                            new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {} });
                }else {
                    locationDialog.dismiss();
                    LoadingDialog = ProgressDialog.show(WarehouseStockTransfer.this, "",
                            "Ładowanie. Proszę czekać...", true);
                    searchLocation(locationName, ctx);
                }
            }
        });
        locationDialog.show();
        locationDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void searchLocation(String content, DbContext ctx) {
        save_btn.setEnabled(true);
        try{
            if(LocationExists(content.toUpperCase(), ctx) != null) {
                GlobalClass.ctxClose(ctx);
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                toLocation_textEdit.setText(content.toUpperCase());
            } else {
                GlobalClass.ctxClose(ctx);
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                GlobalClass.showDialog(WarehouseStockTransfer.this, "Brak lokalizacji!", "W bazie nie ma takej lokalizacji lub została ona błędnie wpisana!", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {} });
            }

        } catch (DBRuntimeException e) {
            GlobalClass.dismissLoadingDialog(LoadingDialog);
            catchExceptionCases(e, "searchLocation", content, ctx);
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
                    searchArticle(article_name, ctx);
                }
            }
        });
        articleDialog.show();
        articleDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    //  SEARCH ARTICLE
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void searchArticle(String content, DbContext ctx) {
        save_btn.setEnabled(true);
        myGlob = new GlobalClass(getApplicationContext());
        try{
            if(myGlob.FindProductByIdno(ctx, content) != null) {
                drawTable(ctx, content);
                GlobalClass.dismissLoadingDialog(LoadingDialog);

            //jeśli nie znajdzie by IDNO
            }else if (myGlob.FindProductByDescr(ctx, content) != null){
                GlobalClass.ctxClose(ctx);
                new setIntentAsyncTask().execute("ArticleNameList", content);

            // jeśli nie znajdzie by DESCR
            } else if (myGlob.FindProductBySwd(ctx, content) != null) {
                GlobalClass.ctxClose(ctx);
                new setIntentAsyncTask().execute("ArticleNameList", content);

            // jeśli nie znajdzie ani tu ani tu
            } else {
                GlobalClass.ctxClose(ctx);
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                GlobalClass.showDialog(WarehouseStockTransfer.this, "Brak artykułu!", "W bazie nie ma takeigo artykłu!", "OK",
                new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {} });
            }

        } catch (DBRuntimeException e) {
            GlobalClass.dismissLoadingDialog(LoadingDialog);
           catchExceptionCases(e, "searchArticle", content, ctx);
        }
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function, String parameter, DbContext ctx){

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
                @SuppressLint("NewApi")
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                public void handleMessage(Message msg) {
                    GlobalClass.dismissLoadingDialog(LoadingDialog);
                    if (function.equals("searchArticle")) {
                        searchArticle(parameter, ctx);
                    }else if(function.equals("save")){
                        save_btn.callOnClick();
                    }else if(function.equals("setLook")){
                        setLook();
                    }else if(function.equals("setLocation")){
                        searchLocation(parameter, ctx);
                    }
                }
            };
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

                    TableRow tableRowStock = GlobalClass.setTableRowList(this);
                    TextView article_textViewTable = new TextView(this);
                    TextView qty_textViewTable = new TextView(this);
                    TextView unit_textViewTable = new TextView(this);
                    TextView place_textViewTable = new TextView(this);

                    TextView[] textViewArray = {article_textViewTable, qty_textViewTable, unit_textViewTable, place_textViewTable};
                    Integer j = stockLayout.getChildCount();
                    j = j - 1; //  table header nie ma być brany pod uwagę więc -1
                    for (TextView textView :textViewArray) {
                        if (j % 2 == 0) {
                            textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
                        } else {
                            textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        }
                    }

                    // Lokalizacja
                    String platzFrom = row.getLplatz().getSwd();
                    GlobalClass.setParamForTextView(place_textViewTable, platzFrom, 13, 20, 5, false);

                    //Artykuł
                    String art = FindProductByIdno(ctx, content).getSwd(),
                           art_descr = FindProductByIdno(ctx, content).getDescr6();
                    TextView articleName_text = (TextView) stockDialog.findViewById(R.id.articleName_textView);
                    articleName_text.setText(Html.fromHtml("<b> " + art_descr + "<b> "));
                    artIDNO = FindProductByIdno(ctx, content).getIdno();
                    String artText = "";
                    if (j == 1) {// ustawia Klucz artykułu tylko dla pierwszego wiersza
                        artText = art;
                    }
                    GlobalClass.setParamForTextView(article_textViewTable, artText, 13, 20, 5, true);

                    //  Jednostka
                    String unit;
                    unit = row.getString("leinheit");
                    unit = GlobalClass.getProperUnit(unit);
                    GlobalClass.setParamForTextView(unit_textViewTable, unit, 13, 20, 5, false);

                    //  Ilość
                    BigDecimal qtyDecimal = row.getLemge().stripTrailingZeros();  //by po przecinku usunąć niepotrzebne zera
                    String Qty = qtyDecimal.toPlainString();    //by wyświetlało liczbę w formacie 20 a nie 2E+1
                    GlobalClass.setParamForTextView(qty_textViewTable, Qty, 13, 20, 5, false);

                    for (TextView textView :textViewArray) {
                        tableRowStock.addView(textView);
                    }
                    stockLayout.addView(tableRowStock, j);

                    String finalUnit = unit, finalQty = Qty;
                    tableRowStock.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            stockDialog.dismiss();
                          //  String platzFrom = row.getLplatz().getSwd();
                            fromLocation_textEdit.setText(platzFrom);
                            article_textEdit.setText(art);
                            unit_textView.setText(finalUnit);
                            qty_textEdit.setHint(finalQty);
                            qty_textEdit.setText("");
                            unit_textView.setVisibility(View.VISIBLE);
                            article_textInfo.setVisibility(View.VISIBLE);
                            location_textInfo.setVisibility(View.VISIBLE);
                            qty_textInfo.setVisibility(View.VISIBLE);
                            GlobalClass.ctxClose(ctx);
                        }
                    });
                }
            }
        }else{
            GlobalClass.showDialog(this, "Brak stanu!", "Artykuł ten nie jest obecnie w zapasie.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
            stockDialog.dismiss();
            GlobalClass.ctxClose(ctx);
        }
    }

    public void getLocation(String content, DbContext ctx) {
        LocationHeader location = LocationExists(content, ctx);
        if (location != null) {
            String location_name = location.getSwd();
            toLocation_textEdit.setText(location_name);
            article_textInfo.setVisibility(View.VISIBLE);
            qty_textInfo.setVisibility(View.VISIBLE);
            location_textInfo.setVisibility(View.VISIBLE);
            GlobalClass.ctxClose(ctx);
        }else {
            toLocation_textEdit.setText("");
            GlobalClass.showDialog(this, "Brak lokalizacji!", "Zeskanowana lokalizacja nie istnieje.", "OK",
            new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {} });
        }
    }

    public LocationHeader LocationExists(String location, DbContext ctx) {
        LocationHeader loc = null;
        try {
            SelectionBuilder<LocationHeader> locationSB = SelectionBuilder.create(LocationHeader.class);
            locationSB.add(Conditions.eq(LocationHeader.META.swd, location));
            loc = QueryUtil.getFirst(ctx, locationSB.build());
            GlobalClass.ctxClose(ctx);
        } catch (Exception e) {
            Log.d("error", e.getMessage());
        }
        return loc;
    }

    public void save(View view) {
        LoadingDialog = ProgressDialog.show(WarehouseStockTransfer.this, "",
                "Ładowanie. Proszę czekać...", true);

        Boolean emptyFields = checkIfFieldsEmpty(),
                locationsAreEqual = checkIfLocationsAreEqual();

        if((emptyFields == false) && (locationsAreEqual == false)){
            save_btn.setEnabled(false);
            //  GlobalClass.ctxClose(ctx);
            // ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            employee = FindEmployeeBySwd(ctx, user_short_name);
            EmployeeEditor employeeEditor = employee.createEditor();
            try {
                employeeEditor.open(EditorAction.UPDATE);

                if (lockIcon.isChecked()) {
                    employeeEditor.setYapplocklocation(toLocation_textEdit.getText().toString());
                } else {
                    employeeEditor.setYapplocklocation("");
                }
                employeeEditor.commit();
                if (employeeEditor.active()) {
                    employeeEditor.abort();
                }
                enterIntoAbas();
                AlertDialog.Builder stockAddAlert = new AlertDialog.Builder(WarehouseStockTransfer.this);
                stockAddAlert.setMessage("Produkt został pomyślnie przeksięgowany.");
                stockAddAlert.setTitle("Zapisano!");
                stockAddAlert.setPositiveButton("Dodaj nowy artykuł",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                GlobalClass.dismissLoadingDialog(LoadingDialog);
                                save_btn.setEnabled(true);
                                article_textEdit.setText("");
                                fromLocation_textEdit.setText("");
                                qty_textEdit.setText("");
                                qty_textEdit.setHint("Ilość");
                                unit_textView.setVisibility(View.INVISIBLE);
                                toLocation_textEdit.setText("");
                                try {
                                    ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");   //?? potrzebne policy?
                                employee = FindEmployeeBySwd(ctx, user_short_name);
                                } catch (DBRuntimeException e) {
                                    catchExceptionCases(e, "save", "", null);
                                }
                                if(employee != null){
                                    if (!employee.getYapplocklocation().equals("")) {
                                        toLocation_textEdit.setHint(employee.getYapplocklocation());
                                        lockIcon.setBackgroundResource(R.drawable.ic_new_lock_closed_icon);
                                        lockIcon.setChecked(true);
                                        GlobalClass.ctxClose(ctx);
                                    } else {
                                        toLocation_textEdit.setHint("Lokalizacja");
                                        lockIcon.setBackgroundResource(R.drawable.ic_new_lock_icon);
                                        lockIcon.setChecked(false);
                                        GlobalClass.ctxClose(ctx);
                                    }
                                }
                            }
                        });
                stockAddAlert.setNegativeButton("Menu",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //dismiss the dialog
                                GlobalClass.dismissLoadingDialog(LoadingDialog);
                                new setIntentAsyncTask().execute("Menu", "");
                            }
                        });
                stockAddAlert.setCancelable(true);
                stockAddAlert.create().show();

            }catch (CommandException e) {
                e.printStackTrace();
            }
        }
    }

    public Boolean checkIfLocationsAreEqual(){
        Boolean locationsEqual = false;
        if(fromLocation_textEdit.getText().toString().equals(toLocation_textEdit.getText().toString())){
            if(!fromLocation_textEdit.getText().toString().equals("")) {
                locationsEqual = true;
                GlobalClass.showDialog(this, "Takie same lokalizacje!", "Nie można przeksięgować artykułu do tej samej lokalizacji.", "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GlobalClass.dismissLoadingDialog(LoadingDialog);
                    }
                });
            }
        }
        return locationsEqual;
    }

    public void enterIntoAbas() throws CommandException {
        EditorCommand cmd = EditorCommandFactory.typedCmd("Lbuchung", "");
        StockAdjustmentEditor stockAdjustmentEditor = (StockAdjustmentEditor) ctx.openEditor(cmd);
        AbasDate today = new AbasDate();
        stockAdjustmentEditor.setString("product", artIDNO);
        stockAdjustmentEditor.setDocNo("MOBILE");
        stockAdjustmentEditor.setDateDoc(today);
        stockAdjustmentEditor.setEntType(EnumEntryTypeStockAdjustment.Transfer);
        StockAdjustmentEditor.Row sadRow = stockAdjustmentEditor.table().getRow(1);
        sadRow.setString("unitQty", qty_textEdit.getText().toString());
        sadRow.setString("location", fromLocation_textEdit.getText().toString());
        sadRow.setString("location2", toLocation_textEdit.getText().toString());
        stockAdjustmentEditor.commit();
        if(stockAdjustmentEditor.active()) {
            stockAdjustmentEditor.abort();
        }
        GlobalClass.ctxClose(ctx);
    }

    public Boolean checkIfFieldsEmpty() {
        Boolean emptyFields = false;
        if (article_textEdit.getText().toString().isEmpty()) {
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak artykułu!", "Proszę wprowadzić artykuł", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override  public void onClick(DialogInterface dialog, int which) {
                            GlobalClass.dismissLoadingDialog(LoadingDialog);
                        }
                    });
        } else if (toLocation_textEdit.getText().toString().isEmpty()) {
            if (toLocation_textEdit.getHint().toString().equals("Lokalizacja")) {
                emptyFields = true;
                GlobalClass.showDialog(this, "Brak lokalizacji!", "Proszę wprowadzić lokalizację.", "OK",
                        new DialogInterface.OnClickListener() {@Override public void onClick(DialogInterface dialog, int which) {
                            GlobalClass.dismissLoadingDialog(LoadingDialog);
                        }});
            } else{
                toLocation_textEdit.setText(toLocation_textEdit.getHint().toString());
                if (qty_textEdit.getText().toString().isEmpty()) {
                    qty_textEdit.setText(qty_textEdit.getHint().toString());
                }
                if (new BigDecimal(qty_textEdit.getText().toString()).compareTo(new BigDecimal(qty_textEdit.getHint().toString())) == 1) {
                    emptyFields = true;
                    GlobalClass.showDialog(this, "Wykroczenie poza stan!", "Wpisana ilość przekracza ilość dostępną na stanie.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override  public void onClick(DialogInterface dialog, int which) {
                            GlobalClass.dismissLoadingDialog(LoadingDialog);
                        }
                    });
                }
            }
        }else {
            if (qty_textEdit.getText().toString().isEmpty()) {
                qty_textEdit.setText(qty_textEdit.getHint().toString());
            }
            if (new BigDecimal(qty_textEdit.getText().toString()).compareTo(new BigDecimal(qty_textEdit.getHint().toString())) == 1) {  // jak dalej będą błędy to zamienić "," na "."
                emptyFields = true;
                GlobalClass.showDialog(this, "Wykroczenie poza stan!", "Wpisana ilość przekracza ilość dostępną na stanie.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override  public void onClick(DialogInterface dialog, int which) {
                                GlobalClass.dismissLoadingDialog(LoadingDialog);
                            }
                        });
            }
        }
        return emptyFields;
    }

    public final Employee FindEmployeeBySwd(DbContext ctx, String name){
        Employee employee = null;
        SelectionBuilder<Employee> employeeSB = SelectionBuilder.create(Employee.class);
        try {
            employeeSB.add(Conditions.eq(Product.META.swd.toString(), name));
            employee = QueryUtil.getFirst(ctx, employeeSB.build());
        } catch (Exception e) {
        }
        GlobalClass.ctxClose(ctx);
        return employee;
    }
}