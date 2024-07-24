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

public class JpInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JpInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JP);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		if (ctx.registers().readFlag(Registers.Flag.PARITY)) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JpInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JpInst(target.resolved(labelResolver));
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
	public static JpInst parse(@NotNull String code) {
		return new JpInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JpInst load(@NotNull InstructionContext ctx) {
		return new JpInst(ValueWrapper.load(ctx));
	}
	
}
