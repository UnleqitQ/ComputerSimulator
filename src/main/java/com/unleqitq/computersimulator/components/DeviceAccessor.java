package com.unleqitq.computersimulator.components;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DeviceAccessor {
	
	@NotNull
	private final Map<Long, IDevice> devices;
	
	public DeviceAccessor() {
		this.devices = new HashMap<>();
	}
	
	public void addDevice(long port, @NotNull IDevice device) {
		if (this.hasDevice(port)) {
			if (this.getDevice(port) == device) {
				return;
			}
			throw new IllegalArgumentException("Port " + port + " is already in use");
		}
		if (this.devices.containsValue(device)) {
			System.err.printf("Device %s is already bound to a port%n", device.getDeviceName());
			System.err.printf("Trying to bind to %x, already bound to %x%n",
				port,
				this.devices.entrySet()
					.stream()
					.filter(entry -> entry.getValue() == device)
					.findFirst()
					.orElseThrow()
					.getKey());
		}
		this.devices.put(port, device);
		device.setBoundPort(port);
	}
	
	@Nullable
	public IDevice getDevice(long port) {
		return this.devices.get(port);
	}
	
	@NotNull
	public Map<Long, IDevice> getDevices() {
		return Collections.unmodifiableMap(this.devices);
	}
	
	public boolean removeDevice(long port) {
		if (!this.hasDevice(port)) {
			return false;
		}
		IDevice device = this.devices.remove(port);
		device.setUnbound();
		return true;
	}
	
	public boolean hasDevice(long port) {
		return this.devices.containsKey(port);
	}
	
	public boolean reroute(long from, long to) {
		if (from == to) {
			// No need to reroute, but it's not an error
			return true;
		}
		
		if (!this.hasDevice(from)) {
			return false;
		}
		
		if (this.hasDevice(to)) {
			return false;
		}
		
		IDevice device = devices.remove(from);
		devices.put(to, device);
		device.setBoundPort(to);
		
		return true;
	}
	
	public interface IDevice {
		
		@NotNull
		String getDeviceName();
		
		long read(long address);
		
		void write(long address, long data);
		
		default void setBoundPort(long port) {
			// Do nothing
		}
		
		default void setUnbound() {
			// Do nothing
		}
		
	}
	
	@Getter
	public static abstract class AbstractDevice implements IDevice {
		
		@NotNull
		private final String deviceName;
		
		private boolean bound;
		private long boundPort;
		
		public AbstractDevice(@NotNull String deviceName) {
			this.deviceName = deviceName;
			this.bound = false;
			this.boundPort = -1;
		}
		
		@Override
		public void setBoundPort(long port) {
			this.bound = true;
			this.boundPort = port;
		}
		
		@Override
		public void setUnbound() {
			this.bound = false;
			this.boundPort = -1;
		}
		
		@Override
		public long read(long address) {
			return 0;
		}
		
		@Override
		public void write(long address, long data) {
			// Do nothing
		}
		
	}
	
}
