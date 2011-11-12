/* ImportListener.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          October 31, 2011
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This inteface provides a means for the ImportExportHandler to return a list of
 * imported sites from an import file.  In order to be notified when the import is
 * complete, the calling class must implement this interface.  Currently, the only
 * user of this interface is the ImportActivity class.
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
 * This inteface provides a means for the ImportExportHandler to return a list of
 * imported sites from an import file.  In order to be notified when the import is
 * complete, the calling class must implement this interface.
 * @author Jeffrey T. Darlington
 * @version 1.3.0
 * @since 1.3.0
 */
public interface ImportListener {

	/**
	 * This method is called once the ImportExportHandler has successfully imported a
	 * list of site parameters.  The implementor may take the list of sites and perform
	 * whatever operation is required upon them.
	 * @param importedSites An Object array containing the list of SiteParameter objects.
	 * Note that this is an Object array, not an arry of SiteParameter objects; each
	 * object must be cast before it can be used.  Also note that the array should be
	 * tested to make sure it isn't null and does not contain zero items.
	 */
	public abstract void onSitesImported(Object[] importedSites);
	
}
