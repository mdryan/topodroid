/* @file PhotoEditDialog.java
 *
 * @author marco corvi
 * @date july 2012
 *
 * @brief TopoDroid photo edit dialog 
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.DistoX;

import android.app.Dialog;
import android.os.Bundle;

import android.content.Intent;
import android.content.Context;
import android.net.Uri;

import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnKeyListener;
import android.view.KeyEvent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.util.Log;

public class PhotoEditDialog extends MyDialog
                             implements View.OnClickListener
{
  private PhotoActivity mParent;
  private PhotoInfo mPhoto;
  private String mFilename;

  private EditText mETcomment;  // photo comment
  private ImageView mIVimage;   // photo image
  private Button   mButtonOK;
  private Button   mButtonDelete;
  // private Button   mButtonCancel;

  /**
   * @param context   context
   */
  PhotoEditDialog( Context context, PhotoActivity parent, PhotoInfo photo, String filename )
  {
    super( context, R.string.PhotoEditDialog );
    mParent = parent;
    mPhoto  = photo;
    mFilename = filename;
    // TDLog.Log(TDLog.LOG_PHOTO, "PhotoEditDialog " + mFilename);
  }

// -------------------------------------------------------------------
  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    // TDLog.Log( TDLog.LOG_PHOTO, "onCreate" );
    initLayout( R.layout.photo_edit_dialog, R.string.title_photo_comment );

    mIVimage      = (ImageView) findViewById( R.id.photo_image );
    mETcomment    = (EditText) findViewById( R.id.photo_comment );
    mButtonOK     = (Button) findViewById( R.id.photo_ok );
    mButtonDelete = (Button) findViewById( R.id.photo_delete );
    // mButtonCancel = (Button) findViewById( R.id.photo_cancel );

    // public String mPhoto.mDate;
    if ( mPhoto.mComment != null ) {
      mETcomment.setText( mPhoto.mComment );
    }
    try {
      // public String getSurveyJpgFile( String name )
      // public String name;    // photo filename without extension ".jpg" and survey prefix dir
      // String filename = TopoDroidApp.APP_FOTO_PATH + mPhoto.name + ".jpg";
      BitmapFactory.Options bfo = new BitmapFactory.Options();
      bfo.inJustDecodeBounds = true;
      BitmapFactory.decodeFile( mFilename, bfo );
      int required_size = TDSetting.mThumbSize;
      int scale = 1;
      while ( bfo.outWidth/scale/2 > required_size || bfo.outHeight/scale/2 > required_size ) {
        scale *= 2;
      }
      bfo.inJustDecodeBounds = false;
      bfo.inSampleSize = scale;
      Bitmap image = BitmapFactory.decodeFile( mFilename, bfo );
      if ( image != null ) {
        int w2 = image.getWidth() / 8;
        int h2 = image.getHeight() / 8;
        Bitmap image2 = Bitmap.createScaledBitmap( image, w2, h2, true );
        if ( image2 != null ) {
          mIVimage.setImageBitmap( image2 );
          // mIVimage.setHeight( h2 );
          // mIVimage.setWidth( w2 );
          mIVimage.setOnClickListener( this );
        } else {
          mIVimage.setVisibility( View.GONE );
        }
      }
    } catch ( OutOfMemoryError e ) {
      Toast.makeText( mParent, R.string.null_bitmap, Toast.LENGTH_SHORT ).show();
    }

    mButtonOK.setOnClickListener( this );
    mButtonDelete.setOnClickListener( this );
    // mButtonCancel.setOnClickListener( this );
  }

  public void onClick(View v) 
  {
    // Button b = (Button) v;
    // TDLog.Log(  TDLog.LOG_INPUT, "PhotoEditDialog onClick() " + b.getText().toString() );

    if ( v.getId() == R.id.photo_ok ) {
      if ( mETcomment.getText() == null ) {
        mParent.updatePhoto( mPhoto, "" );
      } else {
        mParent.updatePhoto( mPhoto, mETcomment.getText().toString() );
      }
    } else if ( v.getId() == R.id.photo_delete ) {
      mParent.dropPhoto( mPhoto );
    } else if ( v.getId() == R.id.photo_image ) {
      mParent.viewPhoto( mFilename );
      return;
    }
    dismiss();
  }

}

