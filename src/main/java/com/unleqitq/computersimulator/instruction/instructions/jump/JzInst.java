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

public class JzInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JzInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JZ);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		if (ctx.registers().readFlag(Registers.Flag.ZERO)) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JzInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JzInst(target.resolved(labelResolver));
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
	public static JzInst parse(@NotNull String code) {
		return new JzInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JzInst load(@NotNull InstructionContext ctx) {
		return new JzInst(ValueWrapper.load(ctx));
	}
	
}
