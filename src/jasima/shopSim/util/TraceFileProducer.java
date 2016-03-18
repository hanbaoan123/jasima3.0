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
package jasima.shopSim.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import jasima.core.simulation.SimComponent;
import jasima.core.simulation.Simulation;
import jasima.core.util.AbstractResultSaver;
import jasima.shopSim.core.IndividualMachine;
import jasima.shopSim.core.Job;
import jasima.shopSim.core.PrioRuleTarget;
import jasima.shopSim.core.Shop;
import jasima.shopSim.core.ShopListener;
import jasima.shopSim.core.WorkStation;
import jasima.shopSim.core.WorkStationListener;

/**
 * Produces a detailed trace of all events of a {@link Shop} in a text file.
 * Creating this file is rather slow, so this class is mainly useful for
 * debugging purposes.
 * 
 * @author Torsten Hildebrandt, 2012-08-24
 */
public class TraceFileProducer implements ShopListener {

	// parameters

	private String fileName;

	// used during run

	private PrintWriter log;

	private String name;

	public TraceFileProducer() {
		super();
	}

	public TraceFileProducer(String fileName) {
		this();

		setFileName(fileName);
	}

	protected WorkStationListener createWSListener() {
		return new WorkStationListener() {
			@Override
			public void arrival(WorkStation m, Job j) {
				if (!j.isFuture()) {
					print(m.shop().simTime() + "\tarrives_at\t" + j + "\t" + m + "\t"
							+ (m.numBusy() == 0 ? "IDLE" : "PROCESSING") + "\t" + (m.numJobsWaiting() - 1));
				}
			}

			@Override
			public void activated(WorkStation ws, IndividualMachine m) {
				print(ws.shop().simTime() + "\tbecomes_available\t" + m.toString() + "\t" + ws.numJobsWaiting()
						+ (m.downReason == null ? "" : "\t" + String.valueOf(m.downReason)));
			}

			@Override
			public void deactivated(WorkStation ws, IndividualMachine m) {
				print(ws.shop().simTime() + "\tunavailable\t" + m.toString() + "\t" + ws.numJobsWaiting()
						+ (m.downReason == null ? "" : "\t" + String.valueOf(m.downReason)));
			}

			@Override
			public void operationStarted(WorkStation m, PrioRuleTarget jobOrBatch, int oldSetupState, int newSetupState,
					double setTime) {
				if (jobOrBatch == null) {
					print(m.shop().simTime() + "\tkeeping_idle\t" + m.currMachine.toString() + "\t" + jobOrBatch);
				} else {
					for (int i = 0; i < jobOrBatch.numJobsInBatch(); i++)
						print(m.shop().simTime() + "\tstart_processing\t" + m.currMachine.toString() + "\t"
								+ jobOrBatch.job(i) + "\t" + "\t" + m.numJobsWaiting());
					// shop.log().debug(
					// shop.simTime + "\tstart_processing\t" + machName + "\t"
					// + batch + "\t" + "\t" + queue.size());
					if (oldSetupState != newSetupState) {
						print(m.shop().simTime() + "\tsetup\t" + m.currMachine.toString() + "\t"
								+ m.setupStateToString(oldSetupState) + "\t" + m.setupStateToString(newSetupState)
								+ "\t" + setTime);
					}
				}
			}

			@Override
			public void operationCompleted(WorkStation m, PrioRuleTarget jobOrBatch) {
				// shop.log().debug(
				// shop.simTime + "\tfinished_processing\t" + machName + "\t"
				// + b);
				for (int i = 0; i < jobOrBatch.numJobsInBatch(); i++)
					print(m.shop().simTime() + "\tfinished_processing\t" + m.currMachine.toString() + "\t"
							+ jobOrBatch.job(i));
			}
		};
	}

	@Override
	public void jobReleased(Shop shop, Job j) {
		print(shop.simTime() + "\tenter_system\t" + j);
	}

	@Override
	public void jobFinished(Shop shop, Job j) {
		print(shop.simTime() + "\tleave_system\t" + j);
	}

	@Override
	public void simEnd(SimComponent shop) {
		print(shop.getSim().simTime() + "\tsim_end");

		log.close();
		log = null;
	}

	@Override
	public void init(SimComponent shop) {
		createLogFile(shop.getSim());
	}

	@Override
	public void simStart(SimComponent shop) {
		print(shop.getSim().simTime() + "\tsim_start");

		Shop s = (Shop) shop;
		s.installMachineListener(createWSListener(), false);
	}

	protected void print(String line) {
		log.println(line);
	}

	private void createLogFile(Simulation sim) {
		try {
			name = getFileName();
			if (name == null) {
				// create some default name
				name = "jasimaTrace" + new SimpleDateFormat("_yyyyMMdd_HHmmss").format(new Date());
				// don't overwrite existing
				name = AbstractResultSaver.findFreeFile(name, ".txt") + ".txt";
			}
			log = new PrintWriter(new BufferedWriter(new FileWriter(name)), true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		String n = getFileName();
		if (n == null)
			n = name;
		return getClass().getSimpleName() + "(" + n + ")";
	}

	// getter/setter for parameter below

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
