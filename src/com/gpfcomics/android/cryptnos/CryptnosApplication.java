/* CryptnosApplication.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          March 30, 2010
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * The core Application class for the Cryptnos program.  The primary purpose
 * of this class is to handle the loading and refreshing of the site list
 * for dependent Activities.  The Cryptnos site list is stored in the database
 * as encrypted text and must be decrypted before it can be used.  Originally,
 * each Activity loaded the site list on its own, which created a lot of
 * duplicated code and a lot of extra load times.  The original theory behind
 * this was security, but the caveats eventually outweighed the benefits.
 * 
 * Now each Activity that wants access to the site list can simply reference
 * this central class and implement the SiteListListener interface.  The site
 * requests the site list from the application core.  If the site list has
 * already been built, it is instantly returned, saving a lot of time and work.
 * Otherwise, this class does the grunt work of loading the sites from the
 * database, decrypting them, and storing them in a String array.  Once this
 * is complete, the class contacts the SiteListListener and passes it the array
 * to do with as it pleases.  If at any time an Activity modifies the database
 * (such as adding, deleting, or modifying a set of site parameters), it should
 * mark the list as "dirty", forcing it to be reloaded the next time it is
 * requested.
 * 
 * This class also stores a few common "global" variables used throughout the
 * application.  Those related to encryption are used in the reading and
 * writing of parameter data to the database, as well as reading the old
 * platform-specific export format.  The encryption constants for the new
 * cross-platform export format are stored in the ImportExportHandler class.
 * 
 * UPDATES FOR 1.1:  Added isIntentAvailable() the let activities test to see
 * if a specified Intent is available on the system.
 * 
 * UPDATES FOR 1.1.1:  Now uses AndroidID wrapper class to get proper version
 * of ANDROID_ID for the API level we're running under.  Moved hard-coded
 * salt-salt to a constant (SALTIER_SALT) so it can be updated in one place if
 * required.  Added new text encoding constants and methods to (hopefully)
 * fix Issue #2 ("New Phone, Weird Passwords").
 * 
 * This program is Copyright 2010, Jeffrey T. Darlington.
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

/**
 * The core Application class for the Cryptnos program.  The primary purpose
 * of this class is to handle the loading and refreshing of the site list
 * for dependent Activities.  The Cryptnos site list is stored in the database
 * as encrypted text and must be decrypted before it can be used.  Originally,
 * each Activity loaded the site list on its own, which created a lot of
 * duplicated code and a lot of extra load times.  The original theory behind
 * this was security, but the caveats eventually outweighed the benefits.
 * 
 * Now each Activity that wants access to the site list can simply reference
 * this central class and implement the SiteListListener interface.  The site
 * requests the site list from the application core.  If the site list has
 * already been built, it is instantly returned, saving a lot of time and work.
 * Otherwise, this class does the grunt work of loading the sites from the
 * database, decrypting them, and storing them in a String array.  Once this
 * is complete, the class contacts the SiteListListener and passes it the array
 * to do with as it pleases.  If at any time an Activity modifies the database
 * (such as adding, deleting, or modifying a set of site parameters), it should
 * mark the list as "dirty", forcing it to be reloaded the next time it is
 * requested.
 * @author Jeffrey T. Darlington
 * @version 1.1
 * @since 1.0
 */
public class CryptnosApplication extends Application {

