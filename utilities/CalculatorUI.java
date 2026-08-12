package com.AtomCAS.utilities;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CalculatorUI {
    private final JFrame frame;
    private final JTextArea inputLayer;
    private final JTextArea outputLayer = new JTextArea();

    private final JTree traceTree = new JTree();
    private final JTextArea traceDetails = new JTextArea();

    private final TraceWindow traceWindow = new TraceWindow();
    private final JCheckBox traceCheckBox = new JCheckBox("Show trace");
    public CalculatorUI() {
        frame = new JFrame("Calculator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);
        frame.setLayout(new BorderLayout());
        inputLayer = new JTextArea("", 4, 30);
        inputLayer.setLineWrap(true);
        inputLayer.setWrapStyleWord(false);
        inputLayer.setEditable(true);
        inputLayer.setFont(new Font("Arial", Font.PLAIN, 15));

        JScrollPane scrollPane = new JScrollPane(inputLayer);
        frame.add(scrollPane, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 7, 10, 5));

        String[] buttons = {
                "7", "8", "9", "/", "C", "(", ")",
                "4", "5", "6", "*", "^", "[", "]",
                "1", "2", "3", "-", "mod", "{", "}",
                "0", ".", "+", "del", "√", "%", "=",
                "‰", "!", "gcd", "lcm", "factor", "totient", "≈"
        };

        for (String text : buttons) {
            JButton button = new JButton(text);
            button.setFont(new Font("TimesNewRoman", Font.PLAIN, 10));
            button.addActionListener(new ButtonClickListener());
            buttonPanel.add(button);
        }

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(traceCheckBox, BorderLayout.SOUTH);

        traceWindow.setSize(1200, 800);
        traceWindow.setLocationRelativeTo(frame);

        frame.add(bottomPanel, BorderLayout.CENTER);
        traceTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selected =
                    (DefaultMutableTreeNode) traceTree.getLastSelectedPathComponent();
            if (selected == null) return;

            Object obj = selected.getUserObject();
            if (obj instanceof LogNode node)
                traceDetails.setText(node.detailText());
        });
        JSplitPane tracePane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(traceTree),
                new JScrollPane(traceDetails)
        );
        traceWindow.add(tracePane, BorderLayout.CENTER);
        frame.setVisible(true);
    }
    private DefaultMutableTreeNode toSwingNode(LogNode node) {
        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(node);

        for (LogNode child : node.children())
            swingNode.add(toSwingNode(child));

        return swingNode;
    }
    private class ButtonClickListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                outputLayer.removeAll();
                String command = ((JButton)e.getSource()).getText();
                if (command.matches("[0123456789.+\\-*/^()\\[\\]{}√%‰!]") || command.equals("mod"))
                    inputLayer.setText(inputLayer.getText() + command);
                else if (command.equals("=")) {
                    String input = inputLayer.getText();

                    try {
                        if (traceCheckBox.isSelected())
                            CASTrace.enable();
                        else
                            CASTrace.disable();

                        ScalarAtom result = Parser.parse(input);

                        inputLayer.setText(result.toString());

                        if (traceCheckBox.isSelected()) {
                            LogNode root = CASTrace.root();
                            traceWindow.showTrace(root);
                        }

                    } catch (Exception ex) {
                        inputLayer.setText(ex.getMessage());

                        if (traceCheckBox.isSelected()) {
                            LogNode root = CASTrace.root();
                            traceWindow.showTrace(root);
                        }
                    }
                }else if (command.equals("≈")) {
                    try {
                        ScalarAtom result = Parser.parse(inputLayer.getText()).approximateToDecimal(16);
                        outputLayer.setText(result.toString());
                    } catch (Exception ex) {inputLayer.setText(ex.getMessage());}
                }else if(command.equals("del")){
                    if(inputLayer.getText().length() > 1)
                        inputLayer.setText(inputLayer.getText().substring(0, inputLayer.getText().length()-1));
                    else inputLayer.setText("");
                }
                else if (command.equals("C"))
                    inputLayer.setText("");
            } catch (Exception ex){JOptionPane.showMessageDialog(frame, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);}
        }
    }
    static void main(String[] args) {SwingUtilities.invokeLater(CalculatorUI::new);}
}
class TraceWindow extends JFrame {
    private final JTree tree;
    private final JTextArea details;

