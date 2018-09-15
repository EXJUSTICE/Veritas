package com.xu.servicequalityrater.listeners;

import com.microsoft.projectoxford.emotion.contract.RecognizeResult;

import java.util.List;

/**TODO AsyncTasks dont execute OnPostExecute  in Services?
 * http://stackoverflow.com/questions/23217280/android-get-result-from-onpostexecute-asynctask-in-service
 */

public interface OnTaskCompleted {
    void onTaskCompleted(List<RecognizeResult> results);
}
