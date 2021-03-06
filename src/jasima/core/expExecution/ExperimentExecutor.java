/*******************************************************************************
 * This file is part of jasima, v1.3, the Java simulator for manufacturing and 
 * logistics.
 *  
 * Copyright (c) 2015 		jasima solutions UG
 * Copyright (c) 2010-2015 Torsten Hildebrandt and jasima contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package jasima.core.expExecution;

import java.util.ArrayList;
import java.util.Collection;

import jasima.core.experiment.Experiment;

/**
 * Base class for classes executing experiments. This class implements the
 * Abstract Factory pattern, therefore ExperimentExecutor.getExecutor() has to
 * be called to create executor instances. This call is delegated to a
 * non-abstract implementation of ExperimentExecutor. Which ExperimentExecutor
 * to use is determined by a system property "
 * {@code jasima.core.expExecution.ExperimentExecutor}". As a default, a
 * {@link ThreadPoolExecutor} is used (with a maximum number of threads equal to
 * the number of available processors).
 * 
 * @author Torsten Hildebrandt
 * @version "$Id$"
 * @see ThreadPoolExecutor
 * @see ForkJoinPoolExecutor
 */
public abstract class ExperimentExecutor {

	public static final String EXECUTOR_FACTORY = ExperimentExecutor.class.getName();
	public static final String DEFAULT_FACTORY = ThreadPoolExecutor.class.getName();

	private static volatile ExperimentExecutor execFactoryInst = null;

	public static ExperimentExecutor getExecutor() {
		if (execFactoryInst == null) {
			synchronized (ExperimentExecutor.class) {
				if (execFactoryInst == null) { // double check for thread safety
					String factName = System.getProperty(EXECUTOR_FACTORY, DEFAULT_FACTORY);
					try {
						Class<?> c = Class.forName(factName);
						execFactoryInst = (ExperimentExecutor) c.newInstance();
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}

					// cleanup
					Runtime.getRuntime().addShutdownHook(new Thread() {
						@Override
						public void run() {
							ExperimentExecutor.getExecutor().shutdownNow();
						}
					});
				}
			}
		}

		return execFactoryInst;
	}

	/**
	 * Protected constructor, use {@link #getExecutor()} instead.
	 */
	protected ExperimentExecutor() {
		super();
	}

	/**
	 * Runs an experiment usually in an asynchronous ways. Therefore an
	 * {@link ExperimentFuture} is returned to access results once they become
	 * available.
	 * 
	 * @param e
	 *            The experiment to execute.
	 * @param parent
	 *            The parent experiment of "e". This might be null.
	 * 
	 * @return An {@link ExperimentFuture} to access experiment results.
	 */
	public abstract ExperimentFuture runExperiment(Experiment e, Experiment parent);

	/**
	 * Shuts down this {@link ExperimentExecutor}.
	 */
	public abstract void shutdownNow();

	/**
	 * Execute many experiments at once. The implementation here simply calls
	 * {@link #runExperiment(Experiment,Experiment)} for all experiments in
	 * {@code es}.
	 * 
	 * @param es
	 *            A list of {@link Experiment}s to run.
	 * @param parent
	 *            The parent experiment of "es". This might be null.
	 * 
	 * @return A {@link Collection} of {@link ExperimentFuture}s, one for each
	 *         submitted experiment.
	 */
	public Collection<ExperimentFuture> runAllExperiments(Collection<? extends Experiment> es, Experiment parent) {
		ArrayList<ExperimentFuture> res = new ArrayList<ExperimentFuture>(es.size());

		for (Experiment e : es) {
			res.add(runExperiment(e, parent));
		}

		return res;
	}

	/**
	 * Clears the singleton {@link ExperimentExecutor} instance. Use this method
	 * only for testing purposes!
	 */
	public static void clearInst() {
		synchronized (ExperimentExecutor.class) {
			if (execFactoryInst != null)
				execFactoryInst.shutdownNow();
			execFactoryInst = null;
		}
	}

}
