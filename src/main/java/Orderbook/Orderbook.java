package Orderbook;

import Orders.*;

import java.util.HashMap;
import java.util.TreeSet;

public class Orderbook {
    private TreeSet<AskLimit> askLimits = new TreeSet<>();
    private TreeSet<BidLimit> bidLimits = new TreeSet<>();
    private HashMap<Integer, Order> orderMap = new HashMap<>();
    private int nextAvailableOrderId;

    public <T extends Limit> void addOrder(Order incomingOrder, Limit limit, TreeSet<T> limitTree, HashMap<Integer, Order> map)
    {
        // TODO: something not quite right with limit class and generic definition
        if (limitTree.contains(limit))
        {
            if (limit.getHead() == null) // limit level exists but no orders in it
            {
                limit.setHead(incomingOrder);
                limit.setTail(incomingOrder);
            }
            else
            {
                // doubly linked list insertion
                incomingOrder.setPrevOrder(limit.getTail()); // current head of limit is the most recent order in the limit
                limit.getTail().setNextOrder(incomingOrder); // set the next pointer of current limit head to the incoming order
                limit.setTail(incomingOrder);
                incomingOrder.setNextOrder(null);
            }
        }
        else
        {
            limitTree.add(limit); // TODO: bug
            limit.setHead(incomingOrder);
            limit.setTail(incomingOrder);
        }

        map.put(incomingOrder.getOrderId(), incomingOrder);
        nextAvailableOrderId = incomingOrder.getOrderId() + 1; // TODO: not sure about this
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
            orderMap.remove(removeOrderId);
        }
    }

    public void modifyOrder(Order modOrder)
    {
        if (containsOrder(modOrder.getOrderId()))
        {
            // modification = deletion + insertion. upon deletion of a particular orderId, does the subsequent
            // insertion use the same deleted orderId? or does it use the next id available? probably latter
            removeOrder(modOrder.getOrderId());
            Order newOrder = new Order(modOrder, nextAvailableOrderId);

            // in the event of a price mod, the Limit pointer of the modOrder should reflect the new Limit
            if (newOrder.getParentLimit().getLimitSide() == Side.BUY)
                addOrder(newOrder, newOrder.getParentLimit(), bidLimits, orderMap);
            else
                addOrder(newOrder, newOrder.getParentLimit(), askLimits, orderMap);
        }
    }

    public boolean containsOrder(int orderId)
    {
        return orderMap.containsKey(orderId);
    }

    public double getBestBid()
    {
        return bidLimits.last().getLimitPrice();
    }

    public double getBestAsk()
    {
        return askLimits.first().getLimitPrice();
    }
}
