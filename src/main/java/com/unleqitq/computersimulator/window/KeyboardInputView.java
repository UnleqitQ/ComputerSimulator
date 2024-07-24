package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerWindow;
import com.unleqitq.computersimulator.components.DeviceAccessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * When this view is focused, the keyboard input is recorded and sent to the computer.
 */
public final class KeyboardInputView extends DeviceAccessor.AbstractDevice implements KeyListener {
	
	public static final long DEFAULT_PORT = 0x8000L;
	
	@NotNull
	private final ComputerWindow computerWindow;
	@NotNull
	public final JInternalFrame internalFrame;
	
	@NotNull
	public final JToolBar toolBar;
	
	@NotNull
	public final JButton clearButton;
	
	@NotNull
	public final JButton infoButton;
	
	@NotNull
	public final JPanel rootPanel;
	
	@NotNull
	public final JScrollPane scrollPane;
	
	@NotNull
	public final JPanel panel;
	
	@NotNull
	public final JLabel scanCodeLabel;
	
	@NotNull
	public final JLabel nameLabel;
	
	@NotNull
	public final JLabel actionLabel;
	
	@NotNull
	public final ConcurrentLinkedQueue<KeyboardInput> inputBuffer;
	
	public KeyboardInputView(@NotNull ComputerWindow computerWindow) {
		super("Keyboard");
		this.computerWindow = computerWindow;
		inputBuffer = new ConcurrentLinkedQueue<>();
		
		internalFrame = new JInternalFrame("Keyboard input");
		internalFrame.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		internalFrame.setSize(500, 150);
		internalFrame.setLocation(700, 550);
		internalFrame.setMaximizable(true);
		internalFrame.setIconifiable(true);
		internalFrame.setResizable(true);
		internalFrame.setClosable(true);
		internalFrame.show();
		
		rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());
		
		toolBar = new JToolBar();
		toolBar.setFloatable(true);
		clearButton = new JButton("Clear buffer");
		infoButton = new JButton("Info");
		toolBar.add(clearButton);
		toolBar.add(infoButton);
		clearButton.addActionListener(e -> {
			inputBuffer.clear();
			update();
		});
		infoButton.addActionListener(e -> {
			String message = """
				Default Port: 0x%X,
				Bound Port: %s,
				Input actions:
				- Get: 0x%X
				- Size: 0x%X
				Output actions:
				- Clear: 0x%X
				""".formatted(DEFAULT_PORT, isBound() ? "0x%X".formatted(getBoundPort()) : "None",
				InputAction.GET.getAddress(), InputAction.SIZE.getAddress(),
				OutputAction.CLEAR.getAddress());
			JOptionPane.showMessageDialog(computerWindow.frame, message, "Keyboard info",
				JOptionPane.INFORMATION_MESSAGE);
		});
		rootPanel.add(toolBar, BorderLayout.NORTH);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		scrollPane = new JScrollPane(panel);
		rootPanel.add(scrollPane, BorderLayout.CENTER);
		
		scanCodeLabel = new JLabel();
		nameLabel = new JLabel();
		actionLabel = new JLabel();
		
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
		scanCodeLabel.setFont(font);
		nameLabel.setFont(font);
		actionLabel.setFont(font);
		
		panel.add(scanCodeLabel);
		panel.add(nameLabel);
		panel.add(actionLabel);
		
		internalFrame.setContentPane(rootPanel);
		
		
		internalFrame.addKeyListener(this);
		
		internalFrame.setFocusable(true);
		
		computerWindow.computer.devices().addDevice(DEFAULT_PORT, this);
	}
	
	public void update() {
		StringBuilder scanCodeText = new StringBuilder("Scan code buffer: ");
		StringBuilder nameText = new StringBuilder("Name buffer:      ");
		StringBuilder actionText = new StringBuilder("Action buffer:    ");
		for (KeyboardInput input : inputBuffer) {
			String scanCode = Integer.toString(input.scanCode);
			String name = KeyEvent.getKeyText(input.scanCode);
			String action = input.pressed ? "DOWN" : "UP";
			int pad = Math.max(Math.max(scanCode.length(), name.length()), action.length());
			scanCodeText.append(String.format("%" + pad + "s", scanCode)).append(" ");
			nameText.append(String.format("%" + pad + "s", name)).append(" ");
			actionText.append(String.format("%" + pad + "s", action)).append(" ");
		}
		scanCodeLabel.setText(scanCodeText.toString());
		nameLabel.setText(nameText.toString());
		actionLabel.setText(actionText.toString());
	}
	
	@Override
	public long read(long address) {
		InputAction action = InputAction.of(address);
		if (action == null) {
			return 0;
		}
		return switch (action) {
			case GET -> {
				KeyboardInput input = inputBuffer.poll();
				if (input == null) {
					yield -1;
				}
				yield Integer.toUnsignedLong(input.scanCode) | (input.pressed ? 1L << 31 : 0);
			}
			case SIZE -> Integer.toUnsignedLong(inputBuffer.size());
		};
	}
	
	@Override
	public void write(long address, long data) {
		OutputAction action = OutputAction.of(address);
		if (action == null) {
			return;
		}
		//noinspection SwitchStatementWithTooFewBranches
		switch (action) {
			case CLEAR -> inputBuffer.clear();
		}
	}
	
	@Override
	public void keyTyped(@NotNull KeyEvent e) {}
	
	@Override
	public void keyPressed(@NotNull KeyEvent e) {
		inputBuffer.add(new KeyboardInput(e.getKeyCode(), true));
		update();
	}
	
	@Override
	public void keyReleased(@NotNull KeyEvent e) {
		inputBuffer.add(new KeyboardInput(e.getKeyCode(), false));
		update();
	}
	
	@Getter
	public enum InputAction {
		
		GET(0x10),
		SIZE(0x11),
		;
		
		public final long address;
		
		@NotNull
		public static final Map<Long, InputAction> map = Arrays.stream(values())
			.collect(Collectors.toMap(InputAction::getAddress, Function.identity()));
		
		InputAction(long address) {
			this.address = address;
		}
		
		@Nullable
		public static InputAction of(long address) {
			return map.get(address);
		}
		
	}
	
	@Getter
	public enum OutputAction {
		
		CLEAR(0x10),
		;
		
		public final long address;
		
		@NotNull
		public static final Map<Long, OutputAction> map = Arrays.stream(values())
			.collect(Collectors.toMap(OutputAction::getAddress, Function.identity()));
		
		OutputAction(long address) {
			this.address = address;
		}
		
		@Nullable
		public static OutputAction of(long address) {
			return map.get(address);
		}
		
	}
	
	public record KeyboardInput(int scanCode, boolean pressed) {}
	
}
