package MatchingEngine;

import Orderbook.Orderbook;
import Orders.Limit;
import Orders.Order;
import Orders.Trade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.TreeSet;

class ProRataWithTopMatcher extends AbstractOrderMatcher{
    private static final Logger logger = LogManager.getLogger(ProRataWithTopMatcher.class);
    @Override
    public void matchMarketOrder(Order o, Orderbook ob) {
        int totalSize = o.isBuy() ? ob.getTotalAskSize() : ob.getTotalBidSize();
        if (o.getCurrentQuantity() >= totalSize)
        {
            // if e.g. total ask size is 10000, but an aggressive buy order comes in for 20000, we aren't going to match it.
            logger.error("order qty ({}) is greater than the total aggressive size ({}). Not matching.", o.getInitialQuantity(), totalSize);
            return;
        }

        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();
        int initialQty = o.getCurrentQuantity();
        int filledQty = 0;

        logger.info("order qty: {}, filled qty: {}/{}", initialQty, filledQty, initialQty);

        for (Limit limit: limitTree)
        {
            logger.info("iterating through resting orders at limit={}...", limit.getPrice());
            Order ptr = limit.getHead();
            Order headOrder = limit.getHead();
            int remainingQty = o.getCurrentQuantity();
            int modifiedTotalVolumeAtLimit = limit.getTotalVolumeAtLimit();

            while(ptr!=null && o.getCurrentQuantity() > 0)
            {
                if (ptr == headOrder) // top
                {
                    logger.info("attempting to match entire top order. orderID: {}, price: {}, qty: {}", ptr.getOrderId(), ptr.getPrice(), ptr.getCurrentQuantity());
                    int topOrderRemainingQty = Math.max(ptr.getCurrentQuantity() - o.getCurrentQuantity(), 0);
                    if (topOrderRemainingQty == 0) {
                        Trade trade = new Trade(o.getSide(), limit.getPrice(), ptr.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId());
                        trades.add(trade);
                        logger.info("new trade: {}", trade);

                        o.setCurrentQuantity(o.getCurrentQuantity() - ptr.getCurrentQuantity());
                        filledQty += ptr.getCurrentQuantity();
                        ob.removeOrder(ptr.getOrderId(), true);
                    } else {
                        // top order fully fills the incoming order, update book, exit while loop (matching finished)
                        Trade trade = new Trade(o.getSide(), limit.getPrice(), o.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId());
                        trades.add(trade);
                        logger.info("new trade: {}", trade);
                        logger.info("top order fully fills incoming order. Matching complete.");

                        limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - o.getCurrentQuantity());
                        if (o.isBuy())
                            ob.setTotalAskSize(ob.getTotalAskSize() - o.getCurrentQuantity());
                        else
                            ob.setTotalBidSize(ob.getTotalBidSize() - o.getCurrentQuantity());

                        filledQty += o.getCurrentQuantity();
                        o.setCurrentQuantity(0);
                        ptr.setCurrentQuantity(topOrderRemainingQty);
                    }

                    modifiedTotalVolumeAtLimit = limit.getTotalVolumeAtLimit();
                    remainingQty = o.getCurrentQuantity();
                    logger.info("after matching top order - filledQty: {}/{}, totalVolumeAtLimit: {}, unfilledQty: {}", filledQty, initialQty, modifiedTotalVolumeAtLimit, o.getCurrentQuantity());
                }
                else
                {
                    // pro rata
                    logger.info("Top order was fully filled. Continuing with pro rata matching for remaining qty...");
                    double ratio = (double) ptr.getCurrentQuantity() / modifiedTotalVolumeAtLimit;
                    int qtyToFill = Math.min(ptr.getCurrentQuantity(), (int) (ratio * remainingQty));
                    logger.info("Matching with orderID {}, qty: {}, ratio: ({}/{}) = {}, toDistribute: {}, qtyToFill: {}, canDistribute: {}",
                            ptr.getOrderId(), ptr.getCurrentQuantity(), ptr.getCurrentQuantity(), modifiedTotalVolumeAtLimit, ratio, remainingQty, qtyToFill, qtyToFill > 0);

                    Trade trade = new Trade(o.getSide(), limit.getPrice(), qtyToFill, ptr.getOrderId(), o.getOrderId());
                    trades.add(trade);
                    logger.info("new trade: {}", trade);

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
                        logger.info("filledQty: {}/{}", filledQty, initialQty);
                    }
                }
                ptr = ptr.getNextOrder();
            }

