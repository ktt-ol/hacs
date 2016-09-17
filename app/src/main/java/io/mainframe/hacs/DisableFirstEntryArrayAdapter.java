package io.mainframe.hacs;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by holger on 23.08.16.
 */
public class DisableFirstEntryArrayAdapter<T> extends ArrayAdapter<T> {

    public DisableFirstEntryArrayAdapter(Context context, int resource, T[] objects) {
        super(context, resource, objects);
    }

    @Override
    public boolean isEnabled(int position) {
        return position > 0;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View mView = super.getDropDownView(position, convertView, parent);
        TextView mTextView = (TextView) mView;

        if (position == 0) {
            mTextView.setTextColor(Color.GRAY);
        } else {
            mTextView.setTextColor(Color.BLACK);
        }
        return mView;
    }
}
