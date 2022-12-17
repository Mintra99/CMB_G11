/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.DTNSim;
import core.Settings;
import input.WKTReader;
import movement.map.SimMap;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * A dummy stationary "movement" model where nodes do not move.
 * Might be useful for simulations with only external connection events.
 */
public class LectureStationaryMovement extends MovementModel {
    /** Per node group setting for setting the location ({@value}) */
    public static final String LOCATION_S = "lectureLocationsFile";
    private Coord loc; /** The location of the nodes */

    private int id;
    private static int nextID = 0;
    private List<Coord> allLectures;

    // static initialization of all movement models' random number generator
    static {
        DTNSim.registerForReset(LectureStationaryMovement.class.getCanonicalName());
        reset();
    }

    /**
     * Creates a new movement model based on a Settings object's settings.
     * @param s The Settings object where the settings are read from
     */
    public LectureStationaryMovement(Settings s) {
        super(s);
//		this.id = nextID++;

        String courseLocationsFile = s.getSetting(
                LOCATION_S);

        try {
            allLectures = new LinkedList<Coord>();
            List<Coord> locationsRead = (new WKTReader()).
                    readPoints(new File(courseLocationsFile));
            MapBasedMovement tmp = new MapBasedMovement(s);

            for (Coord coord : locationsRead) {
                SimMap map = tmp.getMap();
                Coord offset = map.getOffset();
                // mirror points if map data is mirrored
                if (map.isMirrored()) {
                    coord.setLocation(coord.getX(), -coord.getY());
                }
                coord.translate(offset.getX(), offset.getY());
                allLectures.add(coord);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Copy constructor.
     * @param sm The StationaryMovement prototype
     */
    public LectureStationaryMovement(LectureStationaryMovement sm) {
        super(sm);
        this.id = nextID++;
        this.loc = sm.allLectures.get(this.id);
    }

    /**
     * Returns the only location of this movement model
     * @return the only location of this movement model
     */
    @Override
    public Coord getInitialLocation() {
        return loc;
    }

    /**
     * Returns a single coordinate path (using the only possible coordinate)
     * @return a single coordinate path
     */
    @Override
    public Path getPath() {
        Path p = new Path(0);
        p.addWaypoint(loc);
        return p;
    }

    @Override
    public double nextPathAvailable() {
        return Double.MAX_VALUE;	// no new paths available
    }

    @Override
    public LectureStationaryMovement replicate() {
        return new LectureStationaryMovement(this);
    }

    public static void reset() {
        nextID = 0;
    }
}
