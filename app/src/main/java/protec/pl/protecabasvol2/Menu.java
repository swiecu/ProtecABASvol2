package protec.pl.protecabasvol2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import de.abas.erp.db.Query;
import de.abas.erp.db.infosystem.standard.la.StockLevelInformation;
import de.abas.erp.db.schema.employee.Employee;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.QueryUtil;
import protec.pl.protecabasvol2.R;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUsers;
import de.abas.erp.db.util.ContextHelper;

import static protec.pl.protecabasvol2.GlobalClass.FindProductByIdno;

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
    CardView quality_cardView;

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
        user_short_name = lu.getYuser();
        TextView loggedUser = findViewById(R.id.loggedUser);
        Employee employee = FindEmployeeBySwd(ctx, user_short_name);
        quality_cardView = findViewById(R.id.quality_cardView);
        if(employee != null) {
            user = employee.getAddr().toUpperCase();
            loggedUser.setText("Zalogowany użytkownik: " + user);
            if (employee.getYqm() == false) {
                quality_cardView.setBackgroundColor(Color.parseColor("#31595959"));
            }
        }else{
            loggedUser.setText("Zalogowany użytkownik: " + user_short_name);
            quality_cardView.setBackgroundColor(Color.parseColor("#31595959"));
        }

        ////////mCardViewBottom = (CardView) findViewById(R.id.card_view_bottom);
       // mCardViewBottom.setRadius(9);
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

    public void checkStock (View view){
        Intent intent = new Intent(this, StockInformation.class );
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
    public void move (View view){
        Intent intent = new Intent(this, Move.class );
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
    public void qualityControl (View view){
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
        Employee employee = FindEmployeeBySwd(ctx, user_short_name);
        if(employee != null) {
            if (employee.getYqm() == true) {
                Log.d("is employee",  "YES");
                Intent intent = new Intent(this, QualityControl.class);
                intent.putExtra("password", getPassword());
                intent.putExtra("user", user);
                startActivity(intent);
            } else {
                Log.d("is employee", "NO");
                quality_cardView.setEnabled(false);
                GlobalClass.showDialog(this, "Ta opcja jest niedostępna!", "Musisz być kontrolerem jakości aby korzystać z tej opcji.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
            }
        }
        Log.d("test", "Test");
    }
    public final Employee FindEmployeeBySwd(DbContext ctx, String name){
        Employee employee = null;
        SelectionBuilder<Employee> employeeSB = SelectionBuilder.create(Employee.class);
        Query<Employee> productQuery = ctx.createQuery(employeeSB.build());
        try {
            employeeSB.add(Conditions.eq(Product.META.swd.toString(), name));
            employee = QueryUtil.getFirst(ctx, employeeSB.build());
        } catch (Exception e) {
        }
        return employee;
    }
}