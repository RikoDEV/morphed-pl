package app.polishapps.extension.olx;

import java.lang.reflect.Field;

/**
 * Called instead of {@code load()} of every google_mobile_ads plugin ad. Reports a "no fill" load
 * error to Dart, the same event the plugin sends when Google has no ad, so the Flutter widgets take
 * their normal empty path instead of waiting for an ad.
 */
@SuppressWarnings("unused")
public final class FlutterAds {
    private static final String PACKAGE = "io.flutter.plugins.googlemobileads.";
    private static final int ERROR_CODE_NO_FILL = 3;

    private FlutterAds() {
    }

    public static void failLoad(Object ad) {
        try {
            Object manager = field(ad, "manager");
            Object adId = field(ad, "adId");
            if (manager == null || !(adId instanceof Integer)) return;

            Class<?> errorClass = Class.forName(PACKAGE + "FlutterAd$FlutterLoadAdError");
            Class<?> responseInfoClass = Class.forName(PACKAGE + "FlutterAd$FlutterResponseInfo");
            Object error = errorClass
                .getConstructor(int.class, String.class, String.class, responseInfoClass)
                .newInstance(ERROR_CODE_NO_FILL, "com.google.android.gms.ads", "No fill.", null);

            manager.getClass()
                .getMethod("onAdFailedToLoad", int.class, errorClass)
                .invoke(manager, adId, error);
        } catch (Throwable ignored) {
            // The ad stays unloaded, which Dart treats the same way.
        }
    }

    private static Object field(Object target, String name) throws IllegalAccessException {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // Declared in a superclass.
            }
        }
        return null;
    }
}
