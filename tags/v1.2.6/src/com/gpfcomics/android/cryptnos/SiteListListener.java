/* SiteListListener.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          March 30, 2010
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This interface provides the backward communication between the
 * CryptnosApplication class and any Activity that requires access to the
 * master site list.  If an Activity needs that list, it should request it
 * from the CryptnosApplication by calling its requestSiteList() method.
 * It should pass in itself (to control the generated ProgressDialog) as well
 * as a SiteListListener, which will receive the list once it is ready to
 * use.  The SiteListListener is usually the same as the calling Activity
 * (rather, the Activity implements this interface), but it doesn't necessarily
 * have to be.
 * 
 * When the list is ready, the CryptnosApplication will call the
 * onSiteListReady() method in this interface.  The implementing class will
 * then take the list and do whatever it needs to do with it:  put it in a
 * ListActivity, create a Dialog with checkboxes, etc.  What the implementor
 * wants to do is up it them.
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

/**
 * This interface provides the backward communication between the
 * CryptnosApplication class and any Activity that requires access to the
 * master site list.  If an Activity needs that list, it should request it
 * from the CryptnosApplication by calling its requestSiteList() method.
 * It should pass in itself (to control the generated ProgressDialog) as well
 * as a SiteListListener, which will receive the list once it is ready to
 * use.  The SiteListListener is usually the same as the calling Activity
 * (rather, the Activity implements this interface), but it doesn't necessarily
 * have to be.
 * 
 * When the list is ready, the CryptnosApplication will call the
 * onSiteListReady() method in this interface.  The implementing class will
 * then take the list and do whatever it needs to do with it:  put it in a
 * ListActivity, create a Dialog with checkboxes, etc.  What the implementor
 * wants to do is up it them.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public interface SiteListListener {

	/**
	 * This method is called by the CryptnosApplication whenever the site
	 * list has been built and is ready to use.  The listener will receive
	 * the list, presented as a String array, and do whatever it needs to
	 * with it after that.
	 * @param siteList A String array containing the site list.  Note that
	 * this could be null if an error occurs.
	 */
	public abstract void onSiteListReady(String[] siteList);
	
}
