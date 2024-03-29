package Simulation;

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
    private final String EVENT_PROBABILITIES_STYLE;
    private final int BOOK_EVENT_DEPTH;
    private final int INIT_ITERATIONS; // build the book, no matching during this phase
    private final int ITERATIONS; // post init
    private final double BID_INIT;
    private final double ASK_INIT;
    private final double TICK_SIZE;
    private final double PRORATA_FAR_TOUCH_MIN_MULTIPLIER;
    private final double PRORATA_FAR_TOUCH_MAX_MULTIPLIER;

    private final double[] decayingProbabilitiesArr;
    private final Map<Event, Double> eventProbabilitiesMap;

    private Random random;

    public OrderbookSimulator(Orderbook ob, SimulationConfig simulationConfig, Random random)
    {
        this.ob = ob;
        this.MATCHING_ENGINE = simulationConfig.getMatchingEngine();
        this.EVENT_PROBABILITIES_STYLE = simulationConfig.getEventProbabilitiesStyle();
        this.BOOK_EVENT_DEPTH = simulationConfig.getBookEventDepth();
        this.INIT_ITERATIONS = simulationConfig.getInitIterations();
        this.ITERATIONS = simulationConfig.getIterations();
        this.BID_INIT = simulationConfig.getBidPriceInit();
        this.ASK_INIT = simulationConfig.getAskPriceInit();
        this.TICK_SIZE = simulationConfig.getTickSize();
        this.PRORATA_FAR_TOUCH_MIN_MULTIPLIER = simulationConfig.getProRataFarTouchMinMultiplier();
        this.PRORATA_FAR_TOUCH_MAX_MULTIPLIER = simulationConfig.getProRataFarTouchMaxMultiplier();

        this.decayingProbabilitiesArr = generateDecayingProbabilities(simulationConfig.getBookEventDepth());
        this.eventProbabilitiesMap = EventProbabilitiesLoader.createEventProbabilitiesMap(simulationConfig.getEventProbabilitiesStyle());

        this.random = random;
    }

    public void run()
    {
        logParameters();
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
            int distanceFrom = selectIndexWithProbability(decayingProbabilitiesArr);
            int volume = generateRandomNumber(1, 100);
            double price;

            if (random.nextDouble() > 0.5)
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
        // core simulation method
        for (int i=0;i<ITERATIONS;i++)
        {
            Event event = pickEvent(eventProbabilitiesMap);
            logger.info("\n");

            if (event == null)
            {
                logger.error("event is null. Not processing further.");
                continue;
            }

            switch (event)
            {
                case PASSIVE_BUY:
                    processPassiveBuy();
                    break;

                case PASSIVE_SELL:
                    processPassiveSell();
                    break;

                case AGGRESSIVE_BUY:
                    processAggressiveBuy();
                    break;

                case AGGRESSIVE_SELL:
                    processAggressiveSell();
                    break;

                case MOD_BUY:
                    processModBuy();
                    break;

                case MOD_SELL:
                    processModSell();
                    break;

                case CANCEL_BUY:
                    processCancelBuy();
                    break;

                case CANCEL_SELL:
                    processCancelSell();
                    break;

                default:
                    break;
            }
        }

        ob.printOrderbook();
    }

    private void processPassiveBuy()
    {
        int distanceFrom = selectIndexWithProbability(decayingProbabilitiesArr);
        double price = ob.getBestAsk() - distanceFrom * TICK_SIZE;
        int volume = generateVolume(true, false);

        logger.info("New event: PASSIVE_BUY - creating new order with price: {}, qty: {}", price, volume);
        ob.addOrder(new Order(0, Side.BUY, volume, price));
    }

    private void processPassiveSell()
    {
        int distanceFrom = selectIndexWithProbability(decayingProbabilitiesArr);
        double price = ob.getBestBid() + distanceFrom * TICK_SIZE;
        int volume = generateVolume(false, false);

        logger.info("New event: PASSIVE_SELL - creating new order with price: {}, qty: {}", price, volume);
        ob.addOrder(new Order(0, Side.SELL, volume, price));
    }

    private void processAggressiveBuy()
    {
//        ob.printOrderbookWithOrders();

        int volume = generateVolume(true, true);
        if (random.nextDouble() > 0.5)
        {
            // market order
            logger.info("New event: AGGRESSIVE_BUY (market) - volume: {}", volume);
            ob.addOrder(new Order(0, Side.BUY, volume, null));
        }
        else
        {
            // aggressive limit order - far touch
            double price = ob.getBestAsk();
            logger.info("New event: AGGRESSIVE_BUY (limit) - price: {}, volume: {}", price, volume);
            ob.addOrder(new Order(0, Side.BUY, volume, price));
        }
    }

    private void processAggressiveSell()
    {
//        ob.printOrderbookWithOrders();

        int volume = generateVolume(false, true);
        if (random.nextDouble() > 0)
        {
            // market order
            logger.info("New event: AGGRESSIVE_SELL (market) - volume: {}", volume);
            ob.addOrder(new Order(0, Side.SELL, volume, null));
        }
        else
        {
            // aggressive limit order - far touch
            double price = ob.getBestBid();
            logger.info("New event: AGGRESSIVE_SELL (market) - price: {}, volume: {}", price, volume);
            ob.addOrder(new Order(0, Side.SELL, volume, price));
        }
    }

    private void processModBuy()
    {
        // mod buy price or mod buy qty, 50/50 probability
        int distanceFrom = selectIndexWithProbability(decayingProbabilitiesArr);
        double price = ob.getBestAsk() - distanceFrom * TICK_SIZE;
        logger.info("New event: MOD_BUY - going to mod a random order at the {} price level.", price);
        if (ob.getBuyOrderIds().get(price) == null || ob.getBuyOrderIds().get(price).isEmpty())
        {
            logger.info("MOD_BUY - price level does not exist yet or there are no orders on that pricw level. Not going to mod.");
            return;
        }

        int buyOrderIdToMod = ob.getBuyOrderIds().get(price).chooseRandomItem();

        if (random.nextDouble() > 0)
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

    private void processModSell()
    {
        // mod sell price or mod sell qty, 50/50 probability
        int distanceFrom = selectIndexWithProbability(decayingProbabilitiesArr);
        double price = ob.getBestBid() + distanceFrom * TICK_SIZE;
        logger.info("New event: MOD_SELL - going to mod a random order at the {} price level.", price);
        if (ob.getSellOrderIds().get(price) == null || ob.getSellOrderIds().get(price).isEmpty())
        {
            logger.info("MOD_SELL - price level does not exist yet or there are no orders on that pricw level. Not going to mod.");
            return;
        }

        int sellOrderIdToMod = ob.getSellOrderIds().get(price).chooseRandomItem();

        if (random.nextDouble() > 0)
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

    private void processCancelBuy()
    {
        int distanceFrom = selectIndexWithProbability(decayingProbabilitiesArr);
        double price = ob.getBestAsk() - distanceFrom * TICK_SIZE;
        logger.info("New event: CANCEL_BUY - going to cancel a random order at the {} price level.", price);

        if (ob.getBuyOrderIds().get(price) == null || ob.getBuyOrderIds().get(price).isEmpty())
        {
            logger.info("CANCEL_BUY - price level does not exist yet or there are no orders on that pricw level. Skipping.");
            return;
        }

        int buyOrderIdToCancel = ob.getBuyOrderIds().get(price).chooseRandomItem();
        logger.info("CANCEL_BUY - going to cancel orderID: {}", buyOrderIdToCancel);
        ob.removeOrder(buyOrderIdToCancel, false);
    }

    private void processCancelSell()
    {
        int distanceFrom = selectIndexWithProbability(decayingProbabilitiesArr);
        double price = ob.getBestBid() + distanceFrom * TICK_SIZE;
        logger.info("New event: CANCEL_SELL - going to cancel a random order at the {} price level.", price);

        if (ob.getSellOrderIds().get(price) == null || ob.getSellOrderIds().get(price).isEmpty())
        {
            logger.info("CANCEL_SELL - price level does not exist yet or there are no orders on that pricw level. Skipping.");
            return;
        }

        int sellOrderIdToCancel = ob.getSellOrderIds().get(price).chooseRandomItem();
        logger.info("CANCEL_SELL - going to cancel orderID: {}", sellOrderIdToCancel);
        ob.removeOrder(sellOrderIdToCancel, false);
    }

    private int generateVolume(boolean isBuy, boolean isAggressiveEvent)
    {
        if (MATCHING_ENGINE.contains("prorata") && isAggressiveEvent)
        {
            // random value between PRORATA_FAR_TOUCH_MIN_MULTIPLIER and PRORATA_FAR_TOUCH_MAX_MULTIPLIER
            double rand = PRORATA_FAR_TOUCH_MIN_MULTIPLIER + random.nextDouble() * (PRORATA_FAR_TOUCH_MAX_MULTIPLIER - PRORATA_FAR_TOUCH_MIN_MULTIPLIER);

            // for pro rata orders, we want to simulate larger order quantities in order to see the pro rata distribution effect more clearly
            if (isBuy)
            {
                logger.info("generating prorata volume. rand: {}, bestAskVolume: {}", rand, ob.getBestAskSize());
                return Math.max(1, (int) (rand * ob.getBestAskSize()));
            }
            else
            {
                logger.info("generating prorata volume. rand: {}, bestBidVolume: {}", rand, ob.getBestBidSize());
                return Math.max(1, (int) (rand * ob.getBestBidSize()));
            }
        }

        return generateRandomNumber(1, 100);
    }

    private double[] generateDecayingProbabilities(int size) {
        // generates an array of decaying probabilities used for determining where a particular event should be assigned to in the book
        // e.g. if a PASSIVE_BUY event is selected, we want to probabilistically assign a price level for it.
        // The price level is determined by the array output of this function, which has decaying values (e.g. price levels closer to the top of book are more likely to be generated as there is usually more activity near the BBO)

        double[] probabilities = new double[size];
        double sum = 0.0;

        for (int i = 0; i < size; i++) {
            double probability = 1.0 / (Math.pow(i + 2, 1.75));  // decay
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
        double cumulativeProbability = 0d;

        for (int i = 0; i < probabilities.length; i++) {
            cumulativeProbability += probabilities[i];
            if (random.nextDouble() <= cumulativeProbability) {
                return i+1;
            }
        }

        return -1;
    }

    public int generateRandomNumber(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private void logParameters()
    {
        logger.info("SIMULATION PARAMETERS:");
        logger.info("MATCHING_ENGINE: {}", MATCHING_ENGINE);
        logger.info("EVENT_PROBABILITIES_STYLE: {}", EVENT_PROBABILITIES_STYLE);
        logger.info("BOOK_EVENT_DEPTH: {}", BOOK_EVENT_DEPTH);
        logger.info("INIT_ITERATIONS: {}", INIT_ITERATIONS);
        logger.info("ITERATIONS: {}", ITERATIONS);
        logger.info("BID_INIT: {}", BID_INIT);
        logger.info("ASK_INIT: {}", ASK_INIT);
        logger.info("TICK_SIZE: {}", TICK_SIZE);
        logger.info("PRORATA_FAR_TOUCH_MIN_MULTIPLIER: {}", PRORATA_FAR_TOUCH_MIN_MULTIPLIER);
        logger.info("PRORATA_FAR_TOUCH_MAX_MULTIPLIER: {}", PRORATA_FAR_TOUCH_MAX_MULTIPLIER);
        logger.info("\n");
    }
}
