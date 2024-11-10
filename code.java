import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;

public class ZooManagementSystem extends JFrame {
    private Connection conn;
    private JTabbedPane tabbedPane;
    private boolean loggedIn = false;

    public ZooManagementSystem() {
        setTitle("Zoo Management System");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initializeDatabase();
        showLogin();
    }

    private void initializeDatabase() {
        try {
            // Connect to MySQL server (without specifying a database)
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "hellosql123");

            // Create database if it doesn't exist
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS zoo_management");
            stmt.close();

            // Now connect to the created database
            this.conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/zoo_management", "root", "hellosql123");
            createTables();
        } catch (SQLException e) {
            showError("Database Connection Error", e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS animals (id INT AUTO_INCREMENT PRIMARY KEY, animal_name VARCHAR(50), species VARCHAR(50), age INT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS staff (id INT AUTO_INCREMENT PRIMARY KEY, staff_name VARCHAR(50), staff_role VARCHAR(50))");
        stmt.execute("CREATE TABLE IF NOT EXISTS checkups (id INT AUTO_INCREMENT PRIMARY KEY, animal_id INT, checkup_date DATE, doctor_assigned VARCHAR(50), description VARCHAR(100), cure VARCHAR(100), FOREIGN KEY (animal_id) REFERENCES animals(id))");
        stmt.execute("CREATE TABLE IF NOT EXISTS doctors (id INT AUTO_INCREMENT PRIMARY KEY, doctor_name VARCHAR(50), available BOOLEAN)");
        stmt.close();
    }

    private void showLogin() {
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");

        // Add username label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END; // Align right
        loginPanel.add(new JLabel("Username:"), gbc);

        // Add username field
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START; // Align left
        loginPanel.add(usernameField, gbc);

        // Add password label
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END; // Align right
        loginPanel.add(new JLabel("Password:"), gbc);

        // Add password field
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START; // Align left
        loginPanel.add(passwordField, gbc);

        // Add login button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // Span across both columns
        gbc.anchor = GridBagConstraints.CENTER; // Center the button
        loginPanel.add(loginButton, gbc);

        // Login button action listener
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (authenticate(username, password)) {
                loggedIn = true;
                initializeTabs();
                remove(loginPanel);
                revalidate();
                repaint();
            } else {
                showError("Login Error", "Invalid username or password.");
            }
        });

        add(loginPanel);
        setVisible(true);
    }

    private boolean authenticate(String username, String password) {
        // Simple authentication logic (replace with real authentication)
        return "admin".equals(username) && "password".equals(password);
    }

    private void initializeTabs() {
        tabbedPane = new JTabbedPane();

        // Create tabs
        tabbedPane.add("Animal Management", new AnimalManagementPanel());
        tabbedPane.add("Staff Management", new StaffManagementPanel());
        tabbedPane.add("Checkup Management", new CheckupManagementPanel());
        tabbedPane.add("Doctor Management", new DoctorManagementPanel());

        add(tabbedPane);
        setVisible(true);
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        new ZooManagementSystem();
    }

    // Animal Management Panel
    class AnimalManagementPanel extends JPanel {
        private JTextField idField, nameField, speciesField, ageField;
        private JTextArea animalListArea;

