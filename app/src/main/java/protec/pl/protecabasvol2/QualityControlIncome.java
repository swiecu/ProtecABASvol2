package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.View;
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
    DbContext ctx, sessionCtx;
    ProgressDialog LoadingDialog;
    AbasDate today;
    HashMap<Vendor, String> vendorsHM;
    Handler handler;
    Intent intent;

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
        super.onBackPressed();
        if(ctx != null){
            ctx.close();
        }
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
        new setIntentAsyncTask().execute("Menu", "", ""); //destination, vendor, purchaseOrder
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        super.onStop();
        if(ctx != null){
            ctx.close();
        }
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
            String destination = strings[0];
            String vendor = strings[1];
            String purchaseOrder = strings[2];
            setIntent(destination, vendor, purchaseOrder);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
        }
    }

    public void setIntent(String destination, String vendor, String purchaseOrders){
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("user", user);
            intent.putExtra("vendor", vendor);
            intent.putExtra("purchaseOrder", purchaseOrders);
            intent.putExtra("userSwd", userSwd);
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
            purchaseOrderSB.addOrderForHead(Order.asc(PurchaseOrder.META.vendorDesrc));
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
            if(ctx != null) {
                ctx.close();
            }

        }catch (DBRuntimeException e) {
            catchExceptionCases(e, "getTableElements", "");
        }
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function, String parameter){
        if(e.getMessage().contains("failed")){
            GlobalClass.showDialog(this,"Brak połączenia!","Nie można się aktualnie połączyć z bazą.", "OK",new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { } });

            //przekroczona liczba licencji
        }else if(e.getMessage().contains("FULL")){
            LoadingDialog = new ProgressDialog(QualityControlIncome.this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
            LoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            LoadingDialog.setTitle("     Przekroczono liczbę licencji.");
            LoadingDialog.setMessage("Zwalniam miejsce w ABAS. Proszę czekać...");
            LoadingDialog.show();
            new Thread(() -> {
                sessionCtx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", "sesje", "mobileApp");  // hasło sesje aby mieć dostęp
                GlobalClass.licenceCleaner(sessionCtx);
                sessionCtx.close();
                handler.sendEmptyMessage(0);
            }).start();
            handler = new Handler() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                public void handleMessage(Message msg) {
                    LoadingDialog.dismiss();
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
        TableRow tableRowList = new TableRow(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        tableRowList.setLayoutParams(lp);
        tableRowList.setBackgroundColor(Color.parseColor("#BDBBBB"));

        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(1, 1, 1, 1);

        TextView vendorTextView = new TextView(this);
        TextView purchasingOrderTextView = new TextView(this);

        Integer j = layoutList.getChildCount();
        if (j % 2 == 0) {
            vendorTextView.setBackgroundColor(Color.parseColor("#E5E5E6"));
        } else {
            vendorTextView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }

        vendorTextView.setText(vendorName);
        vendorTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        vendorTextView.setTextColor(Color.parseColor("#808080"));
        vendorTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f);
        vendorTextView.setPadding(5, 20, 5, 20);
        vendorTextView.setLayoutParams(params);

        purchasingOrderTextView.setVisibility(View.GONE);
        purchasingOrderTextView.setText(purchaseOrdersString);

        tableRowList.addView(vendorTextView);
        tableRowList.addView(purchasingOrderTextView);
        layoutList.addView(tableRowList, j);


        tableRowList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String vendorString = vendor.toString();
                new setIntentAsyncTask().execute("IncomePurchaseOrderList", vendorString, purchaseOrdersString);
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawEmptyTable(){

        TableLayout layoutList = (TableLayout) findViewById(R.id.incomeDeliveryTable);
        TableRow tableRowList = new TableRow(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        tableRowList.setLayoutParams(lp);
        tableRowList.setBackgroundColor(Color.parseColor("#BDBBBB"));

        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
        params.setMargins(1, 1, 1, 1);

        TextView noVendorsTextView = new TextView(this);

        Integer j = layoutList.getChildCount();
        noVendorsTextView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        noVendorsTextView.setText("Brak dostawców na dzisiejszy dzień.");
        noVendorsTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        noVendorsTextView.setTextColor(Color.parseColor("#808080"));
        noVendorsTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f);
        noVendorsTextView.setPadding(5, 100, 5, 100);
        noVendorsTextView.setLayoutParams(params);

        tableRowList.addView(noVendorsTextView);
        layoutList.addView(tableRowList, j);
    }
}