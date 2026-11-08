package app.polishapps.patches.vulcan.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.polishapps.patches.shared.Constants.COMPATIBILITY_VULCAN
import app.polishapps.patches.shared.Constants.VULCAN_26_08_00
import app.polishapps.patches.shared.methodFingerprint
import app.polishapps.patches.shared.prependReturn
import app.polishapps.patches.shared.targetsFor
import app.polishapps.patches.vulcan.containsSnapshotBuildId
import app.polishapps.patches.vulcan.expectBytes
import app.polishapps.patches.vulcan.nativeLibrary
import app.polishapps.patches.vulcan.textFileOffset
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private val manifestFlags = mapOf(
    "firebase_analytics_collection_deactivated" to "true",
    "google_analytics_adid_collection_enabled" to "false",
    "google_analytics_ssaid_collection_enabled" to "false",
    "firebase_crashlytics_collection_enabled" to "false",
    "firebase_sessions_enabled" to "false",
    "firebase_performance_collection_deactivated" to "true",
)

private val manifestPatch = resourcePatch(
    description = "Disables Firebase Analytics, Crashlytics, Performance and Sessions before they start.",
) {
    compatibleWith(COMPATIBILITY_VULCAN)

    execute {
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("AndroidManifest.xml has no <application>")

            val metaData = application.getElementsByTagName("meta-data")
            manifestFlags.forEach { (name, value) ->
                val existing = (0 until metaData.length)
                    .map { metaData.item(it) as Element }
                    .firstOrNull { it.getAttribute("android:name") == name }
                val element = existing ?: manifest.createElement("meta-data").also { created ->
                    created.setAttribute("android:name", name)
                    application.appendChild(created)
                }
                element.setAttribute("android:value", value)
            }
        }
    }
}

/**
 * Obfuscated entry points of the bundled Firebase SDK that decide whether data may be collected.
 * Obfuscated names change per release; see [targetsByVersion].
 */
private data class TelemetryTargets(
    val measurementClient: String,
    val crashlyticsArbiter: String,
    val performanceConfig: String,
    val sessionsConfig: String,
)

private val targetsByVersion = mapOf(
    VULCAN_26_08_00 to TelemetryTargets(
        measurementClient = "Lcom/google/android/gms/internal/measurement/h7;",
        crashlyticsArbiter = "Lo7/y;",
        performanceConfig = "Ls8/a;",
        sessionsConfig = "Ll9/a;",
    ),
)

/**
 * Mnemos tracker is Dart code.
 */
private data class DartTelemetryTargets(
    val libPath: String,
    val snapshotBuildId: String,
    /** `b.ne` in `Tracker.traversal` that skips tracking when analytics consent is off. */
    val trackerSkipBranch: Long,
)

private val dartTargetsByVersion = mapOf(
    VULCAN_26_08_00 to DartTelemetryTargets(
        libPath = "lib/arm64-v8a/libapp.so",
        snapshotBuildId = "d6031998d1b3bbfebf59cc9bbff9aee1",
        trackerSkipBranch = 0x008b2a74,
    ),
)

/** `b.ne 0x8b2b14`, the jump to the early-return path when analytics is off. */
private val TRACKER_SKIP_BRANCH = byteArrayOf(0x01, 0x05, 0x00, 0x54)

/** `b 0x8b2b14`, the same jump but unconditional. */
private val TRACKER_ALWAYS_SKIP = byteArrayOf(0x28, 0x00, 0x00, 0x14)

private val dartTelemetryPatch = resourcePatch(
    description = "Stops the Mnemos tracker from queueing and uploading usage events.",
) {
    compatibleWith(COMPATIBILITY_VULCAN)

    execute {
        val targets = targetsFor(dartTargetsByVersion)
        val file = nativeLibrary(targets.libPath)
        val bytes = file.readBytes()
        if (!bytes.containsSnapshotBuildId(targets.snapshotBuildId)) {
            throw PatchException(
                "${targets.libPath} does not contain the expected Dart snapshot build ID " +
                    targets.snapshotBuildId,
            )
        }

        val offset = bytes.textFileOffset(targets.trackerSkipBranch).toInt()
        bytes.expectBytes(offset, TRACKER_SKIP_BRANCH, "Tracker.traversal analytics branch")
        TRACKER_ALWAYS_SKIP.copyInto(bytes, offset)
        file.writeBytes(bytes)
    }
}

@Suppress("unused")
val blockTelemetryPatch = bytecodePatch(
    name = "Block telemetry",
    description = "Blocks Firebase Analytics, Crashlytics, Performance and Sessions, plus the built-in " +
        "Mnemos usage tracker. Push notifications and Firebase Remote Config keep working.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_VULCAN)

    dependsOn(manifestPatch)
    dependsOn(dartTelemetryPatch)

    execute {
        val targets = targetsFor(targetsByVersion)

        val measurementConstructor = methodFingerprint(
            targets.measurementClient,
            "<init>",
            parameters = listOf("Landroid/content/Context;", "Landroid/os/Bundle;"),
            returnType = "V",
        ).method
        val superCall = measurementConstructor.instructions.toList().indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_DIRECT &&
                (instruction as? ReferenceInstruction)?.reference.let { reference ->
                    reference is MethodReference &&
                        reference.definingClass == "Ljava/lang/Object;" &&
                        reference.name == "<init>"
                }
        }
        if (superCall < 0) {
            throw PatchException("${targets.measurementClient}<init>: super constructor call not found")
        }
        measurementConstructor.addInstructions(
            superCall + 1,
            """
                const/4 v0, 0x1
                iput-boolean v0, p0, ${targets.measurementClient}->e:Z
            """,
        )

        // Crashlytics automatic data collection arbiter.
        methodFingerprint(targets.crashlyticsArbiter, "a", returnType = "Z").method.prependReturn(
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // Performance collection switch.
        methodFingerprint(targets.performanceConfig, "o", returnType = "Z").method.prependReturn(
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // Firebase Sessions local override (`firebase_sessions_enabled`).
        methodFingerprint(targets.sessionsConfig, "a", returnType = "Ljava/lang/Boolean;").method.prependReturn(
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """,
        )
    }
}
