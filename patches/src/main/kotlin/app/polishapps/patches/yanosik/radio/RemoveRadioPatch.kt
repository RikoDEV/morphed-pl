package app.polishapps.patches.yanosik.radio

import app.morphe.patcher.patch.bytecodePatch
import app.polishapps.patches.shared.Constants.COMPATIBILITY_YANOSIK
import app.polishapps.patches.shared.methodFingerprint
import app.polishapps.patches.shared.replaceBody

/**
 * Removes the built-in "Radio Yanosik".
 *
 * `DashboardRadioFeature` is registered in the dashboard pack and its
 * availability (`Feature.d()`) is driven by `DashboardRadioConfiguration.a()`.
 * Every radio entry point resolves the feature with `vq6.a(DashboardRadioFeature)`
 * which filters on `d()`:
 *
 *  - the dashboard/drawer tile (DashboardPackActivity),
 *  - the map radio bar (MainReportActivity adds RadioReportFragment to `radioContainer`),
 *  - the Compose radio (cyg).
 *
 * Forcing `a()` to false therefore removes the radio from the main screen and
 * the map button.
 */
@Suppress("unused")
val removeRadioPatch = bytecodePatch(
    name = "Remove Yanosik radio",
    description = "Removes the built-in Radio Yanosik from the main screen and the map button.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_YANOSIK)

    execute {
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
    }
}
