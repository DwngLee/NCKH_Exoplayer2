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
package com.google.android.exoplayer2.source.hls;

import android.net.Uri;
import android.os.Handler;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.source.CompositeSequenceableLoaderFactory;
import com.google.android.exoplayer2.source.DefaultCompositeSequenceableLoaderFactory;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.MediaSourceEventListener.EventDispatcher;
import com.google.android.exoplayer2.source.SequenceableLoader;
import com.google.android.exoplayer2.source.SinglePeriodTimeline;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistParser;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistTracker;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.ParsingLoadable;
import com.google.android.exoplayer2.util.Assertions;

import org.w3c.dom.ls.LSOutput;

import java.io.IOException;
import java.util.List;

/**
 * An HLS {@link MediaSource}.
 */
public final class HlsMediaSource implements MediaSource,
    HlsPlaylistTracker.PrimaryPlaylistListener {

  static {
    ExoPlayerLibraryInfo.registerModule("goog.exo.hls");
  }

  /** Factory for {@link HlsMediaSource}s. */
  public static final class Factory implements AdsMediaSource.MediaSourceFactory {

    private final HlsDataSourceFactory hlsDataSourceFactory;

    private HlsExtractorFactory extractorFactory;
    private @Nullable ParsingLoadable.Parser<HlsPlaylist> playlistParser;
    private CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
    private int minLoadableRetryCount;
    private boolean allowChunklessPreparation;
    private boolean isCreateCalled;

    /**
     * Creates a new factory for {@link HlsMediaSource}s.
     *
     * @param dataSourceFactory A data source factory that will be wrapped by a {@link
     *     DefaultHlsDataSourceFactory} to create {@link DataSource}s for manifests, segments and
     *     keys.
     */
    public Factory(DataSource.Factory dataSourceFactory) {
      this(new DefaultHlsDataSourceFactory(dataSourceFactory));
    }

    /**
     * Creates a new factory for {@link HlsMediaSource}s.
     *
     * @param hlsDataSourceFactory An {@link HlsDataSourceFactory} for {@link DataSource}s for
     *     manifests, segments and keys.
     */
    public Factory(HlsDataSourceFactory hlsDataSourceFactory) {
      this.hlsDataSourceFactory = Assertions.checkNotNull(hlsDataSourceFactory);
      extractorFactory = HlsExtractorFactory.DEFAULT;
      minLoadableRetryCount = DEFAULT_MIN_LOADABLE_RETRY_COUNT;
      compositeSequenceableLoaderFactory = new DefaultCompositeSequenceableLoaderFactory();
    }

    /**
     * Sets the factory for {@link Extractor}s for the segments. The default value is {@link
     * HlsExtractorFactory#DEFAULT}.
     *
     * @param extractorFactory An {@link HlsExtractorFactory} for {@link Extractor}s for the
     *     segments.
     * @return This factory, for convenience.
     * @throws IllegalStateException If one of the {@code create} methods has already been called.
     */
    public Factory setExtractorFactory(HlsExtractorFactory extractorFactory) {
      Assertions.checkState(!isCreateCalled);
      this.extractorFactory = Assertions.checkNotNull(extractorFactory);
      return this;
    }

    /**
     * Sets the minimum number of times to retry if a loading error occurs. The default value is
     * {@link #DEFAULT_MIN_LOADABLE_RETRY_COUNT}.
     *
     * @param minLoadableRetryCount The minimum number of times to retry if a loading error occurs.
     * @return This factory, for convenience.
     * @throws IllegalStateException If one of the {@code create} methods has already been called.
     */
    public Factory setMinLoadableRetryCount(int minLoadableRetryCount) {
      Assertions.checkState(!isCreateCalled);
      this.minLoadableRetryCount = minLoadableRetryCount;
      return this;
    }

    /**
     * Sets the parser to parse HLS playlists. The default is an instance of {@link
     * HlsPlaylistParser}.
     *
     * @param playlistParser A {@link ParsingLoadable.Parser} for HLS playlists.
     * @return This factory, for convenience.
     * @throws IllegalStateException If one of the {@code create} methods has already been called.
     */
    public Factory setPlaylistParser(ParsingLoadable.Parser<HlsPlaylist> playlistParser) {
      Assertions.checkState(!isCreateCalled);
      this.playlistParser = Assertions.checkNotNull(playlistParser);
      return this;
    }

    /**
     * Sets the factory to create composite {@link SequenceableLoader}s for when this media source
     * loads data from multiple streams (video, audio etc...). The default is an instance of {@link
     * DefaultCompositeSequenceableLoaderFactory}.
     *
     * @param compositeSequenceableLoaderFactory A factory to create composite {@link
     *     SequenceableLoader}s for when this media source loads data from multiple streams (video,
     *     audio etc...).
     * @return This factory, for convenience.
     * @throws IllegalStateException If one of the {@code create} methods has already been called.
     */
    public Factory setCompositeSequenceableLoaderFactory(
        CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory) {
      Assertions.checkState(!isCreateCalled);
      this.compositeSequenceableLoaderFactory =
          Assertions.checkNotNull(compositeSequenceableLoaderFactory);
      return this;
    }

    /**
     * Sets whether chunkless preparation is allowed. If true, preparation without chunk downloads
     * will be enabled for streams that provide sufficient information in their master playlist.
     *
     * @param allowChunklessPreparation Whether chunkless preparation is allowed.
     * @return This factory, for convenience.
     * @throws IllegalStateException If one of the {@code create} methods has already been called.
     */
    public Factory setAllowChunklessPreparation(boolean allowChunklessPreparation) {
      Assertions.checkState(!isCreateCalled);
      this.allowChunklessPreparation = allowChunklessPreparation;
      return this;
    }

    /**
     * Returns a new {@link HlsMediaSource} using the current parameters. Media source events will
     * not be delivered.
     *
     * @return The new {@link HlsMediaSource}.
     */
    public HlsMediaSource createMediaSource(Uri playlistUri) {
      return createMediaSource(playlistUri, null, null);
    }

    /**
     * Returns a new {@link HlsMediaSource} using the current parameters.
     *
     * @param playlistUri The playlist {@link Uri}.
     * @param eventHandler A handler for events.
     * @param eventListener A listener of events.
     * @return The new {@link HlsMediaSource}.
     */
    @Override
    public HlsMediaSource createMediaSource(
        Uri playlistUri,
        @Nullable Handler eventHandler,
        @Nullable MediaSourceEventListener eventListener) {
      isCreateCalled = true;
      if (playlistParser == null) {
        playlistParser = new HlsPlaylistParser();
      }
      return new HlsMediaSource(
          playlistUri,
          hlsDataSourceFactory,
          extractorFactory,
          compositeSequenceableLoaderFactory,
          minLoadableRetryCount,
          eventHandler,
          eventListener,
          playlistParser,
          allowChunklessPreparation);
    }

    @Override
    public int[] getSupportedTypes() {
      return new int[] {C.TYPE_HLS};
    }
  }

  /**
   * The default minimum number of times to retry loading data prior to failing.
   */
  public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT = 3;

  private final HlsExtractorFactory extractorFactory;
  private final Uri manifestUri;
  private final HlsDataSourceFactory dataSourceFactory;
  private final CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
  private final int minLoadableRetryCount;
  private final EventDispatcher eventDispatcher;
  private final ParsingLoadable.Parser<HlsPlaylist> playlistParser;
  private final boolean allowChunklessPreparation;

  private HlsPlaylistTracker playlistTracker;
  private Listener sourceListener;

  /**
   * @param manifestUri The {@link Uri} of the HLS manifest.
   * @param dataSourceFactory An {@link HlsDataSourceFactory} for {@link DataSource}s for manifests,
   *     segments and keys.
   * @param eventHandler A handler for events. May be null if delivery of events is not required.
   * @param eventListener A {@link MediaSourceEventListener}. May be null if delivery of events is
   *     not required.
   * @deprecated Use {@link Factory} instead.
   */
  @Deprecated
  public HlsMediaSource(
      Uri manifestUri,
      DataSource.Factory dataSourceFactory,
      Handler eventHandler,
      MediaSourceEventListener eventListener) {
    this(manifestUri, dataSourceFactory, DEFAULT_MIN_LOADABLE_RETRY_COUNT, eventHandler,
        eventListener);
  }

  /**
   * @param manifestUri The {@link Uri} of the HLS manifest.
   * @param dataSourceFactory An {@link HlsDataSourceFactory} for {@link DataSource}s for manifests,
   *     segments and keys.
   * @param minLoadableRetryCount The minimum number of times loads must be retried before errors
   *     are propagated.
   * @param eventHandler A handler for events. May be null if delivery of events is not required.
   * @param eventListener A {@link MediaSourceEventListener}. May be null if delivery of events is
   *     not required.
   * @deprecated Use {@link Factory} instead.
   */
  @Deprecated
  public HlsMediaSource(
      Uri manifestUri,
      DataSource.Factory dataSourceFactory,
      int minLoadableRetryCount,
      Handler eventHandler,
      MediaSourceEventListener eventListener) {
    this(manifestUri, new DefaultHlsDataSourceFactory(dataSourceFactory),
        HlsExtractorFactory.DEFAULT, minLoadableRetryCount, eventHandler, eventListener,
        new HlsPlaylistParser());
  }

  /**
   * @param manifestUri The {@link Uri} of the HLS manifest.
   * @param dataSourceFactory An {@link HlsDataSourceFactory} for {@link DataSource}s for manifests,
   *     segments and keys.
   * @param extractorFactory An {@link HlsExtractorFactory} for {@link Extractor}s for the segments.
   * @param minLoadableRetryCount The minimum number of times loads must be retried before errors
   *     are propagated.
   * @param eventHandler A handler for events. May be null if delivery of events is not required.
   * @param eventListener A {@link MediaSourceEventListener}. May be null if delivery of events is
   *     not required.
   * @param playlistParser A {@link ParsingLoadable.Parser} for HLS playlists.
   * @deprecated Use {@link Factory} instead.
   */
  @Deprecated
  public HlsMediaSource(
      Uri manifestUri,
      HlsDataSourceFactory dataSourceFactory,
      HlsExtractorFactory extractorFactory,
      int minLoadableRetryCount,
      Handler eventHandler,
      MediaSourceEventListener eventListener,
      ParsingLoadable.Parser<HlsPlaylist> playlistParser) {
    this(
        manifestUri,
        dataSourceFactory,
        extractorFactory,
        new DefaultCompositeSequenceableLoaderFactory(),
        minLoadableRetryCount,
        eventHandler,
        eventListener,
        playlistParser,
        false);
  }

  private HlsMediaSource(
      Uri manifestUri,
      HlsDataSourceFactory dataSourceFactory,
      HlsExtractorFactory extractorFactory,
      CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory,
      int minLoadableRetryCount,
      Handler eventHandler,
      MediaSourceEventListener eventListener,
      ParsingLoadable.Parser<HlsPlaylist> playlistParser,
      boolean allowChunklessPreparation) {
    this.manifestUri = manifestUri;
    this.dataSourceFactory = dataSourceFactory;
    this.extractorFactory = extractorFactory;
    this.compositeSequenceableLoaderFactory = compositeSequenceableLoaderFactory;
    this.minLoadableRetryCount = minLoadableRetryCount;
    this.playlistParser = playlistParser;
    this.allowChunklessPreparation = allowChunklessPreparation;
    eventDispatcher = new EventDispatcher(eventHandler, eventListener);
  }

  @Override
  public void prepareSource(ExoPlayer player, boolean isTopLevelSource, Listener listener) {
    sourceListener = listener;
    playlistTracker = new HlsPlaylistTracker(manifestUri, dataSourceFactory, eventDispatcher,
        minLoadableRetryCount, this, playlistParser);
    playlistTracker.start();
  }

  @Override
  public void maybeThrowSourceInfoRefreshError() throws IOException {
    playlistTracker.maybeThrowPrimaryPlaylistRefreshError();
  }

  @Override
  public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
    Assertions.checkArgument(id.periodIndex == 0);
    return new HlsMediaPeriod(
        extractorFactory,
        playlistTracker,
        dataSourceFactory,
        minLoadableRetryCount,
        eventDispatcher,
        allocator,
        compositeSequenceableLoaderFactory,
        allowChunklessPreparation);
  }

  @Override
  public void releasePeriod(MediaPeriod mediaPeriod) {
    ((HlsMediaPeriod) mediaPeriod).release();
  }

  @Override
  public void releaseSource() {
    if (playlistTracker != null) {
      playlistTracker.release();
      playlistTracker = null;
    }
    sourceListener = null;
  }

  @Override
  public void onPrimaryPlaylistRefreshed(HlsMediaPlaylist playlist) {
    SinglePeriodTimeline timeline;
    long windowStartTimeMs = playlist.hasProgramDateTime ? C.usToMs(playlist.startTimeUs)
        : C.TIME_UNSET;
    // For playlist types EVENT and VOD we know segments are never removed, so the presentation
    // started at the same time as the window. Otherwise, we don't know the presentation start time.
    long presentationStartTimeMs =
        playlist.playlistType == HlsMediaPlaylist.PLAYLIST_TYPE_EVENT
                || playlist.playlistType == HlsMediaPlaylist.PLAYLIST_TYPE_VOD
            ? windowStartTimeMs
            : C.TIME_UNSET;
    long windowDefaultStartPositionUs = playlist.startOffsetUs;
    if (playlistTracker.isLive()) {
      long offsetFromInitialStartTimeUs =
          playlist.startTimeUs - playlistTracker.getInitialStartTimeUs();
      long periodDurationUs =
          playlist.hasEndTag ? offsetFromInitialStartTimeUs + playlist.durationUs : C.TIME_UNSET;
      List<HlsMediaPlaylist.Segment> segments = playlist.segments;
      if (windowDefaultStartPositionUs == C.TIME_UNSET) {
        windowDefaultStartPositionUs = segments.isEmpty() ? 0
            : segments.get(Math.max(0, segments.size() - 3)).relativeStartTimeUs;
      }
      timeline =
          new SinglePeriodTimeline(
              presentationStartTimeMs,
              windowStartTimeMs,
              periodDurationUs,
              /* windowDurationUs= */ playlist.durationUs,
              /* windowPositionInPeriodUs= */ offsetFromInitialStartTimeUs,
              windowDefaultStartPositionUs,
              /* isSeekable= */ true,
              /* isDynamic= */ !playlist.hasEndTag);
    } else /* not live */ {
      if (windowDefaultStartPositionUs == C.TIME_UNSET) {
        windowDefaultStartPositionUs = 0;
      }
      timeline =
          new SinglePeriodTimeline(
              presentationStartTimeMs,
              windowStartTimeMs,
              /* periodDurationUs= */ playlist.durationUs,
              /* windowDurationUs= */ playlist.durationUs,
              /* windowPositionInPeriodUs= */ 0,
              windowDefaultStartPositionUs,
              /* isSeekable= */ true,
              /* isDynamic= */ false);
    }
    sourceListener.onSourceInfoRefreshed(this, timeline,
        new HlsManifest(playlistTracker.getMasterPlaylist(), playlist));
  }

}
