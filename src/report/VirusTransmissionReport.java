/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Reports delivered messages
 * report csv:
 */
public class VirusTransmissionReport extends Report implements MessageListener {
    // This is used as header for the inputs later on
    public static final String HEADER = "message_id|from|to|creation_time|host_location|section|distance";

    public VirusTransmissionReport() {
        init();
    }

    // Writes the header at the top of the txt file.
    @Override
    public void init() {
        super.init();
        write(HEADER);
    }

    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        String event_string = m.getId() + "|"
                + from.toString() + "|"
                + to.toString() + "|"
                + format(getSimTime()) + "|"
                + from.getLocation() + "|"
                + to.getName() + "|"
                + m.getFrom().getLocation().distance(m.getTo().getLocation());
        write(event_string);
    }

    @Override
    public void done() {
        super.done();
    }

    // nothing to implement for the rest (NOT USED BUT HAVE TO BE HERE :/)
    public void newMessage(Message m) {}
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

}
