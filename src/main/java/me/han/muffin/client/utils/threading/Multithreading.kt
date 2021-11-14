package me.han.muffin.client.utils.threading

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object Multithreading {

    val POOL: ExecutorService = Executors.newFixedThreadPool(100, object: ThreadFactory {
        val counter = AtomicInteger(0)
        override fun newThread(r: Runnable?): Thread {
            return Thread(r, String.format("Thread %s", counter.incrementAndGet()))
        }
    })

    private val RUNNABLE_POOL: ScheduledExecutorService = Executors.newScheduledThreadPool(10, object: ThreadFactory {
        private val counter: AtomicInteger = AtomicInteger(0)
        override fun newThread(r: Runnable?): Thread {
            return Thread(r, "Thread " + counter.incrementAndGet())
        }
    })

    fun schedule(r: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit) {
        RUNNABLE_POOL.scheduleAtFixedRate(r, initialDelay, delay, unit)
    }

    fun schedule(r: Runnable, delay: Long, unit: TimeUnit) {
        RUNNABLE_POOL.schedule(r, delay, unit)
    }

    fun runAsync(runnable: Runnable) {
        POOL.execute(runnable)
    }

    fun runCache(runnable: Runnable) {
        val executor = Executors.newCachedThreadPool()
        executor.submit(runnable)
        executor.shutdown()
    }

    fun getTotal(): Int {
        val tpe: ThreadPoolExecutor = POOL as ThreadPoolExecutor
        return tpe.activeCount
    }

}