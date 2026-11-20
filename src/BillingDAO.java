package src;
import java.sql.*;

public class BillingDAO {

    public String createBill(String customerName, int productId, int quantity) {
        String checkSql = "SELECT product_name, price, stock_quantity FROM products WHERE product_id = ?";
        String insertBillSql = "INSERT INTO bills (customer_name, total_amount) VALUES (?, ?)";
        String insertItemSql = "INSERT INTO bill_items (bill_id, product_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
        String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            // 1. Check product price and available stock
            String productName;
            double price;
            int currentStock;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, productId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    return "Error: Product not found.";
                }
                productName = rs.getString("product_name");
                price = rs.getDouble("price");
                currentStock = rs.getInt("stock_quantity");
            }

            if (quantity <= 0 || quantity > currentStock) {
                return "Error: Insufficient stock. Available: " + currentStock;
            }

            double totalAmount = price * quantity;

            // 2. Insert into bills table and get generated bill_id
            int billId;
            try (PreparedStatement billStmt = conn.prepareStatement(insertBillSql, Statement.RETURN_GENERATED_KEYS)) {
                billStmt.setString(1, customerName);
                billStmt.setDouble(2, totalAmount);
                billStmt.executeUpdate();
                ResultSet keys = billStmt.getGeneratedKeys();
                keys.next();
                billId = keys.getInt(1);
            }

            // 3. Insert into bill_items table
            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql)) {
                itemStmt.setInt(1, billId);
                itemStmt.setInt(2, productId);
                itemStmt.setInt(3, quantity);
                itemStmt.setDouble(4, totalAmount);
                itemStmt.executeUpdate();
            }

            // 4. Deduct stock from products table
            try (PreparedStatement stockStmt = conn.prepareStatement(updateStockSql)) {
                stockStmt.setInt(1, quantity);
                stockStmt.setInt(2, productId);
                stockStmt.executeUpdate();
            }

            conn.commit(); // Save all changes
            return "Bill #" + billId + " generated for " + customerName + " | " + productName + " x " + quantity + " = Rs. " + String.format("%.2f", totalAmount);

        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }
}