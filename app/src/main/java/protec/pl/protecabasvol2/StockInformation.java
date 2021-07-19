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

import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorAction;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.standard.la.StockLevelInformation;
import de.abas.erp.db.schema.employee.Employee;
import de.abas.erp.db.schema.employee.EmployeeEditor;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.warehouse.WarehouseGroup;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

import static protec.pl.protecabasvol2.GlobalClass.FindProductByIdno;

public class StockInformation extends Activity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx, sessionCtx; ProgressDialog LoadingDialog; TableLayout layout;
    TextView article_name, suma; TableRow no_art; GlobalClass globFunctions;
    String database, back_article, userSwd;
    Handler handler; Intent intent;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_information);
        getElementsById();
        setLook();
        getElementsFromIntent();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(StockInformation.this, userSwd));
        setPassword(password);

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
        GlobalClass.dismissLoadingDialog(LoadingDialog);
        super.onStop();
    }
    //na cofnięcie Back do tyłu
    @Override
    public void onBackPressed(){
        new setIntentAsyncTask().execute("Menu", "");
        super.onBackPressed();
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        GlobalClass.ctxClose(ctx);
        super.onPause();
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(StockInformation.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(StockInformation.this, "",
                    "Ładowanie. Proszę czekać...", true);
        }

        @Override
        protected String doInBackground(String... strings) {
            String destination = strings[0],
                    content = strings[1];
            setIntent(destination, content);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
        }
    }

    public void setIntent(String destination, String content){
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("content", content);
            intent.putExtra("destination", "StockInformation");
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void getElementsById(){
        layout = (TableLayout) findViewById(R.id.articleNameTable);
        article_name = (TextView) findViewById(R.id.article_name); //nazwa artkułu nad tabelką
        suma = (TextView) findViewById(R.id.suma);
        no_art = (TableRow) findViewById(R.id.no_articles); //table row "Brak artykułów"
    }

    public void setLook(){
        article_name.setVisibility(View.GONE);
        suma.setVisibility(View.GONE);
        no_art.setVisibility(View.VISIBLE);
    }
    public void getElementsFromIntent(){
        password = (getIntent().getStringExtra("password"));
        back_article = (getIntent().getStringExtra("art_idno"));
        database = (getIntent().getStringExtra("database"));
        userSwd = getIntent().getStringExtra("userSwd");
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
        layout.removeViews(1, (layout.getChildCount() -2)); //zmienia liczbę View dla layoutu ("Brak artykułu" jest schowane ale dalej jest child od layout)
        try{
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            if(globFunctions.FindProductByIdno(ctx, content) != null) {  //jeśli Find by IDNO nie równa się null
                drawTable(ctx, content);
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                updateCheckCountOnAbas(ctx, userSwd);

                //jeśli nie znajdzie by IDNO
            }else if (globFunctions.FindProductByDescr(ctx, content) != null){
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                new setIntentAsyncTask().execute("ArticleNameList", content);

                // jeśli nie znajdzie by DESCR
            } else if (globFunctions.FindProductBySwd(ctx, content) != null) {   //jeśli Find by SWD nie równa się null
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                new setIntentAsyncTask().execute("ArticleNameList", content);

                // jeśli nie znajdzie ani tu ani tu
            } else {
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                no_art.setVisibility(View.VISIBLE);
                GlobalClass.showDialog(this, "Brak artykułu!", "W bazie nie ma takiego artykłu!", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {} });
            }
            GlobalClass.ctxClose(ctx);
        } catch (DBRuntimeException e) {
            catchExceptionCases(e, "searchArticle", content);
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
                    if(function.equals("searchArticle")) {
                        searchArticle(parameter);
                    }
                }
            };
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(DbContext ctx, String content){
        BigDecimal sum = BigDecimal.ZERO;
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
        sli.setArtikel(FindProductByIdno(ctx, content));
        sli.setKlgruppe((WarehouseGroup) null);
        sli.setNullmge(false);
        sli.invokeStart();
        Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
        Integer nrRows = sli.getRowCount();
        if (nrRows != 0) {
            for (StockLevelInformation.Row row : sliRows) {
                TableRow tableRow = GlobalClass.setTableRowList(this);
                TextView id_textViewTable = new TextView(this);
                TextView article_textViewTable = new TextView(this);
                TextView qty_textViewTable = new TextView(this);
                TextView unit_textViewTable = new TextView(this);
                TextView place_textViewTable = new TextView(this);
                TextView[] textViewArray = {id_textViewTable, article_textViewTable, qty_textViewTable, unit_textViewTable, place_textViewTable};

                Integer j = layout.getChildCount();
                j = j - 1; //  table header nie ma być brany pod uwagę więc -1
                for (TextView textView :textViewArray) {
                    if (j % 2 == 0) {
                        textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    } else {
                        textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    }
                }

                // id
                GlobalClass.setParamForTextView(id_textViewTable, (j).toString(), 13, 20, 5, false);

                //place
                GlobalClass.setParamForTextView(place_textViewTable, row.getLplatz().getSwd(), 13, 20, 5, false);

                //article
                String artText = "";
                if (j == 1) {// ustawia Klucz artykułu tylko dla pierwszego wiersza
                    artText = FindProductByIdno(ctx, content).getSwd();
                }
                GlobalClass.setParamForTextView(article_textViewTable, artText, 13, 20, 5, false);

                //qty
                String Qty = row.getLemge().stripTrailingZeros().toPlainString();
                GlobalClass.setParamForTextView(qty_textViewTable, Qty, 13, 20, 5, false);

                //unit
                String unit;
                unit = row.getString("leinheit");
                unit = GlobalClass.getProperUnit(unit);
                GlobalClass.setParamForTextView(unit_textViewTable, unit, 13, 20, 5, false);

                for (TextView textView :textViewArray) {
                    tableRow.addView(textView);
                }
                layout.addView(tableRow, j);

                sum = sum.add(new BigDecimal(Qty));
                String articleName = ((Product) row.getTartikel()).getDescr6();
                String sumString = sumString = "Suma: <b> " + sum + "<b> " + unit;
                article_name.setText(Html.fromHtml("Nazwa: <b>" + articleName));
                article_name.setVisibility(View.VISIBLE);
                suma.setText(Html.fromHtml(sumString));
                suma.setVisibility(View.VISIBLE);
                Log.d("TEST", "BEFORE");
                GlobalClass.sendNotification(ctx, "TO JEST POWIADOMIENIE TYLKO DLA tytytyty", "SPRAWDZONO WLASNIE STAN!", "KJ");
                Log.d("TEST", "AFTER");


            }
        }else{
            GlobalClass.showDialog(this, "Brak stanu!", "Artykuł ten nie jest obecnie w zapasie.", "OK", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {} });
            no_art.setVisibility(View.VISIBLE);
        }
        GlobalClass.ctxClose(ctx);
    }

    public void updateCheckCountOnAbas(DbContext ctx, String userSwd) {
        Employee employee = FindEmployeeBySwd(ctx, userSwd);
        Integer checkCount = employee.getYstockqtycheckcoun();
        Log.d("check Count before", checkCount.toString());
        checkCount++;
        Log.d("check Count after", checkCount.toString());
        EmployeeEditor employeeEditor = employee.createEditor();
        try {
            employeeEditor.open(EditorAction.UPDATE);
            employeeEditor.setString("ystockqtycheckcoun", checkCount.toString());
            employeeEditor.commit();
            if(employeeEditor.active()){
                employeeEditor.abort();
            }
        } catch (CommandException e) {
            e.printStackTrace();
        }
    }

    public final Employee FindEmployeeBySwd(DbContext ctx, String name){
        Employee employee = null;
        SelectionBuilder<Employee> employeeSB = SelectionBuilder.create(Employee.class);
        try {
            employeeSB.add(Conditions.eq(Employee.META.swd, name));
            employee = QueryUtil.getFirst(ctx, employeeSB.build());
        } catch (Exception e) {
        }
        return employee;
    }
}