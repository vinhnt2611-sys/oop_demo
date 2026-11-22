package logic.ui;

import logic.model.TruthTable;
import javax.swing.table.AbstractTableModel;

public final class TruthTableModel extends AbstractTableModel {
    private int variables = 3;
    private boolean[] outputs = new boolean[8];
    public int getRowCount() { return outputs.length; }
    public int getColumnCount() { return variables + 2; }
    public String getColumnName(int column) {
        return column == 0 ? "m" : column == variables + 1 ? "F" : "" + (char) ('A' + column - 1);
    }
    public Object getValueAt(int row, int column) {
        if (column == 0) return row;
        if (column == variables + 1) return outputs[row] ? 1 : 0;
        return (row >> (variables - column)) & 1;
    }
    public void toggle(int row) {
        if (row < 0 || row >= outputs.length) return;
        outputs[row] = !outputs[row];
        fireTableCellUpdated(row, variables + 1);
    }
    public void setTable(TruthTable table) {
        boolean structureChanged = variables != table.variableCount();
        variables = table.variableCount();
        outputs = new boolean[table.rowCount()];
        for (int row = 0; row < outputs.length; row++) outputs[row] = table.output(row);
        if (structureChanged) fireTableStructureChanged(); else fireTableDataChanged();
    }
    public TruthTable snapshot() { return new TruthTable(variables, outputs); }
}
