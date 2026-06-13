import java.sql.*;

public class QueryDb {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        String url = "jdbc:h2:./seatSync-db;MODE=PostgreSQL";
        Connection conn = DriverManager.getConnection(url, "sa", "");
        Statement stmt = conn.createStatement();

        try {
            System.out.println("=== SEATSYNC_MEMBERSHIP table ===");
            ResultSet rs = stmt.executeQuery("SELECT * FROM \"SeatSync\".\"SEATSYNC_MEMBERSHIP\"");
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                System.out.print(meta.getColumnName(i) + "\t");
            }
            System.out.println();
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("Error reading SEATSYNC_MEMBERSHIP: " + e.getMessage());
        }
        
        conn.close();
    }
}
