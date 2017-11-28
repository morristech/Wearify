package com.seapip.thomas.wearify.ui.activity;

import android.content.Context;
import android.content.Intent;
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
import com.seapip.thomas.wearify.spotify.objects.Playlist;
import com.seapip.thomas.wearify.spotify.objects.User;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;
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

public class PlaylistsActivity extends BaseActivity {

    private WearableRecyclerView mRecyclerView;
    private ArrayList<Item> mItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawerView) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawerView) findViewById(R.id.bottom_action_drawer), 1);

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.content);
        mItems = new ArrayList<>();
        mItems.add(new Header("Playlists"));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter(this, mItems));
        getPlaylists(50, 0);
    }

    private void getPlaylists(final int limit, final int offset) {
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        getWebAPI(this, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Paging<Playlist>> call = webAPI.getPlaylists(limit, offset);
                call.enqueue(new retrofit2.Callback<Paging<Playlist>>() {
                    @Override
                    public void onResponse(Call<Paging<Playlist>> call, Response<Paging<Playlist>> response) {
                        if (response.isSuccessful()) {
                            mItems.remove(loading);
                            Paging<Playlist> playlists = response.body();
                            for (final Playlist playlist : playlists.items) {
                                final Item item = new Item();
                                item.setPlaylist(playlist, mRecyclerView, true);
                                if (item.imageUrl == null) {
                                    item.image = getDrawable(R.drawable.ic_playlist_black_24px);
                                }
                                item.onClick = new OnClick() {
                                    @Override
                                    public void run(Context context) {
                                        Intent intent = new Intent(context, PlaylistActivity.class).putExtra("uri", playlist.uri);
                                        context.startActivity(intent);
                                    }
                                };
                                mItems.add(item);
                                getWebAPI(PlaylistsActivity.this, new Callback<WebAPI>() {
                                    @Override
                                    public void onSuccess(WebAPI webAPI) {
                                        Call<User> call = webAPI.getUser(playlist.owner.id);
                                        call.enqueue(new retrofit2.Callback<User>() {
                                            @Override
                                            public void onResponse(Call<User> call, Response<User> response) {
                                                if (response.isSuccessful()) {
                                                    User user = response.body();
                                                    String name = "by ";
                                                    if (user.display_name != null) {
                                                        name += user.display_name;
                                                    } else {
                                                        name += user.id;
                                                    }
                                                    item.subTitle = name + " • " + item.subTitle;
                                                    mRecyclerView.getAdapter().notifyDataSetChanged();
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<User> call, Throwable t) {

                                            }
                                        });
                                    }
                                });
                            }
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                            if (playlists.total > playlists.offset + limit) {
                                getPlaylists(limit, playlists.offset + limit);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<Playlist>> call, Throwable t) {

                    }
                });
            }
        });
    }
}