            logger.info("at the current limit price level = {} - the available liquidity was: {}, we filled: {}, unfilledQty: {}",
                    limit.getPrice(), modifiedTotalVolumeAtLimit, filledQty, o.getCurrentQuantity());

            if (o.getCurrentQuantity() > 0 && filledQty != modifiedTotalVolumeAtLimit)
            {
                // the limit is not depleted but there is still remainder qty from the pro rata roundoff. We apply FIFO
                // (time priority) matching for the 'roundoff' quantity.
                // If the limit was fully depleted without any residue, filledQty == totalVolumeAtLimit, and we don't have to do this FIFO residue distribution.

                logger.info("there is a rounding residue of {} as a result of the pro rata distribution. Match residue qty with FIFO, iterating through the existing limit level again...",
                        modifiedTotalVolumeAtLimit - filledQty);

                ptr = limit.getHead();
                while (ptr != null && o.getCurrentQuantity() > 0)
                {
                    // either the market order remaining qty is fully filled by the current limit order, or the market order
                    // clears the entire current limit order and we have to move to the next limit order with ptr.
                    int canAllocate = ptr.getCurrentQuantity();
                    int allocated = Math.min(o.getCurrentQuantity(), canAllocate);
                    logger.info("Matching residue qty of {} with orderID {} (price: {}, qty: {}/{}), unfilled qty: {}. We can allocate a maximum of {} lots.",
                            modifiedTotalVolumeAtLimit - filledQty, ptr.getOrderId(), ptr.getPrice(), canAllocate, ptr.getInitialQuantity(), o.getCurrentQuantity(), allocated);

                    if (allocated > 0)
                    {
                        Trade trade = new Trade(o.getSide(), limit.getPrice(), allocated, ptr.getOrderId(), o.getOrderId());
                        trades.add(trade);
                        logger.info("new trade: {}", trade);

                        // if howMuchLeftToFill reaches 0, the pro rata residue volume has been fully distributed, we can exit the while loop.
                        int howMuchLeftToFill = o.getCurrentQuantity() - allocated;
                        o.setCurrentQuantity(howMuchLeftToFill);

                        // current limit order is fully filled, we can remove it from the book
                        int bookOrderRemainder = ptr.getCurrentQuantity() - allocated;
                        if (bookOrderRemainder == 0)
                        {
                            ob.removeOrder(ptr.getOrderId(), true);
                        }
                        else
                        {
                            // update book
                            limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - allocated);
                            if (o.isBuy())
                                ob.setTotalAskSize(ob.getTotalAskSize() - allocated);
                            else
                                ob.setTotalBidSize(ob.getTotalBidSize() - allocated);

                            ptr.setCurrentQuantity(bookOrderRemainder);
                        }
                    }
                    else
                    {
                        // TODO: we shouldn't reach here
                        ob.removeOrder(ptr.getOrderId(), true);
                    }

                    ptr = ptr.getNextOrder();
                }
            }

            if (o.getCurrentQuantity() == 0)
            {
                // order is fully filled
                logger.info("orderID: {} - fully filled.", o.getOrderId());
                break;
            }

            // If limit is depleted and there is still unfilled qty, we move on to the next best limit.
            logger.info("limit price {} cleared. Order still has {} remaining qty. Continuing to next best limit...", limit.getPrice(), o.getCurrentQuantity());
        }

        ob.clearEmptyLimitsAfterMatching(o.isBuy());

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
        int totalSize = o.isBuy() ? ob.getTotalAskSize() : ob.getTotalBidSize();
        if (o.getCurrentQuantity() >= totalSize)
        {
            // if e.g. total ask size is 10000, but an aggressive buy order comes in for 20000, we aren't going to match it.
            logger.error("order qty ({}) is greater than the total aggressive size ({}). Not matching.", o.getInitialQuantity(), totalSize);
            return;
        }

        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();
        double farTouchPrice = o.isBuy() ? ob.getBestAsk() : ob.getBestBid();
        int initialQty = o.getCurrentQuantity();
        int filledQty = 0;

        logger.info("order qty: {}, filled qty: {}/{}", initialQty, filledQty, initialQty);

        for (Limit limit: limitTree)
        {
            if (limit.getPrice() == farTouchPrice)
            {
                logger.info("iterating through resting orders at limit={} only...", limit.getPrice());
                Order ptr = limit.getHead();
                Order headOrder = limit.getHead();
                int remainingQty = o.getCurrentQuantity();
                int modifiedTotalVolumeAtLimit = limit.getTotalVolumeAtLimit();

                while(ptr!=null && o.getCurrentQuantity() > 0)
                {
                    if (ptr == headOrder) // top
                    {
                        logger.info("attempting to match entire top order. orderID: {}, price: {}, qty: {}", ptr.getOrderId(), ptr.getPrice(), ptr.getCurrentQuantity());
                        int topOrderRemainingQty = Math.max(ptr.getCurrentQuantity() - o.getCurrentQuantity(), 0);
                        if (topOrderRemainingQty == 0) {
                            Trade trade = new Trade(o.getSide(), limit.getPrice(), ptr.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId());
                            trades.add(trade);
                            logger.info("new trade: {}", trade);

                            o.setCurrentQuantity(o.getCurrentQuantity() - ptr.getCurrentQuantity());
                            filledQty += ptr.getCurrentQuantity();
                            ob.removeOrder(ptr.getOrderId(), true);
                        } else {
                            // top order fully fills the incoming order, update book, exit while loop (matching finished)
                            Trade trade = new Trade(o.getSide(), limit.getPrice(), o.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId());
                            trades.add(trade);
                            logger.info("new trade: {}", trade);
                            logger.info("top order fully fills incoming order. Matching complete.");

                            limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - o.getCurrentQuantity());
                            if (o.isBuy())
                                ob.setTotalAskSize(ob.getTotalAskSize() - o.getCurrentQuantity());
                            else
                                ob.setTotalBidSize(ob.getTotalBidSize() - o.getCurrentQuantity());

                            filledQty += o.getCurrentQuantity();
                            o.setCurrentQuantity(0);
                            ptr.setCurrentQuantity(topOrderRemainingQty);
                        }

                        modifiedTotalVolumeAtLimit = limit.getTotalVolumeAtLimit();
                        logger.info("after matching top order - filledQty: {}/{}, totalVolumeAtLimit: {}, unfilledQty: {}", filledQty, initialQty, modifiedTotalVolumeAtLimit, o.getCurrentQuantity());
                    }
                    else
                    {
                        // pro rata
                        logger.info("Top order was fully filled. Continuing with pro rata matching for remaining qty...");
                        double ratio = (double) ptr.getCurrentQuantity() / modifiedTotalVolumeAtLimit;
                        int qtyToFill = Math.min(ptr.getCurrentQuantity(), (int) (ratio * remainingQty));
                        logger.info("Matching with orderID {}, qty: {}, ratio: ({}/{}) = {}, toDistribute: {}, qtyToFill: {}, canDistribute: {}",
                                ptr.getOrderId(), ptr.getCurrentQuantity(), ptr.getCurrentQuantity(), modifiedTotalVolumeAtLimit, ratio, remainingQty, qtyToFill, qtyToFill > 0);

                        Trade trade = new Trade(o.getSide(), limit.getPrice(), qtyToFill, ptr.getOrderId(), o.getOrderId());
                        trades.add(trade);
                        logger.info("new trade: {}", trade);

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
                            logger.info("filledQty: {}/{}", filledQty, initialQty);
                        }
                    }
                    ptr = ptr.getNextOrder();
                }

                logger.info("at the current limit price level = {} - the available liquidity was: {}, we filled: {}, unfilledQty: {}",
                        limit.getPrice(), modifiedTotalVolumeAtLimit, filledQty, o.getCurrentQuantity());

                if (o.getCurrentQuantity() > 0 && filledQty != modifiedTotalVolumeAtLimit)
                {
                    // the limit is not depleted but there is still remainder qty from the pro rata roundoff. We apply FIFO
                    // (time priority) matching for the 'roundoff' quantity.
                    // If the limit was fully depleted without any residue, filledQty == totalVolumeAtLimit, and we don't have to do this FIFO residue distribution.
                    logger.info("there is a rounding residue of {} as a result of the pro rata distribution. Match residue qty with FIFO, iterating through the existing limit level again...",
                            modifiedTotalVolumeAtLimit - filledQty);

                    ptr = limit.getHead();
                    while (ptr != null && o.getCurrentQuantity() > 0)
                    {
                        // either the market order remaining qty is fully filled by the current limit order, or the market order
                        // clears the entire current limit order and we have to move to the next limit order with ptr.
                        int canAllocate = ptr.getCurrentQuantity();
                        int allocated = Math.min(o.getCurrentQuantity(), canAllocate);
                        logger.info("Matching residue qty of {} with orderID {} (price: {}, qty: {}/{}), unfilled qty: {}. We can allocate a maximum of {} lots.",
                                modifiedTotalVolumeAtLimit - filledQty, ptr.getOrderId(), ptr.getPrice(), canAllocate, ptr.getInitialQuantity(), o.getCurrentQuantity(), allocated);

                        if (allocated > 0)
                        {
                            Trade trade = new Trade(o.getSide(), limit.getPrice(), allocated, ptr.getOrderId(), o.getOrderId());
                            trades.add(trade);
                            logger.info("new trade: {}", trade);

                            // if howMuchLeftToFill reaches 0, the pro rata residue volume has been fully distributed, we can exit the while loop.
                            int howMuchLeftToFill = o.getCurrentQuantity() - allocated;
                            o.setCurrentQuantity(howMuchLeftToFill);

                            // current limit order is fully filled, we can remove it from the book
                            int bookOrderRemainder = ptr.getCurrentQuantity() - allocated;
                            if (bookOrderRemainder == 0)
                            {
                                ob.removeOrder(ptr.getOrderId(), true);
                            }
                            else
                            {
                                // update book
                                limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - allocated);
                                if (o.isBuy())
                                    ob.setTotalAskSize(ob.getTotalAskSize() - allocated);
                                else
                                    ob.setTotalBidSize(ob.getTotalBidSize() - allocated);

                                ptr.setCurrentQuantity(bookOrderRemainder);
                            }
                        }
                        else
                        {
                            // TODO: we shouldn't reach here
                            ob.removeOrder(ptr.getOrderId(), true);
                        }

                        ptr = ptr.getNextOrder();
                    }
                }

                if (o.getCurrentQuantity() == 0)
                {
                    // order is fully filled
                    logger.info("orderID: {} - fully filled.", o.getOrderId());
                    break;
                }
            }
            // If limit is depleted and there is still unfilled qty, create a new passive order with remaining qty.
        }

        ob.clearEmptyLimitsAfterMatching(o.isBuy());

        if (o.getCurrentQuantity() > 0)
        {
            logger.info("aggressive limit order cleared the entire far touch qty. Creating a new limit for the remaining qty.");
            ob.addOrder(new Order(o.getSecurityId(), o.getSide(), o.getCurrentQuantity(), farTouchPrice));
        }
    }
}

