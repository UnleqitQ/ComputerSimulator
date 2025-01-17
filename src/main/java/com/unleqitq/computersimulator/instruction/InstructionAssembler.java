package com.unleqitq.computersimulator.instruction;

import com.unleqitq.computersimulator.utils.NumberUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class InstructionAssembler {
	
	@NotNull
	private String resolveIncludes(@NotNull String code, @NotNull File dir,
		@NotNull Collection<File> includePaths) {
		return resolveIncludes(code, dir, new HashMap<>(), new ArrayList<>(), includePaths);
	}
	
	@SuppressWarnings ("InfiniteRecursion")
	@NotNull
	private String resolveIncludes(@NotNull String code, @NotNull File dir,
		@NotNull Map<File, String> includesCache, @NotNull List<File> includeStack,
		@NotNull Collection<File> includePaths) {
		code = minify(removeComments(code));
		try {
			code = removePragmaOnce(code);
			// @include <[path]> ; or @include "[path]" ;
			Pattern includePattern =
				Pattern.compile("@include\\s+(?<include>(\"[^\"]+\")|(<[^>]+>))\\s*;");
			while (true) {
				Matcher matcher = includePattern.matcher(code);
				if (!matcher.find()) {
					break;
				}
				String includeStr = matcher.group("include");
				// quoted includes are resolved relative to the current file
				// unquoted includes are resolved from include paths
				boolean isQuoted = includeStr.startsWith("\"");
				String path = includeStr.substring(1, includeStr.length() - 1);
				// Use absolute paths to avoid mapping issues
				File includeFile;
				if (isQuoted) {
					includeFile = resolveFile(dir, path);
					if (includeFile == null) {
						throw new RuntimeException("File not found: " + path);
					}
				}
				else {
					includeFile = resolveFile(dir, path, includePaths);
				}
				String fileContent = includesCache.get(includeFile);
				if (fileContent == null) {
					fileContent = Files.readString(includeFile.toPath());
					includesCache.put(includeFile, fileContent);
					fileContent = removePragmaOnce(fileContent);
				}
				else {
					// File was already included, check for pragma once
					fileContent = cutPragmaOnce(fileContent);
				}
				File includeBaseDir = includeFile.getParentFile();
				if (checkCircular(includeStack, includeFile)) {
					throw new RuntimeException("Circular include: " + includeFile);
				}
				includeStack.add(includeFile);
				String includeCode =
					resolveIncludes(fileContent, includeBaseDir, includesCache, includeStack, includePaths);
				includeStack.remove(includeFile);
				code = code.substring(0, matcher.start()) + includeCode + code.substring(matcher.end());
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error resolving includes: " + e.getMessage(), e);
		}
		
		return code;
	}
	
	/**
	 * Checks if there is a circular include in the include stack
	 *
	 * @param includeStack the stack of includes
	 * @param includeFile  the file to be included (should be absolute)
	 * @return true if there is a circular include
	 */
	private boolean checkCircular(@NotNull List<File> includeStack, @NotNull File includeFile) {
		// We can assume a circular include, if the include stack ends in two equal sequences,
		// both starting with the include file
		int size = includeStack.size();
		int index = includeStack.lastIndexOf(includeFile);
		if (index == -1) {
			return false;
		}
		int index2 = index * 2 - size;
		if (index2 < 0 || index2 == index) {
			return false;
		}
		List<File> subList = includeStack.subList(index, size);
		List<File> subList2 = includeStack.subList(index2, index);
		return subList.equals(subList2);
	}
	
	@Nullable
	private File resolveFile(@NotNull File baseDir, @NotNull String path) {
		File file = new File(baseDir, path).getAbsoluteFile();
		if (file.exists()) {
			// File exists, return it
			return file;
		}
		// File does not exist, check if the include may not have the extension
		File parent = file.getParentFile();
		String name = file.getName();
		if (!(name.endsWith(".asm") || name.endsWith(".qasm"))) {
			// .qasm is the preferred extension
			File qasmFile = new File(parent, name + ".qasm");
			if (qasmFile.exists()) {
				return qasmFile;
			}
			File asmFile = new File(parent, name + ".asm");
			if (asmFile.exists()) {
				return asmFile;
			}
		}
		return null;
	}
	
	@NotNull
	private File resolveFile(@NotNull File baseDir, @NotNull String path,
		Collection<File> includePaths) {
		File file = resolveFile(baseDir, path);
		if (file != null) {
			return file;
		}
		for (File includePath : includePaths) {
			file = resolveFile(includePath, path);
			if (file != null) {
				return file;
			}
		}
		throw new RuntimeException("File not found: " + path);
	}
	
	/**
	 * Removes everything after the first <code>@pragma once;</code> preprocessor directive
	 */
	@NotNull
	private String cutPragmaOnce(@NotNull String code) {
		Pattern pragmaOncePattern = Pattern.compile("@pragma\\s+once\\s*;");
		Matcher matcher = pragmaOncePattern.matcher(code);
		if (matcher.find()) {
			return code.substring(0, matcher.start());
		}
		return code;
	}
	
	/**
	 * Removes all <code>@pragma once;</code> preprocessor directives
	 */
	@NotNull
	private String removePragmaOnce(@NotNull String code) {
		return code.replaceAll("@pragma\\s+once\\s*;", "");
	}
	
	/**
	 * Removes all preprocessor directives, that might be left
	 */
	@NotNull
	private String removeDirectives(@NotNull String code) {
		return code.replaceAll("@[^;]+;", "");
	}
	
	/**
	 * Removes all comments
	 */
	@NotNull
	private String removeComments(@NotNull String code) {
		while (true) {
			int commentStart = code.indexOf("//");
			if (commentStart == -1) {
				break;
			}
			int commentEnd = code.indexOf("\n", commentStart);
			if (commentEnd == -1) {
				// No newline found, comment goes to the end of the file
				commentEnd = code.length();
			}
			code = code.substring(0, commentStart) + code.substring(commentEnd);
		}
		while (true) {
			int commentStart = code.indexOf("/*");
			if (commentStart == -1) {
				break;
			}
			int commentEnd = code.indexOf("*/", commentStart);
			if (commentEnd == -1) {
				break;
			}
			code = code.substring(0, commentStart) + code.substring(commentEnd + 2);
		}
		return code;
	}
	
	/**
	 * Minifies the code by removing unnecessary whitespaces, tabs, and newlines
	 */
	@NotNull
	private String minify(@NotNull String code) {
		return code.replaceAll("[\r\n]+", " ")
			.replaceAll("[\t\\s]+", " ")
			.replaceAll("\\s{2,}", " ")
			.trim();
	}
	
	
	@NotNull
	public byte[] assemble(@NotNull String code, long baseAddress,
		@NotNull Collection<File> includePaths) {
		return assemble(code, baseAddress, new File(".").getAbsoluteFile(), includePaths);
	}
	
	@NotNull
	public byte[] assemble(@NotNull String code, long baseAddress, @NotNull File baseDir,
		@NotNull Collection<File> includePaths) {
		List<Instruction> instructions = new ArrayList<>();
		code = resolveIncludes(code, baseDir, includePaths);
		code = removeDirectives(code);
		code = removeComments(code);
		code = minify(code);
		
		// Key: Label name
		// Value: Key: valid until, Value: address
		Map<String, TreeMap<Long, Long>> labels = new HashMap<>();
		{
			long address = 0;
			for (String part_ : code.split(";")) {
				String part = part_.trim();
				// Handle labels
				{
					Matcher labelMatcher =
						Pattern.compile("^\\$(?<label>[a-zA-Z0-9_]+):(?<rest>.*)\\s*$").matcher(part);
					if (labelMatcher.matches()) {
						String label = labelMatcher.group("label");
						TreeMap<Long, Long> map = labels.computeIfAbsent(label, k -> new TreeMap<>());
						if (!map.isEmpty() && map.lastKey() == Long.MAX_VALUE) {
							System.err.println("Duplicate label: " + label);
						}
						map.put(Long.MAX_VALUE, address + baseAddress);
						part = labelMatcher.group("rest").trim();
					}
				}
				
				// Assembler Functions
				// Undefine function (undefine label)
				{
					Matcher undefineMatcher =
						Pattern.compile("^\\.undefine\\s+(?<label>[a-zA-Z0-9_]+)\\s*$").matcher(part);
					if (undefineMatcher.matches()) {
						String label = undefineMatcher.group("label");
						TreeMap<Long, Long> map = labels.get(label);
						if (map == null) {
							System.err.println("Trying to undefine non-existing label: " + label);
						}
						else {
							Map.Entry<Long, Long> last = map.lastEntry();
							if (last.getKey() == Long.MAX_VALUE) {
								map.remove(last.getKey());
								map.put(address + baseAddress, last.getValue());
							}
							else {
								System.err.println("Trying to undefine already undefined label: " + label);
							}
						}
						continue;
					}
				}
				// Data function
				{
					Matcher dataMatcher = Pattern.compile("^\\.data\\s+(?<data>.+)\\s*$").matcher(part);
					if (dataMatcher.matches()) {
						String data = dataMatcher.group("data");
						String[] parts = data.split(" ");
						byte[] bytes = new byte[parts.length];
						for (int i = 0; i < parts.length; i++) {
							bytes[i] = (byte) Integer.parseInt(parts[i], 16);
						}
						instructions.add(new PlaceholderInstruction(bytes));
						address += bytes.length;
						continue;
					}
				}
				// ASCII function
				{
					Matcher asciiMatcher =
						Pattern.compile("^\\.ascii\\s+\"(?<ascii>.+)\"\\s*$").matcher(part);
					if (asciiMatcher.matches()) {
						String ascii = asciiMatcher.group("ascii");
						// +1 for null terminator
						byte[] data = new byte[ascii.length() + 1];
						for (int i = 0; i < ascii.length(); i++) {
							data[i] = (byte) ascii.charAt(i);
						}
						data[ascii.length()] = 0;
						instructions.add(new PlaceholderInstruction(data));
						address += data.length;
						continue;
					}
				}
				// Space function
				{
					Matcher spaceMatcher =
						Pattern.compile("^\\.space\\s+(?<size>" + NumberUtils.NUMBER_PATTERN + ")\\s*$")
							.matcher(part);
					if (spaceMatcher.matches()) {
						int size = (int) NumberUtils.parseNumber(spaceMatcher.group("size"));
						byte[] data = new byte[size];
						// Should be zeroed
						instructions.add(new PlaceholderInstruction(data));
						address += data.length;
						continue;
					}
				}
				// Align function
				{
					Matcher alignMatcher =
						Pattern.compile("^\\.align\\s+(?<align>" + NumberUtils.NUMBER_PATTERN + ")\\s*$")
							.matcher(part);
					if (alignMatcher.matches()) {
						int align = (int) NumberUtils.parseNumber(alignMatcher.group("align"));
						int offset = (int) (address % align);
						if (offset != 0) {
							byte[] data = new byte[align - offset];
							// Should be zeroed
							instructions.add(new PlaceholderInstruction(data));
							address += data.length;
						}
						continue;
					}
				}
				// Org function (set address)
				{
					Matcher orgMatcher = Pattern.compile(
							"^\\.org\\s+(?<relative>~)?(?<address>" + NumberUtils.NUMBER_PATTERN + ")\\s*$")
						.matcher(part);
					if (orgMatcher.matches()) {
						long newAddress = NumberUtils.parseNumber(orgMatcher.group("address"));
						if (orgMatcher.group("relative") != null) {
							newAddress += address;
						}
						if (newAddress < address) {
							System.err.println("Cannot set address to lower value: " + newAddress);
						}
						else {
							address = newAddress;
						}
						continue;
					}
				}
				// Fill function
				{
					Matcher fillMatcher = Pattern.compile(
						"^\\.fill\\s+(?<size>" + NumberUtils.NUMBER_PATTERN + ")\\s+(?<value>" +
							NumberUtils.NUMBER_PATTERN + ")\\s*$").matcher(part);
					if (fillMatcher.matches()) {
						int size = (int) NumberUtils.parseNumber(fillMatcher.group("size"));
						byte value = (byte) NumberUtils.parseNumber(fillMatcher.group("value"));
						byte[] data = new byte[size];
						Arrays.fill(data, value);
						instructions.add(new PlaceholderInstruction(data));
						address += data.length;
						continue;
					}
				}
				// Repeat function
				{
					Matcher repeatMatcher = Pattern.compile(
							"^\\.repeat\\s+(?<count>" + NumberUtils.NUMBER_PATTERN + ")\\s+(?<code>.+)\\s*$")
						.matcher(part);
					if (repeatMatcher.matches()) {
						int count = (int) NumberUtils.parseNumber(repeatMatcher.group("count"));
						String code1 = repeatMatcher.group("code");
						Instruction instruction = parseInstruction(code1);
						if (instruction == null) {
							System.err.println("Invalid instruction in repeat: " + code1);
						}
						else {
							for (int i = 0; i < count; i++) {
								instructions.add(instruction);
							}
							address += (long) instruction.getLength() * count;
						}
						continue;
					}
				}
				
				// Instructions
				Instruction instruction = parseInstruction(part);
				if (instruction != null) {
					address += instruction.getLength();
					instructions.add(instruction);
				}
			}
		}
		
		List<Instruction> resolved = new ArrayList<>();
		{
			long address = 0;
			for (Instruction instruction : instructions) {
				long finalAddress = address;
				resolved.add(instruction.resolved(label -> {
					TreeMap<Long, Long> map = labels.get(label);
					if (map == null) {
						System.err.println("Undefined label: " + label);
						return 0L;
					}
					// Find smallest key that is larger than the current address
					Map.Entry<Long, Long> entry = map.ceilingEntry(finalAddress + baseAddress);
					if (entry == null) {
						System.err.println("Label out of scope: " + label);
						return 0L;
					}
					return entry.getValue();
				}));
				address += resolved.getLast().getLength();
			}
		}
		
		return assemble(resolved);
	}
	
	@NotNull
	private byte[] assemble(@NotNull List<Instruction> instructions) {
		ByteBuf buffer = Unpooled.buffer();
		assemble(instructions, buffer);
		byte[] bytes = new byte[buffer.readableBytes()];
		buffer.readBytes(bytes);
		buffer.release();
		return bytes;
	}
	
	private void assemble(@NotNull List<Instruction> instructions, @NotNull ByteBuf buffer) {
		for (Instruction instruction : instructions) {
			instruction.assemble(buffer);
		}
	}
	
	@Nullable
	public Instruction parseInstruction(@NotNull String code) {
		String[] parts = code.trim().split(" ", 2);
		String name = parts[0];
		InstructionDef def = InstructionDef.byName(name);
		if (def == null) {
			return null;
		}
		return def.parse(parts.length == 1 ? "" : parts[1]);
	}
	
	public static class PlaceholderInstruction extends Instruction {
		
		@NotNull
		private final byte[] data;
		
		public PlaceholderInstruction(@NotNull byte[] data) {
			super(InstructionDef.NOP);
			this.data = data;
		}
		
		@Override
		public void execute(@NotNull InstructionContext context) {
			throw new UnsupportedOperationException("Placeholder instruction");
		}
		
		@Override
		protected int getPayloadLength() {
			return data.length;
		}
		
		@Override
		public int getLength() {
			return data.length;
		}
		
		@Override
		protected void assemblePayload(@NotNull ByteBuf buf) {
			buf.writeBytes(data);
		}
		
		@Override
		public void assemble(@NotNull ByteBuf buf) {
			assemblePayload(buf);
		}
		
		@Override
		protected String getAssemblyPayload() {
			return null;
		}
		
	}
	
}
