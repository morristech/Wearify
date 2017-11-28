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
import com.seapip.thomas.wearify.spotify.objects.CursorPaging;
import com.seapip.thomas.wearify.spotify.objects.PlayHistory;
import com.seapip.thomas.wearify.spotify.objects.Playlist;
import com.seapip.thomas.wearify.spotify.objects.User;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;
import com.seapip.thomas.wearify.ui.browse.ActionButton;
import com.seapip.thomas.wearify.ui.browse.BaseActivity;
import com.seapip.thomas.wearify.ui.browse.Adapter;
import com.seapip.thomas.wearify.ui.browse.Category;
import com.seapip.thomas.wearify.ui.browse.Header;
import com.seapip.thomas.wearify.ui.browse.Item;
import com.seapip.thomas.wearify.ui.browse.Loading;
import com.seapip.thomas.wearify.ui.browse.OnClick;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.spotify.Service.getWebAPI;

public class LibraryActivity extends BaseActivity {

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
        ActionButton search = new ActionButton();
        search.icon = getDrawable(R.drawable.ic_search_black_24dp);
        search.iconColor = Color.argb(200, 0, 0, 0);
        search.backgroundColor = Color.parseColor("#00ffe0");
        mItems.add(search);
        mItems.add(new Category("Playlists", getDrawable(R.drawable.ic_playlist_black_24px),
                new OnClick() {
                    @Override
                    public void run(Context context) {
                        context.startActivity(new Intent(context, PlaylistsActivity.class));
                    }
                }));
        mItems.add(new Category("Songs", getDrawable(R.drawable.ic_song_black_24dp),
                new OnClick() {
                    @Override
                    public void run(Context context) {
                        context.startActivity(new Intent(context, TracksActivity.class));
                    }
                }));
        mItems.add(new Category("Albums", getDrawable(R.drawable.ic_album_black_24dp),
                new OnClick() {
                    @Override
                    public void run(Context context) {
                        context.startActivity(new Intent(context, AlbumsActivity.class));
                    }
                }));
        mItems.add(new Category("Artists", getDrawable(R.drawable.ic_artist_black_24dp),
                new OnClick() {
                    @Override
                    public void run(Context context) {
                        context.startActivity(new Intent(context, ArtistsActivity.class));
                    }
                }));
        mItems.add(new Header("Recently Played"));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter(this, mItems));
        getRecentPlayed(50);
    }

    private void getRecentPlayed(final int limit) {
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        getWebAPI(this, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<CursorPaging<PlayHistory>> call = webAPI.getRecentPlayed(limit);
                call.enqueue(new retrofit2.Callback<CursorPaging<PlayHistory>>() {
                    @Override
                    public void onResponse(Call<CursorPaging<PlayHistory>> call, Response<CursorPaging<PlayHistory>> response) {
                        if (response.isSuccessful()) {
                            mItems.remove(loading);
                            CursorPaging<PlayHistory> playHistories = response.body();
                            for (final PlayHistory playHistory : playHistories.items) {
                                if (playHistory.context != null && playHistory.context.type != null && !containsUri(playHistory.context.uri)) {
                                    final Item item = new Item();
                                    item.uri = playHistory.context.uri;
                                    switch (playHistory.context.type) {
                                        case "playlist":
                                            getWebAPI(LibraryActivity.this, new Callback<WebAPI>() {
                                                @Override
                                                public void onSuccess(WebAPI webAPI) {
                                                    Call<Playlist> call = webAPI.getPlaylist(
                                                            item.uri.split(":")[2],
                                                            item.uri.split(":")[4],
                                                            "name,images,uri,owner.id", "from_token");
                                                    call.enqueue(new retrofit2.Callback<Playlist>() {
                                                        @Override
                                                        public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                                                            if (response.isSuccessful()) {
                                                                final Playlist playlist = response.body();
                                                                item.setPlaylist(playlist, mRecyclerView);
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
                                                                mRecyclerView.getAdapter().notifyDataSetChanged();
                                                                getWebAPI(LibraryActivity.this, new Callback<WebAPI>() {
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
                                                        }

                                                        @Override
                                                        public void onFailure(Call<Playlist> call, Throwable t) {

                                                        }
                                                    });
                                                }
                                            });
                                            break;
                                        case "album":
                                            item.setAlbum(playHistory.track.album);
                                            item.onClick = new OnClick() {
                                                @Override
                                                public void run(Context context) {
                                                    Intent intent = new Intent(context, AlbumActivity.class).putExtra("uri", playHistory.track.album.uri);
                                                    context.startActivity(intent);
                                                }
                                            };
                                            break;
                                    }
                                    mItems.add(item);
                                }
                            }
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(Call<CursorPaging<PlayHistory>> call, Throwable t) {

                    }
                });
            }
        });
    }

    private boolean containsUri(String uri) {
        for (Item item : mItems) {
            if (item.uri != null && item.uri.equals(uri)) {
                return true;
            }
        }
        return false;
    }
}
