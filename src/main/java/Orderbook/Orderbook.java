package Orderbook;

import MatchingEngine.AbstractOrderMatcher;
import Orders.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.TreeSet;

public class Orderbook {
    private static final Logger logger = LogManager.getLogger(Orderbook.class);

    @Getter private TreeSet<Limit> askLimits = new TreeSet<>(new AskLimitComparator());
    @Getter private TreeSet<Limit> bidLimits = new TreeSet<>(new BidLimitComparator());
    @Getter private HashMap<Integer, Order> orderMap = new HashMap<>();
    @Getter private AbstractOrderMatcher matchingEngine;
    private int nextAvailableOrderId;
    @Getter private double bestBid = Integer.MIN_VALUE;
    @Getter private double bestAsk = Integer.MAX_VALUE;
    @Getter @Setter private int totalAskSize = 0;
    @Getter @Setter private int totalBidSize = 0;

    public Orderbook(AbstractOrderMatcher matchingEngine)
    {
        this.matchingEngine = matchingEngine;
    }

    public void addOrder(Order incomingOrder)
    {
        //set order id and timestamp
        incomingOrder.setOrderId(nextAvailableOrderId);
        incomingOrder.setTimestamp(System.nanoTime());

        if (orderMap.containsKey(incomingOrder.getOrderId()))
            throw new RuntimeException("orderMap already contains this orderID.");

        addOrder(incomingOrder, incomingOrder.getParentLimit(), incomingOrder.isBuy() ? bidLimits : askLimits);
    }

    private void addOrder(Order incomingOrder, Limit limit, TreeSet<Limit> limitTree)
    {
        logger.info("adding new order: {}", incomingOrder);
        // market orders and aggressive limit orders do NOT need to be put in the orderMap.

        // market order
        if (incomingOrder.getOrdType() == ORDER_TYPE.MARKET)
        {
            logger.info("market order detected. Going to match");
            matchingEngine.matchMarketOrder(incomingOrder, this);
            return;
        }

        // aggressive limit order
        if ((incomingOrder.isBuy() && incomingOrder.getPrice() >= bestAsk) ||
                (!incomingOrder.isBuy() && incomingOrder.getPrice() <= bestBid))
        {
            logger.info("aggressive limit order detected. Going to match");
            matchingEngine.matchAggressiveLimitOrder(incomingOrder, this);
            return;
        }

        logger.info("passive order detected. Adding to book.");

        // passive order
        Limit existingLimit = treeSetTryGetValue(limitTree, limit); // log(n) search (balanced BST). This is basically the .contains method but we also extract the element.
        if (existingLimit != null)
        {
            incomingOrder.setParentLimit(existingLimit); // orders at the same limit level must reference the same limit level object
            if (existingLimit.getHead() == null) // limit level exists but no orders in it
            {
                existingLimit.setHead(incomingOrder);
                existingLimit.setTail(incomingOrder);
            }
            else
            {
                // doubly linked list insertion
                incomingOrder.setPrevOrder(existingLimit.getTail()); // current head of limit is the most recent order in the limit
                existingLimit.getTail().setNextOrder(incomingOrder); // set the next pointer of current limit head to the incoming order
                existingLimit.setTail(incomingOrder);
                incomingOrder.setNextOrder(null);
            }
            existingLimit.setTotalVolumeAtLimit(existingLimit.getTotalVolumeAtLimit() + incomingOrder.getInitialQuantity());
        }
        else
        {
            limitTree.add(limit);
            limit.setHead(incomingOrder);
            limit.setTail(incomingOrder);
            limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() + incomingOrder.getInitialQuantity());
        }

        limit.getOrderIds().add(incomingOrder.getOrderId());
        orderMap.put(incomingOrder.getOrderId(), incomingOrder);
        nextAvailableOrderId = incomingOrder.getOrderId() + 1; // TODO: not sure about this
        updateBookStateAfterAdd(incomingOrder);

