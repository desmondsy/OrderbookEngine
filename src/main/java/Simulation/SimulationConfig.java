package Simulation;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class SimulationConfig {
    private String matchingEngine;
    private int bookEventDepth;
    private int initIterations;
    private int iterations;
    private double bidPriceInit;
    private double askPriceInit;
    private double tickSize;
    private double proRataFarTouchMinMultiplier; // pro rata order size has to be at least % of far touch
    private double proRataFarTouchMaxMultiplier;

    public SimulationConfig(String propertiesFilePath) {
        Properties properties = loadProperties(propertiesFilePath);
        setValuesFromProperties(properties); // TODO: handle null
        validateConfig();
    }

    private static Properties loadProperties(String filePath) {
        Properties properties = new Properties();
        try (InputStream input = SimulationConfig.class.getClassLoader().getResourceAsStream(filePath)) {
            if (input == null) {
                throw new IOException("Failed to load properties file: " + filePath);
            }
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return properties;
    }

    private void setValuesFromProperties(Properties properties) {
        this.matchingEngine = properties.getProperty("MATCHING_ENGINE");
        this.bookEventDepth = Integer.parseInt(properties.getProperty("BOOK_EVENT_DEPTH"));
        this.initIterations = Integer.parseInt(properties.getProperty("INIT_ITERATIONS"));
        this.iterations = Integer.parseInt(properties.getProperty("ITERATIONS"));
        this.bidPriceInit = Double.parseDouble(properties.getProperty("BID_INIT"));
        this.askPriceInit = Double.parseDouble(properties.getProperty("ASK_INIT"));
        this.tickSize = Double.parseDouble(properties.getProperty("TICK_SIZE"));
        this.proRataFarTouchMinMultiplier = Double.parseDouble(properties.getProperty("PRORATA_FAR_TOUCH_MIN_MULTIPLIER"));
        this.proRataFarTouchMaxMultiplier = Double.parseDouble(properties.getProperty("PRORATA_FAR_TOUCH_MAX_MULTIPLIER"));
    }

    private void validateConfig() {
        if (initIterations <= 0 || iterations <= 0 || bidPriceInit <= 0 || askPriceInit <= 0 || tickSize <= 0) {
            throw new IllegalArgumentException("Invalid configuration values. Please check the properties file.");
        }

        if (askPriceInit == bidPriceInit)
        {
            throw new IllegalArgumentException("bidPriceInit == askPriceInit.");
        }

        if (askPriceInit - bidPriceInit < tickSize)
        {
            throw new IllegalArgumentException("tickSize is larger than spread.");
        }

        if (proRataFarTouchMinMultiplier > 1 || proRataFarTouchMinMultiplier <= 0)
        {
            throw new IllegalArgumentException("proRataFarTouchMinMultiplier must be between 0 and 1.");
        }

        if (proRataFarTouchMaxMultiplier > 1)
        {
            throw new IllegalArgumentException("proRataFarTouchMaxMultiplier must be less than 1.");
        }
    }
}
