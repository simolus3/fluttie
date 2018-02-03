package de.simolus3.fluttie;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;

import io.flutter.view.TextureRegistry;

public class FluttieAnimation implements ValueAnimator.AnimatorUpdateListener {

	private static final int BACKGROUND_COLOR = Color.TRANSPARENT;

	private final FluttiePlugin plugin;
	private final TextureRegistry.SurfaceTextureEntry surfaceTexture;

	private boolean playing;

	private boolean pausedButNotByUser;

	private LottieComposition composition;
	private LottieDrawable drawable;

	private Surface surface;

	public FluttieAnimation(FluttiePlugin plugin, TextureRegistry.SurfaceTextureEntry surfaceTexture, LottieComposition composition, float scale) {
		this.plugin = plugin;
		this.surfaceTexture = surfaceTexture;

		surface = new Surface(surfaceTexture.surfaceTexture());
		Rect bounds = composition.getBounds();
		surfaceTexture.surfaceTexture().setDefaultBufferSize(
				(int) scale * bounds.width(), (int) scale * bounds.height());

		drawable = new LottieDrawable();
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
		return surface.lockCanvas(null);
	}

	public void drawFrame(Canvas canvas) {
		//would otherwise draw frames on top of older frames
		canvas.drawColor(BACKGROUND_COLOR, PorterDuff.Mode.CLEAR);
		drawable.draw(canvas);
	}

	public void unlockCanvasAndPost(Canvas canvas) {
		surface.unlockCanvasAndPost(canvas);
	}

	@Override
	public void onAnimationUpdate(@Nullable ValueAnimator valueAnimator) {
		plugin.getRenderingThreads().markDirty(this);
	}

	public boolean isPlaying() {
		return playing;
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
		playing = true;
	}

	public void resumeAnimation() {
		drawable.resumeAnimation();
		playing = true;
	}

	public void pauseAnimation() {
		drawable.pauseAnimation();
		playing = false;
	}

	public void stopAnimation(boolean resetToStart) {
		drawable.stop();
		drawable.setProgress(resetToStart ? 0 : 1);
		playing = false;
	}

	public void stopAndRelease() {
		stopAnimation(false);

		drawable.clearComposition();
		drawable.recycleBitmaps();

		surfaceTexture.release();
		surface.release();
	}
}
