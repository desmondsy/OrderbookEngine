package MatchingEngine;

public class OrderMatcherFactory {
    public static AbstractOrderMatcher createOrderMatcher(String matcher)
    {
        return switch (matcher) {
            case "pricetime" -> new PriceTimePriorityMatcher();
            case "prorata" -> new ProRataMatcher();
            case "proratawithtop" -> new ProRataWithTopMatcher();
            default -> throw new RuntimeException("matcher invalid.");
        };
    }
}
