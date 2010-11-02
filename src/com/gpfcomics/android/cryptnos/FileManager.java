package com.gpfcomics.android.cryptnos;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;

/**
 * This class abstracts third-party file manager applications and provides us with a
 * wrapper for dealing with them.  Cryptnos can use these file managers for selecting
 * files and folders/directories during the import and export processes.  It only
 * knows about specific file managers whose Intents we know how to call, but should
 * provide sufficient abstraction to allow new file managers to be added over time
 * as new Intents are discovered.
 * @author Jeffrey T. Darlington
 * @version 1.2
 * @since 1.2
 */
public class FileManager {

	/** A constant used to represent the case where no file manager is currently
	 *  selected.  This could be the user's preference, or it could be that no
	 *  known file manager is currently available. */
	public static final int NO_FILE_MANAGER = 0;
	
	/** A constant used to represent that OI File Manager is selected as the
	 *  user's preferred file manager. */
	public static final int OI_FILE_MANAGER = 1;
	
	/** A constant used to represent that AndExplorer is selected as the
	 *  user's preferred file manager. */
	public static final int ANDEXPLORER = 2;
	
	/** The return code expected when we call startActivityForResult() with our
	 *  generated Intent to select a file. */
	public static final int INTENT_REQUEST_SELECT_FILE = 4321;
	
	/** The return code expected when we call startActivityForResult() with our
	 *  generated Intent to select a folder or directory. */
	public static final int INTENT_REQUEST_SELECT_FOLDER = 4322;

	/** The package name for OI File Manager */
	private static final String PACKAGE_OI_FILE_MANAGER = "org.openintents.filemanager";
	
	/** The package name for AndExplorer */
	private static final String PACKAGE_ANDEXPLORER = "lysesoft.andexplorer";
	
	/** The select file Intent action for OI File Manager */
	private static final String FILE_SELECT_INTENT_OI = "org.openintents.action.PICK_FILE";

	/** The select folder Intent action for OI File Manager */
	private static final String DIR_SELECT_INTENT_OI = "org.openintents.action.PICK_DIRECTORY";

	/** The select file Intent action for AndExplorer */
	private static final String FILE_SELECT_INTENT_AE = "vnd.android.cursor.dir/lysesoft.andexplorer.file";
	
	/** The select folder Intent action for OI AndExplorer */
	private static final String DIR_SELECT_INTENT_AE = "vnd.android.cursor.dir/lysesoft.andexplorer.directory";
	
	/** Internal reference for the main Cryptnos app */
	private CryptnosApplication theApp = null;
	
	/** The user's current preferred file manager.  Must be one of the public
	 *  constant values.  Defaults to NO_FILE_MANAGER. */
	private int preferredFM = NO_FILE_MANAGER;
	
	/** A list of currently available file managers by constant code */
	private int[] availableFMs = null;
	
	/* Public methods: ***********************************************************/
	
