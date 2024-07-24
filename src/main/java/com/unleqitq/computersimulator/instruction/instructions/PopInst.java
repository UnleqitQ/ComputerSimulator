package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class PopInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination;
	
	public PopInst(@NotNull ValueWrapper destination) {
		super(InstructionDef.POP);
		this.destination = destination;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long value = ctx.stack().pop(destination.getSize());
		destination.write(ctx, value);
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		destination.assemble(buf);
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new PopInst(destination.resolved(labelResolver));
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return destination.toAssembly();
	}
	
	@Override
	protected int getPayloadLength() {
		return destination.getLength();
	}
	
	@NotNull
	public static PopInst parse(@NotNull String code) {
		return new PopInst(ValueWrapper.parse(code.trim()));
	}
	
	@NotNull
	public static PopInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination = ValueWrapper.load(ctx);
		return new PopInst(destination);
	}
	
}
