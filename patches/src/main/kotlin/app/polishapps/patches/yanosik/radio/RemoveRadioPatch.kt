package app.polishapps.patches.yanosik.radio

import app.morphe.patcher.patch.bytecodePatch
import app.polishapps.patches.shared.Constants.COMPATIBILITY_YANOSIK
import app.polishapps.patches.shared.forceInstanceOfTrue
import app.polishapps.patches.shared.methodFingerprint
import app.polishapps.patches.shared.replaceBody

/**
 * Removes the built-in "Radio Yanosik" from the main screen and the map.
 *
 * Two independent code paths are removed:
 *
 * 1. The dashboard pack feature. `Feature.d()` availability comes from
 *    `DashboardRadioConfiguration.a()`, and every entry point resolves the feature with
 *    `vq6.a(DashboardRadioFeature)` which filters on `d()`, so forcing `a()` to false removes
 *    the dashboard/drawer tile, the map radio bar (`RadioReportFragment`) and the Compose radio.
 *
 * 2. The map navigation button. `uy1.invokeSuspend` builds the `Lwof` ("Radio") navigation model
 *    unless the nav state is an `Lhng`, so forcing that `instance-of` to true makes it always
 *    return a null navigation model and the button is never rendered.
 */
@Suppress("unused")
val removeRadioPatch = bytecodePatch(
    name = "Remove Yanosik radio",
    description = "Removes the built-in Radio Yanosik from the main screen and the map button.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_YANOSIK)

    execute {
        // 1. Disable the dashboard pack radio feature.
        methodFingerprint(
            "Lpl/neptis/libraries/actions/dashboardpack/DashboardRadioConfiguration;",
            "a",
            returnType = "Z",
        ).method.replaceBody(
            """
                const/4 p0, 0x0
                return p0
            """,
        )

        // 2. Never build the "Radio" entry in the map navigation model.
        methodFingerprint(
            "Luy1;",
            "invokeSuspend",
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "Ljava/lang/Object;",
        ).method.forceInstanceOfTrue("Lhng;")
    }
}
