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
public class LectureMovement extends MapBasedMovement implements
        SwitchableMovement {

    public static final int TO_LECTURE_MODE = 0;
    public static final int AT_LECTURE_MODE = 1;

    public static final String lECTURE_LENGTH = "lectureLength";
    public static final String NR_OF_LECTURES = "nrOfLectures";
    public static final String LECTURE_LOCATION_FILE = "lectureLocationsFile";

    private static int nrOfLecture = 1;
    private static final int WAIT_15_MIN = 900;
    private double minWaitTime;
    private double maxWaitTime;


    private int[] lectureLength;
    private List<Coord> allLectures;
    private double specificWaitTime;
    private int startedWorkTime;

    private boolean ready;
    private int mode;

    private int distance;
    private DijkstraPathFinder pathFinder;
    private Coord lastWaypoint;
    private Coord lectureLocation;

    /**
     * Creates a LectureMovement
     * @param settings
     */
    public LectureMovement(Settings settings) {
        super(settings);

        lectureLength = settings.getCsvInts(lECTURE_LENGTH);
        nrOfLecture = settings.getInt(NR_OF_LECTURES);

        startedWorkTime = -1;
        pathFinder = new DijkstraPathFinder(null);
        mode = TO_LECTURE_MODE;

        String lectureLocationFile = null;
        try {
            lectureLocationFile = settings.getSetting(LECTURE_LOCATION_FILE);
        } catch (Throwable t){
            System.out.println("Catch lecture");
        }

        try{
            allLectures = new LinkedList<Coord>();
            List<Coord> locationRead = (new WKTReader()).readPoints(new File(lectureLocationFile));
            for (Coord coord : locationRead){
                SimMap map = getMap();
                Coord offset = map.getOffset();
                if (map.isMirrored()){
                    coord.setLocation(coord.getX(), -coord.getY());
                }
                allLectures.add(coord);
            }
            lectureLocation = allLectures.get(rng.nextInt(allLectures.size())).clone();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Creates a LectureMovement from a prototype
     * @param proto
     */
    public LectureMovement(LectureMovement proto) {
        super(proto);
        this.lectureLocation = proto.lectureLocation;
        this.mode = proto.mode;
        this.distance = proto.distance;
        this.pathFinder = proto.pathFinder;
        this.lectureLength = proto.lectureLength;
        startedWorkTime = -1;

        if (proto.allLectures == null){
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().toArray(new MapNode[0]);
            int index = rng.nextInt(mapNodes.length - 1) / (mapNodes.length/ nrOfLecture);
            lectureLocation = mapNodes[index].getLocation().clone();
        } else {
            this.allLectures = proto.allLectures;
            lectureLocation = allLectures.get(rng.nextInt(allLectures.size())).clone();
        }

        minWaitTime = proto.minWaitTime;
        maxWaitTime = proto.maxWaitTime;
    }

    @Override
    public Coord getInitialLocation() {
        double x_coord = rng.nextDouble() * getMaxX();
        double y_coord = rng.nextDouble() * getMaxY();
        Coord c = new Coord(x_coord, y_coord);

        this.lastWaypoint = c;
        return c.clone();
    }

    /**
     * Switches state between getPath() calls
     * @return Always 0
     */
    @Override
    public Path getPath() {
        if (mode == TO_LECTURE_MODE){
            SimMap map = super.getMap();
            if (map == null){
                return null;
            }
            MapNode thisNode = map.getNodeByCoord(lastWaypoint);
            MapNode destinationNode = map.getNodeByCoord(lectureLocation);
            List<MapNode> nodes = pathFinder.getShortestPath(thisNode, destinationNode);
            Path path = new Path(generateSpeed());
            for (MapNode node : nodes) {
                path.addWaypoint(node.getLocation());
            }
            lastWaypoint = lectureLocation.clone();
            mode = AT_LECTURE_MODE;

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


    protected double generateWaitTime() {
        int lower = lectureLength[0];
        int upper = lectureLength[1];

        specificWaitTime = (upper - lower) * rng.nextDouble() + lower;
        return specificWaitTime;
    }


    @Override
    public MapBasedMovement replicate() {
        return new LectureMovement(this);
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
        mode = TO_LECTURE_MODE;
        startedWorkTime = -1;
        ready = false;
    }

    /**
     * @see SwitchableMovement
     */
    public boolean isReady() {
        return ready;
    }

    public Coord getLectureLocation() {
        return lectureLocation.clone();
    }

    public List<Coord> getAllLecture() {return this.allLectures;}

}
