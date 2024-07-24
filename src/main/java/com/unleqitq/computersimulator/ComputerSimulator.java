package com.unleqitq.computersimulator;

import com.unleqitq.computersimulator.components.DeviceAccessor;
import com.unleqitq.computersimulator.components.Memory;
import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.components.StackWrapper;
import com.unleqitq.computersimulator.devices.SystemDevice;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.instruction.InstructionAssembler;
import com.unleqitq.computersimulator.instruction.InstructionContext;
import com.unleqitq.computersimulator.instruction.InstructionDef;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Accessors (fluent = true)
@Getter
public class ComputerSimulator {
	
	public static final Set<File> DEFAULT_INCLUDE_PATHS = new HashSet<>();
	
	static {
		File currentDir = new File("./").getAbsoluteFile();
		DEFAULT_INCLUDE_PATHS.add(currentDir);
		File resourcesDir = new File(currentDir, "src/main/resources");
		if (resourcesDir.exists()) {
			DEFAULT_INCLUDE_PATHS.add(resourcesDir);
		}
	}
	
	@Setter
	private boolean debugPrint = false;
	
	@NotNull
	private final List<InterruptionListener> interruptionListeners = new LinkedList<>();
	
	@NotNull
	private final Memory memory;
	
	@NotNull
	private final Registers registers;
	
	@NotNull
	private final StackWrapper stack;
	
	@NotNull
	private final DeviceAccessor devices;
	
	private boolean interrupted = false;
	private byte interruptCode = 0;
	
	private long stepCount = 0;
	
	public ComputerSimulator(@NotNull ComputerSpecs specs) {
		this.memory = new Memory(specs.memorySize());
		this.registers = new Registers();
		this.stack = new StackWrapper(memory, registers);
		this.devices = new DeviceAccessor();
		devices.addDevice(SystemDevice.DEFAULT_PORT, new SystemDevice(this));
	}
	
	/**
	 * The interruption listeners are called when an interruption is triggered.<br>
	 * The listeners are called in the order they were added until one of them handles the interruption
	 */
	public void addInterruptionListener(@NotNull InterruptionListener listener) {
		interruptionListeners.add(listener);
	}
	
	public void removeInterruptionListener(@NotNull InterruptionListener listener) {
		interruptionListeners.remove(listener);
	}
	
	public void interrupted(byte code) {
		if (code != 0) {
			for (InterruptionListener listener : interruptionListeners) {
				if (listener.onInterrupt(this, code)) {
					return;
				}
			}
		}
		this.interrupted = true;
		this.interruptCode = code;
	}
	
	public boolean isExiting() {
		return interrupted && interruptCode == 0;
	}
	
	public void resetInterrupt() {
		interrupted = false;
		interruptCode = 0;
	}
	
	public void step() {
		InstructionContext ctx;
		Instruction instruction;
		try {
			ctx = new InstructionContext(this);
			InstructionDef def = InstructionDef.byOpcode(ctx.opcode());
			if (def == null) {
				System.err.println("Invalid opcode: " + ctx.opcode());
				registers.writeRegister(Registers.Register.RIP,
					registers.readRegister(Registers.Register.RIP) + ctx.instructionSize());
				return;
			}
			instruction = def.load(ctx);
		}
		catch (Exception e) {
			System.err.println("Error loading instruction: " + e.getMessage());
			registers.writeRegister(Registers.Register.RIP,
				registers.readRegister(Registers.Register.RIP) + 1);
			return;
		}
		if (debugPrint) {
			System.out.println(instruction.toAssembly());
		}
		
		// Instruction pointer is incremented before executing the instruction
		registers.writeRegister(Registers.Register.RIP,
			registers.readRegister(Registers.Register.RIP) + ctx.instructionSize());
		
		try {
			instruction.execute(ctx);
			stepCount++;
			if (debugPrint) {
				System.out.println();
			}
		}
		catch (Exception e) {
			System.err.println("Error executing instruction: " + e.getMessage());
		}
		
		if (ctx.jump()) {
			registers.writeRegister(Registers.Register.RIP, ctx.jumpTarget());
			if (ctx.jumpSegment()) {
				registers.writeRegister(Registers.Register.CS, ctx.jumpSegmentTarget());
			}
		}
	}
	
	@Nullable
	public Instruction getInstruction(long address) {
		InstructionContext ctx =
			new InstructionContext(this, address, registers.readRegister(Registers.Register.CS));
		InstructionDef def = InstructionDef.byOpcode(ctx.opcode());
		if (def == null) {
			return null;
		}
		return def.load(ctx);
	}
	
	public void initialize() {
		for (Registers.Register reg : Registers.Register.values()) {
			registers.writeRegister(reg, 0);
		}
		registers.writeRegister(Registers.Register.RIP, memory.getSize() / 3);
		registers.writeRegister(Registers.Register.DS, 0);
		registers.writeRegister(Registers.Register.CS, 0);
		registers.writeRegister(Registers.Register.ES, 0);
		registers.writeRegister(Registers.Register.SS, 0);
		registers.writeRegister(Registers.Register.RSP, memory.getSize() - 1);
		registers.writeRegister(Registers.Register.RBP, memory.getSize() - 1);
		registers.writeRegister(Registers.Register.FLAGS, 0);
		resetInterrupt();
		
		resetStepCount();
		stack.resetHistory();
	}
	
	public void loadProgram(long address, @NotNull byte[] program) {
		long segment = registers.readRegister(Registers.Register.CS);
		memory.write(address, segment, program);
	}
	
	public void setInstructionPointer(long address) {
		registers.writeRegister(Registers.Register.RIP, address);
	}
	
	public long getInstructionPointer() {
		return registers.readRegister(Registers.Register.RIP);
	}
	
	public void resetStepCount() {
		stepCount = 0;
	}
	
	public static void main(String[] args) {
		ComputerSimulator simulator =
			new ComputerSimulator(ComputerSpecs.builder().memorySize(1 << 12).build()).debugPrint(true);
		simulator.initialize();
		
		String code = loadCode("/code.qasm");
		long address = simulator.getInstructionPointer();
		byte[] program = InstructionAssembler.assemble(code, address, DEFAULT_INCLUDE_PATHS);
		
		simulator.loadProgram(address, program);
		while (!simulator.isExiting()) {
			simulator.step();
		}
	}
	
	@NotNull
	private static String loadCode(@NotNull String path) {
		try (InputStream stream = ComputerSimulator.class.getResourceAsStream(path)) {
			if (stream == null) {
				throw new RuntimeException("Resource not found: " + path);
			}
			return new String(stream.readAllBytes());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public interface InterruptionListener {
		
		/**
		 * Called when an interruption is triggered.<br>
		 * Interrupts with code 0 are used to exit the program and won't be passed to listeners
		 *
		 * @param computer the computer that triggered the interruption
		 * @param code     the interruption code
		 * @return true if the interruption was handled, false otherwise<br>
		 * (if false, the interruption will be passed to the next listener)
		 */
		boolean onInterrupt(@NotNull ComputerSimulator computer, byte code);
		
	}
	
}
