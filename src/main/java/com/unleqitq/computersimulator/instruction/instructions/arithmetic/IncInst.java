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

public class IncInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination;
	
	public IncInst(@NotNull ValueWrapper destination) {
		super(InstructionDef.INC);
		this.destination = destination;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long destinationValue = destination.read(ctx);
		long result = destinationValue + 1;
		destination.write(ctx, result);
		
		ctx.registers()
			.writeFlag(Registers.Flag.ZERO, FlagsUtils.isZeroFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.SIGN, FlagsUtils.isSignFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.PARITY, FlagsUtils.isParityFlag(destination.getSize(), result));
		ctx.registers()
			.writeFlag(Registers.Flag.CARRY, destinationValue == -1);
		ctx.registers()
			.writeFlag(Registers.Flag.OVERFLOW, destinationValue ==
				switch (destination.getSize()) {
					case BYTE -> Byte.MAX_VALUE;
					case WORD -> Short.MAX_VALUE;
					case DWORD -> Integer.MAX_VALUE;
					case QWORD -> Long.MAX_VALUE;
				});
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		destination.assemble(buf);
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new IncInst(destination.resolved(labelResolver));
	}
	
	@Override
	protected int getPayloadLength() {
		return destination.getLength();
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return destination.toAssembly();
	}
	
	@NotNull
	public static IncInst parse(@NotNull String code) {
		return new IncInst(ValueWrapper.parse(code.trim()));
	}
	
	@NotNull
	public static IncInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination = ValueWrapper.load(ctx);
		return new IncInst(destination);
	}
	
}
