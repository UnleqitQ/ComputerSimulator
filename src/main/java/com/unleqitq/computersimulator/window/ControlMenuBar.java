package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerWindow;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class ControlMenuBar {
	
	@NotNull
	public final ComputerWindow computerWindow;
	@NotNull
	public final JMenuBar menuBar;
	
	@NotNull
	public final JMenu viewMenu;
	
	public ControlMenuBar(@NotNull ComputerWindow computerWindow) {
		this.computerWindow = computerWindow;
		menuBar = new JMenuBar();
		
		viewMenu = new JMenu("View");
		menuBar.add(viewMenu);
		
		addViewToggle("Registers", computerWindow.registersView.internalFrame);
		addViewToggle("Instructions", computerWindow.instructionsView.internalFrame);
		addViewToggle("Screen", computerWindow.screenView.internalFrame);
		addViewToggle("Memory", computerWindow.memoryView.internalFrame);
		addViewToggle("Program", computerWindow.programView.internalFrame);
		addViewToggle("Stack", computerWindow.stackView.internalFrame);
		addViewToggle("Keyboard input", computerWindow.keyboardInputView.internalFrame);
		addViewToggle("Info", computerWindow.infoView.internalFrame);
		
		viewMenu.addSeparator();
		
		{
			JMenu valueViewMenu = new JMenu("Value view");
			viewMenu.add(valueViewMenu);
			
			JRadioButtonMenuItem decimalItem = new JRadioButtonMenuItem("Decimal");
			JRadioButtonMenuItem decimalSignedItem = new JRadioButtonMenuItem("Decimal signed");
			JRadioButtonMenuItem hexadecimalItem = new JRadioButtonMenuItem("Hexadecimal");
			JRadioButtonMenuItem binaryItem = new JRadioButtonMenuItem("Binary");
			
			decimalItem.setSelected(
				computerWindow.visualSettings.valueView == ComputerWindow.ValueView.DECIMAL);
			decimalItem.addActionListener(e -> {
				decimalItem.setSelected(true);
				decimalSignedItem.setSelected(false);
				hexadecimalItem.setSelected(false);
				binaryItem.setSelected(false);
				computerWindow.visualSettings.valueView = ComputerWindow.ValueView.DECIMAL;
				computerWindow.update();
			});
			
			decimalItem.setSelected(
				computerWindow.visualSettings.valueView == ComputerWindow.ValueView.DECIMAL_SIGNED);
			decimalSignedItem.addActionListener(e -> {
				decimalItem.setSelected(false);
				decimalSignedItem.setSelected(true);
				hexadecimalItem.setSelected(false);
				binaryItem.setSelected(false);
				computerWindow.visualSettings.valueView = ComputerWindow.ValueView.DECIMAL_SIGNED;
				computerWindow.update();
			});
			
			hexadecimalItem.setSelected(
				computerWindow.visualSettings.valueView == ComputerWindow.ValueView.HEXADECIMAL);
			hexadecimalItem.addActionListener(e -> {
				decimalItem.setSelected(false);
				decimalSignedItem.setSelected(false);
				hexadecimalItem.setSelected(true);
				binaryItem.setSelected(false);
				computerWindow.visualSettings.valueView = ComputerWindow.ValueView.HEXADECIMAL;
				computerWindow.update();
			});
			
			binaryItem.setSelected(
				computerWindow.visualSettings.valueView == ComputerWindow.ValueView.BINARY);
			binaryItem.addActionListener(e -> {
				decimalItem.setSelected(false);
				decimalSignedItem.setSelected(false);
				hexadecimalItem.setSelected(false);
				binaryItem.setSelected(true);
				computerWindow.visualSettings.valueView = ComputerWindow.ValueView.BINARY;
				computerWindow.update();
			});
			
			valueViewMenu.add(decimalItem);
			valueViewMenu.add(decimalSignedItem);
			valueViewMenu.add(hexadecimalItem);
			valueViewMenu.add(binaryItem);
		}
	}
	
	public void addViewToggle(@NotNull String name, @NotNull JInternalFrame intf) {
		JMenuItem item = new JMenuItem("Toggle " + name);
		item.addActionListener(e -> {
			if (intf.isShowing()) {
				intf.hide();
			}
			else {
				intf.show();
			}
		});
		viewMenu.add(item);
	}
	
}
