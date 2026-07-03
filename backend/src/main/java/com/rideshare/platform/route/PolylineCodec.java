package com.rideshare.platform.route;

import java.util.ArrayList;
import java.util.List;

/** Standard Google-encoded-polyline decoder (precision 1e-5), shared by route preview and route storage. */
public final class PolylineCodec {

    private PolylineCodec() {
    }

    public static List<double[]> decode(String encoded) {
        List<double[]> points = new ArrayList<>();
        if (encoded == null) return points;
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int result = 1, shift = 0, b;
            do {
                b = encoded.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encoded.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            points.add(new double[]{lat * 1e-5, lng * 1e-5});
        }
        return points;
    }
}
