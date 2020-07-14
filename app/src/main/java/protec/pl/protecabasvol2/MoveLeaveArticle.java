package protec.pl.protecabasvol2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.abas.erp.common.type.AbasDate;
import de.abas.erp.common.type.enums.EnumEntryTypeStockAdjustment;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorCommand;
import de.abas.erp.db.EditorCommandFactory;
import de.abas.erp.db.Query;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.infosystem.standard.la.StockLevelInformation;
import de.abas.erp.db.schema.location.LocationHeader;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.storagequantity.StockAdjustmentEditor;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;
import de.abas.erp.db.schema.infrastructure.Location;
import protec.pl.protecabasvol2.R;

import static protec.pl.protecabasvol2.GlobalClass.FindProductByIdno;

public class MoveLeaveArticle extends AppCompatActivity {
    private String password;
    public String getPassword() { return password;}
    public void setPassword(String password) {this.password = password; }
    DbContext ctx;
    ProgressDialog LoadingDialog;
    TextView article_textEdit;
    TextView location_textEdit;
    TextView unit_textView;
    TextView qty_textEdit;
    TextView location_textInfo;
    TextView qty_textInfo;
    TextView article_textInfo;
    TableLayout WDRlayout;
    String art_IDNO;
    GlobalClass globFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_leave_article);
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        article_textEdit = findViewById(R.id.article_textEdit);
        location_textEdit = findViewById(R.id.to_textEdit);
        qty_textEdit = findViewById(R.id.qty_TOtextEdit);
        unit_textView = findViewById(R.id.unit_textView);
        location_textInfo = findViewById(R.id.location_textInfo);
        qty_textInfo = findViewById(R.id.qty_textInfo);
        article_textInfo = findViewById(R.id.article_textInfo);
        unit_textView.setVisibility(View.INVISIBLE);
        location_textInfo.setVisibility(View.INVISIBLE);
        qty_textInfo.setVisibility(View.INVISIBLE);
        article_textInfo.setVisibility(View.INVISIBLE);
        article_textEdit.setInputType(0);
        location_textEdit.setInputType(0);
    }
    // na kliknięcie cofnij
    public void onBackPressed(){
        super.onBackPressed();
        Intent intent = new Intent(MoveLeaveArticle.this, Move.class);
        intent.putExtra("password", getPassword());
        startActivity(intent);
    }
    // na wyjście z actvity
    @Override
    protected void onStop(){
        super.onStop();
        if (LoadingDialog != null){
            LoadingDialog.dismiss();
        }
    }

    public void scanArticle(View view){
        try{
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować artykuł");
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

    public void scanLocation(View view){
        try{
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować lokalizację");
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
            if (requestCode == 101) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    LoadingDialog = ProgressDialog.show(MoveLeaveArticle.this, "",
                            "Ładowanie. Proszę czekać...", true);
                    checkWDRStock(content);
                    article_textInfo.setVisibility(View.VISIBLE);
                    location_textInfo.setVisibility(View.VISIBLE);
                    qty_textInfo.setVisibility(View.VISIBLE);
                }
            }
            if (requestCode == 70) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    String location_name = "";
                    if(LocationExists(content) != null){
                        location_name = LocationExists(content).getSwd();
                        location_textEdit.setText(location_name);
                        article_textInfo.setVisibility(View.VISIBLE);
                        location_textInfo.setVisibility(View.VISIBLE);
                        qty_textInfo.setVisibility(View.VISIBLE);
                    }else{
                        location_textEdit.setText("");
                    }
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    public LocationHeader LocationExists(String location){
        LocationHeader loc = null;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
            SelectionBuilder<LocationHeader> locationSB = SelectionBuilder.create(LocationHeader.class);
            locationSB.add(Conditions.eq(LocationHeader.META.idno, location));
            loc = QueryUtil.getFirst(ctx, locationSB.build());
        }catch (Exception e) {
            GlobalClass.showDialog(this, "Brak lokalizacji!", "Zeskanowana lokalizacja nie istnieje.", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
        }
        return loc;
    }

    // CHECK WDR STOCK
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("WrongViewCast")
    public void checkWDRStock(String content) {
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
            String articleSWD;
            String qty;
            String unit;
            StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
            sli.setString("klplatz", "WDR");
            sli.setNullmge(false);
            if (FindProductByIdno(ctx, content) != null) {
                sli.setArtikel(FindProductByIdno(ctx, content));
                sli.invokeStart();
                Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
                Integer nrRows = sli.getRowCount();
                if (nrRows != 0) {
                    for (StockLevelInformation.Row row : sliRows) {
                        articleSWD = row.getTartikel().getSwd();
                        qty = row.getLemge().stripTrailingZeros().toPlainString();
                        unit = row.getLeinheit().toString();
                        if (unit.equals("(20)")) { // jeśli jednostka to szt.
                            unit_textView.setText("szt.");
                            unit = "szt.";
                        } else if (unit.equals("(7)")) { //jeśli jednostka to kg
                            unit_textView.setText("kg");
                            unit = "kg";
                        } else if (unit.equals("(21)")) { // jeśli jednostka to kpl
                            unit_textView.setText("kpl");
                            unit = "kpl";
                        }
                        article_textEdit.setText(articleSWD);
                        unit_textView.setVisibility(View.VISIBLE);
                        unit_textView.setText(unit);
                        qty_textEdit.setHint(qty);
                    }
                } else {// na WDR nie ma takiego artykułu
                    GlobalClass.showDialog(this, "Brak artykułu w WDR!", "W lokalizacji WDR nie ma takiego artykułu.", "OK",
                        new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {} });
                }
            } else { // FindProdByIdno zwrócił null
                GlobalClass.showDialog(this, "Nie zeskanowano artykułu!", "Proszę zeskanować nr ID artykułu.", "OK",
                    new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {} });
            }
            LoadingDialog.dismiss();
        }catch (DBRuntimeException e){
            LoadingDialog.dismiss();
            GlobalClass.showDialog(this, "Brak połączenia!", "Nie można aktualnie połączyć z bazą.", "OK",
                    new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {} });
        }
    }
    @SuppressLint("WrongViewCast")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void chooseArticle(View view){
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
        StockLevelInformation sli = ctx.openInfosystem(StockLevelInformation.class);
        sli.setString("klplatz", "WDR");
        sli.setNullmge(false);
        sli.invokeStart();
        Iterable<StockLevelInformation.Row> sliRows = sli.getTableRows();
        Integer nrRows = sli.getRowCount();
        if (nrRows != 0) {
            AlertDialog.Builder WDRstockDialog = new AlertDialog.Builder(MoveLeaveArticle.this);
            ViewGroup viewGroup = findViewById(android.R.id.content);
            View dialogView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_wdr_stock, viewGroup, false);
            WDRstockDialog.setView(dialogView);
            AlertDialog wdrDialog = WDRstockDialog.create();
            wdrDialog.show();
            for (StockLevelInformation.Row row : sliRows) {
                wdrDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                WDRlayout = (TableLayout) wdrDialog.findViewById(R.id.wdrkNameTable);

                //ustawianie wyglądu dla row
                TableRow tableRowWDR = new TableRow(this);
                TableRow.LayoutParams rowParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                tableRowWDR.setLayoutParams(rowParam);
                tableRowWDR.setBackgroundColor(Color.parseColor("#BDBBBB"));

                //ustawianie wyglądu dla table cell
                TableRow.LayoutParams cellParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                cellParam.setMargins(1, 1, 1, 1);

                TextView article_textViewTable = new TextView(this);
                //TextView art_desc_textViewTable = new TextView(this);
                TextView qty_textViewTable = new TextView(this);
                TextView unit_textViewTable = new TextView(this);

                Integer j = WDRlayout.getChildCount();

                if (j % 2 == 0) {  // zmiana koloru w rowach dla parzystych
                    article_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    qty_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    unit_textViewTable.setBackgroundColor(Color.parseColor("#FFFFFF"));
                } else {
                    article_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    qty_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                    unit_textViewTable.setBackgroundColor(Color.parseColor("#E5E5E6"));
                }

                //Artykuł
                String art = row.getTartikel().getSwd();
                article_textViewTable.setText(art);
                article_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                article_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                article_textViewTable.setTypeface(Typeface.DEFAULT_BOLD);
                article_textViewTable.setPadding(5, 10, 5, 10);
                article_textViewTable.setLayoutParams(cellParam);
                // Nazwa artkyłu
                String art_descr = row.getTartikel().getDescr6();
                // Nazwa artkyłu
                art_IDNO = row.getTartikel().getIdno();
                //  Jednostka
                // Ilość i jednostka .setText na podstawie szt. kg kpl
                String unit = row.getLeinheit().toString();
                if (unit.equals("(20)")) { // jeśli jednostka to szt.
                    unit_textViewTable.setText("szt.");
                    unit = "szt.";
                } else if (unit.equals("(7)")) { //jeśli jednostka to kg
                    unit_textViewTable.setText("kg");
                    unit = "kg";
                } else if (unit.equals("(21)")) { // jeśli jednostka to kpl
                    unit_textViewTable.setText("kpl");
                    unit = "kpl";
                }
                unit_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                unit_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                unit_textViewTable.setPadding(5, 10, 5, 10);
                unit_textViewTable.setLayoutParams(cellParam);
                //  Ilość
                String qty = row.getLemge().stripTrailingZeros().toPlainString();  //by po przecinku usunąć niepotrzebne zera
                qty_textViewTable.setText(qty);
                qty_textViewTable.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                qty_textViewTable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                qty_textViewTable.setPadding(5, 10, 5, 10);
                qty_textViewTable.setLayoutParams(cellParam);

                tableRowWDR.addView(article_textViewTable);
                //tableRowWDR.addView(art_desc_textViewTable);
                tableRowWDR.addView(qty_textViewTable);
                tableRowWDR.addView(unit_textViewTable);
                WDRlayout.addView(tableRowWDR, j);

                String finalUnit = unit;
                tableRowWDR.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        wdrDialog.dismiss();
                        AlertDialog.Builder choosenArticleAlert = new AlertDialog.Builder(MoveLeaveArticle.this);
                        String mge = row.getLemge().toString();
                        String articleString = "<b>" + art + "</b><br/>" + art_descr + "<br/>" + qty + " " + finalUnit;
                        choosenArticleAlert.setMessage(Html.fromHtml(articleString));
                        choosenArticleAlert.setTitle("Wybrany artykuł: ");
                        choosenArticleAlert.setPositiveButton("Wybierz artykuł",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        //dismiss the dialog
                                       // article_textEdit.setFocusable(false);
                                        article_textEdit.setInputType(0);
                                        article_textEdit.setText(art);
                                        unit_textView.setText(finalUnit);
                                        qty_textEdit.setHint(qty);
                                        unit_textView.setVisibility(View.VISIBLE);
                                        article_textInfo.setVisibility(View.VISIBLE);
                                        location_textInfo.setVisibility(View.VISIBLE);
                                        qty_textInfo.setVisibility(View.VISIBLE);
                                    }
                                });
                        choosenArticleAlert.setNegativeButton("Anuluj",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        //dismiss the dialog
                                        wdrDialog.show();
                                    }
                                });
                        choosenArticleAlert.setCancelable(true);
                        choosenArticleAlert.create().show();
                    }
                });
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void leaveArticles(View view) {
        String qty = qty_textEdit.getText().toString();
        String location = location_textEdit.getText().toString();
        String article = article_textEdit.getText().toString();
        globFunctions = new GlobalClass(getApplicationContext());
        if (article.equals("")) {
            GlobalClass.showDialog(this, "Brak artykułu!", "Proszę wprowadzić artykuł", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        } else if (location.equals("")) {
            GlobalClass.showDialog(this, "Brak lokalizacji!", "Proszę wprowadzić lokalizację", "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        } else {
            if (qty.equals("")) {
                qty = qty_textEdit.getHint().toString();
                qty_textEdit.setText(qty);
                //jeśli ilość wpisana jest WIĘKSZA niż na stanie
                if (Double.parseDouble(qty) > Double.parseDouble(qty_textEdit.getHint().toString())) {
                    GlobalClass.showDialog(this, "Wykroczenie poza stan!", "Wpisana ilość przekracza ilość dostępną na stanie.", "OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                } else {
                    try {
                        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "test", getPassword(), "mobileApp");
                        EditorCommand cmd = EditorCommandFactory.typedCmd("Lbuchung", "");
                        StockAdjustmentEditor stockAdjustmentEditor = (StockAdjustmentEditor) ctx.openEditor(cmd);
                        AbasDate today = new AbasDate();
                        Product art = globFunctions.FindProductBySwd(ctx, article);
                        stockAdjustmentEditor.setString("product", art.getIdno());
                        stockAdjustmentEditor.setDocNo("MOBILE");
                        stockAdjustmentEditor.setDateDoc(today);
                        stockAdjustmentEditor.setEntType(EnumEntryTypeStockAdjustment.Transfer);
                        StockAdjustmentEditor.Row sadRow = stockAdjustmentEditor.table().getRow(1);
                        sadRow.setUnitQty(Double.parseDouble(qty));
                        sadRow.setString("location", "WDR");
                        sadRow.setString("location2", location);
                        stockAdjustmentEditor.commit();

                        GlobalClass.showDialog(this, "Odłożono!", "Materiał został odłożony i dodany do bazy.", "OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(MoveLeaveArticle.this, Move.class);
                                        intent.putExtra("password", password);
                                        startActivity(intent);
                                    }
                                });
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        GlobalClass.showDialog(this, "Błąd!", "Podczas zmiany formatu wystąpił błąd.", "OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                    } catch (DBRuntimeException e) {
                        GlobalClass.showDialog(this, "Brak połączenia!", "Nie można aktualnie połączyć z bazą.", "OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                        e.printStackTrace();
                    } catch (CommandException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
}