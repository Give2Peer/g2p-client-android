package org.give2peer.karma;


import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class GeometryUtils {

    /**
     * Checks whether or not the provided point is inside the polygon.
     * Ray casting algorithm, see http://rosettacode.org/wiki/Ray-casting_algorithm
     *
     * @param point to check
     * @param polygon The list of the vertices of the polygon, sequential and looping.
     * @return whether the point is inside the polygon
     */
    public static boolean pointInPolygon(LatLng point, List<LatLng> polygon) {
        int crossings = 0;
        int verticesCount = polygon.size();

        // For each edge
        for (int i = 0; i < verticesCount; i++) {
            int j = (i + 1) % verticesCount;
            LatLng a = polygon.get(i);
            LatLng b = polygon.get(j);
            if (rayCrossesSegment(point, a, b)) crossings++;
        }

        // Odd number of crossings?
        return crossings % 2 == 1;
    }

    /**
     * Ray Casting algorithm checks.
     * Returns true if the point is
     * 1) to the left of the AB segment when AB is vertical and
     * 2) not above nor below the AB segment.
     * Note: we'll use the segment BA if B is lower than A.
     *
     * @param point to check
     * @param a coordinates of the point A
     * @param b coordinates of the point B
     */
    private static boolean rayCrossesSegment(LatLng point, LatLng a, LatLng b) {
        double px = point.longitude,
               py = point.latitude,
               ax = a.longitude,
               ay = a.latitude,
               bx = b.longitude,
               by = b.latitude;
        // Make sure A is always the bottom and B the top
        if (ay > by) {
            ax = b.longitude;
            ay = b.latitude;
            bx = a.longitude;
            by = a.latitude;
        }
        // Alter longitude to cater for 180 degree crossings
        if (px < 0 || ax < 0 || bx < 0) { px += 360; ax += 360; bx += 360; }
        // If the point has the same latitude as a or b, increase slightly py
        if (py == ay || py == by) py += 0.00000001;
        // If the point is above, below or to the right of the segment, it returns false
        if ((py > by || py < ay) || (px > Math.max(ax, bx))) { return false; }
        // If the point is not above, below or to the right and is to the left, return true
        else if (px < Math.min(ax, bx))                      { return true;  }
        // When the two above conditions are not met, compare the slopes of segment AB and AP
        // to see if the point P is to the left of segment AB or not.
        else {
            double slopeAB = (ax != bx) ? ((by - ay) / (bx - ax)) : Double.POSITIVE_INFINITY;
            double slopeAP = (ax != px) ? ((py - ay) / (px - ax)) : Double.POSITIVE_INFINITY;
            return slopeAP >= slopeAB;
        }
    }
}
