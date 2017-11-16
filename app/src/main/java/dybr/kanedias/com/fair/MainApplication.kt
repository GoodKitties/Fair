package dybr.kanedias.com.fair

import android.app.Application
import dybr.kanedias.com.fair.database.DbProvider
import dybr.kanedias.com.fair.entities.Auth

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

        val acc = DbProvider.helper.accDao.queryBuilder().where().eq("current", true).queryForFirst()
        if (acc != null && Network.cookiesNotExpired()) {
            Auth.user = acc
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        DbProvider.releaseHelper()
    }
}
