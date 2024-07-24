package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.utils.NumberUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RetInst extends Instruction {
	
	/**
	 * How many bytes to pop from the stack
	 */
	private final long bytes;
	
	public RetInst(long bytes) {
		super(InstructionDef.RET);
		this.bytes = bytes;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long basePointer = ctx.stack().popQword();
		long returnAddress = ctx.stack().popQword();
		ctx.stack().setBasePointer(basePointer);
		ctx.jump(returnAddress);
		
		ctx.stack().dropBytes(bytes);
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		buf.writeShortLE((int) (bytes & 0xFFFF));
	}
	
	@Override
	protected int getPayloadLength() {
		return 2;
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return "0x" + Long.toUnsignedString(bytes, 16);
	}
	
	@NotNull
	public static RetInst parse(@NotNull String code) {
		if (code.trim().isEmpty()) {
			return new RetInst(0);
		}
		return new RetInst(NumberUtils.parseNumber(code.trim()));
	}
	
	@NotNull
	public static RetInst load(@NotNull InstructionContext context) {
		return new RetInst(context.readInstructionWord());
	}
	
}
