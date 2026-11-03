package org.ungoogled.patches.maps.ui.speed

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload
import java.security.MessageDigest

private const val COORDINATOR="Lorg/ungoogled/ui/MapsGuidanceCoordinator;"
internal data class MapsVoiceContract(val registers:Int,val count:Int,val flags:Int,val hash:String)
internal val mapsVoiceContracts=mapOf(
    "Lbjis;->b()D" to MapsVoiceContract(3, 4, 17, "d37441c1cdd598e06556c4be17b2ef89aff77ff6f7ea349e2ca9596a27ea64d3"),
    "Lbjis;->d()D" to MapsVoiceContract(3, 4, 17, "4970475f59e7b11ee1269fa01446e2a335fdbb9f04b27519fa2571fee9d53d41"),
    "Lbjix;->g()I" to MapsVoiceContract(1, 4, 17, "7748607af06abd2aa78ab1626fdd545cb538c81de5e803c69a45d9796ef1c9e0"),
    "Lbjix;->n(I)Lbjis;" to MapsVoiceContract(7, 16, 17, "1ca6ab4a273501354a404d95038c88274fc3a3e238bd48a5366dbcd78e3ac7ad"),
    "Lblot;-><init>(Lblos;)V" to MapsVoiceContract(4, 43, 65537, "3a4c2c327dd341bfaa53f6114f3ac5a60a658618e6dc745797654c991662e268"),
    "Lyap;-><init>(Lyak;)V" to MapsVoiceContract(20, 465, 65537, "bb627003996140e08c5a426c37953704c8fff380651bc1594a023303b17c8747"),
    "Lbmrq;->g(Lbmuk;I)Z" to MapsVoiceContract(7, 66, 17, "b398d5071ccefea508c0cc6b32b93b01a22e86f70cea02e4666783726567ba02"),
    "Lbmrc;->q(Lbmug;Lbmub;)V" to MapsVoiceContract(3, 5, 8, "ad2ab4bd67b399136a4576e64a4e9c9745cc8cc2a078dd1d95fc0e252e5bcbb0"),
    "Lbmtl;->e(Lbmuc;)V" to MapsVoiceContract(3, 5, 17, "e1f9b5a8933e5f0fdd9f66bc20b3772d9f77e10c25f87dec0fa917453303ef42"),
    "Lbmrb;->b(Ljava/lang/Object;)V" to MapsVoiceContract(3, 30, 4177, "d90d8826de779635d40508a241d6f101fada464e8e7cf6b8e4f70a8a600747d9"),
    "Lbmrc;->B()Z" to MapsVoiceContract(2, 8, 17, "2996c1f3322a46e2ce6d3f9385af948d81f8a414e1f6ed7c9f8cb60d4ba33b4b"),
    "Lbmrc;->k(Lbmvk;Lbmuk;Lbmuc;Lbmug;)Lcom/google/common/util/concurrent/ListenableFuture;" to MapsVoiceContract(26, 123, 17, "f50ec1112a1c4045b47e486f69180497999c075e4a84d85afddaa2de09098304"),
    "Lbmrc;->l(Lbmud;)V" to MapsVoiceContract(4, 6, 17, "6a667183f58005fa73c34c8dd294f4527e6e35eafcd92c0c572e94cf336221d7"),
    "Lbmrc;->m()V" to MapsVoiceContract(3, 6, 17, "28e92e30ea8ff1e14e8c144b1f52cdaf9cfc8695274412fe674a8f8e0420fc54"),
    "Lbmrc;->n()V" to MapsVoiceContract(3, 18, 17, "c78c6d884c61d126f505117df048fceb9db1c4efb8ada4e8f599f0174767a0f2"),
    "Lbmrc;->o()V" to MapsVoiceContract(2, 3, 17, "be0f8ac318fe0432c6bdc9334c6695967c983463993d5918a013ca6949560b9e"),
    "Lbmrc;->p(Lbmtl;)V" to MapsVoiceContract(6, 15, 17, "aa2a8322cf7d032965bb3c6ae1850a0e352176dcdca8e08ffea7ef7824946d2d"),
    "Lbmrc;->y(Lbmtl;)V" to MapsVoiceContract(5, 23, 17, "e60361dd985a05e9e1ddae6e11c38415f56933d268fe8b4c6391b5b420e056ed"),
    "Lbmrc;->z(Lbmuf;)V" to MapsVoiceContract(6, 12, 17, "2fed260ea703e47118ce70b0cec71f019a63649540bae386617c53e5a14de1c5"),
    "Lbmsk;->d(Lbmua;)V" to MapsVoiceContract(5, 37, 131089, "4f61d0978a821829f1e1e9d6a60539c94d32ea6927fa24a75319fbf3a5a374d0"),
    "Lbmsk;->g()V" to MapsVoiceContract(4, 16, 16, "cc8e16b4da0a4419325a28f5226e20cc5b4e049a8245bfe7ad6cd64362e0a71f"),
    "Lbmsk;->l()V" to MapsVoiceContract(1, 2, 17, "1cbf34e05863c2183ddfe80510309f6d7f5aecdabf58648767e2784525eae051"),
    "Lbmsp;->b()Lbmun;" to MapsVoiceContract(2, 16, 131089, "83c36ecba5380c50950dc30a6fbe189702f93668789bbb76462dbe7e98527034"),
    "Lbmsp;->c()Lbmun;" to MapsVoiceContract(2, 20, 131089, "f7a46228f568e74e50f2fa9fe66123da47ceb69478f82f0e2e1242c8402f38df"),
    "Lbmsp;->l(Lbmuk;)Z" to MapsVoiceContract(3, 18, 131089, "ecc9822344b84d91a278c0b5c0bcaacf363e8b6df6733de31af7a0d3d6dcfffc"),
    "Lbmtd;->b(Ljava/lang/Object;)V" to MapsVoiceContract(2, 8, 4177, "e48351425cf5f2386b96984aad5e281743c7a5ce6ea879180d332bcc21fae636"),
    "Lbmtf;->b(Ljava/lang/Object;)V" to MapsVoiceContract(16, 490, 4113, "e7285614a3ea4e6fcf2f80d27079b465bdbef6d5604a90bfcbc7eea32ff83135"),
    "Lbmth;->a(Lbmuc;)V" to MapsVoiceContract(4, 35, 17, "dee2bdd2fc077dde0e3a7402c63af8f9707cec46102956dd2cff71c631d40e3a"),
    "Lbmth;->b(Lbmuc;)V" to MapsVoiceContract(5, 17, 17, "413e6c4c79fa4bc3d777b55707282a1e371d87752da1827338837ef1e976ce76"),
    "Lbmtl;->d()V" to MapsVoiceContract(4, 11, 16, "4c5caafff50528b4b1789ba8173140325825af78907df1a8665a068426d3d561"),
    "Lbmtl;->g()V" to MapsVoiceContract(3, 7, 17, "4d67c45f19f64f9c440eb6f755952f4c4e0a5248a7e379c7bc673daee1212210"),
    "Lbmtl;->h()V" to MapsVoiceContract(3, 8, 17, "79fd02dcdfd4e4db49f569da3ca6d652cd044ea970661659507039a2354083d0"),
    "Lbmuf;-><clinit>()V" to MapsVoiceContract(13, 40, 65544, "5b4f9edb1bd6d75e452b5654ab1a0e352c29afdcfddb031e8f1894edf1a05a90"),
    "Lbmuk;-><clinit>()V" to MapsVoiceContract(34, 324, 65544, "fd83eb436431e0a1c7238ff1eb5a1c99ad020fe92bf9349609be2191c8de70ca"),
    "Lbmun;-><clinit>()V" to MapsVoiceContract(7, 22, 65544, "3a99edf83d697d363203cbe5b125cb75468b6db02409458e2d9724017a2028ba"),
    "Lbmia;-><init>(Lbmhh;Lbmhm;Landroid/content/Context;Lbndw;Lcqmo;Lcqmo;Ljava/util/concurrent/Executor;Lazjr;Lawrs;Lbmdy;)V" to MapsVoiceContract(14, 41, 65537, "9bbf3aad06577a0b0ad8391871e5759ca2a38d6af9cd71d79b2abb30cc27a882"),
    "Lbmia;->u(Lybc;)V" to MapsVoiceContract(6, 44, 17, "e733227fd76bb0ac846d9b21280489289de2a8fae14af4e3d9b4b4acab07107f"),
    "Lbmia;->v()V" to MapsVoiceContract(2, 10, 17, "92fd5bf434f2da2c64116a2040c5a301270d182768209dfc3b5c4833e0592cb9"),
    "Lbmsh;->d(Lbmua;)V" to MapsVoiceContract(6, 17, 17, "2bb8c1eea71d44dbac55f41b79c136efaf25fe1e6ba583f410ee6dc4f1d7bf88"),
    "Lbmsh;->l()V" to MapsVoiceContract(4, 9, 17, "c910363b1beaec254e1aedda2d066f14c933d559b9483892cceae09927c4c918"),
    "Lbmtu;->d(Lbmua;)V" to MapsVoiceContract(4, 12, 17, "5ae9870bccd5540e8d72cfa24c77f2f4a1c5fb8246d3f7a01d2ae55583123e5a"),
    "Lbmtu;->l()V" to MapsVoiceContract(2, 6, 17, "f0b9288e486fe4edf5eb4e14276364f5896b4102bf5f4b590c370a2400d8bf82"),
)

