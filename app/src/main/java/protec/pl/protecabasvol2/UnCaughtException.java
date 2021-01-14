package protec.pl.protecabasvol2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Locale;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.infosystem.custom.owpl.IsMailSender;
import de.abas.erp.db.util.ContextHelper;

public class UnCaughtException implements Thread.UncaughtExceptionHandler {
    private Context context;
    private static Context context1;
    private String userSwd;

    public UnCaughtException(Context ctx, String user) {
        context = ctx;
        context1 = ctx;
        userSwd = user;
    }

    private StatFs getStatFs() {
        File path = Environment.getDataDirectory();
        return new StatFs(path.getPath());
    }

    private long getAvailableInternalMemorySize(StatFs stat) {
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    private long getTotalInternalMemorySize(StatFs stat) {
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    private void addInformation(StringBuilder message) {
        message.append("User: ").append(userSwd).append("<br/>");
        message.append("Locale: ").append(Locale.getDefault()).append("<br/>");
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi;
            pi = pm.getPackageInfo(context.getPackageName(), 0);
            message.append("Version: ").append(pi.versionName).append("<br/>");
            message.append("Package: ").append(pi.packageName).append("<br/>");
        } catch (Exception e) {
            Log.e("CustomExceptionHandler", "Error", e);
            message.append("Could not get Version information for ").append(context.getPackageName());
        }
        message.append("Phone Model ").append(android.os.Build.MODEL).append("<br/>");
        message.append("Android Version : ").append(android.os.Build.VERSION.RELEASE).append("<br/>");
        message.append("Board: ").append(android.os.Build.BOARD).append("<br/>");
        message.append("Brand: ").append(android.os.Build.BRAND).append("<br/>");
        message.append("Device: ").append(android.os.Build.DEVICE).append("<br/>");
        message.append("Host: ").append(android.os.Build.HOST).append("<br/>");
        message.append("ID: ").append(android.os.Build.ID).append("<br/>");
        message.append("Model: ").append(android.os.Build.MODEL).append("<br/>");
        message.append("Product: ").append(android.os.Build.PRODUCT).append("<br/>");
        message.append("Type: ").append(android.os.Build.TYPE).append("<br/>");
        StatFs stat = getStatFs();
        message.append("Total Internal memory: ")
                .append(getTotalInternalMemorySize(stat)).append("<br/>");
        message.append("Available Internal memory: ")
                .append(getAvailableInternalMemorySize(stat)).append("<br/>");
    }

    public void uncaughtException(Thread t, Throwable e) {
        try {

            StringBuilder report = new StringBuilder();
            Date curDate = new Date();
            report.append("Error Report collected on : ").append(curDate.toString()).append("<br/>").append("<br/>");
            report.append("Informations :").append("<br/>");
            addInformation(report);
            report.append("<br/>").append("<br/>");
            report.append("Stack: <br/>");
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            report.append(result.toString());
            printWriter.close();
            report.append("<br/><br/>");
            report.append("**** End of current Report ***");
            Log.e(UnCaughtException.class.getName(),"Error while sendErrorMail" + report);
            sendErrorMail(report);
        } catch (Throwable ignore) {
            Log.e(UnCaughtException.class.getName(),"Error while sending error e-mail", ignore);
        }
    }

    public void sendErrorMail(final StringBuilder errorContent) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                DbContext ctx = ContextHelper.createClientContext("192.168.1.3", 6550, "erp", "sesje", "mobileApp");
                StringBuilder body = new StringBuilder("Yoddle");
                body.append("<br/>").append("<br/>");
                body.append(errorContent + "<br/>").append("<br/>").append("<br/>");
                IsMailSender sender = ctx.openInfosystem(IsMailSender.class);
                sender.setYto("julia.swiec@protec.pl");
                sender.setYsubject("Report o bledzie Aplikacji Protec ABAS");
                if(body.toString().length() < 2999) {
                    sender.setYtrext(body.toString());
                }else{
                    sender.setYtrext(body.toString().substring(0, 2800));
                }
                sender.invokeStart();
                sender.close();


                builder.setTitle("Błąd!");
                builder.create();
                builder.setNegativeButton("Anuluj",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        });
                builder.setPositiveButton("Wyślij report",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {

                            Intent sendIntent = new Intent(Intent.ACTION_SEND);
                            String subject = "Your App crashed! Fix it!";
                            StringBuilder body = new StringBuilder("Yoddle");
                            body.append("<br/>").append("<br/>");
                            body.append(errorContent).append("<br/>").append("<br/>");
                            sendIntent.setType("message/rfc822");
                            sendIntent.putExtra(Intent.EXTRA_EMAIL,new String[] { "julia.swiec@protec.pl" });
                            sendIntent.putExtra(Intent.EXTRA_TEXT,
                                    body.toString());
                            sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                                    subject);
                            sendIntent.setType("message/rfc822");
                            context1.startActivity(sendIntent);
                            System.exit(0);
                        }
                    });
                builder.setMessage("Aplikacja Protec ABAS przestała działać. Zamknij aplikację i spróbuj ponownie za chwilę.");
                builder.show();
                Looper.loop();
            }
        }.start();
    }
}
