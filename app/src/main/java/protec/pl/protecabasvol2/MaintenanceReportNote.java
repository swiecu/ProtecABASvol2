package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorAction;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owpl.IsMailSender;
import de.abas.erp.db.schema.capacity.WorkCenter;
import de.abas.erp.db.schema.capacity.WorkCenterEditor;
import de.abas.erp.db.schema.custom.protec.AppConfigValues;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class MaintenanceReportNote extends AppCompatActivity {
    private String password;
    public String getPassword() { return password;}
    public void setPassword(String password) {this.password = password; }
    String database, user, userSwd;
    TextView machineName_TextView, message; EditText machine_TextEdit;
    DbContext ctx, sessionCtx; ProgressDialog LoadingDialog; AppConfigValues appConfigValues;
    WorkCenter machine; View send_btn; Handler handler; Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_report_note);
        getElementsFromIntent();
        getElementsById();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MaintenanceReportNote.this, userSwd));
        send_btn.setEnabled(true);
    }

    public void onBackPressed(){
        new setIntentAsyncTask().execute("Maintenance");
        super.onBackPressed();
    }
    // na wyjście z actvity
    @Override
    protected void onStop(){
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

    public void getElementsById(){
        machine_TextEdit = findViewById(R.id.machine_TextEdit);
        machineName_TextView = findViewById(R.id.machineName_textView);
        message = findViewById(R.id.message_MultiLine);
        send_btn = findViewById(R.id.send_btn);
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(MaintenanceReportNote.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(MaintenanceReportNote.this, "",
                    "Ładowanie. Proszę czekać...", true);
        }

        @Override
        protected String doInBackground(String... strings) {
            String destination = strings[0];
            setIntent(destination);
            return null;
        }

        protected void onPostExecute(String param){
            startActivity(intent);
        }
    }
    public void setIntent(String destination){
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void scanMachine(View view) {
        try {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować maszynę");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            Intent intent = integrator.createScanIntent();
            startActivityForResult(intent, 101);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
        }
    }

    // ON ACTIVITY RESULT
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data);
        if (result != null) {
            if (requestCode == 101) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    checkMachine(content);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void checkMachine(String content) {
        String machine_swd = "", machine_name = "";

        if (MachineExists(content) != null) {
            send_btn.setEnabled(true);
            machine = MachineExists(content);
            machine_swd = machine.getSwd();
            machine_name = machine.getDescr6();
            machine_TextEdit.setText(machine_swd);
            machineName_TextView.setText(machine_name);
            ctx.close();
        }
        else{
            GlobalClass.showDialog(this, "Brak maszyny!", "Zeskanowana maszyna nie isnieje.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        }
    }

    public WorkCenter MachineExists(String card_nr) {
        machine = null;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<WorkCenter> machineSB = SelectionBuilder.create(WorkCenter.class);
            machineSB.add(Conditions.eq(WorkCenter.META.swd, card_nr));
            machine = QueryUtil.getFirst(ctx, machineSB.build());
            GlobalClass.ctxClose(ctx);
        } catch (DBRuntimeException e) {
            catchExceptionCases(e, "MachineExists", card_nr);
        }
        return machine;
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function, String parameter){

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
                    if(function.equals("MachineExists")) {
                        MachineExists(parameter);
                    }
                }
            };
        }
    }
    public void Send(View view) throws CommandException {
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        Date today = new Date();
        EditText machine_TextEdit = findViewById(R.id.machine_TextEdit);
        String machine_text = machine_TextEdit.getText().toString(), mess_text = message.getText().toString();
        Boolean emptyFields = checkIfFieldsEmpty(machine_text, mess_text);

        if (emptyFields == false) {
            LoadingDialog = ProgressDialog.show(MaintenanceReportNote.this, "",
                    "Ładowanie. Proszę czekać...", true);
            send_btn.setEnabled(false);
            WorkCenterEditor workCenterEditor = machine.createEditor();
            workCenterEditor.open(EditorAction.UPDATE);
            workCenterEditor.setYcomment(mess_text); // na test nie ma tego pola
            workCenterEditor.commit();
            if(workCenterEditor.active()){
                workCenterEditor.abort();
            }
            GlobalClass.ctxClose(ctx);
            appConfigValues = getAppConfigValues();
            IsMailSender sender = ctx.openInfosystem(IsMailSender.class);
            sender.setYto(appConfigValues.getYmaintenancereport()); //pobieranie emaili z abasa
            sender.setYsubject("Nowa wiadomosc odnosnie utrzymania ruchu!");
            String text = ("Dzień dobry! <br/> Użytkownik " + user + " wysłał w dniu " + dateFormat.format(today) + " następnującą wiadomość: <br/><p> " + mess_text + "</p><b>Maszyna: </b>" +  machine_TextEdit.getText()
                    +"<br/><b>Nazwa maszyny: </b>" + machineName_TextView.getText());
            sender.setYtrext(text);
            sender.invokeStart();
            sender.close();
            GlobalClass.ctxClose(ctx);
            GlobalClass.showDialog(this, "Wysłano!", "Wiadomość została pomyślnie wysłana.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GlobalClass.ctxClose(ctx);
                            new setIntentAsyncTask().execute("Menu");
                        }
                    });
            GlobalClass.ctxClose(ctx);
        }
        GlobalClass.dismissLoadingDialog(LoadingDialog);
    }

    public Boolean checkIfFieldsEmpty(String machine_text, String mess_text){
        Boolean emptyFields= false;
        if (machine_text.equals("")) {
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak maszyny!", "Proszę wprowadzić maszynę.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

        }else if (mess_text.equals("")) {
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak wiadomości!", "Proszę wpisać wiadomość.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        }
        return emptyFields;
    }

    public AppConfigValues getAppConfigValues() {
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        SelectionBuilder<AppConfigValues> stocktakingSB = SelectionBuilder.create(AppConfigValues.class);
        stocktakingSB.add(Conditions.eq(AppConfigValues.META.swd, "OGOLNE"));
        appConfigValues = QueryUtil.getFirst(ctx, stocktakingSB.build());
        GlobalClass.ctxClose(ctx);
        return appConfigValues;
    }
}