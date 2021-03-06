/** @file CalibCBlockAdapter.java
 *
 * @author marco corvi
 * @date apr 2012
 *
 * @brief TopoDroid adapter for calibration data
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.DistoX;

import android.content.Context;

import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import java.util.ArrayList;

class CalibCBlockAdapter extends ArrayAdapter< CalibCBlock >
{
  private ArrayList< CalibCBlock > items;  // list if calibration data
  private Context context;                 // context 


  public CalibCBlockAdapter( Context ctx, int id, ArrayList< CalibCBlock > items )
  {
    super( ctx, id, items );
    this.context = ctx;
    this.items = items;
  }

  CalibCBlock get( int pos ) { return items.get(pos); }
 
  @Override
  public View getView( int pos, View convertView, ViewGroup parent )
  {
    View v = convertView;
    if ( v == null ) {
      LayoutInflater li = (LayoutInflater)context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
      v = li.inflate( R.layout.row, null );
    }

    CalibCBlock b = items.get( pos );
    if ( b != null ) {
      TextView tw = (TextView) v.findViewById( R.id.row_text );
      tw.setText( b.toString() );
      tw.setTextSize( TDSetting.mTextSize );
      tw.setTextColor( b.color() );
      if ( b.isSaturated() ) {
        tw.setBackgroundColor( 0xff990000 );
      } else if ( b.mStatus == 0 ) {
        tw.setBackgroundColor( 0xff000000 );
      } else {
        tw.setBackgroundColor( 0xff666666 );
      }
    }
    return v;
  }

}

