package com.unleqitq.computersimulator.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class NumberUtils {
	
	public final String HEX_PATTERN = "-?0x[0-9a-fA-F_]+";
	public final String BIN_PATTERN = "-?0b[01_]+";
	public final String DEC_PATTERN = "-?[0-9_]+";
	public final String NUMBER_PATTERN =
		"(" + HEX_PATTERN + ")|(" + BIN_PATTERN + ")|(" + DEC_PATTERN + ")";
	
	
	public long parseNumber(@NotNull String s) throws NumberFormatException {
		s = s.replaceAll("[_\\s]", "");
		{
			Matcher matcher = Pattern.compile("(?<sign>-?)0x(?<value>[0-9a-fA-F]+)").matcher(s);
			if (matcher.matches()) {
				return Long.parseUnsignedLong(matcher.group("value"), 16) *
					("-".equals(matcher.group("sign")) ? -1 : 1);
			}
		}
		{
			Matcher matcher = Pattern.compile("(?<sign>-?)0b(?<value>[01]+)").matcher(s);
			if (matcher.matches()) {
				return Long.parseUnsignedLong(matcher.group("value"), 2) *
					("-".equals(matcher.group("sign")) ? -1 : 1);
			}
		}
		{
			Matcher matcher = Pattern.compile("(?<sign>-?)(?<value>[0-9]+)").matcher(s);
			if (matcher.matches()) {
				return Long.parseUnsignedLong(matcher.group("value"), 10) *
					("-".equals(matcher.group("sign")) ? -1 : 1);
			}
		}
		throw new NumberFormatException("Invalid number format: " + s);
	}
	
	public boolean isNumber(@NotNull String s) {
		return s.replaceAll("[_\\s]", "").matches(NUMBER_PATTERN);
	}
	
}
