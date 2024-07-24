package com.unleqitq.computersimulator.components;

import lombok.Getter;

import java.util.Arrays;

@Getter
public class Memory {
	
	/**
	 * The size of the memory in bytes
	 */
	private final int size;
	
	/**
	 * The memory data
	 */
	private final byte[] data;
	
	/**
	 * Creates a new memory with the given size
	 *
	 * @param size The size of the memory in bytes
	 */
	public Memory(int size) {
		this.size = size;
		this.data = new byte[size];
	}
	
	/**
	 * Converts an address (qword) to an index (dword) in the memory
	 *
	 * @param address The address to convert
	 * @return The index in the memory
	 */
	private static int addressToIndex(long address) {
		return (int) (address & 0xFFFFFFFFL);
	}
	
	/**
	 * Converts an address (qword) together with a segment (qword) to an index (dword) in the memory
	 *
	 * @param address The address to convert
	 * @param segment The segment to convert
	 * @return The index in the memory
	 */
	private static int addressToIndex(long address, long segment) {
		return (int) ((address + (segment << 4)) & 0xFFFFFFFFL);
	}
	
	public long readByte(long address) {
		return getByte(addressToIndex(address));
	}
	
	public long readByte(long address, long segment) {
		return getByte(addressToIndex(address, segment));
	}
	
	public void writeByte(long address, long value) {
		setByte(addressToIndex(address), value);
	}
	
	public void writeByte(long address, long segment, long value) {
		setByte(addressToIndex(address, segment), value);
	}
	
	public long readWord(long address) {
		return getWord(addressToIndex(address));
	}
	
	public long readWord(long address, long segment) {
		return getWord(addressToIndex(address, segment));
	}
	
	public void writeWord(long address, long value) {
		setWord(addressToIndex(address), value);
	}
	
	public void writeWord(long address, long segment, long value) {
		setWord(addressToIndex(address, segment), value);
	}
	
	public long readDword(long address) {
		return getDword(addressToIndex(address));
	}
	
	public long readDword(long address, long segment) {
		return getDword(addressToIndex(address, segment));
	}
	
	public void writeDword(long address, long value) {
		setDword(addressToIndex(address), value);
	}
	
	public void writeDword(long address, long segment, long value) {
		setDword(addressToIndex(address, segment), value);
	}
	
	public long readQword(long address) {
		return getQword(addressToIndex(address));
	}
	
	public long readQword(long address, long segment) {
		return getQword(addressToIndex(address, segment));
	}
	
	public void writeQword(long address, long value) {
		setQword(addressToIndex(address), value);
	}
	
	public void writeQword(long address, long segment, long value) {
		setQword(addressToIndex(address, segment), value);
	}
	
	private long getByte(int index) {
		return data[index] & 0xFFL;
	}
	
	private void setByte(int index, long value) {
		data[index] = (byte) value;
	}
	
	private long getWord(int index) {
		return (data[index] & 0xFFL) | ((data[index + 1] & 0xFFL) << 8);
	}
	
	private void setWord(int index, long value) {
		data[index] = (byte) (value & 0xFFL);
		data[index + 1] = (byte) ((value >> 8) & 0xFFL);
	}
	
	private long getDword(int index) {
		return (data[index] & 0xFFL) | ((data[index + 1] & 0xFFL) << 8) |
			((data[index + 2] & 0xFFL) << 16) | ((data[index + 3] & 0xFFL) << 24);
	}
	
	private void setDword(int index, long value) {
		data[index] = (byte) (value & 0xFFL);
		data[index + 1] = (byte) ((value >> 8) & 0xFFL);
		data[index + 2] = (byte) ((value >> 16) & 0xFFL);
		data[index + 3] = (byte) ((value >> 24) & 0xFFL);
	}
	
	private long getQword(int index) {
		return (data[index] & 0xFFL) | ((data[index + 1] & 0xFFL) << 8) |
			((data[index + 2] & 0xFFL) << 16) | ((data[index + 3] & 0xFFL) << 24) |
			((data[index + 4] & 0xFFL) << 32) | ((data[index + 5] & 0xFFL) << 40) |
			((data[index + 6] & 0xFFL) << 48) | ((data[index + 7] & 0xFFL) << 56);
	}
	
	private void setQword(int index, long value) {
		data[index] = (byte) (value & 0xFFL);
		data[index + 1] = (byte) ((value >> 8) & 0xFFL);
		data[index + 2] = (byte) ((value >> 16) & 0xFFL);
		data[index + 3] = (byte) ((value >> 24) & 0xFFL);
		data[index + 4] = (byte) ((value >> 32) & 0xFFL);
		data[index + 5] = (byte) ((value >> 40) & 0xFFL);
		data[index + 6] = (byte) ((value >> 48) & 0xFFL);
		data[index + 7] = (byte) ((value >> 56) & 0xFFL);
	}
	
	public void write(long address, long segment, byte[] data) {
		int index = addressToIndex(address, segment);
		System.arraycopy(data, 0, this.data, index, Math.min(data.length, this.data.length - index));
	}
	
	public byte[] read(long address, long segment, int length) {
		int index = addressToIndex(address, segment);
		byte[] data = new byte[length];
		System.arraycopy(this.data, index, data, 0, Math.min(length, this.data.length - index));
		return data;
	}
	
	public byte[] read(long address, int length) {
		int index = addressToIndex(address);
		byte[] data = new byte[length];
		System.arraycopy(this.data, index, data, 0, Math.min(length, this.data.length - index));
		return data;
	}
	
	public void clear() {
		Arrays.fill(data, (byte) 0);
	}
	
}
