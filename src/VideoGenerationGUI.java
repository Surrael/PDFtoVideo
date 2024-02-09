import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class VideoGenerationGUI extends JFrame {

    private File script;
    private File slideSetFile;
    private JTextField outputFileNameField;

    private JLabel slideSetLabel;
    private JLabel scriptLabel;
    private JTextArea scriptTextArea;
    private JProgressBar progressBar;
    private String outputFileName;

    public VideoGenerationGUI() {
        setTitle("Video Generation");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Apply FlatLaf look and feel
        try {
            FlatLightLaf.install();
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        // Panel for file selection buttons
        JPanel fileSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        fileSelectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // File selection buttons
        JButton selectSlideSetButton = new JButton("Select Slide Set");
        JButton selectScriptButton = new JButton("Select Script");

        // Labels to display selected file names
        slideSetLabel = new JLabel("No file selected");
        scriptLabel = new JLabel("No file selected");

        // TextArea for script editing
        scriptTextArea = new JTextArea();
        scriptTextArea.setRows(20);
        scriptTextArea.setColumns(50);
        JScrollPane scriptScrollPane = new JScrollPane(scriptTextArea);
        scriptScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scriptScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Text field for output file name
        outputFileNameField = new JTextField(20);
        JPanel outputFileNamePanel = new JPanel();
        outputFileNamePanel.add(new JLabel("Output File Name:"));
        outputFileNamePanel.add(outputFileNameField);

        selectSlideSetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
                if (slideSetFile != null) {
                    fileChooser.setCurrentDirectory(slideSetFile.getParentFile());
                }
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    slideSetFile = fileChooser.getSelectedFile();
                    slideSetLabel.setText(slideSetFile.getName());
                    addViewButton(fileSelectionPanel, slideSetFile);
                }
            }
        });

        selectScriptButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
                if (script != null) {
                    fileChooser.setCurrentDirectory(script.getParentFile());
                }
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    script = fileChooser.getSelectedFile();
                    scriptLabel.setText(script.getName());
                    loadScriptContent();
                }
            }
        });

        // Panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Start button
        JButton startButton = new JButton("Generate");
        startButton.setForeground(Color.WHITE);
        startButton.setBackground(new Color(59, 89, 182)); // Blue color
        startButton.setFocusPainted(false);
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (script == null || slideSetFile == null) {
                    JOptionPane.showMessageDialog(null, "Please select a script and a slide set.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    saveScriptContent();
                    outputFileName = outputFileNameField.getText().trim();
                    if (outputFileName.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Please specify an output file name.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    generateVideo(outputFileName);
                }
            }
        });

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        // Adding components to panels
        fileSelectionPanel.add(selectScriptButton);
        fileSelectionPanel.add(scriptLabel);
        fileSelectionPanel.add(selectSlideSetButton);
        fileSelectionPanel.add(slideSetLabel);

        buttonPanel.add(outputFileNamePanel);
        buttonPanel.add(startButton);
        buttonPanel.add(progressBar);

        // Adding panels to the frame
        add(fileSelectionPanel, BorderLayout.NORTH);
        add(scriptScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void loadScriptContent() {
        try (BufferedReader reader = new BufferedReader(new FileReader(script))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            scriptTextArea.setText(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveScriptContent() {
        if (script == null) {
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(script))) {
            writer.write(scriptTextArea.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addViewButton(JPanel panel, File file) {
        JButton viewButton = new JButton("View");
        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openPDFFile(file);
            }
        });
        panel.add(viewButton);
        panel.revalidate();
        panel.repaint();
    }

    private void openPDFFile(File file) {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.OPEN)) {
            try {
                desktop.open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Desktop not supported.");
        }
    }

    private void displayCompletionDialog() {
        int response = JOptionPane.showConfirmDialog(null, "Video generation is complete. Do you want to open the video now?", "Generation Complete", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            // Open the generated video file
            openVideoFile(outputFileName + ".mp4");
        }
    }

    private void openVideoFile(String filePath) {
        File file = new File(filePath);
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.OPEN)) {
            try {
                desktop.open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Desktop not supported, handle it accordingly
            System.err.println("Desktop not supported.");
        }
    }

    private void generateVideo(String outputFileName) {
        progressBar.setVisible(true);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // This is where you call your video generation method
                VideoCreator.generateVideo(script, slideSetFile.getAbsolutePath(), outputFileName);
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                displayCompletionDialog();
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new VideoGenerationGUI();
            }
        });
    }
}
