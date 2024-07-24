package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class XchgInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination1;
	@NotNull
	private final ValueWrapper destination2;
	
	public XchgInst(@NotNull ValueWrapper destination1, @NotNull ValueWrapper destination2) {
		super(InstructionDef.XCHG);
		this.destination1 = destination1;
		this.destination2 = destination2;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long value1 = destination1.read(ctx);
		long value2 = destination2.read(ctx);
		destination1.write(ctx, value2);
		destination2.write(ctx, value1);
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		destination1.assemble(buf);
		destination2.assemble(buf);
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return destination1.toAssembly() + ", " + destination2.toAssembly();
	}
	
	@Override
	protected int getPayloadLength() {
		return destination1.getLength() + destination2.getLength();
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new XchgInst(destination1.resolved(labelResolver), destination2.resolved(labelResolver));
	}
	
	@NotNull
	public static XchgInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		return new XchgInst(ValueWrapper.parse(parts[0].trim()), ValueWrapper.parse(parts[1].trim()));
	}
	
	@NotNull
	public static XchgInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination1 = ValueWrapper.load(ctx);
		ValueWrapper destination2 = ValueWrapper.load(ctx);
		return new XchgInst(destination1, destination2);
	}
	
}
