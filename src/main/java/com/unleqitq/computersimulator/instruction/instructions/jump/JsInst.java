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

public class JsInst extends Instruction {
	
	@NotNull
	private final ValueWrapper target;
	
	public JsInst(@NotNull ValueWrapper target) {
		super(InstructionDef.JS);
		this.target = target;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		if (ctx.registers().readFlag(Registers.Flag.SIGN)) {
			ctx.jump(target.read(ctx));
		}
	}
	
	@NotNull
	@Override
	public JsInst resolved(@NotNull Function<String, Long> labelResolver) {
		return new JsInst(target.resolved(labelResolver));
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
	public static JsInst parse(@NotNull String code) {
		return new JsInst(ValueWrapper.parse(code));
	}
	
	@NotNull
	public static JsInst load(@NotNull InstructionContext ctx) {
		return new JsInst(ValueWrapper.load(ctx));
	}
	
}
