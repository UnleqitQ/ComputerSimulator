package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PushaInst extends Instruction {
	
	public PushaInst() {
		super(InstructionDef.PUSHA);
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.RAX, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.RCX, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.RDX, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.RBX, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.RSI, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.RDI, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.R8, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.R9, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.R10, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.R11, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.R12, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.R13, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.R14, Registers.RegisterRegion.QWORD));
		ctx.stack().pushQword(ctx.registers().readRegister(Registers.Register.R15, Registers.RegisterRegion.QWORD));
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
	public static PushaInst parse(@NotNull String code) {
		return new PushaInst();
	}
	
	@NotNull
	public static PushaInst load(@NotNull InstructionContext ctx) {
		return new PushaInst();
	}
	
}
