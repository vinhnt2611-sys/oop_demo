package logic.ui;

import logic.expression.*;
import logic.model.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/** A local educational desktop app: input -> result -> tabulation -> PI chart. */
public final class NormalizerFrame extends JFrame {
    private static final Color INK = new Color(26, 42, 53), MUTED = new Color(104, 119, 129);
    private static final Color BG = new Color(243, 246, 247), LINE = new Color(222, 230, 233);
    private static final Color TEAL = new Color(0, 113, 104), PALE = new Color(229, 245, 240);
    private final MainController controller = new MainController();
    private final TruthTableModel input = new TruthTableModel();
    private final JTable truthTable = new JTable(input);
    private final JComboBox<Integer> variables = new JComboBox<>(new Integer[] {3, 4});
    private final JRadioButton sop = new JRadioButton("SOP", true), pos = new JRadioButton("POS");
    private final JComboBox<String> samples = new JComboBox<>(new String[] {
            "Chọn ví dụ để khám phá…", "01  •  Ghép cơ bản: F = C", "02  •  Phải dùng brute force",
            "03  •  4 biến, nhiều vòng ghép", "04  •  Hàm luôn bằng 0", "05  •  Hàm luôn bằng 1"});
    private final JLabel status = label("", 12, Font.PLAIN, MUTED);
    private final JLabel inputSummary = label("", 12, Font.PLAIN, MUTED);
    private final JLabel resultBadge = label("", 12, Font.BOLD, TEAL);
    private final JLabel formula = label("", 28, Font.BOLD, INK);
    private final JLabel verification = label("", 12, Font.PLAIN, TEAL);
    private final JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
    private final JEditorPane overviewText = htmlPane(), roundExplanation = htmlPane(), coverExplanation = htmlPane();
    private final JEditorPane mergeText = htmlPane();
    private final DefaultListModel<String> roundNames = new DefaultListModel<>();
    private final JList<String> roundList = new JList<>(roundNames);
    private final JTable roundTable = new JTable(), chartTable = new JTable();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JButton copy = new JButton("Sao chép kết quả");
    private MinimizationResult result;
    private boolean updating;

