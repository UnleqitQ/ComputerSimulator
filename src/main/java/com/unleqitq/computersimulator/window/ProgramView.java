package com.unleqitq.computersimulator.window;

import com.unleqitq.computersimulator.ComputerSimulator;
import com.unleqitq.computersimulator.ComputerWindow;
import com.unleqitq.computersimulator.instruction.InstructionAssembler;
import com.unleqitq.computersimulator.utils.NumberUtils;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class ProgramView {
	
	@NotNull
	private final ComputerWindow computerWindow;
	@NotNull
	public final JInternalFrame internalFrame;
	@NotNull
	public final JPanel panel;
	
	@NotNull
	public final JScrollPane scrollPane;
	
	@NotNull
	public final JTextArea textArea;
	
	@NotNull
	public final JToolBar toolBar;
	
	@NotNull
	public final JButton loadButton;
	
	@NotNull
	public final JButton saveButton;
	
	@NotNull
	public final JButton openButton;
	
	@NotNull
	public final JButton changeBaseDirButton;
	
	@NotNull
	public final JFileChooser fileChooser;
	
	@Nullable
	private Long lastAddress;
	
	@NotNull
	private File baseDir;
	
	
	@SneakyThrows
	public ProgramView(@NotNull ComputerWindow computerWindow) {
		this.computerWindow = computerWindow;
		fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(".").getAbsoluteFile());
		
		internalFrame = new JInternalFrame("Program");
		internalFrame.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		internalFrame.setSize(500, 900);
		internalFrame.setLocation(1400, 0);
		internalFrame.setMaximizable(true);
		internalFrame.setIconifiable(true);
		internalFrame.setResizable(true);
		internalFrame.setClosable(true);
		internalFrame.show();
		
		baseDir = new File(".").getAbsoluteFile();
		{
			// Check if in development environment
			File file = new File(baseDir, "src/main/resources/");
			if (file.exists()) {
				baseDir = file;
			}
		}
		
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		internalFrame.setContentPane(panel);
		
		textArea = new JTextArea();
		scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane, BorderLayout.CENTER);
		textArea.setText(ComputerWindow.loadCode("/code.qasm"));
		
		toolBar = new JToolBar();
		loadButton = new JButton("Load");
		saveButton = new JButton("Save");
		openButton = new JButton("Open");
		changeBaseDirButton = new JButton("Change directory");
		toolBar.add(loadButton);
		toolBar.add(saveButton);
		toolBar.add(openButton);
		toolBar.add(changeBaseDirButton);
		loadButton.addActionListener(e -> {
			int result1 =
				JOptionPane.showConfirmDialog(computerWindow.frame, "Load at current instruction pointer?",
					"Load program", JOptionPane.YES_NO_OPTION);
			long address;
			if (result1 == JOptionPane.YES_OPTION) {
				address = computerWindow.computer.getInstructionPointer();
			}
			else if (result1 == JOptionPane.NO_OPTION) {
				String result2;
				if (lastAddress == null) {
					result2 =
						JOptionPane.showInputDialog(computerWindow.frame, "Enter address to load program at",
							"Load program", JOptionPane.QUESTION_MESSAGE);
				}
				else {
					result2 =
						JOptionPane.showInputDialog(computerWindow.frame, "Enter address to load program at",
							"Load program", JOptionPane.QUESTION_MESSAGE, null, null,
							"0x" + Long.toUnsignedString(lastAddress, 16)).toString();
				}
				if (result2 == null) {
					return;
				}
				try {
					address = NumberUtils.parseNumber(result2);
					lastAddress = address;
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(computerWindow.frame, "Invalid address", "Error",
						JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			else {
				return;
			}
			String code = textArea.getText();
			
			byte[] program = InstructionAssembler.assemble(code, address, baseDir,
				ComputerSimulator.DEFAULT_INCLUDE_PATHS);
			computerWindow.computer.loadProgram(address, program);
			computerWindow.instructionsView.reload();
		});
		saveButton.addActionListener(e -> {
			String code = textArea.getText();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.showSaveDialog(computerWindow.frame);
			File file = fileChooser.getSelectedFile();
			if (file != null) {
				try {
					Files.writeString(file.toPath(), code);
				}
				catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
		openButton.addActionListener(e -> {
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.showOpenDialog(computerWindow.frame);
			File file = fileChooser.getSelectedFile();
			if (file != null) {
				try {
					String code = Files.readString(file.toPath());
					textArea.setText(code);
					
					// Set base directory to the directory of the file
					baseDir = file.getParentFile();
				}
				catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
		changeBaseDirButton.addActionListener(e -> {
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.showOpenDialog(computerWindow.frame);
			File file = fileChooser.getSelectedFile();
			if (file != null) {
				baseDir = file.getAbsoluteFile();
			}
		});
		panel.add(toolBar, BorderLayout.NORTH);
	}
	
}
