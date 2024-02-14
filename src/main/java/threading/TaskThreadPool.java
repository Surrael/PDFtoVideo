package threading;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TaskThreadPool<T extends Task<T, V>, V> {

	private final ThreadPoolExecutor executor;
	private Consumer<T> startListener;
	private BiConsumer<T, V> finishListener;
	private BiConsumer<T, Exception> exceptionListener;
	private Runnable shutdownListener;
	
	public TaskThreadPool(int poolSize) {
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
	}
	
	public static <T extends Task<T, V>, V> TaskThreadPool<T, V> createPool(int poolSize) {
		return new TaskThreadPool<T, V>(poolSize);
	}
	
	public void submitTask(T task) {
		task.setStartListener(this.startListener);
		task.setFinishListener(this.finishListener);
		task.setExceptionListener(this.exceptionListener);
		this.executor.submit(task);
	}

	public TaskThreadPool<T, V> setFinishListener(BiConsumer<T, V> listener) {
		this.finishListener = listener;
		return this;
	}
	
	public TaskThreadPool<T, V> setStartListener(Consumer<T> listener) {
		this.startListener = listener;
		return this;
	}
	
	public TaskThreadPool<T, V> setExceptionListener(BiConsumer<T, Exception> listener) {
		this.exceptionListener = listener;
		return this;
	}
	
	public void initShutdown(boolean sync) {
		Thread t = new Thread(()->{
			try {
				this.executor.shutdown();
				this.executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				this.executor.shutdownNow();
			} finally {
				if(this.shutdownListener != null) this.shutdownListener.run();
			}
		});
		t.start();
		if(sync)
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public void setShutdownListener(Runnable listener) {
		this.shutdownListener = listener;
	}
}
