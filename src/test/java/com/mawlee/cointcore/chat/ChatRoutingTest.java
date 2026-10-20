package com.mawlee.cointcore.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRoutingTest {
    private static ChatRouting.Route route(String raw, ChatChannel.View view) {
        return ChatRouting.route(raw, "!", "$", view);
    }

    @Test
    void prefixesPickChannelAndAreStripped() {
        ChatRouting.Route global = route("! hello", ChatChannel.View.ALL);
        assertEquals(ChatChannel.GLOBAL, global.channel());
        assertEquals("hello", global.text());

        ChatRouting.Route trade = route("$sell diamonds", ChatChannel.View.LOCAL);
        assertEquals(ChatChannel.TRADE, trade.channel());
        assertEquals("sell diamonds", trade.text());
    }

    @Test
    void plainLineFollowsViewAndDefaultsToLocal() {
        assertEquals(ChatChannel.LOCAL, route("hi", ChatChannel.View.ALL).channel());
        assertEquals(ChatChannel.LOCAL, route("hi", ChatChannel.View.LOCAL).channel());
        assertEquals(ChatChannel.GLOBAL, route("hi", ChatChannel.View.GLOBAL).channel());
        assertEquals(ChatChannel.TRADE, route("hi", ChatChannel.View.TRADE).channel());
        assertEquals("hi", route("hi", null).text());
    }

    @Test
    void emptyAfterPrefixAndPrivateViewSendNothing() {
        ChatRouting.Route empty = route("!   ", ChatChannel.View.ALL);
        assertNull(empty.channel());
        assertEquals(ChatRouting.EMPTY_AFTER_PREFIX, empty.error());

        ChatRouting.Route pm = route("hi", ChatChannel.View.PRIVATE);
        assertNull(pm.channel());
        assertEquals(ChatRouting.PRIVATE_VIEW, pm.error());
    }

    @Test
    void emptyPrefixCharDisablesShortcut() {
        assertEquals(ChatChannel.LOCAL, ChatRouting.route("!hi", "", "", ChatChannel.View.ALL).channel());
    }

    @Test
    void viewParsingAndVisibility() {
        assertEquals(ChatChannel.View.GLOBAL, ChatChannel.View.parse("G").orElseThrow());
        assertEquals(ChatChannel.View.PRIVATE, ChatChannel.View.parse("pm").orElseThrow());
        assertTrue(ChatChannel.View.parse("nope").isEmpty());
        assertTrue(ChatChannel.View.ALL.shows(ChatChannel.TRADE));
        assertTrue(ChatChannel.View.LOCAL.shows(ChatChannel.LOCAL));
        assertFalse(ChatChannel.View.LOCAL.shows(ChatChannel.GLOBAL));
    }
}