    public NormalizerFrame() {
        super("Logic Lab — Quine–McCluskey | Topic 9");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 750));
        Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1300, available.width - 30), Math.min(900, available.height - 30));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(22, 18));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(20, 26, 16, 26));
        setContentPane(root);
        root.add(header(), BorderLayout.NORTH);
        root.add(inputPanel(), BorderLayout.WEST);
        root.add(workspace(), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(status, BorderLayout.WEST);
        footer.add(label("TOPIC 09   /   JAVA + OOP   /   CHẠY OFFLINE", 11, Font.BOLD, MUTED), BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        bindEvents();
        samples.setSelectedIndex(2);
    }

    private JComponent header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout(14, 0));
        left.setOpaque(false);
        JLabel icon = label(" ƒ ", 32, Font.BOLD, Color.WHITE);
        icon.setOpaque(true); icon.setBackground(TEAL); icon.setBorder(new EmptyBorder(5, 10, 7, 10));
        left.add(icon, BorderLayout.WEST);
        JPanel title = new JPanel(new GridLayout(2, 1, 0, 3)); title.setOpaque(false);
        title.add(label("Logic Lab", 28, Font.BOLD, INK));
        title.add(label("Rút gọn biểu thức. Nhìn rõ từng bước.", 14, Font.PLAIN, MUTED));
        left.add(title, BorderLayout.CENTER); panel.add(left, BorderLayout.WEST);
        JLabel badge = label("QUINE–McCLUSKEY   •   3 / 4 BIẾN", 12, Font.BOLD, TEAL);
        badge.setBorder(new EmptyBorder(0, 12, 0, 0)); panel.add(badge, BorderLayout.EAST);
        return panel;
    }

    private JComponent inputPanel() {
        JPanel panel = card(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(310, 600));
        JPanel controls = vertical();
        controls.add(label("01   THIẾT LẬP ĐẦU VÀO", 12, Font.BOLD, TEAL));
        controls.add(Box.createVerticalStrut(12));
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
        row.add(label("Số biến", 14, Font.BOLD, INK), BorderLayout.WEST);
        variables.setPreferredSize(new Dimension(100, 32)); row.add(variables, BorderLayout.EAST);
        controls.add(row); controls.add(Box.createVerticalStrut(10));
        ButtonGroup group = new ButtonGroup(); group.add(sop); group.add(pos);
        JPanel forms = new JPanel(new GridLayout(1, 2, 8, 0)); forms.setOpaque(false);
        for (JRadioButton button : new JRadioButton[] {sop, pos}) {
            button.setOpaque(false); button.setFont(font(14, Font.BOLD)); forms.add(button);
        }
        controls.add(forms); controls.add(Box.createVerticalStrut(4));
        controls.add(label("SOP: tổng các tích  /  POS: tích các tổng", 11, Font.PLAIN, MUTED));
        controls.add(Box.createVerticalStrut(14));
        samples.setFont(font(12, Font.PLAIN)); samples.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        controls.add(samples); controls.add(Box.createVerticalStrut(15));
        controls.add(label("Bảng chân trị", 16, Font.BOLD, INK));
        controls.add(label("Bấm vào cột F để đổi 0 ↔ 1.", 12, Font.PLAIN, MUTED));
        panel.add(controls, BorderLayout.NORTH);

        styleTable(truthTable); truthTable.setRowHeight(27);
        truthTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        truthTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                setHorizontalAlignment(CENTER); setFont(font(13, col == input.getColumnCount() - 1 ? Font.BOLD : Font.PLAIN));
                setForeground(col == 0 ? MUTED : INK);
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 251));
                if (col == input.getColumnCount() - 1) {
                    setForeground(value.equals(1) ? TEAL : MUTED);
                    setBackground(value.equals(1) ? PALE : new Color(240, 243, 245));
                }
                setBorder(new MatteBorder(0, 0, 1, 0, LINE));
                return this;
            }
        });
        truthTable.setToolTipText("Bấm cột F, hoặc chọn một dòng rồi nhấn Space, để đổi giá trị.");
        panel.add(scroll(truthTable), BorderLayout.CENTER);
        JPanel actions = vertical();
        actions.add(inputSummary); actions.add(Box.createVerticalStrut(8));
        JPanel small = new JPanel(new GridLayout(1, 2, 8, 0)); small.setOpaque(false);
        JButton random = new JButton("Ngẫu nhiên"), clear = new JButton("Đặt tất cả = 0");
        small.add(random); small.add(clear); actions.add(small); actions.add(Box.createVerticalStrut(9));
        JButton calculate = new JButton("RÚT GỌN BIỂU THỨC  →");
        calculate.setBackground(TEAL); calculate.setForeground(Color.WHITE);
        calculate.setFont(font(13, Font.BOLD)); calculate.setFocusPainted(false);
        calculate.setAlignmentX(Component.LEFT_ALIGNMENT);
        calculate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        calculate.setPreferredSize(new Dimension(260, 43)); actions.add(calculate);
        panel.add(actions, BorderLayout.SOUTH);
        calculate.addActionListener(event -> calculate());
        random.addActionListener(event -> {
            boolean[] outputs = new boolean[input.getRowCount()];
            for (int rowIndex = 0; rowIndex < outputs.length; rowIndex++) outputs[rowIndex] = ThreadLocalRandom.current().nextBoolean();
            replaceInput(new TruthTable((Integer) variables.getSelectedItem(), outputs)); calculate();
        });
        clear.addActionListener(event -> { replaceInput(TruthTable.ofMinterms((Integer) variables.getSelectedItem())); calculate(); });
        getRootPane().setDefaultButton(calculate);
        return panel;
    }

    private JComponent workspace() {
        JPanel panel = new JPanel(new BorderLayout(0, 10)); panel.setOpaque(false);
        panel.add(label("02   KHÁM PHÁ LỜI GIẢI", 12, Font.BOLD, TEAL), BorderLayout.NORTH);
        tabs.setFont(font(13, Font.BOLD)); tabs.setBackground(Color.WHITE);
        tabs.addTab("Kết quả", overview());
        tabs.addTab("Các vòng ghép", roundsPanel());
        tabs.addTab("Bảng PI", chartPanel());
        JEditorPane help = htmlPane(); help.setText(html(helpText()));
        JPanel helpCard = card(new BorderLayout()); helpCard.add(scroll(help));
        tabs.addTab("Hiểu thuật toán", helpCard);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JComponent overview() {
        JPanel panel = card(new BorderLayout(0, 18));
        JPanel top = vertical();
        top.add(Box.createVerticalStrut(8)); top.add(resultBadge); top.add(Box.createVerticalStrut(12));
        formula.setVerticalAlignment(SwingConstants.TOP);
        top.add(formula); top.add(Box.createVerticalStrut(12)); top.add(verification);
        top.add(Box.createVerticalStrut(15));
        copy.setAlignmentX(Component.LEFT_ALIGNMENT); top.add(copy); top.add(Box.createVerticalStrut(20));
        stats.setOpaque(false); stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 94)); top.add(stats);
        panel.add(top, BorderLayout.NORTH); panel.add(scroll(overviewText), BorderLayout.CENTER);
        copy.addActionListener(event -> {
            if (result == null) return;
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("F = " + result.expression().text()), null);
            status.setText("Đã sao chép: F = " + result.expression().text());
        });
        return panel;
    }

    private JComponent roundsPanel() {
        JPanel panel = card(new BorderLayout(14, 12));
        roundList.setFixedCellHeight(47); roundList.setFont(font(13, Font.BOLD));
        roundList.setSelectionBackground(PALE); roundList.setSelectionForeground(TEAL);
        JScrollPane choices = scroll(roundList); choices.setPreferredSize(new Dimension(130, 100));
        panel.add(choices, BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout(0, 12)); right.setOpaque(false);
        roundExplanation.setPreferredSize(new Dimension(100, 105)); right.add(roundExplanation, BorderLayout.NORTH);
        styleTable(roundTable); right.add(scroll(roundTable), BorderLayout.CENTER);
        JScrollPane merges = scroll(mergeText); merges.setPreferredSize(new Dimension(100, 190));
        right.add(merges, BorderLayout.SOUTH); panel.add(right, BorderLayout.CENTER);
        roundList.addListSelectionListener(event -> { if (!event.getValueIsAdjusting()) showRound(roundList.getSelectedIndex()); });
        return panel;
    }

    private JComponent chartPanel() {
        JPanel panel = card(new BorderLayout(0, 14));
        JEditorPane intro = htmlPane();
        intro.setText(html("<h2>Prime Implicant Chart</h2><p>Mỗi hàng là một PI; mỗi cột là một dòng cần phủ. "
                + "<b>1</b> = PI phủ dòng đó. Xanh: PI được chọn. Vàng: essential PI bắt buộc chọn.</p>"));
        intro.setPreferredSize(new Dimension(100, 110)); panel.add(intro, BorderLayout.NORTH);
        styleTable(chartTable); chartTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        chartTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                setHorizontalAlignment(CENTER); setFont(font(12, Font.PLAIN)); setForeground(INK);
                Color color = row % 2 == 0 ? Color.WHITE : new Color(248, 250, 251);
                if (result != null && result.solution().selectedIndices().contains(row)) color = PALE;
                if (result != null && result.solution().essentialIndices().contains(row)) color = new Color(255, 246, 218);
                setBackground(color); setBorder(new MatteBorder(0, 0, 1, 0, LINE));
                return this;
            }
        });
        panel.add(scroll(chartTable), BorderLayout.CENTER);
        JScrollPane explanation = scroll(coverExplanation); explanation.setPreferredSize(new Dimension(100, 210));
        panel.add(explanation, BorderLayout.SOUTH); return panel;
    }

    private void bindEvents() {
        input.addTableModelListener(event -> { if (!updating) markDirty(); });
        variables.addActionListener(event -> {
            if (updating) return;
            replaceInput(TruthTable.ofMinterms((Integer) variables.getSelectedItem()));
            calculate();
        });
        sop.addActionListener(event -> calculate()); pos.addActionListener(event -> calculate());
        samples.addActionListener(event -> loadSample(samples.getSelectedIndex()));
        truthTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                int row = truthTable.rowAtPoint(event.getPoint()), col = truthTable.columnAtPoint(event.getPoint());
                if (row >= 0 && col == input.getColumnCount() - 1) input.toggle(row);
            }
        });
        truthTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "toggleOutput");
        truthTable.getActionMap().put("toggleOutput", new AbstractAction() {
            public void actionPerformed(ActionEvent event) { input.toggle(truthTable.getSelectedRow()); }
        });
    }

    private void replaceInput(TruthTable table) {
        updating = true;
        variables.setSelectedItem(table.variableCount()); input.setTable(table);
        samples.setSelectedIndex(0); updating = false; markDirty();
    }
    private void loadSample(int index) {
        if (updating || index == 0) return;
        TruthTable table = switch (index) {
            case 1 -> TruthTable.ofMinterms(3, 1, 3, 5, 7);
            case 2 -> TruthTable.ofMinterms(3, 0, 1, 2, 5, 6, 7);
            case 3 -> TruthTable.ofMinterms(4, 0, 1, 2, 5, 6, 7, 8, 9, 10, 14);
            case 4 -> TruthTable.ofMinterms((Integer) variables.getSelectedItem());
            default -> new TruthTable((Integer) variables.getSelectedItem(), allTrue(1 << (Integer) variables.getSelectedItem()));
        };
        replaceInput(table);
        updating = true; samples.setSelectedIndex(index); updating = false;
        calculate();
    }
    private static boolean[] allTrue(int count) { boolean[] values = new boolean[count]; java.util.Arrays.fill(values, true); return values; }

    private void markDirty() {
        result = null; copy.setEnabled(false);
        inputSummary.setText(input.getRowCount() + " dòng   •   " + input.snapshot().rowsWithValue(true).size() + " giá trị 1");
        resultBadge.setText("ĐẦU VÀO ĐÃ THAY ĐỔI"); formula.setText("Nhấn Rút gọn để tính lại");
        verification.setText("Kết quả trước đã được xóa để tránh nhầm lẫn.");
        overviewText.setText(html("<h2>Bạn đang chỉnh bảng chân trị</h2><p>Chọn giá trị F cho từng tổ hợp đầu vào, "
                + "sau đó nhấn <b>Rút gọn biểu thức</b>. Chỉ nhận 0/1; không có don’t-care.</p>"));
        stats.removeAll(); stats.revalidate(); stats.repaint();
        roundNames.clear(); roundTable.setModel(readOnly(new Object[0][0], new Object[0]));
        chartTable.setModel(readOnly(new Object[0][0], new Object[0]));
        roundExplanation.setText(html("<p>Chưa có kết quả mới.</p>")); mergeText.setText(""); coverExplanation.setText("");
        status.setText("Đầu vào đã đổi  •  Nhấn Rút gọn biểu thức để cập nhật lời giải.");
    }

    private void calculate() {
        try {
            NormalFormStrategy form = sop.isSelected() ? new SopStrategy() : new PosStrategy();
            result = controller.minimize(input.snapshot(), form); copy.setEnabled(true);
            resultBadge.setText(form.name() + "  /  " + input.snapshot().variableCount() + " BIẾN  /  BIỂU THỨC TỐI THIỂU");
            formula.setText("<html><div style='width:490px'>F = " + escape(result.expression().text()) + "</div></html>");
            verification.setText("✓ Đã kiểm chứng tương đương trên " + result.input().rowCount() + "/" + result.input().rowCount() + " tổ hợp đầu vào");
            stats.removeAll();
            stats.add(metric("VÒNG / CỘT", "" + result.rounds().size()));
            stats.add(metric("PI TÌM ĐƯỢC", "" + result.chart().primes().size()));
            stats.add(metric("PI ĐƯỢC CHỌN", "" + result.selectedPrimes().size()));
            stats.add(metric("LITERAL", "" + result.literalCount()));
            stats.revalidate(); stats.repaint();
            showOverview();
            roundNames.clear();
            for (CombinationRound round : result.rounds()) roundNames.addElement("  Cột " + round.index() + "  ·  " + round.entries().size());
            roundList.setSelectedIndex(0); showChart();
            status.setText("Hoàn tất  •  " + form.name() + "  •  " + result.solution().subsetsChecked() + " tập con đã xét bằng brute force");
        } catch (RuntimeException error) {
            markDirty(); status.setText("Không thể tính: " + error.getMessage());
            JOptionPane.showMessageDialog(this, error.getMessage(), "Lỗi xử lý", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showOverview() {
        String mode = result.form().name();
        StringBuilder text = new StringBuilder("<h2>Từ bảng chân trị đến lời giải</h2>");
        text.append("<p><b>Đầu vào:</b> F = Σm(").append(join(result.input().rowsWithValue(true))).append(")</p>");
        if (result.form().targetValue()) text.append("<p><b>SOP:</b> gom trực tiếp các dòng F = 1: ").append(join(result.chart().targets())).append(".</p>");
        else text.append("<p><b>POS:</b> rút gọn F' từ các dòng F = 0: ").append(join(result.chart().targets()))
                .append(". Sau đó dùng De Morgan để lấy F; đảo cả toán tử lẫn dấu của từng biến.</p>");
        text.append("<p><b>Ghép:</b> ").append(result.rounds().size()).append(" cột tạo ra ").append(result.chart().primes().size())
                .append(" PI. Chọn ").append(result.selectedPrimes().size()).append(" PI với ").append(result.literalCount()).append(" literal.</p>");
        if (result.solution().subsetsChecked() > 0) text.append("<p><b>Cần brute force:</b> essential PI chưa phủ hết các dòng. Chương trình xét ")
                .append(result.solution().subsetsChecked()).append(" tập con để tìm phần phủ bổ sung tối thiểu.</p>");
        else text.append("<p><b>Không cần brute force:</b> essential PI đã phủ hết tập mục tiêu, hoặc tập mục tiêu rỗng.</p>");
        text.append("<h3>Các PI được dùng trong kết quả ").append(mode).append("</h3>");
        if (result.selectedPrimes().isEmpty()) text.append("<p>Không có dòng mục tiêu. Kết quả là hàm hằng ").append(result.expression().text()).append(".</p>");
        for (int index : result.solution().selectedIndices()) {
            Implicant term = result.chart().primes().get(index);
            text.append("<p><b>P").append(index + 1).append(" · ").append(term.pattern()).append("</b> → ")
                    .append(escape(result.form().expressionFor(term).text())).append(" &nbsp; <small>")
                    .append(result.solution().essentialIndices().contains(index) ? "Essential PI" : "Chọn bằng brute force").append("</small></p>");
        }
        text.append("<p><small>Ưu tiên ít PI nhất; nếu bằng nhau, ưu tiên ít literal nhất. Có thể có nhiều biểu thức tối thiểu cùng đúng.</small></p>");
        text.append("<p><b>Khám phá tiếp:</b> mở tab Các vòng ghép, rồi Bảng PI. Tab Hiểu thuật toán giải thích các thuật ngữ.</p>");
        overviewText.setText(html(text.toString())); overviewText.setCaretPosition(0);
    }

    private void showRound(int index) {
        if (result == null || index < 0 || index >= result.rounds().size()) return;
        CombinationRound round = result.rounds().get(index);
        roundExplanation.setText(html("<h2>Cột " + index + " · " + round.entries().size() + " term</h2><p>"
                + (index == 0 ? "Nhóm các dòng mục tiêu theo số bit 1." : "Các mẫu đã được loại " + index + " biến sau " + index + " lần ghép.")
                + " Term đã ghép có dấu ✓. Term không ghép tiếp được là <b>PI</b>."
                + (result.form().targetValue() ? "" : " Đang xử lý <b>F'</b> để tạo POS của F.") + "</p>"));
        Object[][] rows = new Object[round.entries().size()][5];
        for (int row = 0; row < rows.length; row++) {
            var entry = round.entries().get(row); Implicant term = entry.implicant();
            rows[row] = new Object[] {term.onesCount(), term.pattern(), join(term.coveredRows()),
                    new SopStrategy().expressionFor(term).text(), entry.combined() ? "✓ Đã ghép" : "PI"};
        }
        roundTable.setModel(readOnly(rows, new Object[] {"Số bit 1", "Mẫu", "Phủ các dòng", "Tích tương ứng", "Trạng thái"}));
        StringBuilder details = new StringBuilder("<h3>Các phép ghép tạo cột kế tiếp</h3>");
        if (round.merges().isEmpty()) details.append("<p>Không còn cặp hợp lệ. Quá trình ghép dừng ở cột này.</p>");
        for (CombinationRound.Merge merge : round.merges()) details.append("<p><code>").append(merge.left().pattern()).append(" + ")
                .append(merge.right().pattern()).append(" → <b>").append(merge.result().pattern()).append("</b></code></p>");
        details.append("<p><small>Hai mẫu phải có cùng vị trí x và khác đúng một bit còn lại. Kết quả trùng được gộp trong cột tiếp theo. "
                + "x là biến bị loại, không phải don’t-care đầu vào.</small></p>");
        mergeText.setText(html(details.toString())); mergeText.setCaretPosition(0);
    }

    private void showChart() {
        var chart = result.chart(); int size = chart.targets().size();
        Object[] columns = new Object[size + 3]; columns[0] = "PI"; columns[1] = "Mẫu"; columns[size + 2] = "Lựa chọn";
        for (int col = 0; col < size; col++) columns[col + 2] = (result.form().targetValue() ? "m" : "z") + chart.targets().get(col);
        Object[][] rows = new Object[chart.primes().size()][columns.length];
        for (int row = 0; row < rows.length; row++) {
            rows[row][0] = "P" + (row + 1); rows[row][1] = chart.primes().get(row).pattern();
            for (int col = 0; col < size; col++) rows[row][col + 2] = chart.covers(row, chart.targets().get(col)) ? "1" : "·";
            rows[row][size + 2] = result.solution().essentialIndices().contains(row) ? "Essential"
                    : result.solution().selectedIndices().contains(row) ? "Brute force" : "Không chọn";
        }
        chartTable.setModel(readOnly(rows, columns));
        for (int col = 0; col < columns.length; col++) chartTable.getColumnModel().getColumn(col)
                .setPreferredWidth(col == 0 ? 45 : col == 1 ? 75 : col == columns.length - 1 ? 110 : 42);
        String essentials = result.solution().essentialIndices().stream().sorted().map(i -> "P" + (i + 1)).collect(Collectors.joining(", "));
        String selected = result.solution().selectedIndices().stream().map(i -> "P" + (i + 1)).collect(Collectors.joining(", "));
        String explanation = "<h3>Chọn tập phủ tối thiểu</h3><p><b>1.</b> Essential PI: " + (essentials.isEmpty() ? "không có" : essentials)
                + ". Một cột chỉ có một số 1 thì PI ở hàng đó là bắt buộc.</p><p><b>2.</b> "
                + (result.solution().subsetsChecked() == 0 ? "Đã phủ đủ, không cần tìm thêm."
                    : "Các dòng còn thiếu: " + rowsInMask(result.solution().remainingMask()) + ". Xét " + result.solution().subsetsChecked()
                    + " tập con; tìm được " + result.solution().optimalCoverCount() + " phương án cùng tối ưu về số PI và literal.")
                + "</p><p><b>3.</b> Tập được chọn: " + (selected.isEmpty() ? "rỗng (hàm hằng)" : selected)
                + ".</p><p><small>m = dòng F = 1 cho SOP. z = dòng F = 0, tức minterm của F', cho POS.</small></p>";
        coverExplanation.setText(html(explanation)); coverExplanation.setCaretPosition(0);
    }

    private static String helpText() {
        return "<h2>Đây là ứng dụng gì?</h2><p>Bạn mô tả một hàm Boolean qua bảng chân trị. Ứng dụng tìm biểu thức đơn giản tương đương "
            + "và trình bày quá trình Quine–McCluskey, thay vì chỉ đưa ra đáp án.</p>"
            + "<h3>Ký hiệu</h3><p><b>A'</b>: NOT A. <b>A · B</b>: A AND B. <b>A + B</b>: A OR B. "
            + "A là bit trái nhất; với 3 biến, dòng m5 = 101 nghĩa là A=1, B=0, C=1.</p>"
            + "<h3>SOP / POS</h3><p><b>SOP</b> (Sum of Products): tổng các tích, ví dụ A · B + A' · C. "
            + "<b>POS</b> (Product of Sums): tích các tổng, ví dụ (A + B) · (A' + C).</p>"
            + "<h3>Minterm / Literal</h3><p>Minterm ứng với đúng một tổ hợp đầu vào, ví dụ 101 → A · B' · C. "
            + "Literal là một biến hoặc phủ định của biến; A · B' có 2 literal.</p>"
            + "<h3>Ghép term / Prime Implicant (PI)</h3><p>011 và 111 chỉ khác A, nên ghép thành x11, tương ứng B · C. "
            + "Mẫu không thể ghép tiếp là PI. Chỉ được ghép khi vị trí x giống nhau và khác đúng một bit cố định.</p>"
            + "<h3>Essential PI / Brute force</h3><p>PI duy nhất phủ một dòng mục tiêu là essential PI, bắt buộc chọn. "
            + "Nếu vẫn còn dòng chưa phủ, thử các tập con PI còn lại để chọn ít PI nhất; bằng nhau thì chọn ít literal nhất.</p>"
            + "<h3>POS và De Morgan</h3><p>Rút gọn F' từ các dòng F=0, sau đó phủ định lại: "
            + "F' = A' · B + A · C → F = (A + B') · (A' + C'). Phải đảo cả toán tử AND/OR và từng literal.</p>"
            + "<h3>Thử theo thứ tự</h3><p>1. Nạp ví dụ Ghép cơ bản: F=C, mở Các vòng ghép.<br>"
            + "2. Nạp ví dụ Phải dùng brute force, mở Bảng PI.<br>3. Chuyển SOP sang POS và so sánh.<br>"
            + "4. Nạp ví dụ 4 biến, rồi tự đổi một vài ô F và tính lại.</p>"
            + "<p><small>Hàm hằng 0/1 được xử lý riêng khi sinh biểu thức. Không có don’t-care đầu vào. "
            + "Ứng dụng tự kiểm chứng kết quả trên toàn bộ bảng chân trị sau mỗi lần tính.</small></p>";
    }

    private static JPanel metric(String title, String value) {
        JPanel panel = new JPanel(new BorderLayout(0, 8)); panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(13, 12, 13, 12));
        panel.add(label(title, 10, Font.BOLD, MUTED), BorderLayout.NORTH);
        panel.add(label(value, 26, Font.BOLD, TEAL), BorderLayout.CENTER); return panel;
    }
    private static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(18, 18, 18, 18))); return panel;
    }
    private static JPanel vertical() {
        JPanel panel = new JPanel() {
            protected void addImpl(Component child, Object constraints, int index) {
                if (child instanceof JComponent component) component.setAlignmentX(Component.LEFT_ALIGNMENT);
                super.addImpl(child, constraints, index);
            }
        }; panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
    private static Font font(int size, int style) { return new Font("Segoe UI", style, size); }
    private static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text); label.setFont(font(size, style)); label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
    private static JEditorPane htmlPane() {
        JEditorPane pane = new JEditorPane("text/html", ""); pane.setEditable(false); pane.setBackground(Color.WHITE);
        pane.setBorder(null); pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setFont(font(14, Font.PLAIN)); return pane;
    }
    private static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component); scroll.setBorder(new LineBorder(LINE));
        scroll.getViewport().setBackground(Color.WHITE); scroll.getVerticalScrollBar().setUnitIncrement(20);
        return scroll;
    }
    private static void styleTable(JTable table) {
        table.setFont(font(13, Font.PLAIN)); table.setRowHeight(35); table.setGridColor(LINE);
        table.setShowVerticalLines(false); table.setShowHorizontalLines(true); table.setFillsViewportHeight(true);
        table.setSelectionBackground(PALE); table.setSelectionForeground(INK); table.setBackground(Color.WHITE);
        table.getTableHeader().setFont(font(12, Font.BOLD)); table.getTableHeader().setBackground(BG);
        table.getTableHeader().setForeground(MUTED); table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer(); center.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, center);
    }
    private static DefaultTableModel readOnly(Object[][] rows, Object[] columns) {
        return new DefaultTableModel(rows, columns) { public boolean isCellEditable(int row, int col) { return false; } };
    }
    private static String html(String body) {
        return "<html><head><style>body{font-family:'Segoe UI';font-size:11px;color:#1a2a35;margin:10px;}"
                + "h2{font-size:17px;margin-top:8px;margin-bottom:12px;}h3{font-size:13px;color:#007168;margin-top:16px;}"
                + "p{margin-top:7px;margin-bottom:10px;}small{color:#687781;font-size:10px;}code{font-family:Consolas;}</style></head><body>"
                + body + "</body></html>";
    }
    private static String escape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static String join(List<Integer> rows) { return rows.isEmpty() ? "∅" : rows.stream().map(String::valueOf).collect(Collectors.joining(", ")); }
    private static String rowsInMask(int mask) {
        java.util.ArrayList<Integer> rows = new java.util.ArrayList<>();
        for (int row = 0; row < 16; row++) if ((mask & (1 << row)) != 0) rows.add(row);
        return join(rows);
    }
}
