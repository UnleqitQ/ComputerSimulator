package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerWindow;
import com.unleqitq.computersimulator.ValueSize;
import com.unleqitq.computersimulator.components.Registers;
import com.unleqitq.computersimulator.instruction.Instruction;
import com.unleqitq.computersimulator.utils.NumberUtils;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public final class InstructionsView {
	
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
	public final SettingsBar settingsBar;
	
	@NotNull
	public final List<InstructionLine> instructionLines;
	
	public long baseInstructionPointer;
	public long instructionPointer;
	
	public int instructionLineCount = 400;
	
	private final ReentrantLock lock = new ReentrantLock();
	
	@SneakyThrows
	public InstructionsView(@NotNull ComputerWindow computerWindow) {
		this.computerWindow = computerWindow;
		baseInstructionPointer =
			computerWindow.computer.registers().readRegister(Registers.Register.RIP);
		instructionPointer = computerWindow.computer.registers().readRegister(Registers.Register.RIP);
		internalFrame = new JInternalFrame("Instructions");
		internalFrame.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		internalFrame.setSize(400, 400);
		internalFrame.setLocation(0, 0);
		internalFrame.setMaximizable(true);
		internalFrame.setIconifiable(true);
		internalFrame.setResizable(true);
		internalFrame.setClosable(true);
		internalFrame.show();
		
		rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());
		
		settingsBar = new SettingsBar();
		rootPanel.add(settingsBar.toolBar, BorderLayout.NORTH);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		scrollPane = new JScrollPane(panel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		rootPanel.add(scrollPane, BorderLayout.CENTER);
		
		internalFrame.setContentPane(rootPanel);
		
		instructionLines = new ArrayList<>();
		
		reload();
	}
	
	public void reload() {
		lock.lock();
		instructionPointer = computerWindow.computer.registers().readRegister(Registers.Register.RIP);
		instructionLines.clear();
		panel.removeAll();
		long address = baseInstructionPointer;
		for (int i = 0; i < instructionLineCount; i++) {
			Instruction instruction;
			try {
				instruction = computerWindow.computer.getInstruction(address);
			}
			catch (Exception e) {
				instruction = null;
			}
			instructionLines.add(new InstructionLine(instruction, address));
			panel.add(instructionLines.get(i).panel);
			address += instruction == null ? 1 : instruction.getLength();
		}
		panel.repaint();
		panel.revalidate();
		
		// Too many components can cause a delay in the GUI
		internalFrame.setIconifiable(instructionLineCount <= 100);
		lock.unlock();
	}
	
	public void update() {
		if (!lock.tryLock()) {
			return;
		}
		long newInstructionPointer =
			computerWindow.computer.registers().readRegister(Registers.Register.RIP);
		boolean changed = newInstructionPointer != instructionPointer;
		boolean autoScroll = settingsBar.autoScrollButton.isSelected();
		instructionPointer = newInstructionPointer;
		if (changed && autoScroll) {
			if (instructionPointer < instructionLines.getFirst().address) {
				scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMinimum());
			}
			else {
				scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
				for (int i = 0; i < instructionLineCount; i++) {
					if (instructionLines.get(i).address >= instructionPointer) {
						scrollPane.getVerticalScrollBar()
							.setValue(scrollPane.getVerticalScrollBar().getMinimum() + Math.max(0,
								i - scrollPane.getHeight() / instructionLines.get(i).panel.getHeight() / 2) *
								(scrollPane.getVerticalScrollBar().getMaximum() -
									scrollPane.getVerticalScrollBar().getMinimum()) / instructionLineCount);
						break;
					}
				}
			}
		}
		
		for (InstructionLine line : instructionLines) {
			line.update();
		}
		
		lock.unlock();
	}
	
	public final class InstructionLine {
		
		@Nullable
		public final Instruction instruction;
		@NotNull
		public final JPanel panel;
		@NotNull
		public final JLabel addressLabel;
		@NotNull
		public final JLabel instructionLabel;
		
		public final long address;
		
		public InstructionLine(@Nullable Instruction instruction, long address) {
			this.instruction = instruction;
			this.address = address;
			this.panel = new JPanel();
			panel.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
			this.addressLabel = new JLabel();
			this.instructionLabel = new JLabel();
			Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
			addressLabel.setFont(font);
			addressLabel.setForeground(Color.WHITE);
			instructionLabel.setFont(font);
			instructionLabel.setForeground(Color.WHITE);
			
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.add(addressLabel);
			panel.add(Box.createHorizontalStrut(10));
			panel.add(instructionLabel);
			addressLabel.setText(String.format("%10s", Long.toUnsignedString(address, 16).toUpperCase()));
			instructionLabel.setText(
				String.format("%-40s", instruction == null ? "NOP" : instruction.toAssembly()));
			
			{
				JPopupMenu popupMenu = new JPopupMenu();
				
				JMenuItem copyAddressItem = new JMenuItem("Copy address");
				copyAddressItem.addActionListener(e -> {
					Toolkit.getDefaultToolkit().getSystemClipboard()
						.setContents(new StringSelection(Long.toUnsignedString(address, 16)), null);
				});
				popupMenu.add(copyAddressItem);
				
				JMenuItem copyInstructionItem = new JMenuItem("Copy instruction");
				copyInstructionItem.addActionListener(e -> {
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
						new StringSelection(instruction == null ? "NOP" : instruction.toAssembly()), null);
				});
				popupMenu.add(copyInstructionItem);
				
				JMenuItem openItem = new JMenuItem("Open in memory view");
				openItem.addActionListener(e -> {
					computerWindow.memoryView.baseAddress = address;
					computerWindow.memoryView.reload();
					computerWindow.memoryView.internalFrame.show();
				});
				popupMenu.add(openItem);
				
				panel.setComponentPopupMenu(popupMenu);
			}
			
			update();
		}
		
		public void update() {
			if (instructionPointer == address) {
				panel.setBackground(new Color(0x007700));
			}
			else {
				panel.setBackground(Color.BLACK);
			}
		}
		
	}
	
	public final class SettingsBar {
		
		@NotNull
		public final JToolBar toolBar;
		
		@NotNull
		public final JLabel linesLabel;
		@NotNull
		public final JTextField linesField;
		
		@NotNull
		public final JLabel baseInstructionPointerLabel;
		@NotNull
		public final JTextField baseInstructionPointerField;
		
		@NotNull
		public final JButton setButton;
		@NotNull
		public final JButton resetButton;
		
		@NotNull
		public final JToggleButton autoScrollButton;
		
		public SettingsBar() {
			toolBar = new JToolBar();
			toolBar.setFloatable(true);
			
			
			linesLabel = new JLabel("Lines:");
			linesField = new JTextField(Integer.toString(instructionLineCount));
			linesField.setColumns(10);
			
			baseInstructionPointerLabel = new JLabel("Address:");
			baseInstructionPointerField =
				new JTextField("0x" + Long.toUnsignedString(baseInstructionPointer, 16));
			baseInstructionPointerField.setColumns(10);
			
			setButton = new JButton("Set");
			resetButton = new JButton("Reset");
			autoScrollButton = new JToggleButton("Auto scroll");
			
			toolBar.add(linesLabel);
			toolBar.add(linesField);
			toolBar.addSeparator();
			toolBar.add(baseInstructionPointerLabel);
			toolBar.add(baseInstructionPointerField);
			toolBar.addSeparator();
			toolBar.add(setButton);
			toolBar.add(resetButton);
			toolBar.addSeparator();
			toolBar.add(autoScrollButton);
			
			setButton.addActionListener(e -> {
				try {
					long v0 = NumberUtils.parseNumber(baseInstructionPointerField.getText());
					int v1 = Integer.parseInt(linesField.getText());
					if (v1 < 10) {
						throw new NumberFormatException();
					}
					baseInstructionPointer = v0;
					instructionLineCount = v1;
					reload();
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(computerWindow.frame, "Invalid value", "Error",
						JOptionPane.ERROR_MESSAGE);
				}
			});
			
			resetButton.addActionListener(e -> {
				baseInstructionPointerField.setText(
					computerWindow.visualSettings.valueView.format(ValueSize.QWORD, baseInstructionPointer));
				linesField.setText(Integer.toString(instructionLineCount));
			});
		}
		
	}
	
}
