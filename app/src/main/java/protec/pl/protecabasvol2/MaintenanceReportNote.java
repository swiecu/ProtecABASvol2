package protec.pl.protecabasvol2;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import de.abas.erp.db.infosystem.custom.owpl.IsMailSender;
import de.abas.erp.db.schema.capacity.WorkCenter;
import de.abas.erp.db.schema.custom.protec.AppConfigValues;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class MaintenanceReportNote extends AppCompatActivity {
    private String password;
    public String getPassword() { return password;}
    public void setPassword(String password) {this.password = password; }
    String database, user;
    TextView machineName_TextView, message;
    EditText machine_TextEdit;
    DbContext ctx;
    ProgressDialog LoadingDialog;
    AppConfigValues appConfigValues;
    WorkCenter machine;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_report_note);
        getElementsFromIntent();
    }

    public void onBackPressed(){
        super.onBackPressed();
        setIntent("Maintenance");
    }
    // na wyjście z actvity
    @Override
    protected void onStop(){
        super.onStop();
        if (LoadingDialog != null){
            LoadingDialog.dismiss();
        }
    }
    public void getElementsFromIntent(){
        String password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        setPassword(password);
        user = (getIntent().getStringExtra("user"));
    }

    public void setIntent(String destination){
        try {
            Intent intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
        String machine_swd = "";
        String machine_name = "";
        machine_TextEdit = findViewById(R.id.machine_TextEdit);
        machineName_TextView = findViewById(R.id.machineName_textView);

        if (MachineExists(content) != null) {
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
            Log.d("try", "before");
            SelectionBuilder<WorkCenter> machineSB = SelectionBuilder.create(WorkCenter.class);
            machineSB.add(Conditions.eq(WorkCenter.META.swd, card_nr));
            machine = QueryUtil.getFirst(ctx, machineSB.build());
            ctx.close();
        } catch (Exception e) {
            machineName_TextView.setText("");
            machine_TextEdit.setText("");
            GlobalClass.showDialog(this, "Brak połączenia!", "Nie można aktualnie połączyć z bazą.", "OK", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {}});
            //LoadingDialog.dismiss();
        }
        return machine;
    }

    public void Send(View view) {
        LoadingDialog = ProgressDialog.show(MaintenanceReportNote.this, "",
                "Ładowanie. Proszę czekać...", true);
        message = findViewById(R.id.message_MultiLine);
        String mess_text = message.getText().toString();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        Date today = new Date();
        EditText machine_TextEdit = findViewById(R.id.machine_TextEdit);
        String machine_text = machine_TextEdit.getText().toString();
        if (machine_text.equals("")) {
            GlobalClass.showDialog(this, "Brak maszyny!", "Proszę wprowadzić maszynę.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

        } else if (!mess_text.equals("")) {
            appConfigValues = getAppConfigValues();
            IsMailSender sender = ctx.openInfosystem(IsMailSender.class);
            sender.setYto(appConfigValues.getYmaintenancereport()); //pobieranie emaili z abasa
            sender.setYsubject("Nowa wiadomosc odnosnie utrzymania ruchu!");
            String text = ("Dzień dobry! <br/> Użytkownik " + user + " wysłał w dniu " + dateFormat.format(today) + " następnującą wiadomość: <br/><p> " + mess_text + "</p><b>Maszyna: </b>" +  machine_TextEdit.getText()
                    +"<br/><b>Nazwa maszyna: </b>" + machineName_TextView.getText());
            sender.setYtrext(text);
            sender.invokeStart();
            sender.close();
            GlobalClass.showDialog(this, "Wysłano!", "Wiadomość została pomyślnie wysłana.", "OK",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setIntent("Menu");
                }
            });
            LoadingDialog.dismiss();
            ctx.close();
        } else {
            GlobalClass.showDialog(this, "Brak wiadomości!", "Proszę wpisać wiadomość.", "OK",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        }
        LoadingDialog.dismiss();
    }
    public AppConfigValues getAppConfigValues() {
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        SelectionBuilder<AppConfigValues> stocktakingSB = SelectionBuilder.create(AppConfigValues.class);
        stocktakingSB.add(Conditions.eq(AppConfigValues.META.swd, "OGOLNE"));
        appConfigValues = QueryUtil.getFirst(ctx, stocktakingSB.build());
        ctx.close();
        return appConfigValues;
    }
}