	/** A constant identifying the progress dialog used during the site
	 *  list load.  Activities implementing the SiteListListener interface
	 *  will to use this for their showDialog()/onCreateDialog() methods. */
	public static final int DIALOG_PROGRESS = 5000;
	/** A constant identifying the warning dialog displayed if the user
	 *  upgrades Cryptnos from an old version to 1.2.0, where we try to enforce
	 *  UTF-8 encoding. */
	public static final int DIALOG_UPGRADE_TO_UTF8 = 5001;
	/** This integer constant lets us define a limit beyond which cryptographic
	 *  hash generation seems to be excessive.  Anything below this should be
	 *  fine and fairly quick; anything above this may cause the application
	 *  to appear sluggish or non-responsive.  At one point, I considered
	 *  putting password generation with iterations higher than this behind a
	 *  ProgressDialog/Thread model, but for now we're using this as an upper
	 *  limit on the number of iterations the user can choose from.  There's
	 *  no science behind this number aside from casual testing, both in the
	 *  SDK emulator and on my personal Motorola Droid.  If there was a
	 *  significant pause observed, that's were I set the limit. */
	public final int HASH_ITERATION_WARNING_LIMIT = 500;
	/** The cryptographic key factory definition.  This will be used by most
	 *  cryptography functions throughout the application (with the exception
	 *  being the new cross-platform import/export format).  Note that this
	 *  will be a "password-based encryption" (PBE) cipher (specifically 
	 *  256-bit AES as of this writing), so take that into account when
	 *  using this value. */
	public static final String KEY_FACTORY = "PBEWITHSHA-256AND256BITAES-CBC-BC";
	/** The number of iterations used for cryptographic key generation, such
	 *  as in creating an AlgorithmParameterSpec.  Ideally, this should be
	 *  fairly high, but we'll use a modest value for performance. */
	public static final int KEY_ITERATION_COUNT = 50;
	/** The length of generated encryption keys.  This will be used for
	 *  generating encryption key specs. */
	public static final int KEY_LENGTH = 32;
	/** Our encryption "salt" for site parameter data.  This value should be
	 *  unique per device and will be used repeatedly, so generating it once
	 *  and storing it in a single place has significant advantages.  Note
	 *  that this is only really used for storing parameters in the database;
	 *  import/export operations will use a salt generated from the user's
	 *  password, which is not device dependent. */
	public static byte[] PARAMETER_SALT = null;
	/** The cryptographic hash to use to generate encryption salts.  Pass this
	 *  into MessageDigest.getInstance() to get the MessageDigest for salt
	 *  generation. */
	public static final String SALT_HASH = "SHA-512";
	/** The number of iterations used for salt generation.  All salts will be
	 *  run through the cryptographic hash specified by
	 *  CryptnosApplication.SALT_HASH this many times  before actually being
	 *  used. */
	public static final int SALT_ITERATION_COUNT = 10;
	/** The identifying string for UTF-8 text encoding. */
	public static final String TEXT_ENCODING_UTF8 = "UTF-8";
	/** The ID string for text encoding within the shared preferences file */
	public static final String PREFS_TEXT_ENCODING = "TEXT_ENCODING";
	/** The ID string for our version number within the shared preferences
	 *  file. */
	public static final String PREFS_VERSION = "VERSION";
	
	/** The actual site list array */
	private static String[] siteList = null;
	/** A boolean flag indicating whether the site list is "dirty" and needs
	 *  to be rebuilt */
	private static boolean isDirty = true;
	/** A File representing the root of all import/export activities.  Files
	 *  will only be written or read from this path. */
	private static File importExportRoot = null;
	/** A common SharedPreferences object that can be used by all Cryptnos
	 *  Activities.  Use getPrefs() below to access it. */
	private static SharedPreferences prefs = null;
	/** The user's preference of text encoding.  The default will be the
	 *  system default, but ideally we want this to be UTF-8.  This value
	 *  will be stored in the Cryptnos shared preferences.  This can be read
	 *  and written to by getTextEncoding() and setTextEncoding()
	 *  respectively. */
	private static String textEncoding = "UTF8";
	/** A boolean flag to indicate whether or not the UpgradeManager has run for
	 *  this particular session. */
	private static boolean upgradeManagerRan = false;

