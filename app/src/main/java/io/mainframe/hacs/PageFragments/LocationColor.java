package io.mainframe.hacs.PageFragments;

import android.util.Pair;

import java.util.Arrays;

/**
 * Created by holger on 20.01.18.
 */

public class LocationColor {

    private Pair<String, String>[] colors = new Pair[]{
            new Pair("Space", "#2ecc71"),
            new Pair("Radstelle", "#1f3a93"),
            new Pair("Fräse", "#fcb900"),
            new Pair("Holz", "#96411b"),
            new Pair("Lager", "#19b5fe"),
            new Pair("Grillplatz", "#d91e18")
    };

    public String getColor(String location) {
        for (Pair<String, String> color : colors) {
            if (color.first.equals(location)) {
                return color.second;
            }
        }

        return null;
    }

    public Pair<String, String>[] getAll() {
        // return a copy
        return Arrays.copyOf(this.colors, this.colors.length);
    }
}
