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

public class AddInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination;
	@NotNull
	private final ValueWrapper source;
	
	public AddInst(@NotNull ValueWrapper destination, @NotNull ValueWrapper source) {
		super(InstructionDef.ADD);
		this.destination = destination;
		this.source = source;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long sourceValue = source.read(ctx);
		long destinationValue = destination.read(ctx);
		long result = destinationValue + sourceValue;
		destination.write(ctx, result);
		
		ctx.registers()
			.writeFlag(Registers.Flag.ZERO, FlagsUtils.isZeroFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.SIGN, FlagsUtils.isSignFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.PARITY, FlagsUtils.isParityFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.CARRY, switch (destination.getSize()) {
				case BYTE -> (result & 0xFFL) < (destinationValue & 0xFFL);
				case WORD -> (result & 0xFFFFL) < (destinationValue & 0xFFFFL);
				case DWORD -> (result & 0xFFFFFFFFL) < (destinationValue & 0xFFFFFFFFL);
				case QWORD -> Long.compareUnsigned(result, destinationValue) < 0;
			});
		ctx.registers()
			.writeFlag(Registers.Flag.OVERFLOW, switch (destination.getSize()) {
				case BYTE -> (result & 0x80L) != (sourceValue & 0x80L) &&
					(result & 0x80L) != (destinationValue & 0x80L);
				case WORD -> (result & 0x8000L) != (sourceValue & 0x8000L) &&
					(result & 0x8000L) != (destinationValue & 0x8000L);
				case DWORD -> (result & 0x80000000L) != (sourceValue & 0x80000000L) &&
					(result & 0x80000000L) != (destinationValue & 0x80000000L);
				case QWORD ->
					(result & 0x8000000000000000L) != (sourceValue & 0x8000000000000000L) &&
						(result & 0x8000000000000000L) != (destinationValue & 0x8000000000000000L);
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
		return new AddInst(destination.resolved(labelResolver), source.resolved(labelResolver));
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
	public static AddInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		return new AddInst(ValueWrapper.parse(parts[0].trim()), ValueWrapper.parse(parts[1].trim()));
	}
	
	@NotNull
	public static AddInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination = ValueWrapper.load(ctx);
		ValueWrapper source = ValueWrapper.load(ctx);
		return new AddInst(destination, source);
	}
	
}
