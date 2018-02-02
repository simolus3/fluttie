package de.simolus3.fluttie;

import android.animation.ValueAnimator;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.OnCompositionLoadedListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;

public class FluttiePlugin implements MethodCallHandler, EventChannel.StreamHandler {

	public static void registerWith(Registrar registrar) {
		FluttiePlugin plugin = new FluttiePlugin(registrar);
		plugin.setUp();
	}

	private Registrar registrar;
	private EventChannel.EventSink sink;

	private SparseArray<FluttieAnimation> managedAnimations = new SparseArray<>();

	private SparseArray<LottieComposition> loadedCompositions = new SparseArray<>();
	private AtomicInteger compositionRequestCounter = new AtomicInteger();

	private RenderingThreads renderingThreads;

	private FluttiePlugin(Registrar registrar) {
		this.registrar = registrar;
	}

	private void setUp() {
		final MethodChannel channel = new MethodChannel(registrar.messenger(), "fluttie/methods");
		final EventChannel eventChannel = new EventChannel(registrar.messenger(), "fluttie/events");

		channel.setMethodCallHandler(this);
		eventChannel.setStreamHandler(this);

		/*
		 When multiple threads are working on rendering multiple animations simultaneously, at some
		 we will inevitably send two texture updates to the flutter engine with a really short time
		 delay in between. This, as it seems, will crash the engine :(
		 There is an issue that could be related to this, https://github.com/flutter/flutter/issues/14169
		 that we're waiting on to be fixed. After that is fixed, we can enable multithreaded rendering,
		 this code should otherwise work. If it still crashes, we can still take a look into that
		 later on and report a new issue to the Flutter team.
		 */
		renderingThreads = new RenderingThreads(1);
		renderingThreads.start();
	}

	private FluttieAnimation getManagedAnimation(MethodCall call) {
		int id = call.argument("id");
		return managedAnimations.get(id);
	}

	public RenderingThreads getRenderingThreads() {
		return renderingThreads;
	}

	@Override
	public void onMethodCall(MethodCall call, Result result) {
		switch (call.method) {
			case "isAvailable":
				result.success(true);
				return;
			case "loadAnimation":
				String sourceType = call.argument("source_type");
				String sourceData = call.argument("source");

				int requestId = compositionRequestCounter.getAndIncrement();
				try {
					loadComposition(requestId, sourceType, sourceData);
					result.success(requestId);
				} catch (JSONException e) {
					result.error("internal", "Could not load composition", e);
				}

				break;
			case "prepareAnimation":
				int compositionId = call.argument("composition");
				LottieComposition composition = loadedCompositions.get(compositionId);
				if (composition == null) {
					result.error(
							"invalid",
							"There is no prepared animation for that request id: "
									+ compositionId, null);
					return;
				}

				int repeatCount = call.argument("repeat_count");
				int repeatMode = call.argument("repeat_reverse") ?
						ValueAnimator.REVERSE : ValueAnimator.RESTART;
				int durationMillis = call.argument("duration");

				TextureRegistry.SurfaceTextureEntry texture =
						registrar.textures().createSurfaceTexture();

				FluttieAnimation animation = new FluttieAnimation(this, texture, composition);
				animation.setRepeatOptions(repeatCount, repeatMode);
				if (durationMillis > 0)
					animation.setDuration(durationMillis);

				managedAnimations.put((int) texture.id(), animation);
				result.success(texture.id());
				return;
			case "startAnimation":
				getManagedAnimation(call).startAnimation();
				result.success(null);
				return;
			case "pauseAnimation":
				getManagedAnimation(call).pauseAnimation();
				result.success(null);
				return;
			case "endAnimation":
				boolean reset = call.argument("reset_start");
				getManagedAnimation(call).stopAnimation(reset);
				result.success(null);
				return;
			case "disposeAnimation":
				FluttieAnimation anim = getManagedAnimation(call);
				anim.stopAndRelease();
				managedAnimations.remove(anim.getId());
				//if scheduled to be drawn, cancel
				renderingThreads.getQueue().removeAnimation(anim);
				result.success(null);
				return;
			default:
				result.notImplemented();
		}
	}

	@Override
	public void onListen(Object o, EventChannel.EventSink eventSink) {
		this.sink = eventSink;
	}

	@Override
	public void onCancel(Object o) {
		this.sink = null;
	}

	private void writeToSink(JSONObject msg) {
		if (sink != null)
			sink.success(msg.toString());
	}

	private void loadComposition(final int requestId, String sourceType, String source) throws JSONException {
		final JSONObject object = new JSONObject();
		object.put("request_id", requestId);
		object.put("event_type", "load_composition");

		OnCompositionLoadedListener listener = new OnCompositionLoadedListener() {
			@Override
			public void onCompositionLoaded(@Nullable LottieComposition composition) {
				loadedCompositions.append(requestId, composition);

				try {
					object.put("success", true);
				} catch (JSONException e) {
					Log.w("FluttiePlugin", "Could not add JSON value to event stream");
				}

				writeToSink(object);
			}
		};

		switch (sourceType) {
			case "inline":
				LottieComposition.Factory.fromJsonString(source, listener);
				break;
			default:
				object.put("success", false);
				writeToSink(object);
		}
	}
}
