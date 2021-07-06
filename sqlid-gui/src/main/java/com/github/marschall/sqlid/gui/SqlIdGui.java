package com.github.marschall.sqlid.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public final class SqlIdGui {

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
      System.err.println("System LaF not supported");
    }
    SqlIdGui gui = new SqlIdGui();
    SwingUtilities.invokeLater(gui::createAndShowGui);
  }

  private final SqlIdModel model;
  private JButton computeButton;

  SqlIdGui() {
    this.model = new SqlIdModel();
  }

  void computeSqlId() {
    // TODO disable GUI
    // TODO enable GUI
    SwingWorker<String, String> task = this.model.computeSqlIdWworker();
    task.execute();
  }
  
  void showException(SQLException exception) {
    // TODO frame
    JOptionPane.showMessageDialog(null, exception.getMessage(), "SQL Exception", JOptionPane.ERROR_MESSAGE);
  }

  private void createAndShowGui() {
    JFrame frame = new JFrame("SQL_ID GUI");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Container contentPane = frame.getContentPane();

    addTextArea(contentPane);

    contentPane.add(createBottomPanel(), BorderLayout.PAGE_END);

    frame.pack();
    frame.setMinimumSize(frame.getSize());

    frame.setVisible(true);
  }

  private void addTextArea(Container container) {
    JTextArea textArea = new JTextArea(40, 80);
    JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    container.add(scrollPane, BorderLayout.CENTER);
  }

  private JPanel createBottomPanel() {
    JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.add(createConnectionPanel(), BorderLayout.LINE_START);
    bottomPanel.add(createButtonPanel(), BorderLayout.LINE_END);
    return bottomPanel;
  }

  private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel();
    computeButton = new JButton("Compute SQL_ID");
    computeButton.addActionListener(e -> computeSqlId());
    buttonPanel.add(computeButton);
    return buttonPanel;
  }

  private JPanel createConnectionPanel() {
    JPanel connectionPanel = new JPanel(new GridBagLayout());
    GridBagConstraints constraints;

    String[] labels = {"URL:", "User:", "Password:"};
    for (int i = 0; i < labels.length; i++) {
      connectionPanel.add(new JLabel(labels[i]), connectionLabelConstraints(i));
    }

    constraints = connectionFieldConstraints(0);
    connectionPanel.add(new JTextField(32), constraints);

    constraints = connectionFieldConstraints(1);
    connectionPanel.add(new JTextField(32), constraints);

    constraints = connectionFieldConstraints(2);
    connectionPanel.add(new JPasswordField(32), constraints);

    return connectionPanel;
  }

  private GridBagConstraints connectionLabelConstraints(int i) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = i;
    constraints.anchor = GridBagConstraints.LINE_END;
    return constraints;
  }

  private GridBagConstraints connectionFieldConstraints(int i) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = i;
    constraints.anchor = GridBagConstraints.LINE_START;
    return constraints;
  }

}
