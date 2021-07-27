package protec.pl.protecabasvol2;

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
import android.util.Log;
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

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.abas.erp.common.type.enums.EnumProcurementType;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.schema.custom.protec.AppConfigValues;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.purchasing.Reservations;
import de.abas.erp.db.schema.purchasing.WorkOrderSuggestion;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class MaterialDemand extends AppCompatActivity {

    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx, sessionCtx; ProgressDialog LoadingDialog; TableLayout layout;
    TextView material_textView, article_textView, unit_textView, materialName_textView;
    EditText ZP_textEdit, qty_textEdit, comments_textEdit;
    String database, userSwd;
    Handler handler; Intent intent; Button send_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_demand);
        getElementsById();
        getElementsFromIntent();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MaterialDemand.this, userSwd));
        setPassword(password);
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
        private ProgressDialog loadDialog = new ProgressDialog(MaterialDemand.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(MaterialDemand.this, "",
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
           // intent.putExtra("destination", "StockInformation");
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void getElementsById(){
        ZP_textEdit = (EditText) findViewById(R.id.ZP_textEdit);
        article_textView = (TextView) findViewById(R.id.article_textView);
        material_textView = (TextView) findViewById(R.id.material_textView);
        unit_textView = (TextView) findViewById(R.id.unit_textView);
        qty_textEdit = (EditText) findViewById(R.id.qty_textEdit);
        comments_textEdit = (EditText) findViewById(R.id.comments_textEdit);
        send_btn = findViewById(R.id.send_btn);
        materialName_textView = findViewById(R.id.materialName_textView);
    }

    public void setLook(WorkOrderSuggestion workOrderSuggestion){
        ZP_textEdit.setText(workOrderSuggestion.getIdno());
        article_textView.setText(workOrderSuggestion.getProduct().getSwd());
        String unit = GlobalClass.getProperUnit(((Product)workOrderSuggestion.getProduct()).getSU().toString());
        unit_textView.setText(unit);
        String materialName = ((Product)workOrderSuggestion.getProduct()).getDescrOperLang();
        if(materialName.equals("")){
            materialName = ((Product)workOrderSuggestion.getProduct()).getDescr6();
        }
        materialName_textView.setText(materialName);
    }
    public void getElementsFromIntent(){
        password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        userSwd = getIntent().getStringExtra("userSwd");
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void scanZP(View view){
        try{
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować ZP");
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
            if (requestCode == 70) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    showMaterialDialog(content);
                }else{
                    Log.d("result not ok", "not ok");
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void showMaterialDialog(String zp){
        AlertDialog.Builder controlAnalysisDialog = new AlertDialog.Builder(MaterialDemand.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_choose_material, viewGroup, false);
        controlAnalysisDialog.setView(dialogView);
        AlertDialog controlDialog = controlAnalysisDialog.create();
        controlDialog.show();
        getMaterials(zp, dialogView, controlDialog);

    }

    public void resetTextViews(){
        material_textView.setText("");
        materialName_textView.setText("");
        article_textView.setText("");
        qty_textEdit.setText("");
        ZP_textEdit.setText("");
        comments_textEdit.setText("");
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void getMaterials(String zp, View dialogView, AlertDialog controlDialog){
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<WorkOrderSuggestion> wosSB = SelectionBuilder.create(WorkOrderSuggestion.class);
            wosSB.add(Conditions.eq(WorkOrderSuggestion.META.idno, zp));
            WorkOrderSuggestion workOrder = QueryUtil.getFirst(ctx, wosSB.build());
            if(workOrder != null){
                setLook(workOrder);
                Reservations res = workOrder.getFirstRes();
                while(res!=null){
                    if((res.getElemDescr() instanceof Product) && (((Product)res.getElemDescr()).getProcureMode() != EnumProcurementType.Subcontracting)){
                        drawTable((Product)res.getElemDescr(), dialogView, controlDialog);
                    }
                    res = res.getItem();
                }
            }else{
                GlobalClass.showDialog(this, "Brak ZP!", "Zeskanowany obiekt ZP nie istnieje.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                controlDialog.dismiss();
                                resetTextViews();
                            }
                        });
            }

        } catch (Exception e) {
            ctx.out().println("Error:" + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(Product product, View dialogView, AlertDialog controlDialog){
        TableLayout layoutList = (TableLayout) dialogView.findViewById(R.id.materialTable);
        TableRow tableRowList = GlobalClass.setTableRowList(this);

        TextView material = new TextView(this);
        TextView materialName = new TextView(this);

        TextView[] textViewArray = {material, materialName};
        Integer j = layoutList.getChildCount();

        for (TextView textView :textViewArray) {
            if (j % 2 == 0) {
                textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            } else {
                textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
            }
        }

        GlobalClass.setParamForTextView(material, product.getSwd(), 12, 20, 5, false);

        //materialName
        String productName = product.getDescrOperLang();
        if(productName.equals("")){
            productName = product.getDescr6();
        }
        GlobalClass.setParamForTextView(materialName, productName, 12, 20, 5, false);
        for (TextView textView :textViewArray) {
            tableRowList.addView(textView);
        }
        layoutList.addView(tableRowList, j);

        tableRowList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                material_textView.setText(product.getSwd());
                controlDialog.dismiss();
            }
        });
    }

    public void send(View view){
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        Date today = new Date();
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        Boolean emptyFields = checkIfTextEditsAreEmpty();
        if(!emptyFields) {
            LoadingDialog = ProgressDialog.show(MaterialDemand.this, "",
                    "Ładowanie. Proszę czekać...", true);

            String comments = "Brak uwag.";
            if(comments_textEdit.getText().length() >0){
                comments = comments_textEdit.getText().toString();
            }
            GlobalClass.sendNotification(ctx, "Nowe zapotrz.: " + material_textView.getText() + ": " + qty_textEdit.getText() + " " + unit_textView.getText(),
                    "Od " + userSwd + "- Uwagi: " + comments, "WOZKOWI");

            String htmlString = "Dzień dobry! <br/><p> Użytkownik <b>" + userSwd + " </b> złożył w dniu " + dateFormat.format(today) + " zapotrzebowanie na materiał <b> "
                    + material_textView.getText() + " </b> w ilości <b>" + qty_textEdit.getText() + " " + unit_textView.getText() + "</b>."
                    + "</br> Dodatkowe uwagi: </br>" + comments;
            AppConfigValues appConfigValues = GlobalClass.getAppConfigValues(ctx);
            GlobalClass.sendMail(ctx, "Nowe zapotrzebowanie na materiał!", htmlString, appConfigValues.getYwarehousenotification());
            GlobalClass.showDialog(this, "Pomyślnie dodano!", "Zapotrzebowanie zostało pomyslnie dodane.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            resetTextViews();
                            ZP_textEdit.requestFocus();
                            GlobalClass.dismissLoadingDialog(LoadingDialog);
                        }
                    });
            GlobalClass.dismissLoadingDialog(LoadingDialog);
        }
        GlobalClass.ctxClose(ctx);
    }

    public Boolean checkIfTextEditsAreEmpty(){
        Boolean emptyFields = false;
        if(ZP_textEdit.getText().length() == 0){
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak ZP!", "Proszę uzupełnić ZP.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            ZP_textEdit.requestFocus();
                        }
                    });
        }else if(qty_textEdit.getText().length() == 0){
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak ilości!", "Proszę uzupełnić ilość.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            qty_textEdit.requestFocus();
                        }
                    });
        }
        return emptyFields;
    }
}