	/** The calling activity, so we can refer back to it.  This is usually the
	 *  same as the site list listener, but doesn't necessarily have to be. */
	private Activity caller = null;
	/** A special listener class that wants to know when the site list is
	 *  ready to be used.  This is usually the same as the caller Activity,
	 *  but doesn't necessarily have to be. */
	private SiteListListener listener = null;
	/** The parameter DB adapter.  Activities should call getDBHelper() to
	 *  get a reference to this object instead of creating their own new
	 *  database objects. */
	private static ParamsDbAdapter DBHelper = null;
	/** A ProgressDialog, which will be attached to the caller Activity but
	 *  which we'll directly control. */
	private static ProgressDialog progressDialog = null;
	/** A ListBuilderThread, which does the grunt work of building the list */
	private ListBuilderThread listBuilderThread = null;
	/** A random-ish string for salting ANDROID_ID.  If the device has never
	 *  been to the Market and never been assigned a unique ANDROID_ID (for
	 *  example, all of the emulators), ANDROID_ID will be null.  We will use
	 *  this string instead if ANDROID_ID is null, or concatenate ANDROID_ID
	 *  with this if it's present.  This string was generated by a little
	 *  script I have that pulls random data out of OpenSSL on Linux, so
	 *  hopefully it's random enough.  Of course, if we're using this all by
	 *  itself, PARAMETER_SALT will *NOT* be unique per device, but that's the
	 *  best we can do.*/
	private static final String SALTIER_SALT = "KnVcUpHHAB5K9HW2Vbq8D9CAk2P7sGiwhQLPeF6wI3UVSCTpJioStD4NFcrR1";

