import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class MultithreadedFlowchartEditor {
    private static final int MAX_THREADS = 100;
    private static final int MAX_BLOCKS = 100;
    private static final int MAX_VARS = 100;

    private JFrame mainFrame;
    private JTabbedPane tabbedPane;
    private List<FlowchartPanel> flowchartPanels;
    private JTextField threadCountField;
    private JButton loadButton;
    private JButton generateCodeButton;
    private JButton testButton;

    private List<String> sharedVariables;
    private JComboBox<String> variableComboBox;
    private JTextField newVariableField;

    private JTextArea codeOutputArea;
    private JTextArea testOutputArea;
//
    private JButton addTestCaseButton;
    private JButton runTestsButton;

    public MultithreadedFlowchartEditor() {
        sharedVariables = new ArrayList<>();
        //
        initializeUI();
    }
}

private void initializeUI() {
    mainFrame = new JFrame("Редактор блок-схем з генерацією коду");
    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setSize(1200, 800);
    mainFrame.setLayout(new BorderLayout());

    initializeThreadControls();
    initializeVariableControls();
    initializeMainWorkspace();
    initializeOutputAreas();
    initializeActionButtons();

    addThread();
    mainFrame.setVisible(true);
}

private void initializeThreadControls() {
    JPanel topPanel = new JPanel(new FlowLayout());
    threadCountField = new JTextField("1", 3);
    JButton addThreadButton = new JButton("Додати потік");
    JButton removeThreadButton = new JButton("Видалити потік");

    addThreadButton.addActionListener(e -> addThread());
    removeThreadButton.addActionListener(e -> removeThread());

    topPanel.add(new JLabel("Потоки:"));
    topPanel.add(threadCountField);
    topPanel.add(addThreadButton);
    topPanel.add(removeThreadButton);

    mainFrame.add(topPanel, BorderLayout.NORTH);
}

private void initializeVariableControls() {
    variableComboBox = new JComboBox<>();
    newVariableField = new JTextField(10);
    JButton addVariableButton = new JButton("Додати змінну");
    JButton deleteVariableButton = new JButton("Видалити змінну");

    // Панель зі змінними
    JPanel varPanel = new JPanel(new FlowLayout());
    varPanel.add(new JLabel("Спільні змінні:"));
    varPanel.add(variableComboBox);
    varPanel.add(new JLabel("Нова:"));
    varPanel.add(newVariableField);
    varPanel.add(addVariableButton);
    varPanel.add(deleteVariableButton);

    // Кнопка згортання/розгортання панелі
    JButton toggleButton = new JButton("⯆ Сховати панель");
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(toggleButton, BorderLayout.NORTH);
    wrapper.add(varPanel, BorderLayout.CENTER);
    mainFrame.add(wrapper, BorderLayout.EAST);

    // Обробка додавання змінної
    addVariableButton.addActionListener(e -> addVariable());

    // Обробка видалення змінної
    deleteVariableButton.addActionListener(e -> {
        String selectedVar = (String) variableComboBox.getSelectedItem();
        if (selectedVar != null) {
            int confirm = JOptionPane.showConfirmDialog(mainFrame,
                    "Ви впевнені, що хочете видалити змінну '" + selectedVar + "'?",
                    "Підтвердження видалення",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                sharedVariables.remove(selectedVar);
                variableComboBox.removeItem(selectedVar);
                for (FlowchartPanel panel : flowchartPanels) {
                    panel.updateVariableList(sharedVariables);
                }
            }
        }
    });

    // Обробка згортання/розгортання
    toggleButton.addActionListener(e -> {
        boolean isVisible = varPanel.isVisible();
        varPanel.setVisible(!isVisible);
        toggleButton.setText(isVisible ? "⯈ Показати панель" : "⯆ Сховати панель");
        mainFrame.revalidate();
    });
}



private void initializeMainWorkspace() {
    tabbedPane = new JTabbedPane();
    flowchartPanels = new ArrayList<>();
    mainFrame.add(tabbedPane, BorderLayout.CENTER);
}

private void initializeOutputAreas() {
    codeOutputArea = new JTextArea(10, 80);
    codeOutputArea.setEditable(false);
    codeOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    JScrollPane codeScrollPane = new JScrollPane(codeOutputArea);

    testOutputArea = new JTextArea(10, 80);
    testOutputArea.setEditable(false);
    JScrollPane testScrollPane = new JScrollPane(testOutputArea);

    JPanel outputPanel = new JPanel(new GridLayout(2, 1));
    outputPanel.add(codeScrollPane);
    outputPanel.add(testScrollPane);
    mainFrame.add(outputPanel, BorderLayout.SOUTH);
}

private void initializeActionButtons() {
    JPanel bottomPanel = new JPanel(new FlowLayout());
    JButton saveButton = new JButton("Зберегти проект");
    loadButton = new JButton("Завантажити проект");
    generateCodeButton = new JButton("Згенерувати код");
    testButton = new JButton("Тестувати");
    //
    saveButton.addActionListener(e -> saveProject());
    loadButton.addActionListener(e -> loadProject());
    generateCodeButton.addActionListener(e -> generatePythonCode());
    testButton.addActionListener(e -> openTestDialog());

    //
    bottomPanel.add(saveButton);
    bottomPanel.add(loadButton);
    bottomPanel.add(generateCodeButton);
    bottomPanel.add(testButton);

    mainFrame.add(bottomPanel, BorderLayout.PAGE_END);
}
private void addThread() {
    try {
        int count = Integer.parseInt(threadCountField.getText());
        if (count < 1 || count > MAX_THREADS) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Кількість потоків повинна бути від 1 до " + MAX_THREADS,
                    "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        while (flowchartPanels.size() < count) {
            FlowchartPanel panel = new FlowchartPanel(sharedVariables);
            flowchartPanels.add(panel);
            tabbedPane.addTab("Потік " + (flowchartPanels.size()), panel);
        }

        threadCountField.setText(String.valueOf(flowchartPanels.size()));
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(mainFrame,
                "Будь ласка, введіть коректне число",
                "Помилка", JOptionPane.ERROR_MESSAGE);
    }
}

