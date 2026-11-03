package patches.vela

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

internal const val VELA_CAR_FRAME = "Lxv;->a(Lorg/maplibre/android/snapshotter/MapSnapshot;)V"
internal const val VELA_CAR_WARNING_HOOK = "Lnl/johninnl/vela/CarUi;->warningFrame(Ljava/lang/Object;Landroid/graphics/Canvas;)V"
private const val FRAME_HASH = "005a5364b0b0636cef02de4ccdafc8db413745b565e90b800fbb4c0abe9fd3d8"

/** The real MapSnapshot callback owns this Canvas; no replacement surface or navigation state. */
internal fun validateVelaCarWarningFrame(classes: Iterable<ClassDef>): Method {
    val all=classes.toList();val owner=all.single { it.type=="Lxv;" }
    check(owner.sourceFile==VELA_MAP_ID) { "Changed Vela car frame owner" }
    val method=owner.methods.single { it.toString()==VELA_CAR_FRAME }
    validateFrame(method)
    val renderer=all.single { it.type=="Lcw;" }
    check(renderer.fields.count { it.name=="I"&&it.type=="Z" }==1)
    check(renderer.methods.count { it.toString()=="Lcw;->n()Z" }==1)
    check(renderer.methods.count { it.toString()=="Lcw;->s()V" }==1)
    return method
}
private fun validateFrame(method:Method) {
    val body=method.implementation?:error("Missing Vela car frame")
    check(method.toString()==VELA_CAR_FRAME&&method.accessFlags==17&&body.registerCount==25
        &&body.instructions.count()==335&&velaFingerprint(method)==FRAME_HASH) {
        "Changed or already patched Vela native car frame $method"
    }
}
context(context: BytecodePatchContext)
internal fun validateVelaCarWarningExtension() {
    check(context.classDefBy("Lnl/johninnl/vela/CarUi;").methods.count { it.toString()==VELA_CAR_WARNING_HOOK }==1) {
        "Missing Vela warning frame extension"
    }
}
internal fun bindVelaCarWarningFrame(method:MutableMethod) {
    validateFrame(method)
    val native=method.getInstruction<FiveRegisterInstruction>(285)
    check(method.getInstruction(285).opcode==Opcode.INVOKE_VIRTUAL
        &&(native as ReferenceInstruction).reference.toString()=="Lcw;->g(Landroid/graphics/Canvas;)V"
        &&native.registerCount==2&&native.registerC==1&&native.registerD==2)
    val join=method.getInstruction<FiveRegisterInstruction>(286)
    check(method.getInstruction(286).opcode==Opcode.INVOKE_VIRTUAL
        &&(join as ReferenceInstruction).reference.toString()=="Lcw;->u()Landroid/graphics/Rect;"
        &&join.registerCount==1&&join.registerC==native.registerC)
    // Both navigation and SpeedDisplay false branches target this location. Replacement keeps
    // those incoming labels on the warning hook; plain insertion would skip it on both paths.
    method.replaceInstruction(286,"invoke-static {v1, v2}, $VELA_CAR_WARNING_HOOK")
    method.addInstructions(287,"invoke-virtual {v1}, Lcw;->u()Landroid/graphics/Rect;")
}
