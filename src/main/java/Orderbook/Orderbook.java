package Orderbook;

import MatchingEngine.IOrderMatcher;
import Orders.*;
import lombok.Getter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public class Orderbook {
    @Getter private TreeSet<Limit> askLimits = new TreeSet<>(new AskLimitComparator());
    @Getter private TreeSet<Limit> bidLimits = new TreeSet<>(new BidLimitComparator());
    @Getter private HashMap<Integer, Order> orderMap = new HashMap<>();
    @Getter private IOrderMatcher matchingEngine;
    private int nextAvailableOrderId;
    @Getter private double bestBid = Integer.MIN_VALUE;
    @Getter private double bestAsk = Integer.MAX_VALUE;
    @Getter private int totalAskSize = 0;
    @Getter private int totalBidSize = 0;

    public Orderbook(IOrderMatcher matchingEngine)
    {
        this.matchingEngine = matchingEngine;
    }

    public void addOrder(Order incomingOrder)
    {
        //set order id and timestamp
        incomingOrder.setOrderId(nextAvailableOrderId);
        incomingOrder.setTimestamp(System.nanoTime());

        if (orderMap.containsKey(incomingOrder.getOrderId()))
            throw new RuntimeException("orderMap already contains this orderId.");

        addOrder(incomingOrder, incomingOrder.getParentLimit(), incomingOrder.isBuy() ? bidLimits : askLimits);
    }

    private void addOrder(Order incomingOrder, Limit limit, TreeSet<Limit> limitTree)
    {
        // market order
        if (incomingOrder.getOrdType() == ORDER_TYPE.MARKET)
        {
            matchingEngine.matchMarketOrder(incomingOrder, this);
            return;
        }

        // aggressive limit order
        if ((incomingOrder.isBuy() && incomingOrder.getPrice() >= bestAsk) ||
                (!incomingOrder.isBuy() && incomingOrder.getPrice() <= bestBid))
        {
            matchingEngine.matchAggressiveLimitOrder(incomingOrder, this);
            return;
        }

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
            existingLimit.setTotalVolumeAtLimit(existingLimit.getTotalVolumeAtLimit() + incomingOrder.getQuantity());
        }
        else
        {
            limitTree.add(limit);
            limit.setHead(incomingOrder);
            limit.setTail(incomingOrder);
            limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() + incomingOrder.getQuantity());
        }

        orderMap.put(incomingOrder.getOrderId(), incomingOrder);
        nextAvailableOrderId = incomingOrder.getOrderId() + 1; // TODO: not sure about this
        updateBookStateAfterAdd(incomingOrder);
    }

    public void removeOrder(int removeOrderId)
    {
        // check if removeOrder id is in the book
        if (containsOrder(removeOrderId))
        {
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

            // cleanup limit from tree if we just removed the only order in the limit level
            removeLimitFromTreeIfEmpty(orderToRemove.getParentLimit(), orderToRemove.isBuy());
            orderMap.remove(removeOrderId);
            updateBookStateAfterRemove(orderToRemove);
        }
    }

    public void modifyOrderPrice(int orderId, double price)
    {
        if (containsOrder(orderId))
        {
            // modification = deletion + insertion. upon deletion of a particular orderId, does the subsequent
            // insertion use the same deleted orderId? or does it use the next id available? probably latter
            Order newOrder = new Order(orderMap.get(orderId), price);
            removeOrder(orderId);

            // in the event of a price mod, the Limit pointer of the modOrder should reflect the new Limit
            addOrder(newOrder);
        }
    }

    public void modifyOrderQty(int orderId, int qty)
    {
        if (containsOrder(orderId))
        {
            // modification = deletion + insertion. upon deletion of a particular orderId, does the subsequent
            // insertion use the same deleted orderId? or does it use the next id available? probably latter
            Order newOrder = new Order(orderMap.get(orderId), qty);
            removeOrder(orderId);

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

    private void removeLimitFromTreeIfEmpty(Limit l, boolean isBuy)
    {
        if (l.isEmpty())
        {
            if (isBuy)
            {
                bidLimits.remove(l);
                if (l.getPrice() == bestBid)
                {
                    Iterator<Limit> it = bidLimits.iterator();
                    if (it.hasNext())
                    {
                        bestBid = it.next().getPrice();
                    }
                    else
                    {
                        // TODO: some warning
                    }
                }
            }
            else
            {
                askLimits.remove(l);
                if (l.getPrice() == bestAsk)
                {
                    Iterator<Limit> it = askLimits.iterator();
                    if (it.hasNext())
                    {
                        bestAsk = it.next().getPrice();
                    }
                    else
                    {
                        // TODO: some warning
                    }
                }
            }
        }
    }

    private void updateBookStateAfterAdd(Order o)
    {
        if (o.isBuy())
        {
            totalBidSize += o.getQuantity();
            updateBestBid(o.getPrice());
        }
        else
        {
            totalAskSize += o.getQuantity();
            updateBestAsk(o.getPrice());
        }
    }

    private void updateBookStateAfterRemove(Order o)
    {
        if (o.isBuy())
        {
            totalBidSize -= o.getQuantity();
        }
        else
        {
            totalAskSize -= o.getQuantity();
        }

        // if the limit still exists, we update the volume at limit by sutracting the qty of the removed order
        if (!o.getParentLimit().isEmpty())
        {
            int newTotalLimitVolume = o.getParentLimit().getTotalVolumeAtLimit() - o.getQuantity();
            o.getParentLimit().setTotalVolumeAtLimit(newTotalLimitVolume);
        }
    }

    private static <T> T treeSetTryGetValue(TreeSet<T> set, T key) {
        T floor = set.floor(key);
        if (floor != null && floor.equals(key)) {
            return floor;
        }
        return null;
    }
}
