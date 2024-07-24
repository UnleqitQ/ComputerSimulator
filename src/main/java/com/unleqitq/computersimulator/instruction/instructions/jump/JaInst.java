package com.unleqitq.computersimulator.instruction.instructions.jump;

import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class JaInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JaInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JA);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		boolean zf = ctx.registers().readFlag(Registers.Flag.ZERO);
		boolean cf = ctx.registers().readFlag(Registers.Flag.CARRY);
		if (!zf && cf) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JaInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JaInst(target.resolved(labelResolver));
	}
	
	@Override
	protected int getPayloadLength() {
		return target.getLength();
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return target.toAssembly();
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		target.assemble(buf);
	}
	
	@NotNull
	public static JaInst parse(@NotNull String code) {
		return new JaInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JaInst load(@NotNull InstructionContext ctx) {
		return new JaInst(ValueWrapper.load(ctx));
	}
	
}
