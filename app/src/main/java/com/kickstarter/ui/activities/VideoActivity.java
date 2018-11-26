package com.kickstarter.ui.activities;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.kickstarter.R;
import com.kickstarter.libs.ApiCapabilities;
import com.kickstarter.libs.BaseActivity;
import com.kickstarter.libs.Build;
import com.kickstarter.libs.qualifiers.RequiresActivityViewModel;
import com.kickstarter.libs.rx.transformers.Transformers;
import com.kickstarter.services.interceptors.WebRequestInterceptor;
import com.kickstarter.viewmodels.VideoViewModel;
import com.trello.rxlifecycle.ActivityEvent;

import butterknife.Bind;
import butterknife.ButterKnife;

@RequiresActivityViewModel(VideoViewModel.ViewModel.class)
public final class VideoActivity extends BaseActivity<VideoViewModel.ViewModel> {
  private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

  private Build build;
  private ExoPlayer player;
  private long playerPosition;
  private TrackSelector trackSelector;
  protected TrackSelectionHelper trackSelectionHelper;


  protected @Bind(R.id.closed_captions) Button closedCaptionsButton;
  protected @Bind(R.id.video_player_layout) View rootView;
  protected @Bind(R.id.player_view) PlayerView playerView;
  protected @Bind(R.id.loading_indicator) ProgressBar loadingIndicatorProgressBar;

  @Override
  public void onCreate(final @Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.video_player_layout);
    ButterKnife.bind(this);

    this.build = environment().build();

    this.viewModel.outputs.preparePlayerWithUrl()
      .compose(Transformers.takeWhen(lifecycle().filter(ActivityEvent.RESUME::equals)))
      .compose(bindToLifecycle())
      .subscribe(this::preparePlayer);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    releasePlayer();
  }

  @Override
  public void onPause() {
    super.onPause();
    releasePlayer();
  }

  @Override
  public void onWindowFocusChanged(final boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);

    if (hasFocus) {
      this.rootView.setSystemUiVisibility(systemUIFlags());
    }
  }

  @TargetApi(19)
  private int systemUIFlags() {
    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      | View.SYSTEM_UI_FLAG_FULLSCREEN;

    return ApiCapabilities.canSetImmersiveSystemUI()
      ? flags | View.SYSTEM_UI_FLAG_IMMERSIVE
      : flags;
  }

  private void onStateChanged(final int playbackState) {
    if (playbackState == Player.STATE_ENDED) {
      finish();
    }

    if (playbackState == Player.STATE_BUFFERING) {
      this.loadingIndicatorProgressBar.setVisibility(View.VISIBLE);
    } else {
      this.loadingIndicatorProgressBar.setVisibility(View.GONE);
    }
  }

  private void preparePlayer(final @NonNull String videoUrl) {
    final TrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
    this.trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
    this.trackSelectionHelper = new TrackSelectionHelper(trackSelector, adaptiveTrackSelectionFactory);


    this.player = ExoPlayerFactory.newSimpleInstance(this, this.trackSelector);
    this.playerView.setPlayer(this.player);
    this.player.addListener(this.eventListener);

    this.player.seekTo(this.playerPosition);
    final boolean playerIsResuming = this.playerPosition != 0;
    this.player.prepare(getMediaSource(videoUrl), playerIsResuming, false);
    this.player.setPlayWhenReady(true);
  }

  private MediaSource getMediaSource(final @NonNull String videoUrl) {
    final DefaultHttpDataSourceFactory dataSourceFactory = new DefaultHttpDataSourceFactory(WebRequestInterceptor.userAgent(this.build));
    final Uri videoUri = Uri.parse(videoUrl);
    final int fileType = Util.inferContentType(videoUri);

    switch (fileType) {
      case C.TYPE_HLS:
        return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri);
      default:
        return new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri);
    }

    handleSubtitles();
  }

  private void handleSubtitles() {
    MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
    if (mappedTrackInfo == null) {
      return;
    }

    for (int i = 0; i < mappedTrackInfo.length; i++) {
      TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
      if (trackGroups.length != 0) {
        switch (this.player.getRendererType(i)) {
          case C.TRACK_TYPE_TEXT:
            closedCaptionsButton.setTag(i);
            closedCaptionsButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                  trackSelectionHelper.showSelectionDialog(
                    this, "Text", mappedTrackInfo, closedCaptionsButton.getTag());
                }
              }
            });
            break;
          default:
            continue;
        }

      }
    }
  }

  private void releasePlayer() {
    if (this.player != null) {
      this.playerPosition = this.player.getCurrentPosition();
      this.player.removeListener(this.eventListener);
      this.player.release();
      this.trackSelector = null;
      this.player = null;
    }
  }

  private @NonNull Player.DefaultEventListener eventListener =
    new Player.DefaultEventListener() {
      @Override
      public void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
        onStateChanged(playbackState);
      }
    };
}