	/**
	 * Default constructor.
	 * @param theApp A reference to the main Cryptnos application
	 */
	public FileManager(CryptnosApplication theApp) {
		this(theApp, NO_FILE_MANAGER);
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
			fmList.add(new Integer(OI_FILE_MANAGER));
		}
		catch (PackageManager.NameNotFoundException ex1) {}
		// AndExplorer:
		try {
			pm.getPackageInfo(PACKAGE_ANDEXPLORER, 0);
			fmList.add(new Integer(ANDEXPLORER));
		}
		catch (PackageManager.NameNotFoundException ex2) {}
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
			if (preferredFM != NO_FILE_MANAGER && !inThere) {
				preferredFM = NO_FILE_MANAGER;
				SharedPreferences.Editor editor = theApp.getPrefs().edit();
				editor.putInt(CryptnosApplication.PREFS_FILE_MANAGER, NO_FILE_MANAGER);
				editor.commit();
			}
		// If no recognized file managers were found, default the user's preference
		// to no file manager and write that preference to the preference file:
		} else {
			preferredFM = NO_FILE_MANAGER;
			availableFMs = null;
			SharedPreferences.Editor editor = theApp.getPrefs().edit();
			editor.putInt(CryptnosApplication.PREFS_FILE_MANAGER, NO_FILE_MANAGER);
			editor.commit();
		}
	}
	
	/** Get an array of codes representing all recognized file managers found on
	 *  the system.  Note that this may return a null or empty list. */
	public int[] getAvailableFileManagers() {
		if (availableFMs == null) findAvailableFileManagers();
		return availableFMs;
	}
	
	/** Get an array of strings representing the names of all recognized file
	 *  managers on the system, suitable for display in a drop-down box or
	 *  similar UI element.  If no recognized file managers are found, this
	 *  returns a single-element list with a string stating that no file
	 *  managers were found. */
	public String[] getAvailableFileManagerNames() {
		if (availableFMs == null) findAvailableFileManagers();
		String[] names = null;
		if (availableFMs != null && availableFMs.length > 0) {
			names = new String[availableFMs.length];
			for (int i = 0; i < names.length; i++)
				names[i] = mapCodeToName(availableFMs[i]);
		} else {
			names = new String[1];
			names[0] = "No recognized file managers found";
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
		if (code == NO_FILE_MANAGER ||
				((code == OI_FILE_MANAGER || code == ANDEXPLORER)) &&
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
			case OI_FILE_MANAGER:
				Intent oii = new Intent(FILE_SELECT_INTENT_OI);
				oii.setData(Uri.parse("file://" + rootPath));
				oii.putExtra("org.openintents.extra.TITLE", dialogTitle);
				oii.putExtra("org.openintents.extra.BUTTON_TEXT", buttonText);
				return oii;
			case ANDEXPLORER:
				Intent aei = new Intent();
				aei.setAction(Intent.ACTION_PICK);
				aei.setDataAndType(Uri.fromFile(new File(rootPath)),
						FILE_SELECT_INTENT_AE);
				aei.putExtra("explorer_title", dialogTitle);
				return aei;
			case NO_FILE_MANAGER:
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
			case OI_FILE_MANAGER:
				Intent oii = new Intent(DIR_SELECT_INTENT_OI);
				oii.setData(Uri.parse("file://" + rootPath));
				oii.putExtra("org.openintents.extra.TITLE", dialogTitle);
				oii.putExtra("org.openintents.extra.BUTTON_TEXT", buttonText);
				return oii;
			case ANDEXPLORER:
				Intent aei = new Intent();
				aei.setAction(Intent.ACTION_PICK);
				aei.setDataAndType(Uri.fromFile(new File(rootPath)),
						DIR_SELECT_INTENT_AE);
				aei.putExtra("explorer_title", dialogTitle);
				return aei;
			case NO_FILE_MANAGER:
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
			case OI_FILE_MANAGER:
				return data.getDataString();
			case ANDEXPLORER:
				Uri uri = data.getData();
				if (uri != null)
				{
					String path = uri.toString();
				    if (path.toLowerCase().startsWith("file://"))
				    	return (new File(URI.create(path))).getAbsolutePath();
				    else return null;
				}
				else return null;
			case NO_FILE_MANAGER:
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
	
	/* Private methods: ***********************************************************/
	
	/**
	 * Map the given code to a string suitable for display
	 * @param code The code.  Must be one of the public file manager code values.
	 * @throws IllegalArgumentException Thrown if the supplied code is not
	 * recognized
	 */
	private String mapCodeToName(int code) {
		switch (code) {
			case NO_FILE_MANAGER:
				return "No file manager selected";
			case OI_FILE_MANAGER:
				return "OI File Manager";
			case ANDEXPLORER:
				return "AndExplorer";
			default:
				throw new IllegalArgumentException("Invalid file manager code");
		}
	}
	
	/**
	 * Map the given display name to the appropriate code constant
	 * @param name The display name of the file manager
	 * @return The file manager's code, or NO_FILE_MANAGER if any error occurs
	 */
	private int mapNameToCode(String name) {
		if (name.compareTo("No recognized file managers found") == 0)
			return NO_FILE_MANAGER;
		if (name.compareTo("No file manager selected") == 0)
			return NO_FILE_MANAGER;
		if (name.compareTo("OI File Manager") == 0)
			return OI_FILE_MANAGER;
		if (name.compareTo("AndExplorer") == 0)
			return ANDEXPLORER;
		return NO_FILE_MANAGER;
	}
	
	/**
	 * Check to see if the specified file manager code is in the available file
	 * manager list
	 * @param code The file manager code to test.  Must be one of the file
	 * manager code constants.
	 * @return True if the file manager is available, false otherwise
	 */
	private boolean codeInAvailableList(int code) {
		if (availableFMs != null & availableFMs.length > 0) {
			for (int i = 0; i < availableFMs.length; i++)
				if (code == availableFMs[i])  return true;
		}
		return false;
	}
}
