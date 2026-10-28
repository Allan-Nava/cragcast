package io.github.allannava.cragcast.ferrata;

import java.util.Map;

/** CG-115: the via ferratas' tops the hourly refresh asks the forecast for, by id, in order. */
public interface FerrataTops {

    Map<String, FerrataClient.Top> tops();
}
