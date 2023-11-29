package MatchingEngine;

import Orderbook.Orderbook;
import Orders.Limit;
import Orders.Order;
import Orders.Trade;

import java.util.HashSet;
import java.util.TreeSet;

class ProRataWithTopMatcher extends AbstractOrderMatcher{
    @Override
    public void matchMarketOrder(Order o, Orderbook ob) {
        int totalSize = o.isBuy() ? ob.getTotalAskSize() : ob.getTotalBidSize();
        if (o.getCurrentQuantity() >= totalSize)
        {
            // if e.g. total ask size is 10000, but an aggressive buy order comes in for 20000, we aren't going to match it.
            return;
        }

        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();
        int initialQty = o.getCurrentQuantity();
        int filledQty = 0;

        for (Limit limit: limitTree)
        {
            Order ptr = limit.getHead();
            Order headOrder = limit.getHead();
            int remainingQty = o.getCurrentQuantity();
            int initialTotalVolumeAtLimit = limit.getTotalVolumeAtLimit();
            int modifiedTotalVolumeAtLimit = initialTotalVolumeAtLimit;

            while(ptr!=null && o.getCurrentQuantity() > 0)
            {
                if (ptr == headOrder) // top
                {
                    int topOrderRemainingQty = Math.max(ptr.getCurrentQuantity() - o.getCurrentQuantity(), 0);
                    if (topOrderRemainingQty == 0) {
                        o.setCurrentQuantity(o.getCurrentQuantity() - ptr.getCurrentQuantity());
                        trades.add(new Trade(o.getSide(), limit.getPrice(), ptr.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId()));
                        ob.removeOrder(ptr.getOrderId(), true);
                    } else {
                        // top order fully fills the incoming order, update book, exit while loop (matching finished)
                        trades.add(new Trade(o.getSide(), limit.getPrice(), o.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId()));
                        limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - o.getCurrentQuantity());
                        if (o.isBuy())
                            ob.setTotalAskSize(ob.getTotalAskSize() - o.getCurrentQuantity());
                        else
                            ob.setTotalBidSize(ob.getTotalBidSize() - o.getCurrentQuantity());

                        o.setCurrentQuantity(0);
                        ptr.setCurrentQuantity(topOrderRemainingQty);
                    }

                    modifiedTotalVolumeAtLimit = limit.getTotalVolumeAtLimit();
                }
                else
                {
                    // pro rata
                    double ratio = (double) ptr.getCurrentQuantity() / modifiedTotalVolumeAtLimit;
//                    System.out.println(ptr.getCurrentQuantity() + " " + modifiedTotalVolumeAtLimit);
                    int qtyToFill = Math.min(ptr.getCurrentQuantity(), (int) (ratio * remainingQty));
                    if (qtyToFill > 0)
                    {
                        int currentLimitOrderRemainingQty = Math.max(0, ptr.getCurrentQuantity() - qtyToFill);
                        if (currentLimitOrderRemainingQty > 0) {
                            limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - qtyToFill);
                            if (o.isBuy())
                                ob.setTotalAskSize(ob.getTotalAskSize() - qtyToFill);
                            else
                                ob.setTotalBidSize(ob.getTotalBidSize() - qtyToFill);
                            ptr.setCurrentQuantity(currentLimitOrderRemainingQty);
                        }
                        else
                        {
                            ob.removeOrder(ptr.getOrderId(), true);
                        }

                        filledQty += qtyToFill;
                        o.setCurrentQuantity(Math.max(0, initialQty - filledQty));
                        getTrades().add(new Trade(o.getSide(), limit.getPrice(), qtyToFill, ptr.getOrderId(), o.getOrderId()));
                    }
                }
                ptr = ptr.getNextOrder();
            }

