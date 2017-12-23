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
        Auth.init(this)

        // load last account if it exists
        val acc = DbProvider.helper.accDao.queryBuilder().where().eq("current", true).queryForFirst()
        acc?.let {
            Auth.user = acc
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        DbProvider.releaseHelper()
    }
}
