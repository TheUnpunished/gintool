package xyz.unpunished.util;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


public final class I18N {


    private static Locale locale = Locale.ENGLISH;
    
    public static List<Locale> getSupportedLocales() {
        return new ArrayList<>(Arrays.asList(
                Locale.ENGLISH,
                new Locale("ru", "RU"),
                new Locale("es", "US"),
                new Locale("hr", "HR"),
                Locale.GERMAN));
    }
    
    
    public static Locale getDefaultLocale() {
        Locale sysDefault = Locale.getDefault();
        return getSupportedLocales().contains(sysDefault) ? sysDefault : Locale.ENGLISH;
    }

    public static Locale getLocale() {
        return locale;
    }

    public static void setLocale(Locale newLocale) {
        locale = newLocale;
        Locale.setDefault(newLocale);
    }
    
    public static String get(final String key, final Object... args) {
        ResourceBundle bundle = ResourceBundle.getBundle("i18n/messages", locale);
        return MessageFormat.format(bundle.getString(key), args);
    }
}