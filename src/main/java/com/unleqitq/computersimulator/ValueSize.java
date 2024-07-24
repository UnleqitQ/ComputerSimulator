package com.unleqitq.computersimulator;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * An enum that represents the size of a value.
 * The names are the same as the ones used in the Intel x86 architecture.
 */
@Getter
public enum ValueSize {
	
	BYTE("byte", 1, 0, Byte.class, byte.class),
	WORD("word", 2, 1, Short.class, short.class),
	DWORD("dword", 4, 2, Integer.class, int.class),
	QWORD("qword", 8, 3, Long.class, long.class);
	
	/**
	 * The name of the size
	 */
	@NotNull
	private final String name;
	/**
	 * The size of the value in bytes
	 */
	private final int size;
	/**
	 * The value of the size (for storing it in instructions)
	 */
	private final int value;
	/**
	 * The wrapper class for the size
	 */
	@NotNull
	private final Class<? extends Number> wrapperClass;
	/**
	 * The primitive class for the size
	 */
	@NotNull
	private final Class<?> primitiveClass;
	
	ValueSize(@NotNull String name, int size, int value,
		@NotNull Class<? extends Number> wrapperClass, @NotNull Class<?> primitiveClass) {
		this.name = name;
		this.size = size;
		this.value = value;
		this.wrapperClass = wrapperClass;
		this.primitiveClass = primitiveClass;
	}
	
	@NotNull
	public static ValueSize fromValue(int value) {
		return switch (value) {
			case 0 -> BYTE;
			case 1 -> WORD;
			case 2 -> DWORD;
			case 3 -> QWORD;
			default -> throw new IllegalArgumentException("Invalid value size: " + value);
		};
	}
	
	public static ValueSize min(@NotNull ValueSize a, @NotNull ValueSize b) {
		return a.size < b.size ? a : b;
	}
	
	public static ValueSize max(@NotNull ValueSize a, @NotNull ValueSize b) {
		return a.size > b.size ? a : b;
	}
	
}
