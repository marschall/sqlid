package com.github.marschall.sqlid.gui;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public final class SqlIdGui {

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
      System.err.println("System LaF not supported");
    }
    SwingUtilities.invokeLater(SqlIdGui::createAndShowGui);
  }

  private static void createAndShowGui() {
    JFrame frame = new JFrame("SQL_ID GUI");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Container contentPane = frame.getContentPane();

    addTextArea(contentPane);

    contentPane.add(new JButton("Compute SQL_ID"), BorderLayout.PAGE_END);

    frame.pack();

    frame.setVisible(true);
  }

  private static void addTextArea(Container container) {
    JTextArea textArea = new JTextArea(40, 80);
    JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    container.add(scrollPane, BorderLayout.CENTER);
  }

}
