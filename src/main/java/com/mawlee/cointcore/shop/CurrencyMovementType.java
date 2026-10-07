package com.mawlee.cointcore.shop;

public enum CurrencyMovementType {
    PAY("pay"),
    TRADER_BUY("trader_buy"),
    TRADER_SELL("trader_sell"),
    ADMIN_SET("admin_set"),
    ADMIN_ADD("admin_add"),
    SITE_TO_SERVER("site_to_server"),
    SERVER_TO_SITE("server_to_site");

    private final String id;

    CurrencyMovementType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static CurrencyMovementType fromId(String id) {
        if (id == null) {
            return PAY;
        }
        for (CurrencyMovementType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return PAY;
    }

    /**
     * Server-wallet events that the site stores as a log only (must not change the site balance).
     * Site queue transfers stay local so they are not posted back through AzLink.
     */
    public boolean isServerWalletLog() {
        return switch (this) {
            case PAY, TRADER_BUY, TRADER_SELL, ADMIN_SET, ADMIN_ADD -> true;
            case SITE_TO_SERVER, SERVER_TO_SITE -> false;
        };
    }
}
