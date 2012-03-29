/* UpgradeManager.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          October 22, 2010
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This class has been added in Cryptnos 1.2 to handle migration and upgrade changes
 * that may occur between versions.  This was largely not an issue until 1.2, where
 * we decided to force UTF-8 for text encoding wherever possible.  This created the
 * need for a shared preferences file, and a need to keep track of what version we
 * previously ran and which version we're now trying to run.
 * 
 * The idea is to run the UpgradeManager once per session, typically when the main
 * menu is launched for the first time.  When the UpgradeManager is launched, it will
 * load up the preferences file and try to determine the last version of Cryptnos that
 * ran.  If it's the same version as we're currently running, nothing happens.  Cryptnos
 * will remember that the UpgradeManager ran and try not to run it again until the
 * app is garbage collected and restarted.
 * 
 * If the previously run version is less than the current version, we'll perform a
 * series of checks to see what needs to be upgraded, if anything.  These checks are
 * intended to fall into each other, so multiple steps can be performed if needed.
 * Hopefully, this will bring the preferences up to the current version by the end
 * of the series of checks.  Obviously, if the application has never been run before,
 * or if we're coming from a version of Cryptnos prior to creating the preferences
 * file, we'll end up running all the checks as a means of setting up the entire
 * environment.
 * 
 * If the current running version of Cryptnos is less than the version from the
 * preferences file, then the user must have "downgraded".  We won't officially
 * support this scenario and this will throw an error.
 *
 * UPDATES FOR 1.2.1:  Added 1.2.1 version code.  This is a minor update, so it
 * probably isn't even needed.  Still, for completeness, we've added it in.
 * 
 * UPDATES FOR 1.2.2:  Added the 1.2.2 version code, but commented out both it and
 * the 1.2.1 code.  These aren't currently needed but are present primarily for
 * documentation purposes.
 * 
 * UPDATES FOR 1.2.3:  Added the 1.2.3 version code, commented out for now.
 * 
 * UPDATES FOR 1.2.4:  Added the 1.2.4 version code and logic to add the new "copy
 * passwords to clipboard" setting to the shared preferences.
 * 
 * UPDATES FOR 1.2.5:  Added the 1.2.5 version code, commented out for now.
 * 
 * UPDATES FOR 1.3.0:  Added the 1.3.0 version code.  Added default preferences for
 * "Show Master Passwords" and QR code scanner preference.
 * 
 * UPDATES FOR 1.3.1:  Added the 1.3.1 version code.  Added default preference for
 * "Clear Passwords on Focus Loss" preference.
 * 
 * "QR code" is a registered trademark of Denso Wave Incorporated.
 * 
 * This program is Copyright 2012, Jeffrey T. Darlington.
 * E-mail:  android_support@cryptnos.com
 * Web:     http://www.cryptnos.com/
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See theGNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
*/
package com.gpfcomics.android.cryptnos;

import java.util.regex.Pattern;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

/**
 * The UpgradeManager performs a series of checks to see what if anything needs to be
 * changed or "upgraded" if the currently running version of Cryptnos is different
 * than the last run version.
 * @author Jeffrey T. Darlington
 * @version 1.3.1
 * @since 1.2
 */
public class UpgradeManager {

	/** A constant to represent the special case when no previous version
	 *  information has been saved. */
	private static final int NO_PREVIOUS_VERSION = -1;
	
	// We'll comment out the following constants to limit the number of warnings
	// in Eclipse, but they're here if we need them later.  We weren't really
	// concerned with upgrade stuff before 1.2.0 so it may be safe to completely
	// ignore prior versions.
	
	///** A constant representing the integer version of Cryptnos 1.0, our
	// *  first public release. */
	//private static final int VERSION_1_0_0 = 1;
	
	///** A constant representing the integer version of Cryptnos 1.1, where
	// *  we introduced cross-platform import/export. */
	//private static final int VERSION_1_1_0 = 2;
	
