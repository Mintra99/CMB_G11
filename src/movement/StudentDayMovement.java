
/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * This movement model makes use of several other movement models to simulate
 * movement with daily routines. People wake up in the morning, go to work,
 * go shopping or similar activities in the evening and finally go home to
 * sleep.
 *
 * @author Frans Ekman
 */
public class StudentDayMovement extends ExtendedMovementModel {

    public static final String PROBABILITY_TO_LECTURE_SETTING = "lectureProb";
    public static final String PROBABILITY_TO_LIBRARY_SETTING = "libraryProb";
    public static final String PROBABILITY_TO_SEMINAR_SETTING = "seminarProb";
    public static final String PROBABILITY_TO_UBAHN_SETTING = "ubahnProb";

    private UbahnMovement ubahnMovement;
    private LectureMovement lectureMovement;
    private LibraryMovement libraryMovement;
    private SeminarMovement seminarMovement;

    private static final int UBAHN_MODE = 0;
    private static final int LECTURE_MODE = 1;
    private static final int LIBRARY_MODE = 2;
    private static final int SEMINAR_MODE = 3;

    private static final int TO_UBAHN = 4;
    private static final int TO_LECTURE = 5;
    private static final int TO_LIBRARY = 6;
    private static final int TO_SEMINAR = 7;

    private int mode;

    private double toLectureProb;
    private double toSeminarProb;
    private double toUbahnProb;
    private double toLibraryProb;

    private StudentWalkMovement SWmm;
    private TransportMovement movementUsedForTransfers;

    /**
     * Creates a new instance of WorkingDayMovement
     * @param settings
     */
    public StudentDayMovement(Settings settings) {
        super(settings);
        ubahnMovement = new UbahnMovement(settings);
        lectureMovement = new LectureMovement(settings);
        libraryMovement = new LibraryMovement(settings);
        seminarMovement = new SeminarMovement(settings);

        toLectureProb = settings.getDouble(PROBABILITY_TO_LECTURE_SETTING);
        toSeminarProb = settings.getDouble(PROBABILITY_TO_SEMINAR_SETTING);
        toUbahnProb = settings.getDouble(PROBABILITY_TO_UBAHN_SETTING);
        toLibraryProb = settings.getDouble(PROBABILITY_TO_LIBRARY_SETTING);

        SWmm = new StudentWalkMovement(settings);
        movementUsedForTransfers = SWmm;

        setCurrentMovementModel(ubahnMovement);
        mode = UBAHN_MODE;
    }

    /**
     * Creates a new instance of StudentDayMovement from a prototype
     * @param proto
     */
    public StudentDayMovement(StudentDayMovement proto) {
        ubahnMovement = new UbahnMovement(proto.ubahnMovement);
        lectureMovement = new LectureMovement(proto.lectureMovement);
        libraryMovement = new LibraryMovement(proto.libraryMovement);
        seminarMovement = new SeminarMovement(proto.seminarMovement);

        toUbahnProb = proto.toUbahnProb;
        toLectureProb = proto.toLectureProb;
        toLibraryProb = proto.toLibraryProb;
        toSeminarProb = proto.toSeminarProb;

        SWmm = new StudentWalkMovement(proto.SWmm);
        movementUsedForTransfers = SWmm;

        setCurrentMovementModel(ubahnMovement);
        mode = proto.mode;
    }

