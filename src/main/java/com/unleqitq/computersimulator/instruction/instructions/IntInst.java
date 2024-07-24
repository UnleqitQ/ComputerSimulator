package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.utils.NumberUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntInst extends Instruction {
	
	private final long code;
	
	public IntInst(long code) {
		super(InstructionDef.INT);
		this.code = code;
	}
	
	@Override
	public void execute(@NotNull InstructionContext context) {
		context.computer().interrupted((byte) (code & 0xFFL));
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		buf.writeByte((byte) (code & 0xFF));
	}
	
	@Override
	protected int getPayloadLength() {
		return 1;
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return "0x" + Long.toUnsignedString(code, 16);
	}
	
	@NotNull
	public static IntInst parse(@NotNull String code) {
		return new IntInst(NumberUtils.parseNumber(code.trim()));
	}
	
	@NotNull
	public static IntInst load(@NotNull InstructionContext context) {
		return new IntInst(context.readInstructionByte());
	}
	
}
