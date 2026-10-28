package io.github.allannava.cragcast.export;

import io.github.allannava.cragcast.ferrata.FerrataForecast;

/** Lets tests in other packages use SiteDataTest's made-up via ferrata forecast. */
public final class SiteDataTestAccess {

    private SiteDataTestAccess() {
    }

    public static FerrataForecast top(boolean storm) {
        return SiteDataTest.top(storm);
    }
}
