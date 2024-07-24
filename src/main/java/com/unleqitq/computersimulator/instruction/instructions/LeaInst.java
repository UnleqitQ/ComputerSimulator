package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.ValueSize;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class LeaInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination;
	@NotNull
	private final ValueWrapper.MemoryValueWrapper source;
	
	public LeaInst(@NotNull ValueWrapper destination,
		@NotNull ValueWrapper.MemoryValueWrapper source) {
		super(InstructionDef.LEA);
		this.destination = destination;
		this.source = source;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		destination.write(ctx, source.getAddress(ctx));
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		destination.assemble(buf);
		source.assembleMemory(buf);
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return destination.toAssembly() + ", " + source.toAssemblyMemory();
	}
	
	@Override
	protected int getPayloadLength() {
		return destination.getLength() + source.getLengthMemory();
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new LeaInst(destination.resolved(labelResolver), source.resolved(labelResolver));
	}
	
	@NotNull
	public static LeaInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		return new LeaInst(ValueWrapper.parse(parts[0].trim()),
			ValueWrapper.parseMemory(parts[1].trim(), ValueSize.QWORD));
	}
	
	@NotNull
	public static LeaInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination = ValueWrapper.load(ctx);
		ValueWrapper.MemoryValueWrapper source = ValueWrapper.loadMemory(ctx, ValueSize.QWORD);
		return new LeaInst(destination, source);
	}
	
}
