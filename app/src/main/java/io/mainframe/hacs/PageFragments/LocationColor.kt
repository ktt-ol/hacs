package io.mainframe.hacs.PageFragments

/**
 * Created by holger on 20.01.18.
 */
class LocationColor {
    val colors = listOf(
        "Space" to "#2ecc71",
        "Radstelle" to "#1f3a93",
        "Fräse" to "#fcb900",
        "Holz" to "#96411b",
        "Lager" to "#19b5fe",
        "Grillplatz" to "#d91e18",
    )

    fun getColor(location: String): String? {
        for (color in colors) {
            if (color.first == location) {
                return color.second
            }
        }

        return null
    }
}
