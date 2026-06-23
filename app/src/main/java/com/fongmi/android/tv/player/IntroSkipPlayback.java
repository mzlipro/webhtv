package com.fongmi.android.tv.player;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.service.IntroSkipService;
import com.fongmi.android.tv.service.IntroSkipService.IntroSkipPlan;
import com.fongmi.android.tv.service.IntroSkipService.Segment;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.crawler.SpiderDebug;

import java.util.HashSet;
import java.util.Set;

public class IntroSkipPlayback {

    private static final long TOLERANCE_MS = 1500;
    private static final long MIN_SKIP_DELTA_MS = 1500;

    private final IntroSkipService service = new IntroSkipService();
    private final Set<String> skipped = new HashSet<>();
    private IntroSkipPlan plan = IntroSkipPlan.empty();
    private String loadedKey = "";
    private String loadingKey = "";
    private int generation;
    private boolean loading;

    public void reset() {
        generation++;
        loading = false;
        loadedKey = "";
        loadingKey = "";
        plan = IntroSkipPlan.empty();
        skipped.clear();
    }

    public void request(IntroSkipService.Query query, Runnable onLoaded) {
        if (query == null || !query.hasLookupKey()) return;
        String key = query.cacheKey();
        if (key.equals(loadedKey) || (loading && key.equals(loadingKey))) return;
        int current = ++generation;
        loading = true;
        loadingKey = key;
        Task.execute(() -> {
            IntroSkipPlan loaded = service.load(query);
            App.post(() -> {
                if (current != generation || !key.equals(loadingKey)) return;
                loading = false;
                loadedKey = key;
                if (!loaded.isEmpty() || plan.isEmpty()) plan = loaded;
                SpiderDebug.log("intro-skip", "plan ready openings=%d endings=%d key=%s", plan.getOpenings().size(), plan.getEndings().size(), key);
                if (onLoaded != null) onLoaded.run();
            });
        });
    }

    public boolean apply(PlayerManager player, Runnable onEnding) {
        if (player == null || plan == null || plan.isEmpty()) return false;
        long position = player.getPosition();
        long duration = player.getDuration();
        if (position < 0) return false;
        if (applyOpening(player, position)) return true;
        return applyEnding(player, position, duration, onEnding);
    }

    private boolean applyOpening(PlayerManager player, long position) {
        for (Segment segment : plan.getOpenings()) {
            String id = id(segment);
            long end = segment.getEndMs();
            if (skipped.contains(id) || end <= 0) continue;
            if (position >= end - MIN_SKIP_DELTA_MS) {
                skipped.add(id);
                continue;
            }
            if (position + TOLERANCE_MS < segment.getStartMs()) continue;
            player.seekTo(end);
            skipped.add(id);
            SpiderDebug.log("intro-skip", "skip opening kind=%s provider=%s from=%d to=%d", segment.getKind(), segment.getProvider(), position, end);
            return true;
        }
        return false;
    }

    private boolean applyEnding(PlayerManager player, long position, long duration, Runnable onEnding) {
        for (Segment segment : plan.getEndings()) {
            String id = id(segment);
            if (skipped.contains(id)) continue;
            long start = segment.getStartMs();
            if (duration > 0 && start >= duration) {
                skipped.add(id);
                continue;
            }
            if (position + TOLERANCE_MS < start) continue;
            long end = segment.getEndMs();
            if (end > 0 && duration > 0 && end > duration) end = duration;
            skipped.add(id);
            SpiderDebug.log("intro-skip", "skip ending provider=%s from=%d start=%d end=%d duration=%d", segment.getProvider(), position, start, end, duration);
            if (end > 0 && (duration <= 0 || end < duration - TOLERANCE_MS) && end - position > MIN_SKIP_DELTA_MS) {
                player.seekTo(end);
            } else if (onEnding != null) {
                onEnding.run();
            }
            return true;
        }
        return false;
    }

    private String id(Segment segment) {
        return segment.getKind() + "|" + segment.getProvider() + "|" + segment.getStartMs() + "|" + segment.getEndMs();
    }
}
