package MatchingEngine;

public class OrderMatcherFactory {
    public static AbstractOrderMatcher createOrderMatcher(String matcher)
    {
        switch (matcher)
        {
            case "pricetime": return new PriceTimePriorityMatcher();
            case "prorata" : return new ProRataMatcher();
            case "proratawithtop" : return new ProRataWithTopMatcher();
            default: throw new RuntimeException("matcher invalid.");
        }
    }
}
