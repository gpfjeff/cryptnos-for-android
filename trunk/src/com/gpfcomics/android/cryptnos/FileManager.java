/* FileManager.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          November 4, 2010
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * The FileManager class was added in Cryptnos 1.2.0 to hopefully abstract the
 * process of using third-party file management applications for selecting import
 * files and export paths.  By pulling this into its own class, we can hopefully
 * add and remove file managers with relative ease.  This class will take care of
 * the process of detecting what recognized file managers are available and
 * maintaining the user's preference of which file manager to use.  Note that
 * only the file managers explicitly choose to support will be available, and we
 * can only use those that publish the necessary data and Intents to make these
 * selections possible.
 * 
 * For the initial release, we will support OI File Manager from OpenIntents and
 * AndExplorer, as these are the only file managers that we know about with
 * published Intents.
 * 
 * UPDATES FOR 1.2.1:  Added ES File Explorer
 * 
 * UPDATES FOR 1.3.0:  Abstracted third-party app names into constants and pulled
 * hard-coded error strings out into strings.xml where they belong.
 * 
 * UPDATES FOR 1.3.1:  Minor tweaks to make Lint happy
 * 
 * This program is Copyright 2011, Jeffrey T. Darlington.
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
import java.net.URI;
import java.util.ArrayList;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;

/**
 * This class abstracts third-party file manager applications and provides us with a
 * wrapper for dealing with them.  Cryptnos can use these file managers for selecting
 * files and folders/directories during the import and export processes.  It only
 * knows about specific file managers whose Intents we know how to call, but should
 * provide sufficient abstraction to allow new file managers to be added over time
 * as new Intents are discovered.
 * @author Jeffrey T. Darlington
 * @version 1.3.1
 * @since 1.2
 */
public class FileManager {

	/* Public Constants *******************************************************/
	
	/** A constant used to represent the case where no file manager is currently
	 *  selected.  This could be the user's preference, or it could be that no
	 *  known file manager is currently available. */
	public static final int APP_NO_FILE_MANAGER = 0;
	
	/** A constant used to represent that OI File Manager is selected as the
	 *  user's preferred file manager. */
	public static final int APP_OI_FILE_MANAGER = 1;
	
	/** This constant represents the OI File Manager app name */
	public static final String NAME_OI_FILE_MANAGER = "OI File Manager";
	
	/** A constant used to represent that AndExplorer is selected as the
	 *  user's preferred file manager. */
	public static final int APP_ANDEXPLORER = 2;
	
	/** This constant represents the AndExplorer app name */
	public static final String NAME_ANDEXPLORER = "AndExplorer";
	
	/** A constant used to represent that ES File Explorer is selected as the
	 *  user's preferred file manager. */
	public static final int APP_ES_FILE_EXPLORER = 3;
	
	/** This constant represents the ES File Explorer app name */
	public static final String NAME_ES_FILE_EXPLORER = "ES File Explorer";
	
	/** The return code expected when we call startActivityForResult() with our
	 *  generated Intent to select a file. */
	public static final int INTENT_REQUEST_SELECT_FILE = 4321;
	
	/** The return code expected when we call startActivityForResult() with our
	 *  generated Intent to select a folder or directory. */
	public static final int INTENT_REQUEST_SELECT_FOLDER = 4322;

	/* Private Constants ********************************************************/
	
	/** The package name for OI File Manager */
	private static final String PACKAGE_OI_FILE_MANAGER = "org.openintents.filemanager";
	
	/** The package name for AndExplorer */
	private static final String PACKAGE_ANDEXPLORER = "lysesoft.andexplorer";
	
	/** The package name for ES File Explorer */
	private static final String PACKAGE_ES_FILE_EXPLORER = "com.estrongs.android.pop";
	
	/** The select file Intent action for OI File Manager */
	private static final String FILE_SELECT_INTENT_OI = "org.openintents.action.PICK_FILE";

	/** The select folder Intent action for OI File Manager */
	private static final String DIR_SELECT_INTENT_OI = "org.openintents.action.PICK_DIRECTORY";

	/** The select file Intent action for AndExplorer */
	private static final String FILE_SELECT_INTENT_AE = "vnd.android.cursor.dir/lysesoft.andexplorer.file";
	
	/** The select folder Intent action for AndExplorer */
	private static final String DIR_SELECT_INTENT_AE = "vnd.android.cursor.dir/lysesoft.andexplorer.directory";
	
