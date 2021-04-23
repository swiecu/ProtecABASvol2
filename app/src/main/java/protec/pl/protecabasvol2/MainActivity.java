package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUser;
import de.abas.erp.db.schema.employee.Employee;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class MainActivity extends AppCompatActivity {
   DbContext ctx, sessionCtx;
   ProgressDialog LoadingDialog;
   private AppUpdateManager mAppUpdateManager;
   Employee employee;
   String user_short_name = "", password;
   Button login_btn;
   Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MainActivity.this, "no user yet- error in MainActivity"));
        login_btn = (Button) findViewById(R.id.login_btn);
        mAppUpdateManager = AppUpdateManagerFactory.create(this);
        mAppUpdateManager.registerListener(installStateUpdatedListener);
        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE /*AppUpdateType.FLEXIBLE*/)){
                try {
                    mAppUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.IMMEDIATE /*AppUpdateType.FLEXIBLE*/, MainActivity.this, 77);
                } catch (IntentSender.SendIntentException e) {
                    e.getMessage();
                }
            }
        });
    }

    InstallStateUpdatedListener installStateUpdatedListener = new
        InstallStateUpdatedListener() {
            @Override
            public void onStateUpdate(InstallState state) {
                try {
                    if (state.installStatus() == InstallStatus.DOWNLOADED) {
                        //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                        // popupSnackbarForCompleteUpdate();
                    }else if (state.installStatus() == InstallStatus.INSTALLED) {
                        if (mAppUpdateManager != null) {
                            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
                        }
                    } else {
                        Log.i("mess", "InstallStateUpdatedListener: state: " + state.installStatus());
                    }
                }catch(Exception e){

                }
            }
        };

    protected void onResume() {
        super.onResume();
        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(
        appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // If an in-app update is already running, resume the update.
                try {
                    mAppUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.IMMEDIATE, this, 77);
                } catch (IntentSender.SendIntentException e) {
                    e.getMessage();
                }
            }
        });
    }

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
        GlobalClass.dismissLoadingDialog(LoadingDialog);
        if (mAppUpdateManager != null) {
            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        GlobalClass.ctxClose(ctx);
        super.onPause();
    }

    @Override
    public void onBackPressed(){
        //do nothing, the code is preventing from going back to being logged in
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
                    GlobalClass.showDialog(this,"Błąd podczas aktuallizacji","Nie udało się zaktualizować aplikacji, proszę spróbować później. Kod błędu: " + resultCode, "OK",new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) { } });
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

   public void login(View view) {
       EditText password_text = findViewById(R.id.password_text);
       password = password_text.getText().toString();
       if (!password.equals("")) {
           try {
               StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
               StrictMode.setThreadPolicy(policy);
               ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", password, "mobileApp");  // musi być erp, na pierwszym logowaniu
               checkUserDatabase(ctx);
               GlobalClass.ctxClose(ctx);
           } catch (DBRuntimeException e) {
               catchExceptionCases(e, "loginCallOnClick");
           }
       } else {
           GlobalClass.showDialog(this, "Brak hasła!", "Proszę wpisać hasło.", "OK", new DialogInterface.OnClickListener() {
               @Override public void onClick(DialogInterface dialog, int which) {}
           });
       }
   }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function){
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
                if(function.equals("loginCallOnClick")) {
                    login_btn.callOnClick();
                }
                }
            };
        }
    }

   public void checkUserDatabase(DbContext ctx){
       IsPrLoggedUser lu = ctx.openInfosystem(IsPrLoggedUser.class);
       user_short_name = lu.getYuser();
       employee = FindEmployeeBySwd(ctx, user_short_name);
       if(employee != null) {
           if ((employee.getYdatabase() == null) || (employee.getYdatabase().isEmpty())){
               GlobalClass.showDialog(this, "Brak dostępu!", "Nie masz dostępu do tej aplikacji.", "OK", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                   }
               });
           } else {
               Boolean correctDatabase = false;
               String database = employee.getYdatabase().toLowerCase();
                switch (database){
                    case "test":
                        correctDatabase = true;
                        break;
                    case "erp":
                        correctDatabase = true;
                        break;
                    case "demo":
                        correctDatabase = true;
                        break;
                }

               if(correctDatabase == true) {
                   Intent intent = new Intent(this, Menu.class);
                   intent.putExtra("password", password);
                   intent.putExtra("database", database);
                   startActivity(intent);
                   LoadingDialog = ProgressDialog.show(MainActivity.this, "",
                           "Ładowanie. Proszę czekać...", true);
               }else{
                   GlobalClass.showDialog(this, "Błędna baza", "Błędna baza ustawiona na użytkowniku.", "OK", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                       }
                   });
               }
           }
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
}