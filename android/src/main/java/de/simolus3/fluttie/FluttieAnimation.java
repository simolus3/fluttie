package de.simolus3.fluttie;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;

import androidx.annotation.Nullable;
import io.flutter.view.TextureRegistry;

public class FluttieAnimation implements ValueAnimator.AnimatorUpdateListener {

	private static final int BACKGROUND_COLOR = Color.TRANSPARENT;

	private final FluttiePlugin plugin;
	private final TextureRegistry.SurfaceTextureEntry surfaceTexture;

	private boolean pausedButNotByUser;
	private volatile boolean isDisposed;

	private LottieComposition composition;
	private LottieDrawable drawable;

	private Surface surface;

	FluttieAnimation(FluttiePlugin plugin, TextureRegistry.SurfaceTextureEntry surfaceTexture, LottieComposition composition, float scale) {
		this.plugin = plugin;
		this.surfaceTexture = surfaceTexture;

		surface = new Surface(surfaceTexture.surfaceTexture());
		Rect bounds = composition.getBounds();
		surfaceTexture.surfaceTexture().setDefaultBufferSize(
				(int) (scale * bounds.width()), (int) (scale * bounds.height()));

		drawable = new LottieDrawable();
		drawable.setCallback(plugin.getAnimationCallback());
		drawable.setScale(scale);
		this.composition = composition;
		drawable.setComposition(composition);

		drawable.addAnimatorUpdateListener(this);

		plugin.getRenderingThreads().markDirty(this);
	}

	void setRepeatOptions(int repeatCount, int repeatMode) {
		drawable.setRepeatCount(repeatCount);
		drawable.setRepeatMode(repeatMode);
	}

	void setDuration(int millis) {
		float factor = composition.getDuration() / (float) millis;
		drawable.setSpeed(Math.copySign(factor, drawable.getSpeed()));
	}

	public int getId() {
		return (int) surfaceTexture.id();
	}

	public Canvas lockCanvas() {
		//Can throw if there if flutter decided not to show the widget anymore
		//in which case surface.lockCanvas() will fail...
		try {
			return surface.isValid() && !isDisposed ? surface.lockCanvas(null) : null;
		} catch (Exception e) {
			Log.w("FluttieAnimation", "Could not obtain canvas. If "
				+ "you remembered to call FluttieAnimationController.dispose()"
				+ ", this should not occur often and is not a problem.", e);
		}

		return null;
	}

	public void drawFrame(Canvas canvas) {
		//would otherwise draw frames on top of older frames
		if (!isDisposed) {
			canvas.drawColor(BACKGROUND_COLOR, PorterDuff.Mode.CLEAR);
			try {
				drawable.draw(canvas);
			} catch (NullPointerException e) {
				Log.d("FluttieAnimation", "Could not draw. Disposed: " + isDisposed, e);
			}
		}
	}

	public void unlockCanvasAndPost(Canvas canvas) {
		//IllegalArgumentException thrown if the animation has been disposed in
		//flutter before we received the message telling us to stop rendering
		//it. There isn't really anything that we could do against it, so ignore
		try {
			surface.unlockCanvasAndPost(canvas);
		} catch (Exception e) {
			Log.w("FluttieAnimation", "Could not send canvas to flutter. If "
				+ "you remembered to call FluttieAnimationController.dispose()"
				+ ", this should not occur often and is not a problem.", e);
		}
	}

	@Override
	public void onAnimationUpdate(@Nullable ValueAnimator valueAnimator) {
		plugin.getRenderingThreads().markDirty(this);
	}

	public boolean isPlaying() {
		return drawable.isAnimating();
	}

	public boolean isPausedButNotByUser() {
		return pausedButNotByUser;
	}

	public void setPausedButNotByUser(boolean pausedButNotByUser) {
		this.pausedButNotByUser = pausedButNotByUser;
	}

	public void startAnimation() {
		stopAnimation(true);
		drawable.start();
	}

	public void resumeAnimation() {
		drawable.resumeAnimation();
	}

	public void pauseAnimation() {
		drawable.pauseAnimation();
	}

	public void stopAnimation(boolean resetToStart) {
		drawable.stop();
		drawable.setProgress(resetToStart ? 0 : 1);
	}

	public void stopAndRelease() {
		Log.d("FluttieAnimation", "Disposing animation with id " + getId());

		isDisposed = true;
		stopAnimation(false);

		drawable.clearComposition();
		drawable.recycleBitmaps();

		surfaceTexture.release();
		surface.release();
	}
}
