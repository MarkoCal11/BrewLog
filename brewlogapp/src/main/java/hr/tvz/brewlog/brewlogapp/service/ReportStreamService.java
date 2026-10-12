package hr.tvz.brewlog.brewlogapp.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Holds the open Server-Sent Events streams for the live "new report" nudge, keyed by username.
 * One-way (server to client): {@link #subscribe} registers a client, {@link #publish} pushes a
 * named {@code report} event to that user's open streams, and a periodic comment heartbeat keeps
 * proxies from idle-closing the connection.
 *
 * <p>The registry is in-memory, so the push only reaches clients connected to <em>this</em>
 * instance. That is fine for a single-instance deployment; a multi-instance setup would need a
 * shared pub/sub hop (e.g. Redis) between {@code publish} and the emitters.
 */
@Service
public class ReportStreamService {

    private static final Logger log = LoggerFactory.getLogger(ReportStreamService.class);

    /** Stream lifetime before the client is asked to reconnect. */
    private static final long STREAM_TIMEOUT_MS = Duration.ofHours(1).toMillis();
    /** Heartbeat cadence; comfortably under the common 30s/60s proxy idle timeouts. */
    private static final long HEARTBEAT_SECONDS = 25L;
    private static final String REPORT_EVENT = "report";

    private final Map<String, Collection<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor(daemonThreadFactory());

    @PostConstruct
    void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeats, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() {
        heartbeatScheduler.shutdownNow();
        emittersByUser.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
        emittersByUser.clear();
    }

    /** Registers a new stream for {@code username} and returns the emitter for the controller to hand back. */
    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emittersByUser.computeIfAbsent(username, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(username, emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> remove(username, emitter));

        try {
            // Establish the stream immediately so the client's onopen fires without waiting for an event.
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            remove(username, emitter);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /** Pushes a {@code report} event carrying {@code payload} to every open stream owned by {@code username}. */
    public void publish(String username, Object payload) {
        Collection<SseEmitter> emitters = emittersByUser.get(username);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(REPORT_EVENT).data(payload));
            } catch (IOException | IllegalStateException e) {
                // Stream is dead (client gone / already completed); drop it via the error callback.
                emitter.completeWithError(e);
            }
        }
    }

    private void sendHeartbeats() {
        emittersByUser.forEach((username, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("hb"));
                } catch (IOException | IllegalStateException e) {
                    emitter.completeWithError(e);
                }
            }
        });
    }

    private void remove(String username, SseEmitter emitter) {
        emittersByUser.computeIfPresent(username, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "report-sse-heartbeat");
            thread.setDaemon(true);
            return thread;
        };
    }
}
