package app.polishapps.patches.olx.telemetry

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.polishapps.patches.olx.shared.MethodSpec
import app.polishapps.patches.olx.shared.rewriteDartHosts
import app.polishapps.patches.olx.shared.stubReturn
import app.polishapps.patches.olx.shared.stubVoid
import app.polishapps.patches.shared.Constants.COMPATIBILITY_OLX
import org.w3c.dom.Element

private val manifestFlags = mapOf(
    "firebase_analytics_collection_deactivated" to "true",
    "google_analytics_adid_collection_enabled" to "false",
    "google_analytics_ssaid_collection_enabled" to "false",
    "firebase_crashlytics_collection_enabled" to "false",
    "firebase_sessions_enabled" to "false",
    "firebase_performance_collection_deactivated" to "true",
    "com.facebook.sdk.AutoLogAppEventsEnabled" to "false",
    "com.facebook.sdk.AdvertiserIDCollectionEnabled" to "false",
    "gemius_sdk_error_reporting_enabled" to "false",
)

/** Firebase's datatransport sends FCM and Crashlytics delivery logs through this backend. */
private const val CCT_BACKEND = "backend:com.google.android.datatransport.cct.CctBackendFactory"

private val manifestPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_OLX)

    execute {
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("AndroidManifest.xml has no <application>")

            val metaData = manifest.getElementsByTagName("meta-data")
            val elements = (0 until metaData.length).map { metaData.item(it) as Element }

            manifestFlags.forEach { (name, value) ->
                val element = elements.firstOrNull { it.getAttribute("android:name") == name && it.parentNode == application }
                    ?: manifest.createElement("meta-data").also { created ->
                        created.setAttribute("android:name", name)
                        application.appendChild(created)
                    }
                element.setAttribute("android:value", value)
            }

            elements.filter { it.getAttribute("android:name") == CCT_BACKEND }
                .forEach { it.parentNode.removeChild(it) }
        }
    }
}

/**
 * Dart sends New Relic events straight to the Insights API, and the app review flow posts to a
 * Hydra endpoint, both over Dart's own HTTP client.
 */
private val dartHostsPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_OLX)

    execute {
        rewriteDartHosts(
            mapOf(
                "insights-collector.newrelic.com" to "insights-collector-nrel.invalid",
                "tracking.olx-st.com" to "tracking-st.invalid",
            ),
        )
    }
}

private const val NINJA = "Lcom/naspers/clm/clm_android_ninja_base/Ninja;"
private const val HYDRA = "Lcom/naspers/clm/clm_android_ninja_hydra/HydraTracker;"

@Suppress("unused")
val blockTelemetryPatch = bytecodePatch(
    name = "Block telemetry",
    description = "Blocks the Ninja/Hydra event tracker and everything it forwards to (Braze, Firebase, " +
        "Adjust, Facebook), Braze, New Relic, Gemius audience measurement, ACRA crash reports, Adjust, " +
        "Firebase Analytics, Crashlytics and Performance.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_OLX)

    dependsOn(manifestPatch, dartHostsPatch)

    execute {
        // Ninja is the app's tracking hub: every trackEvent/trackView goes through a(), which builds the
        // Hydra event and hands it to each registered tracker (Hydra, Braze, Firebase, Adjust, Facebook).
        // init() is kept, because about 30 call sites read getHydraEventDefaultParams() and tracker
        // identities from it.
        stubReturn(
            NINJA,
            0,
            MethodSpec("a", listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/util/HashMap;")),
        )
        stubVoid(
            NINJA,
            "trackDebug",
            "trackError",
            "trackSurvey",
            "onAppLaunch",
            "sendDeepLinkData",
            "onInstallReferrerReceived",
            "flush",
        )
        stubVoid(HYDRA, "track", "trackDebug", "trackError", "trackSurvey", "onAppLaunch", "sendDeepLinkData")

        // Braze has a supported "SDK disabled" mode in which every call is a no-op.
        stubReturn("Lcom/braze/Braze\$Companion;", 1, MethodSpec("isDisabled", emptyList()))
        stubReturn("Lcom/braze/Braze;", 1, MethodSpec("isDisabled", emptyList()))

        stubVoid("Lcom/newrelic/agent/android/NewRelic;", "start")

        stubVoid("Lcom/gemius/sdk/audience/BaseEvent;", "sendEvent")
        stubVoid("Lcom/gemius/sdk/audience/AudienceEvent;", "sendEvent")
        stubVoid("Lcom/gemius/sdk/audience/NetpanelEvent;", "sendEvent")

        stubVoid("Lorg/acra/ACRA;", "init")

        stubVoid(
            "Lcom/adjust/sdk/Adjust;",
            "onCreate",
            "onResume",
            "onPause",
            "trackEvent",
            "trackAdRevenue",
            "trackThirdPartySharing",
            "trackMeasurementConsent",
            "trackPlayStoreSubscription",
            "sendFirstPackages",
            "setPushToken",
            "setReferrer",
            "appWillOpenUrl",
        )
    }
}
