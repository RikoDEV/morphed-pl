package app.polishapps.patches.yanosik.map

import app.morphe.patcher.patch.bytecodePatch
import app.polishapps.patches.shared.Constants.COMPATIBILITY_YANOSIK
import app.polishapps.patches.shared.methodFingerprint
import app.polishapps.patches.shared.replaceBody

/**
 * Removes the bottom sheet/panel of the map navigation view.
 *
 * `nhc.k` composes the panel content: the destination search bar
 * (`dashboard_search_placeholder`), the shortcuts list and the shortcut buttons
 * (home screen, road help, ...). Clearing the composable leaves the panel host with
 * nothing to lay out, so it is no longer shown.
 */
@Suppress("unused")
val removeMapBottomShelfPatch = bytecodePatch(
    name = "Remove Yanosik map bottom shelf",
    description = "Removes the map bottom sheet/panel (search bar, shortcuts, home and road help buttons).",
    default = true,
) {
    compatibleWith(COMPATIBILITY_YANOSIK)

    execute {
        methodFingerprint(
            "Lnhc;",
            "k",
            parameters = listOf(
                "Lu1c;",
                "Lekh;",
                "Lj9e;",
                "Ljava/util/List;",
                "Lc33;",
                "Llp;",
                "Ly93;",
                "I",
            ),
            returnType = "V",
        ).method.replaceBody("return-void")
    }
}
