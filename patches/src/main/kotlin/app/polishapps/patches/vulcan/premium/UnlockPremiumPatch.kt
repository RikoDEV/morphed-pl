package app.polishapps.patches.vulcan.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.polishapps.patches.shared.Constants.COMPATIBILITY_VULCAN
import app.polishapps.patches.shared.Constants.VULCAN_26_08_00
import app.polishapps.patches.shared.targetsFor
import app.polishapps.patches.vulcan.DART_FALSE
import app.polishapps.patches.vulcan.DART_TRUE
import app.polishapps.patches.vulcan.ENTER_FRAME
import app.polishapps.patches.vulcan.blInstruction
import app.polishapps.patches.vulcan.containsSnapshotBuildId
import app.polishapps.patches.vulcan.expectBytes
import app.polishapps.patches.vulcan.nativeLibrary
import app.polishapps.patches.vulcan.textFileOffset

private data class PremiumTargets(
    val libPath: String,
    val snapshotBuildId: String,
    val capabilitiesRemoveCall: Long,
    val setAddStub: Long,
    val hasPremium: Long,
    val premiumOverrideCheck: Long,
    val billingLockVisibilityCheck: Long,
    val premiumLockIsLocked: Long,
)

private val targetsByVersion = mapOf(
    VULCAN_26_08_00 to PremiumTargets(
        libPath = "lib/arm64-v8a/libapp.so",
        snapshotBuildId = "d6031998d1b3bbfebf59cc9bbff9aee1",
        capabilitiesRemoveCall = 0x0107b0bc,
        setAddStub = 0x011b8498,
        hasPremium = 0x00871338,
        premiumOverrideCheck = 0x00eaff10,
        billingLockVisibilityCheck = 0x00eafce8,
        premiumLockIsLocked = 0x00df24c0,
    ),
)

private val REMOVE_PREMIUM_CALL = byteArrayOf(
    0xae.toByte(), 0x0c, 0xe7.toByte(), 0x97.toByte(),
)

@Suppress("unused")
val unlockPremiumPatch = resourcePatch(
    name = "Unlock premium",
    description = "Reports an active Premium capability on every student account and removes " +
        "the premium lock overlays. Billing and the store stay available.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_VULCAN)

    execute {
        val targets = targetsFor(targetsByVersion)
        val file = nativeLibrary(targets.libPath)
        val bytes = file.readBytes()
        if (!bytes.containsSnapshotBuildId(targets.snapshotBuildId)) {
            throw PatchException(
                "${targets.libPath} does not contain the expected Dart snapshot build ID " +
                    targets.snapshotBuildId,
            )
        }

        val capabilitiesOffset = bytes.textFileOffset(targets.capabilitiesRemoveCall).toInt()
        bytes.expectBytes(capabilitiesOffset, REMOVE_PREMIUM_CALL, "capabilities remove call")
        blInstruction(targets.capabilitiesRemoveCall, targets.setAddStub).copyInto(bytes, capabilitiesOffset)

        val hasPremiumOffset = bytes.textFileOffset(targets.hasPremium).toInt()
        bytes.expectBytes(hasPremiumOffset, ENTER_FRAME, "EduVulcanJWTToken.hasPremium prologue")
        DART_TRUE.copyInto(bytes, hasPremiumOffset)

        val overrideOffset = bytes.textFileOffset(targets.premiumOverrideCheck).toInt()
        bytes.expectBytes(overrideOffset, ENTER_FRAME, "EvPremiumOverrideCapabilityCheck.isSatisfied prologue")
        DART_FALSE.copyInto(bytes, overrideOffset)

        val lockOffset = bytes.textFileOffset(targets.billingLockVisibilityCheck).toInt()
        bytes.expectBytes(lockOffset, ENTER_FRAME, "BillingLockVisibilityCheck.isSatisfied prologue")
        DART_FALSE.copyInto(bytes, lockOffset)

        val isLockedOffset = bytes.textFileOffset(targets.premiumLockIsLocked).toInt()
        bytes.expectBytes(isLockedOffset, ENTER_FRAME, "PremiumLockTapOverrider.isLocked prologue")
        DART_FALSE.copyInto(bytes, isLockedOffset)

        file.writeBytes(bytes)
    }
}
