package com.unleqitq.computersimulator.utils;

import com.unleqitq.computersimulator.ValueSize;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class FlagsUtils {
	
	/**
	 * Checks if the value is negative.
	 *
	 * @param size  The size of the value
	 * @param value The value
	 * @return {@code true} if the value is negative (the sign flag needs to be set),
	 * {@code false} otherwise
	 */
	public static boolean isSignFlag(@NotNull ValueSize size, long value) {
		return switch (size) {
			case BYTE -> (value & 0x80L) != 0;
			case WORD -> (value & 0x8000L) != 0;
			case DWORD -> (value & 0x80000000L) != 0;
			case QWORD -> (value & 0x8000000000000000L) != 0;
		};
	}
	
	/**
	 * Checks if the value is zero.
	 *
	 * @param size  The size of the value
	 * @param value The value
	 * @return {@code true} if the value is zero (the zero flag needs to be set),
	 * {@code false} otherwise
	 */
	public static boolean isZeroFlag(@NotNull ValueSize size, long value) {
		return switch (size) {
			case BYTE -> (value & 0xFFL) == 0;
			case WORD -> (value & 0xFFFFL) == 0;
			case DWORD -> (value & 0xFFFFFFFFL) == 0;
			case QWORD -> value == 0;
		};
	}
	
	/**
	 * Checks if the value has an even number of bits set to 1.
	 *
	 * @param size  The size of the value
	 * @param value The value
	 * @return {@code true} if the value has an even number of bits set to 1 (the parity flag needs to be set),
	 * {@code false} otherwise
	 */
	public static boolean isParityFlag(@NotNull ValueSize size, long value) {
		return Long.bitCount(switch (size) {
			case BYTE -> value & 0xFFL;
			case WORD -> value & 0xFFFFL;
			case DWORD -> value & 0xFFFFFFFFL;
			case QWORD -> value;
		}) % 2 == 0;
	}
	
}
