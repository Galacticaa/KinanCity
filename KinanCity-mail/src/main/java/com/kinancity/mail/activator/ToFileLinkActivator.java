package com.kinancity.mail.activator;

import java.util.Properties;

import com.kinancity.mail.Activation;
import com.kinancity.mail.FileLogger;

/**
 * Fake Activator that just saves as file
 *
 * @author drallieiv
 *
 */
public class ToFileLinkActivator implements LinkActivator {

    private Properties config;

    public ToFileLinkActivator(Properties config) {
        this.config = config;
    }

	@Override
	public boolean activateLink(Activation link) {
		FileLogger.logStatus(link, FileLogger.SKIPPED, config);
		return true;
	}

}