/** Same primitive JSON body representation as the independent original-DEX inspector. */
internal fun mapsVoiceFingerprint(method:Method):String {
    val body=method.implementation ?: error("Missing Maps voice body")
    val rows=mutableListOf<Any?>()
    body.instructions.forEach { i ->
        // Opcode exposes a lowercase mnemonic field also named `name`; the oracle uses Enum.name().
        val row=mutableListOf<Any?>(i.opcode.toString(),i.codeUnits)
        if(i is OneRegisterInstruction)row.add(listOf("a",i.registerA))
        if(i is TwoRegisterInstruction)row.add(listOf("b",i.registerB))
        if(i is ThreeRegisterInstruction)row.add(listOf("c",i.registerC))
        if(i is FiveRegisterInstruction)row.add(listOf("invoke",i.registerCount,i.registerC,i.registerD,i.registerE,i.registerF,i.registerG))
        if(i is RegisterRangeInstruction)row.add(listOf("range",i.startRegister,i.registerCount))
        if(i is WideLiteralInstruction)row.add(listOf("literal",i.wideLiteral))
        if(i is OffsetInstruction)row.add(listOf("offset",i.codeOffset))
        if(i is ReferenceInstruction)row.add(listOf("reference",i.reference.toString()))
        if(i is DualReferenceInstruction)row.add(listOf("reference2",i.reference2.toString()))
        if(i is SwitchPayload)i.switchElements.forEach { row.add(listOf("switch",it.key,it.offset)) }
        if(i is ArrayPayload)row.add(listOf("array",i.elementWidth,i.arrayElements))
        rows.add(row)
    }
    body.tryBlocks.forEach { b -> rows.add(listOf("try",b.startCodeAddress,b.codeUnitCount,b.exceptionHandlers.map { listOf(it.exceptionType,it.handlerCodeAddress) })) }
    fun json(value:Any?):String=when(value) {
        null -> "null"
        is String -> buildString {
            append('"');value.forEach { c -> when(c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if(c<' ' || c=='\u2028' || c=='\u2029')append("\\u%04x".format(c.code)) else append(c)
            } };append('"')
        }
        is Iterable<*> -> value.joinToString(",","[","]") { json(it) }
        else -> value.toString()
    }
    return MessageDigest.getInstance("SHA-256").digest(json(rows).toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it.toInt() and 255)}
}

