package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerWindow;
import com.unleqitq.computersimulator.components.StackWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public final class StackView {
	
	private static final int SPACING = 20;
	
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
	public final List<StackRow> stackRows;
	
	private long lastStackPointer = 0;
	private int lastModCount = -1;
	
	public StackView(@NotNull ComputerWindow computerWindow) {
		this.computerWindow = computerWindow;
		
		internalFrame = new JInternalFrame("Stack");
		internalFrame.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		internalFrame.setSize(700, 900);
		internalFrame.setLocation(1200, 0);
		internalFrame.setMaximizable(true);
		internalFrame.setIconifiable(true);
		internalFrame.setResizable(true);
		internalFrame.setClosable(true);
		internalFrame.show();
		
		rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());
		internalFrame.setContentPane(rootPanel);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		scrollPane = new JScrollPane(panel);
		rootPanel.add(scrollPane, BorderLayout.CENTER);
		
		{
			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
			header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
			
			JLabel addressLabel = new JLabel("%-23s".formatted("Address"));
			JLabel sizeLabel = new JLabel("%-9s".formatted("Size"));
			JLabel pushedValueLabel = new JLabel("%-12s".formatted("Original"));
			JLabel valueLabel = new JLabel("Value");
			
			Font font = new Font(Font.MONOSPACED, Font.BOLD, 12);
			addressLabel.setFont(font);
			sizeLabel.setFont(font);
			valueLabel.setFont(font);
			pushedValueLabel.setFont(font);
			
			header.add(addressLabel);
			header.add(Box.createHorizontalStrut(SPACING));
			header.add(sizeLabel);
			header.add(Box.createHorizontalStrut(SPACING));
			header.add(pushedValueLabel);
			header.add(Box.createHorizontalStrut(SPACING));
			header.add(valueLabel);
			
			rootPanel.add(header, BorderLayout.NORTH);
		}
		
		stackRows = new ArrayList<>();
	}
	
	private boolean changed() {
		int modCount = computerWindow.computer.stack().history().getHistory().getModCount();
		if (lastStackPointer != computerWindow.computer.stack().getStackPointer()) {
			lastStackPointer = computerWindow.computer.stack().getStackPointer();
			lastModCount = modCount;
			return true;
		}
		if (lastModCount != modCount) {
			lastModCount = modCount;
			return true;
		}
		return false;
	}
	
	public void update() {
		if (changed()) {
			lastStackPointer = computerWindow.computer.stack().getStackPointer();
			reload();
		}
		else {
			for (StackRow row : stackRows) {
				row.update();
			}
		}
	}
	
	public void reload() {
		panel.removeAll();
		stackRows.clear();
		
		List<StackWrapper.ResolvedStackEntry> stack = computerWindow.computer.stack().resolveStack();
		
		for (StackWrapper.ResolvedStackEntry entry : stack) {
			StackRow row = new StackRow(entry);
			
			stackRows.add(row);
			panel.add(row.rowPanel);
		}
		
		panel.revalidate();
		panel.repaint();
		
		// Too many components can cause a delay in the GUI
		internalFrame.setIconifiable(stack.size() <= 120);
	}
	
	public class StackRow {
		
		@NotNull
		private final JPanel rowPanel;
		
		@NotNull
		private final StackWrapper.ResolvedStackEntry entry;
		
		@NotNull
		private final JLabel addressLabel;
		
		@NotNull
		private final JLabel valueLabel;
		
		@NotNull
		private final JLabel sizeLabel;
		
		@NotNull
		private final JLabel pushedValueLabel;
		
		public StackRow(@NotNull StackWrapper.ResolvedStackEntry entry) {
			this.entry = entry;
			
			rowPanel = new JPanel();
			rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
			rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
			
			addressLabel =
				new JLabel(String.format("0x%08X - 0x%08X", entry.fromAddress(), entry.toAddress()));
			valueLabel = new JLabel();
			StackWrapper.StackHistory.StackEntry matchedEntry = entry.entry();
			if (matchedEntry != null) {
				sizeLabel = new JLabel("%9s".formatted(matchedEntry.size()));
				pushedValueLabel = new JLabel("%12s".formatted(
					computerWindow.visualSettings.valueView.format(matchedEntry.size(),
						matchedEntry.value())));
			}
			else {
				sizeLabel = new JLabel(
					"%9s".formatted("%d bytes".formatted(entry.toAddress() - entry.fromAddress() + 1)));
				pushedValueLabel = new JLabel("%12s".formatted("N/A"));
			}
			
			Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
			addressLabel.setFont(font);
			valueLabel.setFont(font);
			sizeLabel.setFont(font);
			pushedValueLabel.setFont(font);
			
			rowPanel.add(addressLabel);
			rowPanel.add(Box.createHorizontalStrut(SPACING));
			rowPanel.add(sizeLabel);
			rowPanel.add(Box.createHorizontalStrut(SPACING));
			rowPanel.add(pushedValueLabel);
			rowPanel.add(Box.createHorizontalStrut(SPACING));
			rowPanel.add(valueLabel);
			
			{
				JPopupMenu popupMenu = new JPopupMenu();
				
				JMenuItem copyAddressItem = new JMenuItem("Copy address");
				copyAddressItem.addActionListener(e -> {
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
						new StringSelection(String.format("0x%08X", entry.fromAddress())), null);
				});
				popupMenu.add(copyAddressItem);
				
				JMenuItem copyValueItem = new JMenuItem("Copy value");
				copyValueItem.addActionListener(e -> {
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
						new StringSelection(valueLabel.getText()), null);
				});
				popupMenu.add(copyValueItem);
				
				JMenuItem openItem = new JMenuItem("Open in memory view");
				openItem.addActionListener(e -> {
					computerWindow.memoryView.baseAddress = entry.fromAddress();
					computerWindow.memoryView.reload();
					computerWindow.memoryView.internalFrame.show();
				});
				popupMenu.add(openItem);
				
				rowPanel.setComponentPopupMenu(popupMenu);
			}
			
			update();
		}
		
		public void update() {
			long bp = computerWindow.computer.stack().getBasePointer();
			Color fg, bg;
			if (entry.fromAddress() <= bp && bp <= entry.toAddress()) {
				bg = Color.LIGHT_GRAY;
				fg = Color.BLACK;
			}
			else {
				bg = Color.BLACK;
				fg = Color.WHITE;
			}
			rowPanel.setBackground(bg);
			addressLabel.setForeground(fg);
			valueLabel.setForeground(fg);
			sizeLabel.setForeground(fg);
			pushedValueLabel.setForeground(fg);
			
			StringBuilder sb = new StringBuilder();
			int size = (int) (entry.toAddress() - entry.fromAddress() + 1);
			byte[] data = computerWindow.computer.memory().read(entry.fromAddress(), size);
			for (int i = 0; i < data.length; i++) {
				byte b = data[i];
				sb.append(String.format("%02X", b));
				if (i < data.length - 1) sb.append(" ");
			}
			valueLabel.setText(sb.toString());
		}
		
	}
	
}
