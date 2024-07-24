package com.unleqitq.computersimulator;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors (fluent = true)
@Getter
@Builder
public class ComputerSpecs {
	
	/**
	 * The size of the memory in bytes.
	 */
	private final int memorySize;
	
}
