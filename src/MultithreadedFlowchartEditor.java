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

    private List<TestCase> testCases;
    private JButton addTestCaseButton;
    private JButton runTestsButton;

    public MultithreadedFlowchartEditor() {
        sharedVariables = new ArrayList<>();
        testCases = new ArrayList<>();
        initializeUI();
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

    JButton runKButton = new JButton("K-випробування");
    runKButton.addActionListener(e -> {
        String input = JOptionPane.showInputDialog(mainFrame, "Скільки виконань (K, від 1 до 20)?", "10");
        try {
            int k = Integer.parseInt(input);
            if (k >= 1 && k <= 20) {
                runKTests(k);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "K повинно бути в межах 1..20", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(mainFrame, "Некоректне число", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    });

    saveButton.addActionListener(e -> saveProject());
    loadButton.addActionListener(e -> loadProject());
    generateCodeButton.addActionListener(e -> generatePythonCode());
    testButton.addActionListener(e -> openTestDialog());

    bottomPanel.add(runKButton);
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
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                ProjectData data = new ProjectData();
                data.setSharedVariables(new ArrayList<>(sharedVariables));
                data.setFlowchartData(new ArrayList<>());

                for (FlowchartPanel panel : flowchartPanels) {
                    data.getFlowchartData().add(panel.getFlowchartData());
                }

                oos.writeObject(data);
                JOptionPane.showMessageDialog(mainFrame,
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
            ProjectData data = (ProjectData) ois.readObject();

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

private void generatePythonCode() {
    for (int i = 0; i < flowchartPanels.size(); i++) {
        if (flowchartPanels.get(i).findStartBlock() == null) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Потік " + (i+1) + " не має стартового блоку!",
                    "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    StringBuilder pythonCode = new StringBuilder();
    pythonCode.append("import threading\n\n");

    for (String var : sharedVariables) {
        pythonCode.append(var).append(" = 0\n");
    }
    pythonCode.append("lock = threading.Lock()\n\n");

    for (int i = 0; i < flowchartPanels.size(); i++) {
        pythonCode.append("def thread_").append(i+1).append("():\n");
        String threadCode = flowchartPanels.get(i).generatePythonCode();
        threadCode = threadCode.replaceAll("(?m)^", "    ");
        pythonCode.append(threadCode).append("\n\n");
    }

    pythonCode.append("if __name__ == '__main__':\n");
    for (int i = 0; i < flowchartPanels.size(); i++) {
        pythonCode.append("    threading.Thread(target=thread_").append(i+1).append(").start()\n");
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Зберегти Python файл");
    fileChooser.setSelectedFile(new File("generated_code.py"));
    fileChooser.setFileFilter(new FileNameExtensionFilter("Python файли", "py"));

    if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".py")) {
            file = new File(file.getParentFile(), file.getName() + ".py");
        }

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(pythonCode.toString());
            JOptionPane.showMessageDialog(mainFrame,
                    "Python код успішно збережено у файл: " + file.getAbsolutePath(),
                    "Успіх", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Помилка збереження файлу: " + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}

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


private void runTests() {
    if (testCases.isEmpty()) {
        JOptionPane.showMessageDialog(mainFrame, "Не знайдено тестових випадків");
        return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Виберіть Python-файл для тестування");
    int result = fileChooser.showOpenDialog(mainFrame);
    if (result != JFileChooser.APPROVE_OPTION) return;

    File file = fileChooser.getSelectedFile();
    if (!file.exists()) {
        JOptionPane.showMessageDialog(mainFrame, "Файл не знайдено!", "Помилка", JOptionPane.ERROR_MESSAGE);
        return;
    }

    StringBuilder testResults = new StringBuilder();
    int passed = 0;

    for (TestCase tc : testCases) {
        testResults.append("Тестовий випадок:\n");
        testResults.append("Вхідні дані: ").append(tc.getInput()).append("\n");
        testResults.append("Очікуваний результат: ").append(tc.getExpectedOutput()).append("\n");

        try {
            Process runProcess = Runtime.getRuntime().exec("python \"" + file.getAbsolutePath() + "\"");

            try (OutputStream stdin = runProcess.getOutputStream();
                 PrintWriter writer = new PrintWriter(stdin)) {
                writer.println(tc.getInput());
                writer.flush();
            }

            StringBuilder outputBuilder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(runProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line.trim()).append("\n");
                }
            }

            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(runProcess.getErrorStream()))) {
                String errLine;
                while ((errLine = errorReader.readLine()) != null) {
                    outputBuilder.append(errLine.trim()).append("\n");
                }
            }

            runProcess.waitFor();  // Чекаємо завершення

            String actualOutput = outputBuilder.toString().trim();
            testResults.append("Фактичний результат: ").append(actualOutput).append("\n");

            boolean success = actualOutput.contains(tc.getExpectedOutput().trim());

            testResults.append("Результат: ").append(success ? "✔ ПРОЙДЕНО" : "✘ НЕ ПРОЙДЕНО").append("\n\n");
            if (success) passed++;

        } catch (IOException | InterruptedException e) {
            testResults.append("Помилка запуску тесту: ").append(e.getMessage()).append("\n\n");
        }
    }

    testResults.append("Підсумок: ").append(passed).append("/").append(testCases.size()).append(" тестів пройдено");

    JTextArea textArea = new JTextArea(testResults.toString());
    textArea.setEditable(false);
    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setPreferredSize(new Dimension(600, 400));

    JOptionPane.showMessageDialog(mainFrame, scrollPane, "Результати тестування", JOptionPane.INFORMATION_MESSAGE);
}

private void runKTests(int K) {
    if (testCases.isEmpty()) {
        JOptionPane.showMessageDialog(mainFrame, "Не знайдено тестових випадків");
        return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Виберіть Python-файл для тестування");
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION) return;

    File file = fileChooser.getSelectedFile();
    if (!file.exists()) {
        JOptionPane.showMessageDialog(mainFrame, "Файл не знайдено!", "Помилка", JOptionPane.ERROR_MESSAGE);
        return;
    }

    StringBuilder testResults = new StringBuilder();

    for (TestCase tc : testCases) {
        testResults.append("Тестовий випадок:\n");
        testResults.append("Вхідні дані: ").append(tc.getInput()).append("\n");
        testResults.append("Очікуваний результат: ").append(tc.getExpectedOutput()).append("\n");

        int passed = 0;
        int totalRun = 0;

        for (int i = 0; i < K; i++) {
            try {
                Process runProcess = Runtime.getRuntime().exec("python \"" + file.getAbsolutePath() + "\"");

                try (OutputStream stdin = runProcess.getOutputStream();
                     PrintWriter writer = new PrintWriter(stdin)) {
                    writer.println(tc.getInput());
                    writer.flush();
                }

                StringBuilder outputBuilder = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(runProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line.trim()).append("\n");
                    }
                }

                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(runProcess.getErrorStream()))) {
                    String errLine;
                    while ((errLine = errorReader.readLine()) != null) {
                        outputBuilder.append(errLine.trim()).append("\n");
                    }
                }

                runProcess.waitFor();
                totalRun++;

                String actualOutput = outputBuilder.toString().trim();
                boolean success = actualOutput.contains(tc.getExpectedOutput().trim());

                testResults.append("Варіант ").append(i + 1).append(": ").append(success ? "✔ ПРОЙДЕНО" : "✘ НЕ ПРОЙДЕНО").append("\n");
                if (success) passed++;

            } catch (IOException | InterruptedException e) {
                testResults.append("Помилка запуску: ").append(e.getMessage()).append("\n");
            }
        }

        double percentage = (double) passed / totalRun * 100.0;
        testResults.append("Успішних виконань: ").append(passed).append("/").append(totalRun).append(" (")
                .append(String.format("%.2f", percentage)).append("%)").append("\n\n");
    }

    JTextArea textArea = new JTextArea(testResults.toString());
    textArea.setEditable(false);
    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setPreferredSize(new Dimension(600, 400));

    JOptionPane.showMessageDialog(mainFrame, scrollPane, "Результати K-випробувань", JOptionPane.INFORMATION_MESSAGE);
}

