package src;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<String[]> getAllProducts() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT product_id, product_name, category, price, stock_quantity FROM products ORDER BY product_id";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("product_id")),
                    rs.getString("product_name"),
                    rs.getString("category"),
                    String.format("%.2f", rs.getDouble("price")),
                    String.valueOf(rs.getInt("stock_quantity"))
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addProduct(String name, String category, double price, int stock) {
        String sql = "INSERT INTO products (product_name, category, price, stock_quantity) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setDouble(3, price);
            stmt.setInt(4, stock);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
