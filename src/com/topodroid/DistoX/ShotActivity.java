/* @file ShotActivity.java
 *
 * @author marco corvi
 * @date may 2012
 *
 * @brief TopoDroid survey shots management
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.DistoX;

import java.io.File;
import java.io.IOException;
// import java.io.EOFException;
// import java.io.DataInputStream;
// import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
// import java.io.FileWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;

import android.os.Parcelable;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask;
import android.os.Debug;

// import android.os.SystemClock;
// import android.os.PowerManager;
import android.content.res.Resources;

import android.graphics.Rect;

import android.app.Application;
import android.app.Activity;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
// import android.content.DialogInterface.OnCancelListener;
// import android.content.DialogInterface.OnDismissListener;
// import android.content.res.ColorStateList;

import android.provider.Settings.System;

// import android.location.LocationManager;

import android.content.Context;
import android.content.Intent;

import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.app.Dialog;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.LinearLayout;

import android.view.WindowManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.KeyEvent;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;

import android.provider.MediaStore;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Paint.FontMetrics;

import android.net.Uri;

import android.util.Log;

public class ShotActivity extends Activity
                          implements OnItemClickListener
                        , OnItemLongClickListener
                        , OnClickListener
                        , OnLongClickListener
                        , ILister
                        , INewPlot
{
  final static int BTN_DOWNLOAD = 0;
  final static int BTN_PLOT     = 3;
  final static int BTN_AZIMUTH  = 7;

  private static int izons[] = {
                        R.drawable.iz_download,
                        R.drawable.iz_bt,
                        R.drawable.iz_mode,
                        R.drawable.iz_plot,
                        R.drawable.iz_note,
                        R.drawable.iz_plus,
                        R.drawable.iz_station,
                        R.drawable.iz_dial
                      };

  private static int izonsno[] = {
                        0,
                        0,
                        0,
                        R.drawable.iz_plot, // TODO_IZ
                        0,
                        0,
                        0
                      };

  private static int menus[] = {
                        R.string.menu_survey,
                        R.string.menu_recover,
                        R.string.menu_photo,
                        R.string.menu_sensor,
                        R.string.menu_3d,
                        R.string.menu_distox,
                        R.string.menu_options,
                        R.string.menu_help
                     };

  private static int help_icons[] = {
                          R.string.help_download,
                          R.string.help_remote,
                          R.string.help_display,
                          R.string.help_plot,
                          R.string.help_note,
                          R.string.help_add_shot,
                          R.string.help_current_station,
                          R.string.help_azimuth,
                        };
   private static int help_menus[] = {
                          R.string.help_survey_info,
                          R.string.help_undelete,
                          R.string.help_photo,
                          R.string.help_sensor,
                          R.string.help_3d,
                          R.string.help_device,
                          R.string.help_prefs,
                          R.string.help_help
                      };

  private TopoDroidApp mApp;
  private DataDownloader mDataDownloader;

  private static final int SENSOR_ACTIVITY_REQUEST_CODE = 1;
  // private static final int EXTERNAL_ACTIVITY_REQUEST_CODE = 2;
  private static final int INFO_ACTIVITY_REQUEST_CODE = 3;
  static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

  // private static final int REQUEST_DEVICE = 1;
  private static final int REQUEST_ENABLE_BT = 2;

  boolean mSplay = true;  //!< whether to hide splay shots
  boolean mLeg   = true;  //!< whether to hide leg extra shots
  boolean mBlank = false; //!< whether to hide blank shots
  // private Bundle mSavedState = null;
  // long mSecondLastShotId = 0L;
  // long mLastShotId;
  String mRecentPlot     = null;
  long   mRecentPlotType = PlotInfo.PLOT_PLAN;

  int mButtonSize = 42;
  private MyButton[] mButton1;
  private int mNrButton1 = 0;

  public void setRefAzimuth( float azimuth, long fixed_extend )
  {
    mApp.mFixedExtend = fixed_extend;
    mApp.mRefAzimuth  = azimuth;
    setRefAzimuthButton();
  }

  public void setRefAzimuthButton()
  {
    if ( ! TDSetting.mLevelOverNormal ) return;
    if ( mApp.mFixedExtend == 0 ) {
      android.graphics.Matrix m = new android.graphics.Matrix();
      m.postRotate( mApp.mRefAzimuth - 90 );
      // if ( mBMdial != null ) // extra care !!!
      {
        Bitmap bm1 = Bitmap.createScaledBitmap( mBMdial, mButtonSize, mButtonSize, true );
        Bitmap bm2 = Bitmap.createBitmap( bm1, 0, 0, mButtonSize, mButtonSize, m, true);
        mButton1[ BTN_AZIMUTH ].setBackgroundDrawable( new BitmapDrawable( getResources(), bm2 ) );
      }
    } else if ( mApp.mFixedExtend == -1L ) {
      mButton1[ BTN_AZIMUTH ].setBackgroundDrawable( mBMleft );
    } else {
      mButton1[ BTN_AZIMUTH ].setBackgroundDrawable( mBMright );
    } 
  }

  long secondLastShotId() { return mApp.mSecondLastShotId; }

  private ListView mList;
  // private int mListPos = -1;
  // private int mListTop = 0;
  private DistoXDBlockAdapter   mDataAdapter;
  private ArrayList< String > mShowSplay;


  // private long mLastExtend; // id of the last-extend-ed splay 

  // private static final String LIST_STATE = "listState";
  // private int mFirstPos = -1;  
  // private int mScroll   = 0;
  // private int mSavePos  = -1;  // shot entry position
  private int mShotPos  = -1;  // shot entry position
  private int mPrevPos  = 0;   // prev shot entry position
  private int mNextPos  = 0;   // next shot entry position
  // private TextView mSaveTextView = null;

  static long   mSensorId;
  static long   mPhotoId;
  static String mComment;
  static long   mShotId;   // photo/sensor shot id

  // ConnHandler mHandler;

  TopoDroidApp getApp() { return mApp; }

  // -------------------------------------------------------------------
  // FXIME ok only for numbers
  // String getNextStationName()
  // {
  //   return mApp.mData.getNextStationName( mApp.mSID );
  // }

  private void computeMeans( List<DistoXDBlock> list )
  {
    TopoDroidApp.mAccelerationMean = 0.0f;
    TopoDroidApp.mMagneticMean     = 0.0f;
    TopoDroidApp.mDipMean          = 0.0f;
    int size = list.size();
    if ( size > 0 ) {
      int cnt = 0;
      for ( DistoXDBlock blk : list ) {
        if ( blk.mAcceleration > 10.0 ) {
          TopoDroidApp.mAccelerationMean += blk.mAcceleration;
          TopoDroidApp.mMagneticMean     += blk.mMagnetic;
          TopoDroidApp.mDipMean          += blk.mDip;
          ++ cnt;
        }
      }
      if ( cnt > 0 ) {
        TopoDroidApp.mAccelerationMean /= cnt;
        TopoDroidApp.mMagneticMean     /= cnt;
        TopoDroidApp.mDipMean          /= cnt;
      }
      // Log.v( TopoDroidApp.TAG, "Acc " + TopoDroidApp.mAccelerationMean + " Mag " + TopoDroidApp.mMagneticMean 
      //                          + " Dip " + TopoDroidApp.mDipMean );
    }
  }

  @Override
  public void refreshDisplay( int nr, boolean toast ) 
  {
    setConnectionStatus( mDataDownloader.getStatus() );
    if ( nr >= 0 ) {
      if ( nr > 0 ) {
        // mLastShotId = mApp.mData.getLastShotId( mApp.mSID );
        updateDisplay( );
      }
      if ( toast ) {
        Toast.makeText( this, String.format( getString(R.string.read_data), nr ), Toast.LENGTH_SHORT ).show();
        // Toast.makeText( this, getString(R.string.read_) + nr + getString(R.string.data), Toast.LENGTH_SHORT ).show();
      }
    } else if ( nr < 0 ) {
      if ( toast ) {
        // Toast.makeText( this, getString(R.string.read_fail_with_code) + nr, Toast.LENGTH_SHORT ).show();
        Toast.makeText( this, mApp.DistoXConnectionError[ -nr ], Toast.LENGTH_SHORT ).show();
      }
    }
  }
    
  public void updateDisplay( )
  {
    // Log.v( "DistoX", "update Display() " );

    DataHelper data = mApp.mData;
    if ( data != null && mApp.mSID >= 0 ) {
      List<DistoXDBlock> list = data.selectAllShots( mApp.mSID, TopoDroidApp.STATUS_NORMAL );
      if ( list.size() > 4 ) computeMeans( list );

      List< PhotoInfo > photos = data.selectAllPhotos( mApp.mSID, TopoDroidApp.STATUS_NORMAL );
      // TDLog.Log( TDLog.LOG_SHOT, "updateDisplay() shot list size " + list.size() );
      // Log.v( TopoDroidApp.TAG, "updateDisplay() shot list size " + list.size() );
      updateShotList( list, photos );
      
      setTheTitle( );
    } else {
      Toast.makeText( this, R.string.no_survey, Toast.LENGTH_SHORT ).show();
    }
  }

  void setTheTitle()
  {
    setTitle( mApp.getConnectionStateTitleStr() + mApp.mySurvey );
    // FIXME setTitleColor( TDConst.COLOR_NORMAL );
  }

  boolean isCurrentStationName( String st ) { return mApp.isCurrentStationName( st ); }
  void setCurrentStationName( String st ) 
  { 
    mSkipItemClick = true;
    mApp.setCurrentStationName( st ); 
    updateDisplay( );
  }

  // add a block to the adapter (ILister interface)
  // called by the RFcommThread after receiving a data packet
  @Override
  public void updateBlockList( long blk_id )
  {
    DistoXDBlock blk = mApp.mData.selectShot( blk_id, mApp.mSID );
    if ( blk != null ) {
      updateBlockList( blk );
    }
  }

  @Override
  public void updateBlockList( DistoXDBlock blk )
  {
    // FIXME MULTIPLE LIST
    // Log.v("DistoX", "update block list: " + blk.mLength + " " + blk.mBearing + " " + blk.mClino );
    if ( mDataAdapter != null ) {
      mDataAdapter.addDataBlock( blk );
      mApp.assignStations( mDataAdapter.mItems );
      mList.post( new Runnable() {
        @Override public void run() {
          mDataAdapter.notifyDataSetChanged();
        }
      } );

      Message msg = Message.obtain();
      msg.what = MSG_ADD_BLK;
      mListItemsHandler.sendMessage( msg );
    }
  }

  void setShowIds( boolean show ) { mDataAdapter.show_ids = show; }

  boolean getShowIds() { return mDataAdapter.show_ids; }

  private void updateShotList( List<DistoXDBlock> list, List< PhotoInfo > photos )
  {
    TDLog.Log( TDLog.LOG_SHOT, "updateShotList shots " + list.size() + " photos " + photos.size() );
    // Log.v( "DistoX", "update Shot List shots " + list.size() + " photos " + photos.size() );
    mDataAdapter.clear();
    mList.setAdapter( mDataAdapter );
    if ( list.size() == 0 ) {
      // Toast.makeText( this, R.string.no_shots, Toast.LENGTH_SHORT ).show();
      return;
    }
    processShotList( list );
    mDataAdapter.reviseBlockWithPhotos( photos );
  }

  private void processShotList( List<DistoXDBlock> list )
  {
    // Log.v("DistoX", "process shot list");
    DistoXDBlock prev = null;
    boolean prev_is_leg = false;
    for ( DistoXDBlock item : list ) {
      DistoXDBlock cur = item;
      int t = cur.type();

      // TDLog.Log( TDLog.LOG_SHOT, "item " + cur.mLength + " " + cur.mBearing + " " + cur.mClino );

      if ( cur.mType == DistoXDBlock.BLOCK_SEC_LEG || cur.relativeDistance( prev ) ) {
        // Log.v( "DistoX", "item close " + cur.type() + " " + cur.mLength + " " + cur.mBearing + " " + cur.mClino );
        if ( cur.mType == DistoXDBlock.BLOCK_BLANK ) {   // FIXME 20140612
          cur.mType = DistoXDBlock.BLOCK_SEC_LEG;
          mApp.mData.updateShotLeg( cur.mId, mApp.mSID, 1L, true ); // cur.mType ); // FIXME 20140616
        }

        // if ( prev != null && prev.mType == DistoXDBlock.BLOCK_BLANK ) prev.mType = DistoXDBlock.BLOCK_BLANK_LEG;
        if ( prev != null ) prev.setTypeBlankLeg();

        if ( mLeg ) { // flag: hide leg extra shots
          // TDLog.Log( TDLog.LOG_SHOT, "close distance");

          if ( mBlank && prev != null && prev.isTypeBlank() ) {
            // prev was skipped: draw it now
            if ( ! prev_is_leg ) {
              cur = prev;
              prev_is_leg = true;
            } else {
              continue;
            }
          } else {
            continue;
          }
        } else { // do not hide extra leg-shots
          if ( mBlank && prev != null && prev.isTypeBlank() ) {
            if ( ! prev_is_leg ) {
              mDataAdapter.add( prev );
              prev_is_leg = true;
            } else {
              /* nothing */
            }
          } else {
            /* nothing */
          }
        }
      } else {
        // Log.v( "DistoX", "item not close " + cur.type() + " " + cur.mLength + " " + cur.mBearing + " " + cur.mClino );
        // TDLog.Log( TDLog.LOG_SHOT, "not close distance");
        prev_is_leg = false;
        if ( DistoXDBlock.isTypeBlank(t) ) {
          prev = cur;
          if ( mBlank ) continue;
        } else if ( t == DistoXDBlock.BLOCK_SPLAY ) {
          prev = null;
          if ( mSplay ) { // do hide splays, except those that are shown.
            // boolean skip = true;
            // for ( String st : mShowSplay ) {
            //   if ( st.equals( cur.mFrom ) ) { skip = false; break; }
            // }
            // if ( skip ) continue;
            if ( ! showSplaysContains( cur.mFrom ) ) continue;
          }
        } else { // t == DistoXDBlock.BLOCK_MAIN_LEG
          prev = cur;
        }
      }
      // TDLog.Log( TDLog.LOG_SHOT, "adapter add " + cur.mLength + " " + cur.mBearing + " " + cur.mClino );
      // Log.v( "DistoX", "shot adapter add " + cur.mLength + " " + cur.mBearing + " " + cur.mClino );
      mDataAdapter.add( cur );
    }
  }

  // ---------------------------------------------------------------
  // list items click

  @Override 
  public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id)
  {
    if ( closeMenu() ) return true;
    if ( CutNPaste.dismissPopupBT() ) return true;

    TDLog.Log( TDLog.LOG_INPUT, "ShotActivity onItemLongClick id " + id);
    DistoXDBlock blk = mDataAdapter.get(pos);
    mShotId = blk.mId;
    if ( TDSetting.mLevelOverNormal ) {
      (new PhotoSensorsDialog(this, this, blk ) ).show();
    } else {
      (new ShotDeleteDialog( this, this, blk ) ).show();
    }
    return true;
  }

  private boolean mSkipItemClick = false;

  @Override 
  public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
  {
    if ( CutNPaste.dismissPopupBT() ) return;
    if ( mSkipItemClick ) {
      mSkipItemClick = false;
      return;
    }
    if ( mMenu == (ListView)parent ) {
      view.setBackgroundColor( 0xffff9900 ); // R.color.menu_highlight
      view.invalidate();
      try { Thread.sleep( 200 ); } catch( InterruptedException e ) { }
      view.setBackgroundColor( 0xff333333 ); // R.color.menu_background
      closeMenu();
      int p = 0;
      if ( p++ == pos ) { // SURVEY ACTIVITY
        Intent intent = new Intent( this, SurveyActivity.class );
        intent.putExtra( TopoDroidTag.TOPODROID_SURVEY,  0 ); // mustOpen 
        intent.putExtra( TopoDroidTag.TOPODROID_OLDSID, -1 ); // old_sid 
        intent.putExtra( TopoDroidTag.TOPODROID_OLDID,  -1 ); // old_id 
        startActivityForResult( intent, INFO_ACTIVITY_REQUEST_CODE );
      // } else if ( TDSetting.mLevelOverBasic && p++ == pos ) { // CURRENT STATION
      //   (new CurrentStationDialog( this, this, mApp )).show();

      } else if ( TDSetting.mLevelOverBasic && p++ == pos ) { // RECOVER
        (new UndeleteDialog(this, this, mApp.mData, mApp.mSID ) ).show();
        updateDisplay( );
      } else if ( TDSetting.mLevelOverNormal && p++ == pos ) { // PHOTO
        startActivity( new Intent( this, PhotoActivity.class ) );
      } else if ( TDSetting.mLevelOverNormal && p++ == pos ) { // SENSORS
        startActivity( new Intent( this, SensorListActivity.class ) );
      } else if ( TDSetting.mLevelOverBasic && p++ == pos ) { // 3D
        mApp.exportSurveyAsTh(); // make sure to have survey exported as therion
        try {
          Intent intent = new Intent( "Cave3D.intent.action.Launch" );
          intent.putExtra( "survey", TDPath.getSurveyThFile( mApp.mySurvey ) );
          startActivity( intent );
        } catch ( ActivityNotFoundException e ) {
          Toast.makeText( this, R.string.no_cave3d, Toast.LENGTH_SHORT ).show();
        }
      } else if ( TDSetting.mLevelOverNormal && p++ == pos ) { // DEVICE
        if ( mApp.mBTAdapter.isEnabled() ) {
          startActivity( new Intent( Intent.ACTION_EDIT ).setClass( this, DeviceActivity.class ) );
        }
      } else  if ( p++ == pos ) { // OPTIONS
        Intent intent = new Intent( this, TopoDroidPreferences.class );
        intent.putExtra( TopoDroidPreferences.PREF_CATEGORY, TopoDroidPreferences.PREF_CATEGORY_SURVEY );
        startActivity( intent );
      } else if ( p++ == pos ) { // HELP
        // int nn = mNrButton1; //  + ( TopoDroidApp.mLevelOverNormal ?  mNrButton2 : 0 );
        (new HelpDialog(this, izons, menus, help_icons, help_menus, mNrButton1, 8 ) ).show();
      }
      return;
    }
    if ( closeMenu() ) return;

    TDLog.Log( TDLog.LOG_INPUT, "ShotActivity onItemClick id " + id);
    DistoXDBlock blk = mDataAdapter.get(pos);

    mShotPos = pos;
    // mSavePos = pos;
    // mFirstPos = mList.getFirstVisiblePosition();
    // mScroll   = mList.getScrollY();
    // mSaveTextView = (TextView)view;

    // TextView tv = (TextView)view;
    // String msg = tv.getText().toString();
    // String[] st = msg.split( " ", 6 );
    // String data = st[2] + " " + st[3] + " " + st[4];
      
    DistoXDBlock prevBlock = null;
    DistoXDBlock nextBlock = null;
    // if ( blk.type() == DistoXDBlock.BLOCK_BLANK ) {
      // prevBlock = mApp.mData.selectPreviousLegShot( blk.mId, mApp.mSID );
      prevBlock = getPreviousLegShot( blk, false );
      nextBlock = getNextLegShot( blk, false );
      // if ( prevBlock != null ) {
      //   TDLog.Log( TDLog.LOG_SHOT, "prev leg " + prevBlock.mFrom + " " + prevBlock.mTo );
      // }
    // }
    (new ShotDialog( this, this, pos, blk, prevBlock, nextBlock )).show();
  }

