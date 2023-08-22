import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Matcher

fun setButtonVisibilityAction(visibility: Int, clazz: Class<out View>): ViewAction {
    return object : ViewAction {

        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(clazz)
        }

        override fun perform(uiController: UiController, view: View) {
            view.visibility = visibility
        }

        override fun getDescription(): String {
            return "Show / Hide View"
        }
    }
}