	/** The select file Intent action for ES File Explorer */
	private static final String FILE_SELECT_INTENT_ES = "com.estrongs.action.PICK_FILE";
	
	/** The select folder Intent action for ES File Explorer */
	private static final String DIR_SELECT_INTENT_ES = "com.estrongs.action.PICK_DIRECTORY";
	
	/* Private Members **********************************************************/
	
	/** Internal reference for the main Cryptnos app */
	private CryptnosApplication theApp = null;
	
	/** The user's current preferred file manager.  Must be one of the public
	 *  constant values.  Defaults to NO_FILE_MANAGER. */
	private int preferredFM = APP_NO_FILE_MANAGER;
	
	/** A list of currently available file managers by constant code.  May be null
	 *  if no recognized file managers are available. */
	private int[] availableFMs = null;
	
	/* Public methods: ***********************************************************/
	
	/**
	 * Default constructor.
	 * @param theApp A reference to the main Cryptnos application
	 */
	public FileManager(CryptnosApplication theApp) {
		this(theApp, APP_NO_FILE_MANAGER);
	}
	
	/**
	 * Main constructor
	 * @param theApp A reference to the main Cryptnos application
	 * @param preferredFM The user's current preference of file manager.  This
	 * must be one of the file manager constants.  If the specified file manager
	 * is not installed on the system, this value is ignored and the preferred
	 * file manager is set to none.
	 */
	public FileManager(CryptnosApplication theApp, int preferredFM) {
		this.theApp = theApp;
		this.preferredFM = preferredFM;
		findAvailableFileManagers();
	}
	
	/** Searches the system to see if any of the recognized file managers are
	 *  currently installed.  An internal list is maintained of which recognized
	 *  file managers are available.  If the user's preferred file manager is
	 *  not in the list, that setting is reset to none. */
	public void findAvailableFileManagers() {
		// I don't really like this, but create an empty ArrayList of Integer
		// objects.  ArrayList doesn't like primitive types, and we can't
		// dynamically build a simple array of unknown size.
		ArrayList<Integer> fmList = new ArrayList<Integer>();
		// Get the PackageManager from the app:
		PackageManager pm = theApp.getPackageManager();
		// Step through all our recognized file manager package names and see if
		// they're in the list.  This should be quicker than the other alternative
		// of grabbing *all* packages and cycling through them.  The caveat is that
		// we're using exception throwing to see if the file manager is not available,
		// which in itself is expensive.  If no exception is thrown, we know the
		// package exists.
		//
		// First, OI File Manager:
		try {
			pm.getPackageInfo(PACKAGE_OI_FILE_MANAGER, 0);
			fmList.add(Integer.valueOf(APP_OI_FILE_MANAGER));
		}
		catch (PackageManager.NameNotFoundException ex1) {}
		// AndExplorer:
		try {
			pm.getPackageInfo(PACKAGE_ANDEXPLORER, 0);
			fmList.add(Integer.valueOf(APP_ANDEXPLORER));
		}
		catch (PackageManager.NameNotFoundException ex2) {}
		// ES File Explorer:
		try {
			pm.getPackageInfo(PACKAGE_ES_FILE_EXPLORER, 0);
			fmList.add(Integer.valueOf(APP_ES_FILE_EXPLORER));
		}
		catch (PackageManager.NameNotFoundException ex3) {}
		// Now check the ArrayList's size.  If we got anything at all, we found
		// at least one of them.  Convert the ArrayList to a simple integer array:
		if (fmList.size() > 0) {
			availableFMs = new int[fmList.size()];
			// There is a chance that the user specified a preference in the past
			// but uninstalled the package containing the file manager.  If that's
			// so, we don't want to leave that preference in there.  As we build
			// the simple array, check to see if the user's current preference is
			// still a valid one:
			boolean inThere = false;
			// Now convert and test:
			for (int j = 0; j < availableFMs.length; j++) {
				availableFMs[j] = fmList.get(j).intValue();
				if (preferredFM == availableFMs[j])
					inThere = true;
			}
			// If the user has a preference and it's not in the list, reset the
			// preference to no file manager:
			if (preferredFM != APP_NO_FILE_MANAGER && !inThere) {
				preferredFM = APP_NO_FILE_MANAGER;
				SharedPreferences.Editor editor = theApp.getPrefs().edit();
				editor.putInt(CryptnosApplication.PREFS_FILE_MANAGER, APP_NO_FILE_MANAGER);
				editor.commit();
			}
		// If no recognized file managers were found, default the user's preference
		// to no file manager and write that preference to the preference file:
		} else {
			preferredFM = APP_NO_FILE_MANAGER;
			availableFMs = null;
			SharedPreferences.Editor editor = theApp.getPrefs().edit();
			editor.putInt(CryptnosApplication.PREFS_FILE_MANAGER, APP_NO_FILE_MANAGER);
			editor.commit();
		}
	}
	
