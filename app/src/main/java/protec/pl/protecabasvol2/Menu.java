package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.Query;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUsers;
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
    String user_short_name = "";
    String user;
    DbContext ctx;
    RelativeLayout quality_relative_layout, move_relative_layout, stocktaking_relative_layout;
    TextView quality_cont_textView, move_textView, stocktaking_textView, loggedUser;
    ImageView quality_control, move, stocktaking;
    Employee employee;
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        DbContext ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
        IsPrLoggedUsers lu = ctx.openInfosystem(IsPrLoggedUsers.class);

        getElementsById(); //pobieranie wszystkich elementów by id
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
        quality_relative_layout = findViewById(R.id.quality_relative_layout);
        move_relative_layout = findViewById(R.id.move_relative_layout);
        stocktaking_relative_layout = findViewById(R.id.stocktaking_relative_layout);
        quality_cont_textView = findViewById((R.id.quality_cont_textView));
        move_textView = findViewById((R.id.move_textView));
        stocktaking_textView = findViewById((R.id.stocktaking_textView));
        quality_control= findViewById((R.id.quality_control));
        move = findViewById((R.id.move));
        stocktaking = findViewById((R.id.stocktaking));
        loggedUser = findViewById(R.id.loggedUser);
    }
    public void setMenuLook(Employee employee){
        user = employee.getAddr().toUpperCase();
        loggedUser.setText("Zalogowany użytkownik: " + user);
        if (employee.getYqm() == false) {  // jeśli NIE jest kontrolerem jakości ustaw button jako disabled
            quality_relative_layout.setBackgroundColor(Color.parseColor("#41EFEEEE"));
            quality_cont_textView.setAlpha((float) 0.35);
            quality_control.setAlpha((float) 0.25);
        }else if(employee.getYqm() == true){
            quality_relative_layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
            quality_cont_textView.setAlpha((float) 1);
            quality_control.setAlpha((float) 1);
        }
        if (employee.getYwarehouseman() == false) {  // jeśli NIE jest magazynierem ustaw button jako disabled
            move_relative_layout.setBackgroundColor(Color.parseColor("#41EFEEEE"));
            move_textView.setAlpha((float) 0.35);
            move.setAlpha((float) 0.25);
        }else if(employee.getYwarehouseman() == true){
            move_relative_layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
            move_textView.setAlpha((float) 1);
            move.setAlpha((float) 1);
        }
        stocktaking_relative_layout.setBackgroundColor(Color.parseColor("#41EFEEEE"));
        stocktaking_textView.setAlpha((float) 0.35);
        stocktaking.setAlpha((float) 0.25);
    }


    public void checkStock (View view){
        Intent intent = new Intent(this, StockInformation.class );
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
    public void move (View view){
        if(employee != null) {
            if (employee.getYwarehouseman() == true) {
                Intent intent = new Intent(this, Move.class);
                intent.putExtra("password", getPassword());
                startActivity(intent);
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
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
        if(employee != null) {
            if (employee.getYqm() == true) {
                Intent intent = new Intent(this, QualityControl.class);
                intent.putExtra("password", getPassword());
                intent.putExtra("user", user);
                startActivity(intent);
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

    public void stocktaking (View view){
        /*ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
        Intent intent = new Intent(this, Stocktaking.class);
        intent.putExtra("password", getPassword());
        startActivity(intent);*/
        GlobalClass.showDialog(this, "Opcja niedostępna!", "Ta opcja jest jescze niedostępna.", "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
    }

    public final Employee FindEmployeeBySwd(DbContext ctx, String name){
        Employee employee = null;
        SelectionBuilder<Employee> employeeSB = SelectionBuilder.create(Employee.class);
        Query<Employee> employeeQuery = ctx.createQuery(employeeSB.build());
        try {
            employeeSB.add(Conditions.eq(Product.META.swd.toString(), name));
            employee = QueryUtil.getFirst(ctx, employeeSB.build());
        } catch (Exception e) {
        }
        return employee;
    }
}