/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import java.util.List;
import java.util.Random;

import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;
import core.Settings;

/**
 *
 * This class controls the movement of bus travellers. A bus traveller belongs
 * to a bus control system. A bus traveller has a destination and a start
 * location. If the direct path to the destination is longer than the path the
 * node would have to walk if it would take the bus, the node uses the bus. If
 * the destination is not provided, the node will pass a random number of stops
 * determined by Markov chains (defined in settings).
 *
 * @author Frans Ekman
 *
 */
public class UbahnMovement extends MapBasedMovement implements
        SwitchableMovement {

    public static final int TO_UBAHN_MODE = 0;
    public static final int AT_UBAHN_MODE = 1;

    public static final int READY_MODE = 2;

    public static final String UBAHN_LOCATION = "ubahnLocation";

    private static final int WAIT_15_MIN = 900;

    private int mode;

    private int distance;
    private DijkstraPathFinder pathFinder;


    private Coord lastWaypoint;
    private Coord ubahnLocation;

    private boolean isFirstTime;

    /**
     * Creates a UbahnMovement
     * @param settings
     */
    public UbahnMovement(Settings settings) {
        super(settings);
        distance = 0;
        pathFinder = new DijkstraPathFinder(null);
        mode = AT_UBAHN_MODE;
        isFirstTime = true;

        if (settings.contains(UBAHN_LOCATION)) {
            try{
                double[] xy = settings.getCsvDoubles(UBAHN_LOCATION);
                ubahnLocation = new Coord(xy[0], xy[1]).clone();
                SimMap map = getMap();
                Coord offset = map.getOffset();

                if (map.isMirrored()){
                    ubahnLocation.setLocation(ubahnLocation.getX(), -ubahnLocation.getY());
                }
                ubahnLocation.translate(offset.getX(), offset.getY());
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a UbahnMovement from a prototype
     * @param proto
     */
    public UbahnMovement(UbahnMovement proto) {
        super(proto);
        this.ubahnLocation = proto.ubahnLocation;
        this.mode = proto.mode;
        this.distance = proto.distance;
        this.pathFinder = proto.pathFinder;
        this.isFirstTime = proto.isFirstTime;
    }

    @Override
    public Coord getInitialLocation() {
        double x_coord = rng.nextDouble() * getMaxX();
        double y_coord = rng.nextDouble() * getMaxY();
        Coord c = new Coord(x_coord, y_coord);

        this.lastWaypoint = c;
        return c.clone();
    }

    @Override
    public Path getPath() {
        if (mode == TO_UBAHN_MODE){
            SimMap map = super.getMap();
            if (map == null){
                return null;
            }
            MapNode thisNode = map.getNodeByCoord(lastWaypoint);
            MapNode destinationNode = map.getNodeByCoord(ubahnLocation);
            List<MapNode> nodes = pathFinder.getShortestPath(thisNode, destinationNode);
            Path path = new Path(generateSpeed());
            for (MapNode node : nodes) {
                path.addWaypoint(node.getLocation());
            }
            lastWaypoint = ubahnLocation.clone();
            mode = AT_UBAHN_MODE;

            double new_x = lastWaypoint.getX() + (rng.nextDouble() - 0.5) * distance;
            double new_y = lastWaypoint.getY() + (rng.nextDouble() - 0.5) * distance;

            if (new_x > getMaxX()){
                new_x = getMaxX();
            } else if (new_x < getMaxX()){
                new_x = 0;
            }

            if (new_y > getMaxY()){
                new_y = getMaxY();
            } else if (new_y < getMaxY()){
                new_y = 0;
            }

            Coord c = new Coord(new_x, new_y);
            path.addWaypoint(c);
            return path;
        } else {
            Path path = new Path(AT_UBAHN_MODE);
            path.addWaypoint(lastWaypoint.clone());
            mode = READY_MODE;
            return path;
        }
    }

    /**
     * Switches state between getPath() calls
     * @return Always 0
     */
    protected double generateWaitTime() {
        if (mode == AT_UBAHN_MODE) {
            double base = WAIT_15_MIN * rng.nextInt(20);

            if (isFirstTime) return base;
            isFirstTime = false;

            if (!this.getHost().isIncubated() && this.getHost().getNrofMessages() > 0 && rng.nextDouble() > 0.25){
                return Integer.MAX_VALUE;
            }

            double minT = WAIT_15_MIN * 4 * 12;
            double maxT = WAIT_15_MIN * 4 * 16;
            double sleep = (maxT - minT) * rng.nextDouble() + minT;
            return base + sleep;
        } else {
            return 0;
        }
    }

    @Override
    public MapBasedMovement replicate() {
        return new UbahnMovement(this);
    }



    /**
     * @see SwitchableMovement
     */
    public Coord getLastLocation() {
        return lastWaypoint.clone();
    }

    /**
     * @see SwitchableMovement
     */
    public void setLocation(Coord lastWaypoint) {
        this.lastWaypoint = lastWaypoint.clone();
        mode = TO_UBAHN_MODE;
    }

    /**
     * @see SwitchableMovement
     */
    public boolean isReady() {
        return mode == READY_MODE;
    }

    public Coord getUBahnLocation() {
        return ubahnLocation.clone();
    }

}
