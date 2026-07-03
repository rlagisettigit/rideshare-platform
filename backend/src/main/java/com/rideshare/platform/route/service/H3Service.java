package com.rideshare.platform.route.service;

import com.uber.h3core.H3Core;
import com.uber.h3core.LengthUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * FR: Section 7 Route Management - "Convert to H3 Cells" / "Store H3 Index Map".
 * Wraps Uber's H3 spatial indexing library at the configured resolution (app.h3.resolution).
 */
@Service
public class H3Service {

    private final H3Core h3;
    private final int resolution;

    public H3Service(@Value("${app.h3.resolution:9}") int resolution) {
        this.resolution = resolution;
        try {
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize H3Core", e);
        }
    }

    public String latLngToCell(double lat, double lng) {
        return h3.latLngToCellAddress(lat, lng, resolution);
    }

    /** Returns the ring of cells within `k` rings of the given cell - used for pickup-radius search. */
    public List<String> gridDisk(String h3Cell, int k) {
        return h3.gridDisk(h3Cell, k);
    }

    public double gridDistanceKm(String cellA, String cellB) {
        // approximate: use average hexagon edge length at this resolution as a distance proxy
        double edgeLenKm = h3.getHexagonEdgeLengthAvg(resolution, LengthUnit.km);
        long gridDistance = h3.gridDistance(cellA, cellB);
        return gridDistance * edgeLenKm * 1.5; // 1.5x factor approximates path vs straight hex-grid distance
    }

    public int resolution() {
        return resolution;
    }
}
