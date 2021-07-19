package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.abas.erp.common.type.AbasDate;
import de.abas.erp.common.type.enums.EnumProcurementType;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.RowQuery;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.purchasing.PurchaseOrder;
import de.abas.erp.db.schema.purchasing.Purchasing;
import de.abas.erp.db.schema.vendor.Vendor;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.Order;
import de.abas.erp.db.selection.RowSelectionBuilder;
import de.abas.erp.db.util.ContextHelper;

public class QualityControlIncome extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    String database, user, userSwd;
    DbContext ctx, sessionCtx; ProgressDialog LoadingDialog;
    AbasDate today; HashMap<Vendor, String> vendorsHM;
    Handler handler; Intent intent;
    EditText docQty_TextEdit;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality_control_income);
        getElementsFromIntent();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(QualityControlIncome.this, userSwd));
        getTableElements();
    }

    // na kliknięcie cofnij
    @Override
    public void onBackPressed() {
        GlobalClass.ctxClose(ctx);
        GlobalClass.dismissLoadingDialog(LoadingDialog);
        new setIntentAsyncTask().execute("Menu", "", "", ""); //destination, vendor, purchaseOrder
        super.onBackPressed();
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        GlobalClass.ctxClose(ctx);
        GlobalClass.dismissLoadingDialog(LoadingDialog);
        super.onStop();
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
        user = (getIntent().getStringExtra("user"));
        userSwd = getIntent().getStringExtra("userSwd");
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(QualityControlIncome.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = new ProgressDialog(QualityControlIncome.this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
            loadDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loadDialog.setTitle("");
            loadDialog.setMessage("Ładowanie. Proszę czekać...");
            loadDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            String destination = strings[0],
                    vendor = strings[1],
                    purchaseOrder = strings[2],
                    docQty = strings[3];
            setIntent(destination, vendor, purchaseOrder, docQty);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
        }
    }

    public void setIntent(String destination, String vendor, String purchaseOrders, String docQty){
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("user", user);
            intent.putExtra("vendor", vendor);
            intent.putExtra("purchaseOrder", purchaseOrders);
            intent.putExtra("userSwd", userSwd);
            intent.putExtra("docQty", docQty);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void getTableElements(){
        try{
            today = new AbasDate();
            vendorsHM = new LinkedHashMap<>();                                      //database lub erp
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp"); // zmienić pozniej na database
            RowSelectionBuilder<PurchaseOrder, PurchaseOrder.Row> purchaseOrderSB = RowSelectionBuilder.create(PurchaseOrder.class, PurchaseOrder.Row.class);
            purchaseOrderSB.add(Conditions.eq(PurchaseOrder.Row.META.deadlineWeekOrDay, today));
            purchaseOrderSB.addForHead(Conditions.eq(PurchaseOrder.META.procureMode, EnumProcurementType.ExternalProcurement));
            purchaseOrderSB.addOrderForHead(Order.asc(PurchaseOrder.META.vendorDescr));
            RowQuery<PurchaseOrder, PurchaseOrder.Row> purchaseOrderRowQuery = ctx.createQuery(purchaseOrderSB.build());
            String purchaseOrdersString = "";
            List<String> purchaseOrdersList = new ArrayList<>();
            for (PurchaseOrder.Row purchaseOrderRow : purchaseOrderRowQuery) {
                if(!((Vendor)purchaseOrderRow.getVendor()).getSwd().equals("WYBIERZ")) {
                    if (purchaseOrderRow.getProduct() instanceof Product) {
                        if (purchaseOrderRow.getOutstDelQty().compareTo(BigDecimal.ZERO) != 0){
                            if (purchaseOrderRow.getDeadline() != null) {
                                Purchasing purchaseOrder = purchaseOrderRow.getHead();
                                if (!vendorsHM.containsKey((Vendor) purchaseOrderRow.getVendor())) {
                                    purchaseOrdersList.add(purchaseOrder.getIdno());
                                    purchaseOrdersString = purchaseOrder.getIdno();
                                    vendorsHM.put((Vendor) purchaseOrderRow.getVendor(), purchaseOrdersString);
                                }else{
                                    if(!purchaseOrdersList.contains(purchaseOrder.getIdno())) {
                                        purchaseOrdersList.add(purchaseOrder.getIdno()); purchaseOrdersString = purchaseOrdersString + "@" +purchaseOrder.getIdno();
                                        vendorsHM.replace((Vendor) purchaseOrderRow.getVendor(), purchaseOrdersString);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            manageVendorsHashMap();
            GlobalClass.ctxClose(ctx);

        }catch (DBRuntimeException e) {
            catchExceptionCases(e, "getTableElements", "");
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
                    if(function.equals("getTableElements")) {
                        getTableElements();
                    }
                }
            };
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void manageVendorsHashMap(){
        if(!vendorsHM.isEmpty()) {
            for (Map.Entry<Vendor, String> entryVendorsHM : vendorsHM.entrySet()) {
                Vendor vendorKey = entryVendorsHM.getKey();
                String purchaseOrderStringValue = entryVendorsHM.getValue();
                drawTable(vendorKey, purchaseOrderStringValue);
            }
        }else{
            drawEmptyTable();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(Vendor vendor, String purchaseOrdersString){
        String vendorName;
        if(!vendor.getDescrOperLang().isEmpty()) {
            vendorName = vendor.getDescrOperLang();
        }else{
            vendorName = vendor.getDescr6();
        }
        TableLayout layoutList = (TableLayout) findViewById(R.id.incomeDeliveryTable);
        TableRow tableRowList = GlobalClass.setTableRowList(this);
        TextView vendorTextView = new TextView(this);
        TextView purchasingOrderTextView = new TextView(this);

        Integer j = layoutList.getChildCount();
        if (j % 2 == 0) {
            vendorTextView.setBackgroundColor(Color.parseColor("#E5E5E6"));
        } else {
            vendorTextView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
        GlobalClass.setParamForTextView(vendorTextView, vendorName, 15, 20, 5, false);
        purchasingOrderTextView.setVisibility(View.GONE);
        purchasingOrderTextView.setText(purchaseOrdersString);

        tableRowList.addView(vendorTextView);
        tableRowList.addView(purchasingOrderTextView);
        layoutList.addView(tableRowList, j);

        tableRowList.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                showDocumentQtyDialog(purchaseOrdersString, vendor);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void showDocumentQtyDialog(String purchaseOrdersString, Vendor vendor){
        AlertDialog.Builder docQtyDialog = new AlertDialog.Builder(new ContextThemeWrapper(QualityControlIncome.this, R.style.AppTheme));
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enter_document_qty, viewGroup, false);
        docQtyDialog.setView(dialogView);
        AlertDialog docDialog = docQtyDialog.create();
        docDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
        docQty_TextEdit = docDialog.findViewById(R.id.docQty_TextEdit);

        Button button_ok = (Button)dialogView.findViewById(R.id.button_ok);
        button_ok.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v){
                docQty_TextEdit = docDialog.findViewById(R.id.docQty_TextEdit);
                if(!docQty_TextEdit.getText().toString().equals("")){
                    String vendorString = vendor.toString();
                    docDialog.dismiss();
                    new setIntentAsyncTask().execute("IncomePurchaseOrderList", vendorString, purchaseOrdersString, docQty_TextEdit.getText().toString());
                }else{
                    GlobalClass.showDialog(QualityControlIncome.this, "Brak wpisanej ilości!", "Proszę uzupełnić ilość.", "OK",
                            new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {} });
                }
            }
        });
        docDialog.show();
        docDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawEmptyTable(){
        TableLayout layoutList = (TableLayout) findViewById(R.id.incomeDeliveryTable);
        TableRow tableRowList = GlobalClass.setTableRowList(this);
        TextView noVendorsTextView = new TextView(this);
        Integer j = layoutList.getChildCount();

        GlobalClass.setParamForTextView(noVendorsTextView, "Brak dostawców na dzisiejszy dzień.", 18, 100, 5, false);
        noVendorsTextView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        tableRowList.addView(noVendorsTextView);
        layoutList.addView(tableRowList, j);
    }
}