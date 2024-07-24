package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import com.unleqitq.computersimulator.utils.FlagsUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class AndInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination;
	@NotNull
	private final ValueWrapper source;
	
	public AndInst(@NotNull ValueWrapper destination, @NotNull ValueWrapper source) {
		super(InstructionDef.AND);
		this.destination = destination;
		this.source = source;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long sourceValue = source.read(ctx);
		long destinationValue = destination.read(ctx);
		long result = destinationValue & sourceValue;
		destination.write(ctx, result);
		
		ctx.registers()
			.writeFlag(Registers.Flag.ZERO, FlagsUtils.isZeroFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.SIGN, FlagsUtils.isSignFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.PARITY, FlagsUtils.isParityFlag(destination.getSize(), result));
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		destination.assemble(buf);
		source.assemble(buf);
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new AndInst(destination.resolved(labelResolver), source.resolved(labelResolver));
	}
	
	@Override
	protected int getPayloadLength() {
		return destination.getLength() + source.getLength();
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return destination.toAssembly() + ", " + source.toAssembly();
	}
	
	@NotNull
	public static AndInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		return new AndInst(ValueWrapper.parse(parts[0].trim()), ValueWrapper.parse(parts[1].trim()));
	}
	
	@NotNull
	public static AndInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination = ValueWrapper.load(ctx);
		ValueWrapper source = ValueWrapper.load(ctx);
		return new AndInst(destination, source);
	}
	
}
