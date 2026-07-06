package com.rideshare.platform.route.service;

import com.uber.h3core.H3Core;
import com.uber.h3core.LengthUnit;
import com.uber.h3core.exceptions.H3Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(H3Service.class);

    // h3.gridDistance() can throw for cell pairs it can't compute a local grid distance for -
    // e.g. far apart, or straddling a pentagon/icosahedron-face distortion region, even when the
    // underlying points are otherwise unremarkable (a real limitation of the H3 algorithm, not
    // something callers can avoid by validating input first). Treating that as "not usably
    // close" rather than propagating keeps one bad cell pair from failing an entire search/booking
    // request that's comparing many candidate points via Comparator.comparingDouble(...).min(...).
    private static final double UNCOMPUTABLE_DISTANCE_KM = Double.MAX_VALUE;

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
        long gridDistance;
        try {
            gridDistance = h3.gridDistance(cellA, cellB);
        } catch (H3Exception e) {
            log.debug("h3.gridDistance could not compute a distance between {} and {}: {}", cellA, cellB, e.getMessage());
            return UNCOMPUTABLE_DISTANCE_KM;
        }
        return gridDistance * edgeLenKm * 1.5; // 1.5x factor approximates path vs straight hex-grid distance
    }

    public int resolution() {
        return resolution;
    }
}