	@Override
	public void onCreate()
	{
		// Do whatever the super needs to do:
		super.onCreate();
		// Open the database using the DB adaptor so we can access the
		// database:
		DBHelper = new ParamsDbAdapter(this);
		DBHelper.open();
        // Set the root for all import/export activites.  For now, we'll
		// hard code this to the root directory of the external storage
		// device, usually an SD card.  We may change this in the future.
        importExportRoot = Environment.getExternalStorageDirectory();
		// Get the shared preferences:
		prefs = getSharedPreferences("CryptnosPrefs", Context.MODE_PRIVATE);
		// Set the text encoding.  Ideally, we will get this from the shared
		// preferences, but if it doesn't exist, try to get the default value.
		// We'll try to get the system "file.encoding" value if we can, but
		// fall back on UTF-8 as a last resort.  The try/catch is because
		// System.getProperty() can technically crash on us, but it's probably
		// not very likely.  Note that whatever we get from wherever we get
		// it, we'll write it back to the preferences; this is so the
		// preference gets written at least on the first instance.
		try
		{
			textEncoding = prefs.getString(PREFS_TEXT_ENCODING,
					System.getProperty("file.encoding", TEXT_ENCODING_UTF8));			
		}
		catch (Exception e) {
			textEncoding = prefs.getString(PREFS_TEXT_ENCODING,
					TEXT_ENCODING_UTF8);	
		}
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PREFS_TEXT_ENCODING, textEncoding);
		editor.commit();
		// Generate the parameter salt:
		refreshParameterSalt();
	}
	
	@Override
	public void onLowMemory()
	{
		// If we ever start running low on memory, clear out the site list
		// and mark it as "dirty".  This frees up memory and forces us to
		// rebuild the list again when it's needed.
		siteList = null;
		isDirty = true;
		// Clear out the salt as well:
		super.onLowMemory();
	}
	
	@Override
	public void onTerminate()
	{
		// If we're closing up shop, there's no need to keep the site list
		// around; it could even be a security risk.  Clear out the site list
		// and mark it as "dirty".  This frees up memory and forces us to
		// rebuild the list again when it's needed.
		siteList = null;
		isDirty = true;
		// Then let the system do whatever else it needs to do:
		super.onTerminate();
	}
	
	/**
	 * Regenerate the encryption salt for site parameter data.
	 */
	public void refreshParameterSalt()
	{
        // Generate the encryption salt for site parameter data.  Originally,
        // this was done every time a SiteParamemter object was used, but
        // that's really wasteful.  Instead, I've moved it to the application
        // level so it can be easily reused.  By default, we'll use the
        // device's unique ID string, so each salt will be different from
        // device to device.  To avoid the whole deprecation issue surrounding
        // Settings.System.ANDROID_ID vs. Settings.Secure.ANDROID_ID, we now
        // wrap the call to this property inside the AndroidID class.  See
        // that class for more details.
        String uniqueID = null;
        try {
        	AndroidID id = AndroidID.newInstance(this);
        	uniqueID = id.getAndroidID();
        } catch (Exception e1) { }
        // Check the unique ID we just fetched.  If we didn't get anything,
        // we'll just make up a hard-coded random-ish string and use that as
        // our starting point.  Of course, if we're using this, our salt will
        // *NOT* be unique per device, but that's the best we can do.
    	if (uniqueID == null) uniqueID = SALTIER_SALT;
    	// If we *did* get a unique ID above, go ahead and concatenate our
    	// OpenSSL string on to the end of it as well.  That should give us
    	// a salt for our salt.
    	else uniqueID = uniqueID.concat(SALTIER_SALT);
        // Now get the unique ID string as raw bytes.  Try to do this with the
    	// common text encoding if possible, but fall back on the system
    	// default if that bombs.
    	try {
    		PARAMETER_SALT = uniqueID.getBytes(textEncoding);
    	} catch (Exception e) {
    		PARAMETER_SALT = uniqueID.getBytes();
    	}
        // Ideally, we don't want to use the raw ID by itself; that's too
        // easy to guess.  Rather, let's hash this a few times to give us
        // something less predictable.  Note that if the hashing fails for
        // any reason, we'll still fall back to the raw ID string's bytes.
		try {
			MessageDigest hasher = MessageDigest.getInstance(SALT_HASH);
			for (int i = 0; i < SALT_ITERATION_COUNT; i++)
				PARAMETER_SALT = hasher.digest(PARAMETER_SALT);
		} catch (Exception e) {}
	}
	
	/**
	 * Request the current site list.  The site will be returned to the
	 * SiteListListener specified by call its onSiteListReady() method.
	 * @param caller The Activity requesting the site list.  This may be the
	 * same as the SiteListListener, but it doesn't have to be.
	 * @param listener The SiteListListener that will receive the list when
	 * it's ready.  This may be the same as the calling Activity, but it
	 * doesn't have to be.
	 */
	public void requestSiteList(Activity caller,
			SiteListListener listener) {
		// First, let's short circuit things if we can.  If the list has
		// already been built and it's not "dirty", go ahead an call the
		// listener and give it the list.  There's no point making the user
		// sit around and wait if they don't have to.
		if (siteList != null && !isDirty)
			listener.onSiteListReady(siteList);
		// Otherwise, take note of the caller and listener, then start up
		// the progress dialog that will do the rest of the work.  The
		// caller and listener will have to wait for it to finish.  Note,
		// however, that we have to call the caller's showDialog() method
		// to get the dialog to actually appear.  The caller's onCreateDialog()
		// method should check for CryptnosApplication.DIALOG_PROGRESS as an
		// option, then call CryptnosApplication.onCreateDialog() to get the
		// actual dialog reference.  Convoluted, I know, but it works.
		else {
			this.caller = caller;
			this.listener = listener;
			caller.showDialog(DIALOG_PROGRESS);
		}
	}
	
	/**
	 * Checks to see if the site list currently "dirty" and needs to be
	 * rebuilt 
	 * @return True of the list is "dirty", false otherwise
	 */
	public boolean isSiteListDirty() { return isDirty; }
	
	/**
	 * Set the site list as "dirty" and in need of being rebuilt.  This
	 * should be called by any Activity that may modify the database, which
	 * subsequently requires the site list to be refreshed.  Note that an
	 * activity cannot declare the list to be "clean"; only the application
	 * itself can do that.
	 */
	public void setSiteListDirty() { isDirty = true; }
	
	/**
	 * Get the common ParamsDbAdapter object for the entire application.  All
	 * activities should call this to get access to the database rather than
	 * opening their own independent connections.
	 * @return A ParamsDbAdapter to access the Cryptnos database
	 */
	public ParamsDbAdapter getDBHelper() { return DBHelper; }

	/**
	 * Get the application's SharedPreferences object.  If any editing occurs
	 * while this object is in your possession, make sure to commit your
	 * changes!
	 * @return The application's SharedPreferences object
	 */
	public SharedPreferences getPrefs() { return prefs; }
	
	/**
	 * Get the user's preferred text encoding (or the default if no
	 * preference has been set).  Use this for all String.getBytes()
	 * operations.
	 * @return
	 */
	public String getTextEncoding() { return textEncoding; }
	
	/**
	 * Set the user's preferred text encoding, which will be used for all
	 * String.getBytes() operations.  This value will automatically be
	 * written to the shared preferences if successful.
	 * @param encoding The text encoding ID string of the user's preferred
	 * encoding
	 * @throws UnsupportedEncodingException Thrown if the specified encoding
	 * is not supported
	 */
	public void setTextEncoding(String encoding)
		throws UnsupportedEncodingException
	{
		// This may be a bit wasteful, but try to generate some bytes
		// using the specified encoding.  If this fails, the encoding is
		// not valid and an UnsupportedEncodingException will be thrown,
		// which we'll let the calling method handle.
		"test me".getBytes(encoding);
		// If we pass that test, update our local value and push that on
		// to the shared preferences:
		textEncoding = encoding;
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PREFS_TEXT_ENCODING, textEncoding);
		editor.commit();
	}
	
	/**
	 * Get a File representing the root of all import/export activities.
	 * Files will only be written or read from this path.
	 * @return A File representing the import/export root path.
	 */
	public File getImportExportRootPath() { return importExportRoot; }
	
	/**
	 * Check the external storage mechanism to make sure we can write data
	 * to it
	 * @return True if we can write to external storage, false otherwise
	 */
	public boolean canWriteToExternalStorage()
	{
		return Environment.getExternalStorageState().compareTo(Environment.MEDIA_MOUNTED) == 0;
	}
	
	/**
	 * Check the external storage mechanism to make sure we can read data
	 * from it
	 * @return True if we can read from external storage, false otherwise
	 */
	public boolean canReadFromExternalStorage()
	{
		return (Environment.getExternalStorageState().compareTo(Environment.MEDIA_MOUNTED) == 0)
			|| (Environment.getExternalStorageState().compareTo(Environment.MEDIA_MOUNTED_READ_ONLY) == 0);
	}
	
	/**
	 * Check to see if the UpgradeManager has run for this session
	 * @return True if the UpgradeManager has run, false otherwise
	 */
	public boolean hasUpgradeManagerRun() { return upgradeManagerRan; }
	
	/**
	 * Run the UpgradeManager
	 * @param caller The calling Activity
	 */
	public void runUpgradeManager(Activity caller) {
		if (!upgradeManagerRan) {
			this.caller = caller;
			upgradeManagerRan = true;
			UpgradeManager um = new UpgradeManager(this, caller);
			um.performUpgradeCheck();
		}
	}
	
	/**
	 * Create and return a dialog box.  Note that Android Application classes
	 * do not ordinarily control or own individual dialogs; any dialog created
	 * by this method actually becomes the property of the Activity set by
	 * requestSiteList().  This method should be called by the Activity's
	 * own onCreateDialog() method, which should look for
	 * CryptnosApplication.DIALOG_PROGRESS as a potential dialog ID.
	 * @param id A constant specifying which dialog to create
	 * @return The Dialog
	 */
    public Dialog onCreateDialog(int id)
    {
    	// We need a dialog reference to pass out, as well as references
    	// to ourself to pass in for contexts on Toasts and dialog control.
    	Dialog dialog = null;
    	// Determine which dialog to show:
    	switch (id)
    	{
    		// The progress dialog is used when loading data from the data-
    		// base, which can be a time consuming process.  We'll let the
    		// ListBuilderThread do the heavy lifting.
	    	case DIALOG_PROGRESS:
	    		progressDialog = new ProgressDialog(caller);
	    		progressDialog.setOwnerActivity(caller);
	    		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    		progressDialog.setMax(100);
	            progressDialog.setMessage(getResources().getString(R.string.sitelist_loading_message));
	            listBuilderThread = new ListBuilderThread(this, handler);
	            listBuilderThread.start();
	            dialog = progressDialog;
	    		break;
	    	// If the user is upgrading from a version before 1.2.0 and their
	    	// default text encoding is not UTF-8, show them a warning message
	    	// telling them they should really change their encoding for
	    	// compatibility reasons.  If they agree, take them to the
	    	// Advanced Settings activity.
	    	case DIALOG_UPGRADE_TO_UTF8:
	    		// We'll use the AlertDialog builder to build this one.  Note that we
	    		// need to grab the message string from the resources and tweak it
	    		// before setting the message.
	    		AlertDialog.Builder adb = new AlertDialog.Builder(caller);
	    		String message = getResources().getString(R.string.error_upgrader_change_encoding_warning);
	    		message = message.replace(getResources().getString(R.string.meta_replace_token),
	    				System.getProperty("file.encoding", "No Default"));
	    		adb.setMessage(message);
	    		adb.setCancelable(true);
	    		// What to do when the Yes button is clicked:
	    		adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					// If they said yes, launch the Advanced Settings activity:
					@Override
					public void onClick(DialogInterface dialog, int which) {
			            Intent i1 = new Intent(caller, AdvancedSettingsActivity.class);
			            startActivity(i1);
					}
	    		});
	    		// What to do when the No button is clicked:
    			adb.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
    				@Override
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   // For now, just cancel the dialog.  We'll follow
 		        	   // up on that below.
 		        	   dialog.cancel();
 		           }
 		       	});
    			// What to do if the dialog is canceled:
    			adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
	    				// If they said no or canceled the dialog, go ahead and set
						// the app file encoding to the system default and refresh
						// the salt.
						try {
							setTextEncoding(System.getProperty("file.encoding",
								TEXT_ENCODING_UTF8));
							refreshParameterSalt();
						}
						catch (Exception ex) {}
						// See the comment above.  Simply canceling the dialog
						// makes it be reused, causing the message text not to
						// get refreshed.  We have to actually tell the activity
						// to remove the dialog and force it to be rebuilt the
						// next time it is needed.
						caller.removeDialog(DIALOG_UPGRADE_TO_UTF8);
					}
				});
	    		dialog = (Dialog)adb.create();
	    		break;
    	}
    	return dialog;
    }
    
    /** Define the Handler that receives messages from the list builder
     *  thread and updates the progress */
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	// Get our percent done and update the progress dialog:
            int total = msg.getData().getInt("percent_done");
            if (total > 0) progressDialog.setProgress(total);
            // If we reach 100%, it's time to close up shop:
            if (total >= 100) {
                // Originally, we used dismissDialog() here to close the
            	// dialog.  The difference is that dismiss keeps the dialog
            	// around in memory, which is more efficient.  The caveat is
            	// that the next time we need it, our ListBuilderThread won't
            	// work properly.  So instead we'll remove the dialog once
            	// we're done, forcing Android to rebuild it and refresh the
            	// list every time.  Less efficient, but at least it works.
                caller.removeDialog(DIALOG_PROGRESS);
                // Close down the list builder thread:
                listBuilderThread.setState(ListBuilderThread.STATE_DONE);
                // If the site list contains anything useful, mark it as clean
                // (i.e. not dirty).  Otherwise, make it dirty so we'll be
                // forced to rebuild it again next time.
                if (siteList != null) isDirty = false;
                else isDirty = true;
                // Now that we have a list, pass it on to the listener waiting
                // to get it:
                listener.onSiteListReady(siteList);
            // If we got a "percentage" less than zero, some sort of error
            // occurred.  Warn the user.  Note that we also set our "dirty"
            // flag to true to make sure that we force a refresh the next
            // time the site list is requested.
            } else if (total < 0) {
                listBuilderThread.setState(ListBuilderThread.STATE_DONE);
                isDirty = true;
            	Toast.makeText(caller, R.string.error_bad_listfetch,
                		Toast.LENGTH_LONG).show();
            }
        }
    };
    
    /** This private Thread-based class builds the site list in a separate
     *  thread of execution to improved the responsiveness and perceived
     *  performance of the application.  This does the heavy lifting of
     *  reading parameters from the database, decrypting them, and building
     *  the site list array for this activity to display. */
    private class ListBuilderThread extends Thread {
    	Handler mHandler;
    	CryptnosApplication theApp;
        final static int STATE_DONE = 0;
        final static int STATE_RUNNING = 1;
        int mState;
        int mSiteCount = 0;
        int mCounter = 0;

        /**
         * The ListBuilderThread constructor
         * @param caller The calling Activity
         * @param handler The Handler that will catch our messages
         */
        ListBuilderThread(CryptnosApplication theApp, Handler handler) {
        	this.theApp = theApp;
        	mHandler = handler;
        }
       
        @Override
        public void run() {
            mState = STATE_RUNNING;
            Message msg = null;
            Bundle b = null;
            
            // Asbestos underpants:
            try
            {
    	        // Get our list of parameter information:
    	        Cursor cursor = DBHelper.fetchAllSites();
    	        caller.startManagingCursor(cursor);
    	        cursor.moveToFirst();
    	        mSiteCount = cursor.getCount();
    	        // Unfortunately, since we're encrypting all our data, we can't
    	        // take advantage of Android's build in list adapter stuff.  We'll
    	        // have to do this ourselves.  Start by creating an ArrayList,
    	        // which we'll populate with the decrypted site name tokens.
    	        ArrayList<String> sites = new ArrayList<String>();
    	        // Step through the data:
    	        while (!cursor.isAfterLast() && mState == STATE_RUNNING)
    	        {
    	        	// We only want to add stuff if it doesn't blow up:
    	        	try
    	        	{
    	        		// Reconstitute the parameters from the encrypted string
    	        		// pulled from the database.  Note that we need the site
    	        		// key to unlock the data.
    		        	SiteParameters params =
    		        		new SiteParameters(theApp,
    		        			cursor.getString(1),
    		        			cursor.getString(2));
    		        	// Get the site name from the parameters and add it to
    		        	// the site array.
    		        	sites.add(params.getSite());
    	        	}
    	        	// If anything blows up, ignore it:
    	        	catch (Exception e) {}
		        	// Update the progress so far.  Note that we're
		        	// assuming that this process is 95% of our work
		        	// rather than 100%; we'll manually update the other
		        	// 5% below.  (And these numbers may get tweaked
		        	// later.)
		        	msg = mHandler.obtainMessage();
	                b = new Bundle();
	                b.putInt("percent_done",
	                	(int)(Math.floor(((double)mCounter / (double)mSiteCount * 95.0d))));
	                msg.setData(b);
	                mHandler.sendMessage(msg);
	                mCounter++;
    	        	cursor.moveToNext();
    	        }
    	        // At this point, we're done with the database.  Go ahead and
    	        // close the cursor:
    	        cursor.close();
    	        // Now we want to sort the list to be more presentable to the
    	        // user.  To do that, we need to move the ArrayList into an
    	        // ordinary String array and take advantage of the built-in
    	        // array sorting routines.
    	        String[] sortedSites = new String[sites.size()];
    	        sites.toArray(sortedSites);
    	        java.util.Arrays.sort(sortedSites, String.CASE_INSENSITIVE_ORDER);
    	        // At this point, we're essentially ready to go.  Set the
    	        // official list to our newly sorted one.  Then declare the
    	        // site list clean (i.e. not dirty) so we won't have to rebuild
    	        // it the next time it's needed.
                siteList = sortedSites;
                isDirty = false;
                // Now that we're done, send a message to the handler so it
                // can pass the list back to the listener:
	        	msg = mHandler.obtainMessage();
                b = new Bundle();
                b.putInt("percent_done", 100);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
            // If anything blew up, inform the user:
            catch (Exception e)
            {
	        	msg = mHandler.obtainMessage();
                b = new Bundle();
                b.putInt("percent_done", -1);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        }
        
        /** Set the state of the thread to the given value. */
        public void setState(int state) {
            mState = state;
        }

    }
    
    /**
     * Check to see if the specified intent is available
     * @param context A Context
     * @param action A string specifying the intent to search for
     * @return True if the intent is available, false otherwise
     */
    public static boolean isIntentAvailable(Context context, String action) {
    	// This is taken as-is from the Android Developer's Guide.  Look to
    	// to see if the specified intent resolves, and if we get anything
    	// back, return true.  Otherwise, return false.
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

}
