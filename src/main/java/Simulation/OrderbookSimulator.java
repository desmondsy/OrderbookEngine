package Simulation;

import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Orders.Order;
import Orders.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Random;

public class OrderbookSimulator {

    private static final Logger logger = LogManager.getLogger(OrderbookSimulator.class);
    private Orderbook ob;

    private final String MATCHING_ENGINE;
    private final int INIT_ITERATIONS; // build the book, no matching during this phase
    private final int ITERATIONS; // post init
    private final double BID_INIT;
    private final double ASK_INIT;
    private final double TICK_SIZE;

    private final double[] decayingProbabilitiesArr;

    public OrderbookSimulator(Orderbook ob, SimulationConfig simulationConfig)
    {
        this.ob = ob;
        this.MATCHING_ENGINE = simulationConfig.getMatchingEngine();
        this.INIT_ITERATIONS = simulationConfig.getInitIterations();
        this.ITERATIONS = simulationConfig.getInitIterations();
        this.BID_INIT = simulationConfig.getBidPriceInit();
        this.ASK_INIT = simulationConfig.getAskPriceInit();
        this.TICK_SIZE = simulationConfig.getTickSize();

        this.decayingProbabilitiesArr = generateDecayingProbabilities(simulationConfig.getBookEventDepth());
    }

    public void simulate()
    {
        initializeOrderbook();
        simulateEvents();
    }

    private void initializeOrderbook()
    {
        // init with one order on each side
        ob.addOrder(new Order(0, Side.BUY, 50, BID_INIT));
        ob.addOrder(new Order(0, Side.SELL, 50, ASK_INIT));

        // initialize the order book with passive orders only on each side.
        for (int i=0;i<INIT_ITERATIONS;i++) {
            double random = Math.random();

            int distanceFrom = selectIndexWithProbability(decayingProbabilitiesArr);
            int volume = generateRandomNumber(1, 100);
            double price;

            if (random > 0.5)
            {
                // PASSIVE BUY
                price = ob.getBestAsk() - distanceFrom * TICK_SIZE;
                ob.addOrder(new Order(0, Side.BUY, volume, price));
            }
            else
            {
                // PASSIVE SELL
                price = ob.getBestBid() + distanceFrom * TICK_SIZE;
                ob.addOrder(new Order(0, Side.SELL, volume, price));
            }
        }
    }

