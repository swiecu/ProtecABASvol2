package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    DbContext ctx, sessionCtx; AbasDate today;
    String database, name, vendor, purchaseOrdersString, proof_Nr, back_article, userSwd, articlesForEmail, docQty;
    TextView vendorNameTextView;
    EditText docQty_TextEdit, proofNr_TextEdit; Vendor vendorObject;
    ProgressDialog LoadingDialog; GlobalClass globFunctions;
    HashMap<String, String> tableRowsHM; TableLayout layoutList;
    Button addArticle_btn, alertQualityControl_btn, createPZ_btn;
    Handler handler; AppConfigValues appConfigValues;
    Intent intent; Boolean oneDocument = false;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getElementsFromIntent();
        if(new BigDecimal(docQty).compareTo(BigDecimal.ONE) == 0){ //docQty == 1
            setContentView(R.layout.activity_income_purchase_order_list_for_1);
            oneDocument = true;
        }else{
            setContentView(R.layout.activity_income_purchase_order_list_more_than_1);
        }
        LoadingDialog = ProgressDialog.show(IncomePurchaseOrderList.this, "",
                "Ładowanie. Proszę czekać...", true);
        getElementsById();
        setLook();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(IncomePurchaseOrderList.this, userSwd));
    }

    //na cofnięcie Back do tyłu
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        GlobalClass.ctxClose(ctx);
        new setIntentAsyncTask().execute("QualityControlIncome", "");
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        GlobalClass.ctxClose(ctx);
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
        docQty = getIntent().getStringExtra("docQty");
    }

    public void getElementsById(){
        vendorNameTextView = (TextView) findViewById(R.id.vendorNameTextView);
        addArticle_btn = (Button) findViewById(R.id.addArticle_btn);
        alertQualityControl_btn = (Button) findViewById(R.id.alertQualityControl_btn);
        createPZ_btn = (Button) findViewById(R.id.createPZ_btn);
        proofNr_TextEdit = (EditText) findViewById(R.id.proofNr_TextEdit);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public  void setLook(){
        today = new AbasDate();
        String vendorName = getVendor().getDescrOperLang();
        vendorNameTextView.setText(vendorName);
        if(oneDocument == true) {
            proofNr_TextEdit.setText(proof_Nr);
        }
        GlobalClass.dismissLoadingDialog(LoadingDialog);
        GlobalClass.ctxClose(ctx);
        if(back_article != null) {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            for (Map.Entry<String, String> entryTableRowsHM : tableRowsHM.entrySet()) {

                String productKey = entryTableRowsHM.getKey();
                String[] qtyValues = entryTableRowsHM.getValue().split("@");
                String toDeliverQty = qtyValues[0], deliveredQty = qtyValues[1];
                Log.d("productKey", productKey);
                Log.d("toDeliver", toDeliverQty);
                Log.d("deliveredQty", deliveredQty);
                Product article = GlobalClass.FindProductBySwd(ctx, productKey);
                Log.d("product getBySwd", article.getSwd());
                if(oneDocument == true){
                    drawTableForOneDocument(null, article, toDeliverQty, deliveredQty);
                }else{
                    drawTableForMoreThanOneDocument(null, article, toDeliverQty, deliveredQty);
                }
            }
            GlobalClass.ctxClose(ctx);
            searchArticle(back_article);
        }else{
            tableRowsHM = new HashMap<>();
            getChoosenVendorPurchaseOrders();
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        GlobalClass.ctxClose(ctx);
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
            if(oneDocument == true){
                intent.putExtra("proof_Nr", proofNr_TextEdit.getText().toString());
            }
            intent.putExtra("docQty", docQty);
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
                            if(oneDocument == true){
                                drawTableForOneDocument(poRow, (Product)poRow.getProduct(), "0", "empty");
                            }else{
                                drawTableForMoreThanOneDocument(poRow, (Product)poRow.getProduct(), "0", "empty");
                            }

                        }
                    }
                }
            }
            GlobalClass.ctxClose(ctx);
        }catch (DBRuntimeException e) {
            catchExceptionCases(e, "getChoosenVendorPurchaseOrders", "");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void searchArticle(String content) {
        globFunctions = new GlobalClass(getApplicationContext());
        try{
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            if(globFunctions.FindProductByIdno(ctx, content) != null) {
                Product article = globFunctions.FindProductByIdno(ctx, content);
                GlobalClass.ctxClose(ctx);
                if(oneDocument == true){
                    drawTableForOneDocument(null, article, "0", "empty");
                }else{
                    drawTableForMoreThanOneDocument(null, article, "0", "empty");
                }

                GlobalClass.dismissLoadingDialog(LoadingDialog);

                //jeśli nie znajdzie by IDNO
            }else if (globFunctions.FindProductByDescr(ctx, content) != null){
                GlobalClass.ctxClose(ctx);
                new setIntentAsyncTask().execute("ArticleNameList", content);

                // jeśli nie znajdzie by DESCR
            } else if (globFunctions.FindProductBySwd(ctx, content) != null) {
                GlobalClass.ctxClose(ctx);
                new setIntentAsyncTask().execute("ArticleNameList", content);

                // jeśli nie znajdzie ani tu ani tu
            } else {
                GlobalClass.ctxClose(ctx);
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                GlobalClass.showDialog(IncomePurchaseOrderList.this, "Brak artykułu!", "W bazie nie ma takeigo artykłu!", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {} });
            }
        } catch (DBRuntimeException e) {
            GlobalClass.dismissLoadingDialog(LoadingDialog);
            catchExceptionCases(e, "searchArticle", content);
        }
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function, String parameter){
        GlobalClass.catchExceptionCases(e, this);

        //przekroczona liczba licencji
        if (e.getMessage().contains("FULL")) {
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
                    if(function.equals("getChoosenVendorPurchaseOrders")){
                        getChoosenVendorPurchaseOrders();
                    }else if(function.equals("searchArticle")){
                        searchArticle(parameter);
                    }else if(function.equals("createPZInABAS")){
                        createPZInABAS();
                    }
                }
            };
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTableForOneDocument(PurchaseOrder.Row purchaseOrderRow, Product product, String toDeliver, String deliveredEnteredQty){
        String articleString, unitIdString, articleIdnoString = "";
        BigDecimal qtyToDeliverNr;
        Log.d("product", product.toString());
        if(purchaseOrderRow != null) {
            articleString = purchaseOrderRow.getProduct().getSwd();
            articleIdnoString = purchaseOrderRow.getProduct().getIdno();
            qtyToDeliverNr = purchaseOrderRow.getOutstDelQty().stripTrailingZeros();
            unitIdString = purchaseOrderRow.getString("tradeUnit");
        }else{
            if(toDeliver.equals("--------")) {
                qtyToDeliverNr = BigDecimal.ZERO;
            }else{
                qtyToDeliverNr = new BigDecimal(toDeliver);
            }
            articleString = product.getSwd();
            unitIdString = product.getString("SU");
            articleIdnoString = product.getIdno();
        }

        layoutList = findViewById(R.id.incomePurchaseOrderTable);
        TableRow tableRowList = GlobalClass.setTableRowList(this);
        TextView article = new TextView(this), qtyToDeliver = new TextView(this), unit = new TextView(this), articleIdno = new TextView(this);
        EditText deliveredQty = new EditText(this);

        Integer j = layoutList.getChildCount();
        TextView[] textViewArray = {article, qtyToDeliver, deliveredQty, unit, articleIdno};
        for (TextView textView :textViewArray) {
            if (j % 2 == 0) {
                textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
            } else {
                textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }
        //article
        GlobalClass.setParamForTextView(article, articleString, 14, 25, 5, true);

        //qtyToDeliver
        String qtyToDelivertText;
        if(qtyToDeliverNr.compareTo(BigDecimal.ZERO) !=0){
            qtyToDelivertText = qtyToDeliverNr.toPlainString();
        }else {
            qtyToDelivertText = "--------";
        }
        GlobalClass.setParamForTextView(qtyToDeliver, qtyToDelivertText, 14, 25, 5, false);

        //deliveredQty
        String deliveredQtyText;
        if(deliveredEnteredQty.equals("empty")){
            deliveredQtyText = "";
        }else {
            deliveredQtyText = deliveredEnteredQty;
        }
        deliveredQty.setHint("Wypisz ilość");
        deliveredQty.setHintTextColor(Color.parseColor("#a8a8a8"));
        deliveredQty.setInputType(InputType.TYPE_CLASS_NUMBER);
        deliveredQty.setGravity(Gravity.CENTER);
        GlobalClass.setParamForTextView(deliveredQty, deliveredQtyText, 14, 25, 5, false);

        //unit
        unitIdString = GlobalClass.getProperUnit(unitIdString);
        GlobalClass.setParamForTextView(unit, unitIdString, 14, 25, 10, false);

        //articleIdno
        articleIdno.setText(articleIdnoString);
        articleIdno.setVisibility(View.GONE);

        for (TextView textView :textViewArray) {
            tableRowList.addView(textView);
        }
        layoutList.addView(tableRowList, j);
        GlobalClass.ctxClose(ctx);
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTableForMoreThanOneDocument(PurchaseOrder.Row purchaseOrderRow, Product product, String toDeliver, String deliveredEnteredQty){
        String articleString, unitIdString, articleIdnoString = "";
        BigDecimal qtyToDeliverNr;

        if(purchaseOrderRow != null) {
            articleString = purchaseOrderRow.getProduct().getSwd();
            articleIdnoString = purchaseOrderRow.getProduct().getIdno();
            qtyToDeliverNr = purchaseOrderRow.getOutstDelQty().stripTrailingZeros();
            unitIdString = purchaseOrderRow.getString("tradeUnit");
        }else{
            if(toDeliver.equals("--------")) {
                qtyToDeliverNr = BigDecimal.ZERO;
            }else{
                qtyToDeliverNr = new BigDecimal(toDeliver);
            }
            articleString = product.getSwd();
            unitIdString = product.getString("SU");
            articleIdnoString = product.getIdno();
        }

        layoutList = findViewById(R.id.incomePurchaseOrderTable);
        TableRow tableRowList = GlobalClass.setTableRowList(this);
        TextView article = new TextView(this), qtyToDeliver = new TextView(this), unit = new TextView(this), articleIdno = new TextView(this);
        CheckBox isDelivered = new CheckBox(this);

        Integer j = layoutList.getChildCount();
        TextView[] textViewArray = {article, qtyToDeliver, unit, isDelivered};
        for (TextView textView :textViewArray) {
            if (j % 2 == 0) {
                textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
            } else {
                textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }
        //article
        GlobalClass.setParamForTextView(article, articleString, 14, 25, 5, true);

        //qtyToDeliver
        String qtyToDelivertText;
        if(qtyToDeliverNr.compareTo(BigDecimal.ZERO) !=0){
            qtyToDelivertText = qtyToDeliverNr.toPlainString();
        }else {
            qtyToDelivertText = "--------";
        }
        GlobalClass.setParamForTextView(qtyToDeliver, qtyToDelivertText, 14, 25, 5, false);

        //unit
        unitIdString = GlobalClass.getProperUnit(unitIdString);
        GlobalClass.setParamForTextView(unit, unitIdString, 14, 25, 5, false);

        //is delivered checkbox
        isDelivered.setGravity(Gravity.CENTER);
        isDelivered.setChecked(false);
        isDelivered.setButtonTintList(ColorStateList.valueOf(Color.parseColor(("#7580BC"))));
        GlobalClass.setParamForTextView(isDelivered, "", 14, 25, 30, false);

        //articleIdno
        articleIdno.setText(articleIdnoString);
        articleIdno.setVisibility(View.GONE);

        for (TextView textView :textViewArray) {
            tableRowList.addView(textView);
        }
        layoutList.addView(tableRowList, j);
        GlobalClass.ctxClose(ctx);
    }

    public void enterArticle(View view){
        createTableElementsHM(false);
        AlertDialog.Builder enterArticleDialog = new AlertDialog.Builder(new ContextThemeWrapper(IncomePurchaseOrderList.this, R.style.MyDialog));
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_enter_article, viewGroup, false);
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

    public void createTableElementsHM(Boolean hashMapForCreatingPZ){
        tableRowsHM.clear();
        articlesForEmail = "<br/>";
        for(int i = layoutList.getChildCount(), j = 1; i >=j; i--) {
            View child = layoutList.getChildAt(i);
            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                TextView article_TextView = (TextView) row.getChildAt(0), qtyToDeliver = (TextView) row.getChildAt(1);
                if(oneDocument == true){
                    TextView qtyDelivered = (TextView) row.getChildAt(2), unit = (TextView) row.getChildAt(3);
                    if(!qtyDelivered.getText().equals("") || (!qtyDelivered.getText().equals("0"))){
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
                }else{
                    TextView unit = (TextView) row.getChildAt(2);
                    CheckBox isDelivered = (CheckBox) row.getChildAt(3);
                    if(isDelivered.isChecked()){
                        articlesForEmail += "Artykuł: <b>" + article_TextView.getText().toString() + "</b><br/> Il. do dostarczenia: "
                                + qtyToDeliver.getText().toString() + " " + unit.getText().toString() + "<br/><br/>";
                    }
                    if(!tableRowsHM.containsKey(article_TextView.getText().toString())){
                        if(hashMapForCreatingPZ){
                            if(isDelivered.isChecked()){
                                tableRowsHM.put(article_TextView.getText().toString(), qtyToDeliver.getText().toString() + "@" + "true");
                            }else{
                                tableRowsHM.put(article_TextView.getText().toString(), qtyToDeliver.getText().toString() + "@" + "false");
                            }
                        }else{
                            tableRowsHM.put(article_TextView.getText().toString(), qtyToDeliver.getText().toString() + "@" + "empty");
                        }
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
                        GlobalClass.ctxClose(ctx);
                    }
                }, new DialogInterface.OnClickListener() { //Anuluj button
                    @Override public void onClick(DialogInterface dialogInterface, int i) { } });
    }

    public void createPZ(View view){

        GlobalClass.showDialogTwoButtons(this, "Tworzenie PZ", "Czy napewno chcesz utworzyć PZ z artykułami w tabelce?", "Utwórz", "Anuluj",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        createTableElementsHM(true);
                        Boolean emptyFields = checkIfFieldsEmpty();
                        today = new AbasDate();
                        if (emptyFields == false) {
                            createPZInABAS();
                        }else{
                            tableRowsHM.clear();
                        }
                    }
                }, new DialogInterface.OnClickListener() { //Anuluj button
                    @Override public void onClick(DialogInterface dialogInterface, int i) {} });
    }

    public void createPZInABAS(){
        Integer docCount = 0;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        }catch (DBRuntimeException e) {
            GlobalClass.dismissLoadingDialog(LoadingDialog);
            catchExceptionCases(e, "createPZInABAS", "");
        }
        for(int i=0;  i<Integer.parseInt(docQty); i++) {
            createTableElementsHM(true);
            PackingSlipEditor packingSlipEditor = (PackingSlipEditor) ctx.newObject(PackingSlipEditor.class);
            docCount++;
            String purchaseOrdersStringForEmail = setNewPurchaseOrderFields(packingSlipEditor);
            deleteArticleFromHMIfExistsInABASAndAddQtyToRow(packingSlipEditor);
            enterOtherProductsIntoPZ(packingSlipEditor);
            checkIfAllRowsAreNotSetToZero(packingSlipEditor, docCount, purchaseOrdersStringForEmail);

            if (packingSlipEditor.active()) {
                packingSlipEditor.abort();
            }
        }
        GlobalClass.ctxClose(ctx);
    }
    public void deleteArticleFromHMIfExistsInABASAndAddQtyToRow(PackingSlipEditor packingSlipEditor){
        Iterable<PackingSlipEditor.Row> slipRows = packingSlipEditor.table().getEditableRows();

        for (PackingSlipEditor.Row row : slipRows) {
            Iterator<Map.Entry<String, String>> iterHM = tableRowsHM.entrySet().iterator();
            while (iterHM.hasNext()) {
                Map.Entry entryTableRowsHM = iterHM.next();
                String product = entryTableRowsHM.getKey().toString();
                String[] qtyValues = entryTableRowsHM.getValue().toString().split("@");
                String toDeliverQty = qtyValues[0], deliveredQty="", isChecked="";
                if(oneDocument == true){
                    deliveredQty = qtyValues[1];
                }else{
                    isChecked = qtyValues[1];
                }

                if (row.getProduct().getSwd().equals(product)){
                    //add Qty to Row
                    if(oneDocument == true){
                        if (!deliveredQty.equals("0.000")) {
                            row.setString("unitqty", deliveredQty);
                        } else {
                            row.setUnitQty(BigDecimal.ZERO);
                        }
                    }else{
                        if(!isChecked.equals("false")){
                            BigDecimal qtyPerDocument = new BigDecimal(toDeliverQty).divide(new BigDecimal(docQty), 2, RoundingMode.HALF_UP); // toDeliverQty/documentQty (for each document the same value)
                            row.setString("unitqty", qtyPerDocument.toString());
                        }
                    }
                    iterHM.remove();
                }
            }
        }
    }

    public void enterOtherProductsIntoPZ(PackingSlipEditor packingSlipEditor){
        Integer abasRowCount;
        for (Map.Entry<String, String> entryTableRowsHM : tableRowsHM.entrySet()) {
            String[] qtyValues = entryTableRowsHM.getValue().split("@");
            String deliveredQty = qtyValues[1], toDeliverQty = qtyValues[0], product = entryTableRowsHM.getKey();
            packingSlipEditor.table().appendRow();
            abasRowCount = packingSlipEditor.table().getRowCount();
            PackingSlipEditor.Row newRow = packingSlipEditor.table().getRow(abasRowCount);
            newRow.setString("product", product);
            newRow.setDeadlineWeekOrDay(today);
            if(oneDocument == true) {
                if (!deliveredQty.equals("0.000")) {
                    newRow.setString("unitqty", deliveredQty);
                } else {
                    newRow.setUnitQty(BigDecimal.ZERO);
                }
            }else{
                if(toDeliverQty.equals("--------")){
                    newRow.setString("unitqty", "1");  //one because there is no qty to deliver and value 0 could cause problems
                }else{
                    BigDecimal qtyPerDocument = new BigDecimal(toDeliverQty).divide(new BigDecimal(docQty), 2, RoundingMode.HALF_UP);
                    newRow.setString("unitqty", qtyPerDocument.toPlainString());
                }
            }
        }
    }

    public String setNewPurchaseOrderFields(PackingSlipEditor packingSlipEditor){
        String[] purchaseOrders = purchaseOrdersString.split("@");
        int nrOfPurchaseOrders = purchaseOrders.length; String purchaseOrderStringForEmail = "";

        //set docNo in Abas to import rows
        for (int j = 0; j < nrOfPurchaseOrders; j++) {
            Log.d("docNo", purchaseOrders[j]);
            packingSlipEditor.setString("docno", purchaseOrders[j]);
            Log.d("purchaseOrders[j]", purchaseOrders[j]);
            purchaseOrderStringForEmail += purchaseOrders[j] + ", ";
        }
        packingSlipEditor.setDateFrom(today);
        packingSlipEditor.setYdeliverydate(today);
        packingSlipEditor.setString("vendor", vendor);
        if (oneDocument == true) {
            packingSlipEditor.setExtDocNo(proofNr_TextEdit.getText().toString());
        } else {
            packingSlipEditor.setExtDocNo("UZUPELNIJ");
        }
        return  purchaseOrderStringForEmail;
    }

    public void checkIfAllRowsAreNotSetToZero(PackingSlipEditor packingSlipEditor, Integer docCount, String purchaseOrderStringForEmail){
        //check how many rows have qty set 0
        Integer rowCount = 0, qtyEqualToZero = 0; Boolean intrastatSetCorreclly = false;
        Iterable<PackingSlipEditor.Row> slipCountRows = packingSlipEditor.table().getEditableRows();
        for (PackingSlipEditor.Row row : slipCountRows) {
            if(!row.getProduct().getSwd().equals("TR.")){ //not counting seperator row
                rowCount++;
                if ((row.getUnitQty() == null) || row.getUnitQty().toString().equals("0.000")) {
                    qtyEqualToZero++;
                }
                //Intrastat
                Log.d("row.getDestDispatch", row.getDestDispatchCtryPos().getSwd());
                if (row.getCtryOfOrigin() == null) {
                    row.setCtryOfOrigin(row.getDestDispatchCtryPos());
                    Log.d("set new CtryOfOrigin:", row.getCtryOfOrigin().getSwd());
                }
                if (row.getRegIntra() == null) {
                    row.setString("regIntra", "OPOLSKIE");
                    Log.d("set new regintra:", "OPOLSKIE");
                }
                if (row.getNatureBusiness() == null) {
                    row.setString("naturebusiness", "RT11");
                    Log.d("set new naturebusiness:", "RT11");
                }
            }
        }

        //check if all qty is not set 0
        if (rowCount != qtyEqualToZero) {
            packingSlipEditor.commit();
            if (docCount == 1) {
                DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                Date todayDate = new Date();
                String text = ("Dzień dobry! <br/><br/> W dniu " + dateFormat.format(todayDate) + " została utworzona nowa PZ dla dostawcy <b> "
                        + vendorNameTextView.getText().toString() + "</b> , numer zamówienia: <b>" + purchaseOrderStringForEmail +
                        "</b> </p> " + articlesForEmail);
                GlobalClass.ctxClose(ctx);
                sendEmail("yincomecreatepz", "Utworzono nowy dowod przyjecia PZ!", text);

                GlobalClass.showDialog(IncomePurchaseOrderList.this, "Utworzono!", "Nowa PZ została pomyślnie utworzona.", "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GlobalClass.dismissLoadingDialog(LoadingDialog);
                        new setIntentAsyncTask().execute("Menu", "");
                    }
                });
            }
        }else {
            if (docCount == 1) {
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                GlobalClass.showDialog(IncomePurchaseOrderList.this, "Nie utworzono PZ!", "Nowa PZ nie została utworzona, ponieważ wszystkie artykuły mają ilość dostawy równą 0.", "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
            }
        }

    }

    public Boolean checkIfFieldsEmpty(){
        Boolean emptyFields= false; Integer entryCount=0, notCheckedCount = 0;
        if(oneDocument == true){
            if (proofNr_TextEdit.getText().toString().equals("")) {
                emptyFields = true;
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                GlobalClass.showDialog(IncomePurchaseOrderList.this, "Brak numeru dowodu!", "Proszę uzupełnić numer dowodu.", "OK", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {} });
            }else{
                for (Map.Entry<String, String> entryTableRowsHM : tableRowsHM.entrySet()) {
                    entryCount++;
                    String[] qtyValues = entryTableRowsHM.getValue().split("@");
                    String deliveredQty = qtyValues[1];
                    if (deliveredQty.equals("empty")) {
                        emptyFields = true;
                        GlobalClass.dismissLoadingDialog(LoadingDialog);
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
        }else{
            for (Map.Entry<String, String> entryTableRowsHM : tableRowsHM.entrySet()) {
                entryCount++;
                String[] qtyValues = entryTableRowsHM.getValue().split("@");
                String isChecked = qtyValues[1];
                if (isChecked.equals("false")) {
                    notCheckedCount++;
                }
            }
            if(entryCount == notCheckedCount){
                emptyFields = true;
                GlobalClass.dismissLoadingDialog(LoadingDialog);
                GlobalClass.showDialog(IncomePurchaseOrderList.this, "Nie zaznaczono artykułu!",
                        "Proszę zaznaczyć dostarczone artykuły.", "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
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
        GlobalClass.ctxClose(ctx);
    }

    public Vendor getVendor(){
        vendorObject = null;
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        SelectionBuilder<Vendor> stocktakingSB = SelectionBuilder.create(Vendor.class);
        stocktakingSB.add(Conditions.eq(Vendor.META.id.toString(), vendor));
        vendorObject = QueryUtil.getFirst(ctx, stocktakingSB.build());
        GlobalClass.ctxClose(ctx);
        return vendorObject;
    }

    public AppConfigValues getAppConfigValues(DbContext ctx) {
        SelectionBuilder<AppConfigValues> stocktakingSB = SelectionBuilder.create(AppConfigValues.class);
        stocktakingSB.add(Conditions.eq(AppConfigValues.META.swd, "OGOLNE"));
        appConfigValues = QueryUtil.getFirst(ctx, stocktakingSB.build());
        GlobalClass.ctxClose(ctx);
        return appConfigValues;
    }
}