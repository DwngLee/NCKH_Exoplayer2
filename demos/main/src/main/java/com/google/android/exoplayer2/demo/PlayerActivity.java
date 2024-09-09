/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.C.ContentType;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.demo.custom_bandwidth_meter.PredictiveBandwidthMeter;
import com.google.android.exoplayer2.demo.models.BandwidthTorchRecurrentModel;
import com.google.android.exoplayer2.demo.models.BaseBandwidthPredictionModel;
import com.google.android.exoplayer2.demo.models.Logger;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.ads.AdsLoader;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.UUID;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends Activity
    implements OnClickListener, PlaybackPreparer, PlayerControlView.VisibilityListener, Player.EventListener {

  public static final String DRM_SCHEME_EXTRA = "drm_scheme";
  public static final String DRM_LICENSE_URL = "drm_license_url";
  public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";
  public static final String DRM_MULTI_SESSION = "drm_multi_session";
  public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

  public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
  public static final String EXTENSION_EXTRA = "extension";

  public static final String ACTION_VIEW_LIST =
      "com.google.android.exoplayer.demo.action.VIEW_LIST";
  public static final String URI_LIST_EXTRA = "uri_list";
  public static final String EXTENSION_LIST_EXTRA = "extension_list";
  public static final String AD_TAG_URI_EXTRA = "ad_tag_uri";

  // For backwards compatibility.
  private static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";
  private static final CookieManager DEFAULT_COOKIE_MANAGER;

  static {
    DEFAULT_COOKIE_MANAGER = new CookieManager();
    DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private Handler mainHandler;
  private EventLogger eventLogger;
  private PlayerView playerView;
  private LinearLayout debugRootView;
  private TextView debugTextView;
  private Button retryButton;

  private DataSource.Factory mediaDataSourceFactory;
  private SimpleExoPlayer player;
  private DefaultTrackSelector trackSelector;
  private TrackSelectionHelper trackSelectionHelper;
  private DebugTextViewHelper debugViewHelper;
  private boolean inErrorState;
  private TrackGroupArray lastSeenTrackGroupArray;

  private boolean shouldAutoPlay;
  private int resumeWindow;
  private long resumePosition;

  private UiUpdateHandler mUiUpdateHandler;
  private long bufferedDurationMs;
  private long bytesDownloaded;
  private int droppedFrames;
  private ScrollView player_stats_layout;
  private LineChart player_stats_speed_chart;
  private LineChart player_stats_health_chart;
  private LineChart player_stats_nw_chart;
  private TextView player_stats_size;
  private TextView player_stats_res;
  private TextView player_stats_dropframes;
  private Format currentVideoFormat;

  public static final int MSG_UPDATE_STATS = 10005;
  public static final int MSG_UPDATE_STATS_NW_ONLY = 10006;

  // Fields used only for ad playback. The ads loader is loaded via reflection.
  private AdsLoader adsLoader;
  private Uri loadedAdTagUri;
  private ViewGroup adUiViewGroup;

  private PredictiveBandwidthMeter bandwidthMeter;

  // Model
  private BaseBandwidthPredictionModel ml_model;

  private final String model_path ="bandwidth_predictor_mobile.ptl";
  private final long []model_input_shape = {5, 1, 1};
  private long bitrateEstimate;


  private final String log_tag = "PlayerActivity log";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //create file log
    Logger.init(getFilesDir());
    System.out.println("dir: " + getFilesDir());

    // create model
    this.ml_model = new BandwidthTorchRecurrentModel(this, model_path, model_input_shape);
    int input_length = 1;
    for(long dim: model_input_shape)
        input_length *= (int)dim;

    mUiUpdateHandler = new UiUpdateHandler(this);
    bandwidthMeter = new PredictiveBandwidthMeter(
        mUiUpdateHandler,
        new BandwidthMeter.EventListener() {
          @Override
          public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
            // update newest predicted value
            bitrateEstimate = bitrate;
            bytesDownloaded = bytes;
            Log.i(log_tag, String.format("new estimated bitrate: %d; at: %d", bitrate, System.currentTimeMillis()));
          }
        },
        this.ml_model,
        input_length
    );

    shouldAutoPlay = true;
    clearResumePosition();
    mediaDataSourceFactory = buildDataSourceFactory(true);
    mainHandler = new Handler();
    if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
      CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
    }

    setContentView(R.layout.player_activity);
    View rootView = findViewById(R.id.root);
    rootView.setOnClickListener(this);
    debugRootView = findViewById(R.id.controls_root);
    debugTextView = findViewById(R.id.debug_text_view);

    playerView = findViewById(R.id.player_view);
    playerView.setControllerVisibilityListener(this);
    playerView.requestFocus();

    //Add by Tran Tien Anh
    retryButton = (Button) findViewById(R.id.retry_button);
    retryButton.setOnClickListener(this);

    player_stats_layout = (ScrollView) findViewById(R.id.player_stats_layout);
    player_stats_speed_chart = (LineChart) findViewById(R.id.player_stats_speed_chart);
    player_stats_health_chart = (LineChart) findViewById(R.id.player_stats_health_chart);
    player_stats_nw_chart = (LineChart) findViewById(R.id.player_stats_nw_chart);

    player_stats_res = (TextView) findViewById(R.id.player_stats_res);
    player_stats_size = (TextView) findViewById(R.id.player_stats_size);
    player_stats_dropframes = (TextView) findViewById(R.id.player_stats_dropframes);

    initStatChart(player_stats_speed_chart);
    initStatChart(player_stats_health_chart);
    initStatChart(player_stats_nw_chart);
    //Add by Tran Tien Anh

  }

  @Override
  public void onNewIntent(Intent intent) {
    releasePlayer();
    shouldAutoPlay = true;
    clearResumePosition();
    setIntent(intent);
  }

  @Override
  public void onStart() {
    super.onStart();
    if (Util.SDK_INT > 23) {
      initializePlayer();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (Util.SDK_INT <= 23 || player == null) {
      initializePlayer();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (Util.SDK_INT <= 23) {
      releasePlayer();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (Util.SDK_INT > 23) {
      releasePlayer();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    releaseAdsLoader();
    ml_model.close();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      initializePlayer();
    } else {
      showToast(R.string.storage_permission_denied);
      finish();
    }
  }

  // Activity input

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    // See whether the player view wants to handle media or DPAD keys events.
    return playerView.dispatchKeyEvent(event) || playerView.dispatchKeyEvent(event);
  }

  // OnClickListener methods

  @Override
  public void onClick(View view) {
//    if (view.getParent() == debugRootView) {
//      MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
//      if (mappedTrackInfo != null) {
//        trackSelectionHelper.showSelectionDialog(
//            this, ((Button) view).getText(), mappedTrackInfo, (int) view.getTag());
//      }
//    }
    if (view == retryButton) {
      initializePlayer();
    } else if (view.getParent() == debugRootView) {
      if ("stats".equals(view.getTag())) {
        if (player_stats_layout.getVisibility() == View.VISIBLE) {
          ((TextView) view).setText("Stats: OFF");
          player_stats_layout.setVisibility(View.GONE);
        } else {
          player_stats_layout.setVisibility(View.VISIBLE);
          ((TextView) view).setText("Stats: ON");
        }
      } else {
        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
          trackSelectionHelper.showSelectionDialog(this, ((Button) view).getText(),
                  trackSelector.getCurrentMappedTrackInfo(), (int) view.getTag());
        }
      }
    }
  }

  // PlaybackControlView.PlaybackPreparer implementation

  @Override
  public void preparePlayback() {
    initializePlayer();
  }

  // PlaybackControlView.VisibilityListener implementation

  @Override
  public void onVisibilityChange(int visibility) {
    debugRootView.setVisibility(visibility);
  }

  // Internal methods

  private void initializePlayer() {
    Intent intent = getIntent();
    boolean needNewPlayer = player == null;
    if (needNewPlayer) {
      TrackSelection.Factory adaptiveTrackSelectionFactory =
          new AdaptiveTrackSelection.Factory(bandwidthMeter);
      trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
      trackSelectionHelper = new TrackSelectionHelper(trackSelector, adaptiveTrackSelectionFactory);
      lastSeenTrackGroupArray = null;
      eventLogger = new EventLogger(trackSelector);

      DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
      if (intent.hasExtra(DRM_SCHEME_EXTRA) || intent.hasExtra(DRM_SCHEME_UUID_EXTRA)) {
        String drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL);
        String[] keyRequestPropertiesArray = intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES);
        boolean multiSession = intent.getBooleanExtra(DRM_MULTI_SESSION, false);
        int errorStringId = R.string.error_drm_unknown;
        if (Util.SDK_INT < 18) {
          errorStringId = R.string.error_drm_not_supported;
        } else {
          try {
            String drmSchemeExtra = intent.hasExtra(DRM_SCHEME_EXTRA) ? DRM_SCHEME_EXTRA
                : DRM_SCHEME_UUID_EXTRA;
            UUID drmSchemeUuid = Util.getDrmUuid(intent.getStringExtra(drmSchemeExtra));
            if (drmSchemeUuid == null) {
              errorStringId = R.string.error_drm_unsupported_scheme;
            } else {
              drmSessionManager =
                  buildDrmSessionManagerV18(
                      drmSchemeUuid, drmLicenseUrl, keyRequestPropertiesArray, multiSession);
            }
          } catch (UnsupportedDrmException e) {
            errorStringId = e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown;
          }
        }
        if (drmSessionManager == null) {
          showToast(errorStringId);
          return;
        }
      }

      boolean preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false);
      @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode =
          ((DemoApplication) getApplication()).useExtensionRenderers()
              ? (preferExtensionDecoders ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
              : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
              : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
      DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this,
          drmSessionManager, extensionRendererMode);

      player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, new CustomLoadControl(new CustomLoadControl.EventListener() {
        @Override
        public void onBufferedDurationSample(long bufferedDurationUs) {
          bufferedDurationMs = bufferedDurationUs;
        }
      },mUiUpdateHandler));
      player.addListener(this);
      player.setRepeatMode(Player.REPEAT_MODE_ALL);


      //eventLogger = new EventLogger(trackSelector);
      player.addAudioDebugListener(eventLogger);
      player.addVideoDebugListener(new VideoRendererEventListener(){

        @Override
        public void onVideoEnabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onVideoInputFormatChanged(Format format) {
          currentVideoFormat = format;
        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {
          droppedFrames += count;
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {
          startPlayerStats();
        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {

        }
      });
      player.addMetadataOutput(eventLogger);
      player.addListener(eventLogger);

      playerView.setPlayer(player);
      player.setPlayWhenReady(shouldAutoPlay);
      debugViewHelper = new DebugTextViewHelper(player, debugTextView);
      debugViewHelper.start();
    }
    if (needNewPlayer || inErrorState){
      String action = intent.getAction();
      Uri[] uris;
      String[] extensions;
      if (ACTION_VIEW.equals(action)) {
        uris = new Uri[]{intent.getData()};
        extensions = new String[]{intent.getStringExtra(EXTENSION_EXTRA)};
      } else if (ACTION_VIEW_LIST.equals(action)) {
        String[] uriStrings = intent.getStringArrayExtra(URI_LIST_EXTRA);
        uris = new Uri[uriStrings.length];
        for (int i = 0; i < uriStrings.length; i++) {
          uris[i] = Uri.parse(uriStrings[i]);
        }
        extensions = intent.getStringArrayExtra(EXTENSION_LIST_EXTRA);
        if (extensions == null) {
          extensions = new String[uriStrings.length];
        }
      } else {
        showToast(getString(R.string.unexpected_intent_action, action));
        return;
      }
      if (Util.maybeRequestReadExternalStoragePermission(this, uris)) {
        // The player will be reinitialized if the permission is granted.
        return;
      }
      MediaSource[] mediaSources = new MediaSource[uris.length];
      for (int i = 0; i < uris.length; i++) {
        System.out.println("uri"+uris[i]);
        mediaSources[i] = buildMediaSource(uris[i], extensions[i], mainHandler, eventLogger);
      }
      MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
              : new ConcatenatingMediaSource(mediaSources);
      String adTagUriString = intent.getStringExtra(AD_TAG_URI_EXTRA);
      if (adTagUriString != null) {
        Uri adTagUri = Uri.parse(adTagUriString);
        if (!adTagUri.equals(loadedAdTagUri)) {
          releaseAdsLoader();
          loadedAdTagUri = adTagUri;
        }
        MediaSource adsMediaSource = createAdsMediaSource(mediaSource, Uri.parse(adTagUriString));
        if (adsMediaSource != null) {
          mediaSource = adsMediaSource;
        } else {
          showToast(R.string.ima_not_loaded);
        }
      } else {
        releaseAdsLoader();
      }
      boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
      if (haveResumePosition) {
        player.seekTo(resumeWindow, resumePosition);
      }
      player.prepare(mediaSource, !haveResumePosition, false);
      inErrorState = false;
      updateButtonVisibilities();
    }

  }

  private MediaSource buildMediaSource(
      Uri uri,
      String overrideExtension,
      @Nullable Handler handler,
      @Nullable MediaSourceEventListener listener) {
    @ContentType int type = TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
        : Util.inferContentType("." + overrideExtension);
    switch (type) {
      case C.TYPE_DASH:
        System.out.println();
        return new DashMediaSource.Factory(
                new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                buildDataSourceFactory(false))
            .createMediaSource(uri, handler, listener);
      case C.TYPE_SS:
        return new SsMediaSource.Factory(
                new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                buildDataSourceFactory(false))
            .createMediaSource(uri, handler, listener);
      case C.TYPE_HLS:
        return new HlsMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(uri, handler, listener);
      case C.TYPE_OTHER:
        return new ExtractorMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(uri, handler, listener);
      default: {
        throw new IllegalStateException("Unsupported type: " + type);
      }
    }
  }

  private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManagerV18(UUID uuid,
      String licenseUrl, String[] keyRequestPropertiesArray, boolean multiSession)
      throws UnsupportedDrmException {
    HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
        buildHttpDataSourceFactory(false));
    if (keyRequestPropertiesArray != null) {
      for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
        drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
            keyRequestPropertiesArray[i + 1]);
      }
    }
    return new DefaultDrmSessionManager<>(uuid, FrameworkMediaDrm.newInstance(uuid), drmCallback,
        null, mainHandler, eventLogger, multiSession);
  }

  private void releasePlayer() {
    if (player != null) {
      debugViewHelper.stop();
      debugViewHelper = null;
      shouldAutoPlay = player.getPlayWhenReady();
      updateResumePosition();
      player.release();
      player = null;
      trackSelector = null;
      trackSelectionHelper = null;
      eventLogger = null;
    }
    if (null != mUiUpdateHandler)
      mUiUpdateHandler.removeCallbacksAndMessages(null);
  }

  private void updateResumePosition() {
    resumeWindow = player.getCurrentWindowIndex();
//    resumePosition = Math.max(0, player.getContentPosition());
    resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
            : C.TIME_UNSET;
  }

  private void clearResumePosition() {
    resumeWindow = C.INDEX_UNSET;
    resumePosition = C.TIME_UNSET;
  }

  /**
   * Returns a new DataSource factory.
   *
   * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
   *     DataSource factory.
   * @return A new DataSource factory.
   */
  private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
    return ((DemoApplication) getApplication())
        .buildDataSourceFactory(useBandwidthMeter ? bandwidthMeter : null);
  }

  /**
   * Returns a new HttpDataSource factory.
   *
   * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
   *     DataSource factory.
   * @return A new HttpDataSource factory.
   */
  private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
    return ((DemoApplication) getApplication())
        .buildHttpDataSourceFactory(useBandwidthMeter ? bandwidthMeter : null);
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

  }

  @Override
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    updateButtonVisibilities();
    if (trackGroups != lastSeenTrackGroupArray) {
      MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo != null) {
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          showToast(R.string.error_unsupported_video);
        }
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          showToast(R.string.error_unsupported_audio);
        }
      }
      lastSeenTrackGroupArray = trackGroups;
    }
  }



  @Override
  public void onLoadingChanged(boolean isLoading) {
    // Do nothing.
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    if (playbackState == Player.STATE_ENDED) {
      showControls();
    }
    updateButtonVisibilities();
  }

  @Override
  public void onRepeatModeChanged(int repeatMode) {

  }

  @Override
  public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

  }



  @Override
  public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    // Do nothing.
  }

  @Override
  public void onSeekProcessed() {

  }



  @Override
  public void onPlayerError(ExoPlaybackException e) {
    String errorString = null;
    if (e.type == ExoPlaybackException.TYPE_RENDERER) {
      Exception cause = e.getRendererException();
      if (cause instanceof DecoderInitializationException) {
        // Special case for decoder initialization failures.
        DecoderInitializationException decoderInitializationException =
                (DecoderInitializationException) cause;
        if (decoderInitializationException.decoderName == null) {
          if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
            errorString = getString(R.string.error_querying_decoders);
          } else if (decoderInitializationException.secureDecoderRequired) {
            errorString = getString(R.string.error_no_secure_decoder,
                    decoderInitializationException.mimeType);
          } else {
            errorString = getString(R.string.error_no_decoder,
                    decoderInitializationException.mimeType);
          }
        } else {
          errorString = getString(R.string.error_instantiating_decoder,
                  decoderInitializationException.decoderName);
        }
      }
    }
    if (errorString != null) {
      showToast(errorString);
    }
    inErrorState = true;
    if (isBehindLiveWindow(e)) {
      clearResumePosition();
      initializePlayer();
    } else {
      updateResumePosition();
      updateButtonVisibilities();
      showControls();
    }
  }

  @Override
  public void onPositionDiscontinuity(int reason) {
    if (inErrorState) {
      // This will only occur if the user has performed a seek whilst in the error state. Update the
      // resume position so that if the user then retries, playback will resume from the position to
      // which they seeked.
      updateResumePosition();
    }
  }

  /** Returns an ads media source, reusing the ads loader if one exists. */
  private @Nullable MediaSource createAdsMediaSource(MediaSource mediaSource, Uri adTagUri) {
    // Load the extension source using reflection so the demo app doesn't have to depend on it.
    // The ads loader is reused for multiple playbacks, so that ad playback can resume.
    try {
      Class<?> loaderClass = Class.forName("com.google.android.exoplayer2.ext.ima.ImaAdsLoader");
      if (adsLoader == null) {
        // Full class names used so the LINT.IfChange rule triggers should any of the classes move.
        // LINT.IfChange
        Constructor<? extends AdsLoader> loaderConstructor =
            loaderClass
                .asSubclass(AdsLoader.class)
                .getConstructor(android.content.Context.class, android.net.Uri.class);
        // LINT.ThenChange(../../../../../../../../proguard-rules.txt)
        adsLoader = loaderConstructor.newInstance(this, adTagUri);
        adUiViewGroup = new FrameLayout(this);
        // The demo app has a non-null overlay frame layout.
        playerView.getOverlayFrameLayout().addView(adUiViewGroup);
      }
      AdsMediaSource.MediaSourceFactory adMediaSourceFactory =
          new AdsMediaSource.MediaSourceFactory() {
            @Override
            public MediaSource createMediaSource(
                Uri uri, @Nullable Handler handler, @Nullable MediaSourceEventListener listener) {
              return PlayerActivity.this.buildMediaSource(
                  uri, /* overrideExtension= */ null, handler, listener);
            }

            @Override
            public int[] getSupportedTypes() {
              return new int[] {C.TYPE_DASH, C.TYPE_SS, C.TYPE_HLS, C.TYPE_OTHER};
            }
          };
      return new AdsMediaSource(
          mediaSource, adMediaSourceFactory, adsLoader, adUiViewGroup, mainHandler, eventLogger);
    } catch (ClassNotFoundException e) {
      // IMA extension not loaded.
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void releaseAdsLoader() {
    if (adsLoader != null) {
      adsLoader.release();
      adsLoader = null;
      loadedAdTagUri = null;
      playerView.getOverlayFrameLayout().removeAllViews();
    }
  }

  // User controls

  private void updateButtonVisibilities() {
    debugRootView.removeAllViews();

    retryButton.setVisibility(inErrorState ? View.VISIBLE : View.GONE);
    debugRootView.addView(retryButton);

    if (player == null) {
      return;
    }

    MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
    if (mappedTrackInfo == null) {
      return;
    }

    for (int i = 0; i < mappedTrackInfo.length; i++) {
      TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
      if (trackGroups.length != 0) {
        Button button = new Button(this);
        int label;
        switch (player.getRendererType(i)) {
          case C.TRACK_TYPE_AUDIO:
            label = R.string.audio;
            break;
          case C.TRACK_TYPE_VIDEO:
            label = R.string.video;
            break;
          case C.TRACK_TYPE_TEXT:
            label = R.string.text;
            break;
          default:
            continue;
        }
        button.setText(label);
        button.setTag(i);
        button.setOnClickListener(this);
        debugRootView.addView(button);
      }
    }
    Button statsButton = new Button(this);
    statsButton.setText("Stats-OFF");
    statsButton.setTag("stats");
    statsButton.setOnClickListener(this);
    debugRootView.addView(statsButton, debugRootView.getChildCount() - 1);
  }

  private void showControls() {
    debugRootView.setVisibility(View.VISIBLE);
  }

  private void showToast(int messageId) {
    showToast(getString(messageId));
  }

  private void showToast(String message) {
    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
  }

  private static boolean isBehindLiveWindow(ExoPlaybackException e) {
    if (e.type != ExoPlaybackException.TYPE_SOURCE) {
      return false;
    }
    Throwable cause = e.getSourceException();
    while (cause != null) {
      if (cause instanceof BehindLiveWindowException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  private void startPlayerStats() {
    mUiUpdateHandler.removeMessages(MSG_UPDATE_STATS);
    mUiUpdateHandler.removeMessages(MSG_UPDATE_STATS_NW_ONLY);
    depictPlayerStats();
    depictPlayerNWStats();
  }

  protected void depictPlayerStats() {
    if (!canShowStats())
      return;
    String buffer = DemoUtil.getFormattedDouble((bufferedDurationMs / Math.pow(10, 6)), 1).replace(',', '.');
    String brEstimate = DemoUtil.getFormattedDouble((bitrateEstimate / Math.pow(10, 3)), 1).replace(',', '.');;
    updateStatChart(player_stats_health_chart, Float.parseFloat(buffer), ColorTemplate.getHoloBlue(), "Buffer Health: " + buffer + " s");
   Log.e("DLC","Buffer Health: " + buffer +" s");

    updateStatChart(player_stats_speed_chart, Float.parseFloat(brEstimate), Color.LTGRAY, "Conn Speed: " + DemoUtil.humanReadableByteCount(
            bitrateEstimate, true, true) + "ps");
   //Log.e("DLC","Bitrate: " + DemoUtil.humanReadableByteCount(
    //     bitrateEstimate, true, true) +" ps" );

//    File file = new File("./sdcard/avi/");
//    file.mkdirs();
//    String csv = "./sdcard/avi/a.csv";
//    try {
//      CSVWriter csvWriter = new CSVWriter(new FileWriter(csv, true));
//      String row[] = new String[]{buffer,brEstimate};
//      csvWriter.writeNext(row);
//      csvWriter.close();
//      Log.e("hello", "hello");
//    } catch (IOException e) {
//
//      e.printStackTrace();
//    }
    mediaDataSourceFactory.createDataSource();

    player_stats_size.setText("Screen Dimensions: " + playerView.getWidth() + " x " + playerView.getHeight());
    player_stats_res.setText("Video Resolution: " + (null != currentVideoFormat ? (currentVideoFormat.width + " x " + currentVideoFormat.height) : "NA"));
    player_stats_dropframes.setText("Dropped Frames: " + droppedFrames);
    mUiUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_STATS, 500);
  }

  protected void depictPlayerNWStats() {
    if (!canShowStats())
      return;
    updateStatChart(player_stats_nw_chart, (float) (bytesDownloaded / Math.pow(10, 3)), Color.CYAN, "Network Activity: " + DemoUtil.humanReadableByteCount(
            bytesDownloaded, true));
    bytesDownloaded = 0;
    mUiUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_STATS_NW_ONLY, 1100);
  }

  private boolean canShowStats() {
    if (null == mUiUpdateHandler) {
      player_stats_layout.setVisibility(View.GONE);
      return false;
    }
    return true;
  }

  private void initStatChart(LineChart chart) {
    // disable description text
    chart.getDescription().setEnabled(false);
    chart.setEnabled(false);
    chart.getLegend().setTextColor(Color.WHITE);
    // disable scaling and dragging
    chart.setTouchEnabled(false);
    chart.setDragEnabled(false);
    chart.setScaleEnabled(false);
    chart.setDrawGridBackground(false);
    // if disabled, scaling can be done on x- and y-axis separately
    chart.setPinchZoom(false);
    // set an alternative background color
    chart.setBackgroundColor(Color.TRANSPARENT);
    chart.getXAxis().setEnabled(false);
    XAxis speed_xl = chart.getXAxis();
    speed_xl.setDrawLabels(false);
    speed_xl.setDrawGridLines(false);
    speed_xl.setAvoidFirstLastClipping(true);
    speed_xl.setPosition(XAxis.XAxisPosition.BOTTOM);
    speed_xl.setEnabled(true);
    YAxis speed_yl = chart.getAxisRight();
    speed_yl.setDrawLabels(false);
    speed_yl.setDrawGridLines(false);
    speed_yl.setEnabled(true);
    YAxis speed_rightAxis = chart.getAxisLeft();
    speed_rightAxis.setEnabled(false);
    LineData speed_data = new LineData();
    // add empty data
    chart.setData(speed_data);
  }

  private void updateStatChart(LineChart chart, float value, int color, String formattedValue) {

    LineData data = chart.getData();

    if (data != null) {

      ILineDataSet set = data.getDataSetByIndex(0);
      // set.addEntry(...); // can be called as well

      if (set == null) {
        set = createDataSetForChart(color);
        data.addDataSet(set);
      }
      set.setLabel(formattedValue);
      data.addEntry(new Entry(set.getEntryCount(), (int) value), 0);
      data.notifyDataChanged();

      // let the chart know it's data has changed
      chart.notifyDataSetChanged();

      // limit the number of visible entries
      chart.setVisibleXRangeMaximum(180);
      // mChart.setVisibleYRange(30, AxisDependency.LEFT);

      // move to the latest entry
      chart.moveViewToX(data.getEntryCount());

      // this automatically refreshes the chart (calls invalidate())
      // mChart.moveViewTo(data.getXValCount()-7, 55f,
      // AxisDependency.LEFT);
    }
  }

  private LineDataSet createDataSetForChart(int color) {
    LineDataSet set = new LineDataSet(null, "Dynamic Data");
    set.setAxisDependency(YAxis.AxisDependency.LEFT);
    set.setColor(color);
    set.setDrawFilled(true);
    set.setDrawCircles(false);
    set.setLineWidth(0);
    set.setFillAlpha(65);
    set.setFillColor(color);
//        set.setHighLightColor(Color.rgb(244, 117, 117));
    set.setDrawValues(false);
    return set;
  }


  private static class UiUpdateHandler extends Handler {

    WeakReference<PlayerActivity> mPlayerActivity;

    UiUpdateHandler(PlayerActivity activity) {
      mPlayerActivity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
      PlayerActivity activity = mPlayerActivity.get();
      if (null == activity)
        return;
      switch (msg.what) {
        case MSG_UPDATE_STATS:
          activity.depictPlayerStats();
          break;
        case MSG_UPDATE_STATS_NW_ONLY:
          activity.depictPlayerNWStats();
          break;
      }

    }
  }

}
