package de.simolus3.fluttie;

import android.graphics.Canvas;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a pool of threads responsible for rendering animations. Each animation frame that needs
 * to be rendered will be assigned to a idling thread that will start to execute it. There is a
 * safety mechanism ensuring that an animation will never be rendered by multiple threads
 * simultaneously, as the Canvas API will not allow this.
 */
public class RenderingThreads implements Runnable {

	private FluttiePlugin plugin;

	private List<Thread> threads = new ArrayList<>();
	private RenderingQueue queue = new RenderingQueue();

	private boolean threadsStarted = false;
	private volatile boolean dontAcceptTasks = false;

	public RenderingThreads(int amount) {
		for (int i = 0; i < amount; i++) {
			threads.add(new Thread(this, "Fluttie Rendering Thread #" + i));
		}
	}

	public RenderingQueue getQueue() {
		return queue;
	}

	/**
	 * Makes the service accept new frames that need to be drawn again.
	 */
	public void start() {
		dontAcceptTasks = false;
		Log.d("RenderingThreads", "RenderingThreads now accepting tasks again");

		if (!threadsStarted) {
			for (Thread t : threads) {
				t.start();
			}

			threadsStarted = true;
		}
	}

	/**
	 * "Stops" the rendering threads from doing their work. This will not actually stop the threads
	 * but they will idle while waiting for new frames, which don't occur if the service is stopped.
	 * That state does not consume many resources.
	 */
	public void stop() {
		Log.d("RenderingThreads", "Stopped rendering threads");
		queue.clearBacklog();
		dontAcceptTasks = true;
	}

	public void markDirty(FluttieAnimation animation) {
		if (!dontAcceptTasks) {
			queue.scheduleDrawing(animation);
		}
	}

	@Override
	public void run() {
		while (true) {
			FluttieAnimation anim = null;
			try {
				anim = queue.waitAndObtain();

				Canvas canvas = anim.lockCanvas();
				if (canvas == null) //Animation decided not to be drawn at this time
					continue;

				anim.drawFrame(canvas);
				anim.unlockCanvasAndPost(canvas);
			} catch (InterruptedException ignore) {}
			finally {
				if (anim != null)
					queue.markCompleted(anim);
			}
		}
	}
}
