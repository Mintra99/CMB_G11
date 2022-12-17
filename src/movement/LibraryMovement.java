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
    public static final String LIBRARY_SIZE = "librarySize";
    public static final String LIBRARY_LOCATION_FILE = "libraryLocationsFile";


    private static int nrOfLibraries = 1;
    //7private double minWaitTime;
    //private double maxWaitTime;
    private int[] libraryLength;
    private List<Coord> allLibraries;
    private double specificWaitTime;
    private int startedWorkTime;

    private boolean ready;
    private int mode;
    private int distance;
    //private double libraryWaitTimeParetoCoeff;
    private DijkstraPathFinder pathFinder;
    private Coord lastWaypoint;
    private Coord libraryLocation;

    /**
     * Creates a UbahnMovement
     * @param settings
     */
    public LibraryMovement(Settings settings) {
        super(settings);

        libraryLength = settings.getCsvInts(LIBRARY_LENGTH);
        nrOfLibraries = settings.getInt(NR_OF_LIBRARY);

        distance = settings.getInt(LIBRARY_SIZE);

        startedWorkTime = -1;
        pathFinder = new DijkstraPathFinder(null);
        mode = TO_LIBRARY_MODE;

        String libraryLocationFile = null;
        try {
            libraryLocationFile = settings.getSetting(LIBRARY_LOCATION_FILE);
        } catch (Throwable t) {
            System.out.println("Catch library");
        }
        if (libraryLocationFile==null) {
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().
                    toArray(new MapNode[0]);
            int officeIndex = rng.nextInt(mapNodes.length - 1) /
                    (mapNodes.length/ nrOfLibraries);
            libraryLocation = mapNodes[officeIndex].getLocation().clone();
        } else {
            try {
                allLibraries = new LinkedList<Coord>();
                List<Coord> locationRead = (new WKTReader()).readPoints(new File(libraryLocationFile));
                MapBasedMovement tmp = new MapBasedMovement(settings);

                for (Coord coord : locationRead) {
                    SimMap map = tmp.getMap();
                    Coord offset = map.getOffset();
                    if (map.isMirrored()) {
                        coord.setLocation(coord.getX(), -coord.getY());
                    }
                    coord.translate(offset.getX(), offset.getY());
                    allLibraries.add(coord);
                }
                libraryLocation = allLibraries.get(rng.nextInt(allLibraries.size())).clone();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a LibraryMovement from a prototype
     * @param proto
     */

    public LibraryMovement(LibraryMovement proto) {
        super(proto);
        this.mode = proto.mode;
        this.distance = proto.distance;
        this.pathFinder = proto.pathFinder;
        this.libraryLength = proto.libraryLength;
        startedWorkTime = -1;

        if (proto.allLibraries == null){
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().toArray(new MapNode[0]);
            int index = rng.nextInt(mapNodes.length - 1) / (mapNodes.length/nrOfLibraries);
            libraryLocation = mapNodes[index].getLocation().clone();
        } else {
            this.allLibraries = proto.allLibraries;
            libraryLocation = allLibraries.get(rng.nextInt(allLibraries.size())).clone();
        }
        //libraryWaitTimeParetoCoeff = proto.libraryWaitTimeParetoCoeff;
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
            MapNode destinationNode = map.getNodeByCoord(libraryLocation);
            List<MapNode> nodes = pathFinder.getShortestPath(thisNode, destinationNode);
            Path path = new Path(generateSpeed());
            for (MapNode node : nodes) {
                path.addWaypoint(node.getLocation());
            }
            lastWaypoint = libraryLocation.clone();
            mode = AT_LIBRARY_MODE;
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
    protected double generateWaitTime() {
        int lower = libraryLength[0];
        int upper = libraryLength[1];

        specificWaitTime = (upper - lower) * rng.nextDouble() + lower;
        return specificWaitTime;
    }


    @Override
    public MapBasedMovement replicate() {
        return new LibraryMovement(this);
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
        startedWorkTime = -1;
        ready = false;
        mode = TO_LIBRARY_MODE;
    }
    public boolean isReady() {
        return ready;
    }

    public Coord getLibraryLocation() {
        return libraryLocation.clone();
    }
    public List<Coord> getAllLibraries() { return this.allLibraries; }

}