internal fun validateMapsWarningVoice(classes:Iterable<ClassDef>):Map<String,Method> {
    val byType=classes.associateBy { it.type }
    listOf(Triple("Lblot;","b","Lyap;"),Triple("Lyap;","k","Lbjix;")).forEach { (owner,name,type) ->
        check(byType.getValue(owner).fields.count { it.name==name && it.type==type }==1) { "Changed Maps native route geometry field $owner->$name" }
    }
    return mapsVoiceContracts.mapValues { (descriptor,c) ->
        val m=byType.getValue(descriptor.substringBefore("->")).methods.single { it.toString()==descriptor }
        check(m.accessFlags==c.flags && m.implementation?.registerCount==c.registers && m.implementation!!.instructions.count()==c.count && mapsVoiceFingerprint(m)==c.hash) { "Changed or already patched Maps warning voice $descriptor" }
        m
    }
}

context(context:BytecodePatchContext)
internal fun bindMapsWarningVoice() {
    val classes=mapsVoiceContracts.keys.map { it.substringBefore("->") }.distinct().map { context.classDefBy(it) }
    val originals=validateMapsWarningVoice(classes)
    val methods=originals.keys.associateWith { descriptor -> context.mutableClassDefBy(descriptor.substringBefore("->")).methods.single { it.toString()==descriptor } }
    val extension=context.mutableClassDefBy(COORDINATOR)
    listOf("dispatch(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", "terminal(Ljava/lang/Object;Ljava/lang/Object;)V", "captureRoute(Ljava/lang/Object;)V", "routeEnded(Ljava/lang/Object;)V", "hooksInstalled()Z", "requestNativeFocus(Landroid/media/AudioManager;Landroid/media/AudioFocusRequest;)I", "cancelPreparedCallback(Ljava/lang/Object;)Z").forEach { signature ->
        check(extension.methods.count { it.toString()=="$COORDINATOR->$signature" }==1) { "Missing Maps warning extension $signature" }
    }
    bindMapsNativeVoice(methods)
    val installed=extension.methods.single { it.name=="hooksInstalled" }
    installed.replaceInstruction(0,"const/4 v0, 0x1")
}

