/* ParameterViewState.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          April 19, 2011
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This class encapsulates parameter and password data for use in the New, Edit,
 * and Generate Existing Password activities.  It is primarily intended to help
 * make these activities behave a bit nicer where faced with the situation where
 * the user initiates a simple configuration change, such as rotating the screen
 * or sliding out a physical keyboard.
 * 
 * Android deals with these events by destroying the current activity and
 * rebuilding it under the new configuration.  If the activity saves its view
 * or instance state, this can be quickly restored during onCreate().  However,
 * we don't store data here for various reasons, primarily for security.  If the
 * user leaves Cryptnos and comes back (a common situation in Android), we don't
 * want the user to pick up where they last left off; we want to start them back
 * at the main menu.  Thus, we can't take advantage of the built-in instance
 * saving mechanisms to restore the view's state for short-term changes like
 * a screen rotation.
 * 
 * We can, however, use Activity.onRetainNonConfigurationInstance() to store a
 * small object for us during the change and get that back using
 * Activity.getLastNonConfigurationInstance().  This object is not stored during
 * long-term changes like leaving the application, doing something else, and
 * coming back, but it *does* get stored during the quick rebuilds for small
 * configuration changes.  Using this, we can save a snapshot of the current
 * data and restore it once the change has been complete.
 * 
 * Because of the similarity of data, this object will be used by both the
 * New/Edit Parameters activity (which handles both cases but disables the
 * site token box when editing a pre-existing set of parameters) and the
 * Generate Existing Password activity.  Use the appropriate constructor for
 * the required instance.
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

/**
 * This class encapsulates parameter and password data for use in the New, Edit,
 * and Generate Existing Password activities.  It is primarily intended to help
 * make these activities behave a bit nicer where faced with the situation where
 * the user initiates a simple configuration change, such as rotating the screen
 * or sliding out a physical keyboard.
 * @author Jeffrey T. Darlington
 * @version 1.2.2
 * @since 1.2.2
 */
class ParameterViewState {

	/** The full SiteParameters object */
	private SiteParameters siteParams = null;
	/** The raw site token string */
	private String site = null;
	/** The user's master password */
	private String masterPassword = null;
	/** The cryptographic hash */
	private String hash = null;
	/** The number of iterations of the hash to perform.  Note that this gets
	 *  stored as a string rather than a number so we don't have to worry about
	 *  trying to parse the text and failing. */
	private String iterations = null;
	/** The index for the Spinner view that lets the user choose what types
	 *  of characters to include or exclude from the final password */
	private int charTypes = -1;
	/** The index for the Spinner containing length restriction value */
	private int charLimit = -1;
	/** The generated final password */
	private String genPassword = null;
	private int mode = -1;
	private String lastSite = null;
	private long rowID = -1L;
	
	/**
	 * The Generate Existing Password constructor.
	 * @param siteParams The SiteParameters object
	 * @param masterPassword The user's master password
	 * @param genPassword The generated password
	 */
	protected ParameterViewState(SiteParameters siteParams, String masterPassword,
			String genPassword) {
		this.siteParams = siteParams;
		this.masterPassword = masterPassword;
		this.genPassword = genPassword;
	}
	
	/**
	 * The New/Edit Parameters constructor
	 * @param site The site token
	 * @param masterPassword The user's master password
	 * @param hash The hash algorithm name
	 * @param iterations The number of iterations
	 * @param charTypes The character type index
	 * @param charLimit The character limit index
	 * @param genPassword The generated password
	 */
	protected ParameterViewState(String site, String masterPassword, String hash,
			String iterations, int charTypes, int charLimit, String genPassword,
			int mode, String lastSite, long rowID) {
		this.site = site;
		this.masterPassword = masterPassword;
		this.hash = hash;
		this.iterations = iterations;
		this.charTypes = charTypes;
		this.charLimit = charLimit;
		this.genPassword = genPassword;
		this.mode = mode;
		this.lastSite = lastSite;
		this.rowID = rowID;
	}
	
	/**
	 * Return the SiteParameters object, if any
	 * @return The SiteParameters object
	 */
	protected SiteParameters getSiteParameters() { return siteParams; }

	/**
	 * Return the site token, if any
	 * @return The site token
	 */
	protected String getSite() { return site; }
	
	/**
	 * Return the master password, if any
	 * @return The master password
	 */
	protected String getMasterPassword() { return masterPassword; }
	
	/**
	 * Return the hash algorithm, if any
	 * @return The hash algorithm
	 */
	protected String getHash() { return hash; }

	/**
	 * Return the number of iterations, if specified
	 * @return The number of iterations
	 */
	protected String getIterations() { return iterations; }

	/**
	 * Return the character type index, if specified
	 * @return The character type index
	 */
	protected int getCharTypes() { return charTypes; }

	/**
	 * Return the character limit index, if specified
	 * @return The character limit index
	 */
	protected int getCharLimit() { return charLimit; }

	/**
	 * Return the generated password, if specified
	 * @return The generated password
	 */
	protected String getGeneratedPassword() { return genPassword; }
	
	/**
	 * Return the new/edit mode, if specified
	 * @return The new/edit mode
	 */
	protected int getMode() { return mode; }
	
	/**
	 * Return the last site token, if specified
	 * @return The last site token
	 */
	protected String getLastSite() { return lastSite; }
	
	/**
	 * Return the database row ID, if specified
	 * @return The database row ID
	 */
	protected long getRowID() { return rowID; }
	
}
