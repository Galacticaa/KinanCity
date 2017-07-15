package com.kinancity.mail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLogger {

	public static final String OK = "OK";
	public static final String DONE = "DONE";
	public static final String BAD = "BAD";
	public static final String THROTTLED = "THROTTLED";
	public static final String ERROR = "ERROR";

	public static final String SKIPPED = "SKIPPED";

	private static Logger LOGGER = LoggerFactory.getLogger("LINKS");
	private static Logger errlog = LoggerFactory.getLogger("com.kinancity.mail");

	private static Connection dbConn = null;

	public static void logStatus(Activation link, String status) {
		LOGGER.info("{};{};{}", link.getLink(), link.getEmail(), status);

		try {
			Class.forName("com.mysql.jdbc.Driver");

			String dsn = String.format("jdbc:mysql://%s:%s/%s", "localhost", 3306, "somedb");
			dbConn = DriverManager.getConnection(dsn, "root", "root");

			if (dbConn == null) {
				errlog.info("Failed to connect to database.");
				return;
			}

			PreparedStatement query;

			switch (status) {
				case OK:
					query = dbConn.prepareStatement("UPDATE accounts SET activated_at=? WHERE email=?");
					query.setTimestamp(1, Timestamp.from(Instant.now()));
					query.setString(2, link.getEmail());
					query.executeUpdate();
					query.close();
					break;
				case DONE:
					query = dbConn.prepareStatement("SELECT activated_at FROM accounts WHERE email=? LIMIT 1");

					ResultSet result = query.executeQuery();
					if (result.next() && result.getTimestamp("activated_at") == null) {
						result.updateTimestamp("activated_at", Timestamp.from(Instant.now()));
						result.updateRow();
					}
					query.close();
					break;
				default:
					break;
			}

			dbConn.close();
		} catch (ClassNotFoundException e) {
			errlog.info("Could not find JDBC driver.");
		} catch (SQLException e) {
			errlog.info("Failed to set activation date for " + link.getEmail());
		}
	}

	public static Activation fromLog(String line) {
		String[] parts = line.split(";");
		if (parts.length > 2) {
			return new Activation(parts[0], parts[1], parts[2]);
		} else {
			return new Activation(parts[0], null, parts[1]);
		}
	}
}