	/** Get an array of codes representing all recognized file managers found on
	 *  the system.  Note that this may return a null or empty list. */
	public int[] getAvailableFileManagers() {
		// Originally, we did the search once and stored the list for the sake of
		// efficiency.  In practice, however, this meant that there was a period of
		// time during which a file manager could be added or removed and the internal
		// list would not be in sync with what's on the system.  Since the search is
		// relatively quick, we'll force this to refresh every time.  We may want to
		// go back to the old method later if this doesn't work in the long term.
		findAvailableFileManagers();
		return availableFMs;
	}
	
	/** Get an array of strings representing the names of all recognized file
	 *  managers on the system, suitable for display in a drop-down box or
	 *  similar UI element.  If no recognized file managers are found, this
	 *  returns a single-element list with a string stating that no file
	 *  managers were found. */
	public String[] getAvailableFileManagerNames() {
		// See getAvailableFileManagers() for why we refresh the list every time.
		findAvailableFileManagers();
		// Declare a string array to hold our names:
		String[] names = null;
		// If we found any file managers at all, start building the array of names.
		// Note that we'll add the item for "no file manager selected" first, making
		// it a valid selection in the Advanced Settings activity.  Then step through
		// the rest of the available names and add them to the list.
		if (availableFMs != null && availableFMs.length > 0) {
			names = new String[availableFMs.length + 1];
			names[0] = mapCodeToName(APP_NO_FILE_MANAGER);
			for (int i = 1; i < names.length; i++)
				names[i] = mapCodeToName(availableFMs[i - 1]);
		// If no file managers could be found, return a single-element array with
		// a message stating as such:
		} else {
			names = new String[1];
			names[0] = theApp.getBaseContext().getResources().getString(R.string.error_no_file_managers_found);
		}
		return names;
	}
	
	/** Get the user's preferred file manager's code */
	public int getPreferredFileManager() {
		return preferredFM;
	}
	
	/** Get the user's preferred file manager's display name*/
	public String getPreferredFileManagerName() {
		return mapCodeToName(preferredFM);
	}
	
	/**
	 * Set the user's preferred file manager by specifying its code
	 * @param code The file manager code.  Must be one of the public code constants
	 * available to this class.
	 */
	public void setPreferredFileManager(int code) {
		// We only want the user to be able to set the preferred file manager if
		// the choice is a valid one.  No file manager is always valid; however,
		// if the code is anything else, make sure it's in the list of available
		// file managers before adding it.
		if (code == APP_NO_FILE_MANAGER ||
				((code == APP_OI_FILE_MANAGER || code == APP_ANDEXPLORER ||
						code == APP_ES_FILE_EXPLORER)) &&
				codeInAvailableList(code)) {
			// Store the preference, first locally then in the shared preferences:
			preferredFM = code;
			SharedPreferences.Editor editor = theApp.getPrefs().edit();
			editor.putInt(CryptnosApplication.PREFS_FILE_MANAGER, code);
			editor.commit();
		}
	}
	
	/**
	 * Set the user's preferred file manager by specifying its name
	 * @param name The file manager name.  Must be a string as output by
	 * getAvailableFileManagerNames()
	 */
	public void setPreferredFileManager(String name) {
		// No sense reinventing the wheel.  Convert the string name to the right
		// code (if possible) and reuse the code above:
		setPreferredFileManager(mapNameToCode(name));
	}
	
