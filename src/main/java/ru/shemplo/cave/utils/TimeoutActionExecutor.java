package ru.shemplo.cave.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeoutActionExecutor implements Closeable {
    
    private ExecutorService executor = Executors.newCachedThreadPool ();
    
    public <T> T runWithTimeout (Callable <T> action, T defaultValue, long timeout) {
        final var future = executor.submit (action);
        try {            
            return future.get (timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException | InterruptedException te) {
            return defaultValue;
        }
    }

    @Override
    public void close () throws IOException {
        //executor.shutdownNow ();
    }
    
}
