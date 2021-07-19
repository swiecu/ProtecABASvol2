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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.abas.erp.common.type.AbasDate;
import de.abas.erp.common.type.enums.EnumEntryTypeStockAdjustment;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorCommand;
import de.abas.erp.db.EditorCommandFactory;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.standard.la.StockLevelInformation;
import de.abas.erp.db.schema.location.LocationHeader;
import de.abas.erp.db.schema.storagequantity.StockAdjustmentEditor;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

import static protec.pl.protecabasvol2.GlobalClass.FindProductByIdno;

public class MoveLeaveArticle extends AppCompatActivity {
    private String password;
    public String getPassword() { return password;}
    public void setPassword(String password) {this.password = password; }
    DbContext ctx, sessionCtx;
    ProgressDialog LoadingDialog;
    TextView article_textEdit, location_textEdit, unit_textView, qty_textEdit, location_textInfo, qty_textInfo, article_textInfo;
    TableLayout WDRlayout; View leave_btn, enterArticle_btn;
    String database, artIDNO, userSwd;
    GlobalClass globFunctions; Handler handler; Intent intent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_leave_article);
        getElementsFromIntent();
        getElementsById();
        setLook();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MoveLeaveArticle.this, userSwd));
    }
    // na kliknięcie cofnij
    public void onBackPressed(){
        GlobalClass.ctxClose(ctx);
        new setIntentAsyncTask().execute("Move");
        super.onBackPressed();
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        GlobalClass.ctxClose(ctx);
        super.onPause();
    }

    public void getElementsFromIntent(){
        String password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        setPassword(password);
        userSwd = getIntent().getStringExtra("userSwd");
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(MoveLeaveArticle.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(MoveLeaveArticle.this, "",
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

    public void setIntent(String destination){
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void getElementsById(){
        article_textEdit = findViewById(R.id.article_textEdit);
        location_textEdit = findViewById(R.id.to_textEdit);
        qty_textEdit = findViewById(R.id.qty_TOtextEdit);
        unit_textView = findViewById(R.id.unit_textView);
        location_textInfo = findViewById(R.id.locationFrom_textInfo);
        qty_textInfo = findViewById(R.id.qty_textInfo);
        article_textInfo = findViewById(R.id.article_textInfo);
        leave_btn = findViewById(R.id.leave_btn);
        enterArticle_btn = findViewById(R.id.enterArticle_btn);
    }

    public void setLook(){
        unit_textView.setVisibility(View.INVISIBLE);
        location_textInfo.setVisibility(View.INVISIBLE);
        qty_textInfo.setVisibility(View.INVISIBLE);
        article_textInfo.setVisibility(View.INVISIBLE);
        article_textEdit.setInputType(0);
        location_textEdit.setInputType(0);
        leave_btn.setEnabled(true);
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        GlobalClass.ctxClose(ctx);
    }

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

    public void scanLocation(View view){
        try{
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować lokalizację");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            Intent intent = integrator.createScanIntent();
            startActivityForResult(intent , 70);
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
                    LoadingDialog = ProgressDialog.show(MoveLeaveArticle.this, "",
                            "Ładowanie. Proszę czekać...", true);
                    leave_btn.setEnabled(true);
                    checkWDRStock(content);
                    article_textInfo.setVisibility(View.VISIBLE);
                    location_textInfo.setVisibility(View.VISIBLE);
                    qty_textInfo.setVisibility(View.VISIBLE);
                }
            }
            if (requestCode == 70) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents(), location_name = "";
                    LocationHeader location = LocationExists(content);
                    if(location != null){
                        location_name = location.getSwd();
                        GlobalClass.ctxClose(ctx);
                        location_textEdit.setText(location_name);
                        article_textInfo.setVisibility(View.VISIBLE);
                        location_textInfo.setVisibility(View.VISIBLE);
                        qty_textInfo.setVisibility(View.VISIBLE);
                    }else{
                        location_textEdit.setText("");
                        GlobalClass.showDialog(this, "Brak lokalizacji!", "Zeskanowana lokalizacja nie istnieje.", "OK", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {} });
                    }
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    // CHECK WDR STOCK
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("WrongViewCast")
    public void checkWDRStock(String content) {
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            String articleSWD, qty, unit;
            StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
            sli.setString("klplatz", "WDR");
            sli.setNullmge(false);
            if (FindProductByIdno(ctx, content) != null) {
                sli.setArtikel(FindProductByIdno(ctx, content));
                sli.invokeStart();
                Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
                Integer nrRows = sli.getRowCount();
                if (nrRows != 0) {
                    for (StockLevelInformation.Row row : sliRows) {
                        articleSWD = row.getTartikel().getSwd();
                        artIDNO = row.getTartikel().getIdno();
                        qty = row.getLemge().stripTrailingZeros().toPlainString();
                        unit = row.getString("leinheit");
                        unit = GlobalClass.getProperUnit(unit);

                        unit_textView.setText(unit);
                        article_textEdit.setText(articleSWD);
                        unit_textView.setVisibility(View.VISIBLE);
                        qty_textEdit.setHint(qty);
                        GlobalClass.ctxClose(ctx);
                    }
                } else {// na WDR nie ma takiego artykułu
                    GlobalClass.showDialog(this, "Brak artykułu w WDR!", "W lokalizacji WDR nie ma takiego artykułu.", "OK",
                            new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {} });
                }
            } else { // FindProdByIdno zwrócił null
                GlobalClass.showDialog(this, "Nie zeskanowano artykułu!", "Proszę zeskanować nr ID artykułu.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {} });
            }
            GlobalClass.dismissLoadingDialog(LoadingDialog);
            GlobalClass.ctxClose(ctx);
        }catch (DBRuntimeException e){
            GlobalClass.dismissLoadingDialog(LoadingDialog);
            catchExceptionCases(e, "checkWDRStock", content);
        }
    }

    @SuppressLint("WrongViewCast")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void chooseArticleFromWDRStockTable(View view){
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
            sli.setString("klplatz", "WDR");
            sli.setNullmge(false);
            sli.invokeStart();
            Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
            Integer nrRows = sli.getRowCount();

            if (nrRows != 0) {
                AlertDialog.Builder WDRstockDialog = new AlertDialog.Builder(MoveLeaveArticle.this);
                ViewGroup viewGroup = findViewById(android.R.id.content);
                View dialogView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_wdr_stock, viewGroup, false);
                WDRstockDialog.setView(dialogView);
                AlertDialog wdrDialog = WDRstockDialog.create();
                wdrDialog.show();
                for (StockLevelInformation.Row row : sliRows) {
                    wdrDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    WDRlayout = (TableLayout) wdrDialog.findViewById(R.id.wdrkNameTable);

                    TableRow tableRowWDR = GlobalClass.setTableRowList(this);
                    TextView article_textViewTable = new TextView(this);
                    TextView qty_textViewTable = new TextView(this);
                    TextView unit_textViewTable = new TextView(this);
                    TextView[] textViewArray = {article_textViewTable, qty_textViewTable, unit_textViewTable};
                    Integer j = WDRlayout.getChildCount();
                    for (TextView textView :textViewArray) {
                        if (j % 2 == 0) {
                            textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        } else {
                            textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
                        }
                    }

                    //Article
                    String art = row.getTartikel().getSwd();
                    String artIdno = row.getTartikel().getIdno();
                    GlobalClass.setParamForTextView(article_textViewTable, art, 13, 20, 5, true);

                    //Unit
                    String unit = row.getString("leinheit");
                    unit = GlobalClass.getProperUnit(unit);
                    GlobalClass.setParamForTextView(unit_textViewTable, unit, 13, 20, 5, false);

                    //Qty
                    String qty = row.getLemge().stripTrailingZeros().toPlainString();  //by po przecinku usunąć niepotrzebne zera
                    GlobalClass.setParamForTextView(qty_textViewTable, qty, 13, 20, 5, false);

                    for (TextView textView :textViewArray) {
                        tableRowWDR.addView(textView);
                    }

                    WDRlayout.addView(tableRowWDR, j);

                    String finalUnit = unit;
                    tableRowWDR.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            wdrDialog.dismiss();
                            artIDNO = artIdno; //zmienione z row.getTartikle.getIdno()
                            leave_btn.setEnabled(true);
                            article_textEdit.setInputType(0);
                            article_textEdit.setText(art);
                            unit_textView.setText(finalUnit);
                            qty_textEdit.setHint(qty);
                            unit_textView.setVisibility(View.VISIBLE);
                            article_textInfo.setVisibility(View.VISIBLE);
                            location_textInfo.setVisibility(View.VISIBLE);
                            qty_textInfo.setVisibility(View.VISIBLE);
                            GlobalClass.ctxClose(ctx);
                        }
                    });
                }
            } else {
                GlobalClass.showDialog(this, "Brak artykułów!", "Brak artykułów na miejscu składowania WDR.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                GlobalClass.ctxClose(ctx);
            }
        }catch (DBRuntimeException e){
            GlobalClass.dismissLoadingDialog(LoadingDialog);
            catchExceptionCases(e, "chooseArticleFromWDRStockTable", "");
        }
    }

    @SuppressLint("HandlerLeak")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void save(View view) {
        String article = article_textEdit.getText().toString(), location = location_textEdit.getText().toString(), qty = qty_textEdit.getText().toString();
        globFunctions = new GlobalClass(getApplicationContext());
        Boolean emptyFields = checkIfFieldsEmpty(article, location, qty);

        if(emptyFields == false){
            try {
                leave_btn.setEnabled(false);
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
                sadRow.setString("location", "WDR");
                sadRow.setString("location2", location);
                stockAdjustmentEditor.commit();
                article_textEdit.setText("");
                location_textEdit.setText("");
                qty_textEdit.setText("");
                GlobalClass.showDialog(this, "Odłożono!", "Materiał został odłożony i dodany do bazy.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new setIntentAsyncTask().execute("Move");
                            }
                        });
                GlobalClass.ctxClose(ctx);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                GlobalClass.showDialog(this, "Błąd!", "Podczas zmiany formatu wystąpił błąd.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
            } catch (DBRuntimeException e) {
                catchExceptionCases(e, "leaveBtnCallOnClick", "");

            } catch (CommandException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function, String parameter){
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
                    if(function.equals("checkWDRStock")) {
                        checkWDRStock(parameter);
                    }else if(function.equals("leaveBtnCallOnClick")){
                        leave_btn.callOnClick();
                    }else if(function.equals("chooseArticleFromWDRStockTable")){
                        enterArticle_btn.callOnClick();
                    }
                }
            };
        }
    }

    public Boolean checkIfFieldsEmpty(String article, String location, String qty){
        Boolean emptyFields= false;
        if (article.equals("")) {
            emptyFields= true;
            GlobalClass.showDialog(this, "Brak artykułu!", "Proszę wprowadzić artykuł", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {}
                    });
        } else if (location.equals("")) {
            emptyFields= true;
            GlobalClass.showDialog(this, "Brak lokalizacji!", "Proszę wprowadzić lokalizację", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {}
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


    public LocationHeader LocationExists(String location){
        LocationHeader loc = null;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<LocationHeader> locationSB = SelectionBuilder.create(LocationHeader.class);
            locationSB.add(Conditions.eq(LocationHeader.META.swd, location));
            loc = QueryUtil.getFirst(ctx, locationSB.build());
            GlobalClass.ctxClose(ctx);
        }catch (Exception e) {
            Log.d("error", e.getMessage());
        }
        return loc;
    }
}