public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        MultithreadedFlowchartEditor editor = new MultithreadedFlowchartEditor();
        editor.mainFrame.setTitle("Редактор блок-схем з генерацією Python коду");
    });
}
}
class FlowchartPanel extends JPanel {
    private List<Block> blocks;
    private List<Connection> connections;
    private List<String> sharedVariables;
    private Block selectedBlock;
    private Point dragStart;

    public FlowchartPanel(List<String> sharedVariables) {
        this.sharedVariables = sharedVariables;
        blocks = new ArrayList<>();
        connections = new ArrayList<>();
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
        });
    }

    public void updateVariableList(List<String> variables) {
        this.sharedVariables = variables;
        repaint();
    }

    public FlowchartData getFlowchartData() {
        FlowchartData data = new FlowchartData();
        data.setBlocks(new ArrayList<>(blocks));
        data.setConnections(new ArrayList<>(connections));
        return data;
    }

    public void setFlowchartData(FlowchartData data) {
        this.blocks = new ArrayList<>(data.getBlocks());
        this.connections = new ArrayList<>(data.getConnections());
        repaint();
    }

    public String generatePythonCode() {
        StringBuilder code = new StringBuilder();
        Block startBlock = findStartBlock();

        if (startBlock == null) {
            return "# Не знайдено стартового блоку\n";
        }

        Set<Block> visited = new HashSet<>();
        generateBlockPythonCode(startBlock, code, visited, 0);

        return code.toString();
    }

    private void generateBlockPythonCode(Block block, StringBuilder code, Set<Block> visited, int indentLevel) {
        if (block == null || visited.contains(block)) return;

        String indent = "    ".repeat(indentLevel);
        String sanitizedCode = block.getCode() != null ? block.getCode().replace("\n", "").trim() : "";

        switch (block.getType()) {
            case START:
                code.append(indent).append("# Початок потоку\n");
                break;

            case INPUT:
                code.append(indent)
                        .append(sanitizedCode)
                        .append(" = int(input('Введіть значення для ")
                        .append(sanitizedCode)
                        .append(": '))\n");
                break;

            case ASSIGNMENT:
                if (sanitizedCode.contains("=") && !sanitizedCode.contains("==")) {
                    code.append(indent).append(sanitizedCode).append("\n");
                } else {
                    code.append(indent).append("# [ПОМИЛКА] Некоректне присвоєння: ").append(sanitizedCode).append("\n");
                }
                break;

            case OUTPUT:
                String rawCode = block.getCode().trim();

                // Якщо це ім'я змінної зі спільного списку — виводимо її значення через f-string
                if (sharedVariables.contains(rawCode) && rawCode.matches("[a-zA-Z_][a-zA-Z_0-9]*")) {
                    code.append(indent).append("print(f'").append(rawCode).append(" = {").append(rawCode).append("}')\n");
                } else {
                    // Інакше — просто текст
                    rawCode = rawCode.replace("\\", "\\\\").replace("'", "\\'");
                    code.append(indent).append("print('").append(rawCode).append("')\n");
                }
                break;




            case CONDITION:
                code.append(indent).append("if ").append(sanitizedCode).append(":\n");

                Connection trueConn = findConnectionFrom(block, true);
                Connection falseConn = findConnectionFrom(block, false);

                Set<Block> visitedTrue = new HashSet<>(visited);
                Set<Block> visitedFalse = new HashSet<>(visited);

                if (trueConn != null) {
                    generateBlockPythonCode(trueConn.getTo(), code, visitedTrue, indentLevel + 1);
                } else {
                    code.append(indent).append("    pass\n");
                }

                code.append(indent).append("else:\n");

                if (falseConn != null) {
                    generateBlockPythonCode(falseConn.getTo(), code, visitedFalse, indentLevel + 1);
                } else {
                    code.append(indent).append("    pass\n");
                }

                Block afterIf = findCommonContinuation(trueConn, falseConn);
                if (afterIf != null && !visited.contains(afterIf)) {
                    generateBlockPythonCode(afterIf, code, visited, indentLevel);
                }
                return;

            case END:
                code.append(indent).append("# Кінець потоку\n");
                return;
        }

        visited.add(block);
        Connection conn = findConnectionFrom(block, null);
        if (conn != null) {
            generateBlockPythonCode(conn.getTo(), code, visited, indentLevel);
        }
    }

