package protec.pl.protecabasvol2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.Query;
import de.abas.erp.db.exception.DBRuntimeException;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
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
            productSB.add(Conditions.matchIgCase(Product.META.descrOperLang.toString(), name));
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

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton(positiveButton, onClickListener);
        dialog.show();
    }
    public static void showDialogTwoButtons(Context context,String title,String message, String positiveButton, String negativeButton,
                                  DialogInterface.OnClickListener onClickListener, DialogInterface.OnClickListener onClickListenerNegative) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton(positiveButton,onClickListener);
        dialog.setNegativeButton(negativeButton,onClickListenerNegative);
        dialog.show();
    }
}
