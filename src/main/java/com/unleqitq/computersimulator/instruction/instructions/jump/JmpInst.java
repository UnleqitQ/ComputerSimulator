package com.unleqitq.computersimulator.instruction.instructions.jump;

import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class JmpInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JmpInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JMP);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		ctx.jump(target.read(ctx));
	}
	
	@Override
	protected int getPayloadLength() {
		return target.getLength();
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		target.assemble(buf);
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return target.toAssembly();
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new JmpInst(target.resolved(labelResolver));
	}
	
	@NotNull
	public static JmpInst parse(@NotNull String code) {
		return new JmpInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JmpInst load(@NotNull InstructionContext ctx) {
		return new JmpInst(ValueWrapper.load(ctx));
	}
	
}
