package com.github.marschall.sqlid.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.StringContent;

public final class SqlIdGui {

  private static final int INSETS = 3;

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
      System.err.println("System LaF not supported");
    }

    SqlIdModel model = new SqlIdModel();
    model.load();
    SqlIdGui gui = new SqlIdGui(model);
    SwingUtilities.invokeLater(gui::createAndShowGui);
  }

  private final SqlIdModel model;
  private JTextField sqlIdField;
  private JButton computeButton;
  private JFrame frame;
  private JTextField urlField;
  private JTextField userField;
  private JPasswordField passwordField;

  SqlIdGui(SqlIdModel model) {
    this.model = model;
  }

  void computeSqlId() {
    // TODO disable GUI
    // TODO enable GUI
    SwingWorker<String, String> task = this.model.computeSqlIdWworker();
    task.addPropertyChangeListener(event -> {
      if (event.getPropertyName().equals("state") && (event.getNewValue() == StateValue.DONE)) {
        try {
          String sqlId = task.get();
          SwingUtilities.invokeLater(() -> SqlIdGui.this.setSqlId(sqlId));
        } catch (InterruptedException e) {
          // should not happen as we got the DONE event
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          SwingUtilities.invokeLater(() -> SqlIdGui.this.showException(e.getCause()));
        }
      }
    });
    task.execute();
  }

  void setSqlId(String sqlId) {
    this.sqlIdField.setText(sqlId);
  }

  void showException(Throwable exception) {
    JOptionPane.showMessageDialog(this.frame, exception.getMessage(), "SQL Exception", JOptionPane.ERROR_MESSAGE);
  }

  private void setNativeSql(boolean isNativeSql) {
    this.setModelProperty(isNativeSql, this.model::setNativeSql);
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

  private void setQuery(String password) {
    this.setModelProperty(password, this.model::setQuery);
  }

  private void addListener(JTextComponent textComponent, Consumer<? super String> setter) {
    textComponent.setDocument(new PlainDocument(new StringContent()));
    textComponent.getDocument().addDocumentListener(new DocumentListener() {

      @Override
      public void removeUpdate(DocumentEvent e) {
        setter.accept(textComponent.getText());
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        setter.accept(textComponent.getText());
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        setter.accept(textComponent.getText());
      }
    });
  }

  private <V> void setModelProperty(V value, Consumer<? super V> setter) {
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
    this.frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        SqlIdGui.this.frame.dispose();
        flushAndExit(SqlIdGui.this.model);
      }
    });

    Container contentPane = this.frame.getContentPane();

    this.addTextArea(contentPane);

    contentPane.add(this.createBottomPanel(), BorderLayout.PAGE_END);

    this.loadModelState();

    this.frame.pack();
    this.frame.setMinimumSize(this.frame.getSize());

    this.frame.setVisible(true);
  }

  private void loadModelState() {
    this.urlField.setText(this.model.getUrl());
    this.userField.setText(this.model.getUser());
    this.passwordField.setText(this.model.getPassword());
    this.setComputeEnabled(this.model.isValid());
  }

  private static void flushAndExit(SqlIdModel model) {
    Thread flusher = new Thread(() -> {
      try {
        model.flush();
      } catch (BackingStoreException e) {
        e.printStackTrace(System.err);
      }
      System.exit(0);
    });
    flusher.start();
  }

  private void addTextArea(Container container) {
    JTextArea textArea = new JTextArea(40, 80);
    this.addListener(textArea, this::setQuery);
    JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    container.add(scrollPane, BorderLayout.CENTER);
  }

  private JPanel createBottomPanel() {
    JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    bottomPanel.add(this.createConnectionPanel(), BorderLayout.LINE_START);
    bottomPanel.add(this.createRightPanel(), BorderLayout.LINE_END);
    return bottomPanel;
  }

  private JPanel createRightPanel() {
    JPanel rightPanel = new JPanel(new GridBagLayout());

    rightPanel.add(this.createNativeCheckbox(), this.createNativeCheckboxConstraints());
    rightPanel.add(this.createSqlIdLabelField(), this.createSqlIdLabelConstaints());
    rightPanel.add(this.createSqlIdField(), this.createSqlIdConstaints());
    rightPanel.add(this.createComputeButton(), this.createComputeButtonConstraints());

    return rightPanel;
  }

  private JTextField createSqlIdField() {
    this.sqlIdField = new JTextField(13);
    this.sqlIdField.setEditable(false);
    return this.sqlIdField;
  }

  private JLabel createSqlIdLabelField() {
    return new JLabel("SQL_ID:");
  }

  private JCheckBox createNativeCheckbox() {
    JCheckBox nativeCheckbox = new JCheckBox("Is native SQL");
    nativeCheckbox.addActionListener(event -> this.setNativeSql(nativeCheckbox.isSelected()));
    return nativeCheckbox;
  }

  private JButton createComputeButton() {
    this.computeButton = new JButton("Compute SQL_ID");
    this.computeButton.addActionListener(e -> this.computeSqlId());
    return this.computeButton;
  }

  private GridBagConstraints createNativeCheckboxConstraints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 0;
//    constraints.gridwidth = 2;
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.insets = new Insets(0, 0, INSETS, 0);
    return constraints;
  }

  private GridBagConstraints createSqlIdConstaints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.insets = new Insets(0, 0, INSETS, 0);
    return constraints;
  }

  private GridBagConstraints createSqlIdLabelConstaints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.anchor = GridBagConstraints.LINE_END;
    constraints.insets = new Insets(0, 0, INSETS, INSETS);
    return constraints;
  }

  private GridBagConstraints createComputeButtonConstraints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.gridwidth = 2;
    constraints.anchor = GridBagConstraints.LINE_END;
    constraints.insets = new Insets(0, 0, INSETS, 0);
    return constraints;
  }

  private JPanel createConnectionPanel() {
    JPanel connectionPanel = new JPanel(new GridBagLayout());
//    connectionPanel.setBorder(BorderFactory.createTitledBorder("Connection"));
    GridBagConstraints constraints;

    String[] labels = {"URL:", "User:", "Password:"};
    for (int i = 0; i < labels.length; i++) {
      connectionPanel.add(new JLabel(labels[i]), this.connectionLabelConstraints(i));
    }

    constraints = this.connectionFieldConstraints(0);
    this.urlField = new JTextField(32);
    this.addListener(this.urlField, this::setUrl);
    connectionPanel.add(this.urlField, constraints);

    constraints = this.connectionFieldConstraints(1);
    this.userField = new JTextField(32);
    this.addListener(this.userField, this::setUser);
    connectionPanel.add(this.userField, constraints);

    constraints = this.connectionFieldConstraints(2);
    this.passwordField = new JPasswordField(32);
    this.addListener(this.passwordField, this::setPassword);
    connectionPanel.add(this.passwordField, constraints);

    return connectionPanel;
  }

  private GridBagConstraints connectionLabelConstraints(int i) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = i;
    constraints.anchor = GridBagConstraints.LINE_END;
    constraints.insets = new Insets(0, 0, INSETS, INSETS);
    return constraints;
  }

  private GridBagConstraints connectionFieldConstraints(int i) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = i;
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.insets = new Insets(0, 0, INSETS, 0);
    return constraints;
  }

}
