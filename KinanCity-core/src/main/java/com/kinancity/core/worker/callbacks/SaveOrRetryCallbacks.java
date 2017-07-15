package com.kinancity.core.worker.callbacks;

import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.model.AccountCreation;
import com.kinancity.core.model.CreationFailure;
import com.kinancity.core.scheduling.AccountCreationQueue;

public class SaveOrRetryCallbacks implements CreationCallbacks {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Connection dbConn;

	// Queue where item can be rescheduled
	private AccountCreationQueue queue;

	// Final place for accountCreation successfully processed
	private List<AccountCreation> done;

	// Final place for accountCreation that failed
	private List<AccountCreation> failed;

	private ResultLogger resultLogger;

	// Number of try max
	private int nbMaxTries = 3;

	public SaveOrRetryCallbacks(AccountCreationQueue queue, List<AccountCreation> done, List<AccountCreation> failed, Connection dbConn, ResultLogger resultLogger) {
		this.queue = queue;
		this.done = done;
		this.failed = failed;
		this.dbConn = dbConn;
		this.resultLogger = resultLogger;
	}

	/**
	 * If this is just a technical error. Reschedule the creation.
	 */
	@Override
	public void onTechnicalIssue(AccountCreation accountCreation) {
		if (accountCreation.getFailures().size() < nbMaxTries) {
			logger.info("Will retry");
			queue.addFirst(accountCreation);
		} else {
			logger.warn("Will not retry, max tries reached.");
			CreationFailure lastFailure = accountCreation.getFailures().get(accountCreation.getFailures().size() - 1);
			resultLogger.logLine(accountCreation.getAccountData().toCsv() + ";ERROR;" + lastFailure.getErrorCode());
			failed.add(accountCreation);
		}
	}

	@Override
	public void onSuccess(AccountCreation accountCreation) {
		resultLogger.logLine(accountCreation.getAccountData().toCsv() + ";OK;");
		done.add(accountCreation);

		try {
			PreparedStatement query = dbConn.prepareStatement("UPDATE accounts SET registered_at=? WHERE username=?");
			query.setTimestamp(1, Timestamp.from(Instant.now()));
			query.setString(2, accountCreation.getAccountData().getUsername());
			query.executeUpdate();
		} catch (SQLException e) {
			logger.info("Failed to set registration date for " + accountCreation.getAccountData().getUsername());
		}
	}

	@Override
	public void onFailure(AccountCreation accountCreation) {
		CreationFailure lastFailure = accountCreation.getFailures().get(accountCreation.getFailures().size() - 1);
		resultLogger.logLine(accountCreation.getAccountData().toCsv() + ";ERROR;" + lastFailure.getErrorCode());
		failed.add(accountCreation);
	}

}
