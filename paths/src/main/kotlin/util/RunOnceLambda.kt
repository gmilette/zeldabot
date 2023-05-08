package util
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class RunOnceLambda<T>(private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (value == null) {
            value = initializer()
        }
        return value!!
    }
}