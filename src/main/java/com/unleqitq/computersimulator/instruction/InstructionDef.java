package com.unleqitq.computersimulator.instruction;

import com.unleqitq.computersimulator.instruction.instructions.*;
import com.unleqitq.computersimulator.instruction.instructions.arithmetic.*;
import com.unleqitq.computersimulator.instruction.instructions.jump.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Getter
public enum InstructionDef {
	
	// No operation
	NOP(0x00, "NOP", NopInst.class),
	// Interrupt
	INT(0x01, "INT", IntInst.class),
	
	// Data movement
	MOV(0x02, "MOV", MovInst.class),
	XCHG(0x03, "XCHG", XchgInst.class),
	LEA(0x04, "LEA", LeaInst.class),
	
	// Arithmetic operations
	INC(0x10, "INC", IncInst.class),
	DEC(0x11, "DEC", DecInst.class),
	ADD(0x12, "ADD", AddInst.class),
	ADC(0x13, "ADC", AdcInst.class),
	SUB(0x14, "SUB", SubInst.class),
	SBB(0x15, "SBB", SbbInst.class),
	NEG(0x16, "NEG", NegInst.class),
	MUL(0x18, "MUL", MulInst.class),
	IMUL(0x19, "IMUL", IMulInst.class),
	DIV(0x1A, "DIV", DivInst.class),
	IDIV(0x1B, "IDIV", IDivInst.class),
	MOD(0x1C, "MOD", ModInst.class),
	IMOD(0x1D, "IMOD", IModInst.class),
	
	// Bitwise operations
	AND(0x20, "AND", AndInst.class),
	OR(0x21, "OR", OrInst.class),
	XOR(0x22, "XOR", XorInst.class),
	NOT(0x23, "NOT", NotInst.class),
	
	// Shifts (without carry)
	SHL(0x28, "SHL", ShlInst.class),
	SHR(0x29, "SHR", ShrInst.class),
	// Shifts (with carry)
	SAL(0x2A, "SAL", SalInst.class),
	SAR(0x2B, "SAR", SarInst.class),
	// Rotates (without carry)
	ROL(0x2C, "ROL", RolInst.class),
	ROR(0x2D, "ROR", RorInst.class),
	// Rotates (with carry)
	RCL(0x2E, "RCL", RclInst.class),
	RCR(0x2F, "RCR", RcrInst.class),
	
	// Test and compare
	TEST(0x50, "TEST", TestInst.class),
	CMP(0x51, "CMP", CmpInst.class),
	
	// Unconditional jump
	JMP(0x60, "JMP", JmpInst.class),
	// Conditional jumps (flags)
	JC(0x64, "JC", JcInst.class),
	JNC(0x65, "JNC", JncInst.class),
	JP(0x66, "JP", JpInst.class),
	JNP(0x67, "JNP", JnpInst.class),
	JZ(0x68, "JZ", JzInst.class),
	JNZ(0x69, "JNZ", JnzInst.class),
	JS(0x6A, "JS", JsInst.class),
	JNS(0x6B, "JNS", JnsInst.class),
	JO(0x6C, "JO", JoInst.class),
	JNO(0x6D, "JNO", JnoInst.class),
	
	// Signed comparison
	JL(0x70, "JL", JlInst.class),
	JLE(0x71, "JLE", JleInst.class),
	JG(0x72, "JG", JgInst.class),
	JGE(0x73, "JGE", JgeInst.class),
	// Unsigned comparison
	JB(0x78, "JB", JbInst.class),
	JBE(0x79, "JBE", JbeInst.class),
	JA(0x7A, "JA", JaInst.class),
	JAE(0x7B, "JAE", JaeInst.class),
	
	// Call and return
	CALL(0x80, "CALL", CallInst.class),
	RET(0x81, "RET", RetInst.class),
	
