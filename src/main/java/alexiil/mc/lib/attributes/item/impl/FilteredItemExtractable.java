/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

public final class FilteredItemExtractable implements ItemExtractable {
    private final ItemExtractable real;
    public final ItemFilter filter;

    public FilteredItemExtractable(ItemExtractable real, ItemFilter filter) {
        this.real = real;
        this.filter = filter;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        return real.attemptExtraction(filter.and(this.filter), maxAmount, simulation);
    }

    @Override
    public ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return real.attemptExtraction(filter, maxAmount, simulation);
    }

    @Override
    public ItemExtractable filtered(ItemFilter filter) {
        return new FilteredItemExtractable(real, filter.and(this.filter));
    }
}
