package com.gpfcomics.android.cryptnos;

import java.io.File;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

public class QRViewActivity extends Activity {

	private String pathToImage = null;
	
	private ImageView imgQRCode = null;
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.qrview_layout);
        
        imgQRCode = (ImageView)findViewById(R.id.imgQRCode);
        
        try {
        	Bundle extras = getIntent().getExtras();
            if (extras != null)
            {
            	pathToImage = extras.getString("qrcode_file");
            	imgQRCode.setImageBitmap(BitmapFactory.decodeFile(pathToImage, null));
            	// TODO:  Need to handle orientation change, restore image
            } else finish();
        } catch (Exception e) {
        	finish();
        }
    }
    
    @Override
    protected void onStop()
    {
    	try {
    		File theFile = new File(pathToImage);
    		theFile.delete();
    	} catch (Exception e) {}
    	super.onStop();
    }
    
}