        public AnimalManagementPanel() {
            setLayout(new BorderLayout());
            JPanel inputPanel = new JPanel(new GridLayout(5, 2));
            inputPanel.add(new JLabel("ID (for update/delete):"));
            idField = new JTextField();
            inputPanel.add(idField);
            inputPanel.add(new JLabel("Name:"));
            nameField = new JTextField();
            inputPanel.add(nameField);
            inputPanel.add(new JLabel("Species:"));
            speciesField = new JTextField();
            inputPanel.add(speciesField);
            inputPanel.add(new JLabel("Age:"));
            ageField = new JTextField();
            inputPanel.add(ageField);
            add(inputPanel, BorderLayout.NORTH);

            JButton addButton = new JButton("Add Animal");
            addButton.addActionListener(e -> addAnimal());
            JButton updateButton = new JButton("Update Animal");
            updateButton.addActionListener(e -> updateAnimal());
            JButton deleteButton = new JButton("Delete Animal");
            deleteButton.addActionListener(e -> deleteAnimal());
            JButton exportButton = new JButton("Export to PDF");
            exportButton.addActionListener(e -> exportAnimalsToPDF());

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(addButton);
            buttonPanel.add(updateButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(exportButton);
            add(buttonPanel, BorderLayout.SOUTH);

            animalListArea = new JTextArea();
            animalListArea.setEditable(false);
            add(new JScrollPane(animalListArea), BorderLayout.CENTER);

            loadAnimals();
        }

        private void addAnimal() {
            try {
                String name = nameField.getText();
                String species = speciesField.getText();
                int age = Integer.parseInt(ageField.getText());

                String sql = "INSERT INTO animals (animal_name, species, age) VALUES (?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, species);
                pstmt.setInt(3, age);
                pstmt.executeUpdate();
                loadAnimals();
                clearFields();
            } catch (SQLException e) {
                showError("Add Animal Error", e.getMessage());
            } catch (NumberFormatException e) {
                showError("Input Error", "Age must be a number.");
            }
        }

        private void updateAnimal() {
            try {
                int id = Integer.parseInt(idField.getText());
                String name = nameField.getText();
                String species = speciesField.getText();
                int age = Integer.parseInt(ageField.getText());

                String sql = "UPDATE animals SET animal_name = ?, species = ?, age = ? WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, species);
                pstmt.setInt(3, age);
                pstmt.setInt(4, id);
                int rowsUpdated = pstmt.executeUpdate();

                if (rowsUpdated > 0) {
                    loadAnimals();
                    clearFields();
                    JOptionPane.showMessageDialog(this, "Animal updated successfully.");
                } else {
                    showError("Update Animal Error", "No animal found with that ID.");
                }
            } catch (NumberFormatException e) {
                showError("Input Error", "ID and Age must be numbers.");
            } catch (SQLException e) {
                showError("Update Animal Error", e.getMessage());
            }
        }

        private void deleteAnimal() {
            try {
                int id = Integer.parseInt(idField.getText());
                String sql = "DELETE FROM animals WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, id);
                int rowsDeleted = pstmt.executeUpdate();

                if (rowsDeleted > 0) {
                    loadAnimals();
                    clearFields();
                    JOptionPane.showMessageDialog(this, "Animal deleted successfully.");
                } else {
                    showError("Delete Animal Error", "No animal found with that ID.");
                }
            } catch (NumberFormatException e) {
                showError("Input Error", "ID must be a number.");
            } catch (SQLException e) {
                showError("Delete Animal Error", e.getMessage());
            }
        }

        private void clearFields() {
            idField.setText("");
            nameField.setText("");
            speciesField.setText("");
            ageField.setText("");
        }

        private void loadAnimals() {
            try {
                String sql = "SELECT * FROM animals";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                animalListArea.setText("");
                while (rs.next()) {
                    animalListArea.append("ID: " + rs.getInt("id") +
                            ", Name: " + rs.getString("animal_name") +
                            ", Species: " + rs.getString("species") +
                            ", Age: " + rs.getInt("age") + "\n");
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                showError("Load Animals Error", e.getMessage());
            }
        }

        private void exportAnimalsToPDF() {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream("Animals.pdf"));
                document.open();
                document.add(new com.itextpdf.text.Paragraph("List of Animals"));
                document.add(new com.itextpdf.text.Paragraph(" "));

                String sql = "SELECT * FROM animals";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    document.add(new com.itextpdf.text.Paragraph(
                            "ID: " + rs.getInt("id") +
                                    ", Name: " + rs.getString("animal_name") +
                                    ", Species: " + rs.getString("species") +
                                    ", Age: " + rs.getInt("age")));
                }

                document.close();
                JOptionPane.showMessageDialog(this, "PDF exported successfully.");
            } catch (Exception e) {
                showError("PDF Export Error", e.getMessage());
            }
        }
    }

    // Staff Management Panel
    class StaffManagementPanel extends JPanel {
        private JTextField idField, nameField, roleField;
        private JTextArea staffListArea;

        public StaffManagementPanel() {
            setLayout(new BorderLayout());
            JPanel inputPanel = new JPanel(new GridLayout(4, 2));
            inputPanel.add(new JLabel("ID (for update/delete):"));
            idField = new JTextField();
            inputPanel.add(idField);
            inputPanel.add(new JLabel("Name:"));
            nameField = new JTextField();
            inputPanel.add(nameField);
            inputPanel.add(new JLabel("Role:"));
            roleField = new JTextField();
            inputPanel.add(roleField);
            add(inputPanel, BorderLayout.NORTH);

            JButton addButton = new JButton("Add Staff");
            addButton.addActionListener(e -> addStaff());
            JButton updateButton = new JButton("Update Staff");
            updateButton.addActionListener(e -> updateStaff());
            JButton deleteButton = new JButton("Delete Staff");
            deleteButton.addActionListener(e -> deleteStaff());
            JButton exportButton = new JButton("Export to PDF");
            exportButton.addActionListener(e -> exportStaffToPDF());

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(addButton);
            buttonPanel.add(updateButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(exportButton);
            add(buttonPanel, BorderLayout.SOUTH);

            staffListArea = new JTextArea();
            staffListArea.setEditable(false);
            add(new JScrollPane(staffListArea), BorderLayout.CENTER);

            loadStaff();
        }

        private void addStaff() {
            try {
                String name = nameField.getText();
                String role = roleField.getText();

                String sql = "INSERT INTO staff (staff_name, staff_role) VALUES (?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, role);
                pstmt.executeUpdate();
                loadStaff();
                clearFields();
            } catch (SQLException e) {
                showError("Add Staff Error", e.getMessage());
            }
        }

        private void updateStaff() {
            try {
                int id = Integer.parseInt(idField.getText());
                String name = nameField.getText();
                String role = roleField.getText();

                String sql = "UPDATE staff SET staff_name = ?, staff_role = ? WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, role);
                pstmt.setInt(3, id);
                int rowsUpdated = pstmt.executeUpdate();

                if (rowsUpdated > 0) {
                    loadStaff();
                    clearFields();
                    JOptionPane.showMessageDialog(this, "Staff updated successfully.");
                } else {
                    showError("Update Staff Error", "No staff found with that ID.");
                }
            } catch (NumberFormatException e) {
                showError("Input Error", "ID must be a number.");
            } catch (SQLException e) {
                showError("Update Staff Error", e.getMessage());
            }
        }

        private void deleteStaff() {
            try {
                int id = Integer.parseInt(idField.getText());
                String sql = "DELETE FROM staff WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, id);
                int rowsDeleted = pstmt.executeUpdate();

                if (rowsDeleted > 0) {
                    loadStaff();
                    clearFields();
                    JOptionPane.showMessageDialog(this, "Staff deleted successfully.");
                } else {
                    showError("Delete Staff Error", "No staff found with that ID.");
                }
            } catch (NumberFormatException e) {
                showError("Input Error", "ID must be a number.");
            } catch (SQLException e) {
                showError("Delete Staff Error", e.getMessage());
            }
        }

        private void clearFields() {
            idField.setText("");
            nameField.setText("");
            roleField.setText("");
        }

        private void loadStaff() {
            try {
                String sql = "SELECT * FROM staff";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                staffListArea.setText("");
                while (rs.next()) {
                    staffListArea.append("ID: " + rs.getInt("id") +
                            ", Name: " + rs.getString("staff_name") +
                            ", Role: " + rs.getString("staff_role") + "\n");
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                showError("Load Staff Error", e.getMessage());
            }
        }
    private void exportStaffToPDF() {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream("Staffs.pdf"));
            document.open();
            document.add(new com.itextpdf.text.Paragraph("List of Staffs"));
            document.add(new com.itextpdf.text.Paragraph(" "));

            String sql = "SELECT * FROM staff";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                document.add(new com.itextpdf.text.Paragraph(
                        "ID: " + rs.getInt("id") +
                                ", Name: " + rs.getString("staff_name") +
                                ", Role: " + rs.getString("staff_role")));
            }

            document.close();
            JOptionPane.showMessageDialog(this, "PDF exported successfully.");
        } catch (Exception e) {
            showError("PDF Export Error", e.getMessage());
        }
    }
}

    // Checkup Management Panel
    class CheckupManagementPanel extends JPanel {
        private JTextField idField, animalIdField, dateField, doctorAssignedField, descriptionField, cureField;
        private JTextArea checkupListArea;

        public CheckupManagementPanel() {
            setLayout(new BorderLayout());
            JPanel inputPanel = new JPanel(new GridLayout(7, 2)); // Changed to 7 rows for the new field
            inputPanel.add(new JLabel("ID (for update/delete):"));
            idField = new JTextField();
            inputPanel.add(idField);
            inputPanel.add(new JLabel("Animal ID:"));
            animalIdField = new JTextField();
            inputPanel.add(animalIdField);
            inputPanel.add(new JLabel("Checkup Date (YYYY-MM-DD):"));
            dateField = new JTextField();
            inputPanel.add(dateField);
            inputPanel.add(new JLabel("Doctor Assigned:"));
            doctorAssignedField = new JTextField();
            inputPanel.add(doctorAssignedField);
            inputPanel.add(new JLabel("Description:"));
            descriptionField = new JTextField();
            inputPanel.add(descriptionField);
            inputPanel.add(new JLabel("Cure:")); // New label for cure
            cureField = new JTextField(); // New field for cure
            inputPanel.add(cureField);
            add(inputPanel, BorderLayout.NORTH);

            JButton addButton = new JButton("Add Checkup");
            addButton.addActionListener(e -> addCheckup());
            JButton updateButton = new JButton("Update Checkup");
            updateButton.addActionListener(e -> updateCheckup());
            JButton deleteButton = new JButton("Delete Checkup");
            deleteButton.addActionListener(e -> deleteCheckup());

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(addButton);
            buttonPanel.add(updateButton);
            buttonPanel.add(deleteButton);
            add(buttonPanel, BorderLayout.SOUTH);

            checkupListArea = new JTextArea();
            checkupListArea.setEditable(false);
            add(new JScrollPane(checkupListArea), BorderLayout.CENTER);

            loadCheckups();
        }

        private void addCheckup() {
            try {
                int animalId = Integer.parseInt(animalIdField.getText());
                Date checkupDate = Date.valueOf(dateField.getText());
                String doctorAssigned = doctorAssignedField.getText();
                String description = descriptionField.getText();
                String cure = cureField.getText(); // Get the cure

                String sql = "INSERT INTO checkups (animal_id, checkup_date, doctor_assigned, description, cure) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, animalId);
                pstmt.setDate(2, checkupDate);
                pstmt.setString(3, doctorAssigned);
                pstmt.setString(4, description);
                pstmt.setString(5, cure); // Set the cure
                pstmt.executeUpdate();
                loadCheckups();
                clearFields();
            } catch (SQLException e) {
                showError("Add Checkup Error", e.getMessage());
            } catch (IllegalArgumentException e) {
                showError("Input Error", "Invalid date format. Use YYYY-MM-DD.");
            }
        }

        private void updateCheckup() {
            try {
                int id = Integer.parseInt(idField.getText());
                int animalId = Integer.parseInt(animalIdField.getText());
                Date checkupDate = Date.valueOf(dateField.getText());
                String doctorAssigned = doctorAssignedField.getText();
                String description = descriptionField.getText();
                String cure = cureField.getText(); // Get the cure

                String sql = "UPDATE checkups SET animal_id = ?, checkup_date = ?, doctor_assigned = ?, description = ?, cure = ? WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, animalId);
                pstmt.setDate(2, checkupDate);
                pstmt.setString(3, doctorAssigned);
                pstmt.setString(4, description);
                pstmt.setString(5, cure); // Set the cure
                pstmt.setInt(6, id);
                int rowsUpdated = pstmt.executeUpdate();

                if (rowsUpdated > 0) {
                    loadCheckups();
                    clearFields();
                    JOptionPane.showMessageDialog(this, "Checkup updated successfully.");
                } else {
                    showError("Update Checkup Error", "No checkup found with that ID.");
                }
            } catch (NumberFormatException e) {
                showError("Input Error", "ID and Animal ID must be numbers.");
            } catch (IllegalArgumentException e) {
                showError("Input Error", "Invalid date format. Use YYYY-MM-DD.");
            } catch (SQLException e) {
                showError("Update Checkup Error", e.getMessage());
            }
        }

        private void deleteCheckup() {
            try {
                int id = Integer.parseInt(idField.getText());
                String sql = "DELETE FROM checkups WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, id);
                int rowsDeleted = pstmt.executeUpdate();

                if (rowsDeleted > 0) {
                    loadCheckups();
                    clearFields();
                    JOptionPane.showMessageDialog(this, "Checkup deleted successfully.");
                } else {
                    showError("Delete Checkup Error", "No checkup found with that ID.");
                }
            } catch (NumberFormatException e) {
                showError("Input Error", "ID must be a number.");
            } catch (SQLException e) {
                showError("Delete Checkup Error", e.getMessage());
            }
        }

        private void clearFields() {
            idField.setText("");
            animalIdField.setText("");
            dateField.setText("");
            doctorAssignedField.setText("");
            descriptionField.setText("");
            cureField.setText(""); // Clear the cure field
        }

        private void loadCheckups() {
            try {
                String sql = "SELECT * FROM checkups";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                checkupListArea.setText("");
                while (rs.next()) {
                    checkupListArea.append("ID: " + rs.getInt("id") +
                            ", Animal ID: " + rs.getInt("animal_id") +
                            ", Checkup Date: " + rs.getDate("checkup_date") +
                            ", Doctor Assigned: " + rs.getString("doctor_assigned") +
                            ", Description: " + rs.getString("description") +
                            ", Cure: " + rs.getString("cure") + "\n"); // Include the cure
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                showError("Load Checkups Error", e.getMessage());
            }
        }
    }



    // Doctor Management Panel
    class DoctorManagementPanel extends JPanel {
        private JTextField idField, nameField, availableField; // Added availableField
        private JTextArea doctorListArea;

        public DoctorManagementPanel() {
            setLayout(new BorderLayout());
            JPanel inputPanel = new JPanel(new GridLayout(4, 2));
            inputPanel.add(new JLabel("ID (for update/delete):"));
            idField = new JTextField();
            inputPanel.add(idField);
            inputPanel.add(new JLabel("Doctor Name:"));
            nameField = new JTextField();
            inputPanel.add(nameField);
            inputPanel.add(new JLabel("Available (true/false):"));
            availableField = new JTextField(); // Initialize availableField
            inputPanel.add(availableField);
            add(inputPanel, BorderLayout.NORTH);

            JButton addButton = new JButton("Add Doctor");
            addButton.addActionListener(e -> addDoctor());
            JButton updateButton = new JButton("Update Doctor");
            updateButton.addActionListener(e -> updateDoctor());
            JButton deleteButton = new JButton("Delete Doctor");
            deleteButton.addActionListener(e -> deleteDoctor());

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(addButton);
            buttonPanel.add(updateButton);
            buttonPanel.add(deleteButton);
            add(buttonPanel, BorderLayout.SOUTH);

            doctorListArea = new JTextArea();
            doctorListArea.setEditable(false);
            add(new JScrollPane(doctorListArea), BorderLayout.CENTER);

            loadDoctors();
        }

        private void addDoctor() {
            try {
                String name = nameField.getText();
                boolean available = Boolean.parseBoolean(availableField.getText());

                String sql = "INSERT INTO doctors (doctor_name, available) VALUES (?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setBoolean(2, available);
                pstmt.executeUpdate();
                loadDoctors();
                clearFields();
            } catch (SQLException e) {
                showError("Add Doctor Error", e.getMessage());
            }
        }

        private void updateDoctor() {
            try {
                int id = Integer.parseInt(idField.getText());
                String name = nameField.getText();
                boolean available = Boolean.parseBoolean(availableField.getText());

                String sql = "UPDATE doctors SET doctor_name = ?, available = ? WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setBoolean(2, available);
                pstmt.setInt(3, id);
                int rowsUpdated = pstmt.executeUpdate();

                if (rowsUpdated > 0) {
                    loadDoctors();
                    clearFields();
                    JOptionPane.showMessageDialog(this, "Doctor updated successfully.");
                } else {
                    showError("Update Doctor Error", "No doctor found with that ID.");
                }
            } catch (NumberFormatException e) {
                showError("Input Error", "ID must be a number.");
            } catch (SQLException e) {
                showError("Update Doctor Error", e.getMessage());
            }
        }

        private void deleteDoctor() {
            try {
                int id = Integer.parseInt(idField.getText());
                String sql = "DELETE FROM doctors WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, id);
                int rowsDeleted = pstmt.executeUpdate();

                if (rowsDeleted > 0) {
                    loadDoctors();
                    clearFields();
                    JOptionPane.showMessageDialog(this, "Doctor deleted successfully.");
                } else {
                    showError("Delete Doctor Error", "No doctor found with that ID.");
                }
            } catch (NumberFormatException e) {
                showError("Input Error", "ID must be a number.");
            } catch (SQLException e) {
                showError("Delete Doctor Error", e.getMessage());
            }
        }

        private void clearFields() {
            idField.setText("");
            nameField.setText("");
            availableField.setText("");
        }

        private void loadDoctors() {
            try {
                String sql = "SELECT * FROM doctors";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                doctorListArea.setText("");
                while (rs.next()) {
                    doctorListArea.append("ID: " + rs.getInt("id") +
                            ", Name: " + rs.getString("doctor_name") +
                            ", Available: " + rs.getBoolean("available") + "\n");
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                showError("Load Doctors Error", e.getMessage());
            }
        }
    }
}
