/* AboutActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          December 9, 2009
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This activity serves as a simple "about box" for Cryptnos.  It displays
 * version, author, and license information about the entire application.
 * 
 * UPDATES FOR 1.1.1:  Changed icon to full logo.
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

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

/**
 * The about "screen" for Cryptnos.  Displays version, copyright, and license
 * information about the entire application. 
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class AboutActivity extends Activity {

	/** The TextView that contains the About activity's header. */
	private TextView header = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);
        
        // We want to put the version number (android:versionName from the
        // manifest) into the About header.  This is not quite as simple as
        // it sounds.  In order to do this, we first need to get a hold of
        // the header TextView so we can modify it:
        header = (TextView)findViewById(R.id.about_header);
        
        // This seems like a round-about way of doing things, but get the
        // PackageInfo for the Cryptnos application package.  From that we
        // can get the version name string.  Append that to the current value
        // of the header string, which is populated from the strings.xml
        // file like everything else.  If that fails for some reason, print
        // something else to indicate the error.  The advantage here is that
        // we can update the version string in one place (the manifest) and
        // that will propagate here with no code changes.
        try {
	        PackageInfo info =
	        	this.getPackageManager().getPackageInfo(this.getPackageName(),
        			PackageManager.GET_META_DATA);
	        header.setText(header.getText().toString().concat(" " + info.versionName)); 
        } catch (Exception e) {
        	header.setText(header.getText().toString().concat(" ???"));
        }
    }
    
}
