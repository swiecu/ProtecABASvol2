package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
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
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.abas.erp.common.type.AbasDate;
import de.abas.erp.common.type.enums.EnumProcurementType;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.RowQuery;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owpl.IsMailSender;
import de.abas.erp.db.schema.custom.protec.AppConfigValues;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.purchasing.PackingSlipEditor;
import de.abas.erp.db.schema.purchasing.PurchaseOrder;
import de.abas.erp.db.schema.vendor.Vendor;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.RowSelectionBuilder;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class IncomePurchaseOrderList extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx, sessionCtx;
    String database, name, vendor, purchaseOrdersString, proof_Nr, back_article, userSwd, articlesForEmail;
    AbasDate today;
    TextView vendorNameTextView, proofNr_TextEdit;
    Vendor vendorObject;
    ProgressDialog LoadingDialog;
    GlobalClass globFunctions;
    HashMap<String, String> tableRowsHM;
    TableLayout layoutList;
    Button addArticle_btn, alertQualityControl_btn, createPZ_btn;
    Handler handler;
    AppConfigValues appConfigValues;
    Intent intent;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_purchase_order_list);
        LoadingDialog = ProgressDialog.show(IncomePurchaseOrderList.this, "",
                "Ładowanie. Proszę czekać...", true);
        getElementsFromIntent();
        getElementsById();
        setLook();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(IncomePurchaseOrderList.this, userSwd));
    }

    //na cofnięcie Back do tyłu
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(ctx != null){
            ctx.close();
        }
        new setIntentAsyncTask().execute("QualityControlIncome", "");
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        if(ctx != null) {
            ctx.close();
        }
        super.onPause();
    }

    public void getElementsFromIntent() {
        password = (getIntent().getStringExtra("password"));
        setPassword(password);
        userSwd = getIntent().getStringExtra("userSwd");
        database = (getIntent().getStringExtra("database"));
        name = (getIntent().getStringExtra("content"));
        vendor = (getIntent().getStringExtra("vendor"));
        purchaseOrdersString = (getIntent().getStringExtra("purchaseOrder"));
        proof_Nr = (getIntent().getStringExtra("proof_Nr"));
        back_article = (getIntent().getStringExtra("art_idno"));
        tableRowsHM = (HashMap<String, String>)getIntent().getSerializableExtra("tableRowsHM");
    }

    public void getElementsById(){
        vendorNameTextView = (TextView) findViewById(R.id.vendorNameTextView);
        addArticle_btn = (Button) findViewById(R.id.addArticle_btn);
        alertQualityControl_btn = (Button) findViewById(R.id.alertQualityControl_btn);
        createPZ_btn = (Button) findViewById(R.id.createPZ_btn);
        proofNr_TextEdit = (TextView) findViewById(R.id.proofNr_TextEdit);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public  void setLook(){
        today = new AbasDate();
        String vendorName = getVendor().getDescrOperLang();
        vendorNameTextView.setText(vendorName);
        proofNr_TextEdit.setText(proof_Nr);
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
        if(ctx != null){ //necessarry
            ctx.close();
        }
        if(back_article != null) {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            for (Map.Entry<String, String> entryTableRowsHM : tableRowsHM.entrySet()) {
                String productKey = entryTableRowsHM.getKey();
                String[] qtyValues = entryTableRowsHM.getValue().split("@");
                String toDeliverQty = qtyValues[0];
                String deliveredQty = qtyValues[1];
                globFunctions = new GlobalClass(getApplicationContext());

                Product article = globFunctions.FindProductBySwd(ctx, productKey);
                drawTable(null, article, toDeliverQty, deliveredQty);
            }
            if(ctx != null) {
                ctx.close();
            }
            searchArticle(back_article);
        }else{
            tableRowsHM = new HashMap<>();
            getChoosenVendorPurchaseOrders();
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if(ctx != null){
            ctx.close();
        }
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(IncomePurchaseOrderList.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = new ProgressDialog(IncomePurchaseOrderList.this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
            loadDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loadDialog.setTitle("");
            loadDialog.setMessage("Ładowanie. Proszę czekać...");
            loadDialog.show();
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
        }
    }

    public void setIntent(String destination, String content) {
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("content", content);
            intent.putExtra("destination", "IncomePurchaseOrderList");
            intent.putExtra("vendor", vendor);
            intent.putExtra("proof_Nr", proofNr_TextEdit.getText().toString());
            intent.putExtra("purchaseOrder", purchaseOrdersString);
            intent.putExtra("tableRowsHM", tableRowsHM);
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void getChoosenVendorPurchaseOrders (){    // tu coś zmienic wyświetla niepotrzebny wiersz \m401603004
        try{                                                                        //database" lub erp
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");  //zmienić pozniej na database
            RowSelectionBuilder<PurchaseOrder, PurchaseOrder.Row> purchaseOrderRowSB = RowSelectionBuilder.create(PurchaseOrder.class, PurchaseOrder.Row.class);
            purchaseOrderRowSB.addForHead(Conditions.eq(PurchaseOrder.META.vendor.toString(), vendor));
            purchaseOrderRowSB.addForHead(Conditions.eq(PurchaseOrder.META.procureMode, EnumProcurementType.ExternalProcurement));
            purchaseOrderRowSB.add(Conditions.eq(PurchaseOrder.Row.META.deadlineWeekOrDay, today));
            RowQuery<PurchaseOrder, PurchaseOrder.Row> purchaseOrderRowQuery = ctx.createQuery(purchaseOrderRowSB.build());
            for (PurchaseOrder.Row poRow : purchaseOrderRowQuery) {
                if(!((Vendor)poRow.getVendor()).getSwd().equals("WYBIERZ")){
                    if (poRow.getProduct() instanceof Product) {
                        if (poRow.getOutstDelQty().compareTo(BigDecimal.ZERO) != 0){
                            drawTable(poRow,  (Product)poRow.getProduct(), "0", "empty");
                        }
                    }
                }
            }
            if(ctx != null) {
                ctx.close();
            }
        }catch (DBRuntimeException e) {
            catchExceptionCases(e, "getChoosenVendorPurchaseOrders", "");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void searchArticle(String content) {
        globFunctions = new GlobalClass(getApplicationContext());
        try{
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            if(globFunctions.FindProductByIdno(ctx, content) != null) {  //jeśli Find by IDNO nie równa się null
                Product article = globFunctions.FindProductByIdno(ctx, content);
                if(ctx != null) {
                    ctx.close();
                }
                drawTable(null, article, "0", "empty");
                if (LoadingDialog != null) {
                    LoadingDialog.dismiss();
                }
                //jeśli nie znajdzie by IDNO
            }else if (globFunctions.FindProductByDescr(ctx, content) != null){
                if(ctx != null) {
                    ctx.close();
                }
                new setIntentAsyncTask().execute("ArticleNameList", content);

                // jeśli nie znajdzie by DESCR
            } else if (globFunctions.FindProductBySwd(ctx, content) != null) {   //jeśli Find by SWD nie równa się null
                if(ctx != null) {
                    ctx.close();
                }
                new setIntentAsyncTask().execute("ArticleNameList", content);

                // jeśli nie znajdzie ani tu ani tu
            } else {
                if(ctx != null) {
                    ctx.close();
                }
                LoadingDialog.dismiss();
                GlobalClass.showDialog(IncomePurchaseOrderList.this, "Brak artykułu!", "W bazie nie ma takeigo artykłu!", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {} });
            }
        } catch (DBRuntimeException e) {
            if(LoadingDialog != null) {LoadingDialog.dismiss(); }
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
            LoadingDialog = ProgressDialog.show(IncomePurchaseOrderList.this, "     Przekroczono liczbę licencji.",
                    "Zwalniam miejsce w ABAS. Proszę czekać...", true);
            new Thread(() -> {
                sessionCtx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", "sesje", "mobileApp");  // hasło sesje i erp aby mieć dostęp
                GlobalClass.licenceCleaner(sessionCtx);
                sessionCtx.close();
                handler.sendEmptyMessage(0);
            }).start();
            handler = new Handler() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                public void handleMessage(Message msg) {
                    LoadingDialog.dismiss();
                    if(function.equals("getChoosenVendorPurchaseOrders")){
                        getChoosenVendorPurchaseOrders();
                    }else if(function.equals("searchArticle")){
                        searchArticle(parameter);
                    }
                }
            };
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(PurchaseOrder.Row purchaseOrderRow, Product product, String toDeliver, String deliveredEnteredQty){
        String articleString;
        BigDecimal qtyToDeliverNr;
        String unitIdString, articleIdnoString = "";

        if(purchaseOrderRow != null) {
             articleString = purchaseOrderRow.getProduct().getSwd();
             articleIdnoString = purchaseOrderRow.getProduct().getIdno();
             qtyToDeliverNr = purchaseOrderRow.getOutstDelQty().stripTrailingZeros();
             unitIdString = purchaseOrderRow.getString("tradeUnit");
        }else{
            if((toDeliver.equals("--------"))) {
                qtyToDeliverNr = BigDecimal.ZERO;
            }else{
                qtyToDeliverNr = new BigDecimal(toDeliver);
            }
            articleString = product.getSwd();
            unitIdString = product.getString("SU");
            articleIdnoString = product.getIdno();
        }

        layoutList = findViewById(R.id.incomePurchaseOrderTable);
        TableRow tableRowList = new TableRow(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        tableRowList.setLayoutParams(lp);
        tableRowList.setBackgroundColor(Color.parseColor("#BDBBBB"));

        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
        params.setMargins(1, 1, 1, 1);

        TextView article = new TextView(this);
        TextView qtyToDeliver = new TextView(this);
        EditText deliveredQty = new EditText(this);
        TextView unit = new TextView(this);
        TextView articleIdno = new TextView(this);

        Integer j = layoutList.getChildCount();
        if (j % 2 == 0) {
            article.setBackgroundColor(Color.parseColor("#E5E5E6"));
            qtyToDeliver.setBackgroundColor(Color.parseColor("#E5E5E6"));
            deliveredQty.setBackgroundColor(Color.parseColor("#E5E5E6"));
            unit.setBackgroundColor(Color.parseColor("#E5E5E6"));
        } else {
            article.setBackgroundColor(Color.parseColor("#FFFFFF"));
            qtyToDeliver.setBackgroundColor(Color.parseColor("#FFFFFF"));
            deliveredQty.setBackgroundColor(Color.parseColor("#FFFFFF"));
            unit.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }

        article.setText(articleString);
        article.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        article.setTypeface(Typeface.DEFAULT_BOLD);
        article.setTextColor(Color.parseColor("#808080"));
        article.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
        article.setPadding(5, 25, 5, 25);
        article.setLayoutParams(params);

        if(qtyToDeliverNr.compareTo(BigDecimal.ZERO) !=0){
            qtyToDeliver.setText(qtyToDeliverNr.toPlainString());
        }else {
            qtyToDeliver.setText("--------");
        }
        qtyToDeliver.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        qtyToDeliver.setTextColor(Color.parseColor("#808080"));
        qtyToDeliver.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
        qtyToDeliver.setPadding(5, 25, 5, 25);
        qtyToDeliver.setLayoutParams(params);

        deliveredQty.setHint("Wypisz ilość");
        deliveredQty.setHintTextColor(Color.parseColor("#a8a8a8"));
        if(deliveredEnteredQty.equals("empty")){
            deliveredQty.setText("");
        }else {
            deliveredQty.setText(deliveredEnteredQty);
        }
        deliveredQty.setInputType(InputType.TYPE_CLASS_NUMBER);
        deliveredQty.setGravity(Gravity.CENTER);
        deliveredQty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        deliveredQty.setTextColor(Color.parseColor("#808080"));
        deliveredQty.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
        deliveredQty.setPadding(5, 25, 5, 25);
        deliveredQty.setLayoutParams(params);

        if (unitIdString.equals("(20)")) {
            unitIdString = "szt.";
        } else if (unitIdString.equals("(7)")) {
            unitIdString = "kg";
        } else if (unitIdString.equals("(21)")) {
            unitIdString = "kpl";
        } else if (unitIdString.equals("(1)")) {
            unitIdString = "m";
        }else if (unitIdString.equals("(10)")) {
            unitIdString = "tona";
        }else if (unitIdString.equals("(28)")) {
            unitIdString = "arkusz";
        }
        unit.setText(unitIdString);
        unit.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        unit.setTextColor(Color.parseColor("#808080"));
        unit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
        unit.setPadding(5, 25, 10, 25);
        unit.setLayoutParams(params);

        articleIdno.setText(articleIdnoString);
        articleIdno.setVisibility(View.GONE);

        tableRowList.addView(article);
        tableRowList.addView(qtyToDeliver);
        tableRowList.addView(deliveredQty);
        tableRowList.addView(unit);
        tableRowList.addView(articleIdno);
        layoutList.addView(tableRowList, j);
        if(ctx != null){
            ctx.close();
        }
    }

    public void enterArticle(View view){

        createTableElementsHM();
        AlertDialog.Builder enterArticleDialog = new AlertDialog.Builder(new ContextThemeWrapper(IncomePurchaseOrderList.this, R.style.MyDialog));
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_enter_article, viewGroup, false);
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
                    GlobalClass.showDialog(IncomePurchaseOrderList.this, "Brak wpisanego artykułu!", "Proszę wprowadzić artykuł.", "OK", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
                }else {
                    articleDialog.dismiss();
                    LoadingDialog = ProgressDialog.show(IncomePurchaseOrderList.this, "",
                            "Ładowanie. Proszę czekać...", true);
                    searchArticle(article_name);
                }
            }
        });
        articleDialog.show();
        articleDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void createTableElementsHM(){
        articlesForEmail = "<br/>";
        for(int i = layoutList.getChildCount(), j = 1; i >=j; i--) {
            View child = layoutList.getChildAt(i);
            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                TextView article_TextView = (TextView) row.getChildAt(0);
                TextView qtyToDeliver = (TextView) row.getChildAt(1);
                TextView qtyDelivered = (TextView) row.getChildAt(2);
                TextView unit = (TextView) row.getChildAt(3);
                Log.d("rowNR", "row"+i);
                if(!article_TextView.getText().equals("") || (!article_TextView.getText().equals("0"))){
                    articlesForEmail += "Artykuł: <b>" + article_TextView.getText().toString() + "</b><br/> Il. do dostarczenia: "
                            + qtyToDeliver.getText().toString() + " " + unit.getText().toString() + "<br/> Il. dostarczona: "
                            + qtyDelivered.getText().toString() + " " + unit.getText().toString() + "<br/><br/>";
                }
                if(!tableRowsHM.containsKey(article_TextView.getText().toString())){
                    if(qtyDelivered.getText().toString().equals("")){
                        tableRowsHM.put(article_TextView.getText().toString(), qtyToDeliver.getText().toString() + "@" + "empty");
                    }else {
                        tableRowsHM.put(article_TextView.getText().toString(), qtyToDeliver.getText().toString() + "@" + qtyDelivered.getText().toString());
                    }
                }
            }
        }
    }


    public void alertQualityControl(View view){
        GlobalClass.showDialogTwoButtons(this, "Powiadomienie do Kontroli Jakości!", "Czy napewno chcesz wysłać powiadomienie do kontroli jakości?", "Wyślij", "Anuluj",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // wysłanie e-maila
                       DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                       Date todayDate = new Date();
                       String text = ("Dzień dobry! <br/><br/>Dostawa z dnia " + dateFormat.format(todayDate) + " od dostawcy <b>" + vendorNameTextView.getText().toString()
                              + "</b> czeka na kontrolę jakości. </p> Powiadomienie wysłane przez użytkownika: " + userSwd + ".");
                       sendEmail("yqualityincome", "Wezwanie do kontroli jakosci dostawy!", text);

                        GlobalClass.showDialog(IncomePurchaseOrderList.this, "Wysłano!", "Wiadomość została pomyślnie wysłana.", "OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                        if(ctx != null) {
                            ctx.close();
                        }
                    }
                }, new DialogInterface.OnClickListener() { //Anuluj button
                    @Override public void onClick(DialogInterface dialogInterface, int i) { } });
    }

    public void createPZ(View view){
        GlobalClass.showDialogTwoButtons(this, "Tworzenie PZ", "Czy napewno chcesz utworzyć PZ z artykułami w tabelce?", "Utwórz", "Anuluj",
         new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LoadingDialog = new ProgressDialog(IncomePurchaseOrderList.this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
                LoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                LoadingDialog.setTitle("");
                LoadingDialog.setMessage("Ładowanie. Proszę czekać...");
                LoadingDialog.show();
                Integer abasRowCount = 0;
                createTableElementsHM();
                Boolean emptyFields = checkIfFieldsEmpty();
                String purchaseOrderStringFormEmail = "";
                today = new AbasDate();
                if (emptyFields == false) {
                    ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
                    String[] purchaseOrders = purchaseOrdersString.split("@");
                    int nrOfPurchaseOrders = purchaseOrders.length;
                    PackingSlipEditor packingSlipEditor = (PackingSlipEditor) ctx.newObject(PackingSlipEditor.class);
                    for (int i = 0; i < nrOfPurchaseOrders; i++){
                        Log.d("PURCHASE ORDER NUMBER", purchaseOrders[i]);
                        packingSlipEditor.setString("docno", purchaseOrders[i]);
                        purchaseOrderStringFormEmail += purchaseOrders[i] + ", ";
                    }
                    Iterable<PackingSlipEditor.Row> slipRows = packingSlipEditor.table().getEditableRows();

                    //delete from HM if it exists in ABAS and add qty to row
                    Iterator<PackingSlipEditor.Row> iter = slipRows.iterator();
                    while(iter.hasNext()){
                        Iterator<Map.Entry<String, String>> iterHM = tableRowsHM.entrySet().iterator();
                        PackingSlipEditor.Row row = iter.next();
                        while(iterHM.hasNext()){
                            Map.Entry entryTableRowsHM = iterHM.next();
                            String product = entryTableRowsHM.getKey().toString();
                            String[] qtyValues = entryTableRowsHM.getValue().toString().split("@");
                            String deliveredQty = qtyValues[1];
                            if (row.getProduct().getSwd().equals(entryTableRowsHM.getKey())) {
                                if(row.getCtryOfOrigin() == null){
                                    row.setCtryOfOrigin(row.getDestDispatchCtryPos());
                                }
                                if(!deliveredQty.equals("0.000")){
                                    row.setString("unitqty", deliveredQty);
                                }else{
                                    row.setUnitQty(BigDecimal.ZERO);
                                }
                                iterHM.remove();
                            }
                        }
                    }


                    //enter other products
                    for(Map.Entry<String, String> entryTableRowsHM : tableRowsHM.entrySet()){
                        String product = entryTableRowsHM.getKey();
                        String[] qtyValues = entryTableRowsHM.getValue().split("@");
                        String deliveredQty = qtyValues[1];
                        packingSlipEditor.table().appendRow();
                        abasRowCount = packingSlipEditor.table().getRowCount();
                        PackingSlipEditor.Row newRow = packingSlipEditor.table().getRow(abasRowCount);
                        newRow.setString("product", product);
                        newRow.setDeadlineWeekOrDay(today);
                        if(!deliveredQty.equals("0.000")) {
                            newRow.setString("unitqty", deliveredQty);
                        }else{
                            newRow.setUnitQty(BigDecimal.ZERO);
                        }
                    }
                    packingSlipEditor.setDateFrom(today);
                    packingSlipEditor.setYdeliverydate(today);
                    packingSlipEditor.setExtDocNo(proofNr_TextEdit.getText().toString());


                    //check how many rows have qty set 0
                    Integer rowCount = 0;
                    Integer qtyEqualToZero = 0;
                    Iterable<PackingSlipEditor.Row> slipCountRows = packingSlipEditor.table().getEditableRows();
                    for (PackingSlipEditor.Row row: slipCountRows){
                        rowCount++;
                        if((row.getUnitQty() == null) || row.getUnitQty().toString().equals("0.000")){
                            qtyEqualToZero++;
                        }
                    }

                    //check if all qty is set 0
                    if (rowCount != qtyEqualToZero){
                        packingSlipEditor.commit();
                        if (packingSlipEditor.active()) {
                            packingSlipEditor.abort();
                        }
                        if(ctx != null){
                            ctx.close();
                        }
                        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                        Date todayDate = new Date();
                        String text = ("Dzień dobry! <br/><br/> W dniu " + dateFormat.format(todayDate) + " została utworzona nowa PZ dla dostawcy <b> "
                                + vendorNameTextView.getText().toString()+ "</b> , numer zamówienia: <b>" + purchaseOrderStringFormEmail +
                                "</b> </p> " + articlesForEmail);
                        sendEmail("yincomecreatepz", "Utworzono nowy dowod przyjecia PZ!", text);

                        GlobalClass.showDialog(IncomePurchaseOrderList.this, "Utworzono!", "Nowa PZ została pomyślnie utworzona.", "OK", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                if(LoadingDialog != null) {
                                    LoadingDialog.dismiss();
                                }
                                new setIntentAsyncTask().execute("Menu", "");}
                        });
                    }else{
                        if(LoadingDialog != null) {
                            LoadingDialog.dismiss();
                        }
                        GlobalClass.showDialog(IncomePurchaseOrderList.this, "Nie utworzono PZ!", "Nowa PZ nie została utworzona, ponieważ wszystkie artykuły mają ilość dostawy równą 0.", "OK", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {} });
                    }

                }else{
                    tableRowsHM.clear();
                }
            }
        }, new DialogInterface.OnClickListener() { //Anuluj button
            @Override public void onClick(DialogInterface dialogInterface, int i) { } });
    }

    public Boolean checkIfFieldsEmpty(){
        Boolean emptyFields= false;
        if (proofNr_TextEdit.getText().toString().equals("")) {
            emptyFields = true;
            GlobalClass.showDialog(IncomePurchaseOrderList.this, "Brak numeru dowodu!", "Proszę uzupełnić numer dowodu.", "OK", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {} });
        }

        for (Map.Entry<String, String> entryTableRowsHM : tableRowsHM.entrySet()) {
            String[] qtyValues = entryTableRowsHM.getValue().split("@");
            String deliveredQty = qtyValues[1];
            if (!proofNr_TextEdit.getText().toString().equals("")) {
                if (deliveredQty.equals("empty")) {
                    emptyFields = true;
                    if(LoadingDialog != null){
                        LoadingDialog.dismiss();
                    }
                    GlobalClass.showDialog(IncomePurchaseOrderList.this, "Brak ilości dostarczonej!",
                            "Proszę uzupełnić ilość. Jeśli dany artykuł nie przyjechał, proszę w polu 'Il.dostarczona' wpisać wartość 0.", "OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    break;
                }
            }
        }
        return emptyFields;
    }

    public void sendEmail(String emailValues, String subject, String text){
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        appConfigValues = getAppConfigValues(ctx); //podbieranie maili z abasa
        IsMailSender sender = ctx.openInfosystem(IsMailSender.class);
        sender.setYto(appConfigValues.getString(emailValues));
        sender.setYsubject(subject);
        sender.setYtrext(text);
        sender.invokeStart();
        sender.close();
        if(ctx != null) {
            ctx.close();
        }
    }
    public Vendor getVendor(){
        vendorObject = null;
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        SelectionBuilder<Vendor> stocktakingSB = SelectionBuilder.create(Vendor.class);
        stocktakingSB.add(Conditions.eq(Vendor.META.id.toString(), vendor));
        vendorObject = QueryUtil.getFirst(ctx, stocktakingSB.build());
        if(ctx != null) {
            ctx.close();
        }
        return vendorObject;
    }

    public AppConfigValues getAppConfigValues(DbContext ctx) {
        SelectionBuilder<AppConfigValues> stocktakingSB = SelectionBuilder.create(AppConfigValues.class);
        stocktakingSB.add(Conditions.eq(AppConfigValues.META.swd, "OGOLNE"));
        appConfigValues = QueryUtil.getFirst(ctx, stocktakingSB.build());
        if(ctx != null) {
            ctx.close();
        }
        return appConfigValues;
    }
}