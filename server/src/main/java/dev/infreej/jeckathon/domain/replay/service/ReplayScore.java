package dev.infreej.jeckathon.domain.replay.service;

public final class ReplayScore {

	private static final double VOLATILITY_CAP = 20;
	private static final double CHANGE_RATE_CAP = 10;
	private static final int VOLATILITY_WEIGHT = 80;
	private static final int CHANGE_RATE_WEIGHT = 20;

	private ReplayScore() {
	}

	public static double calculate(double changeRate, double volatility) {
		double volatilityPoints = Math.min(volatility / VOLATILITY_CAP, 1) * VOLATILITY_WEIGHT;
		double changePoints = Math.min(Math.abs(changeRate) / CHANGE_RATE_CAP, 1) * CHANGE_RATE_WEIGHT;
		return Math.round(volatilityPoints + changePoints);
	}
}
