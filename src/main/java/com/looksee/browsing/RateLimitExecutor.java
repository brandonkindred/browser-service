package com.looksee.browsing;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.Response;

/**
 * A custom command executor that limits the number of actions per second
 */
public class RateLimitExecutor extends HttpCommandExecutor {

    /**
     * The number of concurrent sessions
     */
    public static final int CONCURRENT_SESSIONS = 1;

    /**
     * The number of actions per second
     */
    public static final int ACTIONS_RATE_LIMIT_PER_SECOND = 50;

    /**
     * The number of seconds per action
     */
    public static final double SECONDS_PER_ACTION = ((double) CONCURRENT_SESSIONS)
            / ((double) ACTIONS_RATE_LIMIT_PER_SECOND);
    private long lastExecutionTime;

    /**
     * Creates a new RateLimitExecutor
     * @param addressOfRemoteServer the address of the remote server
     *
     * precondition: addressOfRemoteServer != null
     */
    public RateLimitExecutor(URL addressOfRemoteServer) {
        super(addressOfRemoteServer);
        assert addressOfRemoteServer != null;
        lastExecutionTime = 0;
    }

    /**
     * Executes a command
     * @param command the command to execute
     * @return the response
     * @throws IOException if an I/O error occurs
     *
     * precondition: command != null
     */
    public Response execute(Command command) throws IOException {
        assert command != null;
        long currentTime = Instant.now().toEpochMilli();
        double elapsedTime = TimeUnit.MILLISECONDS.toSeconds(currentTime - lastExecutionTime);
        if (elapsedTime < SECONDS_PER_ACTION) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis((long)(SECONDS_PER_ACTION - elapsedTime)));
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }
        lastExecutionTime = Instant.now().toEpochMilli();
        return super.execute(command);
    }
}