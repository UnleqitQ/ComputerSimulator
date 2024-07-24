package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerWindow;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class ControlsBar {
	@NotNull
	private final ComputerWindow computerWindow;
	@NotNull
	public final JToolBar toolBar;
	
	@NotNull
	public final JButton stepButton;
	
	@NotNull
	public final JButton runButton;
	
	@NotNull
	public final JButton stopButton;
	
	@NotNull
	public final JSlider tpsSlider;
	
	@NotNull
	public final JLabel tpsLabel;
	
	@NotNull
	public final JSlider sptSlider;
	
	@NotNull
	public final JLabel sptLabel;
	
	@NotNull
	public final JButton resetButton;
	
	@NotNull
	public final JButton clearMemoryButton;
	
	@NotNull
	public final JButton resetInterruptButton;
	
	public boolean running = false;
	
	public int tps;
	
	public int spt;
	
	public ControlsBar(@NotNull ComputerWindow computerWindow) {
		this.computerWindow = computerWindow;
		toolBar = new JToolBar();
		toolBar.setFloatable(true);
		
		stepButton = new JButton("Step");
		runButton = new JButton("Run");
		stopButton = new JButton("Stop");
		tps = 100;
		tpsSlider = new JSlider(0, 9, 6);
		tpsLabel = new JLabel();
		updateTpsLabel();
		spt = 1;
		sptSlider = new JSlider(0, 15, 0);
		sptLabel = new JLabel();
		updateSptLabel();
		resetButton = new JButton("Reset");
		clearMemoryButton = new JButton("Clear memory");
		resetInterruptButton = new JButton("Not interrupted");
		
		toolBar.add(stepButton);
		toolBar.add(runButton);
		toolBar.add(stopButton);
		toolBar.addSeparator();
		toolBar.add(tpsSlider);
		toolBar.add(tpsLabel);
		toolBar.addSeparator();
		toolBar.add(sptSlider);
		toolBar.add(sptLabel);
		toolBar.addSeparator();
		toolBar.add(resetButton);
		toolBar.add(clearMemoryButton);
		toolBar.add(resetInterruptButton);
		
		stepButton.addActionListener(e -> {
			computerWindow.computer.step();
			computerWindow.update();
		});
		
		runButton.addActionListener(e -> running = true);
		
		stopButton.addActionListener(e -> running = false);
		
		tpsSlider.addChangeListener(e -> {
			tps = switch (tpsSlider.getValue() % 3) {
				case 0 -> 1;
				case 1 -> 2;
				case 2 -> 5;
				default -> throw new IllegalStateException();
			} * (int) Math.pow(10, (int) (tpsSlider.getValue() / 3.));
			updateTpsLabel();
		});
		
		sptSlider.addChangeListener(e -> {
			spt = switch (sptSlider.getValue() % 3) {
				case 0 -> 1;
				case 1 -> 2;
				case 2 -> 5;
				default -> throw new IllegalStateException();
			} * (int) Math.pow(10, (int) (sptSlider.getValue() / 3.));
			updateSptLabel();
		});
		
		resetButton.addActionListener(e -> {
			computerWindow.computer.initialize();
			computerWindow.update();
		});
		
		clearMemoryButton.addActionListener(e -> {
			computerWindow.computer.memory().clear();
			computerWindow.update();
			computerWindow.instructionsView.reload();
		});
		
		resetInterruptButton.addActionListener(e -> {
			computerWindow.computer.resetInterrupt();
			computerWindow.update();
		});
	}
	
	public void update() {
		if (computerWindow.computer.interrupted()) {
			resetInterruptButton.setText("Interrupted: " + computerWindow.computer.interruptCode());
		}
		else {
			resetInterruptButton.setText("Not interrupted");
		}
	}
	
	public void updateTpsLabel() {
		tpsLabel.setText(tps + " ticks/s");
		if (tps == 1)
			tpsLabel.setText("1 tick/s");
	}
	
	public void updateSptLabel() {
		sptLabel.setText(spt + " steps/tick");
		if (spt == 1)
			sptLabel.setText("1 step/tick");
	}
	
}
