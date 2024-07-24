package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.ValueSize;
import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PushfInst extends Instruction {
	
	public PushfInst() {
		super(InstructionDef.PUSHF);
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long flagsReg = ctx.registers().readRegister(Registers.Register.FLAGS, Registers.RegisterRegion.WORD);
		ctx.stack().push(flagsReg, ValueSize.WORD);
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return null;
	}
	
	@Override
	protected int getPayloadLength() {
		return 0;
	}
	
	@NotNull
	public static PushfInst parse(@NotNull String code) {
		return new PushfInst();
	}
	
	@NotNull
	public static PushfInst load(@NotNull InstructionContext ctx) {
		return new PushfInst();
	}
	
}
