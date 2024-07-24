package com.unleqitq.computersimulator.instruction;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Getter
public abstract class Instruction {
	
	@NotNull
	private final InstructionDef def;
	
	protected Instruction(@NotNull InstructionDef def) {
		this.def = def;
	}
	
	public final int getOpcode() {
		return def.getOpcode();
	}
	
	@NotNull
	public final String getName() {
		return def.getName();
	}
	
	@NotNull
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return this;
	}
	
	public abstract void execute(@NotNull InstructionContext context);
	
	public int getLength() {
		return 1 + getPayloadLength();
	}
	
	protected abstract int getPayloadLength();
	
	public void assemble(@NotNull ByteBuf buf) {
		buf.writeByte(def.getOpcode());
		assemblePayload(buf);
	}
	
	protected abstract void assemblePayload(@NotNull ByteBuf buf);
	
	@NotNull
	public final String toAssembly() {
		try {
			String payload = getAssemblyPayload();
			return payload == null ? def.getName() : def.getName() + " " + payload;
		}
		catch (Exception e) {
			return def.getName() + " ERROR";
		}
	}
	
	@Nullable
	protected abstract String getAssemblyPayload();
	
}
