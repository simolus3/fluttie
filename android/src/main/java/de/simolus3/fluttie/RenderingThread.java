package de.simolus3.fluttie;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RenderingThread implements Runnable {

	private FluttiePlugin plugin;
	private Thread thread;

	private volatile boolean shouldEnd = false;

	private ConcurrentLinkedQueue<FluttieAnimation> queue = new ConcurrentLinkedQueue<>();
	private final Object idleLock = new Object();

	public RenderingThread() {
		thread = new Thread(this, "Fluttie Rendering Thread");
	}

	private void unlock() {
		synchronized (idleLock) {
			idleLock.notifyAll();
		}
	}

	public void start() {
		thread.start();
	}

	public void stop() {
		shouldEnd = true;
	}

	public void markDirty(FluttieAnimation animation) {
		queue.add(animation);
		unlock();
	}

	@Override
	public void run() {
		while (!shouldEnd) {
			FluttieAnimation anim;
			while ((anim = queue.poll()) != null) {
				anim.drawSync();
			}

			try {
				synchronized (idleLock) {
					idleLock.wait();
				}
			} catch (InterruptedException ignore) {}
		}
	}
}
