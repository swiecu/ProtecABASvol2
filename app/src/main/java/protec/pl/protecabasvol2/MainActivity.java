package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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

import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorAction;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUser;
import de.abas.erp.db.schema.employee.Employee;
import de.abas.erp.db.schema.employee.EmployeeEditor;
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

        login_btn = findViewById(R.id.login_btn);
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
                    case "test1":
                        correctDatabase = true;
                        break;
                    case "erp":
                        correctDatabase = true;
                        break;
                    case "test2":
                        correctDatabase = true;
                        break;
                    case "test3":
                        correctDatabase = true;
                        break;
                    case "dev":
                        correctDatabase = true;
                        break;
                }

                if(correctDatabase == true) {
                    setUserReferenceToken(employee, ctx);
                    try {
                        setUserTopics(employee, ctx);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FirebaseMessagingService.setDBandPassword(database, password);
                    LoginContextManagement.setUserName(this, employee.getSwd());
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

    public void setUserReferenceToken(Employee employee, DbContext ctx){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener((com.google.android.gms.tasks.OnCompleteListener<String>) task -> {
            if (!task.isSuccessful()) {
                Log.w("ERROR", "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            if(!employee.getYappuserreference().equals(token)) {
                try {
                    EmployeeEditor employeeEditor = employee.createEditor();
                    employeeEditor.open(EditorAction.UPDATE);
                    employeeEditor.setYappuserreference(token);
                    employeeEditor.commit();
                    if(employeeEditor.active()){
                        employeeEditor.abort();
                    }
                } catch (CommandException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setUserTopics(Employee employee, DbContext ctx) throws IOException {
        readFromTopicFileAndUnsubscribe();
        String employeeTopics = employee.getYappgroups().toUpperCase(), topicString = "";
        String[] topics = {"KJ", "MAGAZYN", "UTRZYMANIERUCHU", "WOZKOWI"};
        for (String topic : topics) {
            if(employeeTopics.contains(topic)){
                FirebaseMessaging.getInstance().subscribeToTopic(topic);
                topicString = topicString + topic + ";";
            }else{
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
            }
        }
        FirebaseMessaging.getInstance().subscribeToTopic(employee.getSwd().toUpperCase());
        topicString = topicString + employee.getSwd().toUpperCase()+";";
        FirebaseMessaging.getInstance().subscribeToTopic("ALL");
        topicString = topicString + "ALL;";
        writeToTopicFile(this, topicString);
    }

    public void readFromTopicFileAndUnsubscribe() {
        FileInputStream fis = null;
        try {
            fis = this.openFileInput("userTopicsFile.txt");
            if (fis != null) {
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line, topicString = "";
                while ((line = bufferedReader.readLine()) != null) {
                    topicString += sb.append(line);
                }
                String[] topicsFromFile = topicString.split(";");
                for (String topic : topicsFromFile) {
                    Log.d("FILE", topic);
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
                }
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToTopicFile(Context context, String sBody){
        File dir = new File(String.valueOf(context.getFilesDir()));
        if(!dir.exists()){
            dir.mkdir();
        }

        try {
            File topicsFile = new File(dir, "userTopicsFile.txt");
            FileWriter writer = new FileWriter(topicsFile);
            writer.append(sBody);
            writer.flush();
            writer.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public final Employee FindEmployeeBySwd(DbContext ctx, String name){
        Employee employee = null;
        SelectionBuilder<Employee> employeeSB = SelectionBuilder.create(Employee.class);
        try {
            employeeSB.add(Conditions.eq(Employee.META.swd.toString(), name));
            employee = QueryUtil.getFirst(ctx, employeeSB.build());
        } catch (Exception e) {
            Log.d("getMessage", e.getMessage());
        }
        return employee;
    }
}