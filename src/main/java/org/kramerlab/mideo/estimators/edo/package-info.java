/**
 * Provides the EDO density estimator, which can be used to estimate
 * joint and conditional densities. {@code ChainBasedEstimator} is the
 * plain version of EDO without any modules or inference
 * operations. {@code EDO} provides facilities to estimate the modules
 * of the current data stream and uses {@code ChainBasedEstimator} for
 * the density estimate of a module. Additionally, it offers inference
 * operations that directly operate on the base estimators.
 */
package org.kramerlab.mideo.estimators.edo;
