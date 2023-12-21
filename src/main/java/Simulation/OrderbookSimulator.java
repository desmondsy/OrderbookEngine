package Simulation;

import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Orders.Order;
import Orders.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public class OrderbookSimulator {

    private static final Logger logger = LogManager.getLogger(OrderbookSimulator.class);

    private static final int INIT_ITERATIONS = 100; // build the book, no matching during this phase
    private static final int ITERATIONS = 1000; // post init
    private static final double BID_INIT = 100d;
    private static final double ASK_INIT = 101d;
    private static final int TICK_SIZE = 1;

    public static void main(String[] args) {
        OrderbookSimulator obs = new OrderbookSimulator();
        Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher("pricetime"));

        // init with one order on each side
        ob.addOrder(new Order(0, Side.BUY, 50, BID_INIT));
        ob.addOrder(new Order(0, Side.SELL, 50, ASK_INIT));

        Map<Event, Double> eventProbabilitiesMap = EventProbabilitiesLoader.createEventProbabilitiesMap("eventProbabilities.properties");
        Map<Event, Double> initEventProbabilitiesMap = EventProbabilitiesLoader.createEventProbabilitiesMap("initEventProbabilities.properties");
        double[] decayingProbabilitiesArr = obs.generateDecayingProbabilities(20);

        // init book
        for (int i=0;i<INIT_ITERATIONS;i++)
        {
            Event event = obs.pickEvent(initEventProbabilitiesMap);
            int distanceFrom = obs.selectIndexWithProbability(decayingProbabilitiesArr);
            int volume = generateRandomNumber(1, 100);
            double price;

            if (event == Event.PASSIVE_BUY)
            {
                price = ob.getBestAsk() - distanceFrom * TICK_SIZE;
                ob.addOrder(new Order(0, Side.BUY, volume, price));
            }
            else if (event == Event.PASSIVE_SELL)
            {
                price = ob.getBestBid() + distanceFrom * TICK_SIZE;
                ob.addOrder(new Order(0, Side.SELL, volume, price));
            }
            else
            {
                logger.error("init events must either be passive buy or sell only.");
            }
        }

        // simulate
        for (int i=0;i<ITERATIONS;i++)
        {
            Event event = obs.pickEvent(eventProbabilitiesMap);

        }

        ob.printOrderbook();
    }

    private double[] generateDecayingProbabilities(int size) {
        double[] probabilities = new double[size];
        double sum = 0.0;

        for (int i = 0; i < size; i++) {
            double probability = 1.0 / (Math.pow(i + 2, 1.75));  // Logarithmic decay formula
            probabilities[i] = probability;
            sum += probability;
        }

        // Normalize probabilities
        for (int i = 0; i < size; i++) {
            probabilities[i] /= sum;
        }

        return probabilities;
    }

    private Event pickEvent(Map<Event, Double> eventProbabilitiesMap)
    {
        Random random = new Random();
        double randomValue = random.nextDouble();
        double cumulativeProbability = 0.0;

        for (Map.Entry<Event, Double> entry : eventProbabilitiesMap.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (randomValue <= cumulativeProbability) {
                return entry.getKey();
            }
        }

        return null;
    }

    private int selectIndexWithProbability(double[] probabilities)
    {
        Random random = new Random();
        double cumulativeProbability = 0d;

        for (int i = 0; i < probabilities.length; i++) {
            cumulativeProbability += probabilities[i];
            if (random.nextDouble() <= cumulativeProbability) {
                return i+1;
            }
        }

        return -1;
    }

    public static int generateRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }
}
