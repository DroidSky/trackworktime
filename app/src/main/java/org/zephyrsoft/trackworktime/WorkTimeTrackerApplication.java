/*
 * This file is part of TrackWorkTime (TWT).
 * 
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime;

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_CODE;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.AVAILABLE_MEM_SIZE;
import static org.acra.ReportField.BRAND;
import static org.acra.ReportField.BUILD;
import static org.acra.ReportField.CRASH_CONFIGURATION;
import static org.acra.ReportField.DEVICE_FEATURES;
import static org.acra.ReportField.DISPLAY;
import static org.acra.ReportField.ENVIRONMENT;
import static org.acra.ReportField.FILE_PATH;
import static org.acra.ReportField.INITIAL_CONFIGURATION;
import static org.acra.ReportField.INSTALLATION_ID;
import static org.acra.ReportField.PACKAGE_NAME;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.PRODUCT;
import static org.acra.ReportField.REPORT_ID;
import static org.acra.ReportField.SHARED_PREFERENCES;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.TOTAL_MEM_SIZE;
import static org.acra.ReportField.USER_APP_START_DATE;
import static org.acra.ReportField.USER_CRASH_DATE;
import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.options.DataType;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Application entry point.
 * 
 * @author Mathis Dirksen-Thedens
 */
@ReportsCrashes(formUri = "http://zephyrsoft.org/crashreport.jsp", mode = ReportingInteractionMode.SILENT, customReportContent = {
	ANDROID_VERSION, APP_VERSION_CODE, APP_VERSION_NAME, AVAILABLE_MEM_SIZE, BRAND, BUILD, CRASH_CONFIGURATION,
	DEVICE_FEATURES, DISPLAY, ENVIRONMENT, FILE_PATH, INITIAL_CONFIGURATION, INSTALLATION_ID, PACKAGE_NAME,
	PHONE_MODEL, PRODUCT, REPORT_ID, SHARED_PREFERENCES, STACK_TRACE, TOTAL_MEM_SIZE, USER_APP_START_DATE,
	USER_CRASH_DATE })
public class WorkTimeTrackerApplication extends Application {

	@Override
	public void onCreate() {
		Logger.info("creating application");
		ACRA.init(this);
		Basics.getOrCreateInstance(getApplicationContext());

		Logger.info("running self-tests");
		TimeSum.test();
		DataType.test();
		DateTimeUtil.test();

		Logger.info("handing off to super");
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		Logger.info("terminating application");
		Basics.getOrCreateInstance(getApplicationContext()).getDao().close();
		super.onTerminate();
	}

	@Override
	public void onLowMemory() {
		Logger.info("low memory for application");
		super.onLowMemory();
	}

}
