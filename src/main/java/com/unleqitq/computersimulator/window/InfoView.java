package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerWindow;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public final class InfoView {
	
	@NotNull
	public final ComputerWindow computerWindow;
	
	@NotNull
	public final JInternalFrame internalFrame;
	
	@NotNull
	public final JPanel rootPanel;
	
	@NotNull
	public final SettingsBar settingsBar;
	
	@NotNull
	public final JPanel panel;
	
	@NotNull
	public final JLabel stepCountLabel;
	
	
	public InfoView(@NotNull ComputerWindow computerWindow) {
		this.computerWindow = computerWindow;
		
		internalFrame = new JInternalFrame("Info");
		internalFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		internalFrame.setResizable(true);
		internalFrame.setIconifiable(true);
		internalFrame.setMaximizable(true);
		internalFrame.setClosable(true);
		internalFrame.setSize(300, 150);
		internalFrame.setLocation(700, 700);
		internalFrame.show();
		
		rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());
		internalFrame.setContentPane(rootPanel);
		
		settingsBar = new SettingsBar();
		rootPanel.add(settingsBar.toolBar, BorderLayout.NORTH);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		rootPanel.add(panel, BorderLayout.CENTER);
		
		stepCountLabel = new JLabel("Step Count: " + computerWindow.computer.stepCount());
		panel.add(stepCountLabel);
	}
	
	public void update() {
		stepCountLabel.setText("Step Count: " + computerWindow.computer.stepCount());
	}
	
	public class SettingsBar {
		
		@NotNull
		public final JToolBar toolBar;
		
		@NotNull
		public final JButton resetStepCountButton;
		
		public SettingsBar() {
			toolBar = new JToolBar();
			toolBar.setFloatable(true);
			rootPanel.add(toolBar, BorderLayout.NORTH);
			
			resetStepCountButton = new JButton("Reset Step Count");
			toolBar.add(resetStepCountButton);
			
			resetStepCountButton.addActionListener(e -> {
				computerWindow.computer.resetStepCount();
				update();
			});
		}
	}
	
}