        orderBookStateLog();
    }

    public void removeOrder(int removeOrderId, boolean isDuringMatching)
    {
        // check if removeOrder id is in the book
        if (containsOrder(removeOrderId))
        {
            logger.info("removing orderID {}", removeOrderId);
            Order orderToRemove = orderMap.get(removeOrderId);
            // alter head/tail pointers of order Limit
            if (orderToRemove.getParentLimit().getHead() == orderToRemove && orderToRemove.getParentLimit().getTail() == orderToRemove)
            {
                orderToRemove.getParentLimit().setHead(null);
                orderToRemove.getParentLimit().setTail(null);
            }
            else if (orderToRemove.getParentLimit().getHead() == orderToRemove)
            {
                orderToRemove.getParentLimit().setHead(orderToRemove.getNextOrder());
            }
            else if (orderToRemove.getParentLimit().getTail() == orderToRemove)
            {
                orderToRemove.getParentLimit().setTail(orderToRemove.getPrevOrder());
            }

            // doubly linked list deletion for orders obj
            if (orderToRemove.getNextOrder() != null && orderToRemove.getPrevOrder() != null)
            {
                orderToRemove.getNextOrder().setPrevOrder(orderToRemove.getPrevOrder());
                orderToRemove.getPrevOrder().setNextOrder(orderToRemove.getNextOrder());
            }
            else if (orderToRemove.getNextOrder() != null)
            {
                orderToRemove.getNextOrder().setPrevOrder(orderToRemove.getPrevOrder());
            }
            else if (orderToRemove.getPrevOrder() != null)
            {
                orderToRemove.getPrevOrder().setNextOrder(orderToRemove.getNextOrder());
            }

            // update the best bid/ask if we remove the only order from the top-of-book limit. If this is called DURING
            // matching, then we cannot remove the empty limit from the limitTree because we are going to be traversing the limitTree,
            // and in the process we cannot modify it. However, if an order is removed during non-matching, e.g. we just simulate an order cancellation,
            // then we are able to directly remove empty limits as we aren't doing any traversing.
            if (isDuringMatching)
            {
                updateBestBidAskIfLimitDepleted(orderToRemove.getParentLimit(), orderToRemove.isBuy());
            }
            else
            {
                updateBestBidAskIfLimitDepletedAndRemoveEmptyLimit(orderToRemove.getParentLimit(), orderToRemove.isBuy());
            }

            orderToRemove.getParentLimit().getOrderIds().remove(removeOrderId);
            orderMap.remove(removeOrderId);
            updateBookStateAfterRemove(orderToRemove);

            orderBookStateLog();
        }
    }

    public void modifyOrderPrice(int orderId, double price)
    {
        if (containsOrder(orderId))
        {
            logger.info("modifying price of orderId {}", orderId);
            // modification = deletion + insertion. upon deletion of a particular orderId, does the subsequent
            // insertion use the same deleted orderId? or does it use the next id available? probably latter
            Order newOrder = new Order(orderMap.get(orderId), price);
            removeOrder(orderId, true);

            // in the event of a price mod, the Limit pointer of the modOrder should reflect the new Limit
            addOrder(newOrder);
        }
    }

    public void modifyOrderQty(int orderId, int qty)
    {
        if (containsOrder(orderId))
        {
            logger.info("modifying order qty of orderId {}", orderId);
            // modification = deletion + insertion. upon deletion of a particular orderId, does the subsequent
            // insertion use the same deleted orderId? or does it use the next id available? probably latter
            Order newOrder = new Order(orderMap.get(orderId), qty);
            removeOrder(orderId, true);

            // in the event of a price mod, the Limit pointer of the modOrder should reflect the new Limit
            addOrder(newOrder);
        }
    }

    public boolean containsOrder(int orderId)
    {
        return orderMap.containsKey(orderId);
    }


    private void updateBestBid(double price)
    {
        if (price > bestBid)
        {
            bestBid = price;
        }
    }

    private void updateBestAsk(double price)
    {
        if (price < bestAsk)
        {
            bestAsk = price;
        }
    }

    private void updateBestBidAskIfLimitDepleted(Limit l, boolean limitOrderIsBuy)
    {
        // TODO: this function is a little convoluted, rewrite it later
        // if a market buy order has just depleted the entirety of a limit sell level P, P will be empty and we need to
        // update the best ask to reflect the depletion. We will therefore enter the below if block.
        // a market buy order will be interacting with the resting limit orders on the ask book. The resting limit order
        // we have just removed to completely deplete P is therefore a sell order (limitOrderIsBuy==false), and so we have
        // to update the bestAsk.
        // A market order could deplete several limit levels, hence when iterating through askLimits, we have a !limit.isEmpty()
        // check to ensure that the bestAsk is not updated to an empty limit. Unfortunately we are unable to remove empty limits
        // DURING the order matching as we will run into a ConcurrentModificationException error, which is another reason why we
        // need the !limit.isEmpty() check.
        if (l.isEmpty())
        {
            if (limitOrderIsBuy)
            {
                if (l.getPrice() == bestBid)
                {
                    for (Limit limit: bidLimits)
                    {
                        if (!limit.isEmpty())
                        {
                            bestBid = limit.getPrice();
                            return;
                        }
                    }
                }
            }
            else
            {
                if (l.getPrice() == bestAsk)
                {
                    for (Limit limit: askLimits)
                    {
                        if (!limit.isEmpty())
                        {
                            bestAsk = limit.getPrice();
                            return;
                        }
                    }
                }
            }
        }
    }

    private void updateBestBidAskIfLimitDepletedAndRemoveEmptyLimit(Limit l, boolean limitOrderIsBuy)
    {
        if (l.isEmpty())
        {
            if (limitOrderIsBuy)
            {
                bidLimits.remove(l);
                if (l.getPrice() == bestBid)
                {
                    for (Limit limit: bidLimits)
                    {
                        bestBid = limit.getPrice();
                        return;
                    }
                }
            }
            else
            {
                askLimits.remove(l);
                if (l.getPrice() == bestAsk)
                {
                    for (Limit limit: askLimits)
                    {
                        bestAsk = limit.getPrice();
                        return;
                    }
                }
            }
        }
    }

    private void updateBookStateAfterAdd(Order o)
    {
        if (o.isBuy())
        {
            totalBidSize += o.getInitialQuantity();
            updateBestBid(o.getPrice());
        }
        else
        {
            totalAskSize += o.getInitialQuantity();
            updateBestAsk(o.getPrice());
        }
    }

    private void updateBookStateAfterRemove(Order o)
    {
        if (o.isBuy())
        {
            totalBidSize -= o.getCurrentQuantity();
        }
        else
        {
            totalAskSize -= o.getCurrentQuantity();
        }

        // if the limit still exists, we update the volume at limit by sutracting the qty of the removed order
        if (!o.getParentLimit().isEmpty())
        {
            int newTotalLimitVolume = o.getParentLimit().getTotalVolumeAtLimit() - o.getCurrentQuantity();
            o.getParentLimit().setTotalVolumeAtLimit(newTotalLimitVolume);
        }
    }

    public void clearEmptyLimitsAfterMatching(boolean isBuy)
    {
         if (isBuy) // market buys (sells)/aggressive limit buys (sells) will only ever interact with the ask (bid) book
         {
             askLimits.removeIf(Limit::isEmpty); // for each askLimit, remove if the limit is empty
         }
         else
         {
             bidLimits.removeIf(Limit::isEmpty);
         }
    }

    private static <T> T treeSetTryGetValue(TreeSet<T> set, T key) {
        T floor = set.floor(key);
        if (floor != null && floor.equals(key)) {
            return floor;
        }
        return null;
    }

    public void printOrderbook()
    {
        logger.info("BID LIMITS");
        for (Limit limit: bidLimits)
        {
            logger.info(limit + ", " + "totalVolumeAtLimit: " + limit.getTotalVolumeAtLimit());
            Order ptr = limit.getHead();
            while (ptr != null)
            {
                logger.info(ptr);
                ptr = ptr.getNextOrder();
            }
        }

        logger.info("ASK LIMITS");
        for (Limit limit: askLimits)
        {
            logger.info(limit + ", " + "totalVolumeAtLimit: " + limit.getTotalVolumeAtLimit());
            Order ptr = limit.getHead();
            while (ptr != null)
            {
                logger.info(ptr);
                ptr = ptr.getNextOrder();
            }
        }

        logger.info("\n####\n");
    }

    private void orderBookStateLog()
    {
        Limit bestBidLimit = treeSetTryGetValue(bidLimits, new Limit(bestBid));
        Limit bestAskLimit = treeSetTryGetValue(askLimits, new Limit(bestAsk));
        if (bestBidLimit != null && bestAskLimit != null)
            logger.info("BBO: {} x {}/{} x {}", bestBidLimit.getTotalVolumeAtLimit(), bestBid, bestAsk, bestAskLimit.getTotalVolumeAtLimit());
        logger.info("totalBidSize: {}, totalAskSize: {}", totalBidSize, totalAskSize);
    }

    public boolean compareTotalBidAskVolumes()
    {
        int askSideTotalVolume = 0;
        int bidSideTotalVolume = 0;

        for (Limit limit: askLimits)
        {
            Order ord = limit.getHead();
            int currentLimitTotalQty = 0;
            while (ord != null)
            {
                currentLimitTotalQty += ord.getCurrentQuantity();
                ord = ord.getNextOrder();
            }

            if (currentLimitTotalQty != limit.getTotalVolumeAtLimit())
                return false;

            askSideTotalVolume += limit.getTotalVolumeAtLimit();
        }

        for (Limit limit: bidLimits)
        {
            Order ord = limit.getHead();
            int currentLimitTotalQty = 0;
            while (ord != null)
            {
                currentLimitTotalQty += ord.getCurrentQuantity();
                ord = ord.getNextOrder();
            }

            if (currentLimitTotalQty != limit.getTotalVolumeAtLimit())
                return false;

            bidSideTotalVolume += limit.getTotalVolumeAtLimit();
        }

        return askSideTotalVolume == totalAskSize && bidSideTotalVolume == totalBidSize;
    }
}
