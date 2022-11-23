package Orderbook;

import Orders.AskLimit;
import Orders.BidLimit;
import Orders.Limit;
import Orders.Order;

import java.util.HashMap;
import java.util.TreeSet;

public class Orderbook {
    private TreeSet<AskLimit> askLimits = new TreeSet<>();
    private TreeSet<BidLimit> bidLimits = new TreeSet<>();
    private HashMap<Integer, Order> orderMap = new HashMap<>();

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
            limitTree.add(limit);
            limit.setHead(incomingOrder);
            limit.setTail(incomingOrder);
        }

        map.put(incomingOrder.getOrderId(), incomingOrder);
    }
}
