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

public class JgeInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JgeInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JGE);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		boolean zf = ctx.registers().readFlag(Registers.Flag.ZERO);
		boolean sf = ctx.registers().readFlag(Registers.Flag.SIGN);
		if (zf || sf) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JgeInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JgeInst(target.resolved(labelResolver));
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
	public static JgeInst parse(@NotNull String code) {
		return new JgeInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JgeInst load(@NotNull InstructionContext ctx) {
		return new JgeInst(ValueWrapper.load(ctx));
	}
	
}
