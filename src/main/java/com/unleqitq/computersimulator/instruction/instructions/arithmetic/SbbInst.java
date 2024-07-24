package com.unleqitq.computersimulator.instruction.instructions.arithmetic;

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

public class SbbInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination;
	@NotNull
	private final ValueWrapper source;
	
	public SbbInst(@NotNull ValueWrapper destination, @NotNull ValueWrapper source) {
		super(InstructionDef.SBB);
		this.destination = destination;
		this.source = source;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long sourceValue = source.read(ctx);
		long destinationValue = destination.read(ctx);
		long result = destinationValue - sourceValue - (ctx.registers().readFlag(Registers.Flag.CARRY) ? 1 : 0);
		destination.write(ctx, result);
		
		ctx.registers()
			.writeFlag(Registers.Flag.ZERO, FlagsUtils.isZeroFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.SIGN, FlagsUtils.isSignFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.PARITY, FlagsUtils.isParityFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.CARRY, switch (destination.getSize()) {
				case BYTE -> (destinationValue & 0xFFL) < (sourceValue & 0xFFL) ||
					(ctx.registers().readFlag(Registers.Flag.CARRY) && (destinationValue & 0xFFL) == (sourceValue & 0xFFL));
				case WORD -> (destinationValue & 0xFFFFL) < (sourceValue & 0xFFFFL) ||
					(ctx.registers().readFlag(Registers.Flag.CARRY) && (destinationValue & 0xFFFFL) == (sourceValue & 0xFFFFL));
				case DWORD -> (destinationValue & 0xFFFFFFFFL) < (sourceValue & 0xFFFFFFFFL) ||
					(ctx.registers().readFlag(Registers.Flag.CARRY) && (destinationValue & 0xFFFFFFFFL) == (sourceValue & 0xFFFFFFFFL));
				case QWORD -> Long.compareUnsigned(destinationValue, sourceValue) < 0 ||
					(ctx.registers().readFlag(Registers.Flag.CARRY) && destinationValue == sourceValue);
			});
		ctx.registers()
			.writeFlag(Registers.Flag.OVERFLOW, switch (destination.getSize()) {
				case BYTE -> (destinationValue & 0x80L) != (sourceValue & 0x80L) &&
					(result & 0x80L) != (sourceValue & 0x80L);
				case WORD -> (destinationValue & 0x8000L) != (sourceValue & 0x8000L) &&
					(result & 0x8000L) != (sourceValue & 0x8000L);
				case DWORD -> (destinationValue & 0x80000000L) != (sourceValue & 0x80000000L) &&
					(result & 0x80000000L) != (sourceValue & 0x80000000L);
				case QWORD ->
					(destinationValue & 0x8000000000000000L) != (sourceValue & 0x8000000000000000L) &&
						(result & 0x8000000000000000L) != (sourceValue & 0x8000000000000000L);
			});
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		destination.assemble(buf);
		source.assemble(buf);
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new SbbInst(destination.resolved(labelResolver), source.resolved(labelResolver));
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
	public static SbbInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		return new SbbInst(ValueWrapper.parse(parts[0].trim()), ValueWrapper.parse(parts[1].trim()));
	}
	
	@NotNull
	public static SbbInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination = ValueWrapper.load(ctx);
		ValueWrapper source = ValueWrapper.load(ctx);
		return new SbbInst(destination, source);
	}
	
}
