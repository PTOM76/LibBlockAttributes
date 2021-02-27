/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.util.List;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.fluid.FixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.FluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.AbstractCombined;

/** An {@link FixedFluidInvView} that delegates to a list of them instead of storing items directly. */
public class CombinedFixedFluidInvView<InvType extends FixedFluidInvView> extends AbstractCombined<InvType> implements FixedFluidInvView {

    public final List<? extends InvType> views;
    private final int[] subTankStartIndex;
    private final int invSize;

    public CombinedFixedFluidInvView(List<? extends InvType> views) {
        super(views);
        this.views = views;
        subTankStartIndex = new int[views.size()];
        int size = 0;
        for (int i = 0; i < views.size(); i++) {
            subTankStartIndex[i] = size;
            FixedFluidInvView view = views.get(i);
            int s = view.getTankCount();
            size += s;
        }
        invSize = size;
    }

    @Override
    public int getTankCount() {
        return invSize;
    }

    protected InvType getInv(int tank) {
        if (tank < 0) {
            throw new IllegalArgumentException("Tank must be non-negative! (was " + tank + ")");
        }

        for (int i = 0; i < subTankStartIndex.length; i++) {
            int startIndex = subTankStartIndex[i];
            if (tank < startIndex) {
                return views.get(i);
            }
        }
        if (tank < invSize) {
            return views.get(views.size() - 1);
        }

        throw new IllegalArgumentException(
            "Tank must be less than getInvSize() (was " + tank + ", maximum tank is " + (invSize - 1) + ")"
        );
    }

    protected int getSubTank(int tank) {
        if (tank < 0) {
            throw new IllegalArgumentException("Tank must be non-negative! (was " + tank + ")");
        }

        for (int i = 0; i < subTankStartIndex.length; i++) {
            int startIndex = subTankStartIndex[i];
            if (tank < startIndex) {
                if (i == 0) {
                    return tank;
                }
                return tank - subTankStartIndex[i - 1];
            }
        }
        if (tank < invSize) {
            return tank - subTankStartIndex[subTankStartIndex.length - 1];
        }

        throw new IllegalArgumentException(
            "Tank must be less than getInvSize() (was " + tank + ", maximum tank is " + (invSize - 1) + ")"
        );
    }

    @Override
    public FluidVolume getInvFluid(int tank) {
        return getInv(tank).getInvFluid(getSubTank(tank));
    }

    @Override
    public boolean isFluidValidForTank(int tank, FluidKey fluid) {
        return getInv(tank).isFluidValidForTank(getSubTank(tank), fluid);
    }

    @Override
    public FluidFilter getFilterForTank(int tank) {
        return getInv(tank).getFilterForTank(getSubTank(tank));
    }

    @Override
    @Deprecated // (since = "0.6.0", forRemoval = true)
    public int getMaxAmount(int tank) {
        return getInv(tank).getMaxAmount(getSubTank(tank));
    }

    @Override
    public FluidAmount getMaxAmount_F(int tank) {
        return getInv(tank).getMaxAmount_F(getSubTank(tank));
    }

    @Override
    public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
        final ListenerToken[] tokens = new ListenerToken[views.size()];
        final ListenerRemovalToken ourRemToken = new ListenerRemovalToken() {

            boolean hasAlreadyRemoved = false;

            @Override
            public void onListenerRemoved() {
                for (ListenerToken token : tokens) {
                    if (token == null) {
                        // This means we have only half-initialised
                        // (and all of the next tokens must also be null)
                        return;
                    }
                    token.removeListener();
                }
                if (!hasAlreadyRemoved) {
                    hasAlreadyRemoved = true;
                    removalToken.onListenerRemoved();
                }
            }
        };
        for (int i = 0; i < tokens.length; i++) {
            final int index = i;
            tokens[i] = views.get(i).addListener((inv, subTank, previous, current) -> {
                listener.onChange(this, subTankStartIndex[index] + subTank, previous, current);
            }, ourRemToken);
            if (tokens[i] == null) {
                for (int j = 0; j < i; j++) {
                    tokens[j].removeListener();
                }
                return null;
            }
        }
        return () -> {
            for (ListenerToken token : tokens) {
                token.removeListener();
            }
        };
    }
}
