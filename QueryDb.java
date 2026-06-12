import java.sql.*;

public class QueryDb {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        String url = "jdbc:h2:./seatSync-db;MODE=PostgreSQL";
        Connection conn = DriverManager.getConnection(url, "sa", "");
        Statement stmt = conn.createStatement();
        
        // Get all user tables (not system tables)
        System.out.println("=== All User Tables ===");
        ResultSet rs = stmt.executeQuery(
            "SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='TABLE' ORDER BY TABLE_SCHEMA, TABLE_NAME"
        );
        while (rs.next()) {
            System.out.println(rs.getString("TABLE_SCHEMA") + "." + rs.getString("TABLE_NAME"));
        }
        
        conn.close();
    }
}
