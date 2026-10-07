package app.polishapps.extension.yanosik;

@SuppressWarnings("unused")
public final class YanosikPatch {

    private YanosikPatch() {
    }

    /**
     * Reported to AdvertService while banner entitlements are checked.
     * Returning {@code true} makes the service take its "adverts disabled" path.
     */
    public static boolean isEntitlementActive() {
        return true;
    }
}
