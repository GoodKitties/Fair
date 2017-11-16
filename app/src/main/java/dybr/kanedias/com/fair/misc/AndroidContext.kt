package dybr.kanedias.com.fair.misc

import android.os.Handler
import android.os.Looper
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor

/**
 * Android Continuation, guarantees that, when resumed, is on the UI Thread
 * @author macastiblancot
 *
 * Created on 2/13/17.
 */
private class AndroidContinuation<in T>(val cont: Continuation<T>) : Continuation<T> by cont {
    override fun resume(value: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) cont.resume(value)
        else Handler(Looper.getMainLooper()).post { cont.resume(value) }
    }

    override fun resumeWithException(exception: Throwable) {
        if (Looper.myLooper() == Looper.getMainLooper()) cont.resumeWithException(exception)
        else Handler(Looper.getMainLooper()).post { cont.resumeWithException(exception) }
    }
}

/**
 * Android context, provides an AndroidContinuation, executes everything on the UI Thread
 */
object Android : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            AndroidContinuation(continuation)
}
