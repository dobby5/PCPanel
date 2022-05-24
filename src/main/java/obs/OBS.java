package obs;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.log4j.Log4j2;
import obsremote.OBSRemoteController;
import obsremote.requests.GetSceneListResponse;
import obsremote.requests.GetSourceTypeListResponse;
import save.Save;

@Log4j2
public class OBS {
    public static volatile OBSRemoteController controller;
    public static final Object OBSMutex = new Object();
    private static final long WAIT_TIME = 1000L;

    public static void main(String[] args) throws InterruptedException {
        controller = new OBSRemoteController("ws://localhost:4444", "123");
        if (controller.isFailed())
            log.error("FAILURE: Controller failed");
        log.debug("{}", getScenes());
        setCurrentScene("Scene");
    }

    public static List<String> getSourcesWithAudio() {
        List<String> sourcesWithAudio = new ArrayList<>();
        List<String> typesWithAudio = new ArrayList<>();
        try {
            controller.getSourceTypes(r -> {
                var response = (GetSourceTypeListResponse) r;
                for (var st : response.getSourceTypes()) {
                    if (st.getCaps().isAudio())
                        typesWithAudio.add(st.getTypeId());
                }
                controller.getSources(x -> {
                });
            });
            synchronized (sourcesWithAudio) {
                sourcesWithAudio.wait(WAIT_TIME);
            }
        } catch (Exception e) {
            log.error("Unable to get sources with audio", e);
        }
        return sourcesWithAudio;
    }

    public static List<String> getScenes() {
        List<String> scenes = new ArrayList<>();
        try {
            controller.getScenes(r -> {
                synchronized (scenes) {
                    var response = (GetSceneListResponse) r;
                    if (response.getScenes() != null)
                        for (var scene : response.getScenes())
                            scenes.add(scene.getName());
                    scenes.notify();
                }
            });
            synchronized (scenes) {
                scenes.wait(WAIT_TIME);
            }
        } catch (Exception e) {
            log.error("Unable to get scenes", e);
        }
        return scenes;
    }

    public static void setSourceVolume(String sourceName, int vol) {
        var waiter = new Object();
        try {
            var decimal = vol / 100.0D;
            controller.setVolume(sourceName, decimal, c -> {
                synchronized (waiter) {
                    waiter.notify();
                }
            });
            synchronized (waiter) {
                waiter.wait(WAIT_TIME);
            }
        } catch (Exception e) {
            log.error("Unable to get source volume", e);
        }
    }

    public static void toggleSourceMute(String sourceName) {
        var waiter = new Object();
        try {
            controller.toggleMute(sourceName, c -> {
                synchronized (waiter) {
                    waiter.notify();
                }
            });
            synchronized (waiter) {
                waiter.wait(WAIT_TIME);
            }
        } catch (Exception e) {
            log.error("Unable to toggle source mute {}", sourceName, e);
        }
    }

    public static void setSourceMute(String sourceName, boolean mute) {
        var waiter = new Object();
        try {
            controller.setMute(sourceName, mute, c -> {
                synchronized (waiter) {
                    waiter.notify();
                }
            });
            synchronized (waiter) {
                waiter.wait(WAIT_TIME);
            }
        } catch (Exception e) {
            log.error("Unable to set source mute {} {}", sourceName, mute, e);
        }
    }

    public static void setCurrentScene(String sceneName) {
        var waiter = new Object();
        try {
            controller.setCurrentScene(sceneName, c -> {
                synchronized (waiter) {
                    waiter.notify();
                }
            });
            synchronized (waiter) {
                waiter.wait(WAIT_TIME);
            }
        } catch (Exception e) {
            log.error("Unable to set current scene to {}", sceneName, e);
        }
    }

    public static boolean isConnected() {
        return Save.isObsEnabled() && controller != null && controller.isConnected();
    }
}
