package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PopaInst extends Instruction {
	
	public PopaInst() {
		super(InstructionDef.POPA);
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		ctx.registers().writeRegister(Registers.Register.R15, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.R14, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.R13, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.R12, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.R11, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.R10, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.R9, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.R8, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.RDI, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.RSI, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.RBX, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.RDX, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.RCX, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
		ctx.registers().writeRegister(Registers.Register.RAX, Registers.RegisterRegion.QWORD, ctx.stack().popQword());
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
	public static PopaInst parse(@NotNull String code) {
		return new PopaInst();
	}
	
	@NotNull
	public static PopaInst load(@NotNull InstructionContext ctx) {
		return new PopaInst();
	}
	
}
