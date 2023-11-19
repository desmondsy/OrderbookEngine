import Orders.Order;
import Orders.Side;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class OrderGeneratorTest {
    public static List<Order> readOrdersFromFile(String filePath) {
        List<Order> orders = new ArrayList<>();

        try (InputStream inputStream = OrderGeneratorTest.class.getClassLoader().getResourceAsStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            // Read the header line
            String header = reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(""))
                    continue;
                String[] values = line.split(",");
                int securityId = Integer.parseInt(values[0].trim());
                Side side = Side.valueOf(values[1].trim());
                int quantity = Integer.parseInt(values[2].trim());
                double price = Double.parseDouble(values[3].trim());

                Order order = new Order(securityId, side, quantity, price);
                orders.add(order);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return orders;
    }
}