    private void simulateEvents()
    {
        Map<Event, Double> eventProbabilitiesMap = EventProbabilitiesLoader.createEventProbabilitiesMap("eventProbabilitiesDowntrendBias.properties");

        // simulate
        for (int i=0;i<ITERATIONS;i++)
        {

            Event event = pickEvent(eventProbabilitiesMap);
            int distanceFrom = selectIndexWithProbability(decayingProbabilitiesArr);
            double price;

            logger.info("\n");

            if (event == Event.PASSIVE_BUY)
            {
                int volume = generateRandomNumber(1, 100);
                price = ob.getBestAsk() - distanceFrom * TICK_SIZE;

                logger.info("New event: PASSIVE_BUY - creating new order with price: {}, qty: {}", price, volume);
                ob.addOrder(new Order(0, Side.BUY, volume, price));
            }
            else if (event == Event.PASSIVE_SELL)
            {
                int volume = generateRandomNumber(1, 100);
                price = ob.getBestBid() + distanceFrom * TICK_SIZE;

                logger.info("New event: PASSIVE_SELL - creating new order with price: {}, qty: {}", price, volume);
                ob.addOrder(new Order(0, Side.SELL, volume, price));
            }
            else if (event == Event.AGGRESSIVE_BUY)
            {
                ob.printOrderbookWithOrders();
                double rand = Math.random();

                int volume = generateRandomNumber(1, 100);
                if (rand > 0.5)
                {
                    // market order
                    logger.info("New event: AGGRESSIVE_BUY (market) - volume: {}", volume);
                    ob.addOrder(new Order(0, Side.BUY, volume, null));
                }
                else
                {
                    // aggressive limit order - far touch
                    price = ob.getBestAsk();
                    logger.info("New event: AGGRESSIVE_BUY (limit) - price: {}, volume: {}", price, volume);
                    ob.addOrder(new Order(0, Side.BUY, volume, price));
                }
            }
            else if (event == Event.AGGRESSIVE_SELL)
            {
                ob.printOrderbookWithOrders();
                double rand = Math.random();

                int volume = generateRandomNumber(1, 100);
                if (rand > 0)
                {
                    // market order
                    logger.info("New event: AGGRESSIVE_SELL (market) - volume: {}", volume);
                    ob.addOrder(new Order(0, Side.SELL, volume, null));
                }
                else
                {
                    // aggressive limit order - far touch
                    price = ob.getBestBid();
                    logger.info("New event: AGGRESSIVE_SELL (market) - price: {}, volume: {}", price, volume);
                    ob.addOrder(new Order(0, Side.SELL, volume, price));
                }
            }
            else if (event == Event.MOD_BUY)
            {
                // mod buy price or mod buy qty, 50/50 probability
                price = ob.getBestAsk() - distanceFrom * TICK_SIZE;
                logger.info("New event: MOD_BUY - going to mod a random order at the {} price level.", price);
                if (ob.getBuyOrderIds().get(price) == null || ob.getBuyOrderIds().get(price).isEmpty())
                {
                    logger.info("MOD_BUY - price level does not exist yet or there are no orders on that pricw level. Not going to mod.");
                    continue;
                }

                int buyOrderIdToMod = ob.getBuyOrderIds().get(price).chooseRandomItem();

                double rand = Math.random();
                if (rand > 0)
                {
                    // mod qty
                    int newQty = generateRandomNumber(1, 100);

                    logger.info("New event: MOD_BUY - going to mod orderID: {}, qty from {} to {}",
                            buyOrderIdToMod, ob.getOrderMap().get(buyOrderIdToMod).getCurrentQuantity(), newQty);
                    ob.modifyOrderQty(buyOrderIdToMod, newQty);
                }
                else
                {
                    // TODO: mod price, use rand > 0 to skip for now
                }
            }
            else if (event == Event.MOD_SELL)
            {
                // mod sell price or mod sell qty, 50/50 probability
                price = ob.getBestBid() + distanceFrom * TICK_SIZE;
                logger.info("New event: MOD_SELL - going to mod a random order at the {} price level.", price);
                if (ob.getSellOrderIds().get(price) == null || ob.getSellOrderIds().get(price).isEmpty())
                {
                    logger.info("MOD_SELL - price level does not exist yet or there are no orders on that pricw level. Not going to mod.");
                    continue;
                }

                int sellOrderIdToMod = ob.getSellOrderIds().get(price).chooseRandomItem();


                double rand = Math.random();
                if (rand > 0)
                {
                    // mod qty
                    int newQty = generateRandomNumber(1, 100);
                    logger.info("New event: MOD_SELL - going to mod orderID: {}, qty from {} to {}",
                            sellOrderIdToMod, ob.getOrderMap().get(sellOrderIdToMod).getCurrentQuantity(), newQty);
                    ob.modifyOrderQty(sellOrderIdToMod, newQty);
                }
                else
                {
                    // TODO: mod price, use rand > 0 to skip for now

                }
            }
            else if (event == Event.CANCEL_BUY)
            {
                price = ob.getBestAsk() - distanceFrom * TICK_SIZE;
                logger.info("New event: CANCEL_BUY - going to cancel a random order at the {} price level.", price);

                if (ob.getBuyOrderIds().get(price) == null || ob.getBuyOrderIds().get(price).isEmpty())
                {
                    logger.info("CANCEL_BUY - price level does not exist yet or there are no orders on that pricw level. Skipping.");
                    continue;
                }

                int buyOrderIdToCancel = ob.getBuyOrderIds().get(price).chooseRandomItem();
                logger.info("CANCEL_BUY - going to cancel orderID: {}", buyOrderIdToCancel);
                ob.removeOrder(buyOrderIdToCancel, false);
            }
            else if (event == Event.CANCEL_SELL)
            {
                price = ob.getBestBid() + distanceFrom * TICK_SIZE;
                logger.info("New event: CANCEL_SELL - going to cancel a random order at the {} price level.", price);

                if (ob.getSellOrderIds().get(price) == null || ob.getSellOrderIds().get(price).isEmpty())
                {
                    logger.info("CANCEL_SELL - price level does not exist yet or there are no orders on that pricw level. Skipping.");
                    continue;
                }

                int sellOrderIdToCancel = ob.getSellOrderIds().get(price).chooseRandomItem();
                logger.info("CANCEL_SELL - going to cancel orderID: {}", sellOrderIdToCancel);
                ob.removeOrder(sellOrderIdToCancel, false);
            }

        }

        ob.printOrderbook();
    }

    private int generateVolume()
    {
        if (MATCHING_ENGINE.contains("prorata"))
        {
            // config multiple of farTouch
        }

        return 0;
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
