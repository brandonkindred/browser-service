package com.looksee.browsing;

/**
 * Provides access to the cost and reward of an object
 */
public interface IObjectValuationAccessor {

	/**
	 * Computes and returns the cost of the current object
	 * 
	 * @return the cost of the current object
	 */
	public abstract double getCost();
	
	/**
	 * Computes and returns the reward for the current object
	 * 
	 * @return the reward for the current object
	 */
	public abstract double getReward();
}
