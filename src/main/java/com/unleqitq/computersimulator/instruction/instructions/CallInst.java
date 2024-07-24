package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class CallInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public CallInst(@NotNull ValueWrapper target) {
		super(InstructionDef.CALL);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		ctx.stack().pushQword(ctx.instructionPointer() + ctx.instructionSize());
		ctx.stack().pushQword(ctx.stack().getBasePointer());
		ctx.jump(target.read(ctx));
		ctx.stack().setBasePointer(ctx.stack().getStackPointer());
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		target.assemble(buf);
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return target.toAssembly();
	}
	
	@Override
	protected int getPayloadLength() {
		return target.getLength();
	}
	
	@NotNull
	public CallInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new CallInst(target.resolved(labelResolver));
	}
	
	@NotNull
	public static CallInst parse(@NotNull String code) {
		return new CallInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static CallInst load(@NotNull InstructionContext ctx) {
		return new CallInst(ValueWrapper.load(ctx));
	}
	
}