	// Stack operations
	PUSH(0x90, "PUSH", PushInst.class),
	POP(0x91, "POP", PopInst.class),
	PUSHF(0x92, "PUSHF", PushfInst.class),
	POPF(0x93, "POPF", PopfInst.class),
	PUSHA(0x94, "PUSHA", PushaInst.class),
	POPA(0x95, "POPA", PopaInst.class),
	
	// I/O operations
	IN(0xA0, "IN", InInst.class),
	OUT(0xA1, "OUT", OutInst.class),
	;
	
	private final int opcode;
	@NotNull
	private final String name;
	@NotNull
	private final Class<? extends Instruction> instructionClass;
	
	@NotNull
	private final InstructionLoader loader;
	@NotNull
	private final InstructionParser parser;
	
	@NotNull
	private final static Map<String, InstructionDef> NAME_MAP = new HashMap<>();
	@NotNull
	private final static Map<Integer, InstructionDef> OPCODE_MAP = new HashMap<>();
	
	static {
		for (InstructionDef def : values()) {
			NAME_MAP.put(def.getName().toUpperCase(), def);
			OPCODE_MAP.put(def.getOpcode(), def);
		}
	}
	
	InstructionDef(int opcode, @NotNull String name,
		@NotNull Class<? extends Instruction> instructionClass, @NotNull InstructionLoader loader,
		@NotNull InstructionParser parser) {
		this.opcode = opcode;
		this.name = name;
		this.instructionClass = instructionClass;
		this.loader = loader;
		this.parser = parser;
	}
	
	InstructionDef(int opcode, @NotNull String name,
		@NotNull Class<? extends Instruction> instructionClass, @NotNull InstructionLoader loader) {
		this(opcode, name, instructionClass, loader, defaultParser(instructionClass));
	}
	
	InstructionDef(int opcode, @NotNull String name,
		@NotNull Class<? extends Instruction> instructionClass, @NotNull InstructionParser parser) {
		this(opcode, name, instructionClass, defaultLoader(instructionClass), parser);
	}
	
	InstructionDef(int opcode, @NotNull String name,
		@NotNull Class<? extends Instruction> instructionClass) {
		this(opcode, name, instructionClass, defaultLoader(instructionClass),
			defaultParser(instructionClass));
	}
	
	@NotNull
	public Instruction load(@NotNull InstructionContext context) {
		return loader.load(context);
	}
	
	@NotNull
	public Instruction parse(@NotNull String code) {
		return parser.parse(code);
	}
	
	@Nullable
	public static InstructionDef byName(@NotNull String name) {
		return NAME_MAP.get(name.toUpperCase());
	}
	
	@Nullable
	public static InstructionDef byOpcode(byte opcode) {
		return byOpcode(Byte.toUnsignedInt(opcode));
	}
	
	@Nullable
	public static InstructionDef byOpcode(int opcode) {
		return OPCODE_MAP.get(opcode);
	}
	
	@NotNull
	private static InstructionLoader defaultLoader(
		@NotNull Class<? extends Instruction> instructionClass) {
		try {
			Method method = instructionClass.getDeclaredMethod("load", InstructionContext.class);
			method.setAccessible(true);
			return context -> {
				try {
					return (Instruction) method.invoke(null, context);
				}
				catch (Throwable e) {
					throw new RuntimeException(e);
				}
			};
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
	
	@NotNull
	private static InstructionParser defaultParser(
		@NotNull Class<? extends Instruction> instructionClass) {
		try {
			Method method = instructionClass.getDeclaredMethod("parse", String.class);
			method.setAccessible(true);
			return code -> {
				try {
					return (Instruction) method.invoke(null, code);
				}
				catch (Throwable e) {
					throw new RuntimeException(e);
				}
			};
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
	
	public interface InstructionLoader {
		
		@NotNull
		Instruction load(@NotNull InstructionContext context);
		
	}
	
	public interface InstructionParser {
		
		@NotNull
		Instruction parse(@NotNull String code);
		
	}
	
}
