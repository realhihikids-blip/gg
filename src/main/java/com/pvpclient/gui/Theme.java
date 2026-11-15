package com.pvpclient.gui;

import com.pvpclient.PvpClient;
import com.pvpclient.util.RenderUtil;

/** Dark palette; the two accents come from the Interface settings (default purple + blue). */
public final class Theme {
    public static final int BG = 0xF00B0B13;
    public static final int SIDEBAR = 0xFF10101B;
    public static final int CARD = 0xFF171726;
    public static final int CARD_HOVER = 0xFF1E1E31;
    public static final int TRACK = 0xFF2A2A40;
    public static final int TEXT = 0xFFEDEDFA;
    public static final int DIM = 0xFF8A8AA8;
    public static final int RED = 0xFFEF4444;

    /** Global fade multiplier used for open / category transitions. */
    public static double fade = 1.0;

    private Theme() {}

    public static int a1() { return 0xFF000000 | PvpClient.settings().accentPrimary.get(); }
    public static int a2() { return 0xFF000000 | PvpClient.settings().accentSecondary.get(); }

    /** Applies the global fade to an ARGB color. */
    public static int f(int argb) { return RenderUtil.mulAlpha(argb, fade); }
}
