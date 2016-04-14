/**
 * Provides an extended version of MOA's HoeffdingTree. The most
 * prominent changes are:
 * <ul>
 * <li>Laplace correction has been added.</li>
 * <li>{@link HoeffdingTree} has a getter method for the root.</li>
 * <li>Various node types of {@link HoeffdingTree} now provide access to
 * their parents and to the probability distribution defined over their
 * children.</li>
 * <li>In the original version of {@link HoeffdingTree}, there was a bug
 * in the {@code HoeffdingTree.attemptToSplit} method: for some
 * datasets, the tree repeatedly splitted on the same attribute. We
 * prevented this behavior by introducing access to a node's parents and
 * adding a corresponding check before splitting.</li>
 * </ul>
 */
package org.kramerlab.mideo.classifiers.trees;
