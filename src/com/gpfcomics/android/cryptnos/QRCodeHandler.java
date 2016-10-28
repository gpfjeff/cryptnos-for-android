/* QRCodeHandler.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          November 4, 2010
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This class implements many of the core features of the new QR code import/export
 * mechanism added in Cryptnos 1.3.0.  Much like the FileManager class, this provides
 * abstraction to allow us to transparently handle multiple third-party barcode
 * scanning apps.  The CryptnosApplication class will have a single shared instance
 * of this class to be instantiated upon first use.  From there, this class will
 * determine the availability of third-party pass, provide common methods for testing
 * for availability, generating intents, and encoding and decoding site parameters
 * into and out of the QR code format.
 * 
 * UPDATES FOR 1.3.1:  Minor tweaks to make Lint happy.
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

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.widget.Toast;

/**
 * This class implements many of the core features of the QR code import/export
 * mechanism.  Much like the FileManager class, this provides abstraction to allow
 * us to transparently handle multiple third-party barcode scanning apps.  The
 * CryptnosApplication class will have a single shared instance of this class to be
 * instantiated upon first use.  From there, this class will determine the
 * availability of third-party pass, provide common methods for testing for
 * availability, generating intents, and encoding and decoding site parameters
 * into and out of the QR code format.
 * @author Jeffrey T. Darlington
 * @version 1.3.1
 * @since 1.3.0
 */
public class QRCodeHandler {
	
	/* Public Constants *******************************************************/
	
	/** A constant which indicates that the user has no preference of QR code scanning
	 *  app and that no app is currently enabled */
	public static final int APP_NONE_SELECTED = 0;
	
	/** This constant identifies ZXing's Barcode Scanner */
	public static final int APP_ZXING = 1;
	
	/** This constant represents the ZXing Barcode Scanner app name */
	public static final String NAME_ZXING = "ZXing Barcode Scanner";
	
	/** This constant identifies QR Droid */
	public static final int APP_QRDROID = 2;
	
	/** This constant represents the QR Droid app name */
	public static final String NAME_QRDROID = "QR Droid";
	
	/** This constant identifies QR Droid Private */
	public static final int APP_QRDROID_PRIVATE = 3;
	
	/** This constant represents the QR Droid Private app name */
	public static final String NAME_QRDROID_PRIVATE = "QR Droid Private";
	
	/** This constant represents the activity request code used by startActivityForResult()
	 *  and onActivityResult() for scanning QR codes to import a set of site parameters. */
	public static final int INTENT_SCAN_QRCODE = 6789;
	
	/** This constant represents the activity request code used by startActivityForResult()
	 *  and onActivityResult() for encoding a set of site parameters into a QR code. */
	public static final int INTENT_ENCODE_QRCODE = INTENT_SCAN_QRCODE + 1;
	
	/* Private Constants ********************************************************/
	
	/** The ZXing Barcode Scanner package */
	private static final String PACKAGE_ZXING = "com.google.zxing.client.android";
	
	/** The QR Droid package */
	private static final String PACKAGE_QRDROID = "la.droid.qr";

	/** The QR Droid Private package */
	private static final String PACKAGE_QRDROID_PRIVATE = "la.droid.qr.priva";

	/** The intent name for scanning codes in ZXing Barcode Scanner */
	private static final String INTENT_SCAN_ZXING = PACKAGE_ZXING + ".SCAN";
	
	/** The intent name for encoding data in ZXing Barcode Scanner */
	private static final String INTENT_ENCODE_ZXING = PACKAGE_ZXING + ".ENCODE";
	
	/** The intent name for scanning codes in QR Droid */
	private static final String INTENT_SCAN_QRDROID = PACKAGE_QRDROID + ".scan";
	
	/** The intent name for encoding data in QR Droid */
	private static final String INTENT_ENCODE_QRDROID = PACKAGE_QRDROID + ".encode";
	
	/** This constant represents the delimiter used to separate parameters from each
	 *  other within the text of the Cryptnos QR code format, as well as separating
	 *  the overall header from the rest of the parameters. */
	private static final String DELIMITER_PARAMS = "|";
	
	/** This constant represents the delimiter that separates an individual parameter's
	 *  header from the parameter value. */
	private static final String DELIMITER_HEADER = ":";
	
	/** The Cryptnos QR code version 1 format header */
	private static final String HEADER_OVERALL_V1 = "CRYPTNOSv1";
	
