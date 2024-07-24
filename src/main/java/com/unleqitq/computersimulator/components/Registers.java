package com.unleqitq.computersimulator.components;

import com.unleqitq.computersimulator.ValueSize;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Registers {
	
	private final long[] registers;
	
	public Registers() {
		this.registers = new long[Register.values().length];
	}
	
	public long readRegister(@NotNull Register register) {
		return registers[register.getValue()];
	}
	
	public void writeRegister(@NotNull Register register, long value) {
		registers[register.getValue()] = value;
	}
	
	public long readRegister(@NotNull Register register, @NotNull RegisterRegion region) {
		if (!register.isRegionAllowed(region)) {
			throw new IllegalArgumentException(
				"Register " + register.getName() + " does not support region " + region);
		}
		long value = readRegister(register);
		return switch (region) {
			case LOW_BYTE -> value & 0xFF;
			case HIGH_BYTE -> (value >> 8) & 0xFF;
			case WORD -> value & 0xFFFF;
			case DWORD -> value & 0xFFFFFFFFL;
			case QWORD -> value;
		};
	}
	
	public void writeRegister(@NotNull Register register, @NotNull RegisterRegion region,
		long value) {
		if (!register.isRegionAllowed(region)) {
			throw new IllegalArgumentException(
				"Register " + register.getName() + " does not support region " + region);
		}
		long registerValue = readRegister(register);
		long newValue = switch (region) {
			case LOW_BYTE -> (registerValue & ~0xFFL) | (value & 0xFFL);
			case HIGH_BYTE -> (registerValue & ~0xFF00L) | ((value & 0xFFL) << 8);
			case WORD -> (registerValue & ~0xFFFFL) | (value & 0xFFFFL);
			case DWORD -> (registerValue & ~0xFFFFFFFFL) | (value & 0xFFFFFFFFL);
			case QWORD -> value;
		};
		writeRegister(register, newValue);
	}
	
	public boolean readFlag(@NotNull Flag flag) {
		return flag.isSet(readRegister(Register.FLAGS));
	}
	
	public void writeFlag(@NotNull Flag flag, boolean value) {
		long flags = readRegister(Register.FLAGS);
		writeRegister(Register.FLAGS, flag.set(flags, value));
	}
	
	
	@Getter
	public enum Register {
		
		// General purpose registers
		RAX("rax", 0, RegisterRegion.values()),
		RBX("rbx", 1, RegisterRegion.values()),
		RCX("rcx", 2, RegisterRegion.values()),
		RDX("rdx", 3, RegisterRegion.values()),
		// Index registers
		RSI("rsi", 4, RegisterRegion.WORD, RegisterRegion.DWORD, RegisterRegion.QWORD),
		RDI("rdi", 5, RegisterRegion.WORD, RegisterRegion.DWORD, RegisterRegion.QWORD),
		// Base pointer
		RBP("rbp", 6, RegisterRegion.WORD, RegisterRegion.DWORD, RegisterRegion.QWORD),
		// Stack pointer
		RSP("rsp", 7, RegisterRegion.WORD, RegisterRegion.DWORD, RegisterRegion.QWORD),
		// Additional general purpose registers
		R8("r8", 8, RegisterRegion.QWORD),
		R9("r9", 9, RegisterRegion.QWORD),
		R10("r10", 10, RegisterRegion.QWORD),
		R11("r11", 11, RegisterRegion.QWORD),
		R12("r12", 12, RegisterRegion.QWORD),
		R13("r13", 13, RegisterRegion.QWORD),
		R14("r14", 14, RegisterRegion.QWORD),
		R15("r15", 15, RegisterRegion.QWORD),
		// Instruction pointer
		RIP("rip", 16, RegisterRegion.WORD, RegisterRegion.DWORD, RegisterRegion.QWORD),
		// Flags register
		FLAGS("flags", 17, RegisterRegion.WORD),
		// Segment registers
		/**
		 * Code segment
		 */
		CS("cs", 18, RegisterRegion.WORD),
		/**
		 * Data segment
		 */
		DS("ds", 19, RegisterRegion.WORD),
		/**
		 * Stack segment
		 */
		SS("ss", 20, RegisterRegion.WORD),
		/**
		 * Extra segment
		 */
		ES("es", 21, RegisterRegion.WORD),
		/**
		 * F segment
		 */
		FS("fs", 22, RegisterRegion.WORD),
		/**
		 * G segment
		 */
		GS("gs", 23, RegisterRegion.WORD);
		
		/**
		 * The name of the register
		 */
		@NotNull
		private final String name;
		/**
		 * The value of the register (used in instructions)
		 */
		private final byte value;
		
		private final byte allowedRegions;
		
		@NotNull
		private static final Map<Byte, Register> VALUE_MAP = new HashMap<>();
		
		static {
			for (Register register : values()) {
				VALUE_MAP.put(register.value, register);
			}
		}
		
		Register(@NotNull String name, int value, @NotNull RegisterRegion... allowedRegions) {
			this.name = name;
			this.value = (byte) value;
			byte allowed = 0;
			for (RegisterRegion region : allowedRegions) {
				allowed |= (byte) (1 << region.getValue());
			}
			this.allowedRegions = allowed;
		}
		
		@NotNull
		public static Register fromValue(int value) {
			Register register = VALUE_MAP.get((byte) value);
			if (register == null) {
				throw new IllegalArgumentException("Invalid register value: " + value);
			}
			return register;
		}
		
		public boolean isRegionAllowed(@NotNull RegisterRegion region) {
			return (allowedRegions & (1 << region.getValue())) != 0;
		}
		
	}
	
	@Getter
	public enum RegisterRegion {
		
		LOW_BYTE(0, 1),
		HIGH_BYTE(1, 1),
		WORD(2, 2),
		DWORD(3, 4),
		QWORD(4, 8);
		
		/**
		 * The value of the region
		 */
		private final byte value;
		/**
		 * The size of the region in bytes
		 */
		private final byte size;
		
		RegisterRegion(int value, int size) {
			this.value = (byte) value;
			this.size = (byte) size;
		}
		
		@NotNull
		public static RegisterRegion fromValue(int value) {
			return switch (value) {
				case 0 -> LOW_BYTE;
				case 1 -> HIGH_BYTE;
				case 2 -> WORD;
				case 3 -> DWORD;
				case 4 -> QWORD;
				default -> throw new IllegalArgumentException("Invalid register region value: " + value);
			};
		}
		
		@NotNull
		public ValueSize getValueSize() {
			return switch (this) {
				case LOW_BYTE, HIGH_BYTE -> ValueSize.BYTE;
				case WORD -> ValueSize.WORD;
				case DWORD -> ValueSize.DWORD;
				case QWORD -> ValueSize.QWORD;
			};
		}
		
	}
	
	public record RegisterValue(Register register, RegisterRegion region) {
		
		@NotNull
		public static RegisterValue fromValue(byte value) {
			return new RegisterValue(Register.fromValue(value >> 3),
				RegisterRegion.fromValue(value & 0b111));
		}
		
		public byte toValue() {
			return (byte) ((register.getValue() << 3) | region.getValue());
		}
		
		@NotNull
		public static RegisterValue fromName(@NotNull String name) {
			return switch (name) {
				// General purpose registers
				case "al" -> new RegisterValue(Register.RAX, RegisterRegion.LOW_BYTE);
				case "ah" -> new RegisterValue(Register.RAX, RegisterRegion.HIGH_BYTE);
				case "ax" -> new RegisterValue(Register.RAX, RegisterRegion.WORD);
				case "eax" -> new RegisterValue(Register.RAX, RegisterRegion.DWORD);
				case "rax" -> new RegisterValue(Register.RAX, RegisterRegion.QWORD);
				case "bl" -> new RegisterValue(Register.RBX, RegisterRegion.LOW_BYTE);
				case "bh" -> new RegisterValue(Register.RBX, RegisterRegion.HIGH_BYTE);
				case "bx" -> new RegisterValue(Register.RBX, RegisterRegion.WORD);
				case "ebx" -> new RegisterValue(Register.RBX, RegisterRegion.DWORD);
				case "rbx" -> new RegisterValue(Register.RBX, RegisterRegion.QWORD);
				case "cl" -> new RegisterValue(Register.RCX, RegisterRegion.LOW_BYTE);
				case "ch" -> new RegisterValue(Register.RCX, RegisterRegion.HIGH_BYTE);
				case "cx" -> new RegisterValue(Register.RCX, RegisterRegion.WORD);
				case "ecx" -> new RegisterValue(Register.RCX, RegisterRegion.DWORD);
				case "rcx" -> new RegisterValue(Register.RCX, RegisterRegion.QWORD);
				case "dl" -> new RegisterValue(Register.RDX, RegisterRegion.LOW_BYTE);
				case "dh" -> new RegisterValue(Register.RDX, RegisterRegion.HIGH_BYTE);
				case "dx" -> new RegisterValue(Register.RDX, RegisterRegion.WORD);
				case "edx" -> new RegisterValue(Register.RDX, RegisterRegion.DWORD);
				case "rdx" -> new RegisterValue(Register.RDX, RegisterRegion.QWORD);
				
				// Index registers
				case "si" -> new RegisterValue(Register.RSI, RegisterRegion.WORD);
				case "esi" -> new RegisterValue(Register.RSI, RegisterRegion.DWORD);
				case "rsi" -> new RegisterValue(Register.RSI, RegisterRegion.QWORD);
				case "di" -> new RegisterValue(Register.RDI, RegisterRegion.WORD);
				case "edi" -> new RegisterValue(Register.RDI, RegisterRegion.DWORD);
				case "rdi" -> new RegisterValue(Register.RDI, RegisterRegion.QWORD);
				
				// Base pointer
				case "bp" -> new RegisterValue(Register.RBP, RegisterRegion.WORD);
				case "ebp" -> new RegisterValue(Register.RBP, RegisterRegion.DWORD);
				case "rbp" -> new RegisterValue(Register.RBP, RegisterRegion.QWORD);
				
				// Stack pointer
				case "sp" -> new RegisterValue(Register.RSP, RegisterRegion.WORD);
				case "esp" -> new RegisterValue(Register.RSP, RegisterRegion.DWORD);
				case "rsp" -> new RegisterValue(Register.RSP, RegisterRegion.QWORD);
				
				// Instruction pointer
				case "ip" -> new RegisterValue(Register.RIP, RegisterRegion.WORD);
				case "eip" -> new RegisterValue(Register.RIP, RegisterRegion.DWORD);
				case "rip" -> new RegisterValue(Register.RIP, RegisterRegion.QWORD);
				
				// Additional general purpose registers
				case "r8" -> new RegisterValue(Register.R8, RegisterRegion.QWORD);
				case "r9" -> new RegisterValue(Register.R9, RegisterRegion.QWORD);
				case "r10" -> new RegisterValue(Register.R10, RegisterRegion.QWORD);
				case "r11" -> new RegisterValue(Register.R11, RegisterRegion.QWORD);
				case "r12" -> new RegisterValue(Register.R12, RegisterRegion.QWORD);
				case "r13" -> new RegisterValue(Register.R13, RegisterRegion.QWORD);
				case "r14" -> new RegisterValue(Register.R14, RegisterRegion.QWORD);
				case "r15" -> new RegisterValue(Register.R15, RegisterRegion.QWORD);
				
				// Segment registers
				case "cs" -> new RegisterValue(Register.CS, RegisterRegion.WORD);
				case "ds" -> new RegisterValue(Register.DS, RegisterRegion.WORD);
				case "ss" -> new RegisterValue(Register.SS, RegisterRegion.WORD);
				case "es" -> new RegisterValue(Register.ES, RegisterRegion.WORD);
				case "fs" -> new RegisterValue(Register.FS, RegisterRegion.WORD);
				case "gs" -> new RegisterValue(Register.GS, RegisterRegion.WORD);
				
				// Flags register
				case "flags" -> new RegisterValue(Register.FLAGS, RegisterRegion.WORD);
				default -> throw new IllegalArgumentException("Invalid register name: " + name);
			};
		}
		
		@NotNull
		public String getName() {
			return switch (region) {
				case LOW_BYTE -> switch (register) {
					case RAX -> "al";
					case RBX -> "bl";
					case RCX -> "cl";
					case RDX -> "dl";
					default ->
						throw new IllegalArgumentException("Invalid register for low byte: " + register);
				};
				case HIGH_BYTE -> switch (register) {
					case RAX -> "ah";
					case RBX -> "bh";
					case RCX -> "ch";
					case RDX -> "dh";
					default ->
						throw new IllegalArgumentException("Invalid register for high byte: " + register);
				};
				case WORD -> switch (register) {
					case RAX -> "ax";
					case RBX -> "bx";
					case RCX -> "cx";
					case RDX -> "dx";
					case RSI -> "si";
					case RDI -> "di";
					case RBP -> "bp";
					case RSP -> "sp";
					case CS -> "cs";
					case DS -> "ds";
					case SS -> "ss";
					case ES -> "es";
					case FS -> "fs";
					case GS -> "gs";
					case RIP -> "ip";
					default -> throw new IllegalArgumentException("Invalid register for word: " + register);
				};
				case DWORD -> switch (register) {
					case RAX -> "eax";
					case RBX -> "ebx";
					case RCX -> "ecx";
					case RDX -> "edx";
					case RSI -> "esi";
					case RDI -> "edi";
					case RBP -> "ebp";
					case RSP -> "esp";
					case RIP -> "eip";
					default -> throw new IllegalArgumentException("Invalid register for dword: " + register);
				};
				case QWORD -> switch (register) {
					case RAX -> "rax";
					case RBX -> "rbx";
					case RCX -> "rcx";
					case RDX -> "rdx";
					case RSI -> "rsi";
					case RDI -> "rdi";
					case RBP -> "rbp";
					case RSP -> "rsp";
					case R8 -> "r8";
					case R9 -> "r9";
					case R10 -> "r10";
					case R11 -> "r11";
					case R12 -> "r12";
					case R13 -> "r13";
					case R14 -> "r14";
					case R15 -> "r15";
					case RIP -> "rip";
					default -> throw new IllegalArgumentException("Invalid register for qword: " + register);
				};
			};
		}
		
	}
	
	@Getter
	public enum Flag {
		
		CARRY("cf", 0),
		PARITY("pf", 2),
		@Deprecated ADJUST("af", 4),
		ZERO("zf", 6),
		SIGN("sf", 7),
		@Deprecated TRAP("tf", 8),
		@Deprecated INTERRUPT("if", 9),
		@Deprecated DIRECTION("df", 10),
		OVERFLOW("of", 11),
		;
		
		/**
		 * The name of the flag
		 */
		@NotNull
		private final String name;
		
		/**
		 * The value of the flag (used in instructions, and for the position in the flags register)
		 */
		private final byte value;
		
		@NotNull
		private static final Map<String, Flag> NAME_MAP = new HashMap<>();
		@NotNull
		private static final Map<Byte, Flag> VALUE_MAP = new HashMap<>();
		
		static {
			for (Flag flag : values()) {
				NAME_MAP.put(flag.name, flag);
				VALUE_MAP.put(flag.value, flag);
			}
		}
		
		Flag(@NotNull String name, int value) {
			this.name = name;
			this.value = (byte) value;
		}
		
		@NotNull
		public static Flag fromValue(int value) {
			Flag flag = VALUE_MAP.get((byte) value);
			if (flag == null) {
				throw new IllegalArgumentException("Invalid flag value: " + value);
			}
			return flag;
		}
		
		@NotNull
		public static Flag fromName(@NotNull String name) {
			Flag flag = NAME_MAP.get(name.toLowerCase());
			if (flag == null) {
				throw new IllegalArgumentException("Invalid flag name: " + name);
			}
			return flag;
		}
		
		public boolean isSet(long flags) {
			return (flags & (1L << value)) != 0;
		}
		
		public long set(long flags, boolean set) {
			return set ? flags | (1L << value) : flags & ~(1L << value);
		}
		
	}
	
}
