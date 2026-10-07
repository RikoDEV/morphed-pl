package app.polishapps.patches.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

/** Builds a [Fingerprint] from a pinned declaration. */
internal fun methodFingerprint(
    definingClass: String,
    name: String,
    parameters: List<String> = emptyList(),
    returnType: String? = null,
) = Fingerprint(
    definingClass = definingClass,
    name = name,
    parameters = parameters,
    returnType = returnType,
)

/** Replaces the whole body of a method with [smali]. */
internal fun MutableMethod.replaceBody(smali: String) {
    removeInstructions(instructions.size)
    addInstructions(0, smali)
}

/**
 * Replaces every `instance-of vX, vY, <type>` with `const/4 vX, 0x1`,
 * mirroring the `force_premium` transform used by the manual smali patch.
 */
internal fun MutableMethod.forceInstanceOfTrue(type: String) {
    var replaced = 0
    for (index in instructions.indices) {
        val instruction = instructions[index]
        if (instruction.opcode != Opcode.INSTANCE_OF) continue
        instruction as BuilderInstruction22c
        val reference = instruction.reference as? TypeReference ?: continue
        if (reference.type != type) continue
        replaceInstruction(index, "const/4 v${instruction.registerA}, 0x1")
        replaced++
    }
    if (replaced == 0) {
        throw PatchException("No `instance-of $type` found in $definingClass->$name")
    }
}
