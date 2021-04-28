package ch.admin.bag.dp3t.checkin.networking;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.crowdnotifier.android.sdk.model.ProblematicEventInfo;
import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.backend.UserAgentInterceptor;

import ch.admin.bag.dp3t.BuildConfig;
import ch.admin.bag.dp3t.checkin.models.Proto;
import ch.admin.bag.dp3t.storage.SecureStorage;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TraceKeysRepository {

	private static final String KEY_BUNDLE_TAG_HEADER = "x-key-bundle-tag";

	private TraceKeysService traceKeysService;
	private SecureStorage storage;

	public TraceKeysRepository(Context context) {

		storage = SecureStorage.getInstance(context);
		String baseUrl = BuildConfig.PUBLISHED_CROWDNOTIFIER_KEYS_BASE_URL;

		OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
		okHttpBuilder.networkInterceptors().add(new UserAgentInterceptor(DP3T.getUserAgent()));

		Retrofit bucketRetrofit = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.client(okHttpBuilder.build())
				.build();

		traceKeysService = bucketRetrofit.create(TraceKeysService.class);
	}

	public void loadTraceKeysAsync(Callback callback) {
		traceKeysService.getTraceKeys(storage.getCrowdNotifierLastKeyBundleTag()).enqueue(new retrofit2.Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
				if (response.isSuccessful()) {
					callback.onTraceKeysLoaded(handleSuccessfulResponse(response));
				} else {
					callback.onTraceKeysLoaded(null);
				}
			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {
				callback.onTraceKeysLoaded(null);
			}
		});
	}

	public List<ProblematicEventInfo> loadTraceKeys() {
		try {
			Response<ResponseBody> response = traceKeysService.getTraceKeys(storage.getCrowdNotifierLastKeyBundleTag()).execute();
			if (response.isSuccessful()) {
				return handleSuccessfulResponse(response);
			}
		} catch (IOException e) {
			return null;
		}
		return null;
	}

	private List<ProblematicEventInfo> handleSuccessfulResponse(Response<ResponseBody> response) {
		try {
			long keyBundleTag = Long.parseLong(response.headers().get(KEY_BUNDLE_TAG_HEADER));
			storage.setCrowdNotifierLastKeyBundleTag(keyBundleTag);
			Proto.ProblematicEventWrapper problematicEventWrapper =
					Proto.ProblematicEventWrapper.parseFrom(response.body().byteStream());
			ArrayList<ProblematicEventInfo> problematicEventInfos = new ArrayList<>();
			for (Proto.ProblematicEvent event : problematicEventWrapper.getEventsList()) {
				problematicEventInfos.add(new ProblematicEventInfo(event.getIdentity().toByteArray(),
						event.getSecretKeyForIdentity().toByteArray(), event.getStartTime(), event.getEndTime(),
						event.getEncryptedAssociatedData().toByteArray(), event.getCipherTextNonce().toByteArray()));
			}
			return problematicEventInfos;
		} catch (IOException e) {
			return null;
		}
	}

	public interface Callback {
		void onTraceKeysLoaded(List<ProblematicEventInfo> traceKeys);

	}

}
