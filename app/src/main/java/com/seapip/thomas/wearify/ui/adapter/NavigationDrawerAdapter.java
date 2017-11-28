package com.seapip.thomas.wearify.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;

import com.seapip.thomas.wearify.R;

public class NavigationDrawerAdapter extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {
    private Context mContext;
    private boolean mSelect;

    public NavigationDrawerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public String getItemText(int i) {
        switch (i) {
            case 0:
                return "Browse";
            case 1:
                return "Library";
            case 2:
                return "Settings";
        }
        return null;
    }

    @Override
    public Drawable getItemDrawable(int i) {
        int id;
        switch (i) {
            default:
            case 0:
                id = R.drawable.ic_browse_black_24dp;
                break;
            case 1:
                id = R.drawable.ic_library_music_black_24dp;
                break;
            case 2:
                id = R.drawable.ic_settings_black_24dp;
                break;
        }
        Drawable drawable = mContext.getDrawable(id);
        drawable.setTint(Color.WHITE);
        return drawable;
    }

    @Override
    public int getCount() {
        return 3;
    }

    public void enabledSelect() {
        mSelect = true;
    }
}
