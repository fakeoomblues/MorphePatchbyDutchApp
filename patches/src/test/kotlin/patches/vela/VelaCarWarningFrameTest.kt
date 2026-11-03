package patches.vela

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.*
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.*
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test

class VelaCarWarningFrameTest {
    private fun classes():List<ClassDef>{val path=System.getenv("VELA_ORIGINAL_APK");assumeNotNull(path);val dex=DexFileFactory.loadDexContainer(File(path!!),Opcodes.forApi(26));return dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }.toList()}
    private fun refs(m:Method)=m.implementation!!.instructions.mapNotNull { (it as? ReferenceInstruction)?.reference?.toString() }.filterNot { it==VELA_CAR_WARNING_HOOK }
    private fun target(m:Method,index:Int):Instruction{val code=m.implementation!!.instructions.toList();val at=code.take(index).sumOf { it.codeUnits };val wanted=at+(code[index] as OffsetInstruction).codeOffset;var p=0;return code.single { val match=p==wanted;p+=it.codeUnits;match }}
    private fun roundtrip(classes:List<ClassDef>,method:Method):Method{val owner=classes.single { it.type==method.definingClass };val pool=DexPool(Opcodes.forApi(26));pool.internClass(ImmutableClassDef(owner.type,owner.accessFlags,owner.superclass,owner.interfaces,owner.sourceFile,owner.annotations,owner.fields,owner.methods.map { if(it.toString()==method.toString())method else it }));val memory=MemoryDataStore();val bytes=try{pool.writeTo(memory);memory.data}finally{memory.close()};return DexBackedDexFile.fromInputStream(Opcodes.forApi(26),ByteArrayInputStream(bytes)).classes.single().methods.single { it.toString()==VELA_CAR_FRAME }}
    @Test fun bothNativeSkipBranchesReachTheRealFrameHookAndOriginalCallsSurviveDexAssembly(){val classes=classes();val original=validateVelaCarWarningFrame(classes);val changed=MutableMethod(original);bindVelaCarWarningFrame(changed);val assembled=roundtrip(classes,changed);assertEquals(refs(original),refs(assembled));assertEquals(25,assembled.implementation!!.registerCount);val code=assembled.implementation!!.instructions.toList();val hook=code.withIndex().single { (it.value as? ReferenceInstruction)?.reference?.toString()==VELA_CAR_WARNING_HOOK };assertEquals(286,hook.index);val invoke=hook.value as FiveRegisterInstruction;assertEquals(2,invoke.registerCount);assertEquals(1,invoke.registerC);assertEquals(2,invoke.registerD);for(index in listOf(274,284))assertEquals("False native navigation/SpeedDisplay branches must run the independent warning layer",VELA_CAR_WARNING_HOOK,(target(assembled,index) as ReferenceInstruction).reference.toString());assertEquals("Lcw;->g(Landroid/graphics/Canvas;)V",(code[285] as ReferenceInstruction).reference.toString());assertEquals("Lcw;->u()Landroid/graphics/Rect;",(code[287] as ReferenceInstruction).reference.toString());}
    @Test fun changedNativeRegistersAndReapplicationFailBeforeAnyMutation(){val original=validateVelaCarWarningFrame(classes());val changed=MutableMethod(original);changed.replaceInstruction(285,"invoke-virtual {v1, v3}, Lcw;->g(Landroid/graphics/Canvas;)V");val before=velaFingerprint(changed);assertThrows(IllegalStateException::class.java){bindVelaCarWarningFrame(changed)};assertEquals(before,velaFingerprint(changed));val once=MutableMethod(original);bindVelaCarWarningFrame(once);val onceHash=velaFingerprint(once);assertThrows(IllegalStateException::class.java){bindVelaCarWarningFrame(once)};assertEquals(onceHash,velaFingerprint(once));}
}
