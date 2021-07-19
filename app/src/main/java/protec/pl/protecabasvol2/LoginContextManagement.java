package protec.pl.protecabasvol2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LoginContextManagement {

        static final String PREF_USER_NAME= "username";

        static SharedPreferences getSharedPreferences(android.content.Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx);
        }

        public static void setUserName(Context ctx, String userName)
        {
            SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
            editor.putString(PREF_USER_NAME, userName);
            editor.commit();
        }

        public static String getUserName(Context ctx)
        {
            return getSharedPreferences(ctx).getString(PREF_USER_NAME, "");
        }

}
