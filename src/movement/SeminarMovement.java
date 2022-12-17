/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import java.util.List;
import java.util.LinkedList;
import java.io.File;

import core.SimClock;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;
import core.Settings;
import input.WKTReader;

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
public class SeminarMovement extends MapBasedMovement implements
        SwitchableMovement {

    public static final int TO_SEMINAR_MODE = 0;
    public static final int AT_SEMINAR_MODE = 1;
    public static final String SEMINAR_LENGTH = "seminarLength";
    public static final String NR_OF_SEMINARS = "nrOfSeminars";
    public static final String SEMINAR_SIZE = "seminarSize";
    public static final String SEMINAR_LOCATION_FILE = "seminarLocationsFile";

    private static int nrOfSeminars = 10;
    private double minWaitTime;
    private double maxWaitTime;
    private int[] seminarLength;
    private List<Coord> allSeminars;
    private double specificWaitTime;
    private int startedWorkTime;

    private boolean ready;
    private int mode;

    private int distance;
    private double seminarWaitTimeParetoCoeff;
    private DijkstraPathFinder pathFinder;
    private Coord lastWaypoint;
    private Coord seminarLocation;


    /**
     * Creates a UbahnMovement
     * @param settings
     */
    public SeminarMovement(Settings settings) {
        super(settings);

        seminarLength = settings.getCsvInts(SEMINAR_LENGTH);
        nrOfSeminars = settings.getInt(NR_OF_SEMINARS);

        distance = settings.getInt(SEMINAR_SIZE);

        startedWorkTime = -1;
        pathFinder = new DijkstraPathFinder(null);
        mode = TO_SEMINAR_MODE;

        String seminarLocationFile = null;
        try {
            seminarLocationFile = settings.getSetting(SEMINAR_LOCATION_FILE);
        } catch (Throwable t){
            System.out.println("Catch seminar");
        }
        if (seminarLocationFile==null) {
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().
                    toArray(new MapNode[0]);
            int officeIndex = rng.nextInt(mapNodes.length - 1) /
                    (mapNodes.length/ nrOfSeminars);
            seminarLocation = mapNodes[officeIndex].getLocation().clone();
        } else {
            try {
                allSeminars = new LinkedList<Coord>();
                List<Coord> locationRead = (new WKTReader()).readPoints(new File(seminarLocationFile));
                MapBasedMovement tmp = new MapBasedMovement(settings);

                for (Coord coord : locationRead) {
                    SimMap map = tmp.getMap();
                    Coord offset = map.getOffset();
                    if (map.isMirrored()) {
                        coord.setLocation(coord.getX(), -coord.getY());
                    }
                    coord.translate(offset.getX(), offset.getY());
                    allSeminars.add(coord);
                }
                seminarLocation = allSeminars.get(rng.nextInt(allSeminars.size())).clone();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a UbahnMovement from a prototype
     * @param proto
     */
    public SeminarMovement(SeminarMovement proto) {
        super(proto);
        this.mode = proto.mode;
        this.distance = proto.distance;
        this.pathFinder = proto.pathFinder;
        this.seminarLength = proto.seminarLength;
        startedWorkTime = -1;

        if (proto.allSeminars == null){
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().toArray(new MapNode[0]);
            int index = rng.nextInt(mapNodes.length - 1) / (mapNodes.length/nrOfSeminars);
            seminarLocation = mapNodes[index].getLocation().clone();
        } else {
            this.allSeminars = proto.allSeminars;
            seminarLocation = allSeminars.get(rng.nextInt(allSeminars.size())).clone();
        }
        //seminarWaitTimeParetoCoeff = proto.seminarWaitTimeParetoCoeff;
        //minWaitTime = proto.minWaitTime;
        //maxWaitTime = proto.maxWaitTime;
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
        if (mode == TO_SEMINAR_MODE){
            SimMap map = super.getMap();
            if (map == null){
                return null;
            }
            MapNode thisNode = map.getNodeByCoord(lastWaypoint);
            MapNode destinationNode = map.getNodeByCoord(seminarLocation);
            List<MapNode> nodes = pathFinder.getShortestPath(thisNode, destinationNode);
            Path path = new Path(generateSpeed());
            for (MapNode node : nodes) {
                path.addWaypoint(node.getLocation());
            }
            lastWaypoint = seminarLocation.clone();
            mode = AT_SEMINAR_MODE;
            return path;
        }
        if (startedWorkTime == -1){
            startedWorkTime = SimClock.getIntTime();
        }
        if(SimClock.getIntTime() >= startedWorkTime + specificWaitTime) {
            Path path = new Path(1);
            path.addWaypoint(lastWaypoint.clone());
            ready = true;
            return path;
        }
        return null;
    }

    /**
     * Switches state between getPath() calls
     * @return Always 0
     */
    protected double generateWaitTime() {
        int lower = seminarLength[0];
        int upper = seminarLength[1];

        specificWaitTime = (upper - lower) * rng.nextDouble() + lower;
        return specificWaitTime;
    }


    @Override
    public MapBasedMovement replicate() {return new SeminarMovement(this);}
    /**
     * @see SwitchableMovement
     */
    public Coord getLastLocation() {return lastWaypoint.clone();}

    /**
     * @see SwitchableMovement
     */
    public void setLocation(Coord lastWaypoint) {
        this.lastWaypoint = lastWaypoint.clone();
        mode = TO_SEMINAR_MODE;
        startedWorkTime = -1;
        ready = false;
    }

    /**
     * @see SwitchableMovement
     */
    public boolean isReady() {return ready;}

    public Coord getSeminarLocation() {return seminarLocation.clone();}

    public List<Coord> getAllSeminars() {return this.allSeminars;}

}
