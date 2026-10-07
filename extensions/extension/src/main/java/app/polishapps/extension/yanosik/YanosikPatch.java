package app.polishapps.extension.yanosik;

@SuppressWarnings("unused")
public final class YanosikPatch {

    private YanosikPatch() {
    }

    /**
     * Builds an active premium entitlement state for the Yanosik subscription flow.
     *
     * The entitlement type and its enum are obfuscated and not available at extension compile
     * time, so they are resolved reflectively against the patched app at runtime. The returned
     * object replaces the initial (non-premium) state value so {@code isPro} is true from the
     * first frame, matching the manual smali build.
     */
    public static Object premiumState() {
        try {
            Class<?> typeClass = Class.forName("h4f");

            Object active = null;
            for (Object constant : typeClass.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals("ACTIVE")) {
                    active = constant;
                    break;
                }
            }
            if (active == null) {
                throw new IllegalStateException("h4f.ACTIVE not found");
            }

            Class<?> premiumClass = Class.forName("b6f");
            java.lang.reflect.Constructor<?> constructor =
                    premiumClass.getConstructor(typeClass, java.util.Date.class, boolean.class);
            return constructor.newInstance(active, new java.util.Date(), true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Yanosik premium state could not be created", e);
        }
    }
}
