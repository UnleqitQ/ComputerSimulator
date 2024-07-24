package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.ValueSize;
import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PopfInst extends Instruction {
	
	public PopfInst() {
		super(InstructionDef.POPF);
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long flagsReg = ctx.stack().pop(ValueSize.WORD);
		ctx.registers().writeRegister(Registers.Register.FLAGS, Registers.RegisterRegion.WORD, flagsReg);
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
	public static PopfInst parse(@NotNull String code) {
		return new PopfInst();
	}
	
	@NotNull
	public static PopfInst load(@NotNull InstructionContext ctx) {
		return new PopfInst();
	}
	
}
