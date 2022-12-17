/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Settings;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class EpidemicRouter extends ActiveRouter {

	// Odds of virus spreading
	public static final double TRANSMISSION_ODDS = 0.25;

	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";

	public static final String EPIDEMIC_NS = "EpidemicRouter";
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */

	private int secondsInTimeUnit;
	public EpidemicRouter(Settings s) {
		super(s);
		Settings epidemicSettings = new Settings(EPIDEMIC_NS);
		secondsInTimeUnit = epidemicSettings.getInt(SECONDS_IN_UNIT_S);

		System.out.println("EPI");
		System.out.println(secondsInTimeUnit);

		//TODO: read&use epidemic router specific settings (if any)
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected EpidemicRouter(EpidemicRouter r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		//TODO: copy epidemic settings here (if any)
	}

	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
	}


	@Override
	public EpidemicRouter replicate() {
		return new EpidemicRouter(this);
	}

}