// ---------------------------------------------------------------

  void askPhotoComment( )
  {
    (new PhotoCommentDialog(this, this) ).show();
  }


  void doTakePhoto( String comment )
  {
    mComment = comment;
    mPhotoId = mApp.mData.nextPhotoId( mApp.mSID );

    // imageFile := PHOTO_DIR / surveyId / photoId .jpg
    File imagefile = new File( TDPath.getSurveyJpgFile( mApp.mySurvey, Long.toString(mPhotoId) ) );
    // TDLog.Log( TDLog.LOG_SHOT, "photo " + imagefile.toString() );
    try {
      Uri outfileuri = Uri.fromFile( imagefile );
      Intent intent = new Intent( android.provider.MediaStore.ACTION_IMAGE_CAPTURE );
      intent.putExtra( MediaStore.EXTRA_OUTPUT, outfileuri );
      intent.putExtra( "outputFormat", Bitmap.CompressFormat.JPEG.toString() );
      startActivityForResult( intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE );
    } catch ( ActivityNotFoundException e ) {
      Toast.makeText( this, "No image capture mApp", Toast.LENGTH_SHORT ).show();
    }
  }

  void askSensor( )
  {
    mSensorId = mApp.mData.nextSensorId( mApp.mSID );
    TDLog.Log( TDLog.LOG_SENSOR, "sensor " + mSensorId );
    Intent intent = new Intent( this, SensorActivity.class );
    startActivityForResult( intent, SENSOR_ACTIVITY_REQUEST_CODE );
  }

  // void askExternal( )
  // {
  //   mSensorId = mApp.mData.nextSensorId( mApp.mSID );
  //   TDLog.Log( TDLog.LOG_SENSOR, "sensor " + mSensorId );
  //   Intent intent = new Intent( this, ExternalActivity.class );
  //   startActivityForResult( intent, EXTERNAL_ACTIVITY_REQUEST_CODE );
  // }

  void askShot( )
  {
    // mSecondLastShotId = mApp.lastShotId( );
    DistoXDBlock last_blk = null; // mApp.mData.selectLastLegShot( mApp.mSID );
    (new ShotNewDialog( this, mApp, this, last_blk, mShotId )).show();
  }

  // called by PhotoSensorDialog to split the survey
  //
  void askSurvey( )
  {
    TopoDroidAlertDialog.makeAlert( this, getResources(), R.string.survey_split,
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick( DialogInterface dialog, int btn ) {
          doSplitSurvey();
        }
    } );
  }

  void doSplitSurvey()
  {
    long old_sid = mApp.mSID;
    long old_id  = mShotId;
    // Log.v( TopoDroidApp.TAG, "split survey " + old_sid + " " + old_id );
    if ( mApp.mShotActivity != null ) {
      mApp.mShotActivity.finish();
      mApp.mShotActivity = null;
    }
    if ( mApp.mSurveyActivity != null ) {
      mApp.mSurveyActivity.finish();
      mApp.mSurveyActivity = null;
    }
    mApp.mActivity.startSplitSurvey( old_sid, old_id ); // SPLIT SURVEY
  }

  void doDeleteShot( long id )
  {
    mApp.mData.deleteShot( id, mApp.mSID, true );
    updateDisplay( ); 
  }

  void insertPhoto( )
  {
    // long shotid = 0;
    mApp.mData.insertPhoto( mApp.mSID, mPhotoId, mShotId, "", TopoDroidUtil.currentDate(), mComment ); // FIXME TITLE has to go
    // FIXME NOTIFY ? no
  }

  // void deletePhoto( PhotoInfo photo ) 
  // {
  //   mApp.mData.deletePhoto( mApp.mSID, photo.id );
  //   File imagefile = new File( mApp.getSurveyJpgFile( Long.toString(photo.id) ) );
  //   try {
  //     imagefile.delete();
  //   } catch ( IOException e ) { }
  // }

  @Override
  protected void onActivityResult( int reqCode, int resCode, Intent data )
  {
    TDLog.Log( TDLog.LOG_ERR, "on Activity Result: request " + reqCode + " result " + resCode );
    switch ( reqCode ) {
      case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
        mApp.resetLocale();
        if ( resCode == Activity.RESULT_OK ) { // RESULT_OK = -1 (0xffffffff)
          // (new PhotoCommentDialog(this, this) ).show();
          insertPhoto();
        } else {
          // mApp.mData.deletePhoto( mApp.mSID, mPhotoId );
        }
        break;
      case SENSOR_ACTIVITY_REQUEST_CODE:
      // case EXTERNAL_ACTIVITY_REQUEST_CODE:
        if ( resCode == Activity.RESULT_OK ) {
          Bundle extras = data.getExtras();
          String type  = extras.getString( TopoDroidTag.TOPODROID_SENSOR_TYPE );
          String value = extras.getString( TopoDroidTag.TOPODROID_SENSOR_VALUE );
          String comment = extras.getString( TopoDroidTag.TOPODROID_SENSOR_COMMENT );
          TDLog.Log( TDLog.LOG_SENSOR, "insert sensor " + type + " " + value + " " + comment );

          mApp.mData.insertSensor( mApp.mSID, mSensorId, mShotId, "", 
                                  TopoDroidUtil.currentDate(),
                                  comment,
                                  type,
                                  value );
          // FIXME NOTIFY ? no
        }
        break;
      case INFO_ACTIVITY_REQUEST_CODE:
        if ( resCode == Activity.RESULT_OK ) {
          finish();
        }
        break;
    }
  }

  // ---------------------------------------------------------------
  // private Button mButtonHelp;
  HorizontalListView mListView;
  HorizontalButtonView mButtonView1;
  HorizontalButtonView mButtonView2;
  ListView   mMenu = null;
  Button     mImage;
  // ArrayAdapter< String > mMenuAdapter;
  MyMenuAdapter mMenuAdapter;
  boolean onMenu = false;

  BitmapDrawable mBMdownload;
  BitmapDrawable mBMdownload_on;
  BitmapDrawable mBMdownload_wait;
  // BitmapDrawable mBMadd;
  BitmapDrawable mBMplot;
  Bitmap mBMdial;
  BitmapDrawable mBMplot_no;
  BitmapDrawable mBMleft;
  BitmapDrawable mBMright;

  private Handler mListItemsHandler;
  static final int MSG_ADD_BLK = 1;
  
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate( savedInstanceState );
    setContentView( R.layout.shot_activity );
    mApp = (TopoDroidApp) getApplication();
    mApp.mShotActivity = this; // FIXME
    mDataDownloader = mApp.mDataDownloader; // new DataDownloader( this, mApp );

    mShowSplay   = new ArrayList< String >();
    mDataAdapter = new DistoXDBlockAdapter( this, this, R.layout.row, new ArrayList<DistoXDBlock>() );

    mListItemsHandler = new Handler() {
      @Override
      public void handleMessage( Message msg ) {
        switch (msg.what) {
        case MSG_ADD_BLK:
          mDataAdapter.notifyDataSetChanged();
          break;
        }
        super.handleMessage( msg );
      }
    };

    mListView = (HorizontalListView) findViewById(R.id.listview);
    mButtonSize = mApp.setListViewHeight( mListView );

    mNrButton1 = TDSetting.mLevelOverNormal ? 8 : ( TDSetting.mLevelOverBasic ? 6 : 5 );
    mButton1 = new MyButton[ mNrButton1 ];
    for ( int k=0; k<mNrButton1; ++k ) {
      mButton1[k] = new MyButton( this, this, mButtonSize, izons[k], 0 );
      if ( k == BTN_DOWNLOAD ) { mBMdownload = mButton1[k].mBitmap; }
      else if ( k == BTN_PLOT ) { mBMplot = mButton1[k].mBitmap; }
    }
    mBMdial          = BitmapFactory.decodeResource( getResources(), R.drawable.iz_dial );
    mBMplot_no       = MyButton.getButtonBackground( mApp, mButtonSize, R.drawable.iz_plot_no );
    mBMdownload_on   = MyButton.getButtonBackground( mApp, mButtonSize, R.drawable.iz_download_on );
    mBMdownload_wait = MyButton.getButtonBackground( mApp, mButtonSize, R.drawable.iz_download_wait );
    mBMleft          = MyButton.getButtonBackground( mApp, mButtonSize, R.drawable.iz_left );
    mBMright         = MyButton.getButtonBackground( mApp, mButtonSize, R.drawable.iz_right );

    if ( TDSetting.mLevelOverBasic ) {
      // mButton1[ BTN_DOWNLOAD ].setOnLongClickListener( this );
      mButton1[ BTN_PLOT ].setOnLongClickListener( this );
    }

    mApp.resetRefAzimuth( 90 );
    // setRefAzimuthButton( ); // called by mApp.resetRefAzimuth

    mButtonView1 = new HorizontalButtonView( mButton1 );
    mListView.setAdapter( mButtonView1.mAdapter );

    mList = (ListView) findViewById(R.id.list);
    mList.setAdapter( mDataAdapter );
    mList.setOnItemClickListener( this );
    mList.setLongClickable( true );
    mList.setOnItemLongClickListener( this );
    mList.setDividerHeight( 2 );
    // mList.setSmoothScrollbarEnabled( true );
    // mList.setFastScrollAlwaysVisible( true ); // API-11
    // mList.setFastScrollEnabled( true );

    // restoreInstanceFromData();
    // mLastExtend = mApp.mData.getLastShotId( mApp.mSID );
    List<DistoXDBlock> list = mApp.mData.selectAllShots( mApp.mSID, TopoDroidApp.STATUS_NORMAL );
    // mSecondLastShotId = mApp.lastShotId( );

    mImage = (Button) findViewById( R.id.handle );
    mImage.setOnClickListener( this );

    // mImage.setBackgroundResource( 
    //   ( TDSetting.mSizeButtons == 2 )? R.drawable.ix_menu : R.drawable.ic_menu );
    MyButton.setButtonBackground( mApp, mImage, mButtonSize, R.drawable.iz_menu);

    mMenu = (ListView) findViewById( R.id.menu );
    setMenuAdapter();
    mMenu.setOnItemClickListener( this );
    mMenu.setVisibility( View.GONE );
    onMenu = false;

    // CutNPaste.dismissPopupBT();

    if ( mDataDownloader != null ) {
      mApp.registerLister( this );
    }
  }

  void enableSketchButton( boolean enabled )
  {
    mApp.mEnableZip = enabled;
    mButton1[ BTN_PLOT ].setEnabled( enabled ); // FIXME SKETCH BUTTON 
    mButton1[ BTN_PLOT ].setBackgroundDrawable( enabled ? mBMplot : mBMplot_no );
  }

  // @Override
  // public synchronized void onStart() 
  // {
  //   super.onStart();
  //   // Debug.startMethodTracing( "distox" );
  //   // Log.v( "DistoX", "Shot Activity onStart() " );
  // }

  @Override
  public synchronized void onDestroy() 
  {
    super.onDestroy();
    // Log.v("DistoX", "ShotActivity onDestroy()" );
    if ( mDataDownloader != null ) {
      mApp.unregisterLister( this );
      mDataDownloader.onStop();
    }
    mApp.disconnectRemoteDevice( false );

    if ( doubleBackHandler != null ) {
      doubleBackHandler.removeCallbacks( doubleBackRunnable );
    }
  }

  // @Override
  // public synchronized void onStop() 
  // {
  //   // Debug.stopMethodTracing( );
  //   super.onStop();
  //   // Log.v("DistoX", "ShotActivity onStop()" );
  // }

  @Override
  public synchronized void onPause() 
  {
    super.onPause();
    // Log.v("DistoX", "ShotActivity onPause()" );
    saveInstanceToData();

    // mApp.unregisterConnListener( mHandler );
    // if ( mApp.mComm != null ) { mApp.mComm.suspend(); }
    // FIXME NOTIFY unregister ILister
  }

  @Override
  public synchronized void onResume() 
  {
    super.onResume();
    // Log.v("DistoX", "ShotActivity onResume()" );

    // FIXME NOTIFY register ILister
    // if ( mApp.mComm != null ) { mApp.mComm.resume(); }
    // Log.v( "DistoX", "Shot Activity onResume()" );
    
    restoreInstanceFromData();
    updateDisplay( );

    if ( mDataDownloader != null ) mDataDownloader.onResume();

    // mApp.registerConnListener( mHandler );
    setConnectionStatus( mDataDownloader.getStatus() );
  }

  // --------------------------------------------------------------

  private boolean doubleBack = false;
  private Handler doubleBackHandler = new Handler();
  private Toast   doubleBackToast = null;

  private final Runnable doubleBackRunnable = new Runnable() {
    @Override 
    public void run() {
      doubleBack = false;
      if ( doubleBackToast != null ) doubleBackToast.cancel();
      doubleBackToast = null;
    }
  };

  @Override
  public void onBackPressed () // askClose
  {
    if ( closeMenu() ) return;
    if ( CutNPaste.dismissPopupBT() ) return;

    if ( doubleBack ) {
      if ( doubleBackToast != null ) doubleBackToast.cancel();
      doubleBackToast = null;
      super.onBackPressed();
      return;
    }
    doubleBack = true;
    doubleBackToast = Toast.makeText( this, R.string.double_back, Toast.LENGTH_SHORT );
    doubleBackToast.show();
    doubleBackHandler.postDelayed( doubleBackRunnable, 1000 );
  }

  // --------------------------------------------------------------

  // FIXME NOTIFY: the display mode is local - do not notify
  private void restoreInstanceFromData()
  { 
    String shots = mApp.mData.getValue( "DISTOX_SHOTS" );
    if ( shots != null ) {
      String[] vals = shots.split( " " );
      // FIXME assert( vals.length > 3 );
      mSplay  = vals[0].equals("1");
      mLeg    = vals[1].equals("1");
      mBlank  = vals[2].equals("1");
      setShowIds( vals[3].equals("1") );
      // Log.v("DistoX", "restore from data mSplay " + mSplay );
    }
  }
    
  private void saveInstanceToData()
  {
    mApp.mData.setValue( "DISTOX_SHOTS", 
      String.format("%d %d %d %d", mSplay?1:0, mLeg?1:0, mBlank?1:0, getShowIds()?1:0 ) );
    // Log.v("DistoX", "save to data mSplay " + mSplay );
  }

  void doBluetooth( Button b )
  {
    // TDLog.Log( TDLog.LOG_INPUT, "Reset button, mode " + TDSetting.mConnectionMode );
    if ( TDSetting.mLevelOverAdvanced && mApp.distoType() == Device.DISTO_X310 ) {
      if ( TDSetting.mConnectionMode == TDSetting.CONN_MODE_BATCH ) {
        mDataDownloader.setDownload( false );
        mDataDownloader.stopDownloadData();
        setConnectionStatus( mDataDownloader.getStatus() );
      }
      CutNPaste.showPopupBT( this, this, mApp, b );
      return;
    }
    mDataDownloader.setDownload( false );
    mDataDownloader.stopDownloadData();
    setConnectionStatus( mDataDownloader.getStatus() );
    mApp.resetComm();
    Toast.makeText(this, R.string.bt_reset, Toast.LENGTH_SHORT).show();
  }

  @Override 
  public boolean onLongClick( View view )
  {
    if ( closeMenu() ) return true;
    if ( CutNPaste.dismissPopupBT() ) return true;

    Button b = (Button)view;
    if ( b == mButton1[ BTN_PLOT ] ) {
      if ( mRecentPlot != null ) {
        startExistingPlot( mRecentPlot, mRecentPlotType );
      }
    // } else if ( b == mButton1[ BTN_DOWNLOAD ] ) {
    //   doBluetooth( b );
    }
    return true;
  } 

  @Override 
  public void onClick(View view)
  {
    if ( closeMenu() ) return;
    if ( CutNPaste.dismissPopupBT() ) return;

    Button b = (Button)view;
    if ( b == mImage ) {
      if ( mMenu.getVisibility() == View.VISIBLE ) {
        mMenu.setVisibility( View.GONE );
        onMenu = false;
      } else {
        mMenu.setVisibility( View.VISIBLE );
        onMenu = true;
      }
      return;
    }

    if ( b != null ) {
      Intent intent;

      int k1 = 0;
      // int k2 = 0;
      if ( k1 < mNrButton1 && b == mButton1[k1++] ) {        // DOWNLOAD
        setConnectionStatus( 2 ); // turn arrow orange
        // TDLog.Log( TDLog.LOG_INPUT, "Download button, mode " + TDSetting.mConnectionMode );
        mDataDownloader.toggleDownload();
        setConnectionStatus( mDataDownloader.getStatus() );
        mDataDownloader.doDataDownload( );
      } else if ( k1 < mNrButton1 && b == mButton1[k1++] ) { // BT RESET
        doBluetooth( b );
      } else if ( k1 < mNrButton1 && b == mButton1[k1++] ) { // DISPLAY 
        new ShotDisplayDialog( this, this ).show();
      } else if ( k1 < mNrButton1 && b == mButton1[k1++] ) { // SKETCH
        new PlotListDialog( this, this, mApp ).show();
      } else if ( k1 < mNrButton1 && b == mButton1[k1++] ) { // NOTE
        if ( mApp.mySurvey != null ) {
          (new DistoXAnnotations( this, mApp.mySurvey )).show();
        }

      } else if ( k1 < mNrButton1 && b == mButton1[k1++] ) { // ADD MANUAL SHOT
        if ( TDSetting.mLevelOverBasic ) {
          // mSecondLastShotId = mApp.lastShotId( );
          DistoXDBlock last_blk = mApp.mData.selectLastLegShot( mApp.mSID );
          // Log.v( "DistoX", "last blk: " + last_blk.toString() );
          (new ShotNewDialog( this, mApp, this, last_blk, -1L )).show();
        }
      } else if ( k1 < mNrButton1 && b == mButton1[k1++] ) { // STATIONS
        if ( TDSetting.mLevelOverNormal ) {
          (new CurrentStationDialog( this, this, mApp )).show();
          // ArrayList<DistoXDBlock> list = numberSplays(); // SPLAYS splays numbering no longer active
          // if ( list != null && list.size() > 0 ) {
          //   updateDisplay( );
          // }
        }
      } else if ( k1 < mNrButton1 && b == mButton1[k1++] ) { // AZIMUTH
        if ( TDSetting.mLevelOverNormal ) {
          if ( TDSetting.mAzimuthManual ) {
            setRefAzimuth( 0, - mApp.mFixedExtend );
          } else {
            (new AzimuthDialDialog( this, this, mApp.mRefAzimuth, mBMdial )).show();
          }
        }
      }
    }
  }

  // ------------------------------------------------------------------

  public boolean hasSurveyPlot( String name )
  {
    return mApp.mData.hasSurveyPlot( mApp.mSID, name+"p" );
  }
 
  public boolean hasSurveyStation( String start )
  {
    return mApp.mData.hasSurveyStation( mApp.mSID, start );
  }

  public void makeNewPlot( String name, String start )
  {
    // plot-id -1, status 0, azimuth 0.0f
    // long mPIDp = mApp.mData.insertPlot( mApp.mSID, -1L, name+"p",
    //              PlotInfo.PLOT_PLAN, 0L, start, "", 0, 0, TopoDroidApp.mScaleFactor, 0.0f );
    // long mPIDs = mApp.mData.insertPlot( mApp.mSID, -1L, name+"s",
    //              PlotInfo.PLOT_EXTENDED, 0L, start, "", 0, 0, TopoDroidApp.mScaleFactor, 0.0f );
    long mPIDp = mApp.insert2dPlot( mApp.mSID, name, start );

    if ( mPIDp >= 0 ) {
      long mPIDs = mPIDp + 1L; // FIXME !!! this is true but not guaranteed
      startDrawingActivity( start, name+"p", mPIDp, name+"s", mPIDs, PlotInfo.PLOT_PLAN );
    // } else {
    //   Toast.makeText( this, R.string.plot_duplicate_name, Toast.LENGTH_LONG).show();
    }
    // updateDisplay( );
  }

  // FIXME_SKETCH_3D
  public void makeNewSketch3d( String name, String st1, String st2 )
  {
    // FIXME xoffset yoffset, east south and vert (downwards)
    if ( st2 != null ) {
      if ( ! mApp.mData.hasShot( mApp.mSID, st1, st2 ) ) {
        Toast.makeText( this, R.string.no_shot_between_stations, Toast.LENGTH_SHORT).show();
        return;
      }
    } else {
      st2 = mApp.mData.nextStation( mApp.mSID, st1 );
    }
    if ( st2 != null ) {
      float e = 0.0f; // NOTE (e,s,v) are the coord of station st1, and st1 is taken as the origin of the ref-frame
      float s = 0.0f;
      float v = 0.0f;
      long mPID = mApp.mData.insertSketch3d( mApp.mSID, -1L, name, 0L, st1, st1, st2,
                                            0, // mApp.mDisplayWidth/(2*TopoDroidApp.mScaleFactor),
                                            0, // mApp.mDisplayHeight/(2*TopoDroidApp.mScaleFactor),
                                            10 * TopoDroidApp.mScaleFactor,
                                            0, 0, 10 * TopoDroidApp.mScaleFactor,
                                            0, 0, 10 * TopoDroidApp.mScaleFactor,
                                            e, s, v, 180, 0 );
      if ( mPID >= 0 ) {
        startSketchActivity( name );
      }
    } else {
      Toast.makeText( this, "no to station", Toast.LENGTH_SHORT).show();
    }
  }
  // END_SKETCH_3D

  // public void startPlotDialog( String name, String type ) // name = plot/sketch3d name
  // {
  //   // FIXME SKETCH-3D
  //     PlotInfo plot1 =  mApp.mData.getPlotInfo( mApp.mSID, name+"p" );
  //     if ( plot1 != null ) {
  //       PlotInfo plot2 = mApp.mData.getPlotInfo( mApp.mSID, name+"s" );
  //       ( new PlotDialog( this, this, plot1, plot2 )).show();
  //       return;
  //     }
  //   Toast.makeText( this, R.string.plot_not_found, Toast.LENGTH_SHORT).show();
  // }

  public void startExistingPlot( String name, long type ) // name = plot/sketch3d name
  {
    // TDLog.Log( TDLog.LOG_SHOT, "startExistingPlot \"" + name + "\" type " + type + " sid " + mApp.mSID );

    // FIXME_SKETCH_3D
    if ( type == PlotInfo.PLOT_SKETCH_3D ) {
      Sketch3dInfo sketch = mApp.mData.getSketch3dInfo( mApp.mSID, name );
      if ( sketch != null ) {
        startSketchActivity( sketch.name );
        return;
      }
    } else {
    // END_SKETCH_3D
      PlotInfo plot1 =  mApp.mData.getPlotInfo( mApp.mSID, name+"p" );
      if ( plot1 != null ) {
        mRecentPlot     = name;
        mRecentPlotType = type;
        PlotInfo plot2 =  mApp.mData.getPlotInfo( mApp.mSID, name+"s" );
        startDrawingActivity( plot1.start, plot1.name, plot1.id, plot2.name, plot2.id, type );
        return;
      } else {
        mRecentPlot = null;
      }
    }
    Toast.makeText( this, R.string.plot_not_found, Toast.LENGTH_SHORT).show();
  }
 
  // FIXME_SKETCH_3D
  private void startSketchActivity( String name )
  {
    if ( mApp.mSID < 0 ) {
      Toast.makeText( this, R.string.no_survey, Toast.LENGTH_SHORT ).show();
      return;
    }

    // notice when starting the SketchActivity the remote device is disconnected 
    // FIXME mApp.disconnectRemoteDevice();

    // TODO
    Intent sketchIntent = new Intent( Intent.ACTION_VIEW ).setClass( this, SketchActivity.class );
    sketchIntent.putExtra( TopoDroidTag.TOPODROID_SURVEY_ID, mApp.mSID );
    sketchIntent.putExtra( TopoDroidTag.TOPODROID_SKETCH_NAME, name );
    startActivity( sketchIntent );
  }
  // END_SKETCH_3D

  private void startDrawingActivity( String start, String plot1_name, long plot1_id,
                                                   String plot2_name, long plot2_id, long type )
  {
    if ( mApp.mSID < 0 || plot1_id < 0 || plot2_id < 0 ) {
      Toast.makeText( this, R.string.no_survey, Toast.LENGTH_SHORT ).show();
      return;
    }
    
    // notice when starting the DrawingActivity the remote device is disconnected 
    // FIXME mApp.disconnectRemoteDevice();

    Intent drawIntent = new Intent( Intent.ACTION_VIEW ).setClass( this, DrawingActivity.class );
    drawIntent.putExtra( TopoDroidTag.TOPODROID_SURVEY_ID, mApp.mSID );
    drawIntent.putExtra( TopoDroidTag.TOPODROID_PLOT_NAME, plot1_name );
    drawIntent.putExtra( TopoDroidTag.TOPODROID_PLOT_NAME2, plot2_name );
    drawIntent.putExtra( TopoDroidTag.TOPODROID_PLOT_TYPE, type );
    drawIntent.putExtra( TopoDroidTag.TOPODROID_PLOT_FROM, start );
    // drawIntent.putExtra( TopoDroidTag.TOPODROID_PLOT_ID, plot1_id ); // not necessary
    // drawIntent.putExtra( TopoDroidTag.TOPODROID_PLOT_ID2, plot2_id ); // not necessary

    startActivity( drawIntent );
  }

  // ---------------------------------------------------------------------------------

  // public void dropShot( DistoXDBlock blk )
  // {
  //   mApp.mData.deleteShot( blk.mId, mApp.mSID );
  //   updateDisplay( ); // FIXME
  // }

  public DistoXDBlock getNextBlankLegShot( DistoXDBlock blk )
  {
    DistoXDBlock ret = null;
    long id = 0;
    for ( int k=0; k<mDataAdapter.size(); ++k ) {
      DistoXDBlock b = mDataAdapter.get(k);
      if ( b.isTypeBlank() ) {
        id = b.mId - 1;
        break;
      }
    }
    List<DistoXDBlock> list = mApp.mData.selectShotsAfterId( mApp.mSID, id , 0 );
    for ( DistoXDBlock b : list ) {
      if ( b.isTypeBlank() ) {
        // Log.v( TopoDroidApp.TAG, "BLANK " + b.mLength + " " + b.mBearing + " " + b.mClino );
        if ( ret != null && ret.relativeDistance( b ) ) return ret;
        ret = b;
      } else if ( b.mType == DistoXDBlock.BLOCK_SEC_LEG ) {
        // Log.v( TopoDroidApp.TAG, "LEG " + b.mLength + " " + b.mBearing + " " + b.mClino );
        if ( ret != null &&  ret.relativeDistance( b ) ) return ret;
      } else {
        // Log.v( TopoDroidApp.TAG, "OTHER " + b.mLength + " " + b.mBearing + " " + b.mClino );
        ret = null;
      }
    }
    return null;
  }

  // get the next centerline shot and set mNextPos index
  public DistoXDBlock getNextLegShot( DistoXDBlock blk, boolean move_down )
  {
    // TDLog.Log( TDLog.LOG_SHOT, "getNextLegShot: pos " + mShotPos );
    if ( blk == null ) {
      // TDLog.Log( TDLog.LOG_SHOT, "   block is null");
      return null;
    }
    if ( move_down ) {
      mPrevPos = mShotPos;
      mShotPos = mNextPos;
      mNextPos = mPrevPos; // the old mShotPos;
    } else {
      mNextPos = mShotPos;
    }
    while ( mNextPos < mDataAdapter.size() && blk != mDataAdapter.get(mNextPos) ) ++ mNextPos;
    ++ mNextPos; // one position after blk
    while ( mNextPos < mDataAdapter.size() ) {
      DistoXDBlock b = mDataAdapter.get(mNextPos);
      int t = b.type();
      if ( t == DistoXDBlock.BLOCK_MAIN_LEG ) {
        return b;
      } else if (    DistoXDBlock.isTypeBlank( t )
                  && mNextPos+1 < mDataAdapter.size()
                  && b.relativeDistance( mDataAdapter.get(mNextPos+1) ) ) {
        return b;
      }
      ++ mNextPos;
    }
    return null;
  }

  // get the previous centerline shot and set the mPrevPos index
  public DistoXDBlock getPreviousLegShot( DistoXDBlock blk, boolean move_up )
  {
    // TDLog.Log( TDLog.LOG_SHOT, "getPreviousLegShot: pos " + mShotPos );
    if ( blk == null ) return null;
    if ( move_up ) {
      mNextPos = mShotPos;
      mShotPos = mPrevPos;
      mPrevPos = mNextPos; // the old mShotPos;
    } else {
      mPrevPos = mShotPos;
    }
    while ( mPrevPos >= 0 && blk != mDataAdapter.get(mPrevPos) ) -- mPrevPos;
    while ( mPrevPos > 0 ) {
      -- mPrevPos;
      DistoXDBlock b = mDataAdapter.get(mPrevPos);
      if ( b.type() == DistoXDBlock.BLOCK_MAIN_LEG ) {
        return b;
      }
    }
    return null;
  }

  void updateShotDistanceBearingClino( float d, float b, float c, DistoXDBlock blk )
  {
    // Log.v("DistoX", "update shot DBC length " + d );
    mApp.mData.updateShotDistanceBearingClino( blk.mId, mApp.mSID, d, b, c, true );
    blk.mLength  = d;
    blk.mBearing = b;
    blk.mClino   = c;
    mDataAdapter.updateBlockView( blk );
  }

  void updateShot( String from, String to, long extend, long flag, boolean leg, String comment, DistoXDBlock blk )
  {
    // TDLog.Log( TDLog.LOG_SHOT, "updateShot From >" + from + "< To >" + to + "< comment " + comment );
    blk.setName( from, to );

    int ret = mApp.mData.updateShot( blk.mId, mApp.mSID, from, to, extend, flag, leg?1:0, comment, true );

    if ( ret == -1 ) {
      Toast.makeText( this, R.string.no_db, Toast.LENGTH_SHORT ).show();
    // } else if ( ret == -2 ) {
    //   Toast.makeText( this, R.string.makes_cycle, Toast.LENGTH_SHORT ).show();
    } else {
      // update same shots of the given block
      List< DistoXDBlock > blk_list = mApp.mData.selectShotsAfterId( blk.mId, mApp.mSID, 0L );
      for ( DistoXDBlock blk1 : blk_list ) {
        if ( ! blk1.relativeDistance( blk ) ) break;
        mApp.mData.updateShotLeg( blk1.mId, mApp.mSID, 1L, true );
      }
    }
    mDataAdapter.updateBlockView( blk );
  }

  
  // this method is called by ShotDialog() with to.length() == 0 ie to == ""
  // and blk splay shot
  public void updateSplayShots( String from, String to, long extend, long flag, boolean leg, String comment, DistoXDBlock blk )
  {
    ArrayList< DistoXDBlock > splays = mDataAdapter.getSplaysAtId( blk.mId );
    for ( DistoXDBlock b : splays ) {
      if ( b.mId == blk.mId ) {
        blk.setName( from, to );
        // FIXME leg should be 0
        int ret = mApp.mData.updateShot( blk.mId, mApp.mSID, from, to, extend, flag, leg?1:0, comment, true );

        if ( ret == -1 ) {
          Toast.makeText( this, R.string.no_db, Toast.LENGTH_SHORT ).show();
        // } else if ( ret == -2 ) {
        //   Toast.makeText( this, R.string.makes_cycle, Toast.LENGTH_SHORT ).show();
        } else {
          // // update same shots of the given block: SHOULD NOT HAPPEN
          // List< DistoXDBlock > blk_list = mApp.mData.selectShotsAfterId( blk.mId, mApp.mSID, 0L );
          // for ( DistoXDBlock blk1 : blk_list ) {
          //   if ( ! blk1.relativeDistance( blk ) ) break;
          //   mApp.mData.updateShotLeg( blk1.mId, mApp.mSID, 1L, true );
          // }
        }
      } else {
        b.setName( from, to );
        mApp.mData.updateShotName( b.mId, mApp.mSID, from, to, true );
      }
      mDataAdapter.updateBlockView( b );
    }
  }

  // ------------------------------------------------------------------------

  @Override
  public boolean onSearchRequested()
  {
    // TDLog.Error( "search requested" );
    Intent intent = new Intent( this, TopoDroidPreferences.class );
    intent.putExtra( TopoDroidPreferences.PREF_CATEGORY, TopoDroidPreferences.PREF_CATEGORY_SURVEY );
    startActivity( intent );
    return true;
  }

  @Override
  public boolean onKeyDown( int code, KeyEvent event )
  {
    switch ( code ) {
      case KeyEvent.KEYCODE_BACK: // HARDWARE BACK (4)
        onBackPressed();
        return true;
      case KeyEvent.KEYCODE_SEARCH:
        return onSearchRequested();
      case KeyEvent.KEYCODE_MENU:   // HARDWRAE MENU (82)
        String help_page = getResources().getString( R.string.ShotActivity );
        if ( help_page != null ) UserManualActivity.showHelpPage( this, help_page );
        return true;
      // case KeyEvent.KEYCODE_VOLUME_UP:   // (24)
      // case KeyEvent.KEYCODE_VOLUME_DOWN: // (25)
      default:
        // TDLog.Error( "key down: code " + code );
    }
    return false;
  }

  // ---------------------------------------------------------

  private void setMenuAdapter()
  {
    int k = 0;
    Resources res = getResources();
    // mMenuAdapter = new ArrayAdapter<String>(this, R.layout.menu );
    mMenuAdapter = new MyMenuAdapter( this, this, mMenu, R.layout.menu, new ArrayList< MyMenuItem >() );

    mMenuAdapter.add( res.getString( menus[k++] ) );                                             // menu_survey
    if ( TDSetting.mLevelOverBasic  ) mMenuAdapter.add( res.getString( menus[k] ) ); k++; // menu_recover
    if ( TDSetting.mLevelOverNormal ) mMenuAdapter.add( res.getString( menus[k] ) ); k++; // menu_photo  
    if ( TDSetting.mLevelOverNormal ) mMenuAdapter.add( res.getString( menus[k] ) ); k++; // menu_sensor
    if ( TDSetting.mLevelOverBasic  ) mMenuAdapter.add( res.getString( menus[k] ) ); k++; // menu_3d
    if ( TDSetting.mLevelOverNormal ) mMenuAdapter.add( res.getString( menus[k] ) ); k++; // menu_distox
    mMenuAdapter.add( res.getString( menus[k++] ) );  // menu_options
    mMenuAdapter.add( res.getString( menus[k++] ) );  // menu_help
    mMenu.setAdapter( mMenuAdapter );
    mMenu.invalidate();
  }

  private boolean closeMenu()
  {
    if ( onMenu ) {
      mMenu.setVisibility( View.GONE );
      onMenu = false;
      return true;
    }
    return false;
  }


  void deletePlot( long pid1, long pid2 )
  {
    mApp.mData.deletePlot( pid1, mApp.mSID );
    mApp.mData.deletePlot( pid2, mApp.mSID );
    // FIXME NOTIFY
  }

  void recomputeItems( String st )
  {
    if ( ! mShowSplay.remove( st ) ) {
      mShowSplay.add( st );
    }
    updateDisplay( );
  }

  private boolean showSplaysContains( String name ) 
  {
    for ( String st : mShowSplay ) {
      if ( st.equals( name ) ) return true;
    }
    return false;
  }


  public void setConnectionStatus( int status )
  { 
    if ( mApp.mDevice == null ) {
      mButton1[ BTN_DOWNLOAD ].setVisibility( View.GONE );
    } else {
      mButton1[ BTN_DOWNLOAD ].setVisibility( View.VISIBLE );
      switch ( status ) {
        case 1:
          mButton1[BTN_DOWNLOAD].setBackgroundDrawable( mBMdownload_on );
          break;
        case 2:
          mButton1[BTN_DOWNLOAD].setBackgroundDrawable( mBMdownload_wait );
          break;
        default:
          mButton1[BTN_DOWNLOAD].setBackgroundDrawable( mBMdownload );
      }
    }
  }

  void renumberShotsAfter( DistoXDBlock blk )
  {
    // Log.v("DistoX", "renumber shots after " + blk.mLength + " " + blk.mBearing + " " + blk.mClino );
    // NEED TO FORWARD to the APP to change the stations accordingly
 
    List< DistoXDBlock > shots = mApp.mData.selectAllShotsAfter( blk.mId, mApp.mSID, TopoDroidApp.STATUS_NORMAL );
    mApp.assignStationsAfter( blk, shots );

    // DEBUG re-assign all the stations
    // List< DistoXDBlock > shots = mApp.mData.selectAllShots( mApp.mSID, TopoDroidApp.STATUS_NORMAL );
    // mApp.assignStations( shots );

    updateDisplay();
  }

  // merge this block to the following (or second following) block if this is a leg
  // if success update FROM/TO of the block
  long mergeToNextLeg( DistoXDBlock blk )
  {
    long id = mApp.mData.mergeToNextLeg( blk, mApp.mSID, false );
    // Log.v("DistoX", "merge next leg: block " + blk.mId + " leg " + id );
    if ( id >= 0 && id != blk.mId ) {
      // mDataAdapter.updateBlockName( id, "", "" ); // name has already been updated in DB
      updateDisplay(); // FIXME change only block with id
    }
    return id;
  }

  // ------------------------------------------------------------------

}