	/** Generate a new Intent suitable for passing to startActivityForResult() to
	 *  select a file, using the user's preferred file manager.  If the user has
	 *  no preference of file manager, this returns null. */
	public Intent generateSelectFileIntent(String rootPath, String dialogTitle,
			String buttonText) {
		switch (preferredFM) {
			// Generate an OI File Manager intent:
			case APP_OI_FILE_MANAGER:
				Intent oii = new Intent(FILE_SELECT_INTENT_OI);
				oii.setData(Uri.parse("file://" + rootPath));
				oii.putExtra("org.openintents.extra.TITLE", dialogTitle);
				oii.putExtra("org.openintents.extra.BUTTON_TEXT", buttonText);
				return oii;
			// Generate an AndExplorer intent:
			case APP_ANDEXPLORER:
				Intent aei = new Intent();
				aei.setAction(Intent.ACTION_PICK);
				aei.setDataAndType(Uri.fromFile(new File(rootPath)),
						FILE_SELECT_INTENT_AE);
				aei.putExtra("explorer_title", dialogTitle);
				aei.putExtra("browser_list_layout", "0");
				return aei;
			// ES File Explorer:
			case APP_ES_FILE_EXPLORER:
				Intent esi = new Intent(FILE_SELECT_INTENT_ES);
				esi.putExtra("com.estrongs.intent.extra.TITLE", buttonText);
				return esi;
			// If there's no preference or it's something we don't recognize,
			// do nothing:
			case APP_NO_FILE_MANAGER:
			default:
				return null;
		}
	}
	
	/** Generate a new Intent suitable for passing to startActivityForResult() to
	 *  select a folder, using the user's preferred file manager.  If the user has
	 *  no preference of file manager, this returns null. */
	public Intent generateSelectFolderIntent(String rootPath, String dialogTitle,
			String buttonText) {
		switch (preferredFM) {
			// Generate an OI File Manager intent:
			case APP_OI_FILE_MANAGER:
				Intent oii = new Intent(DIR_SELECT_INTENT_OI);
				oii.setData(Uri.parse("file://" + rootPath));
				oii.putExtra("org.openintents.extra.TITLE", dialogTitle);
				oii.putExtra("org.openintents.extra.BUTTON_TEXT", buttonText);
				return oii;
			// Generate an AndExplorer intent:
			case APP_ANDEXPLORER:
				Intent aei = new Intent();
				aei.setAction(Intent.ACTION_PICK);
				aei.setDataAndType(Uri.fromFile(new File(rootPath)),
						DIR_SELECT_INTENT_AE);
				aei.putExtra("explorer_title", dialogTitle);
				return aei;
			// ES File Explorer:
			case APP_ES_FILE_EXPLORER:
				Intent esi = new Intent(DIR_SELECT_INTENT_ES);
				esi.putExtra("com.estrongs.intent.extra.TITLE", buttonText);
				return esi;
			// If there's no preference or it's something we don't recognize,
			// do nothing:
			case APP_NO_FILE_MANAGER:
			default:
				return null;
		}
	}
	
	/**
	 * Given an Intent returned from the called file manager, get the selected file's
	 * name and return it to the caller
	 * @param data An Intent returned by the file manager which should contain the
	 * selected file's path
	 * @return The selected file's path, or null if no file was selected
	 */
	public String getSelectedFile(Intent data) {
		if (data == null) return null;
		switch (preferredFM) {
			// Getting the file from OI File Manager is pretty simple:
			case APP_OI_FILE_MANAGER:
				return data.getDataString();
			// AndExplorer takes a bit more work, and it may technically not return
			// anything useful:
			case APP_ANDEXPLORER:
				Uri uri = data.getData();
				if (uri != null)
				{
					String path = uri.toString();
				    if (path.toLowerCase().startsWith("file://"))
				    	return (new File(URI.create(path))).getAbsolutePath();
				    else return null;
				}
				else return null;
			// This was taken pretty much verbatim from the ES File Explorer
			// Developers page (http://www.estrongs.com/en/support/developers.html):
			case APP_ES_FILE_EXPLORER:
				Uri uri2 = data.getData();
				if (uri2 != null) return uri2.getPath();
				else return null;    
			// If there's no preference or it's something we don't recognize,
			// do nothing:
			case APP_NO_FILE_MANAGER:
			default:
				return null;
		}
	}
	
	/**
	 * Given an Intent returned from the called file manager, get the selected folder's
	 * name and return it to the caller
	 * @param data An Intent returned by the file manager which should contain the
	 * selected folder's path
	 * @return The selected folder's path, or null if no folder was selected
	 */
	public String getSelectedFolder(Intent data) {
		// I took a long look at the existing code and examples and it looks like
		// the returns for both OI File Manager and AndExplorer are the same regardless
		// of whether we're getting files or folders.  In that case, we'll just make
		// this an alias for getSelectedFile() for now and keep it as a separate
		// method in case future file manager additions require special work here.
		return getSelectedFile(data);
	}
	
