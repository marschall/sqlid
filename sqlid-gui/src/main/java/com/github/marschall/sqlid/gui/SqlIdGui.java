package com.github.marschall.sqlid.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
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
import javax.swing.SwingWorker.StateValue;
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
  private JFrame frame;

  SqlIdGui() {
    this.model = new SqlIdModel();
  }

  void computeSqlId() {
    // TODO disable GUI
    // TODO enable GUI
    SwingWorker<String, String> task = this.model.computeSqlIdWworker();
    task.addPropertyChangeListener(event -> {
      if (event.getPropertyName().equals("state")) {
        Object newValue = event.getNewValue();
        Object oldValue = event.getOldValue();
        try {
          task.get();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (ExecutionException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
    task.execute();
  }

  void showException(SQLException exception) {
    JOptionPane.showMessageDialog(this.frame, exception.getMessage(), "SQL Exception", JOptionPane.ERROR_MESSAGE);
  }

  private void setUrl(String url) {
    this.setModelProperty(url, this.model::setUrl);
  }

  private void setUser(String user) {
    this.setModelProperty(user, this.model::setUser);
  }
  
  private void setPassword(String password) {
    this.setModelProperty(password, this.model::setPassword);
  }

  private void setModelProperty(String value, Consumer<String> setter) {
    boolean validBefore = this.model.isValid();
    setter.accept(value);
    boolean validAfter = this.model.isValid();
    if (validBefore != validAfter) {
      this.setComputeEnabled(validAfter);
    }
  }

  private void setComputeEnabled(boolean enabled) {
    this.computeButton.setEnabled(enabled);
  }

  private void createAndShowGui() {
    this.frame = new JFrame("SQL_ID GUI");
    this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Container contentPane = this.frame.getContentPane();

    addTextArea(contentPane);

    contentPane.add(createBottomPanel(), BorderLayout.PAGE_END);

    this.frame.pack();
    this.frame.setMinimumSize(this.frame.getSize());

    this.frame.setVisible(true);
  }

  private void addTextArea(Container container) {
    JTextArea textArea = new JTextArea(40, 80);
    JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    container.add(scrollPane, BorderLayout.CENTER);
  }

  private JPanel createBottomPanel() {
    JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
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
//    connectionPanel.setBorder(BorderFactory.createTitledBorder("Connection"));
    GridBagConstraints constraints;

    String[] labels = {"URL:", "User:", "Password:"};
    for (int i = 0; i < labels.length; i++) {
      connectionPanel.add(new JLabel(labels[i]), connectionLabelConstraints(i));
    }

    constraints = connectionFieldConstraints(0);
    JTextField urlField = new JTextField(32);
    urlField.addActionListener(event -> setUrl(urlField.getText()));
    connectionPanel.add(urlField, constraints);

    constraints = connectionFieldConstraints(1);
    JTextField userField = new JTextField(32);
    userField.addActionListener(event -> setUser(userField.getText()));
    connectionPanel.add(userField, constraints);

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
