package dybr.kanedias.com.fair

import android.app.Application
import dybr.kanedias.com.fair.database.DbProvider

/**
 * Place to initialize all data prior to launching activities
 *
 * @author Kanedias
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DbProvider.setHelper(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        DbProvider.releaseHelper()
    }
}
