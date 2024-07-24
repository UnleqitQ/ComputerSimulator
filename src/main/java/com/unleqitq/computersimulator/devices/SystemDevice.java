package com.unleqitq.computersimulator.devices;

import com.unleqitq.computersimulator.ComputerSimulator;
import com.unleqitq.computersimulator.components.DeviceAccessor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SystemDevice extends DeviceAccessor.AbstractDevice {
	
	public static final long DEFAULT_PORT = 0x0L;
	
	@NotNull
	private final Random random = new Random();
	
	@NotNull
	private final ComputerSimulator computer;
	
	public SystemDevice(@NotNull ComputerSimulator computer) {
		super("System");
		this.computer = computer;
	}
	
	@Override
	public long read(long address) {
		InputAction action = InputAction.byAddress(address);
		if (action == null) {
			return 0;
		}
		return switch (action) {
			case GET_MEMORY_SIZE -> Integer.toUnsignedLong(computer.memory().getSize());
			case GET_HOUR -> Instant.now().get(ChronoField.HOUR_OF_DAY);
			case GET_MINUTE -> Instant.now().get(ChronoField.MINUTE_OF_HOUR);
			case GET_SECOND -> Instant.now().get(ChronoField.SECOND_OF_MINUTE);
			case GET_MILLISECOND -> Instant.now().get(ChronoField.MILLI_OF_SECOND);
			case GET_MILLIS -> System.currentTimeMillis();
			case GET_NANOS -> System.nanoTime();
			case GET_RANDOM_QWORD -> random.nextLong();
		};
	}
	
	/**
	 * Addresses the program requests data from the device
	 */
	@Accessors (fluent = false)
	@Getter
	public enum InputAction {
		// Specifications
		GET_MEMORY_SIZE(0x100L),
		
		// Clock
		GET_HOUR (0x200L),
		GET_MINUTE (0x201L),
		GET_SECOND (0x202L),
		GET_MILLISECOND (0x203L),
		
		GET_MILLIS (0x210L),
		GET_NANOS (0x211L),
		
		// Random
		GET_RANDOM_QWORD (0x1004L),
		;
		
		private final long address;
		
		@NotNull
		private static final Map<Long, InputAction> ADDRESS_MAP = Arrays.stream(values())
			.collect(Collectors.toMap(InputAction::getAddress, Function.identity()));
		
		@Nullable
		public static InputAction byAddress(long address) {
			return ADDRESS_MAP.get(address);
		}
		
		InputAction(long address) {
			this.address = address;
		}
	}
	
}
