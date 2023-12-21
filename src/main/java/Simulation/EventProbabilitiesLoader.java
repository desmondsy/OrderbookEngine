package Simulation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class EventProbabilitiesLoader {

    public static Map<Event, Double> createEventProbabilitiesMap(String propertiesFileName) {
        Map<Event, Double> eventProbabilities = new HashMap<>();

        Properties properties = new Properties();
        try (InputStream input = EventProbabilitiesLoader.class.getClassLoader().getResourceAsStream(propertiesFileName)) {
            if (input == null) {
                throw new IOException("Failed to load properties file: " + propertiesFileName);
            }
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return eventProbabilities; // Return an empty map if the properties file cannot be loaded
        }

        double sum = 0d;

        for (Event event : Event.values()) {
            String key = event.name();
            String value = properties.getProperty(key);
            if (value != null) {
                double probability = Double.parseDouble(value);
                sum += probability;
                eventProbabilities.put(event, probability);
            }
        }
        System.out.println(sum);
        if (sum != 1.0d)
        {
            throw new RuntimeException("eventProbabilities do not sum to 1.");
        }

        return eventProbabilities;
    }

}
