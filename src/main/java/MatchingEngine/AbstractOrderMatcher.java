package MatchingEngine;

import Orders.Trade;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class AbstractOrderMatcher implements IOrderMatcher {
    final List<Trade> trades = new ArrayList<>();

    public void printTrades()
    {
        for (Trade t: trades)
        {
            System.out.println(t);
        }
    }
}
