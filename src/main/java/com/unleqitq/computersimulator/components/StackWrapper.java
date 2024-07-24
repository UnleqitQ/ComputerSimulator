package com.unleqitq.computersimulator.components;

import com.unleqitq.computersimulator.ValueSize;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class StackWrapper {
	
	@NotNull
	private final Memory memory;
	@NotNull
	private final Registers registers;
	
	/**
	 * This is only used for debugging purposes and has no effect on the actual stack
	 */
	@Accessors (fluent = true)
	@Getter
	@NotNull
	private final StackHistory history = new StackHistory();
	
	public StackWrapper(@NotNull Memory memory, @NotNull Registers registers) {
		this.memory = memory;
		this.registers = registers;
	}
	
	public long getStackPointer() {
		return registers.readRegister(Registers.Register.RSP);
	}
	
	public void setStackPointer(long value) {
		registers.writeRegister(Registers.Register.RSP, value);
	}
	
	public long getBasePointer() {
		return registers.readRegister(Registers.Register.RBP);
	}
	
	public void setBasePointer(long value) {
		registers.writeRegister(Registers.Register.RBP, value);
	}
	
	public long getStackSegment() {
		return registers.readRegister(Registers.Register.SS);
	}
	
	public void setStackSegment(long value) {
		registers.writeRegister(Registers.Register.SS, value);
	}
	
	public void pushByte(long value) {
		long nsp = getStackPointer() - 1;
		setStackPointer(nsp);
		memory.writeByte(nsp, getStackSegment(), value);
		history.push(value, nsp, ValueSize.BYTE);
	}
	
	public void pushWord(long value) {
		long nsp = getStackPointer() - 2;
		setStackPointer(nsp);
		memory.writeWord(nsp, getStackSegment(), value);
		history.push(value, nsp, ValueSize.WORD);
	}
	
	public void pushDword(long value) {
		long nsp = getStackPointer() - 4;
		setStackPointer(nsp);
		memory.writeDword(nsp, getStackSegment(), value);
		history.push(value, nsp, ValueSize.DWORD);
	}
	
	public void pushQword(long value) {
		long nsp = getStackPointer() - 8;
		setStackPointer(nsp);
		memory.writeQword(nsp, getStackSegment(), value);
		history.push(value, nsp, ValueSize.QWORD);
	}
	
	public long popByte() {
		long sp = getStackPointer();
		long value = memory.readByte(sp, getStackSegment());
		setStackPointer(sp + 1);
		return value;
	}
	
	public long popWord() {
		long sp = getStackPointer();
		history.pop(sp);
		long value = memory.readWord(sp, getStackSegment());
		setStackPointer(sp + 2);
		return value;
	}
	
	public long popDword() {
		long sp = getStackPointer();
		history.pop(sp);
		long value = memory.readDword(sp, getStackSegment());
		setStackPointer(sp + 4);
		return value;
	}
	
	public long popQword() {
		long sp = getStackPointer();
		history.pop(sp);
		long value = memory.readQword(sp, getStackSegment());
		setStackPointer(sp + 8);
		return value;
	}
	
	public void dropBytes(long count) {
		long newStackPointer = getStackPointer() + count;
		setStackPointer(newStackPointer);
		history.dropBefore(newStackPointer);
	}
	
	public void push(long value, @NotNull ValueSize size) {
		switch (size) {
			case BYTE -> pushByte(value);
			case WORD -> pushWord(value);
			case DWORD -> pushDword(value);
			case QWORD -> pushQword(value);
		}
	}
	
	public long pop(@NotNull ValueSize size) {
		return switch (size) {
			case BYTE -> popByte();
			case WORD -> popWord();
			case DWORD -> popDword();
			case QWORD -> popQword();
		};
	}
	
	public void resetHistory() {
		history.history.clear();
	}
	
	@NotNull
	public List<ResolvedStackEntry> resolveStack() {
		long sp = getStackPointer();
		long end = memory.getSize();
		List<ResolvedStackEntry> entries = new LinkedList<>();
		
		List<StackHistory.StackEntry> history = new LinkedList<>(this.history.history);
		while (sp < end - 1) {
			StackHistory.StackEntry nextEntry = history.isEmpty() ? null : history.getLast();
			if (nextEntry != null && nextEntry.address() < sp) {
				continue;
			}
			if (nextEntry != null && nextEntry.address() == sp) {
				history.removeLast();
				entries.add(new ResolvedStackEntry(sp, sp + nextEntry.size().getSize() - 1, nextEntry));
				sp += nextEntry.size().getSize();
			}
			else {
				long nextAddress = nextEntry == null ? end : nextEntry.address();
				entries.add(new ResolvedStackEntry(sp, nextAddress - 1, null));
				sp = nextAddress;
			}
		}
		
		return entries;
	}
	
	/**
	 * Represents an entry in the stack
	 * <p>
	 * If entries can be matched with the stack history,
	 * the entry will contain the stack history entry that was matched<br>
	 * If there is space between two matched entries, it will be assumed to be frame data
	 * and will be added as a separate entry with a null stack history entry
	 * </p>
	 *
	 * @param fromAddress The start address of the entry
	 * @param toAddress   The end address of the entry
	 * @param entry       The stack history entry of the entry (if it could be matched)
	 */
	public record ResolvedStackEntry(
		long fromAddress, long toAddress, @Nullable StackHistory.StackEntry entry
	) {}
	
	@Getter
	public static class StackHistory {
		
		@NotNull
		private final WrappedLinkedList<StackEntry> history;
		
		public StackHistory() {
			history = new WrappedLinkedList<>();
		}
		
		public void push(long value, long address, @NotNull ValueSize size) {
			history.add(new StackEntry(value, address, size));
		}
		
		public void pop(long address) {
			if (history.isEmpty() || history.getLast().address() != address) {
				System.err.println("Stack may be corrupted");
			}
			
			// Pop until the new last entry's address is bigger than the current address
			while (!history.isEmpty() && history.getLast().address() <= address) {
				history.removeLast();
			}
		}
		
		public void dropBefore(long address) {
			// Pop until the new last entry's address is bigger or equal to the current address
			while (!history.isEmpty() && history.getLast().address() < address) {
				history.removeLast();
			}
		}
		
		public record StackEntry(long value, long address, @NotNull ValueSize size) {}
		
	}
	
	public static class WrappedLinkedList<E> extends LinkedList<E> {
		
		public int getModCount() {
			return modCount;
		}
		
	}
	
}
