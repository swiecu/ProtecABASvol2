package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
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
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.abas.erp.common.type.AbasDate;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUser;
import de.abas.erp.db.schema.custom.protec.AppConfigValues;
import de.abas.erp.db.schema.custom.protec.StocktakingProtec;
import de.abas.erp.db.schema.custom.protec.StocktakingProtecEditor;
import de.abas.erp.db.schema.employee.Employee;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class Menu extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    String user_short_name = "", user, database, userSwd;
    DbContext ctx, sessionCtx;
    RelativeLayout quality_relative_layout, move_relative_layout, stocktaking_relative_layout,maintenance_relative_layout, warehouseTransfer_relative_layout, stockInfo_relative_layout, income_relative_layout;
    TextView quality_cont_textView, move_textView, stocktaking_textView, stockInfo_textView, maintenance_textView, warehosueTransfer_textView, income_textView, loggedUser;
    ImageView quality_control, move, stocktaking, stockInfo, maintenance, warehosueTransfer, income;
    Employee employee; Intent intent; Handler handler;
    ProgressDialog LoadingDialog; AppConfigValues appConfigValues;
    RelativeLayout[] relativeLayoutList; TextView[] textViewList; ImageView[] imageViewList;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Log.d("CHECKING USER NAME", LoginContextManagement.getUserName(this));
        if(LoginContextManagement.getUserName(this).length() == 0)
        {
            Log.d("REDIRECTED", "TO MAINACTIVITY");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else
        {
            Log.d("STAYED", "HERE");
        }
        Log.d("entered MENU","ENTERED MENU OPTION");
        getElementsFromIntent();
        getElementsById();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", getPassword(), "mobileApp"); //informacja o dostęp. funkcjach dla użytk. zaciągnięte z erp
            IsPrLoggedUser lu = ctx.openInfosystem(IsPrLoggedUser.class);
            user_short_name = lu.getYuser();
            employee = FindEmployeeBySwd(ctx, user_short_name);
            if (employee != null) {
                setMenuLook(employee);
                GlobalClass.ctxClose(ctx);
            }
            Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(Menu.this, userSwd));
        }catch (DBRuntimeException e){
            catchExceptionCases(e);
        }
    }

    @Override
    public void onBackPressed(){
        GlobalClass.showDialogTwoButtons(this, "Wylogowanie", "Czy napewno chcesz się wylogować?", "Wyloguj", "Anuluj",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LoginContextManagement.setUserName(Menu.this, ""); // clear out login instance
                        Intent intent = new Intent(Menu.this, MainActivity.class);
                        startActivity(intent);
                    }
                }, new DialogInterface.OnClickListener() { //Anuluj button
                    @Override public void onClick(DialogInterface dialogInterface, int i) { } });
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        GlobalClass.ctxClose(ctx);
        super.onPause(); //has to be after code
    }

    protected void onResume() {
        super.onResume();
    }

    public void getElementsById(){
        stockInfo_relative_layout = findViewById(R.id.stockInfo_relative_layout);
        quality_relative_layout = findViewById(R.id.quality_relative_layout);
        move_relative_layout = findViewById(R.id.move_relative_layout);
        stocktaking_relative_layout = findViewById(R.id.stocktaking_relative_layout);
        maintenance_relative_layout = findViewById(R.id.maintenance_relative_layout);
        warehouseTransfer_relative_layout = findViewById(R.id.warehosueTransfer_relative_layout);
        income_relative_layout = findViewById(R.id.income_relative_layout);

        quality_cont_textView = findViewById((R.id.quality_cont_textView));
        move_textView = findViewById((R.id.move_textView));
        stocktaking_textView = findViewById((R.id.stocktaking_textView));
        stockInfo_textView = findViewById(R.id.stockInfo_textView);
        maintenance_textView = findViewById(R.id.maintenance_textView);
        warehosueTransfer_textView = findViewById(R.id.warehosueTransfer_textView);
        income_textView = findViewById(R.id.income_textView);

        quality_control= findViewById((R.id.quality_control));
        move = findViewById((R.id.move));
        stocktaking = findViewById((R.id.stocktaking));
        stockInfo = findViewById((R.id.stockInfo));
        maintenance = findViewById((R.id.maintenance));
        warehosueTransfer = findViewById((R.id.warehosueTransfer));
        loggedUser = findViewById(R.id.loggedUser);
        income = findViewById(R.id.income);
    }

    public void setMenuLook(Employee employee){
        user = employee.getAddr().toUpperCase();
        GlobalClass.ctxClose(ctx); //necessary
        userSwd = getEmployeeSwd();
        appConfigValues = getAppConfigValues();
        Log.d("userSwdMenu", userSwd);
        Log.d("databaseMenu", database);
        loggedUser.setText(user);
        relativeLayoutList = new RelativeLayout[] {quality_relative_layout, move_relative_layout, stocktaking_relative_layout, maintenance_relative_layout, warehouseTransfer_relative_layout, stockInfo_relative_layout, income_relative_layout};
        textViewList = new TextView[] {quality_cont_textView, move_textView, stocktaking_textView, stockInfo_textView, maintenance_textView, warehosueTransfer_textView, income_textView, loggedUser};
        imageViewList = new ImageView[]{quality_control, move, stocktaking, stockInfo, maintenance, warehosueTransfer, income};
        for(int i=0; i<relativeLayoutList.length; i++){
            if(appConfigValues.getYstocktakinglock() == false) { //diabling while inventory
                setLookForMenuOption(relativeLayoutList[i], textViewList[i], imageViewList[i], "#FFFFFF", (float) 1, (float) 1); //enable
            }else{
                setLookForMenuOption(relativeLayoutList[i], textViewList[i], imageViewList[i], "#41EFEEEE", (float) 0.35, (float) 0.25); //disable
                relativeLayoutList[i].setEnabled(false);
            }
        }
        setLookForMenuOption(stocktaking_relative_layout, stocktaking_textView, stocktaking, "#FFFFFF", (float) 1, (float) 1); //for stocktaking always enable
        stocktaking_relative_layout.setEnabled(true);

        if (employee.getYqm() == false) {
            setLookForMenuOption(quality_relative_layout, quality_cont_textView, quality_control, "#41EFEEEE", (float) 0.35, (float) 0.25);
        }
        if (employee.getYwarehouseman() == false) {
            setLookForMenuOption(move_relative_layout, move_textView, move, "#41EFEEEE", (float) 0.35, (float) 0.25);
        }

        //temporary on income
//        setLookForMenuOption(income_relative_layout, income_textView, income, "#41EFEEEE", (float) 0.35, (float) 0.25); //disable
    }

    public void setLookForMenuOption(RelativeLayout relativeLayout, TextView textView, ImageView imageView, String colorString,  Float alphaValueTextView, Float alphaValueImageView){
        relativeLayout.setBackgroundColor(Color.parseColor(colorString));
        relativeLayout.setBackgroundResource(R.drawable.card_edge);
        textView.setAlpha(alphaValueTextView);
        imageView.setAlpha(alphaValueImageView);
    }

    public void getElementsFromIntent(){
        String password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        userSwd = (getIntent().getStringExtra("userSwd"));
        setPassword(password);
    }

    // setting Intent With AsyncTask
    public void setIntent(String destination, String stockID){
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("user", user);
            intent.putExtra("stockID", stockID);
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(Menu.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(Menu.this, "",
                    "Ładowanie. Proszę czekać...", true);
        }

        @Override
        protected String doInBackground(String... strings) {
            String destination = strings[0], stockID = strings[1];
            setIntent(destination, stockID);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
        }
    }

    public void checkStock (View view){
        new setIntentAsyncTask().execute("StockInformation", "");
    }

    public void move (View view){
        if(employee != null) {
            if (employee.getYwarehouseman() == true) {
                new setIntentAsyncTask().execute("Move", "");
            } else {
                GlobalClass.showDialog(this, "Ta opcja jest niedostępna!", "Musisz być magazynierem aby korzystać z tej opcji.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}});
            }
        }
    }

    public void qualityControl (View view){
        if(employee != null) {
            if (employee.getYqm() == true) {
                new setIntentAsyncTask().execute("QualityControl", "");
            } else {
                GlobalClass.showDialog(this, "Ta opcja jest niedostępna!", "Musisz być kontrolerem jakości aby korzystać z tej opcji.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}});
            }
        }
    }

    public void maintenance (View view){
        new setIntentAsyncTask().execute("Maintenance", "");
    }

    public void warehouseStockTranfer(View view){
        new setIntentAsyncTask().execute("WarehouseStockTransfer", "");
    }

    public void income (View view){
//        GlobalClass.showDialog(this, "Opcja niedostępna!", "Ta opcja jest aktualnie niedostępna.", "OK",
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {}});
        new setIntentAsyncTask().execute("QualityControlIncome", "");
    }

    public void showMap(View view){
        new setIntentAsyncTask().execute("DialogMap", "");
    }

    public void stocktaking (View view){
        AlertDialog.Builder scanCommitteeDialog = new AlertDialog.Builder(Menu.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_stocktaking_committee_scan, viewGroup, false);
        scanCommitteeDialog.setView(dialogView);
        AlertDialog committeeDialog = scanCommitteeDialog.create();
        Button button_indivStocktaking = (Button)dialogView.findViewById(R.id.button_indivStocktaking);
        committeeDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);

        button_indivStocktaking .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalClass.showDialogTwoButtons(Menu.this, "Otwórz Inwentaryzację Wewnętrzną", "Czy napewno chcesz otworzyć nową Inwentaryzację Wewnętrzną?",
                        "Otwórz", "Anuluj",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String stocktskingObject = createNewStocktakingObject();
                                new setIntentAsyncTask().execute("Stocktaking", stocktskingObject);
                            }
                        }, new DialogInterface.OnClickListener() { //Anuluj button
                            @Override public void onClick(DialogInterface dialogInterface, int i) { } });
                ;}
        });
        Button button_qr = (Button)dialogView.findViewById(R.id.button_qr);
        button_qr.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v){
                ScanQR();
            }
        });
        committeeDialog.show();
        committeeDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public String createNewStocktakingObject(){
        AbasDate today = new AbasDate();
        String year = today.toString().substring(0,4);
        String newObject = userSwd +"_"+ today.toString(), objectSwd = "KI"+year+"_"+newObject, stocktakingIdno;
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        StocktakingProtec stocktakingObject = getStocktakingBySwd(ctx, objectSwd);
        if(stocktakingObject == null){
            StocktakingProtecEditor newStocktaking = (StocktakingProtecEditor) ctx.newObject(StocktakingProtecEditor.class);
            newStocktaking.setYstocktakingnumber(userSwd);
            newStocktaking.setYcommittee(today.toString());  //generated swd model : "KI+year_Ycommittee_YstocktakingNumber (KI2020_BKOCHAN_20210113)
            newStocktaking.setYinternal(true);
            newStocktaking.commit();
            if(newStocktaking.active()) {
                newStocktaking.abort();
            }
            stocktakingIdno = getStocktakingBySwd(ctx, objectSwd).getIdno(); //has to getStocktaking by swd, because newStocktaking has no idno before saving
            GlobalClass.ctxClose(ctx);
        }else{
            stocktakingIdno = stocktakingObject.getIdno();
        }
        return  stocktakingIdno;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void ScanQR(){
        try{
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować nr komisji");
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
                    ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
                    StocktakingProtec stock = FindStocktakingByIdno(ctx, content);
                    if(stock != null){
                        GlobalClass.ctxClose(ctx);
                        new setIntentAsyncTask().execute("Stocktaking", stock.getIdno());
                    }else{
                        GlobalClass.showDialog(Menu.this, "Błędny numer komisji", "Podany numer komisji nie istnieje.", "OK", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {} });
                    }
                    GlobalClass.ctxClose(ctx);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    public final Employee FindEmployeeBySwd(DbContext ctx, String name){
        Employee employee = null;
        SelectionBuilder<Employee> employeeSB = SelectionBuilder.create(Employee.class);
        try {
            employeeSB.add(Conditions.eq(Product.META.swd.toString(), name));
            employee = QueryUtil.getFirst(ctx, employeeSB.build());
        } catch (Exception e) {
        }
        return employee;
    }

    public final StocktakingProtec getStocktakingBySwd(DbContext ctx, String newObject){
        StocktakingProtec stocktaking = null;
        SelectionBuilder<StocktakingProtec> stocktakingSB = SelectionBuilder.create(StocktakingProtec.class);
        try {
            stocktakingSB.add(Conditions.eq(Product.META.swd.toString(), newObject));
            stocktaking = QueryUtil.getFirst(ctx, stocktakingSB.build());
            GlobalClass.ctxClose(ctx);
        } catch (Exception e) {
        }
        return stocktaking;
    }

    public final StocktakingProtec FindStocktakingByIdno(DbContext ctx, String idno){
        StocktakingProtec stocktaking = null;
        SelectionBuilder<StocktakingProtec> stocktakingSB = SelectionBuilder.create(StocktakingProtec.class);
        try {
            stocktakingSB.add(Conditions.eq(Product.META.idno.toString(), idno));
            stocktaking = QueryUtil.getFirst(ctx, stocktakingSB.build());
            GlobalClass.ctxClose(ctx);
        } catch (Exception e) {
        }
        return stocktaking;
    }

    public String getEmployeeSwd(){
        DbContext ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", getPassword(), "mobileApp");
        IsPrLoggedUser lu = ctx.openInfosystem(IsPrLoggedUser.class);
        String userSwd = lu.getYuser();
        GlobalClass.ctxClose(ctx);
        return userSwd;
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e) {

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
                    startActivity(new Intent(Menu.this, Menu.class));
                }
            };
        }
    }

    public AppConfigValues getAppConfigValues() {
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", getPassword(), "mobileApp");
        SelectionBuilder<AppConfigValues> stocktakingSB = SelectionBuilder.create(AppConfigValues.class);
        stocktakingSB.add(Conditions.eq(AppConfigValues.META.swd, "OGOLNE"));
        appConfigValues = QueryUtil.getFirst(ctx, stocktakingSB.build());
        GlobalClass.ctxClose(ctx);
        return appConfigValues;
    }
}