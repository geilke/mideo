/**
 * RED (Representative-based online Estimation of Densities) addresses
 * the problem of estimating the density of heterogeneous data streams
 * in higher dimensions. 
 *
 * <p>The main idea is to project the data stream into a vector space of
 * lower dimensionality by computing distances to well-defined reference
 * points. In particular, it distinguishes between three types of
 * objects: landmarks, representatives, and instances. Landmarks are
 * reference points spanning a vector space for representatives and
 * instances, so that the position of each object can be defined in
 * terms of distances to the landmarks.</p>
 * 
 * <p>Representatives stand for clusters of instances and will be the
 * main components for estimating probabilities. The landmarks will be
 * used to compute the relative distance of instances, and the
 * representatives will maintain statistical information about instances
 * that have been observed in their neighborhood.</p>
 *
 * <p>A candidate is a precursor of a representative. It will be turned
 * into a representative, if it gathers enough distance vectors around
 * it.</p>
 *
 * <p>The intuition behind these objects is that the landmarks provide a
 * space with certain properties and guarantees and that the
 * representatives and candidates are responsible for modeling the
 * density.</p>
 */
package org.kramerlab.mideo.estimators.red;
