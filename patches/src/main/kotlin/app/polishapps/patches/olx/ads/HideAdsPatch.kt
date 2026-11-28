package app.polishapps.patches.olx.ads

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.polishapps.patches.inpost.shared.findMethods
import app.polishapps.patches.inpost.shared.mutableMethod
import app.polishapps.patches.olx.shared.MethodSpec
import app.polishapps.patches.olx.shared.rewriteDartHosts
import app.polishapps.patches.olx.shared.stub
import app.polishapps.patches.olx.shared.stubReturn
import app.polishapps.patches.shared.Constants.COMPATIBILITY_OLX
import app.polishapps.patches.shared.prependReturn

private const val BAXTER = "Lcom/naspers/advertising/baxterandroid/Baxter;"
private const val BAXTER_LISTENER = "Lcom/naspers/advertising/baxterandroid/BaxterListener;"
private const val GOOGLE_MOBILE_ADS = "Lio/flutter/plugins/googlemobileads/"
private const val EXTENSION_CLASS = "Lapp/polishapps/extension/olx/FlutterAds;"

/**
 * The Dart copy of the Baxter SDK fetches its slot config from this CDN. Without a config it reports
 * "Config init failed" and renders no slots, including the static (HTML) ones that never reach the
 * google_mobile_ads plugin.
 */
private val dartAdHostsPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_OLX)

    execute {
        rewriteDartHosts(mapOf("cdn.slots.baxter.olx.org" to "cdn-slots-baxter.invalid"))
    }
}

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes the advertising banners from the home feed, search results, ad pages and " +
        "job offers, and blocks interstitial ads, in the native and the Flutter screens.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_OLX)

    dependsOn(dartAdHostsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        // Native screens use Baxter, the Naspers ad SDK. Without init there is no slot config, so the
        // search results inject no ad tiles, and every loadAd() takes the SDK's own "not initialized"
        // path, which calls the failure callback that collapses the banner. The app start sync waits
        // for the listener, so it is told that the SDK failed to start.
        stub(
            BAXTER,
            """
                if-eqz p3, :no_listener
                const-string v0, "Ads disabled"
                invoke-interface {p3, v0}, $BAXTER_LISTENER->onFailedToStart(Ljava/lang/String;)V
                :no_listener
                return-void
            """,
            MethodSpec(
                "init",
                listOf(
                    "Landroid/content/Context;",
                    "Lcom/naspers/advertising/baxterandroid/data/entities/ClientConfig;",
                    BAXTER_LISTENER,
                    "Ljava/lang/String;",
                    "Z",
                ),
            ),
        )
        stubReturn(
            BAXTER,
            0,
            MethodSpec("isInitialized", emptyList()),
            MethodSpec("getAdvertisingConfig", emptyList()),
        )

        // Flutter screens load Google Ad Manager ads through the google_mobile_ads plugin.
        val loads = findMethods { classDef, method ->
            classDef.type.startsWith(GOOGLE_MOBILE_ADS) &&
                method.name == "load" &&
                method.returnType == "V" &&
                method.parameterTypes.isEmpty() &&
                method.implementation != null
        }
        if (loads.isEmpty()) throw PatchException("google_mobile_ads: no ad load() methods found")
        loads.forEach { (classDef, method) ->
            mutableMethod(classDef, method).prependReturn(
                """
                    invoke-static/range {p0 .. p0}, $EXTENSION_CLASS->failLoad(Ljava/lang/Object;)V
                    return-void
                """,
            )
        }
    }
}