    @Override
    public boolean newOrders() {
        switch (mode) {
            case UBAHN_MODE:
                if (ubahnMovement.isReady()) {
                    setCurrentMovementModel(movementUsedForTransfers);
                    if (toLectureProb > rng.nextDouble()) {
                        movementUsedForTransfers.setNextRoute(
                                ubahnMovement.getUBahnLocation(),
                                lectureMovement.getLectureLocation());
                        mode = TO_LECTURE;
                    } else if (toSeminarProb > rng.nextDouble()){
                        movementUsedForTransfers.setNextRoute(
                                ubahnMovement.getUBahnLocation(),
                                seminarMovement.getSeminarLocation());
                        mode = TO_SEMINAR;
                    } else {
                        movementUsedForTransfers.setNextRoute(
                                ubahnMovement.getUBahnLocation(),
                                libraryMovement.getLibraryLocation());
                        mode = TO_LIBRARY;
                    }
                }
                break;
            case LECTURE_MODE:
                if (lectureMovement.isReady()) {
                    setCurrentMovementModel(movementUsedForTransfers);
                    if (toUbahnProb > rng.nextDouble()) {
                        movementUsedForTransfers.setNextRoute(
                                lectureMovement.getLectureLocation(),
                                ubahnMovement.getUBahnLocation());
                        mode = TO_UBAHN;
                    } else if(toSeminarProb > rng.nextDouble()){
                        movementUsedForTransfers.setNextRoute(
                                lectureMovement.getLectureLocation(),
                                seminarMovement.getSeminarLocation());
                        mode = TO_SEMINAR;
                    } else{
                        movementUsedForTransfers.setNextRoute(
                                lectureMovement.getLectureLocation(),
                                libraryMovement.getLibraryLocation());
                        mode = TO_LIBRARY;
                    }
                }
                break;
            case SEMINAR_MODE:
                if (seminarMovement.isReady()) {
                    setCurrentMovementModel(movementUsedForTransfers);
                    if (toUbahnProb > rng.nextDouble()) {
                        movementUsedForTransfers.setNextRoute(
                                seminarMovement.getSeminarLocation(),
                                ubahnMovement.getUBahnLocation());
                        mode = TO_UBAHN;
                    } else if(toLectureProb > rng.nextDouble()){
                        movementUsedForTransfers.setNextRoute(
                                seminarMovement.getSeminarLocation(),
                                lectureMovement.getLectureLocation());
                        mode = TO_LECTURE;
                    } else{
                        movementUsedForTransfers.setNextRoute(
                                seminarMovement.getSeminarLocation(),
                                libraryMovement.getLibraryLocation());
                        mode = TO_LIBRARY;
                    }
                }
                break;
            case LIBRARY_MODE:
                if (libraryMovement.isReady()) {
                    setCurrentMovementModel(movementUsedForTransfers);
                    if (toUbahnProb > rng.nextDouble()) {
                        movementUsedForTransfers.setNextRoute(
                                libraryMovement.getLibraryLocation(),
                                ubahnMovement.getUBahnLocation());
                        mode = TO_UBAHN;
                    } else if(toLectureProb > rng.nextDouble()){
                        movementUsedForTransfers.setNextRoute(
                                libraryMovement.getLibraryLocation(),
                                lectureMovement.getLectureLocation());
                        mode = TO_LECTURE;
                    } else{
                        movementUsedForTransfers.setNextRoute(
                                libraryMovement.getLibraryLocation(),
                                seminarMovement.getSeminarLocation());
                        mode = TO_SEMINAR;
                    }
                }
                break;
            case TO_LECTURE:
                if (movementUsedForTransfers.isReady()){
                    setCurrentMovementModel(lectureMovement);
                    mode = LECTURE_MODE;
                }
                break;
            case TO_LIBRARY:
                if (movementUsedForTransfers.isReady()){
                    setCurrentMovementModel(libraryMovement);
                    mode = LIBRARY_MODE;
                }
                break;
            case TO_SEMINAR:
                if (movementUsedForTransfers.isReady()){
                    setCurrentMovementModel(seminarMovement);
                    mode = SEMINAR_MODE;
                }
                break;
            case TO_UBAHN:
                if (movementUsedForTransfers.isReady()){
                    setCurrentMovementModel(ubahnMovement);
                    mode = UBAHN_MODE;
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public Coord getInitialLocation() {
        Coord ubahnLoc = ubahnMovement.getUBahnLocation().clone();
        ubahnMovement.setLocation(ubahnLoc);
        return ubahnLoc;
    }

    @Override
    public MovementModel replicate() {return new StudentDayMovement(this);}

    public SwitchableMovement getModel() {return this.getCurrentMovementModel(); }

}
