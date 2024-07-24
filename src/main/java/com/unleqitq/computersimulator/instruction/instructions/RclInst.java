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

public class RclInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination;
	@NotNull
	private final ValueWrapper source;
	
	public RclInst(@NotNull ValueWrapper destination, @NotNull ValueWrapper source) {
		super(InstructionDef.RCL);
		this.destination = destination;
		this.source = source;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		// Rotate left through carry
		long sourceValue = source.read(ctx);
		long destinationValue = destination.read(ctx);
		long destinationSize = destination.getSize().getSize();
		long shift = Long.remainderUnsigned(sourceValue, destinationSize);
		long result = destinationValue;
		if (shift > 0) {
			result = (destinationValue << shift) | (destinationValue >>> (destinationSize - shift + 1));
			boolean oldCarry = ctx.registers().readFlag(Registers.Flag.CARRY);
			boolean newCarry = (destinationValue & (1L << (destinationSize - shift))) != 0;
			ctx.registers().writeFlag(Registers.Flag.CARRY, newCarry);
			result |= oldCarry ? (1L << (shift - 1)) : 0;
			destination.write(ctx, result);
		}
		
		ctx.registers()
			.writeFlag(Registers.Flag.SIGN, FlagsUtils.isSignFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.ZERO, FlagsUtils.isZeroFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.PARITY, FlagsUtils.isParityFlag(destination.getSize(), result));
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new RclInst(destination.resolved(labelResolver), source.resolved(labelResolver));
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		destination.assemble(buf);
		source.assemble(buf);
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
	public static RclInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		return new RclInst(ValueWrapper.parse(parts[0].trim()), ValueWrapper.parse(parts[1].trim()));
	}
	
	@NotNull
	public static RclInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination = ValueWrapper.load(ctx);
		ValueWrapper source = ValueWrapper.load(ctx);
		return new RclInst(destination, source);
	}
	
}
