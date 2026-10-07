package app.polishapps.patches.yanosik.ads

import app.morphe.patcher.patch.bytecodePatch
import app.polishapps.patches.shared.Constants.COMPATIBILITY_YANOSIK
import app.polishapps.patches.shared.forceInstanceOfTrue
import app.polishapps.patches.shared.methodFingerprint
import app.polishapps.patches.shared.replaceBody

private const val ENTITLEMENT = "Ld6f;"

private data class PremiumGate(
    val definingClass: String,
    val name: String,
    val parameters: List<String> = emptyList(),
    val returnType: String? = null,
)

/**
 * Methods that contain `instance-of ..., Ld6f;` to decide whether an advert is
 * shown. Forcing the result to `true` treats the user as entitled, which
 * suppresses the advert exactly like the manual smali patch did.
 *
 * These targets are pinned to Yanosik 26.9.0; obfuscated names change per release.
 */
private val premiumGates = listOf(
    PremiumGate(
        "Lpl/neptis/data/startappadvert/StartAppAdvertService;",
        "refresh\$lambda\$0",
        listOf("Lpl/neptis/data/startappadvert/StartAppAdvertService;"),
        "Ljava/lang/Object;",
    ),
    PremiumGate(
        "Lpl/neptis/yanosik/mobi/android/common/ui/activities/launcher/LauncherActivity;",
        "x0",
        listOf(
            "Lpl/neptis/yanosik/mobi/android/common/ui/activities/launcher/LauncherActivity;",
            "J",
            "L",
        ),
        "Ljava/lang/Object;",
    ),
    PremiumGate(
        "Lpl/neptis/yanosik/mobi/android/common/ui/activities/main/MainReportActivity;",
        "onCreate",
        listOf("Landroid/os/Bundle;"),
        "V",
    ),
    PremiumGate("Lm8b;", "invokeSuspend", listOf("Ljava/lang/Object;"), "Ljava/lang/Object;"),
    PremiumGate("Lk9k;", "invokeSuspend", listOf("Ljava/lang/Object;"), "Ljava/lang/Object;"),
    PremiumGate("Lxo;", "invokeSuspend", listOf("Ljava/lang/Object;"), "Ljava/lang/Object;"),
    PremiumGate("Lni8;", "invoke", listOf("L", "L", "L", "L"), "Ljava/lang/Object;"),
    PremiumGate("Lzr;", "J"),
    PremiumGate("Letc;", "f"),
    PremiumGate("Lur;", "x", listOf("L", "L")),
    PremiumGate("Lur;", "y", listOf("L")),
    PremiumGate("Lpl/neptis/features/audioalerts/AudioAlertsService;", "tryToPlay"),
    PremiumGate("Lxok;", "g", listOf("L"), "L"),
)

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove Yanosik ads",
    description = "Removes banner ads, splash/start adverts and advert-based navigation POIs.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_YANOSIK)

    execute {
        // Banner service: always take the "adverts disabled" branch.
        methodFingerprint(
            "Lpl/neptis/features/adsbanners/adsservice/AdvertService;",
            "isAnyEntitlementsActive",
            returnType = "Z",
        ).method.replaceBody(
            """
                const/4 v0, 0x1
                return v0
            """,
        )

        methodFingerprint(
            "Lpl/neptis/features/adsbanners/adsservice/AdvertService;",
            "checkEntitlements",
            parameters = listOf("Z"),
        ).method.replaceBody(
            """
                const-class p1, Lgq;
                invoke-static {p1}, Lasm;->a(Ljava/lang/Class;)V
                const-class p1, Liq;
                invoke-static {p1}, Lasm;->a(Ljava/lang/Class;)V
                const-class p1, Lhq;
                invoke-static {p1}, Lasm;->a(Ljava/lang/Class;)V
                const-class p1, Llq;
                invoke-static {p1}, Lasm;->a(Ljava/lang/Class;)V
                const-class p1, Lmq;
                invoke-static {p1}, Lasm;->a(Ljava/lang/Class;)V
                new-instance p1, Ljq;
                invoke-direct {p1}, Ljava/lang/Object;-><init>()V
                const/4 v0, 0x1
                invoke-static {p1, v0}, Lasm;->e(Ljava/lang/Object;Z)V
                invoke-direct {p0}, Lpl/neptis/features/adsbanners/adsservice/AdvertService;->stopTimer()V
                return-void
            """,
        )

        // Banner fragments render nothing.
        for (fragment in listOf("dashboard/AdsDashboardMainFragment", "small/AdsSmallMainFragment")) {
            methodFingerprint(
                "Lpl/neptis/features/adsbanners/banners/$fragment;",
                "onCreateView",
                parameters = listOf(
                    "Landroid/view/LayoutInflater;",
                    "Landroid/view/ViewGroup;",
                    "Landroid/os/Bundle;",
                ),
                returnType = "Landroid/view/View;",
            ).method.replaceBody(
                """
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
        }

        // Entitlement-gated advert code paths.
        premiumGates.forEach { gate ->
            methodFingerprint(
                gate.definingClass,
                gate.name,
                parameters = gate.parameters,
                returnType = gate.returnType,
            ).method.forceInstanceOfTrue(ENTITLEMENT)
        }

        // Settings: never set up the "buy PRO" upsell alert banner.
        methodFingerprint(
            "Lpl/neptis/features/settings/AppPreferenceActivity;",
            "setupProAlert",
            returnType = "V",
        ).method.replaceBody(
            """
                return-void
            """,
        )
    }
}
