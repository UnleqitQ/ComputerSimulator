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

public class JleInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JleInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JLE);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		boolean zf = ctx.registers().readFlag(Registers.Flag.ZERO);
		boolean sf = ctx.registers().readFlag(Registers.Flag.SIGN);
		if (!sf || zf) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JleInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JleInst(target.resolved(labelResolver));
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
	public static JleInst parse(@NotNull String code) {
		return new JleInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JleInst load(@NotNull InstructionContext ctx) {
		return new JleInst(ValueWrapper.load(ctx));
	}
	
}
