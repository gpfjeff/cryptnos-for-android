/* HelpActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          December 10, 2009
 * PROJECT:       Cryptnos (Android)
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This activity displays an individual help item.  It is launched from the
 * HelpMenuActivity, which will pass it a resource ID of the help text via
 * an Intent.  This activity is actually pretty dumb; aside from a static
 * TextView that basically says "Tap Back to return to the help menu", it
 * just displays whatever text it's been told to display.
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
import android.os.Bundle;
import android.widget.TextView;

/**
 * This activity displays an individual help item.  It is launched from the
 * HelpMenuActivity, which will pass it a resource ID of the help text via
 * an Intent.  This activity is actually pretty dumb; aside from a static
 * TextView that basically says "Tap Back to return to the help menu", it
 * just displays whatever text it's been told to display.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class HelpActivity extends Activity {

	/** The TextView that will contain the main body of our help text. */
	TextView helpText = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.help_layout);
        // Get a reference to the main help label:
        helpText = (TextView)findViewById(R.id.labelHelp);
        // Find out which help text we're supposed to display by looking
        // at the bundle passed to us.  It ought to contain an integer
        // value corresponding to the ID of the help text we're supposed
        // to display.
        Bundle extras = getIntent().getExtras();
        helpText.setText(extras.getInt("helptext"));
    }
    
}
