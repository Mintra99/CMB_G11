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

public class LibraryMovement extends MapBasedMovement implements
        SwitchableMovement {

    public static final int TO_LIBRARY_MODE = 0;
    public static final int AT_LIBRARY_MODE = 1;

    public static final String LIBRARY_LENGTH = "libraryLength";
    public static final String NR_OF_LIBRARY = "nrOfLibraries";
    public static final String LIBRARY_LOCATION_FILE = "libraryLocationsFile";

    private int mode;
    private static int nrOfLectures = 1;
    private int startedWorkTime;
    private int[] lectureLength;
    private DijkstraPathFinder pathFinder;
    private int distance;
    private Coord lastWaypoint;
    private List<Coord> allLibrary;
    private Coord libaryLocation;
    private boolean ready;
    private double specificWaitTime;

    /**
     * Creates a UbahnMovement
     * @param settings
     */
    public LibraryMovement(Settings settings) {
        super(settings);

        String libraryLocationFile = null;
        try {
            libraryLocationFile = settings.getSetting(LIBRARY_LOCATION_FILE);
        } catch (Throwable t) {
            System.out.println("Catch library");
        }

        try {
            allLibrary = new LinkedList<Coord>();
            List<Coord> locationRead = (new WKTReader()).readPoints(new File(libraryLocationFile));
            MapBasedMovement tmp = new MapBasedMovement(settings);

            for (Coord coord : locationRead){
                SimMap map = tmp.getMap();
                if (map.isMirrored()){
                    coord.setLocation(coord.getX(), -coord.getY());
                }
                System.out.println("lib coord:" + coord);
                allLibrary.add(coord);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a LibraryMovement from a prototype
     * @param proto
     */

    public LibraryMovement(LibraryMovement proto) {
        super(proto);
        this.libaryLocation = proto.libaryLocation;
        this.mode = proto.mode;
        this.distance = proto.distance;
        this.pathFinder = proto.pathFinder;
        this.lectureLength = proto.lectureLength;
        startedWorkTime = -1;

        if (proto.allLibrary == null){
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().toArray(new MapNode[0]);
            int index = rng.nextInt(mapNodes.length - 1) / (mapNodes.length/nrOfLectures);
            libaryLocation = mapNodes[index].getLocation().clone();
        } else {
            this.allLibrary = proto.allLibrary;
            libaryLocation = allLibrary.get(rng.nextInt(allLibrary.size())).clone();
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

    @Override
    public Path getPath() {
        if (mode == TO_LIBRARY_MODE){
            SimMap map = super.getMap();
            if (map == null){
                return null;
            }
            MapNode thisNode = map.getNodeByCoord(lastWaypoint);
            MapNode destinationNode = map.getNodeByCoord(libaryLocation);
            List<MapNode> nodes = pathFinder.getShortestPath(thisNode, destinationNode);
            Path path = new Path(generateSpeed());
            for (MapNode node : nodes) {
                path.addWaypoint(node.getLocation());
            }
            lastWaypoint = libaryLocation.clone();
            mode = AT_LIBRARY_MODE;

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


    @Override
    public MapBasedMovement replicate() {
        return new LibraryMovement(this);
    }

    public Coord getLibraryLocation() {
        return libaryLocation.clone();
    }

    public double nextPathAvailable() { return Double.MAX_VALUE;}

}