    public TraceWindow() {
        super("CAS Trace Log");

        tree = new JTree(new DefaultMutableTreeNode("No trace yet"));

        details = new JTextArea();
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);

        JScrollPane treeScroll = new JScrollPane(tree);
        JScrollPane detailsScroll = new JScrollPane(details);

        treeScroll.setPreferredSize(new Dimension(450, 700));
        detailsScroll.setPreferredSize(new Dimension(500, 700));

        treeScroll.setMinimumSize(new Dimension(250, 400));
        detailsScroll.setMinimumSize(new Dimension(250, 400));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                treeScroll,
                detailsScroll
        );

        splitPane.setResizeWeight(0.45);
        splitPane.setDividerLocation(450);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);

        setSize(1000, 750);
        setMinimumSize(new Dimension(700, 500));
        setLocationByPlatform(true);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    public void showTrace(LogNode root) {
        if (root == null) {
            tree.setModel(new DefaultTreeModel(
                    new DefaultMutableTreeNode("No trace available")
            ));
            details.setText("");
        } else {
            DefaultMutableTreeNode swingRoot = toSwingNode(root);
            tree.setModel(new DefaultTreeModel(swingRoot));

            details.setText(root.detailText());

            for (int i = 0; i < Math.min(tree.getRowCount(), 8); i++)
                tree.expandRow(i);
        }

        tree.revalidate();
        tree.repaint();
        details.revalidate();
        details.repaint();

        if (!isVisible())
            setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
    }

    private DefaultMutableTreeNode toSwingNode(LogNode node) {
        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(node);

        for (LogNode child : node.children())
            swingNode.add(toSwingNode(child));

        return swingNode;
    }
}
class GraphPanel extends JPanel {
    public GraphPanel() {setBackground(Color.WHITE);}
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        drawGrid(g2, width, height, centerX, centerY);
        drawAxes(g2, width, height, centerX, centerY);
    }

    private void drawGrid(Graphics2D g2, int width, int height, int centerX, int centerY) {
        g2.setColor(new Color(220, 220, 220));
        g2.setStroke(new BasicStroke(1));

        // vertical grid lines
        // pixels per grid unit
        int scale = 10;
        for (int x = centerX; x < width; x += scale)
            g2.drawLine(x, 0, x, height);
        for (int x = centerX- scale; x > 0; x -= scale)
            g2.drawLine(x, 0, x, height);

        // horizontal grid lines
        for (int y = centerY; y < height; y += scale)
            g2.drawLine(0, y, width, y);

        for (int y = centerY; y > 0; y -= scale)
            g2.drawLine(0, y, width, y);
        //labels
        Font small = new Font("Arial", Font.PLAIN, 5);
        g2.setColor(Color.BLACK);
        g2.setFont(small);
        FontMetrics fm = g2.getFontMetrics();
        int labelY = centerY-3;
        for (int x = centerX; x < width; x += scale) {
            int value = (x - centerX) / scale;
            String text = String.valueOf(value);
            int textWidth = fm.stringWidth(text);
            g2.drawString(text, x - textWidth / 2, labelY);
        }

        for (int x = centerX - scale; x >= 0; x -= scale) {
            int value = (x - centerX) / scale;
            String text = String.valueOf(value);
            int textWidth = fm.stringWidth(text);
            g2.drawString(text, x - textWidth / 2, labelY);
        }
    }

    private void drawAxes(Graphics2D g2, int width, int height, int centerX, int centerY) {
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));

        // x-axis
        g2.drawLine(0, centerY, width, centerY);

        // y-axis
        g2.drawLine(centerX, 0, centerX, height);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("2D Graph Grid");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(new GraphPanel());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}