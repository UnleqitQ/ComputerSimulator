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

public class NotInst extends Instruction {
	
	@NotNull
	private final ValueWrapper destination;
	
	public NotInst(@NotNull ValueWrapper destination) {
		super(InstructionDef.NOT);
		this.destination = destination;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long destinationValue = destination.read(ctx);
		long result = ~destinationValue;
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
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new NotInst(destination.resolved(labelResolver));
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
	public static NotInst parse(@NotNull String code) {
		return new NotInst(ValueWrapper.parse(code.trim()));
	}
	
	@NotNull
	public static NotInst load(@NotNull InstructionContext ctx) {
		ValueWrapper destination = ValueWrapper.load(ctx);
		return new NotInst(destination);
	}
	
}