            if (o.getCurrentQuantity() > 0 && filledQty != limit.getTotalVolumeAtLimit())
            {
                // the limit is not depleted but there is still remainder qty from the pro rata roundoff. We apply FIFO
                // (time priority) matching for the 'roundoff' quantity. If the limit was fully depleted without any residue, filledQty == totalVolumeAtLimit.
                ptr = limit.getHead();
                while (ptr != null && o.getCurrentQuantity() > 0)
                {
                    int canAllocate = ptr.getCurrentQuantity();
                    int allocated = Math.min(o.getCurrentQuantity(), canAllocate);
                    if (allocated > 0)
                    {
                        limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - allocated);
                        if (o.isBuy())
                            ob.setTotalAskSize(ob.getTotalAskSize() - allocated);
                        else
                            ob.setTotalBidSize(ob.getTotalBidSize() - allocated);
                        ptr.setCurrentQuantity(ptr.getCurrentQuantity() - allocated);
                    }
                    else
                    {
                        ob.removeOrder(ptr.getOrderId(), true);
                    }

                    getTrades().add(new Trade(o.getSide(), limit.getPrice(), allocated, ptr.getOrderId(), o.getOrderId()));
                    int howMuchLeftToFill = o.getCurrentQuantity() - allocated;
                    o.setCurrentQuantity(howMuchLeftToFill);
                    ptr = ptr.getNextOrder();
                }
            }

            if (o.getCurrentQuantity() == 0) // order is fully filled
                break;

            // If limit is depleted and there is still unfilled qty, we move on to the next best limit.
        }

        if (o.getCurrentQuantity() > 0)
        {
            // ideally there should be no order quantity remaining after going through ALL the limit levels
            // the initial total size condition should prevent us from reaching this block.
            // raise exception if we somehow reach here.
            throw new UnexpectedRemainingVolumeException("There shouldn't be any remaining quantity here.");
        }

    }

    @Override
    public void matchAggressiveLimitOrder(Order o, Orderbook ob) {
//        int totalSize = o.isBuy() ? ob.getTotalAskSize() : ob.getTotalBidSize();
//        if (o.getCurrentQuantity() >= totalSize)
//        {
//            // if e.g. total ask size is 10000, but an aggressive buy order comes in for 20000, we aren't going to match it.
//            return;
//        }
//
//        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();
//        HashSet<Integer> ordersToRemove = new HashSet<>();
//        double farTouchPrice = o.isBuy() ? ob.getBestAsk() : ob.getBestBid();
//        int initialQty = o.getCurrentQuantity();
//        int filledQty = 0;
//
//        for (Limit limit: limitTree)
//        {
//            Order ptr = limit.getHead();
//            int totalVolumeAtLimit = limit.getTotalVolumeAtLimit();
//            int remainingQty = o.getCurrentQuantity();
//
//            if (limit.getPrice() == farTouchPrice)
//            {
//                while(ptr!=null && o.getCurrentQuantity() > 0)
//                {
//                    double ratio = (double) ptr.getCurrentQuantity() / totalVolumeAtLimit;
//                    int qtyToFill = Math.min(ptr.getCurrentQuantity(), (int) (ratio * remainingQty));
//                    if (qtyToFill > 0)
//                    {
//                        int currentLimitOrderRemainingQty = Math.max(0, ptr.getCurrentQuantity() - qtyToFill);
//                        if (currentLimitOrderRemainingQty > 0)
//                        {
//                            limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - qtyToFill);
//                            if (o.isBuy())
//                                ob.setTotalAskSize(ob.getTotalAskSize() - qtyToFill);
//                            else
//                                ob.setTotalBidSize(ob.getTotalBidSize() - qtyToFill);
//                            ptr.setCurrentQuantity(currentLimitOrderRemainingQty);
//                        }
//                        else
//                        {
//                            ordersToRemove.add(ptr.getOrderId());
//                        }
//
//                        filledQty += qtyToFill;
//                        o.setCurrentQuantity(Math.max(0, initialQty - filledQty));
//                        getTrades().add(new Trade(o.getSide(), limit.getPrice(), qtyToFill, ptr.getOrderId(), o.getOrderId()));
//                    }
//
//                    ptr = ptr.getNextOrder();
//                }
//
//                if (o.getCurrentQuantity() > 0 && filledQty != totalVolumeAtLimit)
//                {
//                    // the limit is not depleted but there is still remainder qty from the pro rata roundoff. We apply FIFO
//                    // (time priority) matching for the 'roundoff' quantity. If the limit was fully depleted without any residue, filledQty == totalVolumeAtLimit.
//                    ptr = limit.getHead();
//                    while (ptr != null && o.getCurrentQuantity() > 0)
//                    {
//                        int canAllocate = ptr.getCurrentQuantity();
//                        int allocated = Math.min(o.getCurrentQuantity(), canAllocate);
//                        if (allocated > 0)
//                        {
//                            limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - allocated);
//                            if (o.isBuy())
//                                ob.setTotalAskSize(ob.getTotalAskSize() - allocated);
//                            else
//                                ob.setTotalBidSize(ob.getTotalBidSize() - allocated);
//                            ptr.setCurrentQuantity(ptr.getCurrentQuantity() - allocated);
//                        }
//                        else
//                        {
//                            ordersToRemove.add(ptr.getOrderId());
//                        }
//
//                        getTrades().add(new Trade(o.getSide(), limit.getPrice(), allocated, ptr.getOrderId(), o.getOrderId()));
//                        int howMuchLeftToFill = o.getCurrentQuantity() - allocated;
//                        o.setCurrentQuantity(howMuchLeftToFill);
//                        ptr = ptr.getNextOrder();
//                    }
//                }
//            }
//        }
//
//        for (Integer orderId: ordersToRemove)
//        {
//            ob.removeOrder(orderId, true);
//        }
//
//        if (o.getCurrentQuantity() > 0)
//        {
//            ob.addOrder(new Order(o.getSecurityId(), o.getSide(), o.getCurrentQuantity(), farTouchPrice));
//        }
    }
}

