package dybr.kanedias.com.fair

import android.app.Application
import dybr.kanedias.com.fair.database.DbProvider
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Place to initialize all data prior to launching activities
 *
 * @author Kanedias
 */
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DbProvider.setHelper(this)
        Network.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        DbProvider.releaseHelper()
    }
}
