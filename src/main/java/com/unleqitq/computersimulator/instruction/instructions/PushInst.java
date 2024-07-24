package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class PushInst extends Instruction {
	
	@NotNull
	private final ValueWrapper source;
	
	public PushInst(@NotNull ValueWrapper source) {
		super(InstructionDef.PUSH);
		this.source = source;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long sourceValue = source.read(ctx);
		ctx.stack().push(sourceValue, source.getSize());
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new PushInst(source.resolved(labelResolver));
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		source.assemble(buf);
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return source.toAssembly();
	}
	
	@Override
	protected int getPayloadLength() {
		return source.getLength();
	}
	
	@NotNull
	public static PushInst parse(@NotNull String code) {
		return new PushInst(ValueWrapper.parse(code.trim()));
	}
	
	@NotNull
	public static PushInst load(@NotNull InstructionContext ctx) {
		ValueWrapper source = ValueWrapper.load(ctx);
		return new PushInst(source);
	}
	
}
