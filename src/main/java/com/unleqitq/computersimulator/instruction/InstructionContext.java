package com.unleqitq.computersimulator.instruction;

import com.unleqitq.computersimulator.ComputerSimulator;
import com.unleqitq.computersimulator.components.DeviceAccessor;
import com.unleqitq.computersimulator.components.Memory;
import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.components.StackWrapper;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Accessors (fluent = true)
@Getter
public class InstructionContext {
	
	@NotNull
	private final ComputerSimulator computer;
	
	private int instructionSize;
	
	private final long instructionPointer;
	private final long codeSegment;
	private final byte opcode;
	
	private boolean jump;
	private long jumpTarget;
	private boolean jumpSegment;
	private long jumpSegmentTarget;
	
	public InstructionContext(@NotNull ComputerSimulator computer) {
		this.computer = computer;
		this.instructionPointer = computer.registers().readRegister(Registers.Register.RIP);
		this.codeSegment = computer.registers().readRegister(Registers.Register.CS);
		this.instructionSize = 1;
		this.opcode = (byte) (computer.memory().readByte(instructionPointer, codeSegment) & 0xFF);
		this.jump = false;
		this.jumpTarget = 0;
		this.jumpSegment = false;
		this.jumpSegmentTarget = 0;
	}
	
	public InstructionContext(@NotNull ComputerSimulator computer, long instructionPointer, long codeSegment) {
		this.computer = computer;
		this.instructionPointer = instructionPointer;
		this.codeSegment = codeSegment;
		this.instructionSize = 1;
		this.opcode = (byte) (computer.memory().readByte(instructionPointer, codeSegment) & 0xFF);
		this.jump = false;
		this.jumpTarget = 0;
		this.jumpSegment = false;
		this.jumpSegmentTarget = 0;
	}
	
	public long readInstructionByte() {
		long value = computer.memory().readByte(instructionPointer + instructionSize, codeSegment);
		instructionSize++;
		return value;
	}
	
	public long readInstructionWord() {
		long value = computer.memory().readWord(instructionPointer + instructionSize, codeSegment);
		instructionSize += 2;
		return value;
	}
	
	public long readInstructionDword() {
		long value = computer.memory().readDword(instructionPointer + instructionSize, codeSegment);
		instructionSize += 4;
		return value;
	}
	
	public long readInstructionQword() {
		long value = computer.memory().readQword(instructionPointer + instructionSize, codeSegment);
		instructionSize += 8;
		return value;
	}
	
	public void jump(long target) {
		jump = true;
		jumpTarget = target;
	}
	
	public void jump(long target, long segment) {
		jump = true;
		jumpTarget = target;
		jumpSegment = true;
		jumpSegmentTarget = segment;
	}
	
	public void resetJump() {
		jump = false;
		jumpTarget = 0;
		jumpSegment = false;
		jumpSegmentTarget = 0;
	}
	
	@NotNull
	public Memory memory() {
		return computer.memory();
	}
	
	@NotNull
	public Registers registers() {
		return computer.registers();
	}
	
	@NotNull
	public StackWrapper stack() {
		return computer.stack();
	}
	
	@NotNull
	public DeviceAccessor devices() {
		return computer.devices();
	}
	
}
