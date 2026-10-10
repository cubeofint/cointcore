package com.mawlee.cointcore.shop;

import java.util.ArrayList;
import java.util.List;

/**
 * Seller filter tokens ({@code by:nick}, {@code %nick}, {@code ~nick}) live in the
 * market query but must not be forwarded to JEI.
 */
public final class MarketSearchSeller {
    private MarketSearchSeller() {
    }

    public static boolean hasTerms(String query) {
        for (List<MarketSearchQuery.Term> group : MarketSearchQuery.parse(query)) {
            for (MarketSearchQuery.Term term : group) {
                if (term.kind() == MarketSearchQuery.Kind.SELLER) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String strip(String query) {
        return rewrite(query, false);
    }

    public static String fragment(String query) {
        return rewrite(query, true);
    }

    public static String merge(String itemQuery, String sellerFragment) {
        String item = itemQuery == null ? "" : itemQuery.trim();
        String seller = sellerFragment == null ? "" : sellerFragment.trim();
        if (seller.isEmpty()) {
            return item;
        }
        if (item.isEmpty()) {
            return seller;
        }
        return item + " " + seller;
    }

    private static String rewrite(String query, boolean sellersOnly) {
        if (query == null || query.isBlank()) {
            return "";
        }
        List<String> groups = new ArrayList<>();
        for (List<MarketSearchQuery.Term> group : MarketSearchQuery.parse(query)) {
            List<String> kept = new ArrayList<>();
            for (MarketSearchQuery.Term term : group) {
                boolean seller = term.kind() == MarketSearchQuery.Kind.SELLER;
                if (seller != sellersOnly) {
                    continue;
                }
                kept.add(render(term));
            }
            if (!kept.isEmpty()) {
                groups.add(String.join(" ", kept));
            }
        }
        return String.join(" | ", groups);
    }

    private static String render(MarketSearchQuery.Term term) {
        String prefix = switch (term.kind()) {
            case NAME -> "";
            case MOD -> "@";
            case TOOLTIP -> "#";
            case TAG -> "$";
            case COLOR -> "^";
            case CREATIVE_TAB -> "%";
            case RESOURCE_ID -> "&";
            case SELLER -> "by:";
        };
        String body = term.text();
        if (body.indexOf(' ') >= 0) {
            body = "\"" + body + "\"";
        }
        return (term.negated() ? "-" : "") + prefix + body;
    }
}