	/** A constant representing the integer version of Cryptnos 1.2, where
	 *  we introduced the UpgradeManager and UTF-8 as the default text
	 *  encoding. */
	private static final int VERSION_1_2_0 = 3;
	
	///** A constant representing the integer version of Cryptnos 1.2.1, which
	//    mostly consists of minor UI enhancements.  Since there are not any
	//    changes major enough to require upgrading, this constant is currently
	//    commented out until needed. */
	//private static final int VERSION_1_2_1 = 4;
	
	///** A constant representing the integer version of Cryptnos 1.2.2, which
    //    mostly consists of minor UI enhancements.  Since there are not any
	//    changes major enough to require upgrading, this constant is currently
	//    commented out until needed. */
	//private static final int VERSION_1_2_2 = 5;

	///** A constant representing the integer version of Cryptnos 1.2.3, which
    //    mostly consists of a bug fix.  Since there are not any
	//    changes major enough to require upgrading, this constant is currently
	//    commented out until needed. */
	//private static final int VERSION_1_2_3 = 6;

	/** A constant representing the integer version of Cryptnos 1.2.4, which
        introduces the checkbox to toggle on and off copying passcodes to the
        clipboard.  Since this setting was not previously saved anywhere (it
        defaulted to always on,) we'll need the UpgradeManager to add it to
        the preferences file. */
	private static final int VERSION_1_2_4 = 7;

	///** A constant representing the integer version of Cryptnos 1.2.5, which
    //    mostly consists of a bug fix.  Since there are not any
	//    changes major enough to require upgrading, this constant is currently
	//    commented out until needed. */
	//private static final int VERSION_1_2_5 = 8;

	///** A constant representing the integer version of Cryptnos 1.2.6, which
    //    mostly consists of a bug fix.  Since there are not any
	//    changes major enough to require upgrading, this constant is currently
	//    commented out until needed. */
	//private static final int VERSION_1_2_6 = 9;

	///** A constant representing the integer version of Cryptnos 1.2.7, which
    //    mostly consists of a bug fix.  Since there are not any
	//    changes major enough to require upgrading, this constant is currently
	//    commented out until needed. */
	//private static final int VERSION_1_2_7 = 10;

	/** A constant representing the integer version of Cryptnos 1.3, where
	 *  we introduced import and export via QR codes and improve import flexibility
	 *  by letting the user pick and choose which sites in an export file they'd
	 *  like to import. */
	private static final int VERSION_1_3_0 = 11;
	
	private static final int VERSION_1_3_1 = 12;

	/** A regular expression Pattern for matching the UTF-8 character
	 *  encoding string.  This pattern is case insensitive. */
	private Pattern regex_utf8 =
		Pattern.compile("^utf-?8$", Pattern.CASE_INSENSITIVE);
	
	/** A regular expression Pattern for matching the ISO-8859-1 character
	 *  encoding string.  This pattern is case insensitive. */
	private Pattern regex_iso8859 =
		Pattern.compile("^iso-8859-1$", Pattern.CASE_INSENSITIVE);
	
	/** A regular expression Pattern for matching the US-ASCII character
	 *  encoding string.  This pattern is case insensitive. */
	private Pattern regex_ascii =
		Pattern.compile("^(us-)?ascii$", Pattern.CASE_INSENSITIVE);
	
	/** A regular expression Pattern for matching the Windows-1252 character
	 *  encoding string.  This pattern is case insensitive. */
	private Pattern regex_win1252 =
		Pattern.compile("^windows-1252$", Pattern.CASE_INSENSITIVE);
	
	/** A reference to the overall CryptnosApplication, which is where we
	 *  get the app SharedPreferences from as well as get access to certain
	 *  app-level methods. */
	private CryptnosApplication theApp = null;
	
	/** A reference to the calling Activity, primarily for displaying
	 *  dialog information and getting Context */
	private Activity caller = null;
	
	/** The SharedPreferences for Cryptnos, obtained from the
	 *  CryptnosApplication.getPrefs() call.  We'll put this in its own
	 *  variable for convenience. */
	private SharedPreferences prefs = null;
	
