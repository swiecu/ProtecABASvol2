package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
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
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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

import de.abas.erp.db.DbContext;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owfe.IsApPdcAnalysis;
import de.abas.erp.db.infosystem.custom.owpl.IsMailSender;
import de.abas.erp.db.schema.capacity.WorkCenter;
import de.abas.erp.db.schema.custom.protec.AppConfigValues;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.workorder.WorkOrders;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class QualityControlReportNonCompliance extends AppCompatActivity {
    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    DbContext ctx, sessionCtx;
    ProgressDialog LoadingDialog; EditText nrCard_TextEdit;
    TextView artName_TextView, article_TextView, nrZP_TextView, message;
    WorkOrders card; TableLayout controlLayout;
    String user, choosenEmployee, choosenDepartment, choosenOperation, choosenMachineGroup, database, userSwd;
    AppConfigValues appConfigValues;
    View save_btn; Handler handler; Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality_control_report_non_compliance);
        getElementsFromIntent();
        getElementsById();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(QualityControlReportNonCompliance.this, userSwd));
        save_btn.setEnabled(true);
    }

    // na kliknięcie cofnij
    public void onBackPressed() {
        new setIntentAsyncTask().execute("QualityControl");
        super.onBackPressed();
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        GlobalClass.ctxClose(ctx);
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
        message = findViewById(R.id.message_MultiLine);
        nrCard_TextEdit = findViewById(R.id.machine_TextEdit);
        artName_TextView = findViewById(R.id.artName_TextView);
        article_TextView = findViewById(R.id.articleQuality_TextView);
        nrZP_TextView = findViewById(R.id.nrZP_TextView);
        save_btn = findViewById(R.id.save_btn);
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(QualityControlReportNonCompliance.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(QualityControlReportNonCompliance.this, "",
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

    public void scanWorkCard(View view) {
        try {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować nr karty");
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
                    save_btn.setEnabled(true);
                    checkCard(content);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void checkCard(String content) {
        String nr_card = "", article = "", article_name = "", nr_ZP = "";

        if (CardNrExists(content) != null) {
            WorkOrders card = CardNrExists(content);
            nr_card = card.getIdno();
            article = card.getProduct().getSwd();
            article_name = ((Product) card.getProduct()).getDescr6();
            nr_ZP = nr_card.substring(0, 4);

            drawTable(content, card);
            nrCard_TextEdit.setText(content);
            article_TextView.setText(article);
            artName_TextView.setText(article_name);
            nrZP_TextView.setText(nr_ZP);
            GlobalClass.ctxClose(ctx);
        }
        else{
            GlobalClass.showDialog(this, "Brak karty pracy!", "Zeskanowany nr karty nie istnieje.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {}
                    });
        }
    }

    public WorkOrders CardNrExists(String card_nr) {
        card = null;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            Log.d("try", "before");
            SelectionBuilder<WorkOrders> prodCardSB = SelectionBuilder.create(WorkOrders.class);
            prodCardSB.add(Conditions.eq(WorkOrders.META.idno, card_nr));
            card = QueryUtil.getFirst(ctx, prodCardSB.build());
            GlobalClass.ctxClose(ctx);
        } catch (DBRuntimeException e) {
            nrZP_TextView.setText("");
            article_TextView.setText("");
            artName_TextView.setText("");
            nrCard_TextEdit.setText("");
            catchExceptionCases(e, "CardNrExists", card_nr);
        }
        return card;
    }

    @SuppressLint("HandlerLeak")
    public void catchExceptionCases (DBRuntimeException e, String function, String parameter){
        GlobalClass.catchExceptionCases(e, this);
        if (e.getMessage().contains("FULL")) { //przekroczona liczba licencji
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
                    if(function.equals("CardNrExists")){
                        CardNrExists(parameter);
                    }
                }
            };
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(String content, WorkOrders card) {
        IsApPdcAnalysis terminalRequestAnalysis = ctx.openInfosystem(IsApPdcAnalysis.class);
        terminalRequestAnalysis.setYworkslipsel(card);
        terminalRequestAnalysis.setYordertime(true);
        terminalRequestAnalysis.setYstarted(true);
        terminalRequestAnalysis.setYfinished(false);
        terminalRequestAnalysis.invokeStart();

        Iterable<IsApPdcAnalysis.Row> terminalRequestAnalysisRows = terminalRequestAnalysis.getTableRows();
        Integer nrRows = terminalRequestAnalysis.getRowCount();
        if (nrRows != 0) {
            //wyświetl tabelkę z danymi
            AlertDialog.Builder controlAnalysisDialog = new AlertDialog.Builder(QualityControlReportNonCompliance.this);
            ViewGroup viewGroup = findViewById(android.R.id.content);
            View dialogView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_control_analysis, viewGroup, false);
            controlAnalysisDialog.setView(dialogView);
            AlertDialog controlDialog = controlAnalysisDialog.create();
            controlDialog.show();
            for (IsApPdcAnalysis.Row row : terminalRequestAnalysisRows) {
                controlDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                controlLayout = (TableLayout) controlDialog.findViewById(R.id.controlTable);

                //ustawianie wyglądu dla row
                TableRow tableRowControl = GlobalClass.setTableRowList(this);
                TextView employee_textViewTable = new TextView(this);
                TextView operation_textViewTable = new TextView(this);
                TextView machineGroup_textViewTable = new TextView(this);
                TextView[] textViewArray = {employee_textViewTable, operation_textViewTable, machineGroup_textViewTable};
                Integer j = controlLayout.getChildCount();
                for (TextView textView :textViewArray) {
                    if (j % 2 == 0) {
                        textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    } else {
                        textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    }
                }

                //Pracownik
                String employee = row.getYtemployee().getDescrOperLang();
                GlobalClass.setParamForTextView(employee_textViewTable, employee, 13, 20, 10, true);
                //employee_textViewTable.setTypeface(Typeface.DEFAULT_BOLD);

                // Grupa Maszyn
                String department = row.getYtemployeedeptdesc().getDescr6();
                String machine_group = ((WorkCenter)row.getYtworkcenter()).getDescr6();
                GlobalClass.setParamForTextView(machineGroup_textViewTable, machine_group, 13, 20, 10, false);

                //  Operacja
                String operation = row.getYoperation().getDescr6();
                GlobalClass.setParamForTextView(operation_textViewTable, operation, 13, 20, 10, false);

                for (TextView textView :textViewArray) {
                    tableRowControl.addView(textView);
                }
                controlLayout.addView(tableRowControl, j);
                tableRowControl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        controlDialog.dismiss();
                        choosenEmployee = employee;
                        choosenDepartment = department;
                        choosenOperation = operation;
                        choosenMachineGroup = machine_group;
                    }
                });
            }
        }else{ //sprawdza zgłoszenia zakończone
            IsApPdcAnalysis terminalRequestAnalysisFinished = ctx.openInfosystem(IsApPdcAnalysis.class);
            terminalRequestAnalysisFinished.setYworkslipsel(card);
            terminalRequestAnalysisFinished.setYordertime(true);
            terminalRequestAnalysisFinished.setYstarted(false);
            terminalRequestAnalysisFinished.setYfinished(true);
            terminalRequestAnalysisFinished.invokeStart();
            Iterable<IsApPdcAnalysis.Row> terminalRequestAnalysisFinishedRows = terminalRequestAnalysisFinished.getTableRows();
            Integer nrRowsFinished = terminalRequestAnalysisFinished.getRowCount();
            if (nrRowsFinished != 0) {
                GlobalClass.showDialog(this, "Zgłoszenie zakończone!", "Zgłoszenie zostało zakończone.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ctx.close();
                            }
                        });
                choosenEmployee = "brak danych - zgłoszenie zakończone";
                choosenDepartment = "brak danych - zgłoszenie zakończone";
                choosenOperation = "brak danych - zgłoszenie zakończone";
                choosenMachineGroup = "brak danych - zgłoszenie zakończone";

            }else{
                GlobalClass.showDialog(this, "Brak informacji!", "Brak informacji o danym zgłoszeniu.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ctx.close();
                            }
                        });
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void Send(View view) {
        LoadingDialog = ProgressDialog.show(QualityControlReportNonCompliance.this, "",
                "Ładowanie. Proszę czekać...", true);
        message = findViewById(R.id.message_MultiLine);
        user = (getIntent().getStringExtra("user"));
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        Date today = new Date();
        EditText nrCard_TextEdit = findViewById(R.id.machine_TextEdit);
        String nrCard_text = nrCard_TextEdit.getText().toString(),
                mess_text = message.getText().toString();
        Boolean emptyFields = checkIfFieldsEmpty(nrCard_text, mess_text);

        if(emptyFields == false){
            save_btn.setEnabled(false);
            appConfigValues = getAppConfigValues(); //podbieranie maili z abasa
            IsMailSender sender = ctx.openInfosystem(IsMailSender.class);
            if(choosenOperation.equalsIgnoreCase("Pakowanie")) {
                sender.setYto(appConfigValues.getYqualitycontrpack());
            }else{ // produkcja w toku
                sender.setYto(appConfigValues.getYqualitycontrprod());
            }
            sender.setYsubject("Nowa wiadomosc z kontroli jakosci produkcji!");
            String text = ("Dzień dobry! <br/> Użytkownik " + user + " wysłał w dniu " + dateFormat.format(today) + " następnującą wiadomość: <br/><p> " + mess_text + "</p>Karta pracy: " +  nrCard_TextEdit.getText()
                    +"<br/>Nr zlecenia produkcyjnego: " + nrZP_TextView.getText() + "<br/> Artykuł: " + article_TextView.getText() + "<br/> Nazwa artykułu: " + artName_TextView.getText()
                    + "</br></p><p> Pracownik: " + choosenEmployee + "<br/> Operacja: " + choosenOperation + "<br/> Grupa Maszyn: " + choosenMachineGroup + "<br/> Wydział: " + choosenDepartment + "</p>");
            sender.setYtrext(text);
            sender.invokeStart();
            sender.close();
            GlobalClass.showDialog(this, "Wysłano!", "Wiadomość została pomyślnie wysłana.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            new setIntentAsyncTask().execute("Menu");
                        }
                    });
            GlobalClass.ctxClose(ctx);
        }
        GlobalClass.dismissLoadingDialog(LoadingDialog);
    }

    public Boolean checkIfFieldsEmpty(String nrCard_text, String mess_text) {
        Boolean emptyFields = false;
        if (nrCard_text.equals("")) {
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak karty pracy!", "Proszę wprowadzić nr karty.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { }
                    });

        } else if (mess_text.equals("")) {
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak wiadomości!", "Proszę wpisać wiadomość.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { }
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