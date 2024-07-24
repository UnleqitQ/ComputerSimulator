package com.unleqitq.computersimulator;

import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import com.unleqitq.computersimulator.instruction.InstructionAssembler;
import com.unleqitq.computersimulator.window.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;

public class ComputerWindow {
	
	@NotNull
	public final JFrame frame;
	
	@NotNull
	public final JPanel rootPanel;
	
	@NotNull
	public final JDesktopPane desktopPane;
	
	@NotNull
	public final ComputerSimulator computer;
	
	@NotNull
	public final ControlMenuBar controlMenuBar;
	
	@NotNull
	public final ControlsBar controlsBar;
	
	@NotNull
	public final VisualSettings visualSettings;
	
	@NotNull
	public final RegistersView registersView;
	@NotNull
	public final InstructionsView instructionsView;
	@NotNull
	public final MemoryView memoryView;
	@NotNull
	public final ScreenView screenView;
	@NotNull
	public final ProgramView programView;
	@NotNull
	public final InfoView infoView;
	@NotNull
	public final StackView stackView;
	@NotNull
	public final KeyboardInputView keyboardInputView;
	
	public ComputerWindow(@NotNull ComputerSimulator computer) {
		this.computer = computer;
		this.visualSettings = new VisualSettings();
		
		frame = new JFrame("Computer Simulator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		
		rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());
		
		desktopPane = new JDesktopPane();
		
		controlsBar = new ControlsBar(this);
		registersView = new RegistersView(this);
		instructionsView = new InstructionsView(this);
		screenView = new ScreenView(this);
		memoryView = new MemoryView(this);
		programView = new ProgramView(this);
		infoView = new InfoView(this);
		stackView = new StackView(this);
		keyboardInputView = new KeyboardInputView(this);
		
		rootPanel.add(controlsBar.toolBar, BorderLayout.NORTH);
		rootPanel.add(desktopPane, BorderLayout.CENTER);
		
		controlMenuBar = new ControlMenuBar(this);
		frame.setJMenuBar(controlMenuBar.menuBar);
		
		desktopPane.add(registersView.internalFrame);
		desktopPane.add(instructionsView.internalFrame);
		desktopPane.add(screenView.internalFrame);
		desktopPane.add(memoryView.internalFrame);
		desktopPane.add(programView.internalFrame);
		desktopPane.add(infoView.internalFrame);
		desktopPane.add(stackView.internalFrame);
		desktopPane.add(keyboardInputView.internalFrame);
		
		{
			programView.internalFrame.hide();
			keyboardInputView.internalFrame.hide();
		}
		
		frame.setContentPane(rootPanel);
		frame.setVisible(true);
		
		update();
		
		computer.addInterruptionListener((ignore, code) -> {
			// Interrupt code 3 is a breakpoint
			final byte BREAKPOINT = (byte) 3;
			if (code == BREAKPOINT) {
				controlsBar.running = false;
				return true;
			}
			return false;
		});
	}
	
	public void update() {
		update(false);
	}
	
	public void update(boolean all) {
		controlsBar.update();
		registersView.update();
		if (all) instructionsView.reload();
		else instructionsView.update();
		screenView.update();
		if (all) memoryView.reload();
		else memoryView.update();
		infoView.update();
		stackView.update();
		keyboardInputView.update();
	}
	
	public static class VisualSettings {
		
		@NotNull
		public ValueView valueView = ValueView.HEXADECIMAL;
		
	}
	
	public enum ValueView {
		DECIMAL,
		DECIMAL_SIGNED,
		HEXADECIMAL,
		BINARY,
		;
		
		public String format(@NotNull ValueSize size, long value) {
			return format(size, value, false);
		}
		
		public String format(@NotNull ValueSize size, long value, boolean fill) {
			return switch (this) {
				case DECIMAL -> Long.toUnsignedString(value);
				case DECIMAL_SIGNED -> switch (size) {
					case BYTE -> Byte.toString((byte) value);
					case WORD -> Short.toString((short) value);
					case DWORD -> Integer.toString((int) value);
					case QWORD -> Long.toString(value);
				};
				case HEXADECIMAL -> {
					String hex = Long.toUnsignedString(value, 16).toUpperCase();
					if (fill) {
						hex = "0".repeat(size.getSize() * 2 - hex.length()) + hex;
					}
					yield "0x" + hex;
				}
				case BINARY -> {
					String bin = Long.toUnsignedString(value, 2);
					if (fill) {
						bin = "0".repeat(size.getSize() * 8 - bin.length()) + bin;
					}
					yield "0b" + bin;
				}
			};
		}
		
		public int maxWidth(@NotNull ValueSize size) {
			return switch (this) {
				case DECIMAL -> switch (size) {
					case BYTE -> 3;
					case WORD -> 5;
					case DWORD -> 10;
					case QWORD -> 20;
				};
				case DECIMAL_SIGNED -> switch (size) {
					case BYTE -> 4;
					case WORD -> 6;
					case DWORD -> 11;
					case QWORD -> 21;
				};
				case BINARY -> size.getSize() * 8 + 2;
				case HEXADECIMAL -> size.getSize() * 2 + 2;
			};
		}
		
	}
	
	public static void main(String[] args) {
		FlatDarkPurpleIJTheme.setup();
		
		String code = loadCode("/code.qasm");
		ComputerSimulator computer =
			new ComputerSimulator(ComputerSpecs.builder().memorySize(1 << 18).build());
		computer.initialize();
		long rip = computer.getInstructionPointer();
		ComputerWindow window = new ComputerWindow(computer);
		
		{
			byte[] program = null;
			try {
				File baseDir = new File("src/main/resources/").getAbsoluteFile();
				program = InstructionAssembler.assemble(code, rip, baseDir,
					ComputerSimulator.DEFAULT_INCLUDE_PATHS);
			}
			catch (Exception e) {
				System.err.println("Error assembling program");
				e.printStackTrace();
				String message = e.getMessage();
				JOptionPane.showMessageDialog(window.frame, message, "Error assembling program",
					JOptionPane.ERROR_MESSAGE);
			}
			if (program != null) computer.loadProgram(rip, program);
		}
		
		window.update(true);
		
		while (true) {
			if (window.controlsBar.running) {
				long start = System.nanoTime();
				for (int i = 0; i < window.controlsBar.spt; i++) {
					computer.step();
					if (computer.isExiting() || !window.controlsBar.running) {
						break;
					}
				}
				window.update();
				long end = System.nanoTime();
				long elapsed = end - start;
				long sleep = 1000000000 / window.controlsBar.tps - elapsed;
				if (sleep > 0) {
					try {
						Thread.sleep(sleep / 1000000, (int) (sleep % 1000000));
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (computer.isExiting()) {
					window.controlsBar.running = false;
				}
			}
			else {
				Thread.yield();
			}
		}
	}
	
	@NotNull
	public static String loadCode(@NotNull String path) {
		try (InputStream stream = ComputerWindow.class.getResourceAsStream(path)) {
			if (stream == null) {
				throw new RuntimeException("Resource not found: " + path);
			}
			return new String(stream.readAllBytes());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