	/** The site token parameter header */
	private static final String HEADER_SITE = "S";
	
	/** The hash algorithm parameter header */
	private static final String HEADER_HASH = "H";

	/** The iterations parameter header */
	private static final String HEADER_ITERATIONS = "I";

	/** The character types parameter header */
	private static final String HEADER_CHARTYPES = "C";

	/** The character limit parameter header */
	private static final String HEADER_CHARLIMIT = "L";
	
	/** Enable or disable debug Toasts.  This should always be set to false for
	 *  official releases. */
	private static final boolean DEBUG = true;

	/* Private Members **********************************************************/
	
	/** Internal reference for the main Cryptnos app */
	private CryptnosApplication theApp = null;

	/** The user's preferred QR code scanning app */
	private int preferredQRCodeApp = APP_NONE_SELECTED;
	
	/** An integer array containing the internal codes for all QR code scanners
	 *  we could find on the system */
	private int[] availableQRCApps = null;
	
	/* Public methods: ***********************************************************/
	
	/**
	 * Default constructor.
	 * @param theApp A reference to the main Cryptnos application
	 */
	public QRCodeHandler(CryptnosApplication theApp) {
		this(theApp, APP_NONE_SELECTED);
	}
	
	/**
	 * Main constructor
	 * @param theApp A reference to the main Cryptnos application
	 * @param preferredQRCodeApp The user's current preference of barcode scanner.  This
	 * must be one of the barcode scanner constants.  If the specified barcode scanner
	 * is not installed on the system, this value is ignored and the preferred
	 * barcode scanner is set to none.
	 */
	public QRCodeHandler(CryptnosApplication theApp, int preferredQRCodeApp) {
		this.theApp = theApp;
		this.preferredQRCodeApp = preferredQRCodeApp;
		findAvailableQRCodeApps();
	}
	
	/** Searches the system to see if any of the recognized barcode scanners are
	 *  currently installed.  An internal list is maintained of which recognized
	 *  barcode scanners are available.  If the user's preferred barcode scanner is
	 *  not in the list, that setting is reset to none. */
	public void findAvailableQRCodeApps() {
		// I don't really like this, but create an empty ArrayList of Integer
		// objects.  ArrayList doesn't like primitive types, and we can't
		// dynamically build a simple array of unknown size.
		ArrayList<Integer> qrcappList = new ArrayList<Integer>();
		// Get the PackageManager from the app:
		PackageManager pm = theApp.getPackageManager();
		// Step through all our recognized barcode scanner package names and see if
		// they're in the list.  This should be quicker than the other alternative
		// of grabbing *all* packages and cycling through them.  The caveat is that
		// we're using exception throwing to see if the barcode scanner is not available,
		// which in itself is expensive.  If no exception is thrown, we know the
		// package exists.
		//
		// First, ZXing Barcode Scanner:
		try {
			pm.getPackageInfo(PACKAGE_ZXING, 0);
			qrcappList.add(Integer.valueOf(APP_ZXING));
		}
		catch (PackageManager.NameNotFoundException ex1) {}
		// QR Droid:
		try {
			pm.getPackageInfo(PACKAGE_QRDROID, 0);
			qrcappList.add(Integer.valueOf(APP_QRDROID));
		}
		catch (PackageManager.NameNotFoundException ex2) {}
		// QR Droid Private:
		try {
			pm.getPackageInfo(PACKAGE_QRDROID_PRIVATE, 0);
			qrcappList.add(Integer.valueOf(APP_QRDROID_PRIVATE));
		}
		catch (PackageManager.NameNotFoundException ex3) {}
		// Now check the ArrayList's size.  If we got anything at all, we found
		// at least one of them.  Convert the ArrayList to a simple integer array:
		if (qrcappList.size() > 0) {
			availableQRCApps = new int[qrcappList.size()];
			// There is a chance that the user specified a preference in the past
			// but uninstalled the package containing the barcode scanner.  If that's
			// so, we don't want to leave that preference in there.  As we build
			// the simple array, check to see if the user's current preference is
			// still a valid one:
			boolean inThere = false;
			// Now convert and test:
			for (int j = 0; j < availableQRCApps.length; j++) {
				availableQRCApps[j] = qrcappList.get(j).intValue();
				if (preferredQRCodeApp == availableQRCApps[j])
					inThere = true;
			}
			// If there's only one barcode scanner available, we'll force that
			// app to be the preferred one, regardless of any previous preference:
			if (availableQRCApps.length == 1) {
				preferredQRCodeApp = availableQRCApps[0];
				SharedPreferences.Editor editor = theApp.getPrefs().edit();
				editor.putInt(CryptnosApplication.PREFS_QRCODE_SCANNER, preferredQRCodeApp);
				editor.commit();
			}
			// If the user has a preference and it's not in the list but there
			// are other apps available, we'll pick the first app in the list
			// and force it to be the default:
			if (preferredQRCodeApp != APP_NONE_SELECTED && !inThere &&
					availableQRCApps.length > 0) {
				preferredQRCodeApp = availableQRCApps[0];
				SharedPreferences.Editor editor = theApp.getPrefs().edit();
				editor.putInt(CryptnosApplication.PREFS_QRCODE_SCANNER, preferredQRCodeApp);
				editor.commit();
			}
		// If no recognized barcode scanners were found, default the user's preference
		// to no barcode scanner and write that preference to the preference file:
		} else {
			preferredQRCodeApp = APP_NONE_SELECTED;
			availableQRCApps = null;
			SharedPreferences.Editor editor = theApp.getPrefs().edit();
			editor.putInt(CryptnosApplication.PREFS_QRCODE_SCANNER, APP_NONE_SELECTED);
			editor.commit();
		}
	}
	
