package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerWindow;
import com.unleqitq.computersimulator.ValueSize;
import com.unleqitq.computersimulator.components.Registers;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RegistersView {
	
	@NotNull
	private final ComputerWindow computerWindow;
	@NotNull
	public final JInternalFrame internalFrame;
	
	@NotNull
	public final JPanel panel;
	
	@NotNull
	public final List<RegisterField> registerFields;
	
	@NotNull
	public final FlagsPanel flagsPanel;
	
	@SneakyThrows
	public RegistersView(@NotNull ComputerWindow computerWindow) {
		this.computerWindow = computerWindow;
		internalFrame = new JInternalFrame("Registers");
		internalFrame.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		internalFrame.setSize(500, 550);
		internalFrame.setLocation(700, 0);
		internalFrame.setMaximizable(true);
		internalFrame.setIconifiable(true);
		internalFrame.setResizable(true);
		internalFrame.setClosable(true);
		internalFrame.show();
		
		this.panel = new JPanel();
		this.flagsPanel = new FlagsPanel();
		{
			GridBagLayout gbl = new GridBagLayout();
			gbl.columnWeights = new double[3];
			gbl.rowWeights = new double[10];
			Arrays.fill(gbl.columnWeights, 1.0);
			Arrays.fill(gbl.rowWeights, 1.0);
			panel.setLayout(gbl);
		}
		
		registerFields = new ArrayList<>();
		
		addRegisterField(Registers.Register.RAX, 0, 0);
		addRegisterField(Registers.Register.RBX, 0, 1);
		addRegisterField(Registers.Register.RCX, 0, 2);
		addRegisterField(Registers.Register.RDX, 0, 3);
		addRegisterField(Registers.Register.RSI, 0, 4);
		addRegisterField(Registers.Register.RDI, 0, 5);
		addRegisterField(Registers.Register.FLAGS, 0, 6);
		addRegisterField(Registers.Register.RBP, 0, 7);
		addRegisterField(Registers.Register.RSP, 0, 8);
		addRegisterField(Registers.Register.RIP, 0, 9);
		
		addRegisterField(Registers.Register.R8 , 1, 0);
		addRegisterField(Registers.Register.R9 , 1, 1);
		addRegisterField(Registers.Register.R10, 1, 2);
		addRegisterField(Registers.Register.R11, 1, 3);
		addRegisterField(Registers.Register.R12, 1, 4);
		addRegisterField(Registers.Register.R13, 1, 5);
		addRegisterField(Registers.Register.R14, 1, 6);
		addRegisterField(Registers.Register.R15, 1, 7);
		
		addRegisterField(Registers.Register.CS, 2, 0);
		addRegisterField(Registers.Register.DS, 2, 1);
		addRegisterField(Registers.Register.ES, 2, 2);
		addRegisterField(Registers.Register.FS, 2, 3);
		addRegisterField(Registers.Register.GS, 2, 4);
		addRegisterField(Registers.Register.SS, 2, 5);
		
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx = 2;
			gbc.gridy = 6;
			gbc.gridheight = 4;
			panel.add(flagsPanel.panel, gbc);
		}
		
		internalFrame.setContentPane(panel);
	}
	
	public void addRegisterField(@NotNull Registers.Register register, int x, int y) {
		RegisterField field = new RegisterField(register.name(), register);
		registerFields.add(field);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = x;
		gbc.gridy = y;
		panel.add(field.panel, gbc);
	}
	
	public final class RegisterField {
		
		@NotNull
		public final JPanel panel;
		
		@NotNull
		public final JLabel label;
		
		@NotNull
		public final JTextField field;
		
		@NotNull
		public final JButton setButton;
		
		@NotNull
		public final JButton resetButton;
		
		@NotNull
		public final Registers.Register register;
		
		@NotNull
		public final Color color;
		
		public RegisterField(@NotNull String name, @NotNull Registers.Register register) {
			color = switch (register) {
				case RAX, RBX, RCX, RDX -> new Color(0x003399);
				case RSI, RDI, RBP, RSP, RIP -> new Color(0x009900);
				case R8, R9, R10, R11, R12, R13, R14, R15 -> new Color(0x660066);
				case CS, DS, ES, FS, GS, SS -> new Color(0x994400);
				case FLAGS -> new Color(0x990000);
			};
			
			this.register = register;
			
			panel = new JPanel();
			panel.setBackground(color);
			{
				GridBagLayout layout = new GridBagLayout();
				layout.columnWeights = new double[] {1.0, 1.0};
				layout.rowWeights = new double[] {1.0, 1.0, 0.2};
				panel.setLayout(layout);
			}
			
			label = new JLabel(name);
			field = new JTextField();
			field.setColumns(10);
			field.setBackground(color);
			
			setButton = new JButton("Set");
			setButton.setBackground(color);
			resetButton = new JButton("Reset");
			resetButton.setBackground(color);
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridy = 0;
			panel.add(label, gbc);
			gbc.gridx = 1;
			panel.add(field, gbc);
			
			gbc.gridx = 0;
			gbc.gridy = 1;
			panel.add(setButton, gbc);
			gbc.gridx = 1;
			panel.add(resetButton, gbc);
			
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.gridwidth = 2;
			panel.add(Box.createRigidArea(new Dimension(0, 5)), gbc);
			
			setButton.addActionListener(e -> {
				if (set()) {
					update();
				}
				else {
					JOptionPane.showMessageDialog(computerWindow.frame, "Invalid value", "Error",
						JOptionPane.ERROR_MESSAGE);
				}
			});
			
			resetButton.addActionListener(e -> update());
		}
		
		public void update() {
			field.setText(computerWindow.visualSettings.valueView.format(ValueSize.QWORD,
				computerWindow.computer.registers().readRegister(register)));
		}
		
		public boolean set() {
			String text = field.getText();
			long value;
			if (text.isEmpty()) {
				return false;
			}
			if (text.startsWith("0x")) {
				value = Long.parseUnsignedLong(text.substring(2), 16);
			}
			else if (text.startsWith("0b")) {
				value = Long.parseUnsignedLong(text.substring(2), 2);
			}
			else if (text.startsWith("-")) {
				value = Long.parseLong(text);
			}
			else {
				value = Long.parseUnsignedLong(text);
			}
			computerWindow.computer.registers().writeRegister(register, value);
			return true;
		}
		
	}
	
	public void update() {
		registerFields.forEach(RegisterField::update);
		flagsPanel.update();
	}
	
	public final class FlagsPanel {
		
		@NotNull
		public final JPanel panel;
		
		@NotNull
		public final List<FlagsPanel.FlagField> flagFields;
		
		public FlagsPanel() {
			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			
			flagFields = new ArrayList<>();
			
			addFlagField(Registers.Flag.CARRY);
			addFlagField(Registers.Flag.PARITY);
			addFlagField(Registers.Flag.ADJUST);
			addFlagField(Registers.Flag.ZERO);
			addFlagField(Registers.Flag.SIGN);
			addFlagField(Registers.Flag.TRAP);
			addFlagField(Registers.Flag.INTERRUPT);
			addFlagField(Registers.Flag.DIRECTION);
			addFlagField(Registers.Flag.OVERFLOW);
		}
		
		public void addFlagField(@NotNull Registers.Flag flag) {
			FlagsPanel.FlagField field = new FlagsPanel.FlagField(flag.name(), flag);
			flagFields.add(field);
			panel.add(field.checkBox);
		}
		
		public final class FlagField {
			
			@NotNull
			public final JCheckBox checkBox;
			
			@NotNull
			public final Registers.Flag flag;
			
			public FlagField(@NotNull String name, @NotNull Registers.Flag flag) {
				this.flag = flag;
				
				checkBox = new JCheckBox(name);
				
				checkBox.addActionListener(e -> {
					computerWindow.computer.registers().writeFlag(flag, checkBox.isSelected());
					computerWindow.update();
				});
			}
			
			public void update() {
				checkBox.setSelected(computerWindow.computer.registers().readFlag(flag));
			}
			
		}
		
		public void update() {
			flagFields.forEach(FlagsPanel.FlagField::update);
		}
		
	}
	
}
