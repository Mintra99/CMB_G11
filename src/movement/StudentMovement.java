/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import core.SimClock;

/**
 *
 * This movement model makes use of several other movement models to simulate
 * movement with daily routines. People wake up in the morning, go to work,
 * go shopping or similar activities in the evening and finally go home to
 * sleep.
 *
 * @author Frans Ekman
 */
public class StudentMovement extends ExtendedMovementModel {

    private BusTravellerMovement busTravellerMM;
    private OfficeActivityMovement workerMM;
    private HomeActivityMovement homeMM;
    private TransportMovement movementUsedForTransfers;
    private static final String KEEP_STUDYING_PROB = "keepStudyingProb";
    private static final String DAY_END = "dayEnd";
    private static final int BUS_TO_WORK_MODE = 0;
    private static final int BUS_TO_HOME_MODE = 1;
    private static final int WORK_MODE = 3;
    private static final int HOME_MODE = 4;
    private double ksprob = 0;
    private int dayend = 64800;
    private int mode;


    /**
     * Creates a new instance of WorkingDayMovement
     * @param settings
     */
    public StudentMovement(Settings settings) {
        super(settings);
        busTravellerMM = new BusTravellerMovement(settings);
        workerMM = new OfficeActivityMovement(settings);
        homeMM = new HomeActivityMovement(settings);
        movementUsedForTransfers = busTravellerMM;
        setCurrentMovementModel(homeMM);
        mode = HOME_MODE;
        if (settings.contains("keepStudyingProb")){
            ksprob = settings.getDouble(KEEP_STUDYING_PROB);
        }
        if (settings.contains("dayEnd")){
            dayend = settings.getInt(DAY_END);
        }
    }

    /**
     * Creates a new instance of WorkingDayMovement from a prototype
     * @param proto
     */
    public StudentMovement(StudentMovement proto) {
        super(proto);
        busTravellerMM = new BusTravellerMovement(proto.busTravellerMM);
        workerMM = new OfficeActivityMovement(proto.workerMM);
        homeMM = new HomeActivityMovement(proto.homeMM);
        movementUsedForTransfers = busTravellerMM;
        setCurrentMovementModel(homeMM);
        mode = proto.mode;
        ksprob = proto.ksprob;
        dayend = proto.dayend;
    }

    @Override
    public boolean newOrders() {
        switch (mode) {
            case WORK_MODE:
                if (workerMM.isReady()) {
                    if ((rng.nextDouble() < ksprob) && (SimClock.getTime() % 86400) < dayend){
                        setCurrentMovementModel(workerMM);
                        mode = WORK_MODE;
                        break;
                    }
                    setCurrentMovementModel(movementUsedForTransfers);
                    movementUsedForTransfers.setNextRoute(
                            workerMM.getOfficeLocation(),
                            homeMM.getHomeLocation());
                    mode = BUS_TO_HOME_MODE;
                }
                break;
            case HOME_MODE:
                if (homeMM.isReady()) {
                    setCurrentMovementModel(movementUsedForTransfers);
                    movementUsedForTransfers.setNextRoute(homeMM.getHomeLocation(),
                            workerMM.getOfficeLocation());
                    mode = BUS_TO_WORK_MODE;
                }
                break;
            case BUS_TO_WORK_MODE:
                if (movementUsedForTransfers.isReady()) {
                    setCurrentMovementModel(workerMM);
                    mode = WORK_MODE;
                }
                break;
            case BUS_TO_HOME_MODE:
                if (movementUsedForTransfers.isReady()) {
                    setCurrentMovementModel(homeMM);
                    mode = HOME_MODE;
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public Coord getInitialLocation() {
        Coord homeLoc = homeMM.getHomeLocation().clone();
        homeMM.setLocation(homeLoc);
        return homeLoc;
    }

    @Override
    public MovementModel replicate() {
        return new StudentMovement(this);
    }

    public Coord getOfficeLocation() {
        return workerMM.getOfficeLocation().clone();
    }

    public Coord getHomeLocation() {
        return homeMM.getHomeLocation().clone();
    }

}
