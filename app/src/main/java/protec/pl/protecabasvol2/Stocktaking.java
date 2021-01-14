package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorAction;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.schema.custom.protec.StocktakingProtec;
import de.abas.erp.db.schema.custom.protec.StocktakingProtecEditor;
import de.abas.erp.db.schema.location.LocationHeader;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class Stocktaking extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx, sessionCtx;
    ProgressDialog LoadingDialog;
    String database, stockID, back_article, qtySumEquation, userSwd;
    CheckBox lockIcon;
    TextView unit, test, article_textInfo, location_textInfo, qty_textInfo, info_textInfo, equation_textView;
    EditText article_textEdit, location_textEdit, qty_textEdit, info_textEdit;
    GlobalClass myGlob;
    StocktakingProtec stocktaking = null;
    Boolean isHandWritten;
    BigDecimal qtySum = new BigDecimal(BigInteger.ZERO);
    View save_btn;
    Handler handler;
    Intent intent;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stocktaking);
        getElementsFromIntent();
        getElementsById();
        setLook();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(Stocktaking.this, userSwd));
    }

    public void onBackPressed() {
        super.onBackPressed();
        new setIntentAsyncTask().execute("Menu", "", "");
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        super.onStop();
        if (LoadingDialog != null) {
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

    public void getElementsFromIntent() {
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        database = (getIntent().getStringExtra("database"));
        stockID = (getIntent().getStringExtra("stockID"));
        back_article = (getIntent().getStringExtra("art_idno"));
        userSwd = getIntent().getStringExtra("userSwd");
    }

    public void getElementsById() {
        lockIcon = (CheckBox) findViewById(R.id.lockIcon);
        unit = (TextView) findViewById(R.id.unit_textView);
        test = (TextView) findViewById(R.id.stockInfo_textView);
        article_textEdit = (EditText) findViewById(R.id.article_textEdit);
        location_textEdit = (EditText) findViewById(R.id.to_textEdit);
        qty_textEdit = (EditText) findViewById(R.id.qty_TOtextEdit);
        article_textInfo = (TextView) findViewById(R.id.article_textInfo);
        location_textInfo = (TextView) findViewById(R.id.locationFrom_textInfo);
        qty_textInfo = (TextView) findViewById(R.id.qty_textInfo);
        info_textInfo = (TextView) findViewById(R.id.info_textInfo);
        info_textEdit= (EditText) findViewById(R.id.info_textEdit);
        equation_textView = (TextView) findViewById(R.id.equation_textView);
        save_btn = findViewById(R.id.save_btn);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void setLook() {
        unit.setVisibility(View.INVISIBLE);
        info_textEdit.setText("");
        location_textEdit.setInputType(0);
        article_textEdit.setInputType(0);
        isHandWritten = false;
        equation_textView.setVisibility(View.INVISIBLE);
        save_btn.setEnabled(true);

        stocktaking = getStocktaing();
        if(!stocktaking.getYcurrlocation().equals("")){
            lockIcon.setChecked(true);
            location_textEdit.setHint(stocktaking.getYcurrlocation());
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_closed_icon);   //OK
        }else{
            lockIcon.setChecked(false);
            location_textEdit.setHint("Lokalizacja");
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_icon);
        }
        if(ctx != null) {
            ctx.close();
        }

        //jeśli wraca z ArticleNameList
        if(back_article != null) {
            String password = (getIntent().getStringExtra("password"));
            setPassword(password);
            LoadingDialog = ProgressDialog.show(Stocktaking.this, "",
                    "Ładowanie. Proszę czekać...", true);
            isHandWritten = true;
            Log.d("isHandWritten SetLook", isHandWritten.toString());

            searchArticle(back_article);
        }
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(Stocktaking.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(Stocktaking.this, "",
                    "Ładowanie. Proszę czekać...", true);
        }

        @Override
        protected String doInBackground(String... strings) {
            String destination = strings[0];
            String stockID = strings[1];
            String article = strings[2];
            setIntent(destination, stockID, article);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
        }
    }

    public void setIntent(String destination, String stockID, String article) {
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("stockID", stockID);
            intent.putExtra("content", article);
            intent.putExtra("destination", "Stocktaking");
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("WrongViewCast")
    public void lockLocation(View view) {
        lockIcon = (CheckBox) view;
        if (lockIcon.isChecked()) {
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_closed_icon);
        } else {
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_icon);  //Ok
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
                    setPageLookForArticle(content);
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void setPageLookForArticle(String content) {
        myGlob = new GlobalClass(getApplicationContext());
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            Product product = myGlob.FindProductByIdno(ctx, content);
            if (product != null) {  //jeśli Find by IDNO nie równa się null
                article_textEdit.setText(product.getIdno());
                article_textInfo.setVisibility(View.VISIBLE);
                info_textInfo.setVisibility(View.VISIBLE);
                qty_textInfo.setVisibility(View.VISIBLE);
                location_textInfo.setVisibility(View.VISIBLE);
                String unit_text = product.getString("SU");
                if (unit_text.equals("(20)")) {
                    unit_text = "szt.";
                }else if (unit_text.equals("(7)")) {
                    unit_text = "kg";
                }else if (unit_text.equals("(21)")) {
                    unit_text = "kpl";
                }else if (unit_text.equals("(1)")) {
                    unit_text = "m";
                }else if (unit_text.equals("(10)")) {
                    unit_text = "tona";
                }else if (unit_text.equals("(28)")) {
                    unit_text = "arkusz";
                }
                unit.setVisibility(View.VISIBLE);
                unit.setText(unit_text);
                Log.d("isHandWrittenSetPag ", isHandWritten.toString());
                if(ctx != null) {
                    ctx.close();
                }
            } else {
                article_textEdit.setText("");
                GlobalClass.showDialog(Stocktaking.this, "Brak artykułu!", "W bazie nie ma takeigo artykłu!", "OK",
                new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(ctx != null) {
                        ctx.close();
                    }
                }
                });
            }
        } catch (DBRuntimeException e) {
            catchExceptionCases(e, "setPageLookForArticle", content);
        }
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function, String parameter){
        if(e.getMessage().contains("failed")){
            GlobalClass.showDialog(this,"Brak połączenia!","Nie można się aktualnie połączyć z bazą.", "OK",new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { } });

            //przekroczona liczba licencji
        }else if(e.getMessage().contains("FULL")){
            LoadingDialog = ProgressDialog.show(Stocktaking.this, "     Przekroczono liczbę licencji.",
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
                    if(LoadingDialog != null) {
                        LoadingDialog.dismiss();
                    }
                    if(function.equals("setPageLookForArticle")) {
                        setPageLookForArticle(parameter);
                    }else if(function.equals("LocationExists")){
                        getLocation(parameter);
                    }
                }
            };
        }
    }

    public void enterFreehandArticle(View view){
        AlertDialog.Builder enterArticleDialog = new AlertDialog.Builder(Stocktaking.this);
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
                    GlobalClass.showDialog(Stocktaking.this, "Brak wpisanego artykułu!", "Proszę wprowadzić artykuł.", "OK",
                        new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
                }else {
                    articleDialog.dismiss();
                    LoadingDialog = ProgressDialog.show(Stocktaking.this, "",
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
        save_btn.setEnabled(true);
        myGlob = new GlobalClass(getApplicationContext());
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            if (myGlob.FindProductByIdno(ctx, content) != null) {  //jeśli Find by IDNO nie równa się null
                article_textEdit.setText(content);
                if(ctx != null) {   //zakomentować jakby cos nie działało
                    ctx.close();    //
                }                   //
                setPageLookForArticle(content);
                if (LoadingDialog != null) {
                    LoadingDialog.dismiss();
                }
                //jeśli nie znajdzie by IDNO
            } else if (myGlob.FindProductByDescr(ctx, content) != null) {
                if(ctx != null) {
                    ctx.close();
                }
                if (LoadingDialog != null) {
                    LoadingDialog.dismiss();
                }
                new setIntentAsyncTask().execute("ArticleNameList", stockID, content);

                // jeśli nie znajdzie by DESCR
            } else if (myGlob.FindProductBySwd(ctx, content) != null) {   //jeśli Find by SWD nie równa się null
                if(ctx != null) {
                    ctx.close();
                }
                if (LoadingDialog != null) {
                    LoadingDialog.dismiss();
                }
                new setIntentAsyncTask().execute("ArticleNameList", stockID, content);

                // jeśli nie znajdzie ani tu ani tu
            } else {
                if(ctx != null) {
                    ctx.close();
                }
                if (LoadingDialog != null) {
                    LoadingDialog.dismiss();
                }
                GlobalClass.showDialog(Stocktaking.this, "Brak artykułu!", "W bazie nie ma takeigo artykłu!", "OK",
                new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {}
                });
            }
        } catch (DBRuntimeException e) {
            if (LoadingDialog != null) {
                LoadingDialog.dismiss();
            }
            catchExceptionCases(e, "getArticle", content);
        }
    }

    public void getLocation(String content) {
        LocationHeader location = LocationExists(content);
        if (location != null) {
            String location_name = location.getSwd();
            location_textEdit.setText(location_name);
            article_textInfo.setVisibility(View.VISIBLE);
            info_textInfo.setVisibility(View.VISIBLE);
            qty_textInfo.setVisibility(View.VISIBLE);
            location_textInfo.setVisibility(View.VISIBLE);
            if(ctx != null) {
                ctx.close();
            }
        }else {
            location_textEdit.setText("");
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
            if(ctx != null) {
                ctx.close();
            }
        } catch (DBRuntimeException e) {
            Log.d("error", e.getMessage());
            catchExceptionCases(e, "LocationExists", location);
        }
        return loc;
    }

    public void save(View view){
        Boolean emptyFields = checkIfFieldsEmpty();
        if(emptyFields == false){
            LoadingDialog = ProgressDialog.show(Stocktaking.this, "",
                    "Ładowanie. Proszę czekać...", true);
            save_btn.setEnabled(false);
            enterStockRow();
            AlertDialog.Builder stockAddAlert = new AlertDialog.Builder(Stocktaking.this);
            stockAddAlert.setMessage("Produkt został pomyślnie dodany do bazy inwentaryzacji.");
            stockAddAlert.setTitle("Dodano!");
            stockAddAlert.setPositiveButton("Dodaj nowy artykuł",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //dismiss the dialog
                            save_btn.setEnabled(true);
                            article_textEdit.setText("");
                            location_textEdit.setText("");
                            qty_textEdit.setText("");
                            qty_textEdit.setHint("Ilość");
                            info_textEdit.setText("");
                            stocktaking = getStocktaing();
                            if (!stocktaking.getYcurrlocation().equals("")) {
                                location_textEdit.setHint(stocktaking.getYcurrlocation());
                                lockIcon.setBackgroundResource(R.drawable.ic_new_lock_closed_icon);
                                lockIcon.setChecked(true);
                            } else {
                                location_textEdit.setHint("Lokalizacja");
                                lockIcon.setBackgroundResource(R.drawable.ic_new_lock_icon);
                                lockIcon.setChecked(false);
                            }
                            unit.setVisibility(View.INVISIBLE);
                            isHandWritten = false;
                            equation_textView.setText("suma");
                            equation_textView.setVisibility(View.INVISIBLE);
                            qtySum = BigDecimal.ZERO;
                            qtySumEquation = null;
                            if(ctx != null) {
                                ctx.close();
                            }
                        }
                    });
            stockAddAlert.setNegativeButton("Menu",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //dismiss the dialog
                            new setIntentAsyncTask().execute("Menu", stockID, "");
                        }
                    });
            stockAddAlert.setCancelable(true);
            stockAddAlert.create().show();

            if (LoadingDialog != null) {
                LoadingDialog.dismiss();
            }
        }
    }

    public Boolean checkIfFieldsEmpty() {
        Boolean emptyFields = false;
        if (article_textEdit.getText().toString().isEmpty()) {
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak artykułu!", "Proszę wprowadzić artykuł.", "OK",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        } else if ((qty_textEdit.getText().toString().isEmpty())) {
            if (equation_textView.getText().toString().equals("suma")) {
                emptyFields = true;
                GlobalClass.showDialog(this, "Brak ilości!", "Proszę wprowadzić ilość.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            }else {
                qty_textEdit.setText(qty_textEdit.getHint().toString());
            }
            if (location_textEdit.getText().toString().isEmpty()) {
                if (location_textEdit.getHint().toString().equals("Lokalizacja")) {
                    emptyFields = true;
                    GlobalClass.showDialog(this, "Brak lokalizacji!", "Proszę wprowadzić lokalizację.", "OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                } else {
                    location_textEdit.setText(location_textEdit.getHint().toString());
                }
            }
        } else if (location_textEdit.getText().toString().isEmpty()) {
            if (location_textEdit.getHint().toString().equals("Lokalizacja")) {
                emptyFields = true;
                GlobalClass.showDialog(this, "Brak lokalizacji!", "Proszę wprowadzić lokalizację.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            } else {
                location_textEdit.setText(location_textEdit.getHint().toString());
            }
        }
        return emptyFields;
    }

    public void enterStockRow(){
        stocktaking = getStocktaing();
        StocktakingProtecEditor stocktakingProtecEditor = stocktaking.createEditor();
        try {
            stocktakingProtecEditor.open(EditorAction.UPDATE);
        } catch (CommandException e){
            Log.d("command exception", e.getMessage());
        }
        if(lockIcon.isChecked()){
            stocktakingProtecEditor.setYcurrlocation(location_textEdit.getText().toString());
        }else{
            stocktakingProtecEditor.setYcurrlocation("");
        }
        stocktakingProtecEditor.table().appendRow();
        StocktakingProtecEditor.Row row =  stocktakingProtecEditor.table().getRow(stocktakingProtecEditor.getRowCount());
        Log.d("location_textEdit", location_textEdit.getText().toString());
        Log.d("isHandWritten", isHandWritten.toString());
        Integer rowNo = stocktaking.table().getRowCount();
        row.setString("yid", rowNo.toString());
        row.setString("yarticle", article_textEdit.getText());
        row.setString("ycountedqty", qty_textEdit.getText());
        row.setString("yunit", unit.getText());
        row.setString("ystorageplace", location_textEdit.getText().toString());
        row.setYishandwritten(isHandWritten);
        row.setString("yinfo", info_textEdit.getText());
        stocktakingProtecEditor.commit();
        if(stocktakingProtecEditor.active()){
            stocktakingProtecEditor.abort();
        }
        if(ctx != null) {
            ctx.close();
        }
    }

    public StocktakingProtec getStocktaing() {
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<StocktakingProtec> stocktakingSB = SelectionBuilder.create(StocktakingProtec.class);
            stocktakingSB.add(Conditions.eq(StocktakingProtec.META.idno, stockID));
            stocktaking = QueryUtil.getFirst(ctx, stocktakingSB.build());
            if (ctx != null) {
                ctx.close();
            }
        }catch(DBRuntimeException e){
            //
        }
        return stocktaking;
    }

    public void showMyList(View view){
        LoadingDialog = ProgressDialog.show(Stocktaking.this, "",
                "Ładowanie. Proszę czekać...", true);
        new setIntentAsyncTask().execute("MyStocktakingList", stockID, "");
    }

    public void addQty(View view){
        String mathOperator = "";
        if(qty_textEdit.getText().toString().isEmpty()){
            GlobalClass.showDialog(this, "Brak ilości!", "Proszę wprowadzić ilość.", "OK",
                    new DialogInterface.OnClickListener() {@Override public void onClick(DialogInterface dialog, int which) {} });
        }else {
            BigDecimal newQty = new BigDecimal(qty_textEdit.getText().toString());
            qtySum = qtySum.add(newQty);
            if (newQty.compareTo(BigDecimal.ZERO) != -1) {
                mathOperator = "+";
            }
            if(qtySumEquation != null) {
                qtySumEquation = qtySumEquation + mathOperator + qty_textEdit.getText().toString();
            }else{
                qtySumEquation = qty_textEdit.getText().toString();
            }
            equation_textView.setVisibility(View.VISIBLE);
            equation_textView.setText(qtySumEquation);
            qty_textEdit.setHint(qtySum.toString());
            qty_textEdit.setText("");
        }
    }
}