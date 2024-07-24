package com.unleqitq.computersimulator.instruction.instructions;

import com.unleqitq.computersimulator.components.DeviceAccessor;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import com.unleqitq.computersimulator.instruction.ValueWrapper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class OutInst extends Instruction {
	
	@NotNull
	private final ValueWrapper port;
	@NotNull
	private final ValueWrapper address;
	@NotNull
	private final ValueWrapper source;
	
	public OutInst(@NotNull ValueWrapper port, @NotNull ValueWrapper address,
		@NotNull ValueWrapper source) {
		super(InstructionDef.OUT);
		this.port = port;
		this.address = address;
		this.source = source;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long portValue = port.read(ctx);
		long addressValue = address.read(ctx);
		long sourceValue = source.read(ctx);
		
		DeviceAccessor.IDevice device = ctx.devices().getDevice(portValue);
		if (device == null) System.err.println("Invalid device: " + portValue);
		else device.write(addressValue, sourceValue);
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		port.assemble(buf);
		address.assemble(buf);
		source.assemble(buf);
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new OutInst(port.resolved(labelResolver), address.resolved(labelResolver),
			source.resolved(labelResolver));
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return port.toAssembly() + "," + address.toAssembly() + "," + source.toAssembly();
	}
	
	@Override
	protected int getPayloadLength() {
		return port.getLength() + address.getLength() + source.getLength();
	}
	
	@NotNull
	public static OutInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		ValueWrapper port = ValueWrapper.parse(parts[0]);
		ValueWrapper address = ValueWrapper.parse(parts[1]);
		ValueWrapper source = ValueWrapper.parse(parts[2]);
		return new OutInst(port, address, source);
	}
	
	@NotNull
	public static OutInst load(@NotNull InstructionContext ctx) {
		ValueWrapper port = ValueWrapper.load(ctx);
		ValueWrapper address = ValueWrapper.load(ctx);
		ValueWrapper source = ValueWrapper.load(ctx);
		return new OutInst(port, address, source);
	}
	
}
