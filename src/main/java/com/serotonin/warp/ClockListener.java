/*
 * Copyright (c) 2017, Matthew Lohbihler
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.serotonin.warp;

import java.time.LocalDateTime;

@FunctionalInterface
public interface ClockListener {
    /**
     * @param dateTime
     *            the instant at which this method is called
     */
    void clockUpdate(LocalDateTime dateTime);
}