private Block findCommonContinuation(Connection trueConn, Connection falseConn) {
    if (trueConn == null || falseConn == null) return null;
    Block trueTarget = trueConn.getTo();
    Block falseTarget = falseConn.getTo();

    if (trueTarget == null || falseTarget == null) return null;

    // BFS до кінців з кожної гілки
    Set<Block> trueReachable = collectReachable(trueTarget);
    Set<Block> falseReachable = collectReachable(falseTarget);

    // Знайти перший спільний
    for (Block b : trueReachable) {
        if (falseReachable.contains(b)) return b;
    }
    return null;
}

    private Set<Block> collectReachable(Block start) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            if (!visited.add(current)) continue;

            Connection conn = findConnectionFrom(current, null);
            if (conn != null && conn.getTo() != null) {
                queue.add(conn.getTo());
            }
        }
        return visited;
    }


    private Connection findConnectionFrom(Block from, Boolean condition) {
        for (Connection conn : connections) {
            if (conn.getFrom() == from) {
                if (condition == null && conn.condition == null) return conn;
                if (condition != null && conn.condition != null && conn.condition.equals(condition)) return conn;
            }
        }
        return null;
    }


    public Block findStartBlock() {
        for (Block block : blocks) {
            if (block.getType() == BlockType.START) {
                return block;
            }
        }
        return null;
    }

    private void handleMousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            showContextMenu(e.getPoint());
            return;
        }

        selectedBlock = null;
        for (Block block : blocks) {
            if (block.isNearOutput(e.getPoint())) {
                Connection newConn = new Connection(block, null);
                connections.add(newConn);
                selectedBlock = block;
                break;
            }
            if (block.contains(e.getPoint())) {
                selectedBlock = block;
                dragStart = e.getPoint();
                break;
            }
        }

        repaint();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (selectedBlock != null) {
            if (connections.stream().anyMatch(c -> c.getFrom() == selectedBlock && c.getTo() == null)) {
                Connection conn = connections.stream()
                        .filter(c -> c.getFrom() == selectedBlock && c.getTo() == null)
                        .findFirst().orElse(null);

                if (conn != null) {
                    conn.setDragPoint(e.getPoint());
                }
            } else {
                int dx = e.getX() - dragStart.x;
                int dy = e.getY() - dragStart.y;
                selectedBlock.move(dx, dy);
                dragStart = e.getPoint();
            }

            repaint();
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (selectedBlock != null) {
            Connection incompleteConn = connections.stream()
                    .filter(c -> c.getFrom() == selectedBlock && c.getTo() == null)
                    .findFirst().orElse(null);

            if (incompleteConn != null) {
                for (Block block : blocks) {
                    if (block != selectedBlock && block.contains(e.getPoint())) {
                        incompleteConn.setTo(block);

                        if (selectedBlock.getType() == BlockType.CONDITION) {
                            String[] options = {"True (істина)", "False (хиба)"};
                            int choice = JOptionPane.showOptionDialog(
                                    this,
                                    "Це перехід по гілці:",
                                    "Вибір гілки умови",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    options,
                                    options[0]
                            );
                            if (choice == 0) {
                                incompleteConn.setCondition(true);
                            } else if (choice == 1) {
                                incompleteConn.setCondition(false);
                            } else {
                                // Користувач натиснув "Закрити" — відмінити з’єднання
                                connections.remove(incompleteConn);
                                repaint();
                                return;
                            }
                        }


                        connections.removeIf(c ->
                                c != incompleteConn &&
                                        c.getFrom() == selectedBlock &&
                                        c.getTo() == block &&
                                        (selectedBlock.getType() != BlockType.CONDITION ||
                                                c.isCondition() == incompleteConn.isCondition()));

                        break;
                    }
                }

                if (incompleteConn.getTo() == null) {
                    connections.remove(incompleteConn);
                }
            }

            selectedBlock = null;
            repaint();
        }
    }

    private void showContextMenu(Point point) {
        JPopupMenu menu = new JPopupMenu();

        Block clickedBlock = blocks.stream().filter(block -> block.contains(point)).findFirst().orElse(null);

        if (clickedBlock != null) {
            JMenuItem editItem = new JMenuItem("Редагувати блок");
            editItem.addActionListener(e -> editBlock(clickedBlock));
            menu.add(editItem);

            JMenuItem deleteItem = new JMenuItem("Видалити блок");
            deleteItem.addActionListener(e -> {
                blocks.remove(clickedBlock);
                connections.removeIf(c -> c.getFrom() == clickedBlock || c.getTo() == clickedBlock);
                repaint();
            });
            menu.add(deleteItem);
        } else {
            for (BlockType type : BlockType.values()) {
                JMenuItem item = new JMenuItem("Додати " + type);
                item.addActionListener(e -> addNewBlock(type, point));
                menu.add(item);
            }
        }

        menu.show(this, point.x, point.y);
    }

    private void addNewBlock(BlockType type, Point location) {
        Block block = new Block(type, location.x, location.y);

        switch (type) {
            case ASSIGNMENT:
                if (!sharedVariables.isEmpty()) {
                    block.setCode(sharedVariables.get(0) + " = " + (sharedVariables.size() > 1 ? sharedVariables.get(1) : "0"));
                }
                break;
            case INPUT:
                if (!sharedVariables.isEmpty()) {
                    block.setCode(sharedVariables.get(0));
                }
                break;
            case OUTPUT:
                if (!sharedVariables.isEmpty()) {
                    block.setCode(sharedVariables.get(0));
                }
                break;
            case CONDITION:
                if (!sharedVariables.isEmpty()) {
                    block.setCode(sharedVariables.get(0) + " == 0");
                }
                break;
        }

        blocks.add(block);
        repaint();
    }

    private void editBlock(Block block) {
        JDialog editDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Редагування блоку", true);
        editDialog.setSize(400, 200);
        editDialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new BorderLayout());
        JLabel typeLabel = new JLabel("Тип: " + block.getType());
        contentPanel.add(typeLabel, BorderLayout.NORTH);

        if (block.getType() == BlockType.OUTPUT) {
            // 🔽 Додано спеціальне редагування для OUTPUT
            JComboBox<String> modeCombo = new JComboBox<>(new String[]{"Змінна", "Текст"});
            JTextField valueField = new JTextField(block.getCode() != null ? block.getCode() : "", 20);

            // Автоматичне визначення режиму
            String code = block.getCode();
            if (code != null && !code.trim().isEmpty()) {
                if (code.trim().matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    modeCombo.setSelectedItem("Змінна");
                } else {
                    modeCombo.setSelectedItem("Текст");
                }
            }

            JPanel outputPanel = new JPanel(new FlowLayout());
            outputPanel.add(new JLabel("Режим:"));
            outputPanel.add(modeCombo);
            outputPanel.add(new JLabel("Значення:"));
            outputPanel.add(valueField);

            JButton saveButton = new JButton("Зберегти");
            saveButton.addActionListener(e -> {
                block.setCode(valueField.getText().trim());
                editDialog.dispose();
                repaint();
            });

            contentPanel.add(outputPanel, BorderLayout.CENTER);
            contentPanel.add(saveButton, BorderLayout.SOUTH);
        }

        else if (block.getType() == BlockType.CONDITION || block.getType() == BlockType.ASSIGNMENT) {
            JPanel varPanel = new JPanel(new FlowLayout());
            JComboBox<String> varCombo = new JComboBox<>();

            for (String var : sharedVariables) {
                varCombo.addItem(var);
            }

            if (block.getType() == BlockType.CONDITION) {
                JComboBox<String> opCombo = new JComboBox<>(new String[]{"==", "<", ">"});
                JTextField valueField = new JTextField(10);

                String code = block.getCode();
                if (code != null && !code.isEmpty()) {
                    String[] parts = code.split("\\s+");
                    if (parts.length >= 3) {
                        varCombo.setSelectedItem(parts[0]);
                        opCombo.setSelectedItem(parts[1]);
                        valueField.setText(parts[2]);
                    }
                }

                JButton applyButton = new JButton("Застосувати");
                applyButton.addActionListener(e -> {
                    block.setCode(varCombo.getSelectedItem() + " " +
                            opCombo.getSelectedItem() + " " +
                            valueField.getText());
                    editDialog.dispose();
                    repaint();
                });

                varPanel.add(varCombo);
                varPanel.add(opCombo);
                varPanel.add(valueField);
                varPanel.add(applyButton);
            } else {
                JLabel equalsLabel = new JLabel("=");
                JTextField valueField = new JTextField(10);

                String code = block.getCode();
                if (code != null && !code.isEmpty()) {
                    String[] parts = code.split("=");
                    if (parts.length >= 2) {
                        varCombo.setSelectedItem(parts[0].trim());
                        valueField.setText(parts[1].trim());
                    }
                }

                JButton applyButton = new JButton("Застосувати");
                applyButton.addActionListener(e -> {
                    block.setCode(varCombo.getSelectedItem() + " = " + valueField.getText());
                    editDialog.dispose();
                    repaint();
                });

                varPanel.add(varCombo);
                varPanel.add(equalsLabel);
                varPanel.add(valueField);
                varPanel.add(applyButton);
            }

            contentPanel.add(varPanel, BorderLayout.CENTER);
        }

        else {
            // Стандартне текстове поле для інших типів
            JTextArea codeArea = new JTextArea(block.getCode(), 3, 30);
            JButton saveButton = new JButton("Зберегти");
            saveButton.addActionListener(e -> {
                block.setCode(codeArea.getText().trim());
                editDialog.dispose();
                repaint();
            });

            contentPanel.add(new JScrollPane(codeArea), BorderLayout.CENTER);
            contentPanel.add(saveButton, BorderLayout.SOUTH);
        }

        editDialog.add(contentPanel);
        editDialog.setVisible(true);
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Connection conn : connections) {
            conn.draw(g2d);
        }

        for (Block block : blocks) {
            block.draw(g2d);
        }
    }
}
class Block implements Serializable {
    private static final int WIDTH = 120;
    private static final int HEIGHT = 60;