private void removeThread() {
    int selectedIndex = tabbedPane.getSelectedIndex();

    if (selectedIndex == -1 || flowchartPanels.isEmpty()) {
        JOptionPane.showMessageDialog(mainFrame,
                "Немає потоків для видалення.",
                "Помилка", JOptionPane.ERROR_MESSAGE);
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(mainFrame,
            "Ви впевнені, що хочете видалити потік №" + (selectedIndex + 1) + "?",
            "Підтвердження видалення потоку",
            JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
        tabbedPane.remove(selectedIndex);
        flowchartPanels.remove(selectedIndex);

        // Перейменувати всі вкладки після видалення
        for (int i = 0; i < flowchartPanels.size(); i++) {
            tabbedPane.setTitleAt(i, "Потік " + (i + 1));
        }

        threadCountField.setText(String.valueOf(flowchartPanels.size()));
    }
}


private void addVariable() {
    String varName = newVariableField.getText().trim();
    if (varName.isEmpty()) {
        JOptionPane.showMessageDialog(mainFrame,
                "Назва змінної не може бути порожньою",
                "Помилка", JOptionPane.ERROR_MESSAGE);
        return;
    }

    if (sharedVariables.size() >= MAX_VARS) {
        JOptionPane.showMessageDialog(mainFrame,
                "Досягнуто максимальної кількості змінних (" + MAX_VARS + ")",
                "Помилка", JOptionPane.ERROR_MESSAGE);
        return;
    }

    if (!sharedVariables.contains(varName)) {
        sharedVariables.add(varName);
        variableComboBox.addItem(varName);
        newVariableField.setText("");

        for (FlowchartPanel panel : flowchartPanels) {
            panel.updateVariableList(sharedVariables);
        }
    } else {
        JOptionPane.showMessageDialog(mainFrame,
                "Змінна з такою назвою вже існує",
                "Помилка", JOptionPane.ERROR_MESSAGE);
    }
}

private void saveProject() {
    JFileChooser fileChooser = new JFileChooser();
    if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {JOptionPane.showMessageDialog(mainFrame,
                "Проект успішно збережено",
                "Успіх", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Помилка збереження проекту: " + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}

private void loadProject() {
    JFileChooser fileChooser = new JFileChooser();
    if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            //

            sharedVariables.clear();
            variableComboBox.removeAllItems();
            flowchartPanels.clear();
            tabbedPane.removeAll();

            sharedVariables.addAll(data.getSharedVariables());
            for (String var : sharedVariables) {
                variableComboBox.addItem(var);
            }

            for (FlowchartData fd : data.getFlowchartData()) {
                FlowchartPanel panel = new FlowchartPanel(sharedVariables);
                panel.setFlowchartData(fd);
                flowchartPanels.add(panel);
                tabbedPane.addTab("Потік " + (flowchartPanels.size()), panel);
            }

            threadCountField.setText(String.valueOf(flowchartPanels.size()));
            JOptionPane.showMessageDialog(mainFrame,
                    "Проект успішно завантажено",
                    "Успіх", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Помилка завантаження проекту: " + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
//
private void openTestDialog() {
    JDialog testDialog = new JDialog(mainFrame, "Тестові випадки", true);
    testDialog.setSize(600, 550);
    testDialog.setLayout(new BorderLayout());

    DefaultListModel<TestCase> listModel = new DefaultListModel<>();
    for (TestCase tc : testCases) listModel.addElement(tc);
    JList<TestCase> testList = new JList<>(listModel);
    JScrollPane listScrollPane = new JScrollPane(testList);

    JPanel ioPanel = new JPanel(new GridLayout(3, 1));
    JTextArea inputArea = new JTextArea(3, 40);
    JTextArea outputArea = new JTextArea(3, 40);
    JTextField kField = new JTextField("5");

    JPanel kPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    kPanel.add(new JLabel("K (кількість варіантів):"));
    kPanel.add(kField);

    ioPanel.add(new JScrollPane(inputArea));
    ioPanel.add(new JScrollPane(outputArea));
    ioPanel.add(kPanel);

    JPanel buttonPanel = new JPanel(new FlowLayout());
    JButton addTestCaseButton = new JButton("Додати тест");
    JButton runTestsButton = new JButton("K-випробування");
    JButton closeButton = new JButton("Закрити");

    addTestCaseButton.addActionListener(e -> {
        String input = inputArea.getText().trim();
        String output = outputArea.getText().trim();
        if (!input.isEmpty() && !output.isEmpty()) {
            TestCase tc = new TestCase(input, output);
            testCases.add(tc);
            listModel.addElement(tc);
            inputArea.setText("");
            outputArea.setText("");
        }
    });

    runTestsButton.addActionListener(e -> {
        try {
            int K = Integer.parseInt(kField.getText().trim());
            if (K < 1 || K > 20) throw new NumberFormatException();
            runKTests(K);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(testDialog, "K повинно бути числом від 1 до 20", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    });

    closeButton.addActionListener(e -> testDialog.dispose());

    buttonPanel.add(addTestCaseButton);
    buttonPanel.add(runTestsButton);
    buttonPanel.add(closeButton);

    testDialog.add(listScrollPane, BorderLayout.CENTER);
    testDialog.add(ioPanel, BorderLayout.NORTH);
    testDialog.add(buttonPanel, BorderLayout.SOUTH);

    testDialog.setVisible(true);
}
//
