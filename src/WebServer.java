package src;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class WebServer {
    private static final ProductDAO productDAO = new ProductDAO();
    private static final BillingDAO billingDAO = new BillingDAO();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Route 0: Login Page & Authentication
        server.createContext("/login", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String html = Files.readString(Paths.get("web/login.html"));
                sendHtml(exchange, html);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseForm(exchange);
                String username = params.getOrDefault("username", "");
                String password = params.getOrDefault("password", "");

                if ("admin".equals(username) && "skstore123".equals(password)) {
                    exchange.getResponseHeaders().add("Set-Cookie", "sk_session=true; Path=/; HttpOnly");
                    redirect(exchange, "/inventory");
                } else {
                    redirect(exchange, "/login?error=1");
                }
            }
        });

        // Logout Route
        server.createContext("/logout", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "sk_session=; Path=/; Max-Age=0");
            redirect(exchange, "/login");
        });

        // Route 1: Inventory Page (Protected)
        server.createContext("/inventory", exchange -> {
            if (!isLoggedIn(exchange)) {
                redirect(exchange, "/login");
                return;
            }
            String html = Files.readString(Paths.get("web/inventory.html"));
            StringBuilder rows = new StringBuilder();
            for (String[] p : productDAO.getAllProducts()) {
                rows.append("<tr>")
                    .append("<td>").append(p[0]).append("</td>")
                    .append("<td>").append(p[1]).append("</td>")
                    .append("<td>").append(p[2]).append("</td>")
                    .append("<td>Rs. ").append(p[3]).append("</td>")
                    .append("<td>").append(p[4]).append("</td>")
                    .append("<td><form action='/delete-product' method='POST' style='margin:0;'>")
                    .append("<input type='hidden' name='product_id' value='").append(p[0]).append("'>")
                    .append("<button type='submit'>Delete</button></form></td>")
                    .append("</tr>");
            }
            sendHtml(exchange, html.replace("{{PRODUCT_ROWS}}", rows.toString()));
        });

        // Route 2: Add Product (Protected)
        server.createContext("/add-product", exchange -> {
            if (!isLoggedIn(exchange)) {
                redirect(exchange, "/login");
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseForm(exchange);
                productDAO.addProduct(
                    params.get("product_name"),
                    params.get("category"),
                    Double.parseDouble(params.get("price")),
                    Integer.parseInt(params.get("stock_quantity"))
                );
            }
            redirect(exchange, "/inventory");
        });

        // Route 3: Delete Product (Protected)
        server.createContext("/delete-product", exchange -> {
            if (!isLoggedIn(exchange)) {
                redirect(exchange, "/login");
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseForm(exchange);
                productDAO.deleteProduct(Integer.parseInt(params.get("product_id")));
            }
            redirect(exchange, "/inventory");
        });

        // Route 4: Billing Page (Protected)
        server.createContext("/billing", exchange -> {
            if (!isLoggedIn(exchange)) {
                redirect(exchange, "/login");
                return;
            }
            String html = Files.readString(Paths.get("web/billing.html"));
            StringBuilder options = new StringBuilder();
            for (String[] p : productDAO.getAllProducts()) {
                options.append("<option value='").append(p[0]).append("'>")
                       .append(p[1]).append(" - Rs. ").append(p[3])
                       .append(" (Stock: ").append(p[4]).append(")")
                       .append("</option>");
            }
            String query = exchange.getRequestURI().getQuery();
            String msg = "";
            if (query != null && query.startsWith("msg=")) {
                msg = URLDecoder.decode(query.substring(4), StandardCharsets.UTF_8);
            }
            html = html.replace("{{PRODUCT_OPTIONS}}", options.toString())
                       .replace("{{BILL_MESSAGE}}", msg);
            sendHtml(exchange, html);
        });

        // Route 5: Create Bill (Protected)
        server.createContext("/create-bill", exchange -> {
            if (!isLoggedIn(exchange)) {
                redirect(exchange, "/login");
                return;
            }
            String result = "";
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseForm(exchange);
                result = billingDAO.createBill(
                    params.get("customer_name"),
                    Integer.parseInt(params.get("product_id")),
                    Integer.parseInt(params.get("quantity"))
                );
            }
            redirect(exchange, "/billing?msg=" + URLEncoder.encode(result, StandardCharsets.UTF_8));
        });

        server.start();
        System.out.println("Server started! Open http://localhost:8080/login in your browser.");
    }

    private static boolean isLoggedIn(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        return cookie != null && cookie.contains("sk_session=true");
    }

    private static void sendHtml(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                );
            }
        }
        return map;
    }
}