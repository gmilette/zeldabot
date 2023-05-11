package util
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class RunOnceLambda(private val initializer: () -> Unit) : ReadOnlyProperty<Any?, String> {
    private var value: String? = null
    private val valueSet: String = "VALUE_SET"

    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        if (value == null) {
            initializer()
            value = valueSet
        }
        return value ?: ""
    }
}