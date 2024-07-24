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

public class JnoInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JnoInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JNO);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		if (!ctx.registers().readFlag(Registers.Flag.OVERFLOW)) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JnoInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JnoInst(target.resolved(labelResolver));
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
	public static JnoInst parse(@NotNull String code) {
		return new JnoInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JnoInst load(@NotNull InstructionContext ctx) {
		return new JnoInst(ValueWrapper.load(ctx));
	}
	
}
