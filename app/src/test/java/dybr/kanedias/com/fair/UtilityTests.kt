package dybr.kanedias.com.fair

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.DisplayMetrics
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.entities.Account
import com.kanedias.dybr.fair.entities.Auth
import org.junit.Test

import org.junit.Ignore
import org.mockito.Mockito
import ru.noties.markwon.Markwon

/**
 * This test checks correctness of work of various utilities and components used by the main application
 *
 */
class UtilityTests {

    @Ignore("Doesn't work without real dimensions")
    @Test
    fun checkMarkwonTables() {
        val themeTa = Mockito.mock(TypedArray::class.java)
        Mockito.`when`(themeTa.getColor(Mockito.anyInt(), Mockito.anyInt())).thenReturn(0xffffff)

        val metrics = DisplayMetrics()
        metrics.density = 166f

        val resources = Mockito.mock(Resources::class.java)
        Mockito.`when`(resources.displayMetrics).thenReturn(metrics)

        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.obtainStyledAttributes(Mockito.anyInt(), Mockito.any())).thenReturn(themeTa)
        Mockito.`when`(context.resources).thenReturn(resources)

        val mdContent = Markwon.markdown(context, "aaaa")
        println(mdContent)
    }

    @Ignore("Not yet enabled in API")
    @Test
    fun confirmTokenWorks() {
        val context = Mockito.mock(Context::class.java)

        Network.init(context)
        Auth.user = Account()
        val answer = Network.confirmRegistration("kairllur@mail.ru", "7ju_mDbt61gTTSyHN8oD")
        println(answer)
    }
}
