package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.ContextThemeWrapper;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.Query;
import de.abas.erp.db.infosystem.custom.sy.IsPrDisplays;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.QueryUtil;

public class GlobalClass {
    Context mContext;
    private String password;
    public String getPassword() {return password; }
    public void setPassword(String password) {
        this.password = password;
    }
    DbContext ctx;

    // constructor
    public GlobalClass(Context Thiscontext){
        this.mContext = Thiscontext;
    }

    //Find Product By IDNO
    public final static Product FindProductByIdno(DbContext ctx, String idno) {
        SelectionBuilder<Product> productSB = SelectionBuilder.create(Product.class);
        Product product = null;
        try {
            productSB.add(Conditions.eq(Product.META.idno.toString(), idno));
            if(QueryUtil.getFirst(ctx, productSB.build()) != null){ //jesli pierwszy nie równa się null
                product = QueryUtil.getFirst(ctx, productSB.build());
            }else{
                productSB.add(Conditions.eq(Product.META.descrOperLang.toString(), idno));
            }
        } catch (Exception e) {
        }
        return product;
    }

    // Find Product By DESCR
    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final Product FindProductByDescr(DbContext ctx, String name){
        Product product = null;
        SelectionBuilder<Product> productSB = SelectionBuilder.create(Product.class);
        Query<Product> productQuery = ctx.createQuery(productSB.build());
        try {
            productSB.add(Conditions.matchIgCase(Product.META.descr6.toString(), name));
            product = QueryUtil.getFirst(ctx, productSB.build());
        } catch (Exception e) {
        }
        return product;
    }

    // Find Product By SWD (KLUCZ)
    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final Product FindProductBySwd(DbContext ctx, String name){
        Product product = null;
        SelectionBuilder<Product> productSB = SelectionBuilder.create(Product.class);
        Query<Product> productQuery = ctx.createQuery(productSB.build());
        try {
            productSB.add(Conditions.matchIgCase(Product.META.swd.toString(), name));
            product = QueryUtil.getFirst(ctx, productSB.build());
        } catch (Exception e) {
        }
        return product;
    }
    public static void showDialog(Context context,String title,String message, String positiveButton,
                                  DialogInterface.OnClickListener onClickListener) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.MyDialog));
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton(positiveButton, onClickListener);
        dialog.show();
    }
    public static void showDialogTwoButtons(Context context,String title,String message, String positiveButton, String negativeButton,
                                  DialogInterface.OnClickListener onClickListener, DialogInterface.OnClickListener onClickListenerNegative) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.MyDialog));
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton(positiveButton,onClickListener);
        dialog.setNegativeButton(negativeButton,onClickListenerNegative);
        dialog.show();
    }

    public static void licenceCleaner(DbContext ctx) {
        try {
            IsPrDisplays isPrDisplays = ctx.openInfosystem(IsPrDisplays.class);
            isPrDisplays.invokeStart();
            isPrDisplays.table().appendRow();
            Iterable<IsPrDisplays.Row> isRows = isPrDisplays.getTableRows();
            HashMap<Integer, Long> loggedUsers = new HashMap<>();
            Integer currentUser = 0;
            SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd HH:mm");
            Date currentRowTime = parser.parse("19700101 00:00");
            Date now = new Date();
            // Gather rows
            for (IsPrDisplays.Row row : isRows) {
                if (row.getTpid().isEmpty() && row.getYltcount() == 0) {
                    if (currentUser != 0) {
                        Long timeDifference = now.getTime() - currentRowTime.getTime();
                        if (timeDifference / 1000 / 60 < 10000) {
                            loggedUsers.put(currentUser, timeDifference / 1000 / 60);
                        }
                    }
                    currentUser = row.getRowNo();
                    currentRowTime = parser.parse("19700101 00:00");
                } else if (!row.getTpid().isEmpty()) {
                    if (currentRowTime.before(parser.parse(row.getString("tdateofpid") + " " + row.getString("ttimeofpid")))) {
                        currentRowTime = parser.parse(row.getString("tdateofpid") + " " + row.getString("ttimeofpid"));
                    }
                }
            }

            // Find the biggest time difference
            Long biggestTimeDifference = new Long(0);
            Integer userToKill = 0;
            for (HashMap.Entry<Integer, Long> loggedEntry : loggedUsers.entrySet()) {
                if (biggestTimeDifference < loggedEntry.getValue()) {
                    biggestTimeDifference = loggedEntry.getValue();
                    userToKill = loggedEntry.getKey();
                }
            }

            // Kill
            for (IsPrDisplays.Row row : isRows) {
                if (row.getRowNo() == userToKill) {
                    row.invokeTbukill();
                    break;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
