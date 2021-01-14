package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.abas.erp.common.type.AbasDate;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.custom.owfe.IsApPdcAnalysis;
import de.abas.erp.db.infosystem.custom.owpl.IsPrLoggedUser;
import de.abas.erp.db.schema.capacity.WorkCenter;
import de.abas.erp.db.schema.employee.Employee;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.pdc.ShortProductionOrderEditor;
import de.abas.erp.db.schema.workorder.WorkOrders;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class QualityControlProduction extends AppCompatActivity {
    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    DbContext ctx, sessionCtx;
    ProgressDialog LoadingDialog;
    EditText nrCard_TextEdit, controlledQty_textEdit, goodQty_textEdit, badQty_textEdit, message_MultiLine;
    TextView artName_TextView, article_TextView, nrZP_TextView, message;
    WorkOrders card;
    TableLayout controlLayout;
    String user, choosenEmployee, choosenDepartment, choosenOperation, choosenMachineGroup, database, machine_groupSWD, user_short_name, userSwd;
    Employee employee;
    BigDecimal controlledQty, goodQty, badQty;
    View save_btn;
    Handler handler;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality_control_production);
        getElementsFromIntent();
        getElementsById();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(QualityControlProduction.this, userSwd));
        save_btn.setEnabled(true);
    }

    // na kliknięcie cofnij
    public void onBackPressed() {
        super.onBackPressed();
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
        new setIntentAsyncTask().execute("QualityControl");
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        super.onStop();
        if(ctx != null) {
            ctx.close();
        }
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
    }

    @Override
    protected void onPause(){  //closes ctx if the app is minimized
        if(ctx != null) {
            ctx.close();
        }
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
        nrCard_TextEdit = findViewById(R.id.machine_TextEdit);
        artName_TextView = findViewById(R.id.artName_TextView);
        article_TextView = findViewById(R.id.articleQuality_TextView);
        nrZP_TextView = findViewById(R.id.nrZP_TextView);
        message = findViewById(R.id.message_MultiLine);
        controlledQty_textEdit = findViewById(R.id.controlledQty_textEdit);
        goodQty_textEdit = findViewById(R.id.goodQty_textEdit);
        badQty_textEdit = findViewById(R.id.badQty_textEdit);
        message_MultiLine = findViewById(R.id.message_MultiLine);
        save_btn = findViewById(R.id.save_btn);
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(QualityControlProduction.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loadDialog = ProgressDialog.show(QualityControlProduction.this, "",
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
            intent.putExtra("user", user);
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
        String nr_card = "";
        String article = "";
        String article_name = "";
        String nr_ZP = "";

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
            ctx.close();
        }
        else{
            GlobalClass.showDialog(this, "Brak karty pracy!", "Zeskanowany nr karty nie istnieje.", "OK",
            new DialogInterface.OnClickListener() {
                @Override  public void onClick(DialogInterface dialog, int which) { }
            });
        }
    }

    public WorkOrders CardNrExists(String card_nr) {
        card = null;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<WorkOrders> prodCardSB = SelectionBuilder.create(WorkOrders.class);
            prodCardSB.add(Conditions.eq(WorkOrders.META.idno, card_nr));
            card = QueryUtil.getFirst(ctx, prodCardSB.build());
            ctx.close();
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
        if(e.getMessage().contains("failed")){
            GlobalClass.showDialog(this,"Brak połączenia!","Nie można się aktualnie połączyć z bazą.", "OK",new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { } });
        }else if(e.getMessage().contains("FULL")){
            LoadingDialog = ProgressDialog.show(QualityControlProduction.this, "     Przekroczono liczbę licencji.",
                    "Zwalniam miejsce w ABAS. Proszę czekać...", true);
            new Thread(() -> {
                sessionCtx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", "sesje", "mobileApp");  // hasło sesje aby mieć dostęp
                GlobalClass.licenceCleaner(sessionCtx);
                sessionCtx.close();
                handler.sendEmptyMessage(0);
            }).start();
            handler = new Handler() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                public void handleMessage(Message msg) {
                LoadingDialog.dismiss();
                if(function.equals("CardNrExists")) {
                    CardNrExists(parameter);
                }else if(function.equals("save")){
                    save_btn.callOnClick();
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
            AlertDialog.Builder controlAnalysisDialog = new AlertDialog.Builder(QualityControlProduction.this);
            ViewGroup viewGroup = findViewById(android.R.id.content);
            View dialogView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_control_analysis, viewGroup, false);
            controlAnalysisDialog.setView(dialogView);
            AlertDialog controlDialog = controlAnalysisDialog.create();
            controlDialog.show();
            for (IsApPdcAnalysis.Row row : terminalRequestAnalysisRows) {
                controlDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                controlLayout = (TableLayout) controlDialog.findViewById(R.id.controlTable);

                //ustawianie wyglądu dla row
                TableRow tableRowControl = new TableRow(this);
                TableRow.LayoutParams rowParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                tableRowControl.setLayoutParams(rowParam);
                tableRowControl.setBackgroundColor(Color.parseColor("#BDBBBB"));

                //ustawianie wyglądu dla table cell
                TableRow.LayoutParams cellParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                cellParam.setMargins(1, 1, 1, 1);

                TextView employee_textViewTable = new TextView(this);
                TextView operation_textViewTable = new TextView(this);
                TextView machineGroup_textViewTable = new TextView(this);

                Integer j = controlLayout.getChildCount();

                if (j % 2 == 0) {  // zmiana koloru w rowach dla parzystych
                    employee_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    operation_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    machineGroup_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                } else {
                    employee_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    operation_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    machineGroup_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                }

                //Pracownik
                String employee = row.getYtemployee().getDescrOperLang();
                employee_textViewTable.setText(employee);
                employee_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                employee_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                employee_textViewTable.setTypeface(Typeface.DEFAULT_BOLD);
                employee_textViewTable.setPadding(10, 20, 10, 20);
                employee_textViewTable.setLayoutParams(cellParam);

                // Grupa Maszyn
                String department = row.getYtemployeedeptdesc().getDescr6();
                String machine_group = ((WorkCenter)row.getYtworkcenter()).getDescr6();
                machine_groupSWD = ((WorkCenter)row.getYtworkcenter()).getSwd();
                machineGroup_textViewTable.setText(machine_group);
                machineGroup_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                machineGroup_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                machineGroup_textViewTable.setPadding(10, 20, 10, 20);
                machineGroup_textViewTable.setLayoutParams(cellParam);

                //  Operacja
                String operation = row.getYoperation().getDescr6();
                operation_textViewTable.setText(operation);
                operation_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                operation_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                operation_textViewTable.setPadding(10, 20, 10, 20);
                operation_textViewTable.setLayoutParams(cellParam);

                tableRowControl.addView(employee_textViewTable);
                tableRowControl.addView(operation_textViewTable);
                tableRowControl.addView(machineGroup_textViewTable);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void save(View view){
        String nrCard_text = nrCard_TextEdit.getText().toString();
        Boolean emptyFields = checkIfFieldsEmpty(nrCard_text);
        if(emptyFields == false){
            save_btn.setEnabled(false);
            LoadingDialog = ProgressDialog.show(QualityControlProduction.this, "",
                    "Ładowanie. Proszę czekać...", true);
            try {
                ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
                AbasDate today = new AbasDate();
                DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                Date date = new Date();
                IsPrLoggedUser lu = ctx.openInfosystem(IsPrLoggedUser.class);
                user_short_name = lu.getYuser();
                employee = FindEmployeeBySwd(ctx, user_short_name);
                lu.close();
                ShortProductionOrderEditor shortProductionOrderEditor = ctx.newObject(ShortProductionOrderEditor.class);
                shortProductionOrderEditor.setString("employee", employee.getSwd());
                shortProductionOrderEditor.setString("wSlipNoText ", "  " + nrCard_TextEdit.getText().toString());
                shortProductionOrderEditor.setString("workCenter", machine_groupSWD);
                shortProductionOrderEditor.setStartTimePair(today);
                shortProductionOrderEditor.setString("startTime", dateFormat.format(date));
                shortProductionOrderEditor.setYqmqtychecked(controlledQty);
                shortProductionOrderEditor.setYqmqtygood(goodQty);
                shortProductionOrderEditor.setYqmqtyscrap(badQty);
                shortProductionOrderEditor.setYqmcomments(message_MultiLine.getText().toString());
                shortProductionOrderEditor.commit();

                GlobalClass.showDialog(this, "Dodano!", "Nowa kontrola produkcji została dodana do bazy.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ctx.close();
                                new setIntentAsyncTask().execute("QualityControl");
                            }
                        });
            } catch (NumberFormatException e) {
                e.printStackTrace();
                GlobalClass.showDialog(this, "Błąd!", "Podczas zmiany formatu wystąpił błąd.", "OK",
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {}
                        });
            } catch (DBRuntimeException e) {
                catchExceptionCases(e, "save", "");
            }
        }
        if(LoadingDialog != null){
            LoadingDialog.dismiss();
        }
    }

    public Boolean checkIfFieldsEmpty(String nrCard_text) {
        Boolean emptyFields = false;
        if (nrCard_text.equals("")){
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak karty pracy!", "Proszę wprowadzić nr karty.", "OK",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        }else if(controlledQty_textEdit.getText().toString().equals("") || ((goodQty_textEdit.getText().toString().equals("")) || (badQty_textEdit.getText().toString().equals("")))){
            emptyFields = true;
            GlobalClass.showDialog(this, "Brak wprowadzonej ilości!", "Proszę uzupełnić ilości.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        }else{
            controlledQty = new BigDecimal(controlledQty_textEdit.getText().toString());
            goodQty = new BigDecimal(goodQty_textEdit.getText().toString());
            badQty = new BigDecimal(badQty_textEdit.getText().toString());
            if ((goodQty.add(badQty)).compareTo(controlledQty) != 0) {
                emptyFields = true;
                GlobalClass.showDialog(this, "Niezgodność sum!", "Suma zgodnych i niezgodnych ilości się nie zgadza!", "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
            }
        }
        return  emptyFields;
    }

    public final Employee FindEmployeeBySwd(DbContext ctx, String name){
        Employee employee = null;
        SelectionBuilder<Employee> employeeSB = SelectionBuilder.create(Employee.class);
        try {
            employeeSB.add(Conditions.eq(Employee.META.swd, name));
            employee = QueryUtil.getFirst(ctx, employeeSB.build());
        } catch (Exception e) {
        }
        return employee;
    }
}
