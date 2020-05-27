/*
 * Copyright (C) 2019 Texas Instruments Incorporated - http://www.ti.com/
 *
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *    Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 *    Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.yonglin.pdc.epaper_setting.tasks;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.yonglin.pdc.epaper_setting.R;
import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.utils.FileUtils;
import com.yonglin.pdc.epaper_setting.utils.NetworkUtil;

import java.util.UUID;

public class CopyFileAsyncTask extends AsyncTask<Object, Void, Boolean> {
    private static final String TAG = "CopyFileAsyncTask";

    private SyncTaskCallback mTaskCallback;

    public CopyFileAsyncTask(SyncTaskCallback callBack) {
        mTaskCallback = callBack;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Log.d(TAG, "onPost started");
        super.onPostExecute(result);
    }

    @Override
    protected Boolean doInBackground(Object... objects) {
        Log.d(TAG, "doInBackground started");
        Context context = (Context) objects[0];
        UUID uuid = (UUID) objects[1];
        String filename = (String) objects[2];
        Uri uri = (Uri) objects[3];
        String result = FileUtils.saveExternalFile(context, uuid, filename, uri);
        boolean isDone = (result == null);
        if (mTaskCallback != null) {
            if (isDone) {
                mTaskCallback.onCompleted(filename);
            } else {
                mTaskCallback.onFailure(filename, result);
            }
        }

        return isDone;
    }

    public interface SyncTaskCallback {
        void onCompleted(String targetFileName);

        void onFailure(String targetFileName, String errorMessage);
    }
}
