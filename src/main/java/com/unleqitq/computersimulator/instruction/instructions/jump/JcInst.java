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

public class JcInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JcInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JC);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		if (ctx.registers().readFlag(Registers.Flag.CARRY)) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JcInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JcInst(target.resolved(labelResolver));
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
	public static JcInst parse(@NotNull String code) {
		return new JcInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JcInst load(@NotNull InstructionContext ctx) {
		return new JcInst(ValueWrapper.load(ctx));
	}
	
}
