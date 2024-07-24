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

public class JnsInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JnsInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JNS);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		if (!ctx.registers().readFlag(Registers.Flag.SIGN)) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JnsInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JnsInst(target.resolved(labelResolver));
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
	public static JnsInst parse(@NotNull String code) {
		return new JnsInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JnsInst load(@NotNull InstructionContext ctx) {
		return new JnsInst(ValueWrapper.load(ctx));
	}
	
}