	/**
	 * The UpgradeManager constructor
	 * @param app A reference to the overall application
	 * @param caller The calling Activity
	 */
	public UpgradeManager(CryptnosApplication app, Activity caller)
	{
		theApp = app;
		this.caller = caller;
		prefs = theApp.getPrefs();
	}
	
	/**
	 * Perform the actual upgrade check
	 */
	public void performUpgradeCheck()
	{
		// Asbestos underpants:
		try {
			// Get the old version number from the shared preferences file.
			// Note that if this information has not been saved at all,
			// default to a negative one.
			int oldVersion = prefs.getInt(CryptnosApplication.PREFS_VERSION,
					NO_PREVIOUS_VERSION);
			// Now get the version number of the currently running version.
			// We'll need the Package Manager for this.  This could technically
			// explode on us, which is the primary reason for the try/catch
			// block.
	        PackageInfo info =
	        	theApp.getPackageManager().getPackageInfo(theApp.getPackageName(),
        			PackageManager.GET_META_DATA);
	        int newVersion = info.versionCode;
	        // Now compare the version numbers.  If we're running a newer
	        // version, it's time to perform some upgrade logic.
	        if (oldVersion < newVersion) {
	        	// Get a reference to the shared preferences editor.  We'll
	        	// put this near the top because multiple checks may need
	        	// to add or remove preferences.
	        	SharedPreferences.Editor editor = prefs.edit();
	        	// This section should consist of a series of if statements,
	        	// one after another.  Each if should check for a specific
	        	// version number case where we want to upgrade our data.
	        	// Note that these ifs should not be if/else blocks; they
	        	// should be isolated ifs that check for one case only,
	        	// allowing one if case to flow into the next and thus
	        	// chaining upgrades in a successive fashion.
	        	//
	        	// If we've never had a preferences file before, this can
	        	// happen in two instances: (a) the user just upgraded from a
	        	// version of Cryptnos before we started using preferences, or
	        	// (b) they've never run the application before.  Since the
	        	// second case is the easier to deal with, we'll tackle it
	        	// first.
	        	if (oldVersion <= NO_PREVIOUS_VERSION) {
	        		// First, let's check to see if they have any parameters
	        		// in the database.  If they don't, they probably have
	        		// never run the app before, and in that case all we
	        		// really need to do is set the version number in the
	        		// preferences and make sure to default their text
	        		// encoding to UTF-8.  Note that we need to refresh the
	        		// parameter salt just in case, since it uses the same
	        		// text encoding as well.
	        		if (theApp.getDBHelper().recordCount() == 0) {
		        		theApp.setTextEncoding(CryptnosApplication.TEXT_ENCODING_UTF8);
		        		theApp.refreshParameterSalt();
		        		oldVersion = newVersion;
		        	// If the user has data, then we need to tread lightly.
		        	// They must be upgrading from a version before we started
		        	// using shared preferences, so we need to be careful
		        	// with what we do.
	        		} else {
	        			// Look at the existing value of the system file
	        			// encoding property.  If it's already UTF-8 or if it
	        			// produces similar printable characters, such as US
	        			// ASCII or ISO-8859-1, we're in luck.  In that case,
	        			// conversion to binary *should* be compatible, so
	        			// we'll force these users to be UTF-8, just like
	        			// above.  Note that we're assuming that the data the
	        			// user enters will only be "standard" printable
	        			// characters in the first 128 of the set, which should
	        			// include all things entered on the keyboard.
	        			String defaultEncoding =
	        				System.getProperty("file.encoding", "No Default");
	        			if (regex_utf8.matcher(defaultEncoding).matches()
	        					|| regex_iso8859.matcher(defaultEncoding).matches()
	        					|| regex_ascii.matcher(defaultEncoding).matches()
	        					|| regex_win1252.matcher(defaultEncoding).matches()) {
			        		theApp.setTextEncoding(CryptnosApplication.TEXT_ENCODING_UTF8);
			        		theApp.refreshParameterSalt();
			        		oldVersion = newVersion;
			        	// Otherwise, the default encoding is not binary
			        	// compatible with UTF-8.  Now we need to take this
			        	// issue to the user and let them decide how to
			        	// proceed.
	        			} else {
	        				// Show a warning dialog to ask the user what to do.
	        				// If they decide to change their text encoding, this
	        				// dialog will take them to the advanced settings
	        				// activity to do just that.
	        				caller.showDialog(CryptnosApplication.DIALOG_UPGRADE_TO_UTF8);
	        			}
	        			// Now that that's taken care up, bump the user up
	        			// to Version 1.2, which is where the UTF-8 fiasco
	        			// should get fixed.
	        			oldVersion = VERSION_1_2_0;
	        		}
	        	}
	        	// In version 1.2.4 we introduced a checkbox in the Advanced Settings
	        	// activity to let the user enable and disable the copy passwords to
	        	// the clipboard setting.  Previously, this was an always-on, hard-
	        	// coded thing, so we'll need to add the setting to the preferences
	        	// and default it to true.
	        	if (oldVersion < VERSION_1_2_4) {
	        		editor.putBoolean(CryptnosApplication.PREFS_COPY_TO_CLIPBOARD, true);
	        		oldVersion = VERSION_1_2_4;
	        	}
	        	// In version 1.3.0 we introduce the ability to import and export
	        	// sites via QR codes, which requires a third-party barcode scanning
	        	// app.  To set this up, we'll grab the application's QR code handler
	        	// object and let it find all available scanners that are installed.
	        	// We'll then set the scanner preference to a default value and place
	        	// that into the preferences until the user changes it.
	        	if (oldVersion < VERSION_1_3_0) {
	        		QRCodeHandler qrCodeHandler = theApp.getQRCodeHandler();
	        		editor.putInt(CryptnosApplication.PREFS_QRCODE_SCANNER, 
	        				qrCodeHandler.getPreferredQRCodeApp());
	        		// Also default the "show master passwords" setting to false:
	        		editor.putBoolean(CryptnosApplication.PREFS_SHOW_MASTER_PASSWD, false);
	        		oldVersion = VERSION_1_3_0;
	        	}
	        	// In version 1.3.1, we added the "clear passwords on focus loss"
	        	// preference, which makes Cryptnos clear the master and generated
	        	// password boxes whenever Cryptnos goes into the background (such as
	        	// when the user switches to a different app).  To replicate the old
	        	// behavior before we introduced this, we default this preference to
	        	// off.
	        	if (oldVersion < VERSION_1_3_1) {
	        		editor.putBoolean(CryptnosApplication.PREFS_CLEAR_PASSWDS_ON_FOCUS_LOSS,
	        				false);
	        		oldVersion = VERSION_1_3_1;
	        	}
	        	// Additional version checks should follow here, allowing
	        	// them to chain from check to check:
	        	// if (oldVersion < VERSION_x_y_z) {
	        	// }
	        	// Now that we're done, write the new version number to the
	        	// shared preferences so we'll know next time that we're
	        	// up to date:
	        	editor.putInt(CryptnosApplication.PREFS_VERSION, newVersion);
	        	editor.commit();
	        // If the old version is higher than the new version, the user is
	        // running an old version of Cryptnos using a newer version's
	        // data.  This could happen if they decide to "downgrade" by
	        // uninstalling a new version and reinstalling an old one.  This
	        // is not a state we want to be in, so we're declaring this state
	        // to be unsupported.  Display an error message asking them to
	        // wipe their data and reinstall.
	        } else if (oldVersion > newVersion) {
	        	Toast.makeText(caller,
	        			caller.getResources().getText(R.string.error_upgrader_old_version),
	        			Toast.LENGTH_LONG).show();
	        }
	    // If anything blows up, warn the user:
        } catch (Exception e) {
        	Toast.makeText(caller,
        			caller.getResources().getText(R.string.error_upgrader_exception),
        			Toast.LENGTH_LONG).show();
        }
	}
}
