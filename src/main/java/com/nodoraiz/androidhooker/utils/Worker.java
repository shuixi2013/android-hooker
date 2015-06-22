package com.nodoraiz.androidhooker.utils;

import java.util.concurrent.*;

public class Worker {

    private static final int TASK_TIMEOUT_IN_SECONDS = 30;
    private static ExecutorService executorService;

    private Worker(){}

    /**
     * Run a callable in a separate thread if there isn't another callable running
     *
     * @param callable Callable to be executed in a separated thread
     * @return
     */
    public static boolean work(Callable<Void> callable){

        if(Worker.executorService != null && !Worker.executorService.isTerminated()){
            return false;
        }

        Worker.executorService = Executors.newSingleThreadExecutor();
        final Future task = Worker.executorService.submit(callable);
        Worker.executorService.shutdown();

        // Cancel task
        ScheduledExecutorService stopExecutorService = Executors.newSingleThreadScheduledExecutor();
        stopExecutorService.schedule(new Runnable() {
            public void run() {
                if(!task.isDone() && !task.isCancelled()) {
                    task.cancel(true);
                }
            }
        }, TASK_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

        return true;
    }

}
