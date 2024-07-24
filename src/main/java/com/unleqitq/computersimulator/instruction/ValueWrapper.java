package com.unleqitq.computersimulator.instruction;

import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.ValueSize;
import com.unleqitq.computersimulator.utils.NumberUtils;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public abstract class ValueWrapper {
	
	@NotNull
	private final ValueSize size;
	
	@NotNull
	private final Type type;
	
	@Override
	public String toString() {
		return this.type.name().toLowerCase() + " " + this.size.getName();
	}
	
	@NotNull
	public abstract String toAssembly();
	
	protected ValueWrapper(@NotNull ValueSize size, @NotNull Type type) {
		this.size = size;
		this.type = type;
	}
	
	public abstract long read(@NotNull InstructionContext ctx);
	
	public abstract void write(@NotNull InstructionContext ctx, long value);
	
	public final void assemble(@NotNull ByteBuf buf) {
		buf.writeByte(this.type.getValue() << 6 | this.size.getValue());
		this.assemble0(buf);
	}
	
	protected abstract void assemble0(@NotNull ByteBuf buf);
	
	@NotNull
	public ValueWrapper resolved(@NotNull Function<String, Long> labelResolver) {
		return this;
	}
	
	public int getLength() {
		return 1 + this.getLength0();
	}
	
	protected abstract int getLength0();
	
	@NotNull
	public static ValueWrapper load(@NotNull InstructionContext ctx) {
		long data = ctx.readInstructionByte();
		ValueSize size;
		{
			int sizeValue = (int) (data & 0b11);
			size = ValueSize.fromValue(sizeValue);
		}
		Type type;
		{
			int typeValue = (int) (data >> 6);
			type = Type.fromValue(typeValue);
		}
		return switch (type) {
			case IMMEDIATE -> ImmediateValueWrapper.load(ctx, size);
			case REGISTER -> RegisterValueWrapper.load(ctx, size);
			case MEMORY -> MemoryValueWrapper.load(ctx, size);
		};
	}
	
	@NotNull
	public static MemoryValueWrapper loadMemory(@NotNull InstructionContext ctx,
		@NotNull ValueSize size) {
		return MemoryValueWrapper.load(ctx, size);
	}
	
	@NotNull
	public static ValueWrapper parse(@NotNull String text) {
		ValueSize size = null;
		text = text.trim();
		if (text.split(" ", 2)[0].matches("(byte)|(word)|(dword)|(qword)")) {
			size = ValueSize.valueOf(text.split(" ", 2)[0].toUpperCase());
			text = text.split(" ", 2)[1].trim();
		}
		if (size == null) {
			if (text.startsWith("$")) {
				size = ValueSize.QWORD;
				return new ImmediateValueWrapper(size, text.substring(1));
			}
			// Should be a register, as its size is specified in its name
			Registers.RegisterValue reg = Registers.RegisterValue.fromName(text);
			return new RegisterValueWrapper(reg.register(), reg.region());
		}
		if (text.startsWith("$")) {
			return new ImmediateValueWrapper(size, text.substring(1));
		}
		try {
			long value = NumberUtils.parseNumber(text);
			return new ImmediateValueWrapper(size, value);
		}
		catch (NumberFormatException e) {
			// Not a number
		}
		{
			Matcher m1 =
				Pattern.compile("((?<segment>(cs)|(ds)|(es)|(fs)|(gs)|(ss)):)?\\[(?<address>.+)]")
					.matcher(text.replaceAll("\\s", ""));
			if (m1.matches()) {
				MemoryValueWrapper.MemorySegment segment =
					m1.group("segment") == null ? MemoryValueWrapper.MemorySegment.DS :
						MemoryValueWrapper.MemorySegment.valueOf(m1.group("segment").toUpperCase());
				String address = m1.group("address").trim();
				String regPattern = "(al)|(ah)|(ax)|(eax)|(rax)|(bl)|(bh)|(bx)|(ebx)|(rbx)|" +
					"(cl)|(ch)|(cx)|(ecx)|(rcx)|(dl)|(dh)|(dx)|(edx)|(rdx)|" +
					"(si)|(esi)|(rsi)|(di)|(edi)|(rdi)|(bp)|(ebp)|(rbp)|(sp)|(esp)|(rsp)|" +
					"(r[89])|(r1[0-5])|(cs)|(ds)|(es)|(fs)|(gs)|(ss)|(rip)|(eip)|(ip)";
				
				// Direct
				{
					if (address.startsWith("$")) {
						return new MemoryValueWrapper.DirectMemoryValueWrapper(size, segment,
							address.substring(1));
					}
					if (NumberUtils.isNumber(address)) {
						return new MemoryValueWrapper.DirectMemoryValueWrapper(size, segment,
							NumberUtils.parseNumber(address));
					}
				}
				// Indirect
				{
					Matcher m2 = Pattern.compile(regPattern).matcher(address);
					if (m2.matches()) {
						Registers.RegisterValue reg = Registers.RegisterValue.fromName(address);
						return new MemoryValueWrapper.IndirectMemoryValueWrapper(size, segment, reg.register(),
							reg.region());
					}
				}
				// Displacement
				{
					Matcher m2 = Pattern.compile(
						"(?<reg>" + regPattern + ")(?<operator>[+\\-])(?<disp>(" + NumberUtils.NUMBER_PATTERN +
							")|(\\$.+))").matcher(address);
					if (m2.matches()) {
						Registers.RegisterValue reg = Registers.RegisterValue.fromName(m2.group("reg"));
						boolean add = m2.group("operator").equals("+");
						String dispGroup = m2.group("disp");
						if (dispGroup.startsWith("$")) {
							return new MemoryValueWrapper.DisplacementMemoryValueWrapper(size, segment,
								dispGroup.substring(1), reg.register(), reg.region());
						}
						long disp = NumberUtils.parseNumber(dispGroup);
						return new MemoryValueWrapper.DisplacementMemoryValueWrapper(size, segment,
							disp * (add ? 1 : -1), reg.register(), reg.region());
					}
				}
				// Indexed
				{
					Matcher m2 = Pattern.compile("(?<base>" + regPattern + ")\\+(?<index>" + regPattern + ")")
						.matcher(address);
					if (m2.matches()) {
						Registers.RegisterValue base = Registers.RegisterValue.fromName(m2.group("base"));
						Registers.RegisterValue index = Registers.RegisterValue.fromName(m2.group("index"));
						return new MemoryValueWrapper.IndexedMemoryValueWrapper(size, segment, base.register(),
							base.region(), index.register(), index.region());
					}
				}
				// Indexed Displacement
				{
					Matcher m2 = Pattern.compile(
						"(?<base>" + regPattern + ")\\+(?<index>" + regPattern + ")(?<op>[+\\-])(?<disp>(" +
							NumberUtils.NUMBER_PATTERN + ")|(\\$.+))").matcher(address);
					if (m2.matches()) {
						boolean add = m2.group("op").equals("+");
						Registers.RegisterValue base = Registers.RegisterValue.fromName(m2.group("base"));
						Registers.RegisterValue index = Registers.RegisterValue.fromName(m2.group("index"));
						String dispGroup = m2.group("disp");
						if (dispGroup.startsWith("$")) {
							return new MemoryValueWrapper.IndexedDisplacementMemoryValueWrapper(size, segment,
								dispGroup.substring(1), base.register(), base.region(), index.register(),
								index.region());
						}
						long disp = NumberUtils.parseNumber(dispGroup);
						return new MemoryValueWrapper.IndexedDisplacementMemoryValueWrapper(size, segment,
							disp * (add ? 1 : -1), base.register(), base.region(), index.register(),
							index.region());
					}
				}
				// Scaled
				{
					Matcher m2 = Pattern.compile(
							"(?<neg>-)?(?<reg>" + regPattern + ")\\*(?<scale>" + NumberUtils.NUMBER_PATTERN + ")")
						.matcher(address);
					if (m2.matches()) {
						Registers.RegisterValue reg = Registers.RegisterValue.fromName(m2.group("reg"));
						boolean neg = m2.group("neg") != null;
						long scale = NumberUtils.parseNumber(m2.group("scale"));
						return new MemoryValueWrapper.ScaledMemoryValueWrapper(size, segment,
							scale * (neg ? -1 : 1), reg.register(), reg.region());
					}
				}
				// Scaled Displacement
				{
					Matcher m2 = Pattern.compile(
							"(?<neg>-)?(?<reg>" + regPattern + ")\\*(?<scale>" + NumberUtils.NUMBER_PATTERN +
								")(?<op>[+\\-])(?<disp>(" + NumberUtils.NUMBER_PATTERN + ")|(\\$.+))")
						.matcher(address);
					if (m2.matches()) {
						Registers.RegisterValue reg = Registers.RegisterValue.fromName(m2.group("reg"));
						boolean neg = m2.group("neg") != null;
						boolean add = m2.group("op").equals("+");
						long scale = NumberUtils.parseNumber(m2.group("scale"));
						String dispGroup = m2.group("disp");
						if (dispGroup.startsWith("$")) {
							return new MemoryValueWrapper.ScaledDisplacementMemoryValueWrapper(size, segment,
								dispGroup.substring(1), scale * (neg ? -1 : 1), reg.register(), reg.region());
						}
						long disp = NumberUtils.parseNumber(dispGroup);
						return new MemoryValueWrapper.ScaledDisplacementMemoryValueWrapper(size, segment,
							disp * (add ? 1 : -1), scale * (neg ? -1 : 1), reg.register(), reg.region());
					}
				}
				// Indexed Scaled
				{
					Matcher m2 = Pattern.compile(
						"(?<base>" + regPattern + ")(?<op>[+\\-])(?<index>" + regPattern + ")\\*(?<scale>" +
							NumberUtils.NUMBER_PATTERN + ")").matcher(address);
					if (m2.matches()) {
						Registers.RegisterValue base = Registers.RegisterValue.fromName(m2.group("base"));
						Registers.RegisterValue index = Registers.RegisterValue.fromName(m2.group("index"));
						boolean add = m2.group("op").equals("+");
						long scale = NumberUtils.parseNumber(m2.group("scale"));
						return new MemoryValueWrapper.IndexedScaledMemoryValueWrapper(size, segment,
							scale * (add ? 1 : -1), base.register(), base.region(), index.register(),
							index.region());
					}
				}
				// Indexed Scaled Displacement
				{
					Matcher m2 = Pattern.compile(
						"(?<base>" + regPattern + ")(?<op1>[+\\-])(?<index>" + regPattern + ")\\*(?<scale>" +
							NumberUtils.NUMBER_PATTERN + ")(?<op2>[+\\-])(?<disp>(" + NumberUtils.NUMBER_PATTERN +
							")|(\\$.+))").matcher(address);
					if (m2.matches()) {
						Registers.RegisterValue base = Registers.RegisterValue.fromName(m2.group("base"));
						Registers.RegisterValue index = Registers.RegisterValue.fromName(m2.group("index"));
						boolean add1 = m2.group("op1").equals("+");
						boolean add2 = m2.group("op2").equals("+");
						long scale = NumberUtils.parseNumber(m2.group("scale"));
						String dispGroup = m2.group("disp");
						if (dispGroup.startsWith("$")) {
							return new MemoryValueWrapper.IndexedScaledDisplacementMemoryValueWrapper(size,
								segment, dispGroup.substring(1), scale * (add1 ? 1 : -1), base.register(),
								base.region(), index.register(), index.region());
						}
						long disp = NumberUtils.parseNumber(dispGroup);
						return new MemoryValueWrapper.IndexedScaledDisplacementMemoryValueWrapper(size, segment,
							disp * (add2 ? 1 : -1), scale * (add1 ? 1 : -1), base.register(), base.region(),
							index.register(), index.region());
					}
				}
			}
		}
		throw new IllegalArgumentException("Invalid value: " + text);
	}
	
	@NotNull
	public static MemoryValueWrapper parseMemory(@NotNull String text, @NotNull ValueSize size) {
		Matcher m1 = Pattern.compile("((?<segment>(cs)|(ds)|(es)|(fs)|(gs)|(ss)):)?\\[(?<address>.+)]")
			.matcher(text.replaceAll("\\s", ""));
		if (m1.matches()) {
			MemoryValueWrapper.MemorySegment segment =
				m1.group("segment") == null ? MemoryValueWrapper.MemorySegment.DS :
					MemoryValueWrapper.MemorySegment.valueOf(m1.group("segment").toUpperCase());
			String address = m1.group("address").trim();
			String regPattern = "(al)|(ah)|(ax)|(eax)|(rax)|(bl)|(bh)|(bx)|(ebx)|(rbx)|" +
				"(cl)|(ch)|(cx)|(ecx)|(rcx)|(dl)|(dh)|(dx)|(edx)|(rdx)|" +
				"(si)|(esi)|(rsi)|(di)|(edi)|(rdi)|(bp)|(ebp)|(rbp)|(sp)|(esp)|(rsp)|" +
				"(r[89])|(r1[0-5])|(cs)|(ds)|(es)|(fs)|(gs)|(ss)|(rip)|(eip)|(ip)";
			
			// Direct
			{
				if (address.startsWith("$")) {
					return new MemoryValueWrapper.DirectMemoryValueWrapper(size, segment,
						address.substring(1));
				}
				if (NumberUtils.isNumber(address)) {
					return new MemoryValueWrapper.DirectMemoryValueWrapper(size, segment,
						NumberUtils.parseNumber(address));
				}
			}
			// Indirect
			{
				Matcher m2 = Pattern.compile(regPattern).matcher(address);
				if (m2.matches()) {
					Registers.RegisterValue reg = Registers.RegisterValue.fromName(address);
					return new MemoryValueWrapper.IndirectMemoryValueWrapper(size, segment, reg.register(),
						reg.region());
				}
			}
			// Displacement
			{
				Matcher m2 = Pattern.compile(
					"(?<reg>" + regPattern + ")(?<operator>[+\\-])(?<disp>(" + NumberUtils.NUMBER_PATTERN +
						")|(\\$.+))").matcher(address);
				if (m2.matches()) {
					Registers.RegisterValue reg = Registers.RegisterValue.fromName(m2.group("reg"));
					boolean add = m2.group("operator").equals("+");
					String dispGroup = m2.group("disp");
					if (dispGroup.startsWith("$")) {
						return new MemoryValueWrapper.DisplacementMemoryValueWrapper(size, segment,
							dispGroup.substring(1), reg.register(), reg.region());
					}
					long disp = NumberUtils.parseNumber(dispGroup);
					return new MemoryValueWrapper.DisplacementMemoryValueWrapper(size, segment,
						disp * (add ? 1 : -1), reg.register(), reg.region());
				}
			}
			// Indexed
			{
				Matcher m2 = Pattern.compile("(?<base>" + regPattern + ")\\+(?<index>" + regPattern + ")")
					.matcher(address);
				if (m2.matches()) {
					Registers.RegisterValue base = Registers.RegisterValue.fromName(m2.group("base"));
					Registers.RegisterValue index = Registers.RegisterValue.fromName(m2.group("index"));
					return new MemoryValueWrapper.IndexedMemoryValueWrapper(size, segment, base.register(),
						base.region(), index.register(), index.region());
				}
			}
			// Indexed Displacement
			{
				Matcher m2 = Pattern.compile(
					"(?<base>" + regPattern + ")\\+(?<index>" + regPattern + ")(?<op>[+\\-])(?<disp>(" +
						NumberUtils.NUMBER_PATTERN + ")|(\\$.+))").matcher(address);
				if (m2.matches()) {
					boolean add = m2.group("op").equals("+");
					Registers.RegisterValue base = Registers.RegisterValue.fromName(m2.group("base"));
					Registers.RegisterValue index = Registers.RegisterValue.fromName(m2.group("index"));
					String dispGroup = m2.group("disp");
					if (dispGroup.startsWith("$")) {
						return new MemoryValueWrapper.IndexedDisplacementMemoryValueWrapper(size, segment,
							dispGroup.substring(1), base.register(), base.region(), index.register(),
							index.region());
					}
					long disp = NumberUtils.parseNumber(dispGroup);
					return new MemoryValueWrapper.IndexedDisplacementMemoryValueWrapper(size, segment,
						disp * (add ? 1 : -1), base.register(), base.region(), index.register(),
						index.region());
				}
			}
			// Scaled
			{
				Matcher m2 = Pattern.compile(
						"(?<neg>-)?(?<reg>" + regPattern + ")\\*(?<scale>" + NumberUtils.NUMBER_PATTERN + ")")
					.matcher(address);
				if (m2.matches()) {
					Registers.RegisterValue reg = Registers.RegisterValue.fromName(m2.group("reg"));
					boolean neg = m2.group("neg") != null;
					long scale = NumberUtils.parseNumber(m2.group("scale"));
					return new MemoryValueWrapper.ScaledMemoryValueWrapper(size, segment,
						scale * (neg ? -1 : 1), reg.register(), reg.region());
				}
			}
			// Scaled Displacement
			{
				Matcher m2 = Pattern.compile(
					"(?<neg>-)?(?<reg>" + regPattern + ")\\*(?<scale>" + NumberUtils.NUMBER_PATTERN +
						")(?<op>[+\\-])(?<disp>(" + NumberUtils.NUMBER_PATTERN + ")|(\\$.+))").matcher(address);
				if (m2.matches()) {
					Registers.RegisterValue reg = Registers.RegisterValue.fromName(m2.group("reg"));
					boolean neg = m2.group("neg") != null;
					boolean add = m2.group("op").equals("+");
					long scale = NumberUtils.parseNumber(m2.group("scale"));
					String dispGroup = m2.group("disp");
					if (dispGroup.startsWith("$")) {
						return new MemoryValueWrapper.ScaledDisplacementMemoryValueWrapper(size, segment,
							dispGroup.substring(1), scale * (neg ? -1 : 1), reg.register(), reg.region());
					}
					long disp = NumberUtils.parseNumber(dispGroup);
					return new MemoryValueWrapper.ScaledDisplacementMemoryValueWrapper(size, segment,
						disp * (add ? 1 : -1), scale * (neg ? -1 : 1), reg.register(), reg.region());
				}
			}
			// Indexed Scaled
			{
				Matcher m2 = Pattern.compile(
					"(?<base>" + regPattern + ")(?<op>[+\\-])(?<index>" + regPattern + ")\\*(?<scale>" +
						NumberUtils.NUMBER_PATTERN + ")").matcher(address);
				if (m2.matches()) {
					Registers.RegisterValue base = Registers.RegisterValue.fromName(m2.group("base"));
					Registers.RegisterValue index = Registers.RegisterValue.fromName(m2.group("index"));
					boolean add = m2.group("op").equals("+");
					long scale = NumberUtils.parseNumber(m2.group("scale"));
					return new MemoryValueWrapper.IndexedScaledMemoryValueWrapper(size, segment,
						scale * (add ? 1 : -1), base.register(), base.region(), index.register(),
						index.region());
				}
			}
			// Indexed Scaled Displacement
			{
				Matcher m2 = Pattern.compile(
					"(?<base>" + regPattern + ")(?<op1>[+\\-])(?<index>" + regPattern + ")\\*(?<scale>" +
						NumberUtils.NUMBER_PATTERN + ")(?<op2>[+\\-])(?<disp>(" + NumberUtils.NUMBER_PATTERN +
						")|(\\$.+))").matcher(address);
				if (m2.matches()) {
					Registers.RegisterValue base = Registers.RegisterValue.fromName(m2.group("base"));
					Registers.RegisterValue index = Registers.RegisterValue.fromName(m2.group("index"));
					boolean add1 = m2.group("op1").equals("+");
					boolean add2 = m2.group("op2").equals("+");
					long scale = NumberUtils.parseNumber(m2.group("scale"));
					String dispGroup = m2.group("disp");
					if (dispGroup.startsWith("$")) {
						return new MemoryValueWrapper.IndexedScaledDisplacementMemoryValueWrapper(size, segment,
							dispGroup.substring(1), scale * (add1 ? 1 : -1), base.register(), base.region(),
							index.register(), index.region());
					}
					long disp = NumberUtils.parseNumber(dispGroup);
					return new MemoryValueWrapper.IndexedScaledDisplacementMemoryValueWrapper(size, segment,
						disp * (add2 ? 1 : -1), scale * (add1 ? 1 : -1), base.register(), base.region(),
						index.register(), index.region());
				}
			}
		}
		throw new IllegalArgumentException("Invalid value: " + text);
	}
	
	public static final class ImmediateValueWrapper extends ValueWrapper {
		
		private final long value;
		@Nullable
		private final String label;
		
		public ImmediateValueWrapper(@NotNull ValueSize size, long value) {
			super(size, Type.IMMEDIATE);
			this.value = value;
			this.label = null;
		}
		
		public ImmediateValueWrapper(@NotNull ValueSize size, @NotNull String label) {
			super(size, Type.IMMEDIATE);
			this.value = 0;
			this.label = label;
		}
		
		@NotNull
		@Override
		public ValueWrapper resolved(@NotNull Function<String, Long> labelResolver) {
			if (this.label != null) {
				Long value = labelResolver.apply(this.label);
				if (value == null) {
					throw new IllegalArgumentException("Label not found: " + this.label);
				}
				return new ImmediateValueWrapper(this.getSize(), value);
			}
			return this;
		}
		
		@NotNull
		@Override
		public String toAssembly() {
			return getSize().getName() + " 0x" + Long.toUnsignedString(this.value, 16);
		}
		
		@Override
		public long read(@NotNull InstructionContext ctx) {
			return this.value;
		}
		
		@Override
		public void write(@NotNull InstructionContext ctx, long value) {
			throw new UnsupportedOperationException("Cannot write to an immediate value");
		}
		
		@Override
		protected void assemble0(@NotNull ByteBuf buf) {
			switch (this.getSize()) {
				case BYTE -> buf.writeByte((byte) (this.value & 0xFFL));
				case WORD -> buf.writeShortLE((short) (this.value & 0xFFFFL));
				case DWORD -> buf.writeIntLE((int) (this.value & 0xFFFFFFFFL));
				case QWORD -> buf.writeLongLE(this.value);
			}
		}
		
		@NotNull
		public static ImmediateValueWrapper load(@NotNull InstructionContext ctx,
			@NotNull ValueSize size) {
			return new ImmediateValueWrapper(size, switch (size) {
				case BYTE -> ctx.readInstructionByte();
				case WORD -> ctx.readInstructionWord();
				case DWORD -> ctx.readInstructionDword();
				case QWORD -> ctx.readInstructionQword();
			});
		}
		
		protected int getLength0() {
			return getSize().getSize();
		}
		
	}
	
	public static final class RegisterValueWrapper extends ValueWrapper {
		
		@NotNull
		private final Registers.Register register;
		@NotNull
		private final Registers.RegisterRegion region;
		
		public RegisterValueWrapper(@NotNull Registers.Register register,
			@NotNull Registers.RegisterRegion region) {
			super(region.getValueSize(), Type.REGISTER);
			this.register = register;
			this.region = region;
		}
		
		@NotNull
		public String toAssembly() {
			return new Registers.RegisterValue(this.register, this.region).getName();
		}
		
		@Override
		public long read(@NotNull InstructionContext ctx) {
			long value = ctx.registers().readRegister(this.register, this.region);
			if (ctx.computer().debugPrint()) {
				System.out.println(
					"Read " + this.toAssembly() + " = 0x" + Long.toUnsignedString(value, 16));
			}
			return value;
		}
		
		@Override
		public void write(@NotNull InstructionContext ctx, long value) {
			ctx.registers().writeRegister(this.register, this.region, value);
			if (ctx.computer().debugPrint()) {
				System.out.println("Write " + this.toAssembly() + " = 0x" +
					Long.toUnsignedString(ctx.registers().readRegister(this.register, this.region), 16));
			}
		}
		
		@Override
		protected void assemble0(@NotNull ByteBuf buf) {
			// Register value is 5 bits, region is 3 bits
			buf.writeByte((byte) (this.register.getValue() << 3 | this.region.getValue()));
		}
		
		@NotNull
		public static RegisterValueWrapper load(@NotNull InstructionContext ctx,
			@NotNull ValueSize size) {
			long data = ctx.readInstructionByte();
			Registers.Register register = Registers.Register.fromValue((int) (data >> 3));
			Registers.RegisterRegion region = Registers.RegisterRegion.fromValue((int) (data & 0b111));
			if (region.getValueSize() != size) {
				throw new IllegalArgumentException("Invalid register region size");
			}
			return new RegisterValueWrapper(register, region);
		}
		
		protected int getLength0() {
			return 1;
		}
		
	}
	
	public static abstract class MemoryValueWrapper extends ValueWrapper {
		
		@NotNull
		protected final MemoryAddressingMode addressingMode;
		@NotNull
		protected final MemorySegment segment;
		
		protected MemoryValueWrapper(@NotNull ValueSize size,
			@NotNull MemoryAddressingMode addressingMode, @NotNull MemorySegment segment) {
			super(size, Type.MEMORY);
			this.addressingMode = addressingMode;
			this.segment = segment;
		}
		
		public abstract long getAddress(@NotNull InstructionContext ctx);
		
		public long getFullAddress(@NotNull InstructionContext ctx) {
			long address = this.getAddress(ctx);
			long segment =
				ctx.registers().readRegister(this.segment.getRegister(), Registers.RegisterRegion.WORD);
			return (segment << 4) + address;
		}
		
		@Override
		public long read(@NotNull InstructionContext ctx) {
			long segment =
				ctx.registers().readRegister(this.segment.getRegister(), Registers.RegisterRegion.WORD);
			long address = this.getAddress(ctx);
			long value = switch (this.getSize()) {
				case BYTE -> ctx.memory().readByte(address, segment);
				case WORD -> ctx.memory().readWord(address, segment);
				case DWORD -> ctx.memory().readDword(address, segment);
				case QWORD -> ctx.memory().readQword(address, segment);
			};
			if (ctx.computer().debugPrint()) {
				System.out.println(
					"Read " + this.toAssembly() + ", resolved to 0x" + Long.toUnsignedString(segment, 16) +
						":[0x" + Long.toUnsignedString(address, 16) + "] = 0x" +
						Long.toUnsignedString(value, 16));
			}
			return value;
		}
		
		@Override
		public void write(@NotNull InstructionContext ctx, long value) {
			long segment =
				ctx.registers().readRegister(this.segment.getRegister(), Registers.RegisterRegion.WORD);
			long address = this.getAddress(ctx);
			switch (this.getSize()) {
				case BYTE -> ctx.memory().writeByte(address, segment, value);
				case WORD -> ctx.memory().writeWord(address, segment, value);
				case DWORD -> ctx.memory().writeDword(address, segment, value);
				case QWORD -> ctx.memory().writeQword(address, segment, value);
			}
			
			if (ctx.computer().debugPrint()) {
				System.out.println(
					"Write " + this.toAssembly() + ", resolved to 0x" + Long.toUnsignedString(segment, 16) +
						":[0x" + Long.toUnsignedString(address, 16) + "] = 0x" +
						Long.toUnsignedString(value, 16));
			}
		}
		
		@Override
		protected void assemble0(@NotNull ByteBuf buf) {
			buf.writeByte((byte) (this.addressingMode.getValue() << 3 | this.segment.getValue()));
			this.assemble1(buf);
		}
		
		protected abstract void assemble1(@NotNull ByteBuf buf);
		
		public void assembleMemory(@NotNull ByteBuf buf) {
			this.assemble0(buf);
		}
		
		protected int getLength0() {
			return 1 + this.getLength1();
		}
		
		public int getLengthMemory() {
			return getLength0();
		}
		
		protected abstract int getLength1();
		
		@NotNull
		@Override
		public String toAssembly() {
			return getSize().getName() + " " + this.toAssemblyMemory();
		}
		
		@NotNull
		public abstract String toAssemblyMemory();
		
		@NotNull
		@Override
		public MemoryValueWrapper resolved(@NotNull Function<String, Long> labelResolver) {
			return this;
		}
		
		@NotNull
		public static MemoryValueWrapper load(@NotNull InstructionContext ctx,
			@NotNull ValueSize size) {
			byte data = (byte) ctx.readInstructionByte();
			MemoryAddressingMode addressingMode = MemoryAddressingMode.fromValue((byte) (data >> 3));
			MemorySegment segment = MemorySegment.fromValue((byte) (data & 0b111));
			return switch (addressingMode) {
				case DIRECT -> DirectMemoryValueWrapper.load(ctx, size, segment);
				case INDIRECT -> IndirectMemoryValueWrapper.load(ctx, size, segment);
				case DISPLACEMENT -> DisplacementMemoryValueWrapper.load(ctx, size, segment);
				case INDEXED -> IndexedMemoryValueWrapper.load(ctx, size, segment);
				case INDEXED_DISPLACEMENT -> IndexedDisplacementMemoryValueWrapper.load(ctx, size, segment);
				case SCALED -> ScaledMemoryValueWrapper.load(ctx, size, segment);
				case SCALED_DISPLACEMENT -> ScaledDisplacementMemoryValueWrapper.load(ctx, size, segment);
				case INDEXED_SCALED -> IndexedScaledMemoryValueWrapper.load(ctx, size, segment);
				case INDEXED_SCALED_DISPLACEMENT ->
					IndexedScaledDisplacementMemoryValueWrapper.load(ctx, size, segment);
			};
		}
		
		/**
		 * Direct
		 */
		public static final class DirectMemoryValueWrapper extends MemoryValueWrapper {
			
			private final long address;
			@Nullable
			private final String label;
			
			public DirectMemoryValueWrapper(@NotNull ValueSize size, @NotNull MemorySegment segment,
				long address) {
				super(size, MemoryAddressingMode.DIRECT, segment);
				this.address = address;
				this.label = null;
			}
			
			public DirectMemoryValueWrapper(@NotNull ValueSize size, @NotNull MemorySegment segment,
				@NotNull String label) {
				super(size, MemoryAddressingMode.DIRECT, segment);
				this.address = 0;
				this.label = label;
			}
			
			@NotNull
			@Override
			public MemoryValueWrapper resolved(@NotNull Function<String, Long> labelResolver) {
				if (this.label != null) {
					return new DirectMemoryValueWrapper(getSize(), segment, labelResolver.apply(this.label));
				}
				return this;
			}
			
			@Override
			public long getAddress(@NotNull InstructionContext ctx) {
				return this.address;
			}
			
			@Override
			protected void assemble1(@NotNull ByteBuf buf) {
				// The address is always stored as a qword
				buf.writeLongLE(this.address);
			}
			
			@Override
			protected int getLength1() {
				return 8;
			}
			
			@NotNull
			public static DirectMemoryValueWrapper load(@NotNull InstructionContext ctx,
				@NotNull ValueSize size, @NotNull MemorySegment segment) {
				// The address is always stored as a qword
				long address = ctx.readInstructionQword();
				return new DirectMemoryValueWrapper(size, segment, address);
			}
			
			@NotNull
			@Override
			public String toAssemblyMemory() {
				return segment.name().toLowerCase() + ":[0x" + Long.toUnsignedString(this.address, 16) +
					"]";
			}
			
		}
		
		/**
		 * Register
		 */
		public static final class IndirectMemoryValueWrapper extends MemoryValueWrapper {
			
			@NotNull
			private final Registers.Register register;
			@NotNull
			private final Registers.RegisterRegion registerRegion;
			
			public IndirectMemoryValueWrapper(@NotNull ValueSize size, @NotNull MemorySegment segment,
				@NotNull Registers.Register register, @NotNull Registers.RegisterRegion registerRegion) {
				super(size, MemoryAddressingMode.INDIRECT, segment);
				this.register = register;
				this.registerRegion = registerRegion;
			}
			
			@Override
			public long getAddress(@NotNull InstructionContext ctx) {
				return ctx.registers().readRegister(this.register, this.registerRegion);
			}
			
			@Override
			protected void assemble1(@NotNull ByteBuf buf) {
				// Register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.register.getValue() << 3 | this.registerRegion.getValue()));
			}
			
			@Override
			protected int getLength1() {
				return 1;
			}
			
			@NotNull
			public static IndirectMemoryValueWrapper load(@NotNull InstructionContext ctx,
				@NotNull ValueSize size, @NotNull MemorySegment segment) {
				byte data = (byte) ctx.readInstructionByte();
				Registers.Register register = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion registerRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				return new IndirectMemoryValueWrapper(size, segment, register, registerRegion);
			}
			
			@NotNull
			@Override
			public String toAssemblyMemory() {
				return segment.name().toLowerCase() + ":[" +
					new Registers.RegisterValue(this.register, this.registerRegion).getName() + "]";
			}
			
		}
		
		/**
		 * Register + Displacement
		 */
		public static final class DisplacementMemoryValueWrapper extends MemoryValueWrapper {
			
			private final long displacement;
			@Nullable
			private final String label;
			
			@NotNull
			private final Registers.Register register;
			@NotNull
			private final Registers.RegisterRegion registerRegion;
			
			public DisplacementMemoryValueWrapper(@NotNull ValueSize size, @NotNull MemorySegment segment,
				long displacement, @NotNull Registers.Register register,
				@NotNull Registers.RegisterRegion registerRegion) {
				super(size, MemoryAddressingMode.DISPLACEMENT, segment);
				this.displacement = displacement;
				this.label = null;
				this.register = register;
				this.registerRegion = registerRegion;
			}
			
			public DisplacementMemoryValueWrapper(@NotNull ValueSize size, @NotNull MemorySegment segment,
				@NotNull String label, @NotNull Registers.Register register,
				@NotNull Registers.RegisterRegion registerRegion) {
				super(size, MemoryAddressingMode.DISPLACEMENT, segment);
				this.displacement = 0;
				this.label = label;
				this.register = register;
				this.registerRegion = registerRegion;
			}
			
			@NotNull
			@Override
			public MemoryValueWrapper resolved(@NotNull Function<String, Long> labelResolver) {
				if (this.label != null) {
					return new DisplacementMemoryValueWrapper(getSize(), segment,
						labelResolver.apply(this.label), register, registerRegion);
				}
				return this;
			}
			
			@Override
			public long getAddress(@NotNull InstructionContext ctx) {
				return ctx.registers().readRegister(this.register, this.registerRegion) + this.displacement;
			}
			
			@Override
			protected void assemble1(@NotNull ByteBuf buf) {
				// The displacement is always stored as a qword
				buf.writeLongLE(this.displacement);
				// Register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.register.getValue() << 3 | this.registerRegion.getValue()));
			}
			
			@Override
			protected int getLength1() {
				return 9;
			}
			
			@NotNull
			public static DisplacementMemoryValueWrapper load(@NotNull InstructionContext ctx,
				@NotNull ValueSize size, @NotNull MemorySegment segment) {
				// The displacement is always stored as a qword
				long displacement = ctx.readInstructionQword();
				byte data = (byte) ctx.readInstructionByte();
				Registers.Register register = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion registerRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				return new DisplacementMemoryValueWrapper(size, segment, displacement, register,
					registerRegion);
			}
			
			@NotNull
			@Override
			public String toAssemblyMemory() {
				return segment.name().toLowerCase() + ":[" +
					new Registers.RegisterValue(this.register, this.registerRegion).getName() + " + 0x" +
					Long.toUnsignedString(this.displacement, 16) + "]";
			}
			
		}
		
		/**
		 * Register (base) + Register (index)
		 */
		public static final class IndexedMemoryValueWrapper extends MemoryValueWrapper {
			
			@NotNull
			private final Registers.Register base;
			@NotNull
			private final Registers.RegisterRegion baseRegion;
			
			@NotNull
			private final Registers.Register index;
			@NotNull
			private final Registers.RegisterRegion indexRegion;
			
			public IndexedMemoryValueWrapper(@NotNull ValueSize size, @NotNull MemorySegment segment,
				@NotNull Registers.Register base, @NotNull Registers.RegisterRegion baseRegion,
				@NotNull Registers.Register index, @NotNull Registers.RegisterRegion indexRegion) {
				super(size, MemoryAddressingMode.INDEXED, segment);
				this.base = base;
				this.baseRegion = baseRegion;
				this.index = index;
				this.indexRegion = indexRegion;
			}
			
			@Override
			public long getAddress(@NotNull InstructionContext ctx) {
				return ctx.registers().readRegister(this.base, this.baseRegion) +
					ctx.registers().readRegister(this.index, this.indexRegion);
			}
			
			@Override
			protected void assemble1(@NotNull ByteBuf buf) {
				// Base register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.base.getValue() << 3 | this.baseRegion.getValue()));
				// Index register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.index.getValue() << 3 | this.indexRegion.getValue()));
			}
			
			@Override
			protected int getLength1() {
				return 2;
			}
			
			@NotNull
			public static IndexedMemoryValueWrapper load(@NotNull InstructionContext ctx,
				@NotNull ValueSize size, @NotNull MemorySegment segment) {
				byte data = (byte) ctx.readInstructionByte();
				Registers.Register base = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion baseRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				data = (byte) ctx.readInstructionByte();
				Registers.Register index = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion indexRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				return new IndexedMemoryValueWrapper(size, segment, base, baseRegion, index, indexRegion);
			}
			
			@NotNull
			@Override
			public String toAssemblyMemory() {
				return segment.name().toLowerCase() + ":[" +
					new Registers.RegisterValue(this.base, this.baseRegion).getName() + " + " +
					new Registers.RegisterValue(this.index, this.indexRegion).getName() + "]";
			}
			
		}
		
		/**
		 * Register (base) + Register (index) + Displacement
		 */
		public static final class IndexedDisplacementMemoryValueWrapper extends MemoryValueWrapper {
			
			private final long displacement;
			@Nullable
			private final String label;
			
			@NotNull
			private final Registers.Register base;
			@NotNull
			private final Registers.RegisterRegion baseRegion;
			
			@NotNull
			private final Registers.Register index;
			@NotNull
			private final Registers.RegisterRegion indexRegion;
			
			public IndexedDisplacementMemoryValueWrapper(@NotNull ValueSize size,
				@NotNull MemorySegment segment, long displacement, @NotNull Registers.Register base,
				@NotNull Registers.RegisterRegion baseRegion, @NotNull Registers.Register index,
				@NotNull Registers.RegisterRegion indexRegion) {
				super(size, MemoryAddressingMode.INDEXED_DISPLACEMENT, segment);
				this.displacement = displacement;
				this.label = null;
				this.base = base;
				this.baseRegion = baseRegion;
				this.index = index;
				this.indexRegion = indexRegion;
			}
			
			public IndexedDisplacementMemoryValueWrapper(@NotNull ValueSize size,
				@NotNull MemorySegment segment, @NotNull String label, @NotNull Registers.Register base,
				@NotNull Registers.RegisterRegion baseRegion, @NotNull Registers.Register index,
				@NotNull Registers.RegisterRegion indexRegion) {
				super(size, MemoryAddressingMode.INDEXED_DISPLACEMENT, segment);
				this.displacement = 0;
				this.label = label;
				this.base = base;
				this.baseRegion = baseRegion;
				this.index = index;
				this.indexRegion = indexRegion;
			}
			
			@NotNull
			@Override
			public MemoryValueWrapper resolved(@NotNull Function<String, Long> labelResolver) {
				if (this.label != null) {
					return new IndexedDisplacementMemoryValueWrapper(getSize(), segment,
						labelResolver.apply(this.label), base, baseRegion, index, indexRegion);
				}
				return this;
			}
			
			@Override
			public long getAddress(@NotNull InstructionContext ctx) {
				return ctx.registers().readRegister(this.base, this.baseRegion) +
					ctx.registers().readRegister(this.index, this.indexRegion) + this.displacement;
			}
			
			@Override
			protected void assemble1(@NotNull ByteBuf buf) {
				// The displacement is always stored as a qword
				buf.writeLongLE(this.displacement);
				// Base register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.base.getValue() << 3 | this.baseRegion.getValue()));
				// Index register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.index.getValue() << 3 | this.indexRegion.getValue()));
			}
			
			@Override
			protected int getLength1() {
				return 10;
			}
			
			@NotNull
			public static IndexedDisplacementMemoryValueWrapper load(@NotNull InstructionContext ctx,
				@NotNull ValueSize size, @NotNull MemorySegment segment) {
				// The displacement is always stored as a qword
				long displacement = ctx.readInstructionQword();
				byte data = (byte) ctx.readInstructionByte();
				Registers.Register base = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion baseRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				data = (byte) ctx.readInstructionByte();
				Registers.Register index = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion indexRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				return new IndexedDisplacementMemoryValueWrapper(size, segment, displacement, base,
					baseRegion, index, indexRegion);
			}
			
			@NotNull
			@Override
			public String toAssemblyMemory() {
				return segment.name().toLowerCase() + ":[" +
					new Registers.RegisterValue(this.base, this.baseRegion).getName() + " + " +
					new Registers.RegisterValue(this.index, this.indexRegion).getName() + " + 0x" +
					Long.toUnsignedString(this.displacement, 16) + "]";
			}
			
		}
		
		/**
		 * Register * Scale
		 */
		public static final class ScaledMemoryValueWrapper extends MemoryValueWrapper {
			
			@NotNull
			private final Registers.Register register;
			@NotNull
			private final Registers.RegisterRegion registerRegion;
			
			private final long scale;
			
			public ScaledMemoryValueWrapper(@NotNull ValueSize size, @NotNull MemorySegment segment,
				long scale, @NotNull Registers.Register register,
				@NotNull Registers.RegisterRegion registerRegion) {
				super(size, MemoryAddressingMode.SCALED, segment);
				this.scale = scale;
				this.register = register;
				this.registerRegion = registerRegion;
			}
			
			@Override
			public long getAddress(@NotNull InstructionContext ctx) {
				return ctx.registers().readRegister(this.register, this.registerRegion) * this.scale;
			}
			
			@Override
			protected void assemble1(@NotNull ByteBuf buf) {
				// Register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.register.getValue() << 3 | this.registerRegion.getValue()));
				// The scale is always stored as a word
				buf.writeShortLE((short) this.scale);
			}
			
			@Override
			protected int getLength1() {
				return 3;
			}
			
			@NotNull
			public static ScaledMemoryValueWrapper load(@NotNull InstructionContext ctx,
				@NotNull ValueSize size, @NotNull MemorySegment segment) {
				byte data = (byte) ctx.readInstructionByte();
				Registers.Register register = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion registerRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				// The scale is always stored as a word
				long scale = ctx.readInstructionWord();
				return new ScaledMemoryValueWrapper(size, segment, scale, register, registerRegion);
			}
			
			@NotNull
			@Override
			public String toAssemblyMemory() {
				return segment.name().toLowerCase() + ":[" +
					new Registers.RegisterValue(this.register, this.registerRegion).getName() + " * 0x" +
					Long.toUnsignedString(this.scale, 16) + "]";
			}
			
		}
		
		/**
		 * Register * Scale + Displacement
		 */
		public static final class ScaledDisplacementMemoryValueWrapper extends MemoryValueWrapper {
			
			private final long displacement;
			@Nullable
			private final String label;
			
			@NotNull
			private final Registers.Register register;
			@NotNull
			private final Registers.RegisterRegion registerRegion;
			
			private final long scale;
			
			public ScaledDisplacementMemoryValueWrapper(@NotNull ValueSize size,
				@NotNull MemorySegment segment, @NotNull String label, long scale,
				@NotNull Registers.Register register, @NotNull Registers.RegisterRegion registerRegion) {
				super(size, MemoryAddressingMode.SCALED_DISPLACEMENT, segment);
				this.displacement = 0;
				this.label = label;
				this.register = register;
				this.registerRegion = registerRegion;
				this.scale = scale;
			}
			
			public ScaledDisplacementMemoryValueWrapper(@NotNull ValueSize size,
				@NotNull MemorySegment segment, long displacement, long scale,
				@NotNull Registers.Register register, @NotNull Registers.RegisterRegion registerRegion) {
				super(size, MemoryAddressingMode.SCALED_DISPLACEMENT, segment);
				this.displacement = displacement;
				this.label = null;
				this.register = register;
				this.registerRegion = registerRegion;
				this.scale = scale;
			}
			
			@NotNull
			@Override
			public MemoryValueWrapper resolved(@NotNull Function<String, Long> labelResolver) {
				if (this.label != null) {
					return new ScaledDisplacementMemoryValueWrapper(getSize(), segment,
						labelResolver.apply(this.label), scale, register, registerRegion);
				}
				return this;
			}
			
			@Override
			public long getAddress(@NotNull InstructionContext ctx) {
				return ctx.registers().readRegister(this.register, this.registerRegion) * this.scale +
					this.displacement;
			}
			
			@Override
			protected void assemble1(@NotNull ByteBuf buf) {
				// The displacement is always stored as a qword
				buf.writeLongLE(this.displacement);
				// Register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.register.getValue() << 3 | this.registerRegion.getValue()));
				// The scale is always stored as a word
				buf.writeShortLE((short) this.scale);
			}
			
			@Override
			protected int getLength1() {
				return 11;
			}
			
			@NotNull
			public static ScaledDisplacementMemoryValueWrapper load(@NotNull InstructionContext ctx,
				@NotNull ValueSize size, @NotNull MemorySegment segment) {
				// The displacement is always stored as a qword
				long displacement = ctx.readInstructionQword();
				byte data = (byte) ctx.readInstructionByte();
				Registers.Register register = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion registerRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				// The scale is always stored as a word
				long scale = ctx.readInstructionWord();
				return new ScaledDisplacementMemoryValueWrapper(size, segment, displacement, scale,
					register, registerRegion);
			}
			
			@NotNull
			@Override
			public String toAssemblyMemory() {
				return segment.name().toLowerCase() + ":[" +
					new Registers.RegisterValue(this.register, this.registerRegion).getName() + " * 0x" +
					Long.toUnsignedString(this.scale, 16) + " + 0x" +
					Long.toUnsignedString(this.displacement, 16) + "]";
			}
			
		}
		
		/**
		 * Register + Register * Scale
		 */
		public static final class IndexedScaledMemoryValueWrapper extends MemoryValueWrapper {
			
			@NotNull
			private final Registers.Register base;
			@NotNull
			private final Registers.RegisterRegion baseRegion;
			
			@NotNull
			private final Registers.Register index;
			@NotNull
			private final Registers.RegisterRegion indexRegion;
			
			private final long scale;
			
			public IndexedScaledMemoryValueWrapper(@NotNull ValueSize size,
				@NotNull MemorySegment segment, long scale, @NotNull Registers.Register base,
				@NotNull Registers.RegisterRegion baseRegion, @NotNull Registers.Register index,
				@NotNull Registers.RegisterRegion indexRegion) {
				super(size, MemoryAddressingMode.INDEXED_SCALED, segment);
				this.scale = scale;
				this.base = base;
				this.baseRegion = baseRegion;
				this.index = index;
				this.indexRegion = indexRegion;
			}
			
			@Override
			public long getAddress(@NotNull InstructionContext ctx) {
				return ctx.registers().readRegister(this.base, this.baseRegion) +
					ctx.registers().readRegister(this.index, this.indexRegion) * this.scale;
			}
			
			@Override
			protected void assemble1(@NotNull ByteBuf buf) {
				// Base register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.base.getValue() << 3 | this.baseRegion.getValue()));
				// Index register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.index.getValue() << 3 | this.indexRegion.getValue()));
				// The scale is always stored as a word
				buf.writeShortLE((short) this.scale);
			}
			
			@Override
			protected int getLength1() {
				return 4;
			}
			
			@NotNull
			public static IndexedScaledMemoryValueWrapper load(@NotNull InstructionContext ctx,
				@NotNull ValueSize size, @NotNull MemorySegment segment) {
				byte data = (byte) ctx.readInstructionByte();
				Registers.Register base = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion baseRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				data = (byte) ctx.readInstructionByte();
				Registers.Register index = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion indexRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				// The scale is always stored as a word
				long scale = ctx.readInstructionWord();
				return new IndexedScaledMemoryValueWrapper(size, segment, scale, base, baseRegion, index,
					indexRegion);
			}
			
			@NotNull
			@Override
			public String toAssemblyMemory() {
				return segment.name().toLowerCase() + ":[" +
					new Registers.RegisterValue(this.base, this.baseRegion).getName() + " + " +
					new Registers.RegisterValue(this.index, this.indexRegion).getName() + " * 0x" +
					Long.toUnsignedString(this.scale, 16) + "]";
			}
			
		}
		
		/**
		 * Register + Register * Scale + Displacement
		 */
		public static final class IndexedScaledDisplacementMemoryValueWrapper
			extends MemoryValueWrapper {
			
			private final long displacement;
			
			@Nullable
			private final String label;
			
			@NotNull
			private final Registers.Register base;
			@NotNull
			private final Registers.RegisterRegion baseRegion;
			
			@NotNull
			private final Registers.Register index;
			@NotNull
			private final Registers.RegisterRegion indexRegion;
			
			private final long scale;
			
			public IndexedScaledDisplacementMemoryValueWrapper(@NotNull ValueSize size,
				@NotNull MemorySegment segment, long displacement, long scale,
				@NotNull Registers.Register base, @NotNull Registers.RegisterRegion baseRegion,
				@NotNull Registers.Register index, @NotNull Registers.RegisterRegion indexRegion) {
				super(size, MemoryAddressingMode.INDEXED_SCALED_DISPLACEMENT, segment);
				this.label = null;
				this.displacement = displacement;
				this.base = base;
				this.baseRegion = baseRegion;
				this.index = index;
				this.indexRegion = indexRegion;
				this.scale = scale;
			}
			
			public IndexedScaledDisplacementMemoryValueWrapper(@NotNull ValueSize size,
				@NotNull MemorySegment segment, @NotNull String label, long scale,
				@NotNull Registers.Register base, @NotNull Registers.RegisterRegion baseRegion,
				@NotNull Registers.Register index, @NotNull Registers.RegisterRegion indexRegion) {
				super(size, MemoryAddressingMode.INDEXED_SCALED_DISPLACEMENT, segment);
				this.label = label;
				this.displacement = 0;
				this.base = base;
				this.baseRegion = baseRegion;
				this.index = index;
				this.indexRegion = indexRegion;
				this.scale = scale;
			}
			
			@NotNull
			@Override
			public MemoryValueWrapper resolved(@NotNull Function<String, Long> labelResolver) {
				if (this.label != null) {
					return new IndexedScaledDisplacementMemoryValueWrapper(getSize(), this.segment,
						labelResolver.apply(this.label), this.scale, this.base, this.baseRegion, this.index,
						this.indexRegion);
				}
				return this;
			}
			
			@Override
			public long getAddress(@NotNull InstructionContext ctx) {
				return ctx.registers().readRegister(this.base, this.baseRegion) +
					ctx.registers().readRegister(this.index, this.indexRegion) * this.scale +
					this.displacement;
			}
			
			@Override
			protected void assemble1(@NotNull ByteBuf buf) {
				// The displacement is always stored as a qword
				buf.writeLongLE(this.displacement);
				// Base register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.base.getValue() << 3 | this.baseRegion.getValue()));
				// Index register value is 5 bits and region is 3 bits
				buf.writeByte((byte) (this.index.getValue() << 3 | this.indexRegion.getValue()));
				// The scale is always stored as a word
				buf.writeShortLE((short) this.scale);
			}
			
			@Override
			protected int getLength1() {
				return 12;
			}
			
			@NotNull
			public static IndexedScaledDisplacementMemoryValueWrapper load(
				@NotNull InstructionContext ctx, @NotNull ValueSize size, @NotNull MemorySegment segment) {
				// The displacement is always stored as a qword
				long displacement = ctx.readInstructionQword();
				byte data = (byte) ctx.readInstructionByte();
				Registers.Register base = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion baseRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				data = (byte) ctx.readInstructionByte();
				Registers.Register index = Registers.Register.fromValue(data >> 3);
				Registers.RegisterRegion indexRegion = Registers.RegisterRegion.fromValue(data & 0b111);
				// The scale is always stored as a word
				long scale = ctx.readInstructionWord();
				return new IndexedScaledDisplacementMemoryValueWrapper(size, segment, displacement, scale,
					base, baseRegion, index, indexRegion);
			}
			
			@NotNull
			@Override
			public String toAssemblyMemory() {
				return segment.name().toLowerCase() + ":[" +
					new Registers.RegisterValue(this.base, this.baseRegion).getName() + " + " +
					new Registers.RegisterValue(this.index, this.indexRegion).getName() + " * 0x" +
					Long.toUnsignedString(this.scale, 16) + " + 0x" +
					Long.toUnsignedString(this.displacement, 16) + "]";
			}
			
		}
		
		@Getter
		public enum MemoryAddressingMode {
			
			/**
			 * Immediate value
			 */
			DIRECT(0),
			/**
			 * Register value
			 */
			INDIRECT(1),
			/**
			 * Register value + immediate value
			 */
			DISPLACEMENT(2),
			/**
			 * Register value + register value
			 */
			INDEXED(3),
			/**
			 * Register value + register value + immediate value
			 */
			INDEXED_DISPLACEMENT(4),
			/**
			 * Register value * scale
			 */
			SCALED(5),
			/**
			 * Register value * scale + immediate value
			 */
			SCALED_DISPLACEMENT(6),
			/**
			 * Register value + register value * scale
			 */
			INDEXED_SCALED(7),
			/**
			 * Register value + register value * scale + immediate value
			 */
			INDEXED_SCALED_DISPLACEMENT(8);
			
			private final byte value;
			
			@NotNull
			private static final Map<Byte, MemoryAddressingMode> VALUES_MAP = new HashMap<>();
			
			static {
				for (MemoryAddressingMode mode : values()) {
					VALUES_MAP.put(mode.value, mode);
				}
			}
			
			MemoryAddressingMode(int value) {
				this.value = (byte) value;
			}
			
			@NotNull
			public static MemoryAddressingMode fromValue(byte value) {
				MemoryAddressingMode mode = VALUES_MAP.get(value);
				if (mode == null) {
					throw new IllegalArgumentException("Invalid value: " + value);
				}
				return mode;
			}
			
		}
		
		@Getter
		public enum MemorySegment {
			
			CS(0, Registers.Register.CS),
			DS(1, Registers.Register.DS),
			ES(2, Registers.Register.ES),
			FS(3, Registers.Register.FS),
			GS(4, Registers.Register.GS),
			SS(5, Registers.Register.SS);
			
			private final byte value;
			@NotNull
			private final Registers.Register register;
			
			MemorySegment(int value, @NotNull Registers.Register register) {
				this.value = (byte) value;
				this.register = register;
			}
			
			@NotNull
			public static MemorySegment fromValue(byte value) {
				return switch (value) {
					case 0 -> CS;
					case 1 -> DS;
					case 2 -> ES;
					case 3 -> FS;
					case 4 -> GS;
					case 5 -> SS;
					default -> throw new IllegalArgumentException("Invalid value: " + value);
				};
			}
			
		}
		
	}
	
	@Getter
	public enum Type {
		
		REGISTER(0),
		MEMORY(1),
		IMMEDIATE(2);
		
		/**
		 * The value of the type. (used for instruction encoding)
		 */
		private final byte value;
		
		Type(int value) {
			this.value = (byte) value;
		}
		
		@NotNull
		public static Type fromValue(int value) {
			return switch (value) {
				case 0 -> REGISTER;
				case 1 -> MEMORY;
				case 2 -> IMMEDIATE;
				default -> throw new IllegalArgumentException("Invalid value: " + value);
			};
		}
		
	}
	
}