	/** Get an array of codes representing all recognized barcode scanners found on
	 *  the system.  Note that this may return a null or empty list. */
	public int[] getAvailableQRCodeApps() {
		// Originally, we did the search once and stored the list for the sake of
		// efficiency.  In practice, however, this meant that there was a period of
		// time during which a barcode scanner could be added or removed and the internal
		// list would not be in sync with what's on the system.  Since the search is
		// relatively quick, we'll force this to refresh every time.  We may want to
		// go back to the old method later if this doesn't work in the long term.
		findAvailableQRCodeApps();
		return availableQRCApps;
	}
	
	/** Get an array of strings representing the names of all recognized barcode
	 *  scanners on the system, suitable for display in a drop-down box or
	 *  similar UI element.  If no recognized barcode scanners are found, this
	 *  returns a single-element list with a string stating that no barcode
	 *  scanners were found. */
	public String[] getAvailableQRCodeAppNames() {
		// See getAvailableQRCodeApps() for why we refresh the list every time.
		findAvailableQRCodeApps();
		// Declare a string array to hold our names:
		String[] names = null;
		// If we found any file managers at all, start building the array of names.
		// Step through the available names and add them to the list.
		if (availableQRCApps != null && availableQRCApps.length > 0) {
			names = new String[availableQRCApps.length];
			for (int i = 0; i < names.length; i++)
				names[i] = mapCodeToName(availableQRCApps[i]);
		// If no barcode scanners could be found, return a single-element array with
		// a message stating as such:
		} else {
			names = new String[1];
			names[0] = theApp.getBaseContext().getString(R.string.error_no_qrscanners_found);
		}
		return names;
	}
	
	/** Get the user's preferred barcode scanner's code */
	public int getPreferredQRCodeApp() {
		return preferredQRCodeApp;
	}
	
	/** Get the user's preferred barcode scanner's display name*/
	public String getPreferredQRCodeAppName() {
		return mapCodeToName(preferredQRCodeApp);
	}
	
	/**
	 * Set the user's preferred barcode scanner by specifying its code
	 * @param code The barcode scanner code.  Must be one of the public code constants
	 * available to this class.
	 */
	public void setPreferredQRCodeApp(int code) {
		// We only want the user to be able to set the preferred barcode scanner if
		// the choice is a valid one.  No barcode scanner is always valid; however,
		// if the code is anything else, make sure it's in the list of available
		// barcode scanners before adding it.
		if (code == APP_NONE_SELECTED ||
				((code == APP_ZXING || code == APP_QRDROID || code == APP_QRDROID_PRIVATE) &&
				codeInAvailableList(code))) {
			// Store the preference, first locally then in the shared preferences:
			preferredQRCodeApp = code;
			SharedPreferences.Editor editor = theApp.getPrefs().edit();
			editor.putInt(CryptnosApplication.PREFS_QRCODE_SCANNER, code);
			editor.commit();
		}
	}
	
