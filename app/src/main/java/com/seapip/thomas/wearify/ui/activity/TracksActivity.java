package com.seapip.thomas.wearify.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableDrawerLayout;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;

import com.seapip.thomas.wearify.R;
import com.seapip.thomas.wearify.spotify.Callback;
import com.seapip.thomas.wearify.spotify.objects.Paging;
import com.seapip.thomas.wearify.spotify.objects.SavedTrack;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;
import com.seapip.thomas.wearify.ui.browse.ActionButtonSmall;
import com.seapip.thomas.wearify.ui.browse.BaseActivity;
import com.seapip.thomas.wearify.ui.browse.Adapter;
import com.seapip.thomas.wearify.ui.browse.Header;
import com.seapip.thomas.wearify.ui.browse.Item;
import com.seapip.thomas.wearify.ui.browse.Loading;
import com.seapip.thomas.wearify.ui.browse.OnClick;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.spotify.Service.getWebAPI;

public class TracksActivity extends BaseActivity {

    private WearableRecyclerView mRecyclerView;
    private ArrayList<Item> mItems;
    private ArrayList<String> mUris;
    private int mPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawerView) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawerView) findViewById(R.id.bottom_action_drawer), 1);

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.content);
        mItems = new ArrayList<>();
        mItems.add(new Header("Songs"));
        ActionButtonSmall shuffle = new ActionButtonSmall();
        shuffle.icon = getDrawable(R.drawable.ic_shuffle_black_24dp);
        shuffle.iconColor = Color.argb(200, 0, 0, 0);
        shuffle.backgroundColor = Color.parseColor("#00ffe0");
        shuffle.text = "Shuffle Play";
        mItems.add(shuffle);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter(this, mItems));
        mUris = new ArrayList<>();
        getTracks(50, 0);
    }

    private void getTracks(final int limit, final int offset) {
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        getWebAPI(this, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Paging<SavedTrack>> call = webAPI.getTracks(limit, offset, "from_token");
                call.enqueue(new retrofit2.Callback<Paging<SavedTrack>>() {
                    @Override
                    public void onResponse(Call<Paging<SavedTrack>> call, Response<Paging<SavedTrack>> response) {
                        if (response.isSuccessful()) {
                            mItems.remove(loading);
                            Paging<SavedTrack> savedTracks = response.body();
                            for (SavedTrack savedTrack : savedTracks.items) {
                                Item item = new Item();
                                item.setTrack(savedTrack.track);
                                final int position = mPosition++;
                                item.onClick = new OnClick() {
                                    @Override
                                    public void run(Context context) {
                                        getService().play(mUris.toArray(new String[mUris.size()]), null,
                                                position, false, "off", 0);
                                    }
                                };
                                mItems.add(item);
                                mUris.add(savedTrack.track.uri);
                            }
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                            if (savedTracks.total > savedTracks.offset + limit) {
                                getTracks(limit, savedTracks.offset + limit);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<SavedTrack>> call, Throwable t) {

                    }
                });
            }
        });
    }
}
