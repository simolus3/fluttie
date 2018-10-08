package de.simolus3.fluttie;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class that takes incoming animations which need to be drawn and lets worker Threads compete to
 * draw them. It makes sure that two animations will never be drawn to at the same time.
 * Threads wait for an FluttieAnimation to be assigned to them. After drawing it, they mark their
 * work as done. This class keeps track of what animations are currently being processed and what
 * still needs to be done.
 */
class RenderingQueue {

	//Animations waiting to be rendered
	private LinkedList<FluttieAnimation> backlog = new LinkedList<>();
	//Animations that are currently being rendered
	private Set<FluttieAnimation> currentlyHandling = new HashSet<>();

	private final ReentrantLock lock = new ReentrantLock();
	private final Object idleLock = new Object();

	/**
	 * Waits for a FluttieAnimation that needs to be drawn and returns it. It makes sure that there
	 * can never be two FluttieAnimations drawn to at the same time and sends calling Threads in
	 * idle if there is no work to do.
	 *
	 * If this method returns null, it means that the threads should stop.
	 *
	 * @return the first FluttieAnimation that needs to be drawn
	 * @throws InterruptedException if the Thread gets interrupted while waiting
	 */
	@NonNull
	FluttieAnimation waitAndObtain() throws InterruptedException {
		FluttieAnimation animation;
		while ((animation = obtain()) == null) {
			synchronized (idleLock) {
				idleLock.wait();
			}
		}

		return animation;
	}

	/**
	 * Returns the first FluttieAnimation that needs to be drawn, or null if there is nothing to do
	 * right now.
	 * @return an animation that needs to be drawn, or null
	 */
	@Nullable
	FluttieAnimation obtain() {
		try {
			lock.lock();
			FluttieAnimation animation = backlog.poll();
			currentlyHandling.add(animation);

			return animation;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Used to indicate that the animation has just been drawn. After this method has finished, the
	 * queue will allow that animation to be scheduled for a redraw again.
	 * @param animation the animation that has been drawn
	 */
	void markCompleted(FluttieAnimation animation) {
		try {
			lock.lock();
			currentlyHandling.remove(animation);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Schedules the given animation to be drawn by a worker thread later on. If the animation has
	 * already been scheduled, this method won't do anything. This does not lead to the animation
	 * lagging behind much, it will just skip a frame.
	 * @param animation the animation which needs to be drawn
	 */
	void scheduleDrawing(FluttieAnimation animation) {
		try {
			lock.lock();
			if (backlog.contains(animation))
				return;
			if (currentlyHandling.contains(animation))
				return;

			backlog.add(animation);

			synchronized (idleLock) {
				idleLock.notify();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * If the given animation has been scheduled to be drawn, attempts to delete it from the
	 * schedule. It can happen that the animation has already been assigned to a worker thread, in
	 * which case this method cannot stop the thread from drawing the started frame.
	 * @param animation the animation to remove
	 */
	void removeAnimation(FluttieAnimation animation) {
		try {
			lock.lock();

			backlog.remove(animation);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Clears the backlog holding a list of animations that need to be drawn.
	 */
	void clearBacklog() {
		try {
			lock.lock();

			backlog.clear();
		} finally {
			lock.unlock();
		}
	}
}
