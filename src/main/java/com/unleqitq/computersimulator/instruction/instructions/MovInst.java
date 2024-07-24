package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class MovInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination;
	@NotNull
	private final ValueWrapper source;
	
	public MovInst(@NotNull ValueWrapper destination, @NotNull ValueWrapper source) {
		super(InstructionDef.MOV);
		this.destination = destination;
		this.source = source;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		destination.write(ctx, source.read(ctx));
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		destination.assemble(buf);
		source.assemble(buf);
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return destination.toAssembly() + ", " + source.toAssembly();
	}
	
	@Override
	protected int getPayloadLength() {
		return destination.getLength() + source.getLength();
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new MovInst(destination.resolved(labelResolver), source.resolved(labelResolver));
	}
	
	@NotNull
	public static MovInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		return new MovInst(ValueWrapper.parse(parts[0].trim()), ValueWrapper.parse(parts[1].trim()));
	}
	
	@NotNull
	public static MovInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination = ValueWrapper.load(ctx);
		ValueWrapper source = ValueWrapper.load(ctx);
		return new MovInst(destination, source);
	}
	
}
