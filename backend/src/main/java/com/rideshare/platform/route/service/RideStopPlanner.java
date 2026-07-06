package com.rideshare.platform.route.service;

import com.rideshare.platform.common.GeoUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Orders a small set of stops (a handful of passenger pickup or drop-off points on one ride)
 * into an approximately shortest open path from a fixed starting point - nearest-neighbor for a
 * quick initial tour, then 2-opt to remove the obvious crossings/detours nearest-neighbor leaves
 * behind. Distances are straight-line (haversine) rather than road distance: this runs inline
 * when a driver taps "Start ride" and only needs to rank a few candidate orderings against each
 * other, not produce turn-by-turn directions, so it doesn't justify the latency/cost of calling
 * out to a routing provider for every pair of stops.
 */
@Service
public class RideStopPlanner {

    public record Stop(String key, double lat, double lng) {}

    /** Orders {@code stops} starting from ({@code startLat}, {@code startLng}) - that start
     *  point is not itself included in the returned list, it's just where the tour begins. */
    public List<Stop> plan(double startLat, double startLng, List<Stop> stops) {
        return planConstrained(startLat, startLng, stops, candidate -> true);
    }

    /** Same as {@link #plan}, but prefers a reordering that also satisfies {@code constraint} -
     *  e.g. "no passenger's drop-off detour exceeds the ride's max detour" - over one that's
     *  merely shorter. The search itself always explores by pure distance (so it isn't blocked
     *  from looking past a constraint-failing local step to find a better one beyond it), but
     *  separately tracks the shortest candidate seen that *does* satisfy the constraint and
     *  returns that if one was found anywhere during the search. If no explored tour ever
     *  satisfies the constraint (some detour is unavoidable once a driver picks up more than one
     *  passenger before dropping anyone off), falls back to the shortest tour found overall -
     *  never to an arbitrary, unvalidated "safe-looking" order that might in fact be worse on
     *  every measure than the one the constraint just rejected. */
    public List<Stop> planConstrained(double startLat, double startLng, List<Stop> stops, Predicate<List<Stop>> constraint) {
        if (stops.size() <= 1) {
            return List.copyOf(stops);
        }
        List<Stop> nearestNeighborOrder = nearestNeighbor(startLat, startLng, stops);
        return twoOpt(startLat, startLng, nearestNeighborOrder, constraint);
    }

    /** Total open-path distance visiting {@code order} in sequence, starting from the given point. */
    public double totalDistanceMeters(double startLat, double startLng, List<Stop> order) {
        double total = 0;
        double curLat = startLat;
        double curLng = startLng;
        for (Stop s : order) {
            total += GeoUtils.haversineMeters(curLat, curLng, s.lat(), s.lng());
            curLat = s.lat();
            curLng = s.lng();
        }
        return total;
    }

    private List<Stop> nearestNeighbor(double startLat, double startLng, List<Stop> stops) {
        List<Stop> remaining = new ArrayList<>(stops);
        List<Stop> ordered = new ArrayList<>(stops.size());
        double curLat = startLat;
        double curLng = startLng;
        while (!remaining.isEmpty()) {
            Stop nearest = remaining.get(0);
            double best = GeoUtils.haversineMeters(curLat, curLng, nearest.lat(), nearest.lng());
            for (Stop candidate : remaining) {
                double d = GeoUtils.haversineMeters(curLat, curLng, candidate.lat(), candidate.lng());
                if (d < best) {
                    best = d;
                    nearest = candidate;
                }
            }
            ordered.add(nearest);
            remaining.remove(nearest);
            curLat = nearest.lat();
            curLng = nearest.lng();
        }
        return ordered;
    }

    private List<Stop> twoOpt(double startLat, double startLng, List<Stop> initialOrder, Predicate<List<Stop>> constraint) {
        List<Stop> best = new ArrayList<>(initialOrder);
        double bestDistance = totalDistanceMeters(startLat, startLng, best);

        List<Stop> bestConstrained = constraint.test(best) ? best : null;
        double bestConstrainedDistance = bestConstrained != null ? bestDistance : Double.MAX_VALUE;

        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 0; i < best.size() - 1; i++) {
                for (int j = i + 1; j < best.size(); j++) {
                    List<Stop> candidate = reverseSegment(best, i, j);
                    double candidateDistance = totalDistanceMeters(startLat, startLng, candidate);

                    if (candidateDistance < bestConstrainedDistance - 1e-6 && constraint.test(candidate)) {
                        bestConstrained = candidate;
                        bestConstrainedDistance = candidateDistance;
                    }
                    if (candidateDistance < bestDistance - 1e-6) {
                        best = candidate;
                        bestDistance = candidateDistance;
                        improved = true;
                    }
                }
            }
        }
        return bestConstrained != null ? bestConstrained : best;
    }

    private List<Stop> reverseSegment(List<Stop> order, int i, int j) {
        List<Stop> next = new ArrayList<>(order.subList(0, i));
        List<Stop> reversed = new ArrayList<>(order.subList(i, j + 1));
        Collections.reverse(reversed);
        next.addAll(reversed);
        next.addAll(order.subList(j + 1, order.size()));
        return next;
    }
}
