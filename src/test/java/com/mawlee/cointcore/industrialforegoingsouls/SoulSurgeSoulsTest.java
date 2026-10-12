package com.mawlee.cointcore.industrialforegoingsouls;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulSurgeSoulsTest {
    @Test
    void leftoverTickingTimeCountsAsSouls() {
        assertTrue(SoulSurgeSouls.hasSouls(new Surge(12, Network.EMPTY)));
        assertEquals(12, SoulSurgeSouls.tickingTime(new Surge(12, Network.EMPTY)));
    }

    @Test
    void dummySubclassTickingTimeDoesNotHidePaidDuration() {
        MixinDummySurge surge = new MixinDummySurge(18, Network.EMPTY);
        assertEquals(0, surge.tickingTime);
        assertEquals(18, SoulSurgeSouls.tickingTime(surge));
        assertTrue(SoulSurgeSouls.hasSouls(surge));
    }

    @Test
    void networkTankCountsWhenTickingTimeIsZero() {
        Surge surge = new Surge(0, new Network(7));
        assertEquals(0, SoulSurgeSouls.tickingTime(surge));
        assertEquals(7, SoulSurgeSouls.networkSoulAmount(surge));
        assertTrue(SoulSurgeSouls.hasSouls(surge));
    }

    @Test
    void protectedGetNetworkOnParentIsVisible() {
        assertTrue(SoulSurgeSouls.hasSouls(new ChildSurge(0, new Network(3))));
        assertEquals(3, SoulSurgeSouls.networkSoulAmount(new ChildSurge(0, new Network(3))));
    }

    @Test
    void soulAmountGetterMayBeAnyNumber() {
        assertTrue(SoulSurgeSouls.hasSouls(new Surge(0, new LongNetwork(4L))));
        assertEquals(4, SoulSurgeSouls.networkSoulAmount(new Surge(0, new LongNetwork(4L))));
    }

    @Test
    void soulAmountFieldIsUsedWhenGetterIsMissing() {
        assertEquals(5, SoulSurgeSouls.networkSoulAmount(new Surge(0, new FieldNetwork(5))));
        assertTrue(SoulSurgeSouls.hasSouls(new Surge(0, new FieldNetwork(5))));
    }

    @Test
    void laserBasesSupplySoulsWhenNetworkTankIsEmpty() {
        LaserBase laser = new LaserBase(6);
        OldNetwork network = new OldNetwork(List.of(new Drill(laser)));
        Surge surge = new Surge(0, network);
        assertEquals(0, network.getSoulAmount());
        assertEquals(6, SoulSurgeSouls.networkSoulAmount(surge));
        assertTrue(SoulSurgeSouls.hasSouls(surge));
    }

    @Test
    void isolatedOrBrokenNetworkHasNoSouls() {
        assertFalse(SoulSurgeSouls.hasSouls(new Surge(0, Network.EMPTY)));
        assertFalse(SoulSurgeSouls.hasSouls(new Surge(0, null)));
        assertEquals(0, SoulSurgeSouls.networkSoulAmount(new ThrowingSurge()));
        assertFalse(SoulSurgeSouls.hasSouls(new ThrowingSurge()));
    }

    @Test
    void preRefillTestMatchesIfSoulsConsumeGate() {
        assertFalse(SoulSurgeAcceleration.canWorkThisTick(0, 0));
        assertTrue(SoulSurgeAcceleration.canWorkThisTick(0, 1));
        assertTrue(SoulSurgeAcceleration.canWorkThisTick(1, 0));
    }

    static class Network {
        static final Network EMPTY = new Network(0);
        private final int soulAmount;

        Network(int soulAmount) {
            this.soulAmount = soulAmount;
        }

        public int getSoulAmount() {
            return soulAmount;
        }
    }

    static class LongNetwork {
        private final Long soulAmount;

        LongNetwork(Long soulAmount) {
            this.soulAmount = soulAmount;
        }

        public Long getSoulAmount() {
            return soulAmount;
        }
    }

    static class FieldNetwork {
        @SuppressWarnings("unused")
        private final int soulAmount;

        FieldNetwork(int soulAmount) {
            this.soulAmount = soulAmount;
        }
    }

    static class Surge {
        @SuppressWarnings("unused")
        private final int tickingTime;
        private final Object network;

        Surge(int tickingTime, Object network) {
            this.tickingTime = tickingTime;
            this.network = network;
        }

        public Object getNetwork() {
            return network;
        }
    }

    static class ParentSurge {
        private final Object network;

        ParentSurge(Object network) {
            this.network = network;
        }

        protected Object getNetwork() {
            return network;
        }
    }

    static class ChildSurge extends ParentSurge {
        @SuppressWarnings("unused")
        private final int tickingTime;

        ChildSurge(int tickingTime, Object network) {
            super(network);
            this.tickingTime = tickingTime;
        }
    }

    static class MixinDummySurge extends Surge {
        @SuppressWarnings("unused")
        private final int tickingTime = 0;

        MixinDummySurge(int paidDuration, Object network) {
            super(paidDuration, network);
        }
    }

    static class ThrowingSurge {
        @SuppressWarnings("unused")
        private final int tickingTime = 0;

        public Object getNetwork() {
            throw new NullPointerException("getElement");
        }
    }

    static class LaserBase {
        private final int soulAmount;

        LaserBase(int soulAmount) {
            this.soulAmount = soulAmount;
        }

        public int getSoulAmount() {
            return soulAmount;
        }
    }

    static class Drill {
        private final LaserBase laser;

        Drill(LaserBase laser) {
            this.laser = laser;
        }

        public Drill getLevel() {
            return this;
        }

        public String getPos() {
            return "laser";
        }

        public LaserBase getBlockEntity(String pos) {
            return "laser".equals(pos) ? laser : null;
        }
    }

    static class OldNetwork {
        private final List<Drill> drills;

        OldNetwork(List<Drill> drills) {
            this.drills = drills;
        }

        public int getSoulAmount() {
            return 0;
        }

        public List<Drill> getSoulLaserDrills() {
            return drills;
        }
    }
}
