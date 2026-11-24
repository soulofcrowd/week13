package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Main extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private String currentTable = "students";

    public Main() {
        setTitle("University Management System");
        setSize(850, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel top = new JPanel();

        String[] tables = {"students", "teachers", "subjects", "grades"};

        for (String name : tables) {
            JButton button = new JButton(name);
            button.addActionListener(e -> {
                currentTable = name;
                refresh();
            });
            top.add(button);
        }

        model = new DefaultTableModel();
        table = new JTable(model);

        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");
        JButton refresh = new JButton("Refresh");

        JPanel bottom = new JPanel();
        bottom.add(add);
        bottom.add(edit);
        bottom.add(delete);
        bottom.add(refresh);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        add.addActionListener(e -> addRecord());
        edit.addActionListener(e -> editRecord());
        delete.addActionListener(e -> deleteRecord());
        refresh.addActionListener(e -> refresh());

        refresh();
    }

    private String getQuery() {
        switch (currentTable) {
            case "subjects":
                return "SELECT * FROM subjects";
            case "grades":
                return "SELECT * FROM grades";
            default:
                return "SELECT * FROM " + currentTable;
        }
    }

    private void refresh() {
        model.setRowCount(0);
        model.setColumnCount(0);

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(getQuery())) {

            ResultSetMetaData md = rs.getMetaData();

            for (int i = 1; i <= md.getColumnCount(); i++) {
                model.addColumn(md.getColumnLabel(i));
            }

            while (rs.next()) {
                Object[] row = new Object[md.getColumnCount()];
                for (int i = 0; i < row.length; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                model.addRow(row);
            }

        } catch (SQLException e) {
            error(e);
        }
    }

    private String[] getColumns() {
        switch (currentTable) {
            case "students":
                return new String[]{"full_name", "group_name", "email"};
            case "teachers":
                return new String[]{"full_name", "department"};
            case "subjects":
                return new String[]{"name", "teacher_id"};
            default:
                return new String[]{"student_id", "subject_id", "grade"};
        }
    }

    private void addRecord() {
        String[] columns = getColumns();
        JTextField[] fields = new JTextField[columns.length];
        Object[] form = new Object[columns.length * 2];

        for (int i = 0; i < columns.length; i++) {
            form[i * 2] = columns[i];
            fields[i] = new JTextField();
            form[i * 2 + 1] = fields[i];
        }

        if (JOptionPane.showConfirmDialog(this, form,
                "Add Record", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        try (Connection con = DBConnection.getConnection()) {
            StringBuilder sql = new StringBuilder(
                    "INSERT INTO " + currentTable + " (");

            sql.append(String.join(",", columns));
            sql.append(") VALUES (");
            sql.append("?,".repeat(columns.length));
            sql.setLength(sql.length() - 1);
            sql.append(")");

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].getText().isBlank()) {
                        JOptionPane.showMessageDialog(this, "Fill all required fields!");
                        return;
                    }
                    ps.setString(i + 1, fields[i].getText());
                }
                ps.executeUpdate();
            }

            refresh();

        } catch (SQLException e) {
            error(e);
        }
    }

    private void editRecord() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a record!");
            return;
        }

        String id = model.getValueAt(row, 0).toString();
        String[] columns = getColumns();
        JTextField[] fields = new JTextField[columns.length];
        Object[] form = new Object[columns.length * 2];

        for (int i = 0; i < columns.length; i++) {
            form[i * 2] = columns[i];
            fields[i] = new JTextField(
                    model.getValueAt(row, i + 1).toString());
            form[i * 2 + 1] = fields[i];
        }

        if (JOptionPane.showConfirmDialog(this, form,
                "Edit Record", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        try (Connection con = DBConnection.getConnection()) {
            StringBuilder sql = new StringBuilder("UPDATE " + currentTable + " SET ");

            for (String column : columns) {
                sql.append(column).append("=?,");
            }

            sql.setLength(sql.length() - 1);
            sql.append(" WHERE id=?");

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                for (int i = 0; i < fields.length; i++) {
                    ps.setString(i + 1, fields[i].getText());
                }
                ps.setInt(fields.length + 1, Integer.parseInt(id));
                ps.executeUpdate();
            }

            refresh();

        } catch (SQLException e) {
            error(e);
        }
    }

    private void deleteRecord() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a record!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this, "Delete this record?",
                "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM " + currentTable + " WHERE id=?")) {

            ps.setObject(1, model.getValueAt(row, 0));
            ps.executeUpdate();
            refresh();

        } catch (SQLException e) {
            error(e);
        }
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this,
                e.getMessage(), "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new Main().setVisible(true));
    }
}