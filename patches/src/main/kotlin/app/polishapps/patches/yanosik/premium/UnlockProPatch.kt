package app.polishapps.patches.yanosik.premium

import app.morphe.patcher.patch.bytecodePatch
import app.polishapps.patches.shared.Constants.COMPATIBILITY_YANOSIK
import app.polishapps.patches.shared.methodFingerprint
import app.polishapps.patches.shared.replaceBody

private const val PREMIUM_BODY = """
    new-instance v0, Lb6f;
    sget-object v1, Lh4f;->ACTIVE:Lh4f;
    new-instance v2, Ljava/util/Date;
    invoke-direct {v2}, Ljava/util/Date;-><init>()V
    const/4 v3, 0x1
    invoke-direct {v0, v1, v2, v3}, Lb6f;-><init>(Lh4f;Ljava/util/Date;Z)V
    return-object v0
"""

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock Yanosik PRO",
    description = "Reports an active premium entitlement and unlocks the PRO-only settings " +
        "(view after launch, floating icon).",
    default = true,
) {
    compatibleWith(COMPATIBILITY_YANOSIK)

    execute {
        // RevenueCat entitlement mappers (phone + web) -> always active premium.
        methodFingerprint(
            "Lr5f;",
            "b",
            parameters = listOf("L", "Lcom/revenuecat/purchases/EntitlementInfos;"),
            returnType = "L",
        ).method.replaceBody(PREMIUM_BODY)

        methodFingerprint(
            "Lr5f;",
            "c",
            parameters = listOf("L", "Lcom/revenuecat/purchases/EntitlementInfos;"),
            returnType = "L",
        ).method.replaceBody(PREMIUM_BODY)

        // AppPreferenceActivity ProState -> full PRO (view after launch).
        methodFingerprint(
            "Ll9f;",
            "<init>",
            parameters = listOf("Z", "Z", "L"),
            returnType = "V",
        ).method.replaceBody(
            """
                invoke-direct {p0}, Ljava/lang/Object;-><init>()V
                iput-object p3, p0, Ll9f;->c:Lh9f;
                const/4 p1, 0x1
                iput-boolean p1, p0, Ll9f;->a:Z
                iput-boolean p1, p0, Ll9f;->b:Z
                iput-boolean p1, p0, Ll9f;->d:Z
                iput-boolean p1, p0, Ll9f;->e:Z
                return-void
            """,
        )

        // OverlaySettingsActivity state -> full PRO (floating icon).
        methodFingerprint(
            "Ltmd;",
            "<init>",
            parameters = listOf("L", "Z", "Z", "L"),
            returnType = "V",
        ).method.replaceBody(
            """
                invoke-direct {p0}, Ljava/lang/Object;-><init>()V
                iput-object p1, p0, Ltmd;->a:Limd;
                iput-object p4, p0, Ltmd;->d:Lh9f;
                const/4 p2, 0x1
                const/4 p3, 0x1
                iput-boolean p2, p0, Ltmd;->b:Z
                iput-boolean p3, p0, Ltmd;->c:Z
                return-void
            """,
        )
    }
}
