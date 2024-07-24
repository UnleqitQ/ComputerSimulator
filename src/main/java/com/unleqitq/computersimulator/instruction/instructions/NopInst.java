package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NopInst extends Instruction {
	
	public NopInst() {
		super(InstructionDef.NOP);
	}
	
	@Override
	public void execute(@NotNull InstructionContext context) {
		// Do nothing
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		// Do nothing
	}
	
	@Override
	protected int getPayloadLength() {
		return 0;
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return null;
	}
	
	@NotNull
	public static NopInst parse(@NotNull String code) {
		return new NopInst();
	}
	
	@NotNull
	public static NopInst load(@NotNull InstructionContext context) {
		return new NopInst();
	}
	
}
