package protec.pl.protecabasvol2;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.Query;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUsers;
import de.abas.erp.db.schema.employee.Employee;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class MainActivity extends AppCompatActivity {
   DbContext ctx;
   ProgressDialog LoadingDialog;
   private AppUpdateManager mAppUpdateManager;
   Employee employee;
   String user_short_name = "", password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppUpdateManager = AppUpdateManagerFactory.create(this);
        mAppUpdateManager.registerListener(installStateUpdatedListener);
        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE /*AppUpdateType.IMMEDIATE*/)){
                try {
                    mAppUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.FLEXIBLE /*AppUpdateType.IMMEDIATE*/, MainActivity.this, 77);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED){
                //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                popupSnackbarForCompleteUpdate();
            }
        });
    }
    InstallStateUpdatedListener installStateUpdatedListener = new
            InstallStateUpdatedListener() {
                @Override
                public void onStateUpdate(InstallState state) {
                    if (state.installStatus() == InstallStatus.DOWNLOADED){
                        //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                        popupSnackbarForCompleteUpdate();
                    } else if (state.installStatus() == InstallStatus.INSTALLED){
                        if (mAppUpdateManager != null){
                            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
                        }

                    } else {
                        Log.i("mess", "InstallStateUpdatedListener: state: " + state.installStatus());
                    }
                }
            };
    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.activity_main),
                        "Dostępna nowa wersja aktualizacji!",
                        Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction("Instaluj", view -> {
            if (mAppUpdateManager != null){
                mAppUpdateManager.completeUpdate();
            }
        });
        snackbar.setActionTextColor(getResources().getColor(R.color.colorProtec));
        snackbar.show();
    }

    //po wyjściu z ekranu
    @Override
    protected void  onStop(){
        super.onStop();
        if (LoadingDialog != null){
            LoadingDialog.dismiss();
        }
        if (mAppUpdateManager != null) {
            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }
    // CHECK STOCK
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void ScanQR(View view){
        try{
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować hasło");
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
                    EditText password_text = findViewById(R.id.password_text);
                    password_text.setText(content);
                    findViewById(R.id.login_btn).callOnClick();
                }
            }
            if (requestCode == 77) {
                if (resultCode != RESULT_OK) {
                    GlobalClass.showDialog(this,"Błąd podczas aktuallizacji","Nie udało się zaktualizować aplikacji, proszę spróbować później.", "OK",new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) { } });
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // on Login Button Click
   public void login(View view)  {
        EditText password_text = findViewById(R.id.password_text);
        password = password_text.getText().toString();
        if(!password.equals("")){
            try {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", password, "mobileApp");  // musi być erp, na pierwszym logowaniu
                checkUserDatabase(ctx);
                ctx.close();

            }catch (DBRuntimeException e) {
                //błędne hasło
                if(e.getMessage().contains("password")){
                    GlobalClass.showDialog(this,"Błędne hasło!","Podane hasło jest błędne.", "OK",new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { } });

                //brak połączenia
                }else if(e.getMessage().contains("failed")){
                    GlobalClass.showDialog(this,"Brak połączenia!","Nie można się aktualnie połączyć z bazą.", "OK",new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { } });

                    //przekroczona liczba licencji
                }else if(e.getMessage().contains("FULL")){
                    GlobalClass.showDialog(this,"Przekroczona liczba licencji!","Liczba licencji została przekrocona.", "OK",new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { } });
                }else{
                    Log.d("error", e.getMessage());
                }
            }
         // brak wpisanego hasła
        }else{
            GlobalClass.showDialog(this,"Brak hasła!","Proszę wpisać hasło.", "OK",new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { } });
        }
   }
   public void checkUserDatabase(DbContext ctx){
        Log.d("inCheckUserDatabase", "before");
       IsPrLoggedUsers lu = ctx.openInfosystem(IsPrLoggedUsers.class);
       user_short_name = lu.getYuser();
       employee = FindEmployeeBySwd(ctx, user_short_name);
       Log.d("inCheckUserDatabase", "after");

       if(employee.getYdatabase().isEmpty()){
           GlobalClass.showDialog(this,"Brak dostępu!","Nie masz dostępu do tej aplikacji.", "OK",new DialogInterface.OnClickListener() {
               @Override public void onClick(DialogInterface dialog, int which) { } });
       }else {
           Intent intent = new Intent(this, Menu.class);
           intent.putExtra("password", password);

           if (employee.getYdatabase().equalsIgnoreCase("test")) {
               intent.putExtra("database", "test");
               Log.d("database", "test");
           }
           if (employee.getYdatabase().equalsIgnoreCase("erp")) {
               intent.putExtra("database", "erp");
               Log.d("database", "erp");
           }
           startActivity(intent);
           LoadingDialog =  ProgressDialog.show(MainActivity.this, "",
                   "Ładowanie. Proszę czekać...", true);
       }
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