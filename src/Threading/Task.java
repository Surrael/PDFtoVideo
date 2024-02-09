package Threading;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class Task<T extends Task<T, V>, V> implements Runnable {

	private final int id;
	
	private Consumer<T> startListener;
	private BiConsumer<T, V> finishListener;
	private BiConsumer<T, Exception> exceptionListener;

	public Task(int id) {
		this.id = id;
	}

	@Override
	public void run() {
		if(this.startListener != null) this.startListener.accept((T)this);
		try {
			V val = this.runTask();
			if(this.finishListener != null) this.finishListener.accept((T)this, val);
		} catch (Exception e) {
			if(this.exceptionListener != null) this.exceptionListener.accept((T)this, e);
			else e.printStackTrace();
		}
		
	}
	
	public abstract V runTask() throws Exception;

	public void setFinishListener(BiConsumer<T, V> finishListener) {
		this.finishListener = finishListener;
	}
	
	public void setExceptionListener(BiConsumer<T, Exception> exceptionListener) {
		this.exceptionListener = exceptionListener;
	}
	
	public void setStartListener(Consumer<T> startListener) {
		this.startListener = startListener;
	}

	public int getId() {
		return id;
	}

}
