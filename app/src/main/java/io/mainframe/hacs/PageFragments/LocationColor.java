package io.mainframe.hacs.PageFragments;

import android.util.Pair;

import java.util.Arrays;

/**
 * Created by holger on 20.01.18.
 */

public class LocationColor {

    private Pair<String, String>[] colors = new Pair[]{
            new Pair("Space", "#008000"),
            new Pair("Radstelle", "#9696fd"),
            new Pair("Fräse", "#fdca96"),
            new Pair("Holz", "#761c19")
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
