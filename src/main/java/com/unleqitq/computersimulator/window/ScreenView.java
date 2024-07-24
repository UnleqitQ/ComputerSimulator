package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerWindow;
import com.unleqitq.computersimulator.components.DeviceAccessor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ScreenView {
	
	public int width = 20;
	public int height = 20;
	
	@NotNull
	private final ComputerWindow computerWindow;
	@NotNull
	public final JInternalFrame internalFrame;
	
	@NotNull
	public final SettingsBar settingsBar;
	
	@NotNull
	public final JPanel panel;
	
	@NotNull
	public final Canvas canvas;
	
	@NotNull
	public BufferedImage image;
	
	@NotNull
	public final ScreenDevice device;
	
	@SneakyThrows
	public ScreenView(@NotNull ComputerWindow computerWindow) {
		this.computerWindow = computerWindow;
		
		device = new ScreenDevice();
		computerWindow.computer.devices().addDevice(ScreenDevice.DEFAULT_PORT, device);
		
		internalFrame = new JInternalFrame("Screen");
		internalFrame.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		final int padH = 12;
		final int padV = 66;
		internalFrame.setSize(250 + padH, 250 + padV);
		internalFrame.setLocation(400, 0);
		internalFrame.setMaximizable(true);
		internalFrame.setIconifiable(true);
		internalFrame.setResizable(true);
		internalFrame.setClosable(true);
		internalFrame.show();
		
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		settingsBar = new SettingsBar();
		panel.add(settingsBar.toolBar, BorderLayout.NORTH);
		
		canvas = new Canvas() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
			}
		};
		
		panel.add(canvas, BorderLayout.CENTER);
		
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		internalFrame.setContentPane(panel);
	}
	
	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		BufferedImage oldImage = image;
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.createGraphics();
		g.drawImage(oldImage, 0, 0, null);
		g.dispose();
		oldImage.flush();
	}
	
	public void update() {
		Graphics graphics = canvas.getGraphics();
		if (graphics != null) {
			graphics.drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight(), null);
		}
	}
	
	public final class SettingsBar {
		
		@NotNull
		public final JToolBar toolBar;
		
		@NotNull
		public final JButton infoButton;
		@NotNull
		public final JButton clearButton;
		
		@NotNull
		public final JLabel sizeLabel;
		@NotNull
		public final JTextField widthField;
		@NotNull
		public final JLabel xLabel;
		@NotNull
		public final JTextField heightField;
		
		@NotNull
		public final JButton resizeButton;
		
		public SettingsBar() {
			toolBar = new JToolBar();
			toolBar.setFloatable(true);
			
			infoButton = new JButton("Info");
			clearButton = new JButton("Clear");
			sizeLabel = new JLabel("Size:");
			widthField = new JTextField(String.valueOf(width), 4);
			xLabel = new JLabel("x");
			heightField = new JTextField(String.valueOf(height), 4);
			resizeButton = new JButton("Resize");
			
			toolBar.add(infoButton);
			toolBar.add(clearButton);
			toolBar.addSeparator();
			toolBar.add(sizeLabel);
			toolBar.add(widthField);
			toolBar.add(xLabel);
			toolBar.add(heightField);
			toolBar.add(resizeButton);
			
			infoButton.addActionListener(e -> {
				String info = """
					Screen Info:
					- Default Port: 0x%X
					- Bound Port: %s
					- Width: %d
					- Height: %d
					""".formatted(ScreenDevice.DEFAULT_PORT,
					device.isBound() ? "0x%X".formatted(device.getBoundPort()) : "None", width, height);
				
				String inputActions = """
					Input actions:
					- Get Size: 0x%X
					""".formatted(ScreenDevice.InputAction.SIZE.getAddress());
				
				String outputActions = """
					Output actions:
					- Draw: 0x%X
					- Clear: 0x%X
					""".formatted(ScreenDevice.OutputAction.DRAW.getAddress(),
					ScreenDevice.OutputAction.CLEAR.getAddress());
				
				String message = "%s%n%n%s%n%n%s".formatted(info, inputActions, outputActions);
				JOptionPane.showMessageDialog(null, message, "Screen Info",
					JOptionPane.INFORMATION_MESSAGE);
			});
			
			clearButton.addActionListener(e -> {
				Graphics g = image.createGraphics();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, width, height);
				g.dispose();
				update();
			});
			
			resizeButton.addActionListener(e -> {
				try {
					int newWidth = Integer.parseInt(widthField.getText());
					int newHeight = Integer.parseInt(heightField.getText());
					if (newWidth <= 0 || newHeight <= 0) {
						JOptionPane.showMessageDialog(null, "Width and Height must be greater than 0", "Error",
							JOptionPane.ERROR_MESSAGE);
						return;
					}
					resize(newWidth, newHeight);
					update();
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(null, "Width and Height must be integers", "Error",
						JOptionPane.ERROR_MESSAGE);
				}
			});
		}
		
	}
	
	public final class ScreenDevice extends DeviceAccessor.AbstractDevice {
		
		public static final long DEFAULT_PORT = 0x8020;
		
		public ScreenDevice() {
			super("SCREEN");
		}
		
		@Override
		public void write(long address, long data) {
			OutputAction action = OutputAction.of(address);
			if (action == null) {
				return;
			}
			switch (action) {
				case DRAW -> {
					int x = (int) (data & 0xFFFFL);
					int y = (int) ((data >>> 16) & 0xFFFFL);
					if (x < image.getWidth() && y < image.getHeight()) {
						int color = (int) ((data >>> 32) & 0xFFFFFFL);
						image.setRGB(x, y, color);
					}
				}
				case CLEAR -> {
					Graphics g = image.createGraphics();
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, width, height);
					g.dispose();
				}
			}
		}
		
		@Override
		public long read(long address) {
			InputAction action = InputAction.of(address);
			if (action == null) {
				return 0;
			}
			return switch (action) {
				case SIZE -> Integer.toUnsignedLong(width) | (Integer.toUnsignedLong(height) << 16);
			};
		}
		
		@Getter
		enum OutputAction {
			DRAW(0x0L),
			CLEAR(0x1L),
			;
			
			private final long address;
			
			@NotNull
			private final static Map<Long, OutputAction> map = Arrays.stream(values())
				.collect(Collectors.toMap(OutputAction::getAddress, Function.identity()));
			
			OutputAction(long address) {
				this.address = address;
			}
			
			@Nullable
			public static OutputAction of(long address) {
				return map.get(address);
			}
		}
		
		@Getter
		public enum InputAction {
			SIZE(0x0L),
			;
			
			private final long address;
			
			@NotNull
			private final static Map<Long, InputAction> map = Arrays.stream(values())
				.collect(Collectors.toMap(InputAction::getAddress, Function.identity()));
			
			InputAction(long address) {
				this.address = address;
			}
			
			@Nullable
			public static InputAction of(long address) {
				return map.get(address);
			}
		}
		
	}
	
}
