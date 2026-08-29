package eauction;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuctionListServlet extends HttpServlet {

    protected void processRequest(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        String url =
                "jdbc:oracle:thin:@localhost:1521:xe";

        String username = "system";

        String dbPassword = "manager";

        Connection con = null;

        PreparedStatement ps = null;

        ResultSet rs = null;

        PrintWriter out =
                response.getWriter();

        try {

            Class.forName(
                    "oracle.jdbc.OracleDriver"
            );

            con = DriverManager.getConnection(
                    url,
                    username,
                    dbPassword
            );

            String sql =
                    "SELECT "
                    + "a.auction_id, "
                    + "a.product_id, "
                    + "p.name, "
                    + "p.description, "
                    + "p.image_reference, "
                    + "p.start_price, "
                    + "a.current_price, "
                    + "a.start_time, "
                    + "a.end_time, "
                    + "a.status "
                    + "FROM auctions a "
                    + "JOIN products p "
                    + "ON a.product_id = p.product_id "
                    + "ORDER BY "
                    + "CASE a.status "
                    + "WHEN 'LIVE' THEN 1 "
                    + "WHEN 'SCHEDULED' THEN 2 "
                    + "WHEN 'SOLD' THEN 3 "
                    + "ELSE 4 "
                    + "END, "
                    + "a.start_time";

            ps = con.prepareStatement(sql);

            rs = ps.executeQuery();

            StringBuilder json =
                    new StringBuilder();

            json.append("[");

            boolean first = true;

            while (rs.next()) {

                if (!first) {
                    json.append(",");
                }

                first = false;

                json.append("{");

                json.append(
                        "\"auctionId\":"
                );

                json.append(
                        rs.getInt("auction_id")
                );

                json.append(",");

                json.append(
                        "\"productId\":"
                );

                json.append(
                        rs.getInt("product_id")
                );

                json.append(",");

                json.append(
                        "\"name\":\""
                );

                json.append(
                        escapeJson(
                                rs.getString("name")
                        )
                );

                json.append("\",");

                json.append(
                        "\"description\":\""
                );

                json.append(
                        escapeJson(
                                rs.getString(
                                        "description"
                                )
                        )
                );

                json.append("\",");

                json.append(
                        "\"imageReference\":\""
                );

                json.append(
                        escapeJson(
                                rs.getString(
                                        "image_reference"
                                )
                        )
                );

                json.append("\",");

                json.append(
                        "\"startPrice\":"
                );

                json.append(
                        rs.getBigDecimal(
                                "start_price"
                        )
                );

                json.append(",");

                json.append(
                        "\"currentPrice\":"
                );

                json.append(
                        rs.getBigDecimal(
                                "current_price"
                        )
                );

                json.append(",");

                json.append(
                        "\"startTime\":\""
                );

                json.append(
                        rs.getTimestamp(
                                "start_time"
                        ).toInstant().toString()
                );

                json.append("\",");

                json.append(
                        "\"endTime\":\""
                );

                json.append(
                        rs.getTimestamp(
                                "end_time"
                        ).toInstant().toString()
                );

                json.append("\",");

                json.append(
                        "\"status\":\""
                );

                json.append(
                        escapeJson(
                                rs.getString("status")
                        )
                );

                json.append("\"");

                json.append("}");
            }

            json.append("]");

            out.print(json.toString());

        } catch (Exception e) {

            e.printStackTrace();

            out.print(
                    "{\"error\":\""
                    + escapeJson(
                            e.getMessage()
                    )
                    + "\"}"
            );

        } finally {

            try {

                if (rs != null) {
                    rs.close();
                }

            } catch (Exception ignored) {
            }

            try {

                if (ps != null) {
                    ps.close();
                }

            } catch (Exception ignored) {
            }

            try {

                if (con != null) {
                    con.close();
                }

            } catch (Exception ignored) {
            }
        }
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(
                request,
                response
        );
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(
                request,
                response
        );
    }
}