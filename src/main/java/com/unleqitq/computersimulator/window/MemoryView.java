package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerWindow;
import com.unleqitq.computersimulator.ValueSize;
import com.unleqitq.computersimulator.utils.NumberUtils;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class MemoryView {
	
	private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
	
	public int memoryRowCount = 100;
	
	@NotNull
	private final ComputerWindow computerWindow;
	@NotNull
	public final JInternalFrame internalFrame;
	
	@NotNull
	public final JPanel rootPanel;
	
	@NotNull
	public final JScrollPane scrollPane;
	
	@NotNull
	public final JPanel panel;
	
	@NotNull
	public final JLabel headerLabel;
	
	public long baseAddress;
	
	@NotNull
	public final SettingsBar settingsBar;
	
	@NotNull
	public final List<MemoryRow> memoryRows;
	
	@NotNull
	public final ReentrantLock lock;
	
	@SneakyThrows
	public MemoryView(@NotNull ComputerWindow computerWindow) {
		this.computerWindow = computerWindow;
		lock = new ReentrantLock();
		internalFrame = new JInternalFrame("Memory");
		internalFrame.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		internalFrame.setSize(700, 500);
		internalFrame.setLocation(0, 400);
		internalFrame.setMaximizable(true);
		internalFrame.setIconifiable(true);
		internalFrame.setResizable(true);
		internalFrame.setClosable(true);
		internalFrame.show();
		
		rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());
		
		headerLabel = new JLabel();
		headerLabel.setFont(FONT);
		
		settingsBar = new SettingsBar();
		rootPanel.add(settingsBar.toolBar, BorderLayout.NORTH);
		
		memoryRows = new ArrayList<>();
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		scrollPane = new JScrollPane(panel);
		scrollPane.setColumnHeaderView(headerLabel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		rootPanel.add(scrollPane, BorderLayout.CENTER);
		
		internalFrame.setContentPane(rootPanel);
		
		reload();
	}
	
	public void reload() {
		lock.lock();
		panel.removeAll();
		memoryRows.clear();
		long address = baseAddress;
		
		{
			StringBuilder sb = new StringBuilder();
			sb.append("%10s".formatted("Address")).append(" ".repeat(2));
			for (int i = 0; i < 16; i++) {
				int t = (int) ((i + baseAddress) & 0xF);
				sb.append(
					"%%%ds".formatted(computerWindow.visualSettings.valueView.maxWidth(ValueSize.BYTE))
						.formatted(Integer.toHexString(t).toUpperCase()));
				if (i < 15) sb.append(' ');
			}
			headerLabel.setText(sb.toString());
		}
		
		for (int i = 0; i < memoryRowCount; i++) {
			MemoryRow row = new MemoryRow(address);
			memoryRows.add(row);
			panel.add(row.label);
			address += 16;
		}
		panel.repaint();
		panel.revalidate();
		
		// Too many components can cause a delay in the GUI
		internalFrame.setIconifiable(memoryRowCount <= 100);
		lock.unlock();
	}
	
	public void update() {
		try {
			if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) return;
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		for (MemoryRow row : memoryRows) {
			row.update();
		}
		lock.unlock();
	}
	
	public final class MemoryRow {
		
		@NotNull
		public final JLabel label;
		
		public final long address;
		
		public MemoryRow(long address) {
			this.address = address;
			this.label = new JLabel();
			this.label.setFont(FONT);
			
			update();
		}
		
		public void update() {
			StringBuilder sb = new StringBuilder();
			String addr = Long.toUnsignedString(address >>> 4, 16) + 'x';
			sb.append("%10s".formatted(addr)).append(" ".repeat(2));
			
			for (int i = 0; i < 16; i++) {
				try {
					String v = computerWindow.visualSettings.valueView.format(ValueSize.BYTE,
						computerWindow.computer.memory().readByte(address + i), true);
					sb.append(
						"%%%ds".formatted(computerWindow.visualSettings.valueView.maxWidth(ValueSize.BYTE))
							.formatted(v));
					if (i < 15) sb.append(' ');
				}
				catch (ArrayIndexOutOfBoundsException e) {
					sb.append("?".repeat(computerWindow.visualSettings.valueView.maxWidth(ValueSize.BYTE)));
					if (i < 15) sb.append(' ');
				}
			}
			label.setText(sb.toString());
		}
		
	}
	
	public final class SettingsBar {
		
		@NotNull
		public final JToolBar toolBar;
		
		@NotNull
		public final JLabel rowsLabel;
		@NotNull
		public final JTextField rowsField;
		
		@NotNull
		public final JLabel baseAddressLabel;
		@NotNull
		public final JTextField baseAddressField;
		
		@NotNull
		public final JButton setButton;
		@NotNull
		public final JButton resetButton;
		
		@NotNull
		public final JButton modifyButton;
		
		public SettingsBar() {
			toolBar = new JToolBar();
			toolBar.setFloatable(true);
			
			rowsLabel = new JLabel("Rows:");
			rowsField = new JTextField("100");
			rowsField.setColumns(10);
			
			baseAddressLabel = new JLabel("Address:");
			baseAddressField = new JTextField("0x0");
			baseAddressField.setColumns(10);
			
			setButton = new JButton("Set");
			resetButton = new JButton("Reset");
			
			modifyButton = new JButton("Modify");
			
			toolBar.add(rowsLabel);
			toolBar.add(rowsField);
			toolBar.addSeparator();
			toolBar.add(baseAddressLabel);
			toolBar.add(baseAddressField);
			toolBar.addSeparator();
			toolBar.add(setButton);
			toolBar.add(resetButton);
			toolBar.addSeparator();
			toolBar.add(modifyButton);
			
			setButton.addActionListener(e -> {
				try {
					long v0 = NumberUtils.parseNumber(baseAddressField.getText());
					int v1 = Integer.parseInt(rowsField.getText());
					if (v1 < 2) {
						throw new NumberFormatException();
					}
					baseAddress = v0;
					memoryRowCount = v1;
					reload();
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(computerWindow.frame, "Invalid value", "Error",
						JOptionPane.ERROR_MESSAGE);
				}
			});
			
			resetButton.addActionListener(e -> {
				baseAddressField.setText(
					computerWindow.visualSettings.valueView.format(ValueSize.QWORD, baseAddress));
				rowsField.setText(Integer.toString(memoryRowCount));
			});
			
			modifyButton.addActionListener(e -> {
				handleModify();
			});
		}
		
		private void handleModify() {
			String addressStr =
				JOptionPane.showInputDialog(computerWindow.frame, "Enter address to modify", "Modify",
					JOptionPane.QUESTION_MESSAGE);
			if (addressStr == null) return;
			long address;
			try {
				address = NumberUtils.parseNumber(addressStr);
			}
			catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(computerWindow.frame, "Invalid value", "Error",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
			int sizeIdx =
				JOptionPane.showOptionDialog(computerWindow.frame, "Select size to modify", "Modify",
					JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, ValueSize.values(),
					ValueSize.BYTE);
			if (sizeIdx == JOptionPane.CLOSED_OPTION) return;
			ValueSize size = ValueSize.values()[sizeIdx];
			long prevValue = switch (size) {
				case BYTE -> computerWindow.computer.memory().readByte(address);
				case WORD -> computerWindow.computer.memory().readWord(address);
				case DWORD -> computerWindow.computer.memory().readDword(address);
				case QWORD -> computerWindow.computer.memory().readQword(address);
			};
			String prevValueStr = computerWindow.visualSettings.valueView.format(size, prevValue);
			String valueStr = JOptionPane.showInputDialog(computerWindow.frame,
				"Enter new value (current: %s)".formatted(prevValueStr), prevValueStr);
			if (valueStr == null) return;
			long value;
			try {
				value = NumberUtils.parseNumber(valueStr);
			}
			catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(computerWindow.frame, "Invalid value", "Error",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
			switch (size) {
				case BYTE -> computerWindow.computer.memory().writeByte(address, value);
				case WORD -> computerWindow.computer.memory().writeWord(address, value);
				case DWORD -> computerWindow.computer.memory().writeDword(address, value);
				case QWORD -> computerWindow.computer.memory().writeQword(address, value);
			}
			computerWindow.update();
		}
		
	}
	
}
