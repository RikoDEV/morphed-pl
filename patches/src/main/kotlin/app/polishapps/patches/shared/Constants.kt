package app.polishapps.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

object Constants {
    /**
     * Yanosik is distributed by APKMirror as an APKM bundle.
     *
     * The 26.9.0 targets are pinned to exact version codes because the
     * obfuscated class names the patches rely on (r5f, l9f, tmd, m8b, ...)
     * change between releases.
     */
    val COMPATIBILITY_YANOSIK = Compatibility(
        name = "Yanosik",
        packageName = "pl.neptis.yanosik.mobi.android",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xD4443C,
        targets = listOf(
            AppTarget(
                version = "26.9.0",
                versionCodes = mapOf(
                    SupportedAbi.ARM64_V8A to 6001512,
                ),
            ),
        ),
    )
}
