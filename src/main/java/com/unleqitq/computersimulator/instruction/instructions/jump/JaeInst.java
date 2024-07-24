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

public class JaeInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JaeInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JAE);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		boolean zf = ctx.registers().readFlag(Registers.Flag.ZERO);
		boolean cf = ctx.registers().readFlag(Registers.Flag.CARRY);
		if (zf || cf) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JaeInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JaeInst(target.resolved(labelResolver));
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
	public static JaeInst parse(@NotNull String code) {
		return new JaeInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JaeInst load(@NotNull InstructionContext ctx) {
		return new JaeInst(ValueWrapper.load(ctx));
	}
	
}
