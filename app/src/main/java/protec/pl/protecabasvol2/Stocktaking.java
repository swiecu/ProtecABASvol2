package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorAction;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.schema.custom.protec.StocktakingProtec;
import de.abas.erp.db.schema.custom.protec.StocktakingProtecEditor;
import de.abas.erp.db.schema.location.LocationHeader;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;

public class Stocktaking extends AppCompatActivity {
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx;
    ProgressDialog LoadingDialog;
    String database, stockID, back_article;
    CheckBox lockIcon;
    TextView unit, test, article_textInfo, location_textInfo, qty_textInfo, info_textInfo;
    EditText article_textEdit, location_textEdit, qty_textEdit, info_textEdit;
    GlobalClass myGlob;
    StocktakingProtec stocktaking = null;
    Boolean isHandWritten;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stocktaking);
        getElementsFromIntent();
        getElementsById();
        setLook();
        stocktaking = getStocktaing();
        Log.d("ycuurLockation", stocktaking.getYcurrlocation());
        Log.d("idno", stocktaking.getSwd());
       if(!stocktaking.getYcurrlocation().equals("")){
            lockIcon.setChecked(true);
            location_textEdit.setHint(stocktaking.getYcurrlocation());
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_closed_icon);   //OK
        }else{
            lockIcon.setChecked(false);
            location_textEdit.setHint("Lokalizacja");
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_icon);
        }
        ctx.close();
        //jeśli wraca z ArticleNameList
        if(back_article != null) {
            String password = (getIntent().getStringExtra("password"));
            setPassword(password);
            LoadingDialog = ProgressDialog.show(Stocktaking.this, "",
                    "Ładowanie. Proszę czekać...", true);
            isHandWritten = true;
            searchArticle(back_article);
        }

    }

    public void onBackPressed() {
        super.onBackPressed();
        setIntent("Menu", "", "");
    }

    // na wyjście z actvity
    @Override
    protected void onStop() {
        super.onStop();
        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
    }

    public void getElementsById() {
        lockIcon = (CheckBox) findViewById(R.id.lockIcon);
        unit = (TextView) findViewById(R.id.unit_textView);
        test = (TextView) findViewById(R.id.stockInfo_textView);
        article_textEdit = (EditText) findViewById(R.id.article_textEdit);
        location_textEdit = (EditText) findViewById(R.id.to_textEdit);
        qty_textEdit = (EditText) findViewById(R.id.qty_TOtextEdit);
        article_textInfo = (TextView) findViewById(R.id.article_textInfo);
        location_textInfo = (TextView) findViewById(R.id.locationFrom_textInfo);
        qty_textInfo = (TextView) findViewById(R.id.qty_textInfo);
        info_textInfo = (TextView) findViewById(R.id.info_textInfo);
        info_textEdit= (EditText) findViewById(R.id.info_textEdit);
    }

    public void setLook() {
        unit.setVisibility(View.INVISIBLE);
        article_textInfo.setVisibility(View.INVISIBLE);
        location_textInfo.setVisibility(View.INVISIBLE);
        qty_textInfo.setVisibility(View.INVISIBLE);
        info_textEdit.setText("");
        info_textInfo.setVisibility(View.INVISIBLE);
        location_textEdit.setInputType(0);
        article_textEdit.setInputType(0);
        isHandWritten = false;
    }

    public void getElementsFromIntent() {
        String password = (getIntent().getStringExtra("password"));
        setPassword(password);
        database = (getIntent().getStringExtra("database"));
        stockID = (getIntent().getStringExtra("stockID"));
        back_article = (getIntent().getStringExtra("art_idno"));
    }

    public void setIntent(String destination, String stockID, String article) {
        try {
            Intent intent = new Intent(this, Class.forName("protec.pl.protecabasvol2." + destination));
            intent.putExtra("password", getPassword());
            intent.putExtra("database", database);
            intent.putExtra("stockID", stockID);
            intent.putExtra("content", article);
            intent.putExtra("destination", "Stocktaking");
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("WrongViewCast")
    public void lockLocation(View view) {
        lockIcon = (CheckBox) view;
        if (lockIcon.isChecked()) {
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_closed_icon);
        } else {
            lockIcon.setBackgroundResource(R.drawable.ic_new_lock_icon);  //Ok
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // CHECK STOCK
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void scanArticle(View view) {
        try {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować artykuł");
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

    public void scanLocation(View view) {
        try {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Proszę zeskanować lokalizację");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            Intent intent = integrator.createScanIntent();
            startActivityForResult(intent, 70);
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
                    getArticle(content);
                }
            }
            if (requestCode == 70) {
                if (resultCode == RESULT_OK) {
                    String content = result.getContents();
                    String location_name = "";
                    getLocation(content);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void getArticle(String content) {
        myGlob = new GlobalClass(getApplicationContext());
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            Product product = myGlob.FindProductByIdno(ctx, content);
            if (product != null) {  //jeśli Find by IDNO nie równa się null
                article_textEdit.setText(product.getIdno());
                article_textInfo.setVisibility(View.VISIBLE);
                info_textInfo.setVisibility(View.VISIBLE);
                qty_textInfo.setVisibility(View.VISIBLE);
                location_textInfo.setVisibility(View.VISIBLE);
                String unit_text = product.getSU().toString();
                if(unit_text.equals("(7)")){ unit_text = "kg";}
                else if(unit_text.equals("(20)")){ unit_text = "szt.";}
                else if(unit_text.equals("(21)")){ unit_text = "kpl"; }
                else if(unit_text.equals("(1)")){ unit_text = "m"; }
                unit.setVisibility(View.VISIBLE);
                unit.setText(unit_text);
                isHandWritten = false;
                ctx.close();
            } else {
                article_textEdit.setText("");
                GlobalClass.showDialog(Stocktaking.this, "Brak artykułu!", "W bazie nie ma takeigo artykłu!", "OK",
                new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ctx.close();

                }
                });
            }
        } catch (Exception e) {
            GlobalClass.showDialog(Stocktaking.this, "Brak połączenia!", "Nie można aktualnie połączyć z bazą.", "OK",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        }
    }

    public void enterFreehandArticle(View view){
        AlertDialog.Builder enterArticleDialog = new AlertDialog.Builder(Stocktaking.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_enter_article, viewGroup, false);
        enterArticleDialog.setView(dialogView);
        AlertDialog articleDialog = enterArticleDialog.create();
        Button button_cancel = (Button)dialogView.findViewById(R.id.button_cancel);
        button_cancel .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                articleDialog.dismiss();}
        });
        Button button_ok = (Button)dialogView.findViewById(R.id.button_ok);
        button_ok.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v){
                EditText article = articleDialog.findViewById(R.id.articleEnter);
                String article_name = article.getText().toString();
                if(article_name.matches("")){ //jeśli jest pusty
                    articleDialog.dismiss();
                    GlobalClass.showDialog(Stocktaking.this, "Brak wpisanego artykułu!", "Proszę wprowadzić artykuł.", "OK",
                            new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {} });
                }else {
                    articleDialog.dismiss();
                    LoadingDialog = ProgressDialog.show(Stocktaking.this, "",
                            "Ładowanie. Proszę czekać...", true);

                    searchArticle(article_name);
                }
            }
        });
        articleDialog.show();
        articleDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    //      SEARCH ARTICLE
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void searchArticle(String content) {
        myGlob = new GlobalClass(getApplicationContext());
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");   //?? potrzebne policy?
            if (myGlob.FindProductByIdno(ctx, content) != null) {  //jeśli Find by IDNO nie równa się null
                article_textEdit.setText(content);
                if (LoadingDialog != null) {
                    LoadingDialog.dismiss();
                }
                ctx.close();
                //jeśli nie znajdzie by IDNO
            } else if (myGlob.FindProductByDescr(ctx, content) != null) {
                ctx.close();
                setIntent("ArticleNameList", stockID, content);

                // jeśli nie znajdzie by DESCR
            } else if (myGlob.FindProductBySwd(ctx, content) != null) {   //jeśli Find by SWD nie równa się null
                ctx.close();
                setIntent("ArticleNameList", stockID, content);

                // jeśli nie znajdzie ani tu ani tu
            } else {
                ctx.close();
                LoadingDialog.dismiss();
                GlobalClass.showDialog(Stocktaking.this, "Brak artykułu!", "W bazie nie ma takeigo artykłu!", "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
            }

        } catch (Exception e) {
            if (LoadingDialog != null) {
                LoadingDialog.dismiss();
            }
            GlobalClass.showDialog(Stocktaking.this, "Brak połączenia!", "Nie można aktualnie połączyć z bazą.", "OK",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        }
    }

    public void getLocation(String content) {
        LocationHeader location = LocationExists(content);
        if (location != null) {
            String location_name = location.getSwd();
            location_textEdit.setText(location_name);
            article_textInfo.setVisibility(View.VISIBLE);
            info_textInfo.setVisibility(View.VISIBLE);
            qty_textInfo.setVisibility(View.VISIBLE);
            location_textInfo.setVisibility(View.VISIBLE);
            ctx.close();
        }else {
            location_textEdit.setText("");
            GlobalClass.showDialog(this, "Brak lokalizacji!", "Zeskanowana lokalizacja nie istnieje.", "OK",
            new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {} });
        }
    }

    public LocationHeader LocationExists(String location) {
        LocationHeader loc = null;
        try {
            ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
            SelectionBuilder<LocationHeader> locationSB = SelectionBuilder.create(LocationHeader.class);
            locationSB.add(Conditions.eq(LocationHeader.META.swd, location));
            loc = QueryUtil.getFirst(ctx, locationSB.build());
            ctx.close();
        } catch (Exception e) {
            Log.d("error", e.getMessage());
        }
        return loc;
    }

    public void saveStockEntry (View view){
        if(article_textEdit.getText().toString().isEmpty()){
            GlobalClass.showDialog(this, "Brak artykułu!", "Proszę wprowadzić artykuł.", "OK",
            new DialogInterface.OnClickListener() {@Override public void onClick(DialogInterface dialog, int which) {} });
        }else if((qty_textEdit.getText().toString().isEmpty())){
            GlobalClass.showDialog(this, "Brak ilości!", "Proszę wprowadzić ilość.", "OK",
            new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) {}});
        }else if(location_textEdit.getText().toString().isEmpty()) {
            if (location_textEdit.getHint().toString().equals("Lokalizacja")) {
                GlobalClass.showDialog(this, "Brak lokalizacji!", "Proszę wprowadzić lokalizację.", "OK",
                new DialogInterface.OnClickListener() {@Override public void onClick(DialogInterface dialog, int which) {}});
            } else{
                location_textEdit.setText(location_textEdit.getHint().toString());
                sendToDatabase();
            }
        }else{
            sendToDatabase();
        }
    }
    public void sendToDatabase(){
        LoadingDialog = ProgressDialog.show(Stocktaking.this, "",
                "Ładowanie. Proszę czekać...", true);
        enterStockRow();
        AlertDialog.Builder stockAddAlert = new AlertDialog.Builder(Stocktaking.this);
        stockAddAlert.setMessage("Produkt został pomyślnie dodany do bazy inwentaryzacji.");
        stockAddAlert.setTitle("Dodano!");
        stockAddAlert.setPositiveButton("Dodaj nowy artykuł",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //dismiss the dialog
                    article_textEdit.setText("");
                    location_textEdit.setText("");
                    qty_textEdit.setText("");
                    info_textEdit.setText("");
                    isHandWritten = false;
                    stocktaking = getStocktaing();
                    if (!stocktaking.getYcurrlocation().equals("")) {
                        location_textEdit.setHint(stocktaking.getYcurrlocation());
                        lockIcon.setBackgroundResource(R.drawable.ic_new_lock_closed_icon);
                        lockIcon.setChecked(true);
                    } else {
                        location_textEdit.setHint("Lokalizacja");
                        lockIcon.setBackgroundResource(R.drawable.ic_new_lock_icon);
                        lockIcon.setChecked(false);
                    }
                    ctx.close();
                }
            });
        stockAddAlert.setNegativeButton("Menu",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //dismiss the dialog
                        setIntent("Menu", stockID, "");
                    }
                });
        stockAddAlert.setCancelable(true);
        stockAddAlert.create().show();

        if (LoadingDialog != null) {
            LoadingDialog.dismiss();
        }
    }

    public void enterStockRow(){
        stocktaking = getStocktaing();
        StocktakingProtecEditor stocktakingProtecEditor = stocktaking.createEditor();
        try {
            stocktakingProtecEditor.open(EditorAction.UPDATE);
        } catch (CommandException e){
            Log.d("command exception", e.getMessage());
        }
        if(lockIcon.isChecked()){
            stocktakingProtecEditor.setYcurrlocation(location_textEdit.getText().toString());
        }else{
            stocktakingProtecEditor.setYcurrlocation("");
        }
        stocktakingProtecEditor.table().appendRow();
        StocktakingProtecEditor.Row row =  stocktakingProtecEditor.table().getRow(stocktakingProtecEditor.getRowCount());
        Integer rowNo = stocktaking.table().getRowCount();
        row.setString("yid", rowNo.toString());
        row.setString("yarticle", article_textEdit.getText());
        row.setString("ycountedqty", qty_textEdit.getText());
        row.setString("yunit", unit.getText());
        row.setString("ystorageplace", location_textEdit.getText());
        row.setYishandwritten(isHandWritten);
        row.setString("yinfo", info_textEdit.getText());
        stocktakingProtecEditor.commit();
        if(stocktakingProtecEditor.active()){
            stocktakingProtecEditor.abort();
        }
        ctx.close();
    }

    public StocktakingProtec getStocktaing() {
        ctx = ContextHelper.createClientContext("192.168.1.3", 6550, database, getPassword(), "mobileApp");
        SelectionBuilder<StocktakingProtec> stocktakingSB = SelectionBuilder.create(StocktakingProtec.class);
        stocktakingSB.add(Conditions.eq(StocktakingProtec.META.idno, stockID));
        stocktaking = QueryUtil.getFirst(ctx, stocktakingSB.build());
        ctx.close();
        return stocktaking;
    }

    public void showMyList(View view){
        LoadingDialog = ProgressDialog.show(Stocktaking.this, "",
                "Ładowanie. Proszę czekać...", true);
        setIntent("MyStocktakingList", stockID, "");
    }
}