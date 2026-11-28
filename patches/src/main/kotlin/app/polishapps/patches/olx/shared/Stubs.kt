package app.polishapps.patches.olx.shared

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.polishapps.patches.inpost.shared.methodsOf
import app.polishapps.patches.inpost.shared.parameterTypeNames
import app.polishapps.patches.shared.prependReturn
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method

/** A method picked by name and, optionally, by exact parameter list. */
internal data class MethodSpec(val name: String, val parameters: List<String>? = null) {
    fun matches(method: Method) =
        method.name == name && (parameters == null || method.parameterTypeNames() == parameters)
}

/**
 * Prepends [smali] to every method of [type] matched by [specs]. Each spec must match at least one
 * method, so a renamed SDK method fails the patch instead of being skipped silently.
 */
internal fun BytecodePatchContext.stub(type: String, smali: String, vararg specs: MethodSpec) {
    val classDef = classDefByOrNull(type) ?: throw PatchException("$type not found")
    specs.forEach { spec ->
        val methods = methodsOf(classDef) { spec.matches(it) && it.implementation != null }
        if (methods.isEmpty()) throw PatchException("$type: ${spec.name}${spec.parameters ?: ""} not found")
        methods.forEach { it.prependReturn(smali) }
    }
}

internal fun BytecodePatchContext.stubVoid(type: String, vararg names: String) =
    stub(type, "return-void", *names.map { MethodSpec(it) }.toTypedArray())

/**
 * Makes the methods return the constant [value] (`0` is also `null` for object return types).
 * `const/4` only reaches v0-v15, so the constant goes into v0 when the method has a local, and into
 * the first parameter register otherwise (which is then v0-v15, since there are no locals).
 */
internal fun BytecodePatchContext.stubReturn(type: String, value: Int, vararg specs: MethodSpec) {
    val classDef = classDefByOrNull(type) ?: throw PatchException("$type not found")
    specs.forEach { spec ->
        val methods = methodsOf(classDef) { spec.matches(it) && it.implementation != null }
        if (methods.isEmpty()) throw PatchException("$type: ${spec.name}${spec.parameters ?: ""} not found")
        methods.forEach { method ->
            val parameterRegisters = method.parameterTypes.sumOf { if (it == "J" || it == "D") 2 else 1 } +
                if (AccessFlags.STATIC.isSet(method.accessFlags)) 0 else 1
            val locals = method.implementation!!.registerCount - parameterRegisters
            val register = when {
                locals > 0 -> "v0"
                parameterRegisters > 0 -> "p0"
                else -> throw PatchException("$type->${method.name} has no register for a constant")
            }
            val returnOpcode = when (method.returnType.first()) {
                'L', '[' -> "return-object"
                'V', 'J', 'D' -> throw PatchException("$type->${method.name}: unsupported return type")
                else -> "return"
            }
            method.prependReturn("const/4 $register, $value\n$returnOpcode $register")
        }
    }
}

/**
 * Replaces each host in `lib/arm64-v8a/libapp.so` with one of the same length. Dart AOT snapshots keep
 * string literals as plain one-byte strings, so an in-place rewrite keeps the snapshot valid. Every
 * host must be found, at least once.
 */
internal fun ResourcePatchContext.rewriteDartHosts(hosts: Map<String, String>) {
    val file = get("lib/arm64-v8a/libapp.so")
    if (!file.exists()) throw PatchException("lib/arm64-v8a/libapp.so not found in the patched APK")
    val bytes = file.readBytes()

    hosts.forEach { (from, to) ->
        if (from.length != to.length) throw PatchException("$to must have the same length as $from")
        val needle = from.toByteArray(Charsets.ISO_8859_1)
        val replacement = to.toByteArray(Charsets.ISO_8859_1)
        var found = 0
        var index = bytes.indexOf(needle, 0)
        while (index >= 0) {
            replacement.copyInto(bytes, index)
            found++
            index = bytes.indexOf(needle, index + needle.size)
        }
        if (found == 0) throw PatchException("libapp.so: $from not found")
    }

    file.writeBytes(bytes)
}

private fun ByteArray.indexOf(needle: ByteArray, from: Int): Int {
    outer@ for (start in from..size - needle.size) {
        for (offset in needle.indices) {
            if (this[start + offset] != needle[offset]) continue@outer
        }
        return start
    }
    return -1
}
