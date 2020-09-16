package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUsers;
import de.abas.erp.db.schema.custom.protec.StocktakingProtec;
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
    String user_short_name = "", user, database;
    DbContext ctx;
    RelativeLayout quality_relative_layout, move_relative_layout, stocktaking_relative_layout,maintenance_relative_layout, warehouseTransfer_relative_layout, stockInfo_relative_layout;
    TextView quality_cont_textView, move_textView, stocktaking_textView, stockInfo_textView, maintenance_textView, warehosueTransfer_textView, loggedUser;
    ImageView quality_control, move, stocktaking, stockInfo, maintenance, warehosueTransfer;
    Employee employee;
    ProgressDialog LoadingDialog;
    CardView cardView;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        getElementsFromIntent();
        getElementsById(); //pobieranie wszystkich elementów by id

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", getPassword(), "mobileApp");
        IsPrLoggedUsers lu = ctx.openInfosystem(IsPrLoggedUsers.class);
        user_short_name = lu.getYuser();
        employee = FindEmployeeBySwd(ctx, user_short_name);
        if(employee != null) {
           setMenuLook(employee);
        }
        ctx.close();
    }

    @Override
    public void onBackPressed(){
        GlobalClass.showDialogTwoButtons(this, "Wylogowanie", "Czy napewno chcesz się wylogować?", "Wyloguj", "Anuluj",
            new DialogInterface.OnClickListener() {
            @Override
                public void onClick(DialogInterface dialog, int which) {
                     Intent intent = new Intent(Menu.this, MainActivity.class);
                     startActivity(intent);
                }
            }, new DialogInterface.OnClickListener() { //Anuluj button
            @Override public void onClick(DialogInterface dialogInterface, int i) { } });
    }

    public void getElementsById(){
        stockInfo_relative_layout = findViewById(R.id.stockInfo_relative_layout);
        quality_relative_layout = findViewById(R.id.quality_relative_layout);
        move_relative_layout = findViewById(R.id.move_relative_layout);
        stocktaking_relative_layout = findViewById(R.id.stocktaking_relative_layout);
        maintenance_relative_layout = findViewById(R.id.maintenance_relative_layout);
        warehouseTransfer_relative_layout = findViewById(R.id.warehosueTransfer_relative_layout);

        quality_cont_textView = findViewById((R.id.quality_cont_textView));
        move_textView = findViewById((R.id.move_textView));
        stocktaking_textView = findViewById((R.id.stocktaking_textView));
        stockInfo_textView = findViewById(R.id.stockInfo_textView);
        maintenance_textView = findViewById(R.id.maintenance_textView);
        warehosueTransfer_textView = findViewById(R.id.warehosueTransfer_textView);

        quality_control= findViewById((R.id.quality_control));
        move = findViewById((R.id.move));
        stocktaking = findViewById((R.id.stocktaking));
        stockInfo = findViewById((R.id.stockInfo));
        maintenance = findViewById((R.id.maintenance));
        warehosueTransfer = findViewById((R.id.warehosueTransfer));
        loggedUser = findViewById(R.id.loggedUser);
    }

    public void setMenuLook(Employee employee){
        user = employee.getAddr().toUpperCase();
        loggedUser.setText("Zalogowany użytkownik: " + user);
        if (employee.getYqm() == false) {  // jeśli NIE jest kontrolerem jakości ustaw button jako disabled
            quality_relative_layout.setBackgroundColor(Color.parseColor("#41EFEEEE"));
            quality_relative_layout.setBackgroundResource(R.drawable.card_edge);
            quality_cont_textView.setAlpha((float) 0.35);
            quality_control.setAlpha((float) 0.25);
        }else if(employee.getYqm() == true){
            quality_relative_layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
            quality_relative_layout.setBackgroundResource(R.drawable.card_edge);
            quality_cont_textView.setAlpha((float) 1);
            quality_control.setAlpha((float) 1);
        }
        if (employee.getYwarehouseman() == false) {  // jeśli NIE jest magazynierem ustaw button jako disabled
            move_relative_layout.setBackgroundColor(Color.parseColor("#41EFEEEE"));
            move_relative_layout.setBackgroundResource(R.drawable.card_edge);
            move_textView.setAlpha((float) 0.35);
            move.setAlpha((float) 0.25);
        }else if(employee.getYwarehouseman() == true){
            move_relative_layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
            move_relative_layout.setBackgroundResource(R.drawable.card_edge);
            move_textView.setAlpha((float) 1);
            move.setAlpha((float) 1);
        }
        /*stocktaking_relative_layout.setBackgroundColor(Color.parseColor("#41EFEEEE"));  // pole disabled dla inwentaryzacji  //WAŻNY KOD!
        stocktaking_textView.setAlpha((float) 0.35);
        stocktaking.setAlpha((float) 0.25);*/

        stockInfo_relative_layout.setBackgroundColor(Color.parseColor("#FFFFFF"));  // pole enabled dla informacji o stanie
        stockInfo_relative_layout.setBackgroundResource(R.drawable.card_edge);
        stockInfo_textView.setAlpha((float) 1);
        stockInfo.setAlpha((float) 1);

        maintenance_relative_layout.setBackgroundColor(Color.parseColor("#FFFFFF"));  // pole enabled dla utrzymania ruch
        maintenance_relative_layout.setBackgroundResource(R.drawable.card_edge);
        maintenance_textView.setAlpha((float) 1);
        maintenance.setAlpha((float) 1);

        stocktaking_relative_layout.setBackgroundColor(Color.parseColor("#FFFFFF"));  // pole enabled dla inwentaryzacji
        stocktaking_relative_layout.setBackgroundResource(R.drawable.card_edge);
        stocktaking_textView.setAlpha((float) 1);
        stocktaking.setAlpha((float) 1);

        warehouseTransfer_relative_layout.setBackgroundColor(Color.parseColor("#FFFFFF"));  // pole enabled dla inwentaryzacji
        warehouseTransfer_relative_layout.setBackgroundResource(R.drawable.card_edge);
        warehosueTransfer_textView.setAlpha((float) 1);
        warehosueTransfer.setAlpha((float) 1);
    }

    public void getElementsFromIntent(){
        String password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        setPassword(password);
    }

    public void setIntent(String destination, String content){
        try {
            Intent intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("user", user);
            intent.putExtra("stockID", content);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void checkStock (View view){
        setIntent("StockInformation", "");
    }

    public void move (View view){
        if(employee != null) {
            if (employee.getYwarehouseman() == true) {
                setIntent("Move", "");
            } else {
                GlobalClass.showDialog(this, "Ta opcja jest niedostępna!", "Musisz być magazynierem aby korzystać z tej opcji.", "OK",
                new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
                });
            }
        }
    }

    public void qualityControl (View view){
        if(employee != null) {
            if (employee.getYqm() == true) {
                setIntent("QualityControl", "");
            } else {
                GlobalClass.showDialog(this, "Ta opcja jest niedostępna!", "Musisz być kontrolerem jakości aby korzystać z tej opcji.", "OK",
                new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
                });
            }
        }
    }

    public void maintenance (View view){
         setIntent("Maintenance", "");
    }

    public void warehouseStockTranfer(View view){
        setIntent("WarehouseStockTransfer", "");
    }

    public void stocktaking (View view){
        AlertDialog.Builder scanCommitteeDialog = new AlertDialog.Builder(Menu.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_stocktaking_committee_scan, viewGroup, false);
        scanCommitteeDialog.setView(dialogView);
        AlertDialog committeeDialog = scanCommitteeDialog.create();
        Button button_cancel = (Button)dialogView.findViewById(R.id.button_cancel);
        button_cancel .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                committeeDialog.dismiss();}
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
                        ctx.close();
                        setIntent("Stocktaking", content);
                    }else{
                        GlobalClass.showDialog(Menu.this, "Błędny numer komisji", "Podany numer komisji nie istnieje.", "OK", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
                    }
                    ctx.close();
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

    public final StocktakingProtec FindStocktakingByIdno(DbContext ctx, String idno){
        StocktakingProtec stocktaking = null;
        SelectionBuilder<StocktakingProtec> stocktakingSB = SelectionBuilder.create(StocktakingProtec.class);
        try {
            stocktakingSB.add(Conditions.eq(Product.META.idno.toString(), idno));
            stocktaking = QueryUtil.getFirst(ctx, stocktakingSB.build());
            ctx.close();
        } catch (Exception e) {
        }
        return stocktaking;
    }

    public void showMap(View view){
        setIntent("DialogMap", "");
    }
}