    private BlockType type;
    private int x, y;
    private String code;

    public Block(BlockType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public BlockType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public boolean contains(Point p) {
        return p.x >= x && p.x <= x + WIDTH && p.y >= y && p.y <= y + HEIGHT;
    }

    public boolean isNearOutput(Point p) {
        if (type == BlockType.CONDITION) {
            int trueX = x + WIDTH;
            int falseX = x;
            int outputY = y + HEIGHT / 3;
            int sensitivity = 15;

            boolean nearTrue = Math.abs(p.x - trueX) < sensitivity &&
                    Math.abs(p.y - outputY) < sensitivity;
            boolean nearFalse = Math.abs(p.x - falseX) < sensitivity &&
                    Math.abs(p.y - outputY) < sensitivity;

            return nearTrue || nearFalse;
        } else {
            int outputX = x + WIDTH / 2;
            int outputY = y + HEIGHT;
            return Math.abs(p.x - outputX) < 10 && Math.abs(p.y - outputY) < 10;
        }
    }

    public Point getOutputPoint() {
        return new Point(x + WIDTH / 2, y + HEIGHT);
    }

    public Point getInputPoint() {
        return new Point(x + WIDTH / 2, y);
    }

    public Point getTrueOutputPoint() {
        return new Point(x + WIDTH, y + HEIGHT / 3);
    }

    public Point getFalseOutputPoint() {
        return new Point(x, y + HEIGHT / 3);
    }

    public void draw(Graphics2D g) {
        Color color;
        switch (type) {
            case START:
                color = Color.GREEN;
                break;
            case END:
                color = Color.RED;
                break;
            case CONDITION:
                color = Color.YELLOW;
                break;
            default:
                color = Color.CYAN;
        }

        g.setColor(color);

        if (type == BlockType.START || type == BlockType.END) {
            g.fillOval(x, y, WIDTH, HEIGHT);
        } else if (type == BlockType.CONDITION) {
            int[] xPoints = {x + WIDTH/2, x + WIDTH, x + WIDTH/2, x};
            int[] yPoints = {y, y + HEIGHT/2, y + HEIGHT, y + HEIGHT/2};
            g.fillPolygon(xPoints, yPoints, 4);
        } else {
            g.fillRoundRect(x, y, WIDTH, HEIGHT, 20, 20);
        }

        g.setColor(Color.BLACK);
        if (type == BlockType.START || type == BlockType.END) {
            g.drawOval(x, y, WIDTH, HEIGHT);
        } else if (type == BlockType.CONDITION) {
            int[] xPoints = {x + WIDTH/2, x + WIDTH, x + WIDTH/2, x};
            int[] yPoints = {y, y + HEIGHT/2, y + HEIGHT, y + HEIGHT/2};
            g.drawPolygon(xPoints, yPoints, 4);
        } else {
            g.drawRoundRect(x, y, WIDTH, HEIGHT, 20, 20);
        }

        String displayText = type.toString();
        if (code != null && !code.isEmpty()) {
            displayText += ": " + code;
        }

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(displayText);
        if (textWidth > WIDTH - 10) {
            displayText = type.toString();
        }

        int textX = x + (WIDTH - fm.stringWidth(displayText)) / 2;
        int textY = y + (HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(displayText, textX, textY);

        g.setColor(Color.RED);
        if (type != BlockType.END) {
            Point output = getOutputPoint();
            if (type == BlockType.CONDITION) {
                Point trueOutput = getTrueOutputPoint();
                Point falseOutput = getFalseOutputPoint();
                g.fillOval(trueOutput.x - 3, trueOutput.y - 3, 6, 6);
                g.fillOval(falseOutput.x - 3, falseOutput.y - 3, 6, 6);
            } else {
                g.fillOval(output.x - 3, output.y - 3, 6, 6);
            }
        }

        if (type != BlockType.START) {
            Point input = getInputPoint();
            g.fillOval(input.x - 3, input.y - 3, 6, 6);
        }
    }
}
class Connection implements Serializable {
    private Block from;
    private Block to;
    private Point dragPoint;
    Boolean condition;

    public Connection(Block from, Block to) {
        this.from = from;
        this.to = to;
    }

    public Block getFrom() {
        return from;
    }

    public Block getTo() {
        return to;
    }

    public void setTo(Block to) {
        this.to = to;
    }

    public void setDragPoint(Point p) {
        this.dragPoint = p;
    }

    public boolean isCondition() {
        return condition != null;
    }

    public void setCondition(boolean condition) {
        this.condition = condition;
    }

    public void draw(Graphics2D g) {
        if (from == null) return;

        Point start, end;

        if (from.getType() == BlockType.CONDITION && condition != null) {
            start = condition ? from.getTrueOutputPoint() : from.getFalseOutputPoint();
        } else {
            start = from.getOutputPoint();
        }

        if (to != null) {
            end = to.getInputPoint();
        } else if (dragPoint != null) {
            end = dragPoint;
        } else {
            return;
        }

        g.setColor(Color.BLUE);
        g.drawLine(start.x, start.y, end.x, end.y);

        // Draw arrowhead
        int arrowSize = 8;
        double angle = Math.atan2(end.y - start.y, end.x - start.x);

        int x1 = (int) (end.x - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (end.y - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (end.x - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (end.y - arrowSize * Math.sin(angle + Math.PI / 6));

        g.fillPolygon(new int[]{end.x, x1, x2}, new int[]{end.y, y1, y2}, 3);

        // Draw condition label
        if (from.getType() == BlockType.CONDITION && condition != null) {
            String label = condition ? "T" : "F";
            int labelX = (start.x + end.x) / 2;
            int labelY = (start.y + end.y) / 2;
            g.drawString(label, labelX, labelY);
        }
    }
}

enum BlockType {
    START, END, ASSIGNMENT, INPUT, OUTPUT, CONDITION
}
class ProjectData implements Serializable {
    private List<String> sharedVariables;
    private List<FlowchartData> flowchartData;

    public List<String> getSharedVariables() {
        return sharedVariables;
    }

    public void setSharedVariables(List<String> sharedVariables) {
        this.sharedVariables = sharedVariables;
    }

    public List<FlowchartData> getFlowchartData() {
        return flowchartData;
    }

    public void setFlowchartData(List<FlowchartData> flowchartData) {
        this.flowchartData = flowchartData;
    }
}
class FlowchartData implements Serializable {
    private List<Block> blocks;
    private List<Connection> connections;

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void setConnections(List<Connection> connections) {
        this.connections = connections;
    }
}

class TestCase {
    private String input;
    private String expectedOutput;

    public TestCase(String input, String expectedOutput) {
        this.input = input;
        this.expectedOutput = expectedOutput;
    }

    public String getInput() {
        return input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    @Override
    public String toString() {
        return "Input: " + input + " -> Expected: " + expectedOutput;
    }
}