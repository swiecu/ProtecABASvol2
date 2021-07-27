package protec.pl.protecabasvol2;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.webianks.library.scroll_choice.ScrollChoice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import de.abas.erp.common.type.AbasDate;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorAction;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.schema.custom.protec.MachineInspection;
import de.abas.erp.db.schema.custom.protec.MachineInspectionEditor;
import de.abas.erp.db.schema.employee.Employee;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class MaintenanceService extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx, sessionCtx;
    ProgressDialog LoadingDialog;
    TableLayout layout;
    TableRow no_art;
    GlobalClass globFunctions;
    String database, userSwd;
    Handler handler;
    Intent intent;
    EditText machine_textEdit, infoAboutService_multiLine;
    TextView machineName_textView, serviceTime_textView;
    Button scanMachine_btn;  ScrollChoice scrollChoiceHours, scrollChoiceMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_service);
        getElementsById();
        setLook();
        getElementsFromIntent();
        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MaintenanceService.this, userSwd));
        setPassword(password);
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        GlobalClass.dismissLoadingDialog(LoadingDialog);
        super.onStop();
    }

    //na cofnięcie Back do tyłu
    @Override
    public void onBackPressed() {
        new setIntentAsyncTask().execute("Maintenance", "");
        super.onBackPressed();
    }

    @Override
    protected void onPause() {  //closes ctx if the app is minimized
        GlobalClass.ctxClose(ctx);
        super.onPause();
    }

    private class setIntentAsyncTask extends AsyncTask<String, Void, String> {
        private ProgressDialog loadDialog = new ProgressDialog(MaintenanceService.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadDialog = ProgressDialog.show(MaintenanceService.this, "",
                    "Ładowanie. Proszę czekać...", true);
        }

        @Override
        protected String doInBackground(String... strings) {
            String destination = strings[0],
                    content = strings[1];
            setIntent(destination, content);
            return null;
        }

        protected void onPostExecute(String param) {
            startActivity(intent);
        }
    }

    public void setIntent(String destination, String content) {
        try {
            intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("content", content);
            // intent.putExtra("destination", "StockInformation");
            intent.putExtra("userSwd", userSwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void getElementsById() {
        layout = (TableLayout) findViewById(R.id.serviceTable);
        machine_textEdit = findViewById(R.id.machine_textEdit);
        machineName_textView = findViewById(R.id.machineName_textView);
        serviceTime_textView = findViewById(R.id.serviceTime_textView);
        infoAboutService_multiLine = findViewById(R.id.infoAboutService_multiLine);
        scanMachine_btn = findViewById(R.id.scanMachine_btn);
        no_art = findViewById(R.id.no_articles);
    }

    public void setLook(){
    }

    public void setScrollChoice(ScrollChoice scrollChoiceHours, ScrollChoice scrollChoiceMinutes){
        List<String> hours = new ArrayList<>();
        List<String> minutes = new ArrayList<>();
        for(int hour=0; hour<=99; hour++){
            hours.add(String.valueOf(hour));
        }
        for (int minute=0; minute<=59; minute++){
            minutes.add(String.valueOf(minute));
        }
        scrollChoiceHours.addItems(hours,0);
        scrollChoiceHours.setOnItemSelectedListener(new ScrollChoice.OnItemSelectedListener() {
            @Override
            public void onItemSelected(ScrollChoice scrollChoice, int position, String name) {}}
        );

        scrollChoiceMinutes.addItems(minutes,0);
        scrollChoiceMinutes.setOnItemSelectedListener(new ScrollChoice.OnItemSelectedListener() {
           @Override
           public void onItemSelected(ScrollChoice scrollChoice, int position, String name) {}}
        );
    }

    public void getElementsFromIntent() {
        password = (getIntent().getStringExtra("password"));
        database = (getIntent().getStringExtra("database"));
        userSwd = getIntent().getStringExtra("userSwd");
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        GlobalClass.ctxClose(ctx);
    }

    public void scanMachineInspection(View view){
        try{
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować maszynę");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            Intent intent = integrator.createScanIntent();
            startActivityForResult(intent , 70);
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
            if (requestCode == 70) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    loadTable(content);
                }else{
                    Log.d("result not ok", "not ok");
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void resetTextViews(){
        machine_textEdit.setText("");
        machineName_textView.setText("");
        serviceTime_textView.setText("");
        infoAboutService_multiLine.setText("");
    }

    public void setVisibility(){
        machineName_textView.setVisibility(View.VISIBLE);
        serviceTime_textView.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void loadTable(String machine) {
        String machineSwd = "", machineName = "", nextInspectionDate = "", serviceInfo = "";
        MachineInspection machineInspection = machineInspectionExists(machine);
        if (machineInspection != null) {
            machineSwd = machineInspection.getSwd();
            machineName = machineInspection.getYmachinename().getDescrOperLang();
            nextInspectionDate = machineInspection.getYnextinspection().toString();
            nextInspectionDate = getCorrectDateFormat(nextInspectionDate);
            serviceInfo = machineInspection.getYinspectioninfo();
            GlobalClass.ctxClose(ctx);
            machine_textEdit.setText(machineSwd);
            machineName_textView.setText(machineName);
            serviceTime_textView.setText(nextInspectionDate);
            infoAboutService_multiLine.setText(serviceInfo);
            setVisibility();
            drawTable(machineInspection);
        }else{
            resetTextViews();
            layout.removeViews(1, layout.getChildCount()-1);
            no_art.setVisibility(View.VISIBLE);
            layout.addView(no_art, 1);
            GlobalClass.showDialog(this, "Brak obiektu serwisu!", "Zeskanowany Obiekt Serwisu nie istnieje. Należy go utworzyć w ABAS.", "OK", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {} });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void drawTable(MachineInspection machineInspection){
        layout.removeViews(1, layout.getChildCount()-1);

        Iterable<MachineInspection.Row> machineInspectionRows = machineInspection.getTableRows();
        Integer nrRows = machineInspection.getRowCount();
        Log.d("getRowCount" , String.valueOf(machineInspection.getRowCount()));
        if (nrRows != 0) {
            no_art.setVisibility(View.INVISIBLE);
            for (MachineInspection.Row row : machineInspectionRows) {
                TableRow tableRow = GlobalClass.setTableRowList(this);
                TextView serviceDate_textViewTable = new TextView(this);
                TextView employee_textViewTable = new TextView(this);
                TextView duration_textViewTable = new TextView(this);
                TextView comment_textViewTable = new TextView(this);
                TextView[] textViewArray = {serviceDate_textViewTable, employee_textViewTable, duration_textViewTable, comment_textViewTable};

                Integer j = layout.getChildCount();
                Log.d("j", j.toString());
                j = j - 1; //  table header nie ma być brany pod uwagę więc -1
                for (TextView textView :textViewArray) {
                    if (j % 2 == 0) {
                        textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    } else {
                        textView.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    }
                }

                // serviceDate
                String inspectionDate = getCorrectDateFormat(row.getYtdateinspection().toString());
                GlobalClass.setParamForTextView(serviceDate_textViewTable, inspectionDate, 13, 20, 10, false);

                //employee_textViewTable
                GlobalClass.setParamForTextView(employee_textViewTable, row.getYtworker().getDescrOperLang(), 13, 20, 10, false);

                //duration_textViewTable
                GlobalClass.setParamForTextView(duration_textViewTable, row.getYttime().toString(), 13, 20, 10, false);

                //comment_textViewTable
                String comment = row.getYtcomment();
                if(comment.equals("")){ comment = "-"; }
                GlobalClass.setParamForTextView(comment_textViewTable, comment, 13, 20, 10, false);

                for (TextView textView :textViewArray) {
                    tableRow.addView(textView);
                }
                layout.addView(tableRow, j+1);
            }
        }else{
            no_art.setVisibility(View.VISIBLE);
            layout.addView(no_art, 1);

        }
        GlobalClass.ctxClose(ctx);
    }

    public String getCorrectDateFormat(String nextInspectionDate){
        String year = nextInspectionDate.substring(0,4),
                month = nextInspectionDate.substring(4,6),
                day = nextInspectionDate.substring(6,8);
        nextInspectionDate = day + "." + month + "." + year;
        return nextInspectionDate;
    }

    public void addNewService(View view){
        Boolean machineIsScanned = checkIfMachineIsScanned();
        if(machineIsScanned) {
            String machineSwd = machine_textEdit.getText().toString();
            AlertDialog.Builder enterServiceDialog = new AlertDialog.Builder(MaintenanceService.this);
            ViewGroup viewGroup = findViewById(android.R.id.content);
            View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_new_service, viewGroup, false);
            enterServiceDialog.setView(dialogView);
            AlertDialog serviceDialog = enterServiceDialog.create();
            Button button_cancel = (Button) dialogView.findViewById(R.id.cancel_btn);
            scrollChoiceHours = (ScrollChoice) dialogView.findViewById(R.id.scroll_choice_hours);
            scrollChoiceMinutes = (ScrollChoice) dialogView.findViewById(R.id.scroll_choice_minutes);
            setScrollChoice(scrollChoiceHours, scrollChoiceMinutes);
            button_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    serviceDialog.dismiss();
                }
            });
            Button button_ok = (Button) dialogView.findViewById(R.id.add_btn);
            button_ok.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                @Override
                public void onClick(View v) {
                    EditText comment = serviceDialog.findViewById(R.id.comment_TextEdit);
                    String commentString = comment.getText().toString();
                    serviceDialog.dismiss();
                    LoadingDialog = ProgressDialog.show(MaintenanceService.this, "",
                            "Ładowanie. Proszę czekać...", true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (scrollChoiceHours.getCurrentSelection().equals("0") && (scrollChoiceMinutes.getCurrentSelection().equals("0"))) {
                            GlobalClass.showDialog(MaintenanceService.this, "Za krótki czas serwisu!", "Przegląd musi trwać minimum 1 min.", "OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    serviceDialog.show();
                                    GlobalClass.dismissLoadingDialog(LoadingDialog);
                                }
                            });
                        } else {
                            addNewServiceToAbas(machineSwd, commentString, scrollChoiceHours.getCurrentSelection(), scrollChoiceMinutes.getCurrentSelection());

                        }
                    }
                }
            });
            serviceDialog.show();
            serviceDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public Boolean checkIfMachineIsScanned(){
        Boolean machineIsScanned = false;
        if(machine_textEdit.getText().length() == 0){
            GlobalClass.showDialog(this, "Nie zeskanowano maszyny!", "Aby dodać nowy przegląd należy zeskanować maszynę.", "OK", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {} });
        }else{
            machineIsScanned = true;
        }
        return machineIsScanned;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void addNewServiceToAbas(String machineSwd, String comment, String chosenHours, String chosenMinutes){
        ///jakiś kod który dodaje
        Employee employee = FindEmployeeBySwd(userSwd);
        MachineInspection machineInspection = machineInspectionExists(machineSwd);
        //resetTableProtectionOnMachineInspection(machineInspection);
        MachineInspectionEditor machineInspectionEditor = machineInspection.createEditor();
        AbasDate today = new AbasDate();
        try {
            machineInspectionEditor.open(EditorAction.UPDATE);
            machineInspectionEditor.setYprotect(true);
            machineInspectionEditor.table().appendRow();
            MachineInspectionEditor.Row newRow = machineInspectionEditor.table().getRow(machineInspectionEditor.table().getRowCount());
            newRow.setYtdateinspection(today);
            newRow.setYtworker(employee);
            newRow.setYtworkername(employee);
            newRow.setYtcomment(comment);
            //newRow.setDuration(chosenHours + " godz. " + chosenMinutes + "min");
           // Log.d("test " , chosenHours + " godz. " + chosenMinutes + "min");
            String newNextServiceDate = getNextServiceDate(machineInspectionEditor);
            machineInspectionEditor.setString("ynextinspection", newNextServiceDate);
            machineInspectionEditor.setYprotect(false);
        } catch (CommandException e) {
            e.printStackTrace();
        }
        machineInspectionEditor.commit();
        if(machineInspectionEditor.active()){
            machineInspectionEditor.abort();
        }

        GlobalClass.dismissLoadingDialog(LoadingDialog);
        GlobalClass.showDialog(MaintenanceService.this, "Dodano!", "Nowy serwis został dodany.", "OK",
                new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        loadTable(machineSwd);
                    } });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getNextServiceDate(MachineInspectionEditor machineInspectionEditor){
        AbasDate nextInspectionDate = machineInspectionEditor.getYnextinspection();
        LocalDate nextInspectionLocalDate = LocalDate.parse(nextInspectionDate.toString(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        nextInspectionLocalDate = nextInspectionLocalDate.plusDays(machineInspectionEditor.getYfrequencyday());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedNextServiceDate= nextInspectionLocalDate.format(formatter);
        return formattedNextServiceDate;
    }

    public final Employee FindEmployeeBySwd(String name){
        Employee employee = null;
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        SelectionBuilder<Employee> employeeSB = SelectionBuilder.create(Employee.class);
        try {
            employeeSB.add(Conditions.eq(Employee.META.swd.toString(), name));
            employee = QueryUtil.getFirst(ctx, employeeSB.build());
        } catch (Exception e) {
            Log.d("getMessage", e.getMessage());
        }
        return employee;
    }

    public MachineInspection machineInspectionExists(String machine){
        MachineInspection machineInspection = null;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<MachineInspection> machineInspectionSB = SelectionBuilder.create(MachineInspection.class);
            machineInspectionSB.add(Conditions.eq(MachineInspection.META.swd, machine));
            machineInspection = QueryUtil.getFirst(ctx, machineInspectionSB.build());
            GlobalClass.ctxClose(ctx);
        }catch (Exception e) {
            Log.d("error", e.getMessage());
        }
        return machineInspection;
    }
}