	/**
	 * Determine whether or not the user has an active preference of file manager.
	 * Note that if the user previously set a preference and that file manager is
	 * subsequently uninstalled, this will automatically return false and the
	 * user's preference will be lost; the FileManager class will revert to no
	 * file manager having been selected. 
	 * @return Returns true or false
	 */
	public boolean isFileManagerSelected() {
		// See getAvailableFileManagers() for why we refresh the list every time.
		findAvailableFileManagers();
		// There is a chance that the user set a preferred FM but then later
		// uninstalled it.  Thus, we can't just rely on a simple check to see
		// if the preferred FM isn't the "no file manager" selection.  So first
		// we'll check to see if the preferred FM is actually available, *then*
		// we'll make the check.
		if (codeInAvailableList(preferredFM)) return preferredFM != APP_NO_FILE_MANAGER;
		// If we couldn't find the preferred FM in the available list, force
		// the preference back to nothing selected and return false.  This will
		// revert us back to the default behavior.
		else {
			setPreferredFileManager(APP_NO_FILE_MANAGER);
			return false;
		}
	}
	
	/**
	 * Return a string containing a list of recognized file manager names.  This
	 * is intended to be displayed in the Advanced Settings activity to show the
	 * user what file managers they can install and use with Cryptnos
	 * @return A String containing the recognized file manager names
	 */
	public String getRecognizedFileManagerNames() {
		return "\t" + NAME_OI_FILE_MANAGER + "\n\t" + NAME_ANDEXPLORER +
			"\n\t" + NAME_ES_FILE_EXPLORER;
	}
	
	/* Private methods: ***********************************************************/
	
	/**
	 * Map the given code to a string suitable for display
	 * @param code The code.  Must be one of the public file manager code values.
	 * @throws IllegalArgumentException Thrown if the supplied code is not
	 * recognized
	 */
	private String mapCodeToName(int code) {
		Resources res = theApp.getBaseContext().getResources();
		// Simple enough:  Switch on code and return a string.  If it's a code
		// we don't recognize, throw an exception:
		switch (code) {
			case APP_NO_FILE_MANAGER:
				return res.getString(R.string.error_no_file_managers_selected);
			case APP_OI_FILE_MANAGER:
				return NAME_OI_FILE_MANAGER;
			case APP_ANDEXPLORER:
				return NAME_ANDEXPLORER;
			case APP_ES_FILE_EXPLORER:
				return NAME_ES_FILE_EXPLORER;
			default:
				throw new IllegalArgumentException(res.getString(R.string.error_invalid_file_manager_code));
		}
	}
	
	/**
	 * Map the given display name to the appropriate code constant
	 * @param name The display name of the file manager
	 * @return The file manager's code, or NO_FILE_MANAGER if any error occurs
	 */
	private int mapNameToCode(String name) {
		Resources res = theApp.getBaseContext().getResources();
		// This is the inverse of mapCodeToName(), but a bit more forgiving.
		// Compare the name to the recognized strings and return the appropriate
		// code.  If the name isn't recognize, default back to no file manager
		// preference.
		if (name.compareTo(res.getString(R.string.error_no_file_managers_found)) == 0)
			return APP_NO_FILE_MANAGER;
		if (name.compareTo(res.getString(R.string.error_no_file_managers_selected)) == 0)
			return APP_NO_FILE_MANAGER;
		if (name.compareTo(NAME_OI_FILE_MANAGER) == 0)
			return APP_OI_FILE_MANAGER;
		if (name.compareTo(NAME_ANDEXPLORER) == 0)
			return APP_ANDEXPLORER;
		if (name.compareTo(NAME_ES_FILE_EXPLORER) == 0)
			return APP_ES_FILE_EXPLORER;
		return APP_NO_FILE_MANAGER;
	}
	
	/**
	 * Check to see if the specified file manager code is in the available file
	 * manager list
	 * @param code The file manager code to test.  Must be one of the file
	 * manager code constants.
	 * @return True if the file manager is available, false otherwise
	 */
	private boolean codeInAvailableList(int code) {
		// This is a bit of a kludge, but always return true if we happen to
		// get passed in the "no file manager selected" code.  After all, no
		// selection at all is technically a valid one.
		if (code == APP_NO_FILE_MANAGER) return true;
		// Otherwise, step through the available list and return true if we can
		// find the code.  I wish there were a more efficient way to do this, but
		// fortunately our lists should be small.
		if (availableFMs != null & availableFMs.length > 0) {
			for (int i = 0; i < availableFMs.length; i++)
				if (code == availableFMs[i]) return true;
		}
		return false;
	}
}
