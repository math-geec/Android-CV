package com.example.androidcv

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MLExecutionViewModel : ViewModel() {

    private val _styledBitmap = MutableLiveData<ModelExecutionResult>()

    // communicate to MainActivity using LiveData
    val styledBitmap: LiveData<ModelExecutionResult>
        get() = _styledBitmap

    // create a job instance to controls the lifecycle of the coroutine
    private val viewModelJob = Job()
    // create a scope to keep track of the coroutine
    private val viewModelScope = CoroutineScope(viewModelJob)

    fun onApplyStyle(
        context: Context,
        contentFilePath: String,
        styleFilePath: String,
        styleTransferModelExecutor: StyleTransferModelExecutor,
        inferenceThread: ExecutorCoroutineDispatcher
    ) {
        // start the coroutine that has viewModelScope as a parent
        viewModelScope.launch(inferenceThread) {
            val result =
                styleTransferModelExecutor.execute(contentFilePath, styleFilePath, context)
            _styledBitmap.postValue(result)
        }
    }
}
