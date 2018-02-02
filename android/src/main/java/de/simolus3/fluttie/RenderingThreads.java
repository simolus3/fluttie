package de.simolus3.fluttie;

import android.graphics.Canvas;

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

	private volatile boolean shouldEnd = false;

	public RenderingThreads(int amount) {
		for (int i = 0; i < amount; i++) {
			threads.add(new Thread(this, "Fluttie Rendering Thread #" + i));
		}
	}

	public RenderingQueue getQueue() {
		return queue;
	}

	public void start() {
		shouldEnd = false;

		for (Thread t : threads) {
			t.start();
		}
	}

	public void stop() {
		shouldEnd = true;

		for (Thread t : threads) {
			t.interrupt();
		}
	}

	public void markDirty(FluttieAnimation animation) {
		if (!shouldEnd) {
			queue.scheduleDrawing(animation);
		}
	}

	@Override
	public void run() {
		while (!shouldEnd) {
			try {
				FluttieAnimation anim = queue.waitAndObtain();

				Canvas canvas = anim.lockCanvas();
				anim.drawFrame(canvas);
				anim.unlockCanvasAndPost(canvas);

				queue.markCompleted(anim);
			} catch (InterruptedException ignore) {}
		}
	}
}