	/**
	 * Set the user's preferred barcode scanner by specifying its name
	 * @param name The barcode scanner name.  Must be a string as output by
	 * getAvailableQRCodeAppNames()
	 */
	public void setPreferredQRCodeApp(String name) {
		// No sense reinventing the wheel.  Convert the string name to the right
		// code (if possible) and reuse the code above:
		setPreferredQRCodeApp(mapNameToCode(name));
	}
	
	/** Generate a new Intent suitable for passing to startActivityForResult() to
	 *  scan a QR code using the user's preferred barcode scanner.  If the user has
	 *  no preference of barcode scanner, this returns null.
	 *  @return The Intent to pass to startActivityForResult()
	 */
	@TargetApi(4)
	public Intent generateScanIntent() {
		// This is pointless if we can't handle QR codes:
		if (canScanQRCodes()) {
			// Create a null intent:
			Intent intent = null;
			// Switch on the default barcode scanner:
			switch (preferredQRCodeApp) {
				// ZXing Barcode Scanner:
				case APP_ZXING:
					intent = new Intent(INTENT_SCAN_ZXING);
					// Whoops... Intent.setPackage() is only used in Donut and later.
					// We have to conditionally include this step.
					if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.DONUT)
						intent.setPackage(PACKAGE_ZXING);
					intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
					// Note that we don't want Barcode Scanner to save
					// this in its scan history:
					intent.putExtra("SAVE_HISTORY", false);
					break;
				// QR Droid:
				case APP_QRDROID:
				case APP_QRDROID_PRIVATE:
					intent = new Intent(INTENT_SCAN_QRDROID);
					intent.putExtra(PACKAGE_QRDROID + ".complete", true);
					break;
				// If we didn't get a recognized scanner, the default here
				// will return a null intent, indicating an error:
				default:
					break;
			}
			// Return the intent:
			return intent;
		// There are no scanners available:
		} else return null;
	}
	
	/**
	 * Generate a new Intent suitable for passing to startActivityForResult() to
	 * encode a set of site parameters as a QR code using the users preferred barcode
	 * scanner.  If the user has no preference of barcode scanner, this returns null.
	 * @param siteParams The site parameters to encode
	 * @return The Intent to pass to startActivityForResult()
	 */
	@TargetApi(4)
	public Intent generateEncodeIntent(SiteParameters siteParams) {
		// Asbestos underpants:
		try {
			// If we didn't get any site parameters, there's no point continuing:
			if (siteParams == null) return null;
			// Make sure we can encode QR codes:
			if (canGenerateQRCodes()) {
				// Convert the site parameters to a string which we'll send to
				// the encoder.  This is a very specific format.
				String encodedParams = HEADER_OVERALL_V1 + DELIMITER_PARAMS +
					HEADER_SITE + DELIMITER_HEADER + siteParams.getSite() + DELIMITER_PARAMS +
					HEADER_HASH + DELIMITER_HEADER + siteParams.getHash() + DELIMITER_PARAMS +
					HEADER_ITERATIONS + DELIMITER_HEADER + String.valueOf(siteParams.getIterations()) + DELIMITER_PARAMS +
					HEADER_CHARTYPES + DELIMITER_HEADER + String.valueOf(siteParams.getCharTypes()) + DELIMITER_PARAMS +
					HEADER_CHARLIMIT + DELIMITER_HEADER + String.valueOf(siteParams.getCharLimit());
				// Create a null intent:
				Intent intent = null;
				// Switch based on our scanner preference:
				switch (preferredQRCodeApp) {
					// ZXing Barcode Scanner:
					case APP_ZXING:
						intent = new Intent(INTENT_ENCODE_ZXING);
						// Whoops... Intent.setPackage() is only used in Donut and later.
						// We have to conditionally include this step.
						if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.DONUT)
							intent.setPackage(PACKAGE_ZXING);
						intent.putExtra("ENCODE_DATA", encodedParams);
						intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
						// Don't echo back the text of the code beneath it:
						intent.putExtra("ENCODE_SHOW_CONTENTS", false);
						// ZXing defaults to QR codes.  I'd prefer to explicitly declare
						// this, but I can't find the right format text to put here.  The
						// comments say to use com.google.zxing.BarcodeFormat which is an
						// enum, but that doesn't help us when we're doing this by intent.
						//intent.putExtra("ENCODE_FORMAT", "QR_CODE");
						break;
					// QR Droid:
					case APP_QRDROID:
					case APP_QRDROID_PRIVATE:
						// Note that this is how we generate the intent, but I'm a bit
						// concerned on what to do next.  The intent generates the code
						// and returns a URL (to Google or to a local file) to the file
						// of the generated image.  This is different from ZXing, which
						// displays the code itself.  I'm not sure how we'll handle this
						// but this at least gets us started.
						intent = new Intent(INTENT_ENCODE_QRDROID);
						intent.putExtra(PACKAGE_QRDROID + ".code", encodedParams);
						// This tells QR Droid to store the file on external storage and
						// give us the file name rather than giving us the URL to the
						// Google Chart API that generated it:
						intent.putExtra(PACKAGE_QRDROID + ".image", true);
						break;
					// If we don't recognize the preference, this wil force us to
					// return null:
					default:
						break;
				}
				// Return the intent:
				return intent;
			// If we can't encode, return a null as an error:
			} else return null;
		// If anything blew up, return an error:
		} catch (Exception e) { return null; }
	}
	
	/**
	 * Determine whether or not the QR code scan was successful so we can proceed
	 * to the next step.  Since different QR code scanners require different tests
	 * for success, this method encapsulates that logic in one place.
	 * @param resultCode One of the Activity result codes are passed into
	 * Activity.onActivityResult()
	 * @param data An Intent containing the data returned from the scan.
	 * @return True if the scan was successful, false otherwise.
	 */
	public boolean wasScanSuccessful(int resultCode, Intent data) {
		// Which app did we scan with?
		switch (preferredQRCodeApp) {
			// ZXing Barcode Scanner:
			case APP_ZXING:
				return resultCode == Activity.RESULT_OK && data != null;
				// QR Droid:
			case APP_QRDROID:
			case APP_QRDROID_PRIVATE:
				return data != null && data.getExtras() != null;
		}
		return false;
	}
	
	/**
	 * Determine whether or not the QR code generation was successful so we can proceed
	 * to the next step.  Since different QR code scanners require different tests
	 * for success, this method encapsulates that logic in one place.
	 * @param resultCode One of the Activity result codes are passed into
	 * Activity.onActivityResult()
	 * @param data An Intent containing the data returned from the app.
	 * @return True if the generate was successful, false otherwise.
	 */
	public boolean wasGenerateSuccessful(int resultCode, Intent data) {
		// Right now, only QR Droid needs to be tested:
		switch (preferredQRCodeApp) {
			case APP_QRDROID:
			case APP_QRDROID_PRIVATE:
				return data != null && data.getExtras() != null;
		}
		return false;
	}
	
	/**
	 * Given an Intent returned from a barcode scanning app, try to extract the encoded
	 * site parameters and return a SiteParameters object
	 * @param data The Intent containing the decoded data
	 * @return The decoded site parameters.  This returns null if no valid data
	 * could be found.
	 */
	public SiteParameters getSiteParamsFromScan(Intent data) {
		// Asbestos underpants:
		try {
			// If the input intent is empty, there's no point going forward:
			if (data == null) return null;
			// Declare a string to hold our decoded data:
			String decodedData = null;
			// Based on the preferred scanner app, try to get the decoded string
			// from the intent result:
			switch (preferredQRCodeApp) {
				// ZXing Barcode Scanner:
				case APP_ZXING:
					decodedData = data.getStringExtra("SCAN_RESULT");
					break;
				// QR Droid:
				case APP_QRDROID:
				case APP_QRDROID_PRIVATE:
					decodedData = data.getExtras().getString(PACKAGE_QRDROID + ".result");
					break;
				// If no scanner app is selected, don't do anything.  The string
				// is already null and we'll use that as a check below.
				default:
					break;
			}
			// DEBUG: ****************************************************************
			if (DEBUG) {
				if (decodedData == null)
					Toast.makeText(theApp.getBaseContext(), "Scanned: {NULL}",
							Toast.LENGTH_LONG).show();
				else
					Toast.makeText(theApp.getBaseContext(), "Scanned: " + decodedData,
							Toast.LENGTH_LONG).show();
			}
			// DEBUG: ****************************************************************
			// Did we get anything useful:
			if (decodedData != null) {
				// Begin further decoding the string into its relevant bits.  First,
				// we'll try splitting the string on the parameter delimiter.  If
				// that works and we get the right number of parts, we'll continue.
				// Otherwise, we'll bomb out here and return a null.  Note that if
				// we ever change the format of this string, we may need to reorder
				// or adjust this check.
				String[] bits = decodedData.split("\\" + DELIMITER_PARAMS);
				if (bits.length != 6) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: bits.length = " +
							String.valueOf(bits.length),
							Toast.LENGTH_LONG).show();
					return null;
				}
				// Check the first item in the list and make sure it matches the
				// header all our QR codes are supposed to use.  If not, fail:
				if (bits[0].compareTo(HEADER_OVERALL_V1) != 0) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Wrong header (\"" +
							bits[0] + "\")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				// If we got this far, good.  We'll start off creating a new
				// SiteParamemters object that we'll populate below.
				SiteParameters params = new SiteParameters(theApp);
				// Now we'll start decoding the individual elements.  We'll start
				// with the site token.  Split the string on the header delimiter
				// and make sure we have the right number of parts.  Then check
				// the header and make sure it's OK.  If those pass, record the
				// site token and move on.  Otherwise, fail.  Note that all the
				// other parameters will proceed in the same fashion.
				String[] param = bits[1].split(DELIMITER_HEADER);
				// JTD 10/27/2016:  Fix for Issue #19, part one.  We've had some
				// users who have put colons in their site token; specifically, they
				// were using a website URL, presumably with the protocal included
				// ("http://...").  Unfortunately, we're using colons as a delimiter
				// between headers and values, and we didn't explicitly forbid them
				// from the site text box.  So when we tried to split the value
				// above, we ended up with more than two parts.  It's fine for us
				// to throw an error on the other header/value pairs if they end up
				// with more tha two parts, but not here.  So we'll change this
				// test to throw an error if we get less than two parts after the
				// split, then adjust things below before returning our results.
				if (param.length < 2) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Site param has wrong " +
							"number of items (" + String.valueOf(param.length) + ")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				if (param[0].compareTo(HEADER_SITE) != 0) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Site param has wrong " +
							"header (\"" + param[0] + "\")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				// JTD 10/27/2016:  Fix for Issue #19, part two.  Now that we've
				// validated that we got a good header, we need to set the site token.
				// This is easy if we only have one non-header part.  However, if the
				// site token contained colons, we could end up with multiple parts
				// that we'll have to reassemble.  So declare a String variable to
				// hold the site token, assign the first part to it, then check to see
				// if we have more parts to add.  If we do, loop through those parts
				// and glue them back together, making sure to restore the colons
				// along the way.  This would probably be more efficient if we used
				// a StringBuilder rather than a bunch of concatenates, but I don't
				// think we'll be doing a lot of this.
				String site = param[1];
				if (param.length > 2) {
					// Combine remaining strings into one and stuff into site
					for (int i = 2; i < param.length; i++) {
						site = site.concat(DELIMITER_HEADER).concat(param[i]);
					}
				}
				params.setSite(site);
				// Get the hash algorithm:
				param = bits[2].split(DELIMITER_HEADER);
				if (param.length != 2) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Hash param has wrong " +
							"number of items (" + String.valueOf(param.length) + ")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				if (param[0].compareTo(HEADER_HASH) != 0) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Hash param has wrong " +
							"header (\"" + param[0] + "\")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				params.setHash(param[1]);
				// Get the iterations:
				param = bits[3].split(DELIMITER_HEADER);
				if (param.length != 2) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Iterations param has wrong " +
							"number of items (" + String.valueOf(param.length) + ")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				if (param[0].compareTo(HEADER_ITERATIONS) != 0) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Iterations param has wrong " +
							"header (\"" + param[0] + "\")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				params.setIterations(Integer.parseInt(param[1]));
				// Get the character types:
				param = bits[4].split(DELIMITER_HEADER);
				if (param.length != 2) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Char types param has wrong " +
							"number of items (" + String.valueOf(param.length) + ")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				if (param[0].compareTo(HEADER_CHARTYPES) != 0) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Char types param has wrong " +
							"header (\"" + param[0] + "\")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				params.setCharTypes(Integer.parseInt(param[1]));
				// Get the character limit:
				param = bits[5].split(DELIMITER_HEADER);
				if (param.length != 2) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Char limit param has wrong " +
							"number of items (" + String.valueOf(param.length) + ")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				if (param[0].compareTo(HEADER_CHARLIMIT) != 0) {
					if (DEBUG)
						Toast.makeText(theApp.getBaseContext(), "ERROR: Char limit param has wrong " +
							"header (\"" + param[0] + "\")",
							Toast.LENGTH_LONG).show();
					return null;
				}
				params.setCharLimit(Integer.parseInt(param[1]));
				// If we got this far, the parameters should have been decoded
				// successfully.  Return the site parameters to the caller.
				return params;
			// If the string wasn't populated, return a null to indicate an error:
			} else return null;
		// If we blew up anywhere, return null as an error:
		} catch (Exception e) {
			if (DEBUG)
				Toast.makeText(theApp.getBaseContext(), "ERROR: Exception: " +
					e.getMessage(),	Toast.LENGTH_LONG).show();
			return null;
		}
	}
	
	/**
	 * Given an Intent returned from a barcode scanning app, try to extract the path
	 * to the file on the external storage generated by the app so we can display it
	 * in the QRViewActivity
	 * @param data The Intent containing the path to the file
	 * @return The decoded file path.  This returns null if no valid data
	 * could be found.
	 */
	public String getPathToGeneratedQRCodeFile(Intent data) {
		// Asbestos underpants:
		try {
			// If the intent is empty, return null:
			if (data == null) return null;
			// Which app were we using again?
			switch (preferredQRCodeApp) {
				// QR Droid:
				case APP_QRDROID:
				case APP_QRDROID_PRIVATE:
					return data.getExtras().getString(PACKAGE_QRDROID + ".result");
				// Everything else, return a null;
				default:
					return null;
			}
		// If anything blew up, return a null:
		} catch (Exception e) { return null; }
	}
	
	/**
	 * Check to see if we can scan QR codes.
	 * @return True if a barcode scanner app is available for scanning QR codes, false
	 * otherwise
	 */
	public boolean canScanQRCodes() {
		// See getAvailableQRCodeApps()() for why we refresh the list every time.
		findAvailableQRCodeApps();
		// At the moment, all apps can scan codes if the app is availabe, so return
		// true if any apps were found:
		return availableQRCApps != null;
	}
	
	/**
	 * Check to see if we can generate QR codes.
	 * @return True if a barcode scanner app is available to generate QR codes,
	 * false otherwise.
	 */
	public boolean canGenerateQRCodes() {
		// See getAvailableQRCodeApps()() for why we refresh the list every time.
		findAvailableQRCodeApps();
		// If we have apps available:
		if (availableQRCApps != null) {
			// The answer here is, unfortunately, app dependent:
			switch (preferredQRCodeApp) {
				// ZXing displays QR codes for us, so this is always true:
				case APP_ZXING:
					return true;
				// QR Droid must write to external storage, so if that's not
				// available, we can't generate QR codes:
				case APP_QRDROID:
				case APP_QRDROID_PRIVATE:
					return theApp.canWriteToExternalStorage();
				// For anything else, return false:
				default:
					return false;
			}
		// If no apps are available, obviously we can't generate QR codes:
		} else return false;
	}
	
	/**
	 * Check to see if we can scan or encode QR codes.  This is a convenience method
	 * that reduces a number of steps to a single boolean check suitable for use
	 * elsewhere.
	 * @return True if a barcode scanner app is available for handling QR codes, false
	 * otherwise.
	 */
	public boolean canHandleQRCodes() {
		// For now, this is just an alias to canScanQRCodes(), which does essentially
		// the same thing.  However, we want something generic that says "if we can
		// scan OR generate QR codes" rather than "AND".  If the scan check ever
		// becomes dependent on functionality like the generate test, we may need to
		// tweak this.
		return canScanQRCodes(); // || canGenerateQRCodes();
	}
	
	/**
	 * Check to see whether we need our own internal QR code viewing activity
	 * or whether the preferred scanning app will handle that for us
	 * @return True if we need to use our internal viewer, false if the preferred
	 * app will display the generated QR code itself
	 */
	public boolean needQRViewActivity() {
		// For now, only QR Droid requires us to display the code ourselves:
		if (preferredQRCodeApp == APP_QRDROID || preferredQRCodeApp == APP_QRDROID_PRIVATE)
			return true;
		else return false;
	}
	
	/**
	 * Determine whether or not the user has an active preference of barcode scanner.
	 * Note that if the user previously set a preference and that barcode scanner is
	 * subsequently uninstalled, this will automatically return false and the
	 * user's preference will be lost; the FileManager class will revert to no
	 * barcode scanner having been selected. 
	 * @return Returns true or false
	 */
	public boolean isQRCodeAppSelected() {
		// See getAvailableQRCodeApps()() for why we refresh the list every time.
		findAvailableQRCodeApps();
		// There is a chance that the user set a preferred scanner but then later
		// uninstalled it.  Thus, we can't just rely on a simple check to see
		// if the preferred scanner isn't the "no barcode scanner" selection.  So first
		// we'll check to see if the preferred scanner is actually available, *then*
		// we'll make the check.
		if (codeInAvailableList(preferredQRCodeApp))
			return preferredQRCodeApp != APP_NONE_SELECTED;
		// If we couldn't find the preferred scanner in the available list, force
		// the preference back to nothing selected and return false.  This will
		// revert us back to the default behavior.
		else {
			setPreferredQRCodeApp(APP_NONE_SELECTED);
			return false;
		}
	}
	
	/**
	 * Return a string containing a list of recognized barcode scanner names.  This
	 * is intended to be displayed in the Advanced Settings activity to show the
	 * user what scanners they can install and use with Cryptnos
	 * @return A String containing the recognized scanner names
	 */
	public String getRecognizedQRScannerNames() {
		return "\t" + NAME_ZXING + "\n\t" +
			NAME_QRDROID + "\n\t" + NAME_QRDROID_PRIVATE;
	}


	/* Private methods: ***********************************************************/
	
	/**
	 * Map the given code to a string suitable for display
	 * @param code The code.  Must be one of the public barcode scanner code values.
	 * @throws IllegalArgumentException Thrown if the supplied code is not
	 * recognized
	 */
	private String mapCodeToName(int code) {
		Resources res = theApp.getBaseContext().getResources();
		// Simple enough:  Switch on code and return a string.  If it's a code
		// we don't recognize, throw an exception:
		switch (code) {
			case APP_NONE_SELECTED:
				return res.getString(R.string.error_no_qrscanner_selected);
			case APP_ZXING:
				return NAME_ZXING;
			case APP_QRDROID:
				return NAME_QRDROID;
			case APP_QRDROID_PRIVATE:
				return NAME_QRDROID_PRIVATE;
			default:
				throw new IllegalArgumentException(res.getString(R.string.error_invalid_qrscanner_code));
		}
	}
	
	/**
	 * Map the given display name to the appropriate code constant
	 * @param name The display name of the barcode scanner
	 * @return The barcode scanner's code, or NO_QRCODE_APP if any error occurs
	 */
	private int mapNameToCode(String name) {
		Resources res = theApp.getBaseContext().getResources();
		// This is the inverse of mapCodeToName(), but a bit more forgiving.
		// Compare the name to the recognized strings and return the appropriate
		// code.  If the name isn't recognize, default back to no file manager
		// preference.
		if (name.compareTo(res.getString(R.string.error_no_qrscanners_found)) == 0)
			return APP_NONE_SELECTED;
		if (name.compareTo(res.getString(R.string.error_no_qrscanner_selected)) == 0)
			return APP_NONE_SELECTED;
		if (name.compareTo(NAME_ZXING) == 0)
			return APP_ZXING;
		if (name.compareTo(NAME_QRDROID) == 0)
			return APP_QRDROID;
		if (name.compareTo(NAME_QRDROID_PRIVATE) == 0)
			return APP_QRDROID_PRIVATE;
		return APP_NONE_SELECTED;
	}
	
	/**
	 * Check to see if the specified barcode scanner code is in the available barcode
	 * scanner list
	 * @param code The barcode scanner code to test.  Must be one of the barcode
	 * scanner code constants.
	 * @return True if the barcode scanner is available, false otherwise
	 */
	private boolean codeInAvailableList(int code) {
		// This is a bit of a kludge, but always return true if we happen to
		// get passed in the "no barcode scanner selected" code.  After all, no
		// selection at all is technically a valid one.
		if (code == APP_NONE_SELECTED) return true;
		// Otherwise, step through the available list and return true if we can
		// find the code.  I wish there were a more efficient way to do this, but
		// fortunately our lists should be small.
		if (availableQRCApps != null & availableQRCApps.length > 0) {
			for (int i = 0; i < availableQRCApps.length; i++)
				if (code == availableQRCApps[i]) return true;
		}
		return false;
	}

}
