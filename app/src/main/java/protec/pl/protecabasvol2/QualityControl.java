package protec.pl.protecabasvol2;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import de.abas.erp.db.infosystem.custom.owfe.IsApPdcAnalysis;
import de.abas.erp.db.infosystem.custom.owpl.IsMailSender;
import de.abas.erp.db.schema.capacity.WorkCenter;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.workorder.WorkOrders;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class QualityControl extends AppCompatActivity {
    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    DbContext ctx;
    ProgressDialog LoadingDialog;
    EditText nrCard_TextEdit;
    TextView artName_TextView, article_TextView, nrZP_TextView, message;
    WorkOrders card;
    TableLayout controlLayout;
    String user, choosenEmployee, choosenDepartment, choosenOperation, choosenMachineGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality_control);
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
    }

    // na kliknięcie cofnij
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(QualityControl.this, Menu.class);
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        super.onStop();
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
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
        nrCard_TextEdit = findViewById(R.id.nrCard_TextEdit);
        artName_TextView = findViewById(R.id.artName_TextView);
        article_TextView = findViewById(R.id.articleQuality_TextView);
        nrZP_TextView = findViewById(R.id.nrZP_TextView);

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

        }
    }

    public WorkOrders CardNrExists(String card_nr) {
        card = null;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
            SelectionBuilder<WorkOrders> prodCardSB = SelectionBuilder.create(WorkOrders.class);
            prodCardSB.add(Conditions.eq(WorkOrders.META.idno, card_nr));
            card = QueryUtil.getFirst(ctx, prodCardSB.build());
        } catch (Exception e) {
            GlobalClass.showDialog(this, "Brak karty pracy!", "Zeskanowany nr karty nie istnieje.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            //LoadingDialog.dismiss();
        }
        return card;
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
                AlertDialog.Builder controlAnalysisDialog = new AlertDialog.Builder(QualityControl.this);
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
                    employee_textViewTable.setPadding(5, 10, 5, 10);
                    employee_textViewTable.setLayoutParams(cellParam);

                    // Grupa Maszyn
                    String department = row.getYtemployeedeptdesc().getDescr6();
                    String machine_group = ((WorkCenter)row.getYtworkcenter()).getDescr6();
                    machineGroup_textViewTable.setText(machine_group);
                    machineGroup_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    machineGroup_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                    machineGroup_textViewTable.setPadding(5, 10, 5, 10);
                    machineGroup_textViewTable.setLayoutParams(cellParam);

                    //  Operacja
                    String operation = row.getYoperation().getDescr6();
                    operation_textViewTable.setText(operation);
                    operation_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    operation_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                    operation_textViewTable.setPadding(5, 10, 5, 10);
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

                            AlertDialog.Builder choosenElementAlert = new AlertDialog.Builder(QualityControl.this);
                            String elementeString = "<b>Pracownik: " + choosenEmployee + "</b><br/> Operacja: " + choosenOperation + "<br/> Grupa Maszyn:" + choosenMachineGroup
                                    + "<br/> Wydział:" + choosenDepartment;
                            choosenElementAlert.setMessage(Html.fromHtml(elementeString));
                            choosenElementAlert.setTitle("Wybrany element: ");
                            choosenElementAlert.setPositiveButton("Wybierz",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //dismiss the dialog
                                }
                            });
                            choosenElementAlert.setNegativeButton("Anuluj",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //dismiss the dialog
                                    controlDialog.show();
                                }
                            });
                            choosenElementAlert.setCancelable(true);
                            choosenElementAlert.create().show();
                        }
                    });
                }
        }else{
            GlobalClass.showDialog(this, "Brak parcownika w terminalu!", "Pracownik nie odbił się i nie widnieje w systemie.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void Send(View view) {
        LoadingDialog = ProgressDialog.show(QualityControl.this, "",
                "Ładowanie. Proszę czekać...", true);
        message = findViewById(R.id.message_MultiLine);
        String mess_text = message.getText().toString();
        user = (getIntent().getStringExtra("user"));
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        Date today = new Date();
        EditText nrCard_TextEdit = findViewById(R.id.nrCard_TextEdit);
        String nrCard_text = nrCard_TextEdit.getText().toString();
        if (nrCard_text.equals("")) {
            GlobalClass.showDialog(this, "Brak karty pracy!", "Proszę wprowadzić nr karty.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

        } else if (!mess_text.equals("")) {

            IsMailSender sender = ctx.openInfosystem(IsMailSender.class);
            if(choosenOperation.equalsIgnoreCase("Pakowanie")) {
                sender.setYto("lukasz.smiarowski@protec.pl;krzysztof.grzonka@protec.pl;koordynator.produkcji@protec.pl;krystian.skrzypiec@protec.pl;kj2@protec.pl;kj1@protec.pl;magazyn-log@protec.pl;");  //odbiorcy z wydziału pakowania
                //krzysztof.wolny@protec.pl
            }else{ // produkcja w toku
                sender.setYto("adrian.smieszkol@protec.pl;krzysztof.grzonka@protec.pl;koordynator.produkcji@protec.pl;produkcja1@protec.pl;krystian.skrzypiec@protec.pl;kj2@protec.pl;kj1@protec.pl");
            }

            sender.setYsubject("Nowa wiadomosc z kontroli jakosci produkcji!");
            String text = ("Dzień dobry! <br/> Użytkownik " + user + " wysłał w dniu " + dateFormat.format(today) + " następnującą wiadomość: <br/><p> " + mess_text + "</p>Karta pracy: " +  nrCard_TextEdit.getText()
                    +"<br/>Nr zlecenia produkcyjnego: " + nrZP_TextView.getText() + "<br/> Artykuł: " + article_TextView.getText() + "<br/> Nazwa artykułu: " + artName_TextView.getText()
                    + "</br></p><p> Pracownik: " + choosenEmployee + "<br/> Operacja: " + choosenOperation + "<br/> Grupa Maszyn: " + choosenMachineGroup + "<br/> Wydział: " + choosenDepartment + "</p>");
            sender.setYtrext(text);
            sender.invokeStart();
            sender.close();
            GlobalClass.showDialog(this, "Wysłano!", "Wiadomość została pomyślnie wysłana", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(QualityControl.this, Menu.class);
                            intent.putExtra("password", getPassword());
                            startActivity(intent);
                        }
                    });
            LoadingDialog.dismiss();
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
}