internal fun bindMapsNativeVoice(methods:Map<String,MutableMethod>) {
    fun pick(owner:String,name:String)=methods.values.single { it.definingClass==owner && it.name==name }
    val dispatch=pick("Lbmtf;","b")
    val anchors=dispatch.instructions.withIndex().filter { (it.value as? ReferenceInstruction)?.reference?.toString()=="Lbmuc;->d(Lbmua;)V" }
    check(anchors.size==1)
    val at=anchors.single().index;val call=anchors.single().value as FiveRegisterInstruction
    check(at==417 && call.registerCount==2 && call.registerC==14 && call.registerD==1)
    check((dispatch.instructions[at-1] as ReferenceInstruction).reference.toString()=="Lbmtl;->w:Lbmua;")
    check((dispatch.instructions[at-1] as TwoRegisterInstruction).registerB==15)
    // Replace the existing invoke so all incoming branches run the final asynchronous fence.
    dispatch.replaceInstruction(at,"invoke-static {v15, v14, v1}, $COORDINATOR->dispatch(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V")
    dispatch.addInstructionsWithLabels(0,"""
        invoke-static/range {p0 .. p0}, $COORDINATOR->cancelPreparedCallback(Ljava/lang/Object;)Z
        move-result v0
        if-eqz v0, :maps_native_ready_callback
        return-void
    """,ExternalLabel("maps_native_ready_callback",dispatch.getInstruction(0)))
    val focus=pick("Lbmrq;","g")
    val outline="Laq\$\$ExternalSyntheticApiModelOutline0;->m\$1(Landroid/media/AudioManager;Landroid/media/AudioFocusRequest;)I"
    val focusAt=focus.instructions.withIndex().filter { (it.value as? ReferenceInstruction)?.reference?.toString()==outline }.single().index
    val focusCall=focus.instructions[focusAt] as FiveRegisterInstruction
    check(focusCall.registerCount==2 && focus.instructions[focusAt].opcode==Opcode.INVOKE_STATIC)
    focus.replaceInstruction(focusAt,"invoke-static {v${focusCall.registerC}, v${focusCall.registerD}}, $COORDINATOR->requestNativeFocus(Landroid/media/AudioManager;Landroid/media/AudioFocusRequest;)I")
    pick("Lbmth;","a").addInstruction(0,"invoke-static/range {p0 .. p1}, $COORDINATOR->terminal(Ljava/lang/Object;Ljava/lang/Object;)V")
    val route=pick("Lbmia;","<init>")
    route.instructions.withIndex().filter { it.value.opcode==Opcode.RETURN_VOID }.map { it.index }.reversed().forEach { route.addInstruction(it,"invoke-static/range {p0 .. p0}, $COORDINATOR->captureRoute(Ljava/lang/Object;)V") }
    pick("Lbmia;","v").addInstruction(0,"invoke-static/range {p0 .. p0}, $COORDINATOR->routeEnded(Ljava/lang/Object;)V")
}
