package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.ValueSize;
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

public class TestInst extends Instruction {
	
	@NotNull
	private final ValueWrapper source1;
	@NotNull
	private final ValueWrapper source2;
	
	public TestInst(@NotNull ValueWrapper source1, @NotNull ValueWrapper source2) {
		super(InstructionDef.TEST);
		this.source1 = source1;
		this.source2 = source2;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long source1Value = source1.read(ctx);
		long source2Value = source2.read(ctx);
		long result = source1Value & source2Value;
		ValueSize size = ValueSize.max(source1.getSize(), source2.getSize());
		
		ctx.registers().writeFlag(Registers.Flag.ZERO, FlagsUtils.isZeroFlag(size, result));
		ctx.registers().writeFlag(Registers.Flag.SIGN, FlagsUtils.isSignFlag(size, result));
		ctx.registers().writeFlag(Registers.Flag.PARITY, FlagsUtils.isParityFlag(size, result));
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		source1.assemble(buf);
		source2.assemble(buf);
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new TestInst(source1.resolved(labelResolver), source2.resolved(labelResolver));
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return source1.toAssembly() + ", " + source2.toAssembly();
	}
	
	@Override
	protected int getPayloadLength() {
		return source1.getLength() + source2.getLength();
	}
	
	@NotNull
	public static TestInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		return new TestInst(ValueWrapper.parse(parts[0].trim()), ValueWrapper.parse(parts[1].trim()));
	}
	
	@NotNull
	public static TestInst load(@NotNull InstructionContext ctx) {
		ValueWrapper source1 = ValueWrapper.load(ctx);
		ValueWrapper source2 = ValueWrapper.load(ctx);
		return new TestInst(source1, source2);
	}
	
}
