package com.seapip.thomas.wearify.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.text.TextUtils;

import com.seapip.thomas.wearify.R;
import com.seapip.thomas.wearify.ui.browse.Activity;
import com.seapip.thomas.wearify.ui.browse.Adapter;
import com.seapip.thomas.wearify.ui.browse.Header;
import com.seapip.thomas.wearify.ui.browse.Item;
import com.seapip.thomas.wearify.ui.browse.LetterGroupHeader;
import com.seapip.thomas.wearify.ui.browse.Loading;
import com.seapip.thomas.wearify.ui.browse.OnClick;
import com.seapip.thomas.wearify.spotify.objects.Artist;
import com.seapip.thomas.wearify.spotify.objects.Artists;
import com.seapip.thomas.wearify.spotify.Callback;
import com.seapip.thomas.wearify.spotify.objects.Paging;
import com.seapip.thomas.wearify.spotify.objects.SavedTrack;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;
import com.seapip.thomas.wearify.spotify.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.spotify.Service.getWebAPI;

public class ArtistsActivity extends Activity {

    private WearableRecyclerView mRecyclerView;
    private ArrayList<Item> mItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer), 1);

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.content);
        mItems = new ArrayList<>();
        mItems.add(new Header("Artists"));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter(this, mItems));
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
                                if (savedTrack.track.artists != null) {
                                    if (!containsUri(savedTrack.track.artists[0].uri)) {
                                        final Item item = new Item();
                                        item.setArtist(savedTrack.track.artists[0], 1);
                                        item.image = getDrawable(R.drawable.ic_artist_black_24dp);
                                        item.onClick = new OnClick() {
                                            @Override
                                            public void run(Context context) {
                                                Intent intent = new Intent(context, ArtistActivity.class).putExtra("uri", item.uri);
                                                context.startActivity(intent);
                                            }
                                        };
                                        mItems.add(item);
                                    } else {
                                        for (Item item : mItems) {
                                            if (item.uri != null && item.uri.equals(savedTrack.track.artists[0].uri)) {
                                                item.subTitle = Util.songCount(Integer.parseInt(item.subTitle.split(" ")[0]) + 1);
                                                break;
                                            }
                                        }
                                    }
                                }

                            }
                            groupByLetter();
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                            if (savedTracks.total > savedTracks.offset + limit) {
                                getTracks(limit, savedTracks.offset + limit);
                            } else {
                                final ArrayList<String> artistIds = new ArrayList<>();
                                for (Item item : mItems) {
                                    if (item.uri != null) {
                                        artistIds.add(item.uri.split(":")[2]);
                                    }
                                }
                                getWebAPI(ArtistsActivity.this, new Callback<WebAPI>() {
                                    @Override
                                    public void onSuccess(WebAPI webAPI) {
                                        Call<Artists> call = webAPI.getArtists(TextUtils.join(",", artistIds));
                                        call.enqueue(new retrofit2.Callback<Artists>() {
                                            @Override
                                            public void onResponse(Call<Artists> call, Response<Artists> response) {
                                                if (response.isSuccessful()) {
                                                    Artists artists = response.body();
                                                    for (Item item : mItems) {
                                                        if (item.uri != null) {
                                                            for (Artist artist : artists.artists) {
                                                                if (item.uri.equals(artist.uri)) {
                                                                    item.imageUrl = Util.smallestImageUrl(artist.images);
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    mRecyclerView.getAdapter().notifyDataSetChanged();
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<Artists> call, Throwable t) {

                                            }
                                        });
                                    }
                                });
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

    private boolean containsUri(String uri) {
        for (Item item : mItems) {
            if (item.uri != null && item.uri.equals(uri)) {
                return true;
            }
        }
        return false;
    }

    private void groupByLetter() {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i) instanceof LetterGroupHeader) {
                mItems.remove(i);
            }
        }
        Collections.sort(mItems.subList(1, mItems.size()), new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                return o1.title.trim().replaceFirst("^(?i)The ", "").compareTo(o2.title.trim().replaceFirst("^(?i)The ", ""));
            }
        });
        String letter = null;
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).uri != null) {
                String currentLetter = mItems.get(i).title
                        .trim()
                        .toUpperCase()
                        .replaceFirst("THE ", "")
                        .replaceFirst("\\d", "#")
                        .substring(0, 1);
                if (!currentLetter.equals(letter)) {
                    mItems.add(i, new LetterGroupHeader(currentLetter));
                    letter = currentLetter;
                }
            }
        }
    }
}
