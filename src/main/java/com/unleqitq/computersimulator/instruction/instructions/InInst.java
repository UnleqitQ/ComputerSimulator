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

public class InInst extends Instruction {
	
	@NotNull
	private final ValueWrapper port;
	@NotNull
	private final ValueWrapper address;
	@NotNull
	private final ValueWrapper destination;
	
	public InInst(@NotNull ValueWrapper port, @NotNull ValueWrapper address, @NotNull ValueWrapper destination) {
		super(InstructionDef.IN);
		this.port = port;
		this.address = address;
		this.destination = destination;
	}
	
	@Override
	public void execute(@NotNull InstructionContext ctx) {
		long portValue = port.read(ctx);
		long addressValue = address.read(ctx);
		
		DeviceAccessor.IDevice device = ctx.devices().getDevice(portValue);
		if (device == null) {
			System.err.println("Invalid device: " + portValue);
			destination.write(ctx, 0);
		}
		else {
			long value = device.read(addressValue);
			destination.write(ctx, value);
		}
	}
	
	@Override
	protected void assemblePayload(@NotNull ByteBuf buf) {
		port.assemble(buf);
		address.assemble(buf);
		destination.assemble(buf);
	}
	
	@Nullable
	@Override
	protected String getAssemblyPayload() {
		return port.toAssembly() + "," + address.toAssembly() + "," + destination.toAssembly();
	}
	
	@Override
	protected int getPayloadLength() {
		return port.getLength() + address.getLength() + destination.getLength();
	}
	
	@NotNull
	@Override
	public Instruction resolved(@NotNull Function<String, Long> labelResolver) {
		return new InInst(port.resolved(labelResolver), address.resolved(labelResolver), destination.resolved(labelResolver));
	}
	
	@NotNull
	public static InInst parse(@NotNull String code) {
		String[] parts = code.split(",");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		ValueWrapper port = ValueWrapper.parse(parts[0]);
		ValueWrapper address = ValueWrapper.parse(parts[1]);
		ValueWrapper destination = ValueWrapper.parse(parts[2]);
		return new InInst(port, address, destination);
	}
	
	@NotNull
	public static InInst load(@NotNull InstructionContext ctx) {
		ValueWrapper port = ValueWrapper.load(ctx);
		ValueWrapper address = ValueWrapper.load(ctx);
		ValueWrapper destination = ValueWrapper.load(ctx);
		return new InInst(port, address, destination);
	}
	
}
