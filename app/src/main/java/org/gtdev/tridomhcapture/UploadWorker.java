package org.gtdev.tridomhcapture;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

public class UploadWorker extends Worker {
    public final static String TAG = "UploadWorker";
    private static final String PROGRESS = "PROGRESS";
    public static final MediaType BINARY_MIME = MediaType.parse("application/octet-stream");

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());
    }

    public static class ProgressRequestBody extends RequestBody {

        protected RequestBody mDelegate;
        protected Listener mListener;
        protected CountingSink mCountingSink;

        public ProgressRequestBody(RequestBody delegate, Listener listener) {
            mDelegate = delegate;
            mListener = listener;
        }

        @Override
        public MediaType contentType() {
            return mDelegate.contentType();
        }

        @Override
        public long contentLength() {
            try {
                return mDelegate.contentLength();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return -1;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            mCountingSink = new CountingSink(sink);
            BufferedSink bufferedSink = Okio.buffer(mCountingSink);
            mDelegate.writeTo(bufferedSink);
            bufferedSink.flush();
        }

        protected final class CountingSink extends ForwardingSink {
            private long bytesWritten = 0;
            public CountingSink(Sink delegate) {
                super(delegate);
            }
            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                bytesWritten += byteCount;
                mListener.onProgress((int) (100F * bytesWritten / contentLength()));
            }
        }

        public interface Listener {
            void onProgress(int progress);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Result result = null;
        Data d = getInputData();
        String filePath = d.getString("filePath");
        String targetURL = d.getString("targetURL");
        if (filePath == null || targetURL == null) {
            result = Result.failure();
        } else {
            File file = new File(filePath);
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("video", file.getName(),
                            RequestBody.create(file, BINARY_MIME)).build();

            ProgressRequestBody rbWrapper = new ProgressRequestBody(requestBody, progress -> {
                setProgressAsync(new Data.Builder().putInt(PROGRESS, progress).build());
            });

            Request request = new Request.Builder()
                    .url(targetURL)
                    .post(rbWrapper)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    result = Result.failure();
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            if (result == null) {
                setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                result = Result.success();
            }
        }
        return result;
    